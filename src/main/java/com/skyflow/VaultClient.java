package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.api.QueryApi;
import com.skyflow.generated.rest.api.RecordsApi;
import com.skyflow.generated.rest.api.TokensApi;
import com.skyflow.generated.rest.auth.HttpBearerAuth;
import com.skyflow.generated.rest.models.*;
import com.skyflow.logs.InfoLogs;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.tokens.ColumnValue;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.TokenizeRequest;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VaultClient {
    private final RecordsApi recordsApi;
    private final TokensApi tokensApi;
    private final QueryApi queryApi;
    private final ApiClient apiClient;
    private final VaultConfig vaultConfig;
    private Credentials commonCredentials;
    private Credentials finalCredentials;
    private String token;
    private String apiKey;

    protected VaultClient(VaultConfig vaultConfig, Credentials credentials) {
        super();
        this.vaultConfig = vaultConfig;
        this.commonCredentials = credentials;
        this.apiClient = new ApiClient();
        this.recordsApi = new RecordsApi(this.apiClient);
        this.tokensApi = new TokensApi(this.apiClient);
        this.queryApi = new QueryApi(this.apiClient);
        updateVaultURL();
    }

    protected RecordsApi getRecordsApi() {
        return recordsApi;
    }

    protected TokensApi getTokensApi() {
        return tokensApi;
    }

    protected QueryApi getQueryApi() {
        return queryApi;
    }

    protected ApiClient getApiClient() {
        return apiClient;
    }

    protected VaultConfig getVaultConfig() {
        return vaultConfig;
    }

    protected void setCommonCredentials(Credentials commonCredentials) {
        this.commonCredentials = commonCredentials;
        prioritiseCredentials();
    }

    protected void updateVaultConfig() {
        updateVaultURL();
        prioritiseCredentials();
    }

    protected V1DetokenizePayload getDetokenizePayload(DetokenizeRequest request) {
        V1DetokenizePayload payload = new V1DetokenizePayload();
        payload.setContinueOnError(request.getContinueOnError());
        for (String token : request.getTokens()) {
            V1DetokenizeRecordRequest recordRequest = new V1DetokenizeRecordRequest();
            recordRequest.setToken(token);
            recordRequest.setRedaction(request.getRedactionType().getRedaction());
            payload.addDetokenizationParametersItem(recordRequest);
        }
        return payload;
    }

    protected RecordServiceInsertRecordBody getBulkInsertRequestBody(InsertRequest request) {
        RecordServiceInsertRecordBody insertRecordBody = new RecordServiceInsertRecordBody();
        insertRecordBody.setTokenization(request.getReturnTokens());
        insertRecordBody.setHomogeneous(request.getHomogeneous());
        insertRecordBody.setUpsert(request.getUpsert());
        insertRecordBody.setByot(request.getTokenStrict().getBYOT());

        List<HashMap<String, Object>> values = request.getValues();
        List<HashMap<String, Object>> tokens = request.getTokens();
        List<V1FieldRecords> records = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            V1FieldRecords record = new V1FieldRecords();
            record.setFields(values.get(index));
            if (tokens != null && index < tokens.size()) {
                record.setTokens(tokens.get(index));
            }
            records.add(record);
        }
        insertRecordBody.setRecords(records);
        return insertRecordBody;
    }

    protected RecordServiceBatchOperationBody getBatchInsertRequestBody(InsertRequest request) {
        RecordServiceBatchOperationBody insertRequestBody = new RecordServiceBatchOperationBody();
        insertRequestBody.setContinueOnError(true);
        insertRequestBody.setByot(request.getTokenStrict().getBYOT());

        ArrayList<HashMap<String, Object>> values = request.getValues();
        ArrayList<HashMap<String, Object>> tokens = request.getTokens();
        List<V1BatchRecord> records = new ArrayList<>();

        for (int index = 0; index < values.size(); index++) {
            V1BatchRecord record = new V1BatchRecord();
            record.setMethod(BatchRecordMethod.POST);
            record.setTableName(request.getTable());
            record.setUpsert(request.getUpsert());
            record.setTokenization(request.getReturnTokens());
            record.setFields(values.get(index));
            if (tokens != null && index < tokens.size()) {
                record.setTokens(tokens.get(index));
            }
            records.add(record);
        }

        insertRequestBody.setRecords(records);
        return insertRequestBody;
    }

    protected RecordServiceUpdateRecordBody getUpdateRequestBody(UpdateRequest request) {
        RecordServiceUpdateRecordBody updateRequestBody = new RecordServiceUpdateRecordBody();
        updateRequestBody.byot(request.getTokenStrict().getBYOT());
        updateRequestBody.setTokenization(request.getReturnTokens());
        HashMap<String, Object> values = request.getData();
        HashMap<String, Object> tokens = request.getTokens();
        V1FieldRecords record = new V1FieldRecords();
        record.setFields(values);
        if (tokens != null) {
            record.setTokens(tokens);
        }
        updateRequestBody.setRecord(record);
        return updateRequestBody;
    }

    protected V1TokenizePayload getTokenizePayload(TokenizeRequest request) {
        V1TokenizePayload payload = new V1TokenizePayload();
        for (ColumnValue columnValue : request.getColumnValues()) {
            V1TokenizeRecordRequest recordRequest = new V1TokenizeRecordRequest();
            recordRequest.setValue(columnValue.getValue());
            recordRequest.setColumnGroup(columnValue.getColumnGroup());
            payload.addTokenizationParametersItem(recordRequest);
        }
        return payload;
    }

    protected void setBearerToken() throws SkyflowException {
        prioritiseCredentials();
        Validations.validateCredentials(this.finalCredentials);
        if (this.finalCredentials.getApiKey() != null) {
            setApiKey();
            return;
        } else if (Token.isExpired(token)) {
            LogUtil.printInfoLog(InfoLogs.BEARER_TOKEN_EXPIRED.getLog());
            token = Utils.generateBearerToken(this.finalCredentials);
        } else {
            LogUtil.printInfoLog(InfoLogs.REUSE_BEARER_TOKEN.getLog());
        }
        HttpBearerAuth Bearer = (HttpBearerAuth) this.apiClient.getAuthentication("Bearer");
        Bearer.setBearerToken(token);
    }

    private void setApiKey() {
        if (apiKey == null) {
            apiKey = this.finalCredentials.getApiKey();
        } else {
            LogUtil.printInfoLog(InfoLogs.REUSE_API_KEY.getLog());
        }
        HttpBearerAuth Bearer = (HttpBearerAuth) this.apiClient.getAuthentication("Bearer");
        Bearer.setBearerToken(apiKey);
    }

    private void updateVaultURL() {
        String vaultURL = Utils.getVaultURL(this.vaultConfig.getClusterId(), this.vaultConfig.getEnv());
        this.apiClient.setBasePath(vaultURL);
    }

    private void prioritiseCredentials() {
        try {
            if (this.vaultConfig.getCredentials() != null) {
                this.finalCredentials = this.vaultConfig.getCredentials();
            } else if (this.commonCredentials != null) {
                this.finalCredentials = this.commonCredentials;
            } else {
                Dotenv dotenv = Dotenv.load();
                String sysCredentials = dotenv.get(Constants.ENV_CREDENTIALS_KEY_NAME);
                if (sysCredentials == null) {
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                            ErrorMessage.EmptyCredentials.getMessage());
                } else {
                    this.finalCredentials = new Credentials();
                    this.finalCredentials.setCredentialsString(sysCredentials);
                }
            }
            token = null;
            apiKey = null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
