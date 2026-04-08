package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.UpsertType;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.ApiClientBuilder;
import com.skyflow.generated.rest.resources.flowservice.FlowserviceClient;
import com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest;
import com.skyflow.generated.rest.types.FlowEnumUpdateType;
import com.skyflow.generated.rest.types.V1InsertRecordData;
import com.skyflow.generated.rest.types.V1Upsert;
import com.skyflow.logs.InfoLogs;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.InsertRecord;
import com.skyflow.vault.data.DeleteTokensRequest;
import com.skyflow.vault.data.TokenizeRequest;
import com.skyflow.vault.data.TokenizeRecord;
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

    protected FlowserviceClient getRecordsApi() {
        return this.apiClient.flowservice();
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
        // Fetch vaultURL from ENV
        String vaultURL = Utils.getEnvVaultURL();

        // If vaultURL from ENV is null or empty, fetch vaultURL from vault config
        if (vaultURL == null || vaultURL.isEmpty()) {
            vaultURL = this.vaultConfig.getVaultURL();
        }

        // If vaultURL from vault config is also null or empty, construct vaultURL from clusterId passed in vault config
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

    protected V1InsertRequest getBulkInsertRequestBody(com.skyflow.vault.data.InsertRequest request, VaultConfig config) {
        ArrayList<InsertRecord> records = request.getRecords();
        List<V1InsertRecordData> insertRecordDataList = new ArrayList<>();
        for (InsertRecord record : records) {
            V1InsertRecordData.Builder data = V1InsertRecordData.builder();
            data.data(record.getData());
            if (record.getTable() != null && !record.getTable().isEmpty()) {
                data.tableName(record.getTable());
            }
            if (record.getUpsert() != null && !record.getUpsert().isEmpty()) {
                if (record.getUpsertType() != null) {
                    FlowEnumUpdateType updateType = null;
                    if (record.getUpsertType() == UpsertType.REPLACE) {
                        updateType = FlowEnumUpdateType.REPLACE;
                    } else if (record.getUpsertType() == UpsertType.UPDATE) {
                        updateType = FlowEnumUpdateType.UPDATE;
                    }
                    V1Upsert upsert = V1Upsert.builder().uniqueColumns(record.getUpsert()).updateType(updateType).build();
                    data.upsert(upsert);
                } else {
                    V1Upsert upsert = V1Upsert.builder().uniqueColumns(record.getUpsert()).build();
                    data.upsert(upsert);
                }
            }
            insertRecordDataList.add(data.build());
        }

        V1InsertRequest.Builder builder = V1InsertRequest.builder()
                .vaultId(config.getVaultId())
                .records(insertRecordDataList);

        if (request.getTable() != null && !request.getTable().isEmpty()) {
            builder.tableName(request.getTable());
        }

        if (request.getUpsert() != null && !request.getUpsert().isEmpty()) {
            if (request.getUpsertType() != null) {
                FlowEnumUpdateType updateType = null;
                if (request.getUpsertType() == UpsertType.REPLACE) {
                    updateType = FlowEnumUpdateType.REPLACE;
                } else if (request.getUpsertType() == UpsertType.UPDATE) {
                    updateType = FlowEnumUpdateType.UPDATE;
                }
                V1Upsert upsert = V1Upsert.builder().uniqueColumns(request.getUpsert()).updateType(updateType).build();
                builder.upsert(upsert);
            } else {
                V1Upsert upsert = V1Upsert.builder().uniqueColumns(request.getUpsert()).build();
                builder.upsert(upsert);
            }
        }
        return builder.build();

    }

    protected com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest getDetokenizeRequestBody(DetokenizeRequest request) {
        List<String> tokens = request.getTokens();
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.Builder builder =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.builder()
                        .vaultId(this.vaultConfig.getVaultId())
                        .tokens(tokens);
        if (request.getTokenGroupRedactions() != null) {
            List<com.skyflow.generated.rest.types.V1TokenGroupRedactions> tokenGroupRedactionsList = new ArrayList<>();
            for (com.skyflow.vault.data.TokenGroupRedactions tokenGroupRedactions : request.getTokenGroupRedactions()) {
                com.skyflow.generated.rest.types.V1TokenGroupRedactions redactions =
                        com.skyflow.generated.rest.types.V1TokenGroupRedactions.builder()
                                .tokenGroupName(tokenGroupRedactions.getTokenGroupName())
                                .redaction(tokenGroupRedactions.getRedaction())
                                .build();
                tokenGroupRedactionsList.add(redactions);
            }

            builder.tokenGroupRedactions(tokenGroupRedactionsList);
        }
        return builder.build();
    }

    protected com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest getDeleteTokensRequestBody(DeleteTokensRequest request) {
        return com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest.builder()
                .vaultId(this.vaultConfig.getVaultId())
                .tokens(request.getTokens())
                .build();
    }

    protected com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest getTokenizeRequestBody(TokenizeRequest request) {
        List<com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject> dataList = new ArrayList<>();
        for (TokenizeRecord record : request.getData()) {
            com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject obj =
                    com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder()
                            .value(record.getValue())
                            .tokenGroupNames(record.getTokenGroupNames())
                            .build();
            dataList.add(obj);
        }
        return com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.builder()
                .vaultId(this.vaultConfig.getVaultId())
                .data(dataList)
                .build();
    }
}
