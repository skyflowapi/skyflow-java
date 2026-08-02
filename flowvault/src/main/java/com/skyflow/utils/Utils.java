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
import com.skyflow.generated.rest.core.ObjectMappers;
import com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest;
import com.skyflow.generated.rest.resources.flowservice.requests.V1GetRequest;
import com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest;
import com.skyflow.generated.rest.resources.records.requests.V1ExecuteQueryRequest;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

    private static String extractRequestId(Map<String, List<String>> headers) {
        if (headers == null) return null;
        List<String> ids = headers.get(BaseConstants.REQUEST_ID_HEADER_KEY);
        return (ids == null || ids.isEmpty()) ? null : ids.get(0);
    }

    /**
     * A record counts as failed only when the API returned a non-empty error message together with
     * a non-2xx status, mirroring the check the bulk path has always used.
     */
    private static boolean isFailedRecord(V1DeleteTokenResponseObject record) {
        return record.getError().isPresent()
                && record.getError().get() != null
                && !record.getError().get().isEmpty()
                && record.getHttpCode().orElse(200) != 200;
    }

    /** Maps one SDK request record to the wire object, carrying the BYOT token when supplied. */
    private static V1FlowTokenizeRequestObject buildTokenizeRequestObject(TokenizeRequestRecord record) {
        V1FlowTokenizeRequestObject.Builder builder = V1FlowTokenizeRequestObject.builder()
                .value(record.getValue())
                .tokenGroupNames(record.getTokenGroupNames());
        if (record.getToken() != null) {
            builder = builder.token(record.getToken());
        }
        return builder.build();
    }

    /** Converts one wire record into the unified success/error record shape. */
    private static List<TokenizeResponseToken> buildTokenizeResponseTokens(V1FlowTokenizeResponseObject record) {
        List<TokenizeResponseToken> tokens = new ArrayList<>();
        if (record.getTokens().isPresent()) {
            for (FlowTokenizeResponseObjectToken tokenObj : record.getTokens().get()) {
                boolean failed = tokenObj.getError().isPresent()
                        && tokenObj.getError().get() != null
                        && !tokenObj.getError().get().isEmpty();
                tokens.add(new TokenizeResponseToken(
                        tokenObj.getTokenGroupName().orElse(null),
                        tokenObj.getToken().orElse(null),
                        tokenObj.getHttpCode().orElse(failed ? 500 : 200),
                        failed ? tokenObj.getError().get() : null
                ));
            }
        } else {
            // the API reports one flat row per (value, token group) instead of a nested tokens
            // array; the generated type has no fields for those, so they land in additionalProperties
            TokenizeResponseToken flat = flatToken(record);
            if (flat != null) {
                tokens.add(flat);
            }
        }
        return tokens;
    }

    /**
     * Reads a flat {@code tokenGroupName}/{@code token}/{@code error}/{@code httpCode} row out of
     * the wire object's unmodelled properties. Returns null when the row carries none of them, so a
     * genuinely token-less record still reports an empty list rather than a phantom entry.
     */
    private static TokenizeResponseToken flatToken(V1FlowTokenizeResponseObject record) {
        Map<String, Object> extras = record.getAdditionalProperties();
        if (extras == null || extras.isEmpty()) {
            return null;
        }
        boolean carriesTokenFields = extras.containsKey("token")
                || extras.containsKey("tokenGroupName")
                || extras.containsKey("error")
                || extras.containsKey("httpCode");
        if (!carriesTokenFields) {
            return null;
        }
        String error = asNonEmptyString(extras.get("error"));
        String token = asNonEmptyString(extras.get("token"));
        Integer httpCode = extras.get("httpCode") instanceof Number
                ? ((Number) extras.get("httpCode")).intValue()
                : (error != null ? 500 : 200);
        return new TokenizeResponseToken(
                asNonEmptyString(extras.get("tokenGroupName")), token, httpCode, error);
    }

    /** The API sends "" for a token or error that does not apply; normalise both to null. */
    private static String asNonEmptyString(Object value) {
        if (!(value instanceof String)) {
            return null;
        }
        String text = (String) value;
        return text.isEmpty() ? null : text;
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

    public static com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest getBulkTokenizeRequestBody(
            List<BulkTokenizeRequestRecord> records, String vaultId) {
        List<V1FlowTokenizeRequestObject> dataList = new ArrayList<>();
        for (BulkTokenizeRequestRecord record : records) {
            dataList.add(buildTokenizeRequestObject(record));
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

    /**
     * Splits the caller's records into batches, in order and without gaps.
     *
     * <p>A batch is closed early when the next record repeats a value already in it. The response
     * carries no record identifier, so {@link #formatBulkTokenizeResponse} tells one record's rows
     * from the next by watching the value change — which only works while values are distinct within
     * a request. Batches stay contiguous, so a batch's records still occupy consecutive positions in
     * the caller's list and their indexes follow from the batch's start.
     */
    public static List<List<BulkTokenizeRequestRecord>> createBulkTokenizeBatches(
            List<BulkTokenizeRequestRecord> records, int batchSize) {
        List<List<BulkTokenizeRequestRecord>> batches = new ArrayList<>();
        if (records == null || records.isEmpty()) return batches;
        List<BulkTokenizeRequestRecord> current = new ArrayList<>();
        Set<Object> valuesInBatch = new HashSet<>();
        Set<String> valueKeysInBatch = new HashSet<>();
        for (BulkTokenizeRequestRecord record : records) {
            Object value = record == null ? null : record.getValue();
            String valueKey = String.valueOf(value);
            boolean repeatsValue = valuesInBatch.contains(value) || valueKeysInBatch.contains(valueKey);
            if (!current.isEmpty() && (current.size() >= batchSize || repeatsValue)) {
                batches.add(current);
                current = new ArrayList<>();
                valuesInBatch = new HashSet<>();
                valueKeysInBatch = new HashSet<>();
            }
            current.add(record);
            valuesInBatch.add(value);
            valueKeysInBatch.add(valueKey);
        }
        batches.add(current);
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

    public static List<BulkDeleteTokensResponseRecord> handleBulkDeleteTokensBatchException(
            Throwable ex,
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest batch,
            int batchNumber, int batchSize
    ) {
        List<BulkDeleteTokensResponseRecord> errorRecords = new ArrayList<>();
        List<String> batchTokens = (batch != null && batch.getTokens().isPresent())
                ? batch.getTokens().get() : new ArrayList<>();
        int startIndex = batchNumber * batchSize;
        Throwable cause = ex.getCause();
        if (cause instanceof ApiClientApiException) {
            ApiClientApiException apiException = (ApiClientApiException) cause;
            Object rawBody = apiException.body();
            Map<String, Object> responseBody = (rawBody instanceof Map) ? (Map<String, Object>) rawBody : null;
            if (responseBody != null) {
                if (responseBody.containsKey("tokens")) {
                    Object tokensList = responseBody.get("tokens");
                    if (tokensList instanceof List) {
                        List<?> recordsList = (List<?>) tokensList;
                        for (int position = 0; position < recordsList.size(); position++) {
                            Object record = recordsList.get(position);
                            if (record instanceof Map) {
                                Map<String, Object> recordMap = (Map<String, Object>) record;
                                errorRecords.add(createDeleteTokensErrorRecord(
                                        recordMap, startIndex + position, tokenAt(batchTokens, position)));
                            }
                        }
                    }
                } else if (responseBody.containsKey("error")) {
                    Object errField = responseBody.get("error");
                    Map<String, Object> recordMap = (errField instanceof Map) ? (Map<String, Object>) errField : null;
                    String fallbackMsg = (errField instanceof String) ? (String) errField : null;
                    for (int position = 0; position < batchTokens.size(); position++) {
                        errorRecords.add((recordMap != null)
                                ? createDeleteTokensErrorRecord(recordMap, startIndex + position, tokenAt(batchTokens, position))
                                : new BulkDeleteTokensResponseRecord(
                                        startIndex + position, tokenAt(batchTokens, position),
                                        apiException.statusCode(),
                                        fallbackMsg != null ? fallbackMsg : apiException.getMessage()));
                    }
                }
            }
            if (errorRecords.isEmpty()) {
                for (int position = 0; position < batchTokens.size(); position++) {
                    errorRecords.add(new BulkDeleteTokensResponseRecord(
                            startIndex + position, tokenAt(batchTokens, position),
                            apiException.statusCode(), apiException.getMessage()));
                }
            }
        } else {
            for (int position = 0; position < batchTokens.size(); position++) {
                errorRecords.add(new BulkDeleteTokensResponseRecord(
                        startIndex + position, tokenAt(batchTokens, position), 500, ex.getMessage()));
            }
        }
        return errorRecords;
    }

    private static String tokenAt(List<String> tokens, int position) {
        return (tokens != null && position < tokens.size()) ? tokens.get(position) : null;
    }

    private static BulkDeleteTokensResponseRecord createDeleteTokensErrorRecord(
            Map<String, Object> recordMap, int index, String requestedToken) {
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
        Object echoedToken = recordMap.get("value");
        String token = (echoedToken instanceof String) ? (String) echoedToken : requestedToken;
        return new BulkDeleteTokensResponseRecord(index, token, code, message);
    }

    public static List<BulkTokenizeResponseRecord> handleBulkTokenizeBatchException(
            Throwable ex, List<BulkTokenizeRequestRecord> batchRecords, int startIndex) {
        String message;
        int httpCode;
        Throwable cause = ex.getCause();
        if (cause instanceof ApiClientApiException) {
            ApiClientApiException apiException = (ApiClientApiException) cause;
            // a rejected request still describes each record in its body - prefer that detail over
            // the bare status code, and only synthesise entries when there is nothing to read
            List<BulkTokenizeResponseRecord> fromBody =
                    tokenizeRecordsFromErrorBody(apiException, batchRecords, startIndex);
            if (fromBody != null) {
                return fromBody;
            }
            httpCode = apiException.statusCode();
            message = extractBatchErrorMessage(apiException);
        } else {
            httpCode = 500;
            message = ex.getMessage();
        }
        // a batch-level failure fails every token group of every value in that batch
        List<BulkTokenizeResponseRecord> errorRecords = new ArrayList<>();
        if (batchRecords == null) return errorRecords;
        for (int position = 0; position < batchRecords.size(); position++) {
            BulkTokenizeRequestRecord requested = batchRecords.get(position);
            List<TokenizeResponseToken> tokens = new ArrayList<>();
            List<String> groupNames = requested.getTokenGroupNames();
            if (groupNames == null || groupNames.isEmpty()) {
                tokens.add(new TokenizeResponseToken(null, null, httpCode, message));
            } else {
                for (String groupName : groupNames) {
                    tokens.add(new TokenizeResponseToken(groupName, null, httpCode, message));
                }
            }
            errorRecords.add(new BulkTokenizeResponseRecord(
                    startIndex + position, requested.getValue(), tokens));
        }
        return errorRecords;
    }

    /**
     * Rebuilds the response records from a rejected request's body.
     *
     * <p>The API answers a 4xx with the same {@code response} array it would have returned on
     * success, one row per rejected (value, token group) carrying its own message - for example
     * "Invalid request. BYOT token should contain one token group." Reading it keeps the caller's
     * error identical whether the request was rejected outright or reported per record.
     *
     * @return null when the body is absent or in an unfamiliar shape, so the caller can fall back
     *         to summarising the batch by its status code
     */
    private static List<BulkTokenizeResponseRecord> tokenizeRecordsFromErrorBody(
            ApiClientApiException apiException,
            List<BulkTokenizeRequestRecord> batchRecords,
            int startIndex) {
        Object rawBody = apiException.body();
        if (!(rawBody instanceof Map) || !((Map<?, ?>) rawBody).containsKey("response")) {
            return null;
        }
        try {
            V1FlowTokenizeResponse parsed =
                    ObjectMappers.JSON_MAPPER.convertValue(rawBody, V1FlowTokenizeResponse.class);
            if (!parsed.getResponse().isPresent() || parsed.getResponse().get().isEmpty()) {
                return null;
            }
            return groupTokenizeRows(parsed.getResponse().get(), batchRecords, startIndex);
        } catch (RuntimeException ignored) {
            // body did not deserialise into the shape we know; let the caller summarise instead
            return null;
        }
    }

    /** Pulls the most specific message available from a failed batch response body. */
    private static String extractBatchErrorMessage(ApiClientApiException apiException) {
        Object rawBody = apiException.body();
        if (rawBody instanceof Map) {
            Object errField = ((Map<String, Object>) rawBody).get("error");
            if (errField instanceof String) {
                return (String) errField;
            }
            if (errField instanceof Map) {
                Map<String, Object> errMap = (Map<String, Object>) errField;
                Object message = errMap.containsKey("error") ? errMap.get("error") : errMap.get("message");
                if (message instanceof String) {
                    return (String) message;
                }
            }
        }
        return apiException.getMessage();
    }

    private static BulkTokenizeRequestRecord recordAt(List<BulkTokenizeRequestRecord> records, int position) {
        return (records != null && position < records.size()) ? records.get(position) : null;
    }

    public static BulkInsertResponse formatBulkInsertResponse(V1InsertResponse response, int batch, int batchSize, Map<String, List<String>> headers) {
        BulkInsertResponse formattedResponse = null;
        List<Success> successRecords = new ArrayList<>();
        List<ErrorRecord> errorRecords = new ArrayList<>();
        if (response != null && response.getRecords().isPresent()) {
            String requestId = extractRequestId(headers);
            List<V1RecordResponseObject> record = response.getRecords().get();
            int indexNumber = batch * batchSize;
            int recordsSize = record.size();
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
            V1FlowDeleteTokenResponse response,
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest batchRequest,
            int batch, int batchSize, Map<String, List<String>> headers) {
        if (response != null && response.getTokens().isPresent()) {
            List<V1DeleteTokenResponseObject> records = response.getTokens().get();
            List<String> requestedTokens = batchRequest != null && batchRequest.getTokens().isPresent()
                    ? batchRequest.getTokens().get() : null;
            List<BulkDeleteTokensResponseRecord> responseRecords = new ArrayList<>();
            int indexNumber = batch * batchSize;
            for (int position = 0; position < records.size(); position++) {
                V1DeleteTokenResponseObject record = records.get(position);
                boolean failed = isFailedRecord(record);
                // The API echoes the token back on both paths, but fall back to the token we sent at
                // this position so an error record is never missing its token.
                String tokenValue = record.getValue().orElse(
                        requestedTokens != null && position < requestedTokens.size()
                                ? requestedTokens.get(position) : null);
                responseRecords.add(new BulkDeleteTokensResponseRecord(
                        indexNumber,
                        tokenValue,
                        record.getHttpCode().orElse(failed ? 500 : 200),
                        failed ? record.getError().get() : null
                ));
                indexNumber++;
            }
            return new BulkDeleteTokensResponse(responseRecords);
        }
        return null;
    }

    public static BulkTokenizeResponse formatBulkTokenizeResponse(
            V1FlowTokenizeResponse response,
            List<BulkTokenizeRequestRecord> batchRecords,
            int startIndex,
            Map<String, List<String>> headers) {
        if (response != null && response.getResponse().isPresent()) {
            List<V1FlowTokenizeResponseObject> rows = response.getResponse().get();
            return new BulkTokenizeResponse(groupTokenizeRows(rows, batchRecords, startIndex));
        }
        return null;
    }

    /**
     * Folds the response rows back onto the records that produced them.
     *
     * <p>The API emits one row per (value, token group) rather than one per record, and a record
     * rejected outright yields a single row instead of one per group — so row count is not a
     * function of the request. Rows do arrive in request order, though, and each carries its value,
     * which is enough: a row belongs to the record being filled while it matches that record's value
     * and the record has not yet taken as many rows as it asked for token groups. Anything else
     * starts the next record. Batching keeps values distinct within a request (see
     * {@link #createBulkTokenizeBatches}), so the value comparison never straddles two records.
     *
     * <p>A response already grouped one-row-per-record folds through this unchanged, since each row
     * then matches exactly one record before the value moves on.
     */
    private static List<BulkTokenizeResponseRecord> groupTokenizeRows(
            List<V1FlowTokenizeResponseObject> rows,
            List<BulkTokenizeRequestRecord> batchRecords,
            int startIndex) {
        List<BulkTokenizeResponseRecord> responseRecords = new ArrayList<>();
        if (batchRecords == null || batchRecords.isEmpty()) {
            // nothing to correlate against; fall back to one record per row
            for (int position = 0; position < rows.size(); position++) {
                responseRecords.add(new BulkTokenizeResponseRecord(startIndex + position,
                        rows.get(position).getValue().orElse(null),
                        buildTokenizeResponseTokens(rows.get(position))));
            }
            return responseRecords;
        }

        int recordPosition = 0;
        int rowsTakenByRecord = 0;
        List<TokenizeResponseToken> tokens = new ArrayList<>();
        for (V1FlowTokenizeResponseObject row : rows) {
            Object rowValue = row.getValue().orElse(null);
            while (recordPosition < batchRecords.size()
                    && !acceptsRow(batchRecords.get(recordPosition), rowValue, rowsTakenByRecord)) {
                responseRecords.add(new BulkTokenizeResponseRecord(startIndex + recordPosition,
                        batchRecords.get(recordPosition).getValue(), tokens));
                tokens = new ArrayList<>();
                rowsTakenByRecord = 0;
                recordPosition++;
            }
            if (recordPosition >= batchRecords.size()) {
                // more rows than the request can account for; keep them rather than drop them
                responseRecords.add(new BulkTokenizeResponseRecord(startIndex + recordPosition,
                        rowValue, buildTokenizeResponseTokens(row)));
                recordPosition++;
                continue;
            }
            tokens.addAll(buildTokenizeResponseTokens(row));
            rowsTakenByRecord++;
        }
        // close the record in flight, then any records the response never mentioned
        while (recordPosition < batchRecords.size()) {
            responseRecords.add(new BulkTokenizeResponseRecord(startIndex + recordPosition,
                    batchRecords.get(recordPosition).getValue(), tokens));
            tokens = new ArrayList<>();
            recordPosition++;
        }
        return responseRecords;
    }

    /** A record takes a row while the value still matches and it has room for another token group. */
    private static boolean acceptsRow(BulkTokenizeRequestRecord record, Object rowValue, int rowsTaken) {
        List<String> groups = record.getTokenGroupNames();
        int capacity = (groups == null || groups.isEmpty()) ? 1 : groups.size();
        if (rowsTaken >= capacity) {
            return false;
        }
        // the API echoes the value back; a row that omits it can only belong to the record in flight
        return rowValue == null || valuesMatch(record.getValue(), rowValue);
    }

    /**
     * Compares a requested value with the one echoed back. JSON round-tripping turns numbers into
     * Double/Integer and objects into Maps, so fall back to string form when equals() disagrees.
     */
    private static boolean valuesMatch(Object requested, Object echoed) {
        if (Objects.equals(requested, echoed)) {
            return true;
        }
        if (requested == null || echoed == null) {
            return false;
        }
        return String.valueOf(requested).equals(String.valueOf(echoed));
    }

    public static V1ExecuteQueryRequest getQueryRequestBody(QueryRequest request, String vaultId) {
        return V1ExecuteQueryRequest.builder()
                .vaultId(vaultId)
                .query(request.getQuery())
                .build();
    }

    public static QueryResponse buildQueryResponse(V1ExecuteQueryResponse res) {
        ArrayList<HashMap<String, Object>> fields = new ArrayList<>();
        if (res != null && res.getRecords().isPresent()) {
            for (V1ExecuteQueryRecordResponse record : res.getRecords().get()) {
                HashMap<String, Object> fieldMap = new HashMap<>();
                if (record.getData().isPresent()) {
                    fieldMap.putAll(record.getData().get());
                }
                fields.add(fieldMap);
            }
        }
        return new QueryResponse(fields);
    }

    private static List<V1ColumnRedactions> buildColumnRedactions(List<ColumnRedaction> columnRedactions) {
        List<V1ColumnRedactions> columnRedactionsList = new ArrayList<>();
        for (ColumnRedaction columnRedaction : columnRedactions) {
            columnRedactionsList.add(V1ColumnRedactions.builder()
                    .columnName(columnRedaction.getColumnName())
                    .redaction(columnRedaction.getRedaction())
                    .build());
        }
        return columnRedactionsList;
    }

    private static List<V1UniqueValue> buildUniqueValues(List<Map<String, Object>> uniqueValues) {
        List<V1UniqueValue> uniqueValuesList = new ArrayList<>();
        for (Map<String, Object> uniqueValue : uniqueValues) {
            uniqueValuesList.add(V1UniqueValue.builder().data(uniqueValue).build());
        }
        return uniqueValuesList;
    }

    public static V1GetRequest getGetRequestBody(GetRequest request, String vaultId) {
        V1GetRequest.Builder builder = V1GetRequest.builder().vaultId(vaultId);

        if (request.getRecords() != null && !request.getRecords().isEmpty()) {
            List<V1GetRequestData> recordsList = new ArrayList<>();
            for (GetRecordRequest record : request.getRecords()) {
                V1GetRequestData.Builder recordBuilder = V1GetRequestData.builder()
                        .tableName(record.getTable());
                if (record.getIds() != null) {
                    recordBuilder.skyflowIDs(record.getIds());
                }
                if (record.getFields() != null) {
                    recordBuilder.columns(record.getFields());
                }
                if (record.getColumnRedactions() != null && !record.getColumnRedactions().isEmpty()) {
                    recordBuilder.columnRedactions(buildColumnRedactions(record.getColumnRedactions()));
                }
                if (record.getUniqueValues() != null && !record.getUniqueValues().isEmpty()) {
                    recordBuilder.uniqueValues(buildUniqueValues(record.getUniqueValues()));
                }
                recordsList.add(recordBuilder.build());
            }
            builder.records(recordsList);
        } else {
            builder.tableName(request.getTable());
            if (request.getIds() != null) {
                builder.skyflowIDs(request.getIds());
            }
            if (request.getFields() != null) {
                builder.columns(request.getFields());
            }
            if (request.getColumnRedactions() != null && !request.getColumnRedactions().isEmpty()) {
                builder.columnRedactions(buildColumnRedactions(request.getColumnRedactions()));
            }
            if (request.getUniqueValues() != null && !request.getUniqueValues().isEmpty()) {
                builder.uniqueValues(buildUniqueValues(request.getUniqueValues()));
            }
        }

        if (request.getLimit() != null) {
            builder.limit(request.getLimit());
        }
        if (request.getOffset() != null) {
            builder.offset(request.getOffset());
        }
        return builder.build();
    }

    public static GetResponse buildGetResponse(V1GetResponse res) {
        ArrayList<HashMap<String, Object>> data = new ArrayList<>();
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
                    HashMap<String, Object> getRecord = new HashMap<>();
                    if (record.getData().isPresent()) {
                        getRecord.putAll(record.getData().get());
                    }
                    record.getSkyflowId().ifPresent(skyflowId -> getRecord.put("skyflowId", skyflowId));
                    record.getTableName().ifPresent(tableName -> getRecord.put("tableName", tableName));
                    if (record.getTokens().isPresent()) {
                        getRecord.putAll(record.getTokens().get());
                    }
                    data.add(getRecord);
                }
            }
        }
        return new GetResponse(data, errors);
    }
}
