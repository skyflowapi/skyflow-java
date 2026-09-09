package com.skyflow.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.gson.JsonObject;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.UpdateType;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.core.ObjectMappers;
import com.skyflow.generated.rest.resources.flowservice.requests.V1DeleteRequest;
import com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest;
import com.skyflow.generated.rest.resources.flowservice.requests.V1GetRequest;
import com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest;
import com.skyflow.generated.rest.resources.flowservice.requests.V1UpdateRequest;
import com.skyflow.generated.rest.resources.records.requests.V1ExecuteQueryRequest;
import com.skyflow.generated.rest.types.FlowEnumUpdateType;
import com.skyflow.generated.rest.types.V1ColumnRedactions;
import com.skyflow.generated.rest.types.V1DeleteResponse;
import com.skyflow.generated.rest.types.V1DeleteResponseObject;
import com.skyflow.generated.rest.types.V1DeleteTokenResponseObject;
import com.skyflow.generated.rest.types.V1ExecuteQueryRecordResponse;
import com.skyflow.generated.rest.types.V1ExecuteQueryResponse;
import com.skyflow.generated.rest.types.V1FlowDeleteTokenResponse;
import com.skyflow.generated.rest.types.V1FlowDetokenizeResponse;
import com.skyflow.generated.rest.types.V1FlowDetokenizeResponseObject;
import com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject;
import com.skyflow.generated.rest.types.V1FlowTokenizeResponse;
import com.skyflow.generated.rest.types.V1FlowTokenizeResponseObject;
import com.skyflow.generated.rest.types.V1GetRequestData;
import com.skyflow.generated.rest.types.V1GetResponse;
import com.skyflow.generated.rest.types.V1InsertRecordData;
import com.skyflow.generated.rest.types.V1InsertResponse;
import com.skyflow.generated.rest.types.V1RecordResponseObject;
import com.skyflow.generated.rest.types.V1TokenGroupRedactions;
import com.skyflow.generated.rest.types.V1UniqueValue;
import com.skyflow.generated.rest.types.V1UpdateRecordData;
import com.skyflow.generated.rest.types.V1UpdateResponse;
import com.skyflow.generated.rest.types.V1Upsert;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.vault.data.BulkDeleteTokensRequest;
import com.skyflow.vault.data.BulkDeleteTokensResponse;
import com.skyflow.vault.data.BulkDeleteTokensResponseRecord;
import com.skyflow.vault.data.BulkDetokenizeRequest;
import com.skyflow.vault.data.BulkDetokenizeResponse;
import com.skyflow.vault.data.BulkDetokenizeResponseRecord;
import com.skyflow.vault.data.BulkInsertRequest;
import com.skyflow.vault.data.BulkInsertResponse;
import com.skyflow.vault.data.BulkInsertResponseRecord;
import com.skyflow.vault.data.BulkTokenizeRequestRecord;
import com.skyflow.vault.data.BulkTokenizeResponse;
import com.skyflow.vault.data.BulkTokenizeResponseRecord;
import com.skyflow.vault.data.ColumnRedactions;
import com.skyflow.vault.data.DeleteRequest;
import com.skyflow.vault.data.DeleteResponse;
import com.skyflow.vault.data.DeleteResponseRecord;
import com.skyflow.vault.data.DetokenizeResponseRecordMetadata;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.DetokenizeResponse;
import com.skyflow.vault.data.DetokenizeResponseRecord;
import com.skyflow.vault.data.ErrorRecord;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetRequestRecord;
import com.skyflow.vault.data.GetResponse;
import com.skyflow.vault.data.GetResponseRecord;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertRequestRecord;
import com.skyflow.vault.data.InsertResponse;
import com.skyflow.vault.data.InsertResponseRecord;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.QueryResponse;
import com.skyflow.vault.data.QueryResponseMetadata;
import com.skyflow.vault.data.QueryResponseRecord;
import com.skyflow.vault.data.Token;
import com.skyflow.vault.data.TokenGroupRedactions;
import com.skyflow.vault.data.TokenizeRequestRecord;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateRequestRecord;
import com.skyflow.vault.data.UpdateResponse;
import com.skyflow.vault.data.UpdateResponseRecord;
import com.skyflow.vault.data.UpsertOptions;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

public final class Utils extends BaseUtils {

    // Spellings the vault has used for the per-record status, in precedence order.
    private static final String[] HTTP_CODE_KEYS = {"http_code", "httpCode", "statusCode"};

    public static String getVaultUrl(String clusterId, Env env) {
        // The 3-arg overload is inherited from common's BaseUtils, which keeps the older
        // getVaultURL spelling (shared with v2), so it is qualified rather than renamed here.
        return BaseUtils.getVaultURL(clusterId, env, Constants.VAULT_DOMAIN);
    }

    public static JsonObject getMetrics() {
        JsonObject details = getCommonMetrics();
        String sdkVersion = Constants.SDK_VERSION;
        details.addProperty(Constants.SDK_METRIC_NAME_VERSION, Constants.SDK_METRIC_NAME_VERSION_PREFIX + sdkVersion);
        return details;
    }

    public static String getEnvVaultUrl() throws SkyflowException {
        try {
            String vaultUrl = System.getenv("VAULT_URL");
            if (vaultUrl == null) {
                Dotenv dotenv = Dotenv.load();
                vaultUrl = dotenv.get("VAULT_URL");
            }
            if (vaultUrl != null && vaultUrl.trim().isEmpty()) {
                LogUtil.printErrorLog(ErrorLogs.EMPTY_VAULT_URL.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyVaultUrl.getMessage());
            } else if (vaultUrl != null && !isValidUrl(vaultUrl)) {
                LogUtil.printErrorLog(ErrorLogs.INVALID_VAULT_URL_FORMAT.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidVaultUrlFormat.getMessage());
            }
            return vaultUrl;
        } catch (DotenvException e) {
            return null;
        }
    }

    public static boolean isValidUrl(String url) {
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

    // Mirrors the "present" test used by the request validators: null and blank both count as absent.
    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static V1Upsert toV1Upsert(UpsertOptions upsert) {
        V1Upsert.Builder builder = V1Upsert.builder().uniqueColumns(upsert.getUniqueColumns());
        // updateType is a String on the request; the legal values come from the wire enum itself
        // so there is a single source of truth. Validations rejects anything that does not match.
        String updateType = upsert.getUpdateType();
        for (FlowEnumUpdateType type : FlowEnumUpdateType.values()) {
            if (type.toString().equalsIgnoreCase(updateType)) {
                builder.updateType(type);
                break;
            }
        }
        return builder.build();
    }

    public static V1InsertRequest getInsertRequestBody(InsertRequest request, VaultConfig config) {
        List<InsertRequestRecord> records = request.getRecords();
        List<V1InsertRecordData> insertRecordDataList = new ArrayList<>();
        // tableName and upsert must reach the wire at exactly one level: the vault rejects a body
        // that carries either at both the request and the record level. validateTableAndUpsertPlacement
        // has already forced the caller to pick one, so mirror that choice here rather than copying
        // the request-level value down onto every record.
        for (InsertRequestRecord record : records) {
            V1InsertRecordData.Builder data = V1InsertRecordData.builder()
                    .data(record.getData());
            // A blank record-level table name counts as absent, matching validateInsertRequest.
            if (hasText(record.getTableName())) {
                data.tableName(record.getTableName());
            }
            if (record.getTokens() != null && !record.getTokens().isEmpty()) {
                data.tokens(record.getTokens());
            }
            UpsertOptions recordUpsert = record.getUpsert();
            if (recordUpsert != null && recordUpsert.getUniqueColumns() != null
                    && !recordUpsert.getUniqueColumns().isEmpty()) {
                data.upsert(toV1Upsert(recordUpsert));
            }
            insertRecordDataList.add(data.build());
        }

        V1InsertRequest.Builder builder = V1InsertRequest.builder()
                .vaultId(config.getVaultId())
                .records(insertRecordDataList);
        if (hasText(request.getTableName())) {
            builder.tableName(request.getTableName());
        }
        UpsertOptions requestUpsert = request.getUpsert();
        if (requestUpsert != null && requestUpsert.getUniqueColumns() != null
                && !requestUpsert.getUniqueColumns().isEmpty()) {
            builder.upsert(toV1Upsert(requestUpsert));
        }
        return builder.build();
    }

    private static String extractRequestId(Map<String, List<String>> headers) {
        if (headers == null) return null;
        List<String> ids = headers.get(BaseConstants.REQUEST_ID_HEADER_KEY);
        return (ids == null || ids.isEmpty()) ? null : ids.get(0);
    }

    public static V1GetRequest getGetRequestBody(GetRequest request, VaultConfig config) {
        V1GetRequest.Builder builder = V1GetRequest.builder().vaultId(config.getVaultId());

        // Multi-table mode: Validations has already rejected the case where both this and the
        // single-table fields below are set, so their presence/absence is mutually exclusive.
        if (request.getRecords() != null && !request.getRecords().isEmpty()) {
            List<V1GetRequestData> recordDataList = new ArrayList<>();
            for (GetRequestRecord record : request.getRecords()) {
                recordDataList.add(toV1GetRequestData(record));
            }
            return builder.records(recordDataList).build();
        }

        if (hasText(request.getTable())) {
            builder.tableName(request.getTable());
        }
        if (request.getIds() != null && !request.getIds().isEmpty()) {
            builder.skyflowIDs(request.getIds());
        }
        if (request.getFields() != null && !request.getFields().isEmpty()) {
            builder.columns(request.getFields());
        }
        if (request.getColumnRedactions() != null && !request.getColumnRedactions().isEmpty()) {
            builder.columnRedactions(toV1ColumnRedactionsList(request.getColumnRedactions()));
        }
        if (request.getUniqueValues() != null && !request.getUniqueValues().isEmpty()) {
            builder.uniqueValues(toV1UniqueValueList(request.getUniqueValues()));
        }
        if (request.getLimit() != null) {
            builder.limit(request.getLimit());
        }
        if (request.getOffset() != null) {
            builder.offset(request.getOffset());
        }
        return builder.build();
    }

    private static V1GetRequestData toV1GetRequestData(GetRequestRecord record) {
        V1GetRequestData.Builder data = V1GetRequestData.builder().tableName(record.getTable());
        if (record.getIds() != null && !record.getIds().isEmpty()) {
            data.skyflowIDs(record.getIds());
        }
        if (record.getFields() != null && !record.getFields().isEmpty()) {
            data.columns(record.getFields());
        }
        if (record.getColumnRedactions() != null && !record.getColumnRedactions().isEmpty()) {
            data.columnRedactions(toV1ColumnRedactionsList(record.getColumnRedactions()));
        }
        if (record.getUniqueValues() != null && !record.getUniqueValues().isEmpty()) {
            data.uniqueValues(toV1UniqueValueList(record.getUniqueValues()));
        }
        return data.build();
    }

    private static List<V1ColumnRedactions> toV1ColumnRedactionsList(List<ColumnRedactions> columnRedactions) {
        List<V1ColumnRedactions> list = new ArrayList<>();
        for (ColumnRedactions columnRedaction : columnRedactions) {
            list.add(V1ColumnRedactions.builder()
                    .columnName(columnRedaction.getColumnName())
                    .redaction(columnRedaction.getRedaction())
                    .build());
        }
        return list;
    }

    private static List<V1UniqueValue> toV1UniqueValueList(List<Map<String, Object>> uniqueValues) {
        List<V1UniqueValue> list = new ArrayList<>();
        for (Map<String, Object> uniqueValue : uniqueValues) {
            list.add(V1UniqueValue.builder().data(uniqueValue).build());
        }
        return list;
    }

    public static V1DeleteRequest getDeleteRequestBody(DeleteRequest request, VaultConfig config) {
        V1DeleteRequest.Builder builder = V1DeleteRequest.builder()
                .vaultId(config.getVaultId())
                .tableName(request.getTable());
        if (request.getIds() != null && !request.getIds().isEmpty()) {
            builder.skyflowIDs(request.getIds());
        }
        if (request.getUniqueValues() != null && !request.getUniqueValues().isEmpty()) {
            builder.uniqueValues(toV1UniqueValueList(request.getUniqueValues()));
        }
        return builder.build();
    }

    public static V1UpdateRequest getUpdateRequestBody(UpdateRequest request, VaultConfig config) {
        List<V1UpdateRecordData> updateRecordDataList = new ArrayList<>();
        for (UpdateRequestRecord record : request.getRecords()) {
            V1UpdateRecordData.Builder data = V1UpdateRecordData.builder()
                    .skyflowId(record.getSkyflowId())
                    .data(record.getData());
            if (record.getTokens() != null && !record.getTokens().isEmpty()) {
                data.tokens(record.getTokens());
            }
            // A blank record-level table name counts as absent, matching validateInsertRequest's
            // handling of the same table/record split.
            if (hasText(record.getTableName())) {
                data.tableName(record.getTableName());
            }
            updateRecordDataList.add(data.build());
        }

        V1UpdateRequest.Builder builder = V1UpdateRequest.builder()
                .vaultId(config.getVaultId())
                .tableName(request.getTableName())
                .records(updateRecordDataList);

        // Record-level updateType isn't wired here: the generated V1UpdateRecordData has no
        // updateType setter yet even though the proto declares one (needs a client regeneration).
        UpdateType requestUpdateType = request.getUpdateType();
        if (requestUpdateType != null) {
            builder.updateType(FlowEnumUpdateType.valueOf(requestUpdateType.name()));
        }
        return builder.build();
    }

    // ── Bulk (batched/concurrent) request-body builders ──────────────────────

    // BulkInsertRequest is an InsertRequest, so the bulk body is built exactly the same way.
    public static com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest getBulkInsertRequestBody(BulkInsertRequest request, VaultConfig config) {
        return getInsertRequestBody(request, config);
    }

    public static V1FlowDetokenizeRequest getDetokenizeRequestBody(DetokenizeRequest request, String vaultId) {
        V1FlowDetokenizeRequest.Builder builder = V1FlowDetokenizeRequest.builder()
                .vaultId(vaultId)
                .tokens(request.getTokens());
        if (request.getTokenGroupRedactions() != null && !request.getTokenGroupRedactions().isEmpty()) {
            List<V1TokenGroupRedactions> tokenGroupRedactionsList = new ArrayList<>();
            for (TokenGroupRedactions tokenGroupRedactions : request.getTokenGroupRedactions()) {
                tokenGroupRedactionsList.add(V1TokenGroupRedactions.builder()
                        .tokenGroupName(tokenGroupRedactions.getTokenGroupName())
                        .redaction(tokenGroupRedactions.getRedaction())
                        .build());
            }
            builder.tokenGroupRedactions(tokenGroupRedactionsList);
        }
        return builder.build();
    }

    public static V1FlowDetokenizeRequest getBulkDetokenizeRequestBody(BulkDetokenizeRequest request, String vaultId) {
        return getDetokenizeRequestBody(request, vaultId);
    }

    public static V1ExecuteQueryRequest getQueryRequestBody(QueryRequest request, String vaultId) {
        return V1ExecuteQueryRequest.builder()
                .vaultId(vaultId)
                .query(request.getQuery())
                .build();
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
    // Error bodies routinely carry a key whose value is explicitly null (e.g. "skyflowID": null on a
    // failed record), so containsKey() is not enough to know a value is usable — read through these
    // helpers. Anything that throws here masks the real server error with a parsing crash.

    /** Value as a String, or null when the key is absent or explicitly null. */
    private static String readString(Map<String, Object> recordMap, String key) {
        Object value = recordMap.get(key);
        return value == null ? null : value.toString();
    }

    /** First usable HTTP status among the known spellings, else {@code fallback}. */
    private static int readHttpCode(Map<String, Object> recordMap, int fallback) {
        for (String key : HTTP_CODE_KEYS) {
            Object value = recordMap.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            if (value instanceof String) {
                try {
                    return Integer.parseInt(((String) value).trim());
                } catch (NumberFormatException ignored) {
                    // fall through to the next spelling
                }
            }
        }
        return fallback;
    }

    /**
     * Error text from "error", else "message", else a placeholder. Never null: a null message on an
     * error record would make it read as a success downstream, since that is how failures are counted.
     */
    private static String readErrorMessage(Map<String, Object> recordMap) {
        String error = readString(recordMap, "error");
        if (error != null) {
            return error;
        }
        String message = readString(recordMap, "message");
        return message != null ? message : "Unknown error";
    }

    public static BulkInsertResponseRecord createInsertErrorRecord(Map<String, Object> recordMap, int indexNumber, String requestId) {
        BulkInsertResponseRecord err = null;
        if (recordMap != null) {
            int code = readHttpCode(recordMap, 500);
            String skyflowID = readString(recordMap, "skyflowID");
            String tableName = readString(recordMap, "tableName");
            String message = readErrorMessage(recordMap);
            err = new BulkInsertResponseRecord(indexNumber, tableName, skyflowID, null, null, null, code, message, requestId);
        }
        return err;
    }

    public static BulkDetokenizeResponseRecord createDetokenizeErrorRecord(Map<String, Object> recordMap, int indexNumber, String requestId) {
        BulkDetokenizeResponseRecord err = null;
        if (recordMap != null) {
            int code = readHttpCode(recordMap, 500);
            // the failing token is echoed back so the caller can tell which one it was
            String token = readString(recordMap, "token");
            String tokenGroupName = readString(recordMap, "tokenGroupName");
            String message = readErrorMessage(recordMap);
            err = new BulkDetokenizeResponseRecord(indexNumber, token, null, tokenGroupName, null, code, message, requestId);
        }
        return err;
    }

    public static ErrorRecord createErrorRecord(Map<String, Object> recordMap, int indexNumber, String requestId) {
        ErrorRecord err = null;
        if (recordMap != null) {
            int code = readHttpCode(recordMap, 500);
            String message = readErrorMessage(recordMap);
            err = new ErrorRecord(indexNumber, message, code, requestId);
        }
        return err;
    }

    // Errors are parsed into ErrorRecord (shared with the other bulk ops), then projected onto
    // the unified BulkInsertResponseRecord shape that bulk insert now returns.
    public static List<BulkInsertResponseRecord> handleBulkInsertBatchException(
            Throwable ex, List<V1InsertRecordData> batch, int batchNumber, int batchSize
    ) {
        List<BulkInsertResponseRecord> allRecords = new ArrayList<>();
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
                                BulkInsertResponseRecord err = createInsertErrorRecord(recordMap, indexNumber, requestId);
                                allRecords.add(err);
                                indexNumber++;
                            }
                        }
                    }
                } else if (responseBody.containsKey("error")) {
                    Object errField = responseBody.get("error");
                    Map<String, Object> recordMap = (errField instanceof Map) ? (Map<String, Object>) errField : null;
                    String fallbackMsg = (errField instanceof String) ? (String) errField : null;
                    for (int j = 0; j < batch.size(); j++) {
                        BulkInsertResponseRecord err = null;
                        if(recordMap != null){
                            err = createInsertErrorRecord(recordMap, indexNumber, requestId);
                        } else {
                            String errorMessage = null;
                            if (fallbackMsg != null){
                                errorMessage = fallbackMsg;
                            } else {
                                errorMessage = apiException.getMessage();
                            }
                            err = new BulkInsertResponseRecord(indexNumber, null, null, null, null, null, apiException.statusCode(), errorMessage, requestId);

                        }
                        allRecords.add(err);
                        indexNumber++;
                    }
                }
            }

            if (allRecords.isEmpty()) {
                for (int j = 0; j < batch.size(); j++) {
                    allRecords.add(new BulkInsertResponseRecord(indexNumber, null, null, null, null, null, apiException.statusCode(), apiException.getMessage(), requestId));
                    indexNumber++;
                }
            }
        } else {
            int indexNumber = batchNumber > 0 ? batchNumber * batchSize : 0;
            for (int j = 0; j < batch.size(); j++) {
                String message = null;
                if (cause != null && cause.getMessage() != null){
                    message = cause.getMessage();
                }
                if (cause != null && cause.getLocalizedMessage() !=null) {
                    message = cause.getLocalizedMessage();
                }
                if (cause != null && cause.getCause() !=null) {
                    message = cause.getCause().toString();
                }
                if (message == null || message.isEmpty() || message.trim().isEmpty()){
                    message = ex.getMessage();
                }
                BulkInsertResponseRecord err = new BulkInsertResponseRecord(indexNumber, null, null, null, null, null, 500, message, null);
                allRecords.add(err);
                indexNumber++;
            }
        }
        return allRecords;
    }

    // Errors are parsed into ErrorRecord (shared with the other bulk ops), then projected onto
    // the unified BulkDetokenizeResponseRecord shape that bulk detokenize now returns.
    public static List<BulkDetokenizeResponseRecord> handleBulkDetokenizeBatchException(
            Throwable ex, V1FlowDetokenizeRequest batch, int batchNumber, int batchSize
    ) {
        List<BulkDetokenizeResponseRecord> allRecords = new ArrayList<>();
        Throwable cause = ex.getCause();
        if (cause instanceof ApiClientApiException) {
            ApiClientApiException apiException = (ApiClientApiException) cause;
            String requestId = extractRequestId(apiException.headers());
            Object rawBody = apiException.body();
            Map<String, Object> responseBody = (rawBody instanceof Map) ? (Map<String, Object>) rawBody : null;
            int indexNumber = batchNumber * batchSize;
            int tokenCount = batch.getTokens().isPresent() ? batch.getTokens().get().size() : 0;
            if (responseBody != null) {
                if (responseBody.containsKey("response")) {
                    Object recordss = responseBody.get("response");
                    if (recordss instanceof List) {
                        List<?> recordsList = (List<?>) recordss;
                        for (Object record : recordsList) {
                            if (record instanceof Map) {
                                Map<String, Object> recordMap = (Map<String, Object>) record;
                                BulkDetokenizeResponseRecord err = createDetokenizeErrorRecord(recordMap, indexNumber, requestId);
                                allRecords.add(err);
                                indexNumber++;
                            }
                        }
                    }
                } else if (responseBody.containsKey("error")) {
                    Object errField = responseBody.get("error");
                    Map<String, Object> recordMap = (errField instanceof Map) ? (Map<String, Object>) errField : null;
                    String fallbackMsg = (errField instanceof String) ? (String) errField : null;
                    for (int j = 0; j < tokenCount; j++) {
                        BulkDetokenizeResponseRecord err = null;
                        if (recordMap != null) {
                            err = createDetokenizeErrorRecord(recordMap, indexNumber, requestId);
                        } else {
                            String errorMessage = null;
                            if (fallbackMsg != null) {
                                errorMessage = fallbackMsg;
                            } else {
                                errorMessage = apiException.getMessage();
                            }
                            err = new BulkDetokenizeResponseRecord(indexNumber, null, null, null, null, apiException.statusCode(), errorMessage, requestId);
                        }
                        allRecords.add(err);
                        indexNumber++;
                    }
                }
            }

            if (allRecords.isEmpty()) {
                for (int j = 0; j < tokenCount; j++) {
                    allRecords.add(new BulkDetokenizeResponseRecord(indexNumber, null, null, null, null, apiException.statusCode(), apiException.getMessage(), requestId));
                    indexNumber++;
                }
            }
        } else {
            int indexNumber = batchNumber * batchSize;
            String message = null;
            if (cause != null && cause.getMessage() != null){
                message = cause.getMessage();
            }
            if (cause != null && cause.getLocalizedMessage() !=null) {
                message = cause.getLocalizedMessage();
            }
            if (cause != null && cause.getCause() !=null) {
                message = cause.getCause().toString();
            }
            if (message == null || message.isEmpty() || message.trim().isEmpty()){
                message = ex.getMessage();
            }
            for (int j = 0; j < batch.getTokens().get().size(); j++) {
                BulkDetokenizeResponseRecord err = new BulkDetokenizeResponseRecord(indexNumber, null, null, null, null, 500, message, null);
                allRecords.add(err);
                indexNumber++;
            }
        }
        return allRecords;
    }

    /**
     * Best available description of a failure that never reached the API.
     *
     * <p>The generated client wraps transport failures as "Network error executing HTTP request",
     * which says nothing about what actually went wrong, and the future wraps that again. Walk down
     * to the innermost cause so the caller sees the real problem - for a mistyped cluster id that is
     * {@code java.net.UnknownHostException: <host>: nodename nor servname provided, or not known}
     * rather than the generic wrapper. Mirrors the order bulk insert and detokenize already use.
     */
    private static String describeTransportFailure(Throwable ex, Throwable cause) {
        String message = null;
        if (cause != null && cause.getMessage() != null) {
            message = cause.getMessage();
        }
        if (cause != null && cause.getLocalizedMessage() != null) {
            message = cause.getLocalizedMessage();
        }
        if (cause != null && cause.getCause() != null) {
            message = cause.getCause().toString();
        }
        if (message == null || message.trim().isEmpty()) {
            message = ex.getMessage();
        }
        return message;
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
            String requestId = extractRequestId(apiException.headers());
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
                                errorRecords.add(createDeleteTokensErrorRecord(recordMap,
                                        startIndex + position, tokenAt(batchTokens, position), requestId));
                            }
                        }
                    }
                } else if (responseBody.containsKey("error")) {
                    Object errField = responseBody.get("error");
                    Map<String, Object> recordMap = (errField instanceof Map) ? (Map<String, Object>) errField : null;
                    String fallbackMsg = (errField instanceof String) ? (String) errField : null;
                    for (int position = 0; position < batchTokens.size(); position++) {
                        errorRecords.add((recordMap != null)
                                ? createDeleteTokensErrorRecord(recordMap, startIndex + position,
                                        tokenAt(batchTokens, position), requestId)
                                : new BulkDeleteTokensResponseRecord(
                                        startIndex + position, tokenAt(batchTokens, position),
                                        apiException.statusCode(),
                                        fallbackMsg != null ? fallbackMsg : apiException.getMessage(),
                                        requestId));
                    }
                }
            }
            if (errorRecords.isEmpty()) {
                for (int position = 0; position < batchTokens.size(); position++) {
                    errorRecords.add(new BulkDeleteTokensResponseRecord(
                            startIndex + position, tokenAt(batchTokens, position),
                            apiException.statusCode(), apiException.getMessage(), requestId));
                }
            }
        } else {
            // a transport-level failure never reached the API, so there is no id to report
            String message = describeTransportFailure(ex, cause);
            for (int position = 0; position < batchTokens.size(); position++) {
                errorRecords.add(new BulkDeleteTokensResponseRecord(
                        startIndex + position, tokenAt(batchTokens, position), 500, message));
            }
        }
        return errorRecords;
    }

    private static String tokenAt(List<String> tokens, int position) {
        return (tokens != null && position < tokens.size()) ? tokens.get(position) : null;
    }

    private static BulkDeleteTokensResponseRecord createDeleteTokensErrorRecord(
            Map<String, Object> recordMap, int index, String requestedToken, String requestId) {
        // Read through the shared helpers rather than casting: recordMap holds deserialised JSON,
        // so a status can arrive as Double or String depending on the parser, and a blind
        // (Integer) cast would turn a real API error into a ClassCastException.
        int code = readHttpCode(recordMap, 500);
        String message = readErrorMessage(recordMap);
        String token = readString(recordMap, "value");
        if (token == null) {
            token = requestedToken;
        }
        return new BulkDeleteTokensResponseRecord(index, token, code, message, requestId);
    }

    public static List<BulkTokenizeResponseRecord> handleBulkTokenizeBatchException(
            Throwable ex, List<BulkTokenizeRequestRecord> batchRecords, int startIndex) {
        String message;
        int httpCode;
        String requestId = null;
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
            requestId = extractRequestId(apiException.headers());
        } else {
            // a transport-level failure never reached the API, so there is no id to report
            httpCode = 500;
            message = describeTransportFailure(ex, cause);
        }
        // a batch-level failure fails every token group of every value in that batch
        List<BulkTokenizeResponseRecord> errorRecords = new ArrayList<>();
        if (batchRecords == null) return errorRecords;
        for (int position = 0; position < batchRecords.size(); position++) {
            BulkTokenizeRequestRecord requested = batchRecords.get(position);
            int index = startIndex + position;
            List<String> groupNames = requested.getTokenGroupNames();
            if (groupNames == null || groupNames.isEmpty()) {
                errorRecords.add(new BulkTokenizeResponseRecord(
                        index, requested.getValue(), null, null, httpCode, message, requestId));
            } else {
                for (String groupName : groupNames) {
                    errorRecords.add(new BulkTokenizeResponseRecord(
                            index, requested.getValue(), groupName, null, httpCode, message, requestId));
                }
            }
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
            return groupTokenizeRows(parsed.getResponse().get(), batchRecords, startIndex,
                    extractRequestId(apiException.headers()));
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

    // Unary counterpart of formatBulkInsertResponse: a single, unbatched call has no batch
    // index to attach, but the call's own requestId is still populated on error records,
    // matching bulk's error != null ? requestId : null convention.
    public static InsertResponse formatInsertResponse(V1InsertResponse response, Map<String, List<String>> headers) {
        List<InsertResponseRecord> records = new ArrayList<>();
        if (response != null && response.getRecords().isPresent()) {
            for (V1RecordResponseObject current : response.getRecords().get()) {
                String reqID = current.getError().isPresent() ? extractRequestId(headers) : null;
                records.add(new InsertResponseRecord(
                        current.getTableName().orElse(null),
                        current.getSkyflowId().orElse(null),
                        Token.parseTokens(current.getTokens().orElse(null)),
                        current.getData().orElse(null),
                        current.getHashedData().orElse(null),
                        current.getHttpCode().orElse(current.getError().isPresent() ? 500 : 200),
                        current.getError().orElse(null),
                        reqID));
            }
        }
        return new InsertResponse(records);
    }

    // Update has no bulk/batched counterpart, so there is no index to attach here, but the call's
    // own requestId is still populated on error records. The wire response reuses
    // V1RecordResponseObject (the same shape as insert's), so the per-record mapping mirrors
    // formatInsertResponse.
    // Get has no bulk/batched counterpart, so there is no index to attach here, but the call's own
    // requestId is still populated on error records. The wire response reuses
    // V1RecordResponseObject (the same shape as insert's/update's), so the per-record mapping
    // mirrors formatInsertResponse.
    public static GetResponse formatGetResponse(V1GetResponse response, Map<String, List<String>> headers) {
        List<GetResponseRecord> records = new ArrayList<>();
        if (response != null && response.getRecords().isPresent()) {
            for (V1RecordResponseObject current : response.getRecords().get()) {
                String reqID = current.getError().isPresent() ? extractRequestId(headers) : null;
                records.add(new GetResponseRecord(
                        current.getTableName().orElse(null),
                        current.getSkyflowId().orElse(null),
                        Token.parseTokens(current.getTokens().orElse(null)),
                        current.getData().orElse(null),
                        current.getHashedData().orElse(null),
                        current.getHttpCode().orElse(current.getError().isPresent() ? 500 : 200),
                        current.getError().orElse(null),
                        reqID));
            }
        }
        return new GetResponse(records);
    }

    // Delete has no bulk/batched counterpart, so there is no index to attach here, but the call's
    // own requestId is still populated on error records. Unlike insert/update/get, the wire
    // response (V1DeleteResponseObject) carries no data/tokens.
    public static DeleteResponse formatDeleteResponse(V1DeleteResponse response, Map<String, List<String>> headers) {
        List<DeleteResponseRecord> records = new ArrayList<>();
        if (response != null && response.getRecords().isPresent()) {
            for (V1DeleteResponseObject current : response.getRecords().get()) {
                String reqID = current.getError().isPresent() ? extractRequestId(headers) : null;
                records.add(new DeleteResponseRecord(
                        current.getSkyflowId().orElse(null),
                        current.getHttpCode().orElse(current.getError().isPresent() ? 500 : 200),
                        current.getError().orElse(null),
                        reqID));
            }
        }
        return new DeleteResponse(records);
    }

    public static UpdateResponse formatUpdateResponse(V1UpdateResponse response, Map<String, List<String>> headers) {
        List<UpdateResponseRecord> records = new ArrayList<>();
        if (response != null && response.getRecords().isPresent()) {
            for (V1RecordResponseObject current : response.getRecords().get()) {
                String reqID = current.getError().isPresent() ? extractRequestId(headers) : null;
                records.add(new UpdateResponseRecord(
                        current.getTableName().orElse(null),
                        current.getSkyflowId().orElse(null),
                        Token.parseTokens(current.getTokens().orElse(null)),
                        current.getData().orElse(null),
                        current.getHashedData().orElse(null),
                        current.getHttpCode().orElse(current.getError().isPresent() ? 500 : 200),
                        current.getError().orElse(null),
                        reqID));
            }
        }
        return new UpdateResponse(records);
    }

    public static BulkInsertResponse formatBulkInsertResponse(V1InsertResponse response, int batch, int batchSize, Map<String, List<String>> headers) {
        BulkInsertResponse formattedResponse = null;
        List<BulkInsertResponseRecord> records = new ArrayList<>();
        if (response != null && response.getRecords().isPresent()) {
            List<V1RecordResponseObject> record = response.getRecords().get();
            int indexNumber = batch * batchSize;
            int recordsSize = record.size();
            for (int index = 0; index < recordsSize; index++) {
                V1RecordResponseObject current = record.get(index);
                String reqID = null;
                if(current.getError().isPresent()){
                    reqID = extractRequestId(headers);
                }
                records.add(new BulkInsertResponseRecord(
                        indexNumber,
                        current.getTableName().orElse(null),
                        current.getSkyflowId().orElse(null),
                        Token.parseTokens(current.getTokens().orElse(null)),
                        current.getData().orElse(null),
                        current.getHashedData().orElse(null),
                        current.getHttpCode().orElse(current.getError().isPresent() ? 500 : 200),
                        current.getError().orElse(null),
                        reqID));
                indexNumber++;
            }
            formattedResponse = new BulkInsertResponse(records);
        }
        return formattedResponse;
    }

    // Unary counterpart of formatBulkDetokenizeResponse: a single, unbatched call has no batch
    // index to attach, but the call's own requestId is still populated on error records,
    // matching bulk's error != null ? requestId : null convention.
    public static DetokenizeResponse formatDetokenizeResponse(V1FlowDetokenizeResponse response, Map<String, List<String>> headers) {
        List<DetokenizeResponseRecord> records = new ArrayList<>();
        if (response != null && response.getResponse().isPresent()) {
            for (V1FlowDetokenizeResponseObject current : response.getResponse().get()) {
                DetokenizeResponseRecordMetadata metadata = DetokenizeResponseRecordMetadata.parseMetadata(current.getMetadata().orElse(null));
                String reqID = current.getError().isPresent() ? extractRequestId(headers) : null;
                records.add(new DetokenizeResponseRecord(
                        current.getToken().orElse(null),
                        current.getValue().orElse(null),
                        current.getTokenGroupName().orElse(null),
                        metadata,
                        current.getHttpCode().orElse(current.getError().isPresent() ? 500 : 200),
                        current.getError().orElse(null),
                        reqID));
            }
        }
        return new DetokenizeResponse(records);
    }

    public static BulkDetokenizeResponse formatBulkDetokenizeResponse(V1FlowDetokenizeResponse response, int batch, int batchSize, Map<String, List<String>> headers) {
        if (response != null && response.getResponse().isPresent()) {
            List<V1FlowDetokenizeResponseObject> record = response.getResponse().get();
            List<BulkDetokenizeResponseRecord> records = new ArrayList<>();
            int indexNumber = batch * batchSize;
            int recordsSize = record.size();
            for (int index = 0; index < recordsSize; index++) {
                V1FlowDetokenizeResponseObject current = record.get(index);
                DetokenizeResponseRecordMetadata metadata = DetokenizeResponseRecordMetadata.parseMetadata(current.getMetadata().orElse(null));
                String reqID = null;
                if(current.getError().isPresent()){
                    reqID = extractRequestId(headers);
                }
                records.add(new BulkDetokenizeResponseRecord(
                        indexNumber,
                        current.getToken().orElse(null),
                        current.getValue().orElse(null),
                        current.getTokenGroupName().orElse(null),
                        metadata,
                        current.getHttpCode().orElse(current.getError().isPresent() ? 500 : 200),
                        current.getError().orElse(null),
                        reqID));
                indexNumber++;
            }
            return new BulkDetokenizeResponse(records);
        }
        return null;
    }

    // Query has no batching/bulk counterpart, so there is no index or requestId to attach here.
    public static QueryResponse formatQueryResponse(V1ExecuteQueryResponse response) {
        List<QueryResponseRecord> records = new ArrayList<>();
        QueryResponseMetadata metadata = null;
        if (response != null) {
            if (response.getRecords().isPresent()) {
                for (V1ExecuteQueryRecordResponse record : response.getRecords().get()) {
                    records.add(new QueryResponseRecord(record.getData().orElse(null)));
                }
            }
            if (response.getMetadata().isPresent()) {
                metadata = new QueryResponseMetadata(response.getMetadata().get().getColumns().orElse(null));
            }
        }
        return new QueryResponse(records, metadata);
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
            // one id per API call, so every error this batch reports carries the same one
            String requestId = extractRequestId(headers);
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
                        failed ? record.getError().get() : null,
                        requestId
                ));
                indexNumber++;
            }
            return new BulkDeleteTokensResponse(responseRecords);
        }
        return null;
    }

    /** Converts one wire record into the unified success/error record shape. */
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

    private static BulkTokenizeResponseRecord buildTokenizeResponseRecord(
            int index, V1FlowTokenizeResponseObject record, String requestId) {
        boolean failed = record.getError().isPresent()
                && record.getError().get() != null
                && !record.getError().get().isEmpty();
        return new BulkTokenizeResponseRecord(
                index,
                record.getValue().orElse(null),
                asNonEmptyString(record.getTokenGroupName().orElse(null)),
                asNonEmptyString(record.getToken().orElse(null)),
                record.getHttpCode().orElse(failed ? 500 : 200),
                failed ? record.getError().get() : null,
                requestId
        );
    }

    /** The API sends "" for a token or error that does not apply; normalise both to null. */
    private static String asNonEmptyString(Object value) {
        if (!(value instanceof String)) {
            return null;
        }
        String text = (String) value;
        return text.isEmpty() ? null : text;
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

    public static BulkTokenizeResponse formatBulkTokenizeResponse(
            V1FlowTokenizeResponse response,
            List<BulkTokenizeRequestRecord> batchRecords,
            int startIndex,
            Map<String, List<String>> headers) {
        if (response != null && response.getResponse().isPresent()) {
            List<V1FlowTokenizeResponseObject> rows = response.getResponse().get();
            // one id per API call, so every error this batch reports carries the same one
            String requestId = extractRequestId(headers);
            return new BulkTokenizeResponse(groupTokenizeRows(rows, batchRecords, startIndex, requestId));
        }
        return null;
    }

    /**
     * Assigns each response row the index of the record that produced it.
     *
     * <p>The API emits one row per (value, token group) rather than one per record, and a record
     * rejected outright yields a single row instead of one per group — so row count is not a
     * function of the request. Rows do arrive in request order, though, and each carries its value,
     * which is enough: a row belongs to the record in flight while it matches that record's value
     * and the record has not yet taken as many rows as it asked for token groups. Anything else
     * starts the next record. Batching keeps values distinct within a request (see
     * {@link #createBulkTokenizeBatches}), so the value comparison never straddles two records.
     */
    private static List<BulkTokenizeResponseRecord> groupTokenizeRows(
            List<V1FlowTokenizeResponseObject> rows,
            List<BulkTokenizeRequestRecord> batchRecords,
            int startIndex,
            String requestId) {
        List<BulkTokenizeResponseRecord> responseRecords = new ArrayList<>();
        if (batchRecords == null || batchRecords.isEmpty()) {
            // nothing to correlate against; fall back to one record per row
            for (int position = 0; position < rows.size(); position++) {
                responseRecords.add(buildTokenizeResponseRecord(startIndex + position, rows.get(position), requestId));
            }
            return responseRecords;
        }

        int recordPosition = 0;
        int rowsTakenByRecord = 0;
        for (V1FlowTokenizeResponseObject row : rows) {
            Object rowValue = row.getValue().orElse(null);
            while (recordPosition < batchRecords.size()
                    && !acceptsRow(batchRecords.get(recordPosition), rowValue, rowsTakenByRecord)) {
                rowsTakenByRecord = 0;
                recordPosition++;
            }
            if (recordPosition >= batchRecords.size()) {
                // more rows than the request can account for; keep them rather than drop them
                responseRecords.add(buildTokenizeResponseRecord(startIndex + recordPosition, row, requestId));
                recordPosition++;
                continue;
            }
            responseRecords.add(buildTokenizeResponseRecord(startIndex + recordPosition, row, requestId));
            rowsTakenByRecord++;
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

}
