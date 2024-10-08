package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.api.RecordsApi;
import com.skyflow.generated.rest.api.TokensApi;
import com.skyflow.generated.rest.models.RecordServiceInsertRecordBody;
import com.skyflow.generated.rest.models.V1DetokenizePayload;
import com.skyflow.generated.rest.models.V1DetokenizeRecordRequest;
import com.skyflow.generated.rest.models.V1FieldRecords;
import com.skyflow.utils.Utils;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.tokens.DetokenizeRequest;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VaultClient {
    private final RecordsApi recordsApi;
    private final TokensApi tokensApi;
    private final ApiClient apiClient;
    private final VaultConfig vaultConfig;
    private Credentials commonCredentials;
    private Credentials finalCredentials;

    protected VaultClient(VaultConfig vaultConfig, Credentials credentials) {
        super();
        this.vaultConfig = vaultConfig;
        this.commonCredentials = credentials;
        this.apiClient = new ApiClient();
        this.tokensApi = new TokensApi(this.apiClient);
        this.recordsApi = new RecordsApi(this.apiClient);
        updateVaultURL();
        prioritiseCredentials();
    }

    protected Credentials getFinalCredentials() {
        return finalCredentials;
    }

    protected RecordsApi getRecordsApi() {
        return recordsApi;
    }

    protected TokensApi getTokensApi() {
        return tokensApi;
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

    protected RecordServiceInsertRecordBody getInsertRequestBody(InsertRequest request) {
        RecordServiceInsertRecordBody insertRecordBody = new RecordServiceInsertRecordBody();
        insertRecordBody.setTokenization(request.getReturnTokens());
        insertRecordBody.setHomogeneous(request.getHomogeneous());
        insertRecordBody.setUpsert(request.getUpsert());
        insertRecordBody.setByot(request.getTokenStrict().getBYOT());

        List<HashMap<String, String>> values = request.getValues();
        List<HashMap<String, String>> tokens = request.getTokens();
        List<V1FieldRecords> records = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            V1FieldRecords record = new V1FieldRecords();
            record.setFields(values.get(index));
            record.setTokens(tokens.get(index));
            records.add(record);
        }
        insertRecordBody.setRecords(records);
        return insertRecordBody;
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
                String sysCredentials = dotenv.get("SKYFLOW_CREDENTIALS");
                if (sysCredentials == null) {
                    // throw error for not passing any credentials
                } else {
                    this.finalCredentials = new Credentials();
                    this.finalCredentials.setCredentialsString(sysCredentials);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
