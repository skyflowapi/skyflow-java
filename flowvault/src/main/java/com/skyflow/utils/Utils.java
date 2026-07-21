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
import com.skyflow.logs.WarningLogs;
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

        if (res != null && res.getRecords().isPresent()) {
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

        if (res != null && res.getResponse().isPresent()) {
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

    public static com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest getDeleteTokensRequestBody(DeleteTokensRequest request, String vaultid) {
        return com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest.builder()
                .vaultId(vaultid)
                .tokens(request.getTokens())
                .build();
    }
    private static String extractRequestId(Map<String, List<String>> headers) {
        if (headers == null) return null;
        List<String> ids = headers.get(BaseConstants.REQUEST_ID_HEADER_KEY);
        return (ids == null || ids.isEmpty()) ? null : ids.get(0);
    }

    public static DeleteTokensResponse buildDeleteTokensResponse(V1FlowDeleteTokenResponse res, Map<String, List<String>> headers, int requestedTokenCount) {
        ArrayList<String> deletedTokens = new ArrayList<>();
        ArrayList<HashMap<String, Object>> errors = new ArrayList<>();
        String requestId = extractRequestId(headers);
        if (res != null && res.getTokens().isPresent()) {
            for (V1DeleteTokenResponseObject record : res.getTokens().get()) {
                if (record.getError().isPresent()) {
                    HashMap<String, Object> errorRecord = new HashMap<>();
                    errorRecord.put("error", record.getError().get());
                    record.getHttpCode().ifPresent(httpCode -> errorRecord.put("httpCode", httpCode));
                    errorRecord.put("requestId", requestId);
                    errors.add(errorRecord);
                } else {
                    record.getValue().ifPresent(deletedTokens::add);
                }
            }
            if (deletedTokens.size() + errors.size() != requestedTokenCount) {
                LogUtil.printWarningLog(WarningLogs.INCOMPLETE_DELETE_TOKENS_RESPONSE.getLog());
            }
        } else {
            LogUtil.printWarningLog(WarningLogs.EMPTY_DELETE_TOKENS_RESPONSE.getLog());
        }
        return new DeleteTokensResponse(deletedTokens, errors);
    }

    public static com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest getTokenizeRequestBody(TokenizeRequest request, String vaultid) {
        List<V1FlowTokenizeRequestObject> dataList = new ArrayList<>();
        for (TokenizeRecord record : request.getData()) {
            V1FlowTokenizeRequestObject obj = V1FlowTokenizeRequestObject.builder()
                    .value(record.getValue())
                    .tokenGroupNames(record.getTokenGroupNames())
                    .build();
            dataList.add(obj);
        }
        return com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.builder()
                .vaultId(vaultid)
                .data(dataList)
                .build();
    }

    public static TokenizeResponse buildTokenizeResponse(V1FlowTokenizeResponse res, Map<String, List<String>> headers, int requestedRecordCount) {
        List<TokenizeData> tokenizedData = new ArrayList<>();
        ArrayList<HashMap<String, Object>> errors = new ArrayList<>();
        String requestId = extractRequestId(headers);
        if (res != null && res.getResponse().isPresent()) {
            List<V1FlowTokenizeResponseObject> records = res.getResponse().get();
            int indexNumber = 0;
            for (V1FlowTokenizeResponseObject record : records) {
                Object value = record.getValue().orElse(null);
                TokenizeData tokenizeData = new TokenizeData(value, indexNumber);
                boolean hasAnySuccess = false;
                if (record.getTokens().isPresent()) {
                    for (FlowTokenizeResponseObjectToken tokenObj : record.getTokens().get()) {
                        if (tokenObj.getError().isPresent()) {
                            HashMap<String, Object> errorRecord = new HashMap<>();
                            errorRecord.put("error", tokenObj.getError().get());
                            tokenObj.getHttpCode().ifPresent(httpCode -> errorRecord.put("httpCode", httpCode));
                            tokenObj.getTokenGroupName().ifPresent(name -> errorRecord.put("tokenGroupName", name));
                            errorRecord.put("index", indexNumber);
                            errorRecord.put("requestId", requestId);
                            errors.add(errorRecord);
                        } else if (tokenObj.getTokenGroupName().isPresent() && tokenObj.getToken().isPresent()) {
                            tokenizeData.addToken(tokenObj.getTokenGroupName().get(), tokenObj.getToken().get());
                            hasAnySuccess = true;
                        }
                    }
                }
                if (hasAnySuccess) {
                    tokenizedData.add(tokenizeData);
                }
                indexNumber++;
            }
            if (indexNumber != requestedRecordCount) {
                LogUtil.printWarningLog(WarningLogs.INCOMPLETE_TOKENIZE_RESPONSE.getLog());
            }
        } else {
            LogUtil.printWarningLog(WarningLogs.EMPTY_TOKENIZE_RESPONSE.getLog());
        }
        TokenizeResponse tokenizeResponse = new TokenizeResponse(errors);
        tokenizeResponse.setTokenizedData(tokenizedData);
        return tokenizeResponse;
    }

    // ── Bulk (batched/concurrent) request-body builders ──────────────────────

    public static com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest getBulkInsertRequestBody(BulkInsertRequest request, VaultConfig config) {
        ArrayList<BulkInsertRecord> records = request.getRecords();
        List<V1InsertRecordData> insertRecordDataList = new ArrayList<>();
        for (BulkInsertRecord record : records) {
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

        com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest.Builder builder =
                com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest.builder()
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

    public static V1FlowDetokenizeRequest getBulkDetokenizeRequestBody(BulkDetokenizeRequest request, String vaultId) {
        V1FlowDetokenizeRequest.Builder builder = V1FlowDetokenizeRequest.builder()
                .vaultId(vaultId)
                .tokens(request.getTokens());
        if (request.getTokenGroupRedactions() != null && !request.getTokenGroupRedactions().isEmpty()) {
            List<V1TokenGroupRedactions> tokenGroupRedactionsList = new ArrayList<>();
            for (BulkTokenGroupRedactions tokenGroupRedactions : request.getTokenGroupRedactions()) {
                tokenGroupRedactionsList.add(V1TokenGroupRedactions.builder()
                        .tokenGroupName(tokenGroupRedactions.getTokenGroupName())
                        .redaction(tokenGroupRedactions.getRedaction())
                        .build());
            }
            builder.tokenGroupRedactions(tokenGroupRedactionsList);
        }
        return builder.build();
    }

    public static com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest getBulkDeleteTokensRequestBody(BulkDeleteTokensRequest request, String vaultId) {
        return com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest.builder()
                .vaultId(vaultId)
                .tokens(request.getTokens())
                .build();
    }

    public static com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest getBulkTokenizeRequestBody(BulkTokenizeRequest request, String vaultId) {
        List<V1FlowTokenizeRequestObject> dataList = new ArrayList<>();
        for (BulkTokenizeRecord record : request.getData()) {
            dataList.add(V1FlowTokenizeRequestObject.builder()
                    .value(record.getValue())
                    .tokenGroupNames(record.getTokenGroupNames())
                    .build());
        }
        return com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.builder()
                .vaultId(vaultId)
                .data(dataList)
                .build();
    }

    // ── Bulk batching, exception-handling and response-formatting helpers ────

    public static List<List<V1InsertRecordData>> createBulkInsertBatches(List<V1InsertRecordData> records, int batchSize) {
        List<List<V1InsertRecordData>> batches = new ArrayList<>();
        for (int i = 0; i < records.size(); i += batchSize) {
            batches.add(records.subList(i, Math.min(i + batchSize, records.size())));
        }
        return batches;
    }

    public static List<V1FlowDetokenizeRequest> createBulkDetokenizeBatches(V1FlowDetokenizeRequest request, int batchSize) {
        List<V1FlowDetokenizeRequest> detokenizeRequests = new ArrayList<>();
        List<String> tokens = request.getTokens().get();

        for (int i = 0; i < tokens.size(); i += batchSize) {
            List<String> batchTokens = tokens.subList(i, Math.min(i + batchSize, tokens.size()));
            List<V1TokenGroupRedactions> tokenGroupRedactions = null;
            if (request.getTokenGroupRedactions().isPresent() && !request.getTokenGroupRedactions().get().isEmpty()) {
                tokenGroupRedactions = request.getTokenGroupRedactions().get();
            }
            V1FlowDetokenizeRequest batchRequest = V1FlowDetokenizeRequest.builder()
                    .vaultId(request.getVaultId())
                    .tokens(new ArrayList<>(batchTokens))
                    .tokenGroupRedactions(tokenGroupRedactions)
                    .build();

            detokenizeRequests.add(batchRequest);
        }

        return detokenizeRequests;
    }

    public static List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest> createBulkDeleteTokensBatches(
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest request, int batchSize) {
        List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest> batches = new ArrayList<>();
        List<String> tokens = request.getTokens().get();
        for (int i = 0; i < tokens.size(); i += batchSize) {
            List<String> batchTokens = tokens.subList(i, Math.min(i + batchSize, tokens.size()));
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest batchRequest =
                    com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest.builder()
                            .vaultId(request.getVaultId())
                            .tokens(new ArrayList<>(batchTokens))
                            .build();
            batches.add(batchRequest);
        }
        return batches;
    }

    public static List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest> createBulkTokenizeBatches(
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest request, int batchSize) {
        List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest> batches = new ArrayList<>();
        List<V1FlowTokenizeRequestObject> data = request.getData().get();
        for (int i = 0; i < data.size(); i += batchSize) {
            List<V1FlowTokenizeRequestObject> batchData = data.subList(i, Math.min(i + batchSize, data.size()));
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batchRequest =
                    com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.builder()
                            .vaultId(request.getVaultId())
                            .data(new ArrayList<>(batchData))
                            .build();
            batches.add(batchRequest);
        }
        return batches;
    }

    public static ErrorRecord createErrorRecord(Map<String, Object> recordMap, int indexNumber, String requestId) {
        ErrorRecord err = null;
        if (recordMap != null) {
            int code = 500;
            if (recordMap.containsKey("http_code")) {
                code = (Integer) recordMap.get("http_code");
            } else if (recordMap.containsKey("httpCode")) {
                code = (Integer) recordMap.get("httpCode");
            } else if (recordMap.containsKey("statusCode")) {
                code = (Integer) recordMap.get("statusCode");
            }
            String message = recordMap.containsKey("error") ? (String) recordMap.get("error") :
                    recordMap.containsKey("message") ? (String) recordMap.get("message") : "Unknown error";
            err = new ErrorRecord(indexNumber, message, code, requestId);
        }
        return err;
    }

    public static List<ErrorRecord> handleBulkInsertBatchException(
            Throwable ex, List<V1InsertRecordData> batch, int batchNumber, int batchSize
    ) {
        List<ErrorRecord> errorRecords = new ArrayList<>();
        Throwable cause = ex.getCause();
        if (cause instanceof ApiClientApiException) {
            ApiClientApiException apiException = (ApiClientApiException) cause;
            String requestId = extractRequestId(apiException.headers());
            Object rawBody = apiException.body();
            Map<String, Object> responseBody = (rawBody instanceof Map) ? (Map<String, Object>) rawBody : null;
            int indexNumber = batchNumber > 0 ? batchNumber * batchSize : 0;
            if (responseBody != null) {
                if (responseBody.containsKey("records")) {
                    Object recordss = responseBody.get("records");
                    if (recordss instanceof List) {
                        List<?> recordsList = (List<?>) recordss;
                        for (Object record : recordsList) {
                            if (record instanceof Map) {
                                Map<String, Object> recordMap = (Map<String, Object>) record;
                                ErrorRecord err = createErrorRecord(recordMap, indexNumber, requestId);
                                errorRecords.add(err);
                                indexNumber++;
                            }
                        }
                    }
                } else if (responseBody.containsKey("error")) {
                    Object errField = responseBody.get("error");
                    Map<String, Object> recordMap = (errField instanceof Map) ? (Map<String, Object>) errField : null;
                    String fallbackMsg = (errField instanceof String) ? (String) errField : null;
                    for (int j = 0; j < batch.size(); j++) {
                        ErrorRecord err = (recordMap != null)
                                ? createErrorRecord(recordMap, indexNumber, requestId)
                                : new ErrorRecord(indexNumber, fallbackMsg != null ? fallbackMsg : apiException.getMessage(), apiException.statusCode(), requestId);
                        errorRecords.add(err);
                        indexNumber++;
                    }
                }
            }
            if (errorRecords.isEmpty()) {
                for (int j = 0; j < batch.size(); j++) {
                    errorRecords.add(new ErrorRecord(indexNumber, apiException.getMessage(), apiException.statusCode(), requestId));
                    indexNumber++;
                }
            }
        } else {
            int indexNumber = batchNumber > 0 ? batchNumber * batchSize : 0;
            for (int j = 0; j < batch.size(); j++) {
                ErrorRecord err = new ErrorRecord(indexNumber, ex.getMessage(), 500);
                errorRecords.add(err);
                indexNumber++;
            }
        }
        return errorRecords;
    }

    public static List<ErrorRecord> handleBulkDetokenizeBatchException(
            Throwable ex, V1FlowDetokenizeRequest batch, int batchNumber, int batchSize
    ) {
        List<ErrorRecord> errorRecords = new ArrayList<>();
        Throwable cause = ex.getCause();
        if (cause instanceof ApiClientApiException) {
            ApiClientApiException apiException = (ApiClientApiException) cause;
            String requestId = extractRequestId(apiException.headers());
            Object rawBody = apiException.body();
            Map<String, Object> responseBody = (rawBody instanceof Map) ? (Map<String, Object>) rawBody : null;
            int indexNumber = batchNumber * batchSize;
            if (responseBody != null) {
                if (responseBody.containsKey("response")) {
                    Object recordss = responseBody.get("response");
                    if (recordss instanceof List) {
                        List<?> recordsList = (List<?>) recordss;
                        for (Object record : recordsList) {
                            if (record instanceof Map) {
                                Map<String, Object> recordMap = (Map<String, Object>) record;
                                ErrorRecord err = createErrorRecord(recordMap, indexNumber, requestId);
                                errorRecords.add(err);
                                indexNumber++;
                            }
                        }
                    }
                } else if (responseBody.containsKey("error")) {
                    Object errField = responseBody.get("error");
                    Map<String, Object> recordMap = (errField instanceof Map) ? (Map<String, Object>) errField : null;
                    String fallbackMsg = (errField instanceof String) ? (String) errField : null;
                    int tokenCount = batch.getTokens().isPresent() ? batch.getTokens().get().size() : 0;
                    for (int j = 0; j < tokenCount; j++) {
                        ErrorRecord err = (recordMap != null)
                                ? createErrorRecord(recordMap, indexNumber, requestId)
                                : new ErrorRecord(indexNumber, fallbackMsg != null ? fallbackMsg : apiException.getMessage(), apiException.statusCode(), requestId);
                        errorRecords.add(err);
                        indexNumber++;
                    }
                }
            }
            if (errorRecords.isEmpty()) {
                int tokenCount = batch.getTokens().isPresent() ? batch.getTokens().get().size() : 0;
                for (int j = 0; j < tokenCount; j++) {
                    errorRecords.add(new ErrorRecord(indexNumber, apiException.getMessage(), apiException.statusCode(), requestId));
                    indexNumber++;
                }
            }
        } else {
            int indexNumber = batchNumber * batchSize;
            for (int j = 0; j < batch.getTokens().get().size(); j++) {
                ErrorRecord err = new ErrorRecord(indexNumber, ex.getMessage(), 500);
                errorRecords.add(err);
                indexNumber++;
            }
        }
        return errorRecords;
    }

    public static List<ErrorRecord> handleBulkDeleteTokensBatchException(
            Throwable ex,
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest batch,
            int batchNumber, int batchSize
    ) {
        List<ErrorRecord> errorRecords = new ArrayList<>();
        Throwable cause = ex.getCause();
        if (cause instanceof ApiClientApiException) {
            ApiClientApiException apiException = (ApiClientApiException) cause;
            String requestId = extractRequestId(apiException.headers());
            Object rawBody = apiException.body();
            Map<String, Object> responseBody = (rawBody instanceof Map) ? (Map<String, Object>) rawBody : null;
            int indexNumber = batchNumber * batchSize;
            if (responseBody != null) {
                if (responseBody.containsKey("tokens")) {
                    Object tokensList = responseBody.get("tokens");
                    if (tokensList instanceof List) {
                        List<?> recordsList = (List<?>) tokensList;
                        for (Object record : recordsList) {
                            if (record instanceof Map) {
                                Map<String, Object> recordMap = (Map<String, Object>) record;
                                ErrorRecord err = createErrorRecord(recordMap, indexNumber, requestId);
                                errorRecords.add(err);
                                indexNumber++;
                            }
                        }
                    }
                } else if (responseBody.containsKey("error")) {
                    Object errField = responseBody.get("error");
                    Map<String, Object> recordMap = (errField instanceof Map) ? (Map<String, Object>) errField : null;
                    String fallbackMsg = (errField instanceof String) ? (String) errField : null;
                    int tokenCount = batch.getTokens().isPresent() ? batch.getTokens().get().size() : 0;
                    for (int j = 0; j < tokenCount; j++) {
                        ErrorRecord err = (recordMap != null)
                                ? createErrorRecord(recordMap, indexNumber, requestId)
                                : new ErrorRecord(indexNumber, fallbackMsg != null ? fallbackMsg : apiException.getMessage(), apiException.statusCode(), requestId);
                        errorRecords.add(err);
                        indexNumber++;
                    }
                }
            }
            if (errorRecords.isEmpty()) {
                int tokenCount = batch.getTokens().isPresent() ? batch.getTokens().get().size() : 0;
                for (int j = 0; j < tokenCount; j++) {
                    errorRecords.add(new ErrorRecord(indexNumber, apiException.getMessage(), apiException.statusCode(), requestId));
                    indexNumber++;
                }
            }
        } else {
            int indexNumber = batchNumber * batchSize;
            for (int j = 0; j < batch.getTokens().get().size(); j++) {
                ErrorRecord err = new ErrorRecord(indexNumber, ex.getMessage(), 500);
                errorRecords.add(err);
                indexNumber++;
            }
        }
        return errorRecords;
    }

    public static List<ErrorRecord> handleBulkTokenizeBatchException(
            Throwable ex,
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batch,
            int batchNumber, int batchSize
    ) {
        List<ErrorRecord> errorRecords = new ArrayList<>();
        Throwable cause = ex.getCause();
        if (cause instanceof ApiClientApiException) {
            ApiClientApiException apiException = (ApiClientApiException) cause;
            String requestId = extractRequestId(apiException.headers());
            Object rawBody = apiException.body();
            Map<String, Object> responseBody = (rawBody instanceof Map) ? (Map<String, Object>) rawBody : null;
            int indexNumber = batchNumber * batchSize;
            if (responseBody != null) {
                if (responseBody.containsKey("response")) {
                    Object responseList = responseBody.get("response");
                    if (responseList instanceof List) {
                        List<?> recordsList = (List<?>) responseList;
                        for (Object record : recordsList) {
                            if (record instanceof Map) {
                                Map<String, Object> recordMap = (Map<String, Object>) record;
                                ErrorRecord err = createErrorRecord(recordMap, indexNumber, requestId);
                                errorRecords.add(err);
                                indexNumber++;
                            }
                        }
                    }
                } else if (responseBody.containsKey("error")) {
                    Object errField = responseBody.get("error");
                    Map<String, Object> recordMap = (errField instanceof Map) ? (Map<String, Object>) errField : null;
                    String fallbackMsg = (errField instanceof String) ? (String) errField : null;
                    int batchDataSize = batch.getData().isPresent() ? batch.getData().get().size() : 0;
                    for (int j = 0; j < batchDataSize; j++) {
                        ErrorRecord err = (recordMap != null)
                                ? createErrorRecord(recordMap, indexNumber, requestId)
                                : new ErrorRecord(indexNumber, fallbackMsg != null ? fallbackMsg : apiException.getMessage(), apiException.statusCode(), requestId);
                        errorRecords.add(err);
                        indexNumber++;
                    }
                }
            }
            if (errorRecords.isEmpty()) {
                int batchDataSize = batch.getData().isPresent() ? batch.getData().get().size() : 0;
                for (int j = 0; j < batchDataSize; j++) {
                    errorRecords.add(new ErrorRecord(indexNumber, apiException.getMessage(), apiException.statusCode(), requestId));
                    indexNumber++;
                }
            }
        } else {
            int indexNumber = batchNumber * batchSize;
            int batchDataSize = batch.getData().isPresent() ? batch.getData().get().size() : 0;
            for (int j = 0; j < batchDataSize; j++) {
                ErrorRecord err = new ErrorRecord(indexNumber, ex.getMessage(), 500);
                errorRecords.add(err);
                indexNumber++;
            }
        }
        return errorRecords;
    }

    public static BulkInsertResponse formatBulkInsertResponse(V1InsertResponse response, int batch, int batchSize, Map<String, List<String>> headers) {
        BulkInsertResponse formattedResponse = null;
        List<Success> successRecords = new ArrayList<>();
        List<ErrorRecord> errorRecords = new ArrayList<>();
        if (response != null) {
            String requestId = extractRequestId(headers);
            List<V1RecordResponseObject> record = response.getRecords().get();
            int indexNumber = batch * batchSize;
            int recordsSize = response.getRecords().get().size();
            for (int index = 0; index < recordsSize; index++) {
                if (record.get(index).getError().isPresent()) {
                    ErrorRecord errorRecord = new ErrorRecord(indexNumber, record.get(index).getError().get(), record.get(index).getHttpCode().orElse(500), requestId);
                    errorRecords.add(errorRecord);
                } else {
                    Map<String, List<com.skyflow.vault.data.Token>> tokensMap = null;
                    if (record.get(index).getTokens().isPresent()) {
                        tokensMap = new HashMap<>();
                        Map<String, Object> tok = record.get(index).getTokens().get();
                        for (Map.Entry<String, Object> entry : tok.entrySet()) {
                            String key = entry.getKey();
                            Object value = entry.getValue();
                            List<com.skyflow.vault.data.Token> tokenList = new ArrayList<>();
                            if (value instanceof List) {
                                List<?> valueList = (List<?>) value;
                                for (Object item : valueList) {
                                    if (item instanceof Map) {
                                        Map<String, Object> tokenMap = (Map<String, Object>) item;
                                        com.skyflow.vault.data.Token token = new com.skyflow.vault.data.Token((String) tokenMap.get("token"), (String) tokenMap.get("tokenGroupName"));
                                        tokenList.add(token);
                                    }
                                }
                            }
                            tokensMap.put(key, tokenList);
                        }
                    }
                    Success success = new Success(indexNumber, record.get(index).getSkyflowId().orElse(null), tokensMap, record.get(index).getData().isPresent() ? record.get(index).getData().get() : null, record.get(index).getTableName().isPresent() ? record.get(index).getTableName().get() : null);
                    successRecords.add(success);
                }
                indexNumber++;
            }
            formattedResponse = new BulkInsertResponse(successRecords, errorRecords);
        }
        return formattedResponse;
    }

    public static BulkDetokenizeResponse formatBulkDetokenizeResponse(V1FlowDetokenizeResponse response, int batch, int batchSize, Map<String, List<String>> headers) {
        if (response != null && response.getResponse().isPresent()) {
            String requestId = extractRequestId(headers);
            List<V1FlowDetokenizeResponseObject> record = response.getResponse().get();
            List<ErrorRecord> errorRecords = new ArrayList<>();
            List<DetokenizeResponseObject> successRecords = new ArrayList<>();
            int indexNumber = batch * batchSize;
            int recordsSize = record.size();
            for (int index = 0; index < recordsSize; index++) {
                if (record.get(index).getError().isPresent()) {
                    ErrorRecord errorRecord = new ErrorRecord(indexNumber, record.get(index).getError().get(), record.get(index).getHttpCode().orElse(500), requestId);
                    errorRecords.add(errorRecord);
                } else {
                    DetokenizeResponseObject success = new DetokenizeResponseObject(indexNumber, record.get(index).getToken().orElse(null), record.get(index).getValue().orElse(null), record.get(index).getTokenGroupName().orElse(null), record.get(index).getError().orElse(null), record.get(index).getMetadata().orElse(null));
                    successRecords.add(success);
                }
                indexNumber++;
            }
            return new BulkDetokenizeResponse(successRecords, errorRecords);
        }
        return null;
    }

    public static BulkDeleteTokensResponse formatBulkDeleteTokensResponse(
            V1FlowDeleteTokenResponse response, int batch, int batchSize, Map<String, List<String>> headers) {
        if (response != null && response.getTokens().isPresent()) {
            String requestId = extractRequestId(headers);
            List<V1DeleteTokenResponseObject> records = response.getTokens().get();
            List<ErrorRecord> errorRecords = new ArrayList<>();
            List<DeleteTokensSuccess> successRecords = new ArrayList<>();
            int indexNumber = batch * batchSize;
            for (V1DeleteTokenResponseObject record : records) {
                String tokenValue = record.getValue().orElse(null);
                if (record.getError().isPresent()
                        && record.getError().get() != null
                        && !record.getError().get().isEmpty()
                        && record.getHttpCode().orElse(200) != 200) {
                    ErrorRecord errorRecord = new ErrorRecord(indexNumber, record.getError().get(),
                            record.getHttpCode().orElse(500), requestId);
                    errorRecords.add(errorRecord);
                } else {
                    DeleteTokensSuccess success = new DeleteTokensSuccess(indexNumber, tokenValue);
                    successRecords.add(success);
                }
                indexNumber++;
            }
            return new BulkDeleteTokensResponse(successRecords, errorRecords);
        }
        return null;
    }

    public static BulkTokenizeResponse formatBulkTokenizeResponse(
            V1FlowTokenizeResponse response,
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batchRequest,
            int batchNumber, int batchSize, Map<String, List<String>> headers) {
        if (response != null && response.getResponse().isPresent()) {
            String requestId = extractRequestId(headers);
            List<V1FlowTokenizeResponseObject> records = response.getResponse().get();
            List<TokenizeSuccess> successRecords = new ArrayList<>();
            List<ErrorRecord> errorRecords = new ArrayList<>();
            int indexNumber = batchNumber * batchSize;
            for (V1FlowTokenizeResponseObject record : records) {
                Object value = record.getValue().orElse(null);
                TokenizeSuccess successEntry = null;
                if (record.getTokens().isPresent()) {
                    for (FlowTokenizeResponseObjectToken tokenObj : record.getTokens().get()) {
                        if (tokenObj.getError().isPresent()) {
                            errorRecords.add(new ErrorRecord(indexNumber, tokenObj.getError().get(), tokenObj.getHttpCode().orElse(500), requestId));
                        } else if (tokenObj.getTokenGroupName().isPresent() && tokenObj.getToken().isPresent()) {
                            if (successEntry == null) {
                                successEntry = new TokenizeSuccess(indexNumber, value);
                            }
                            successEntry.addToken(tokenObj.getTokenGroupName().get(), tokenObj.getToken().get());
                        }
                    }
                }
                if (successEntry != null) {
                    successRecords.add(successEntry);
                }
                indexNumber++;
            }
            return new BulkTokenizeResponse(successRecords, errorRecords);
        }
        return null;
    }
}
