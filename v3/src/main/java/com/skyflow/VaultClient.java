package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.UpsertType;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.ApiClientBuilder;
import com.skyflow.generated.rest.resources.recordservice.RecordserviceClient;
import com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest;
import com.skyflow.generated.rest.types.EnumUpdateType;
import com.skyflow.generated.rest.types.InsertRecordData;
import com.skyflow.generated.rest.types.Upsert;
import com.skyflow.logs.InfoLogs;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.InsertRecord;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.ArrayList;
import java.util.List;


public class VaultClient {
    private final VaultConfig vaultConfig;
    private final ApiClientBuilder apiClientBuilder;
    private ApiClient apiClient;
    private Credentials commonCredentials;
    private Credentials finalCredentials;
    private String token;
    private String apiKey;

    protected VaultClient(VaultConfig vaultConfig, Credentials credentials) throws SkyflowException {
        super();
        this.vaultConfig = vaultConfig;
        this.commonCredentials = credentials;
        this.apiClientBuilder = new ApiClientBuilder();
        this.apiClient = null;
        updateVaultURL();
    }

    protected RecordserviceClient getRecordsApi() {
        return this.apiClient.recordservice();
    }

    protected VaultConfig getVaultConfig() {
        return vaultConfig;
    }

    protected void setCommonCredentials(Credentials commonCredentials) throws SkyflowException {
        this.commonCredentials = commonCredentials;
        prioritiseCredentials();
    }

    protected void setBearerToken() throws SkyflowException {
        prioritiseCredentials();
        Validations.validateCredentials(this.finalCredentials);
        if (this.finalCredentials.getApiKey() != null) {
            LogUtil.printInfoLog(InfoLogs.USE_API_KEY.getLog());
            token = this.finalCredentials.getApiKey();
        } else if (token == null || token.trim().isEmpty()) {
            token = Utils.generateBearerToken(this.finalCredentials);
        } else if (Token.isExpired(token)) {
            LogUtil.printInfoLog(InfoLogs.BEARER_TOKEN_EXPIRED.getLog());
            token = Utils.generateBearerToken(this.finalCredentials);
        } else {
            LogUtil.printInfoLog(InfoLogs.REUSE_BEARER_TOKEN.getLog());
        }
        updateExecutorInHTTP(); // update executor
        this.apiClient = this.apiClientBuilder.build();
    }

    private void updateVaultURL() throws SkyflowException {
        String vaultURL = Utils.getEnvVaultURL();
        if (vaultURL == null || vaultURL.isEmpty()) {
            vaultURL = Utils.getVaultURL(this.vaultConfig.getClusterId(), this.vaultConfig.getEnv());
        }
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
                String sysCredentials = System.getenv(Constants.ENV_CREDENTIALS_KEY_NAME);
                if (sysCredentials == null) {
                    Dotenv dotenv = Dotenv.load();
                    sysCredentials = dotenv.get(Constants.ENV_CREDENTIALS_KEY_NAME);
                }
                if (sysCredentials == null) {
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyCredentials.getMessage());
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
        }
//         catch (Exception e) {
//            throw new RuntimeException(e);
//        }
    }

    protected void updateExecutorInHTTP() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request requestWithAuth = original.newBuilder()
                            .header("Authorization", "Bearer " + this.token)
                            .build();
                    return chain.proceed(requestWithAuth);
                })
                .build();
        apiClientBuilder.httpClient(httpClient);
    }

    protected InsertRequest getBulkInsertRequestBody(com.skyflow.vault.data.InsertRequest request, VaultConfig config) {
        ArrayList<InsertRecord> records = request.getRecords();
        List<InsertRecordData> insertRecordDataList = new ArrayList<>();
        for (InsertRecord record : records) {
            InsertRecordData.Builder data = InsertRecordData.builder();
            data.data(record.getData());
            if (record.getTable() != null && !record.getTable().isEmpty()) {
                data.tableName(record.getTable());
            }
            if (record.getUpsert() != null && !record.getUpsert().isEmpty()) {
                if (record.getUpsertType() != null) {
                    EnumUpdateType updateType = null;
                    if (record.getUpsertType() == UpsertType.REPLACE) {
                        updateType = EnumUpdateType.REPLACE;
                    } else if (record.getUpsertType() == UpsertType.UPDATE) {
                        updateType = EnumUpdateType.UPDATE;
                    }
                    Upsert upsert = Upsert.builder().uniqueColumns(record.getUpsert()).updateType(updateType).build();
                    data.upsert(upsert);
                } else {
                    Upsert upsert = Upsert.builder().uniqueColumns(record.getUpsert()).build();
                    data.upsert(upsert);
                }
            }
            insertRecordDataList.add(data.build());
        }

        InsertRequest.Builder builder = InsertRequest.builder()
                .vaultId(config.getVaultId())
                .records(insertRecordDataList);

        if (request.getTable() != null && !request.getTable().isEmpty()) {
            builder.tableName(request.getTable());
        }

        if (request.getUpsert() != null && !request.getUpsert().isEmpty()) {
            if (request.getUpsertType() != null) {
                EnumUpdateType updateType = null;
                if (request.getUpsertType() == UpsertType.REPLACE) {
                    updateType = EnumUpdateType.REPLACE;
                } else if (request.getUpsertType() == UpsertType.UPDATE) {
                    updateType = EnumUpdateType.UPDATE;
                }
                Upsert upsert = Upsert.builder().uniqueColumns(request.getUpsert()).updateType(updateType).build();
                builder.upsert(upsert);
            } else {
                Upsert upsert = Upsert.builder().uniqueColumns(request.getUpsert()).build();
                builder.upsert(upsert);
            }
        }
        return builder.build();

    }

    protected com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest getDetokenizeRequestBody(DetokenizeRequest request) {
        List<String> tokens = request.getTokens();
        com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest.Builder builder =
                com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest.builder()
                        .vaultId(this.vaultConfig.getVaultId())
                        .tokens(tokens);
        if (request.getTokenGroupRedactions() != null) {
            List<com.skyflow.generated.rest.types.TokenGroupRedactions> tokenGroupRedactionsList = new ArrayList<>();
            for (com.skyflow.vault.data.TokenGroupRedactions tokenGroupRedactions : request.getTokenGroupRedactions()) {
                com.skyflow.generated.rest.types.TokenGroupRedactions redactions =
                        com.skyflow.generated.rest.types.TokenGroupRedactions.builder()
                                .tokenGroupName(tokenGroupRedactions.getTokenGroupName())
                                .redaction(tokenGroupRedactions.getRedaction())
                                .build();
                tokenGroupRedactionsList.add(redactions);
            }

            builder.tokenGroupRedactions(tokenGroupRedactionsList);
        }
        return builder.build();
    }
}
