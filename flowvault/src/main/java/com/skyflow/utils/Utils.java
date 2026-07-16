package com.skyflow.utils;

import com.google.gson.JsonObject;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.UpsertType;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest;
import com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest;
import com.skyflow.generated.rest.types.*;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.serviceaccount.util.BearerToken;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.vault.data.*;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Utils extends BaseUtils {

    public static String getVaultURL(String clusterId, Env env) {
        return getVaultURL(clusterId, env, Constants.VAULT_DOMAIN);
    }

    public static JsonObject getMetrics() {
        JsonObject details = getCommonMetrics();
        String sdkVersion = Constants.SDK_VERSION;
        details.addProperty(Constants.SDK_METRIC_NAME_VERSION, Constants.SDK_METRIC_NAME_VERSION_PREFIX + sdkVersion);
        return details;
    }


    public static String getEnvVaultURL() throws SkyflowException {
        try {
            String vaultURL = System.getenv("VAULT_URL");
            if (vaultURL == null) {
                Dotenv dotenv = Dotenv.load();
                vaultURL = dotenv.get("VAULT_URL");
            }
            if (vaultURL != null && vaultURL.trim().isEmpty()) {
                LogUtil.printErrorLog(ErrorLogs.EMPTY_VAULT_URL.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyVaultUrl.getMessage());
            } else if (vaultURL != null && !isValidURL(vaultURL)) {
                LogUtil.printErrorLog(ErrorLogs.INVALID_VAULT_URL_FORMAT.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidVaultUrlFormat.getMessage());
            }
            return vaultURL;
        } catch (DotenvException e) {
            return null;
        }
    }

    public static boolean isValidURL(String url) {
        URL parsedUrl;
        try {
            parsedUrl = new URL(url);
        } catch (MalformedURLException e) {
            return false;
        }

        if (!parsedUrl.getProtocol().equalsIgnoreCase("https")) {
            return false;
        } else {
            return parsedUrl.getHost() != null && !parsedUrl.getHost().isEmpty();
        }
    }


    public static String generateBearerToken(Credentials credentials) throws SkyflowException {
        if (credentials.getPath() != null) {
            BearerToken.BearerTokenBuilder builder = BearerToken.builder()
                    .setCredentials(new File(credentials.getPath()))
                    .setRoles(credentials.getRoles());
            Object ctx = credentials.getContext();
            if (ctx instanceof String) {
                builder.setCtx((String) ctx);
            } else if (ctx instanceof Map) {
                builder.setCtx((Map<String, Object>) ctx);
            }
            return builder.build().getBearerToken();
        } else if (credentials.getCredentialsString() != null) {
            BearerToken.BearerTokenBuilder builder = BearerToken.builder()
                    .setCredentials(credentials.getCredentialsString())
                    .setRoles(credentials.getRoles());
            Object ctx = credentials.getContext();
            if (ctx instanceof String) {
                builder.setCtx((String) ctx);
            } else if (ctx instanceof Map) {
                builder.setCtx((Map<String, Object>) ctx);
            }
            return builder.build().getBearerToken();
        } else {
            return credentials.getToken();
        }
    }

    public static V1InsertRequest getBulkInsertRequestBody(InsertRequest request, VaultConfig config) {
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

    public static InsertResponse buildInsertResponse(V1InsertResponse res) {
        ArrayList<HashMap<String, Object>> insertedFields = new ArrayList<>();
        ArrayList<HashMap<String, Object>> errors = new ArrayList<>();

        if (res.getRecords().isPresent()) {
            for (V1RecordResponseObject record : res.getRecords().get()) {
                if (record.getError().isPresent()) {
                    HashMap<String, Object> errorRecord = new HashMap<>();
                    record.getSkyflowId().ifPresent(skyflowId -> errorRecord.put("skyflowId", skyflowId));
                    record.getTableName().ifPresent(tableName -> errorRecord.put("tableName", tableName));
                    errorRecord.put("error", record.getError().get());
                    record.getHttpCode().ifPresent(httpCode -> errorRecord.put("httpCode", httpCode));
                    errors.add(errorRecord);
                } else {
                    HashMap<String, Object> insertedRecord = new HashMap<>();
                    record.getSkyflowId().ifPresent(skyflowId -> insertedRecord.put("skyflowId", skyflowId));
                    record.getTokens().ifPresent(insertedRecord::putAll);
                    insertedFields.add(insertedRecord);
                }
            }
        }
        return new InsertResponse(insertedFields, errors);
    }

    public static com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest getDetokenizeRequestBody(DetokenizeRequest request, String vaultid) {
        List<DetokenizeData> detokenizeData = request.getDetokenizeData();
        List<String> tokens = new ArrayList<>();
        for(int i = 0; i< detokenizeData.size(); i++){
            tokens.add(detokenizeData.get(i).getToken());
        }
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.Builder builder =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.builder()
                        .vaultId(vaultid)
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

    public static DetokenizeResponse buildDetokenizeResponse(V1FlowDetokenizeResponse res) {
        ArrayList<DetokenizeRecordResponse> detokenizedFields = new ArrayList<>();
        ArrayList<DetokenizeRecordResponse> errors = new ArrayList<>();

        if (res.getResponse().isPresent()) {
            for (V1FlowDetokenizeResponseObject record : res.getResponse().get()) {
                String token = record.getToken().orElse(null);
                String tokenGroupName = record.getTokenGroupName().orElse(null);
                Map<String, Object> metadata = record.getMetadata().orElse(null);
                if (record.getError().isPresent()) {
                    errors.add(new DetokenizeRecordResponse(token, null, record.getError().get(), tokenGroupName, metadata));
                } else {
                    Object value = record.getValue().orElse(null);
                    detokenizedFields.add(new DetokenizeRecordResponse(token, value, null, tokenGroupName, metadata));
                }
            }
        }
        return new DetokenizeResponse(detokenizedFields, errors);
    }
}
