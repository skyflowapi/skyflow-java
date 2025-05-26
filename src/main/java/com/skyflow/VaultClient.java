package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.ApiClientBuilder;
import com.skyflow.generated.rest.resources.query.QueryClient;
import com.skyflow.generated.rest.resources.records.RecordsClient;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceBatchOperationBody;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceInsertRecordBody;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceUpdateRecordBody;
import com.skyflow.generated.rest.resources.strings.StringsClient;
import com.skyflow.generated.rest.resources.tokens.TokensClient;
import com.skyflow.generated.rest.resources.tokens.requests.V1DetokenizePayload;
import com.skyflow.generated.rest.resources.tokens.requests.V1TokenizePayload;
import com.skyflow.generated.rest.types.*;
import com.skyflow.logs.InfoLogs;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.tokens.ColumnValue;
import com.skyflow.vault.tokens.DetokenizeData;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.TokenizeRequest;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VaultClient {
    private final VaultConfig vaultConfig;
    private final ApiClientBuilder apiClientBuilder;
    private ApiClient apiClient;
    private Credentials commonCredentials;
    private Credentials finalCredentials;
    private String token;
    private String apiKey;

    protected VaultClient(VaultConfig vaultConfig, Credentials credentials) {
        super();
        this.vaultConfig = vaultConfig;
        this.commonCredentials = credentials;
        this.apiClientBuilder = new ApiClientBuilder();
        this.apiClient = null;
        updateVaultURL();
    }

    protected RecordsClient getRecordsApi() {
        return this.apiClient.records();
    }

    protected TokensClient getTokensApi() {
        return this.apiClient.tokens();
    }

    protected StringsClient getDetectTextApi() {
        return this.apiClient.strings();
    }

    protected QueryClient getQueryApi() {
        return this.apiClient.query();
    }

    protected VaultConfig getVaultConfig() {
        return vaultConfig;
    }

    protected void setCommonCredentials(Credentials commonCredentials) throws SkyflowException {
        this.commonCredentials = commonCredentials;
        prioritiseCredentials();
    }

    protected void updateVaultConfig() throws SkyflowException {
        updateVaultURL();
        prioritiseCredentials();
    }

    protected V1DetokenizePayload getDetokenizePayload(DetokenizeRequest request) {
        List<V1DetokenizeRecordRequest> recordRequests = new ArrayList<>();

        for (DetokenizeData detokenizeDataRecord : request.getDetokenizeData()) {
            V1DetokenizeRecordRequest recordRequest = V1DetokenizeRecordRequest.builder()
                    .token(detokenizeDataRecord.getToken())
                    .redaction(detokenizeDataRecord.getRedactionType().getRedaction())
                    .build();
            recordRequests.add(recordRequest);
        }

        return V1DetokenizePayload.builder()
                .continueOnError(request.getContinueOnError())
                .downloadUrl(request.getDownloadURL())
                .detokenizationParameters(recordRequests)
                .build();
    }

    protected RecordServiceInsertRecordBody getBulkInsertRequestBody(InsertRequest request) {
        List<HashMap<String, Object>> values = request.getValues();
        List<HashMap<String, Object>> tokens = request.getTokens();
        List<V1FieldRecords> records = new ArrayList<>();

        for (int index = 0; index < values.size(); index++) {
            V1FieldRecords.Builder recordBuilder = V1FieldRecords.builder().fields(values.get(index));
            if (tokens != null && index < tokens.size()) {
                recordBuilder.tokens(tokens.get(index));
            }
            records.add(recordBuilder.build());
        }

        return RecordServiceInsertRecordBody.builder()
                .tokenization(request.getReturnTokens())
                .homogeneous(request.getHomogeneous())
                .upsert(request.getUpsert())
                .byot(request.getTokenMode().getBYOT())
                .records(records)
                .build();
    }

    protected RecordServiceBatchOperationBody getBatchInsertRequestBody(InsertRequest request) {
        ArrayList<HashMap<String, Object>> values = request.getValues();
        ArrayList<HashMap<String, Object>> tokens = request.getTokens();
        List<V1BatchRecord> records = new ArrayList<>();

        for (int index = 0; index < values.size(); index++) {
            V1BatchRecord.Builder recordBuilder = V1BatchRecord.builder()
                    .method(BatchRecordMethod.POST)
                    .tableName(request.getTable())
                    .upsert(request.getUpsert())
                    .tokenization(request.getReturnTokens())
                    .fields(values.get(index));

            if (tokens != null && index < tokens.size()) {
                recordBuilder.tokens(tokens.get(index));
            }

            records.add(recordBuilder.build());
        }

        return RecordServiceBatchOperationBody.builder()
                .continueOnError(true)
                .byot(request.getTokenMode().getBYOT())
                .records(records)
                .build();
    }

    protected RecordServiceUpdateRecordBody getUpdateRequestBody(UpdateRequest request) {
        RecordServiceUpdateRecordBody.Builder updateRequestBodyBuilder = RecordServiceUpdateRecordBody.builder();
        updateRequestBodyBuilder.byot(request.getTokenMode().getBYOT());
        updateRequestBodyBuilder.tokenization(request.getReturnTokens());
        V1FieldRecords.Builder recordBuilder = V1FieldRecords.builder();
        HashMap<String, Object> values = request.getData();

        if (values != null) {
            recordBuilder.fields(values);
        }

        HashMap<String, Object> tokens = request.getTokens();
        if (tokens != null) {
            recordBuilder.tokens(tokens);
        }

        updateRequestBodyBuilder.record(recordBuilder.build());

        return updateRequestBodyBuilder.build();
    }

    protected V1TokenizePayload getTokenizePayload(TokenizeRequest request) {
        List<V1TokenizeRecordRequest> tokenizationParameters = new ArrayList<>();

        for (ColumnValue columnValue : request.getColumnValues()) {
            V1TokenizeRecordRequest.Builder recordBuilder = V1TokenizeRecordRequest.builder();
            String value = columnValue.getValue();
            if (value != null) {
                recordBuilder.value(value);
            }
            String columnGroup = columnValue.getColumnGroup();
            if (columnGroup != null) {
                recordBuilder.columnGroup(columnGroup);
            }

            tokenizationParameters.add(recordBuilder.build());
        }

        V1TokenizePayload.Builder payloadBuilder = V1TokenizePayload.builder();

        if (!tokenizationParameters.isEmpty()) {
            payloadBuilder.tokenizationParameters(tokenizationParameters);
        }

        return payloadBuilder.build();
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
        this.apiClientBuilder.token(token);
        this.apiClient = this.apiClientBuilder.build();
    }

    private void setApiKey() {
        if (apiKey == null) {
            apiKey = this.finalCredentials.getApiKey();
        } else {
            LogUtil.printInfoLog(InfoLogs.REUSE_API_KEY.getLog());
        }
        this.apiClientBuilder.token(token);
    }

    private void updateVaultURL() {
        String vaultURL = Utils.getVaultURL(this.vaultConfig.getClusterId(), this.vaultConfig.getEnv());
        this.apiClientBuilder.url(vaultURL);
    }

    private void prioritiseCredentials() throws SkyflowException {
        try {
            Credentials original = this.finalCredentials;
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
            if (original != null && !original.equals(this.finalCredentials)) {
                token = null;
                apiKey = null;
            }
        } catch (DotenvException e) {
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                    ErrorMessage.EmptyCredentials.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
