package com.skyflow.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.gson.JsonObject;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.InterfaceName;
import com.skyflow.enums.UpdateType;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.core.ObjectMappers;
import com.skyflow.generated.rest.resources.files.requests.FileDeleteRequest;
import com.skyflow.generated.rest.resources.files.requests.FileUploadRequest;
import com.skyflow.generated.rest.resources.query.requests.ExecuteQueryRequest;
import com.skyflow.generated.rest.resources.records.types.UpdateRequestUpdateType;
import com.skyflow.generated.rest.resources.tokens.requests.DeleteTokenRequest;
import com.skyflow.generated.rest.resources.tokens.requests.GetTokensFromValuesRequest;
import com.skyflow.generated.rest.resources.tokens.requests.TokenizeRequest;
import com.skyflow.generated.rest.types.DeleteResponseObject;
import com.skyflow.generated.rest.types.DeleteTokenResponse;
import com.skyflow.generated.rest.types.DeleteTokenResponseObject;
import com.skyflow.generated.rest.types.DetokenizeResponseObject;
import com.skyflow.generated.rest.types.ExecuteQueryRecordResponse;
import com.skyflow.generated.rest.types.ExecuteQueryResponse;
import com.skyflow.generated.rest.types.FileDeleteRecord;
import com.skyflow.generated.rest.types.FileDeleteResponse;
import com.skyflow.generated.rest.types.FileDeleteResponseObject;
import com.skyflow.generated.rest.types.FileUploadColumn;
import com.skyflow.generated.rest.types.FileUploadRecord;
import com.skyflow.generated.rest.types.GetRequestData;
import com.skyflow.generated.rest.types.GetTokensFromValuesRequestObject;
import com.skyflow.generated.rest.types.GetTokensFromValuesResponse;
import com.skyflow.generated.rest.types.GoogleProtobufValue;
import com.skyflow.generated.rest.types.InsertRecordData;
import com.skyflow.generated.rest.types.RecordResponseObject;
import com.skyflow.generated.rest.types.TokenizeRequestObject;
import com.skyflow.generated.rest.types.TokenizeResponse;
import com.skyflow.generated.rest.types.TokenizeResponseObject;
import com.skyflow.generated.rest.types.UniqueValue;
import com.skyflow.generated.rest.types.UpdateRecordData;
import com.skyflow.generated.rest.types.Upsert;
import com.skyflow.generated.rest.types.UpsertUpdateType;
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
import com.skyflow.vault.data.DeleteFilesRequest;
import com.skyflow.vault.data.DeleteFilesRequestRecord;
import com.skyflow.vault.data.DeleteFilesResponse;
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
import com.skyflow.vault.data.GetTokensRequest;
import com.skyflow.vault.data.GetTokensRequestRecord;
import com.skyflow.vault.data.GetTokensResponse;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertRequestRecord;
import com.skyflow.vault.data.InsertResponse;
import com.skyflow.vault.data.InsertResponseRecord;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.QueryResponse;
import com.skyflow.vault.data.QueryResponseMetadata;
import com.skyflow.vault.data.Token;
import com.skyflow.vault.data.TokenGroupRedactions;
import com.skyflow.vault.data.TokenizeRequestRecord;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateRequestRecord;
import com.skyflow.vault.data.UpdateResponse;
import com.skyflow.vault.data.UpdateResponseRecord;
import com.skyflow.vault.data.UploadFilesRequest;
import com.skyflow.vault.data.UploadFilesRequestColumn;
import com.skyflow.vault.data.UploadFilesRequestRecord;
import com.skyflow.vault.data.UploadFilesResponse;
import com.skyflow.vault.data.UpsertOptions;

import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.MediaType;
import okhttp3.RequestBody;
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

    private static Upsert toUpsert(UpsertOptions upsert) {
        Upsert.Builder builder = Upsert.builder().uniqueColumns(upsert.getUniqueColumns());
        // updateType is a String on the request; the legal values come from the wire enum itself
        // so there is a single source of truth. Validations rejects anything that does not match.
        UpsertUpdateType updateType = toUpsertUpdateType(upsert.getUpdateType());
        if (updateType != null) {
            builder.updateType(updateType);
        }
        return builder.build();
    }

    /** The wire enum value for {@code updateType} (any case), or null when it names no known value. */
    public static UpsertUpdateType toUpsertUpdateType(String updateType) {
        if (updateType == null) {
            return null;
        }
        UpsertUpdateType type = UpsertUpdateType.valueOf(updateType.toUpperCase());
        return type.getEnumValue() == UpsertUpdateType.Value.UNKNOWN ? null : type;
    }

    /**
     * The generated InsertRequest/UpdateRequest builders make the request-level tableName a required
     * stage, yet the API takes the table per record too and rejects it at both levels. Binding through
     * the type's own Jackson mapping lets an absent request-level table simply be omitted from the body.
     */
    private static <T> T bindRequest(Map<String, Object> fields, Class<T> type) {
        return ObjectMappers.JSON_MAPPER.convertValue(fields, type);
    }

    public static com.skyflow.generated.rest.resources.records.requests.InsertRequest getInsertRequestBody(InsertRequest request, VaultConfig config) {
        List<InsertRequestRecord> records = request.getRecords();
        List<InsertRecordData> insertRecordDataList = new ArrayList<>();
        // tableName and upsert must reach the wire at exactly one level: the vault rejects a body
        // that carries either at both the request and the record level. validateTableAndUpsertPlacement
        // has already forced the caller to pick one, so mirror that choice here rather than copying
        // the request-level value down onto every record.
        for (InsertRequestRecord record : records) {
            InsertRecordData.Builder data = InsertRecordData.builder()
                    .data(record.getData());
            // A blank record-level table name counts as absent, matching validateInsertRequest.
            if (hasText(record.getTableName())) {
                data.tableName(record.getTableName());
            }
            // The generated InsertRecordData has no tokens field, but the API still accepts one.
            if (record.getTokens() != null && !record.getTokens().isEmpty()) {
                data.additionalProperty("tokens", record.getTokens());
            }
            UpsertOptions recordUpsert = record.getUpsert();
            if (recordUpsert != null && recordUpsert.getUniqueColumns() != null
                    && !recordUpsert.getUniqueColumns().isEmpty()) {
                data.upsert(toUpsert(recordUpsert));
            }
            insertRecordDataList.add(data.build());
        }

        UpsertOptions requestUpsert = request.getUpsert();
        Upsert upsert = requestUpsert != null && requestUpsert.getUniqueColumns() != null
                && !requestUpsert.getUniqueColumns().isEmpty() ? toUpsert(requestUpsert) : null;
        return buildInsertRequest(config.getVaultId(), request.getTableName(), insertRecordDataList, upsert);
    }

    /** Assembles the wire insert body; a blank tableName and a null upsert are left off. */
    public static com.skyflow.generated.rest.resources.records.requests.InsertRequest buildInsertRequest(
            String vaultId, String tableName, List<InsertRecordData> records, Upsert upsert) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("vaultID", vaultId);
        if (hasText(tableName)) {
            fields.put("tableName", tableName);
        }
        fields.put("records", records);
        if (upsert != null) {
            fields.put("upsert", upsert);
        }
        return bindRequest(fields, com.skyflow.generated.rest.resources.records.requests.InsertRequest.class);
    }

    public static String extractRequestId(Map<String, List<String>> headers) {
        if (headers == null) return null;
        List<String> ids = headers.get(BaseConstants.REQUEST_ID_HEADER_KEY);
        return (ids == null || ids.isEmpty()) ? null : ids.get(0);
    }

    public static com.skyflow.generated.rest.resources.records.requests.GetRequest getGetRequestBody(GetRequest request, VaultConfig config) {
        com.skyflow.generated.rest.resources.records.requests.GetRequest._FinalStage builder = com.skyflow.generated.rest.resources.records.requests.GetRequest.builder().vaultId(config.getVaultId());

        // Multi-table mode: Validations has already rejected the case where both this and the
        // single-table fields below are set, so their presence/absence is mutually exclusive.
        if (request.getRecords() != null && !request.getRecords().isEmpty()) {
            List<GetRequestData> recordDataList = new ArrayList<>();
            for (GetRequestRecord record : request.getRecords()) {
                recordDataList.add(toGetRequestData(record));
            }
            return builder.records(recordDataList).build();
        }

        if (hasText(request.getTableName())) {
            builder.tableName(request.getTableName());
        }
        if (request.getSkyflowIds() != null && !request.getSkyflowIds().isEmpty()) {
            builder.skyflowIDs(request.getSkyflowIds());
        }
        if (request.getColumns() != null && !request.getColumns().isEmpty()) {
            builder.columns(request.getColumns());
        }
        if (request.getColumnRedactions() != null && !request.getColumnRedactions().isEmpty()) {
            builder.columnRedactions(toColumnRedactionsList(request.getColumnRedactions()));
        }
        if (request.getUniqueValues() != null && !request.getUniqueValues().isEmpty()) {
            builder.uniqueValues(toUniqueValueList(request.getUniqueValues()));
        }
        if (request.getLimit() != null) {
            builder.limit(request.getLimit());
        }
        if (request.getOffset() != null) {
            builder.offset(request.getOffset());
        }
        return builder.build();
    }

    private static GetRequestData toGetRequestData(GetRequestRecord record) {
        GetRequestData._FinalStage data = GetRequestData.builder().tableName(record.getTableName());
        if (record.getSkyflowIds() != null && !record.getSkyflowIds().isEmpty()) {
            data.skyflowIDs(record.getSkyflowIds());
        }
        if (record.getColumns() != null && !record.getColumns().isEmpty()) {
            data.columns(record.getColumns());
        }
        if (record.getColumnRedactions() != null && !record.getColumnRedactions().isEmpty()) {
            data.columnRedactions(toColumnRedactionsList(record.getColumnRedactions()));
        }
        if (record.getUniqueValues() != null && !record.getUniqueValues().isEmpty()) {
            data.uniqueValues(toUniqueValueList(record.getUniqueValues()));
        }
        return data.build();
    }

    private static List<com.skyflow.generated.rest.types.ColumnRedactions> toColumnRedactionsList(List<ColumnRedactions> columnRedactions) {
        List<com.skyflow.generated.rest.types.ColumnRedactions> list = new ArrayList<>();
        for (ColumnRedactions columnRedaction : columnRedactions) {
            list.add(com.skyflow.generated.rest.types.ColumnRedactions.builder()
                    .columnName(columnRedaction.getColumnName())
                    .redaction(columnRedaction.getRedaction())
                    .build());
        }
        return list;
    }

    private static List<UniqueValue> toUniqueValueList(List<Map<String, Object>> uniqueValues) {
        List<UniqueValue> list = new ArrayList<>();
        for (Map<String, Object> uniqueValue : uniqueValues) {
            list.add(UniqueValue.builder().data(uniqueValue).build());
        }
        return list;
    }

    public static com.skyflow.generated.rest.resources.records.requests.DeleteRequest getDeleteRequestBody(DeleteRequest request, VaultConfig config) {
        com.skyflow.generated.rest.resources.records.requests.DeleteRequest._FinalStage builder = com.skyflow.generated.rest.resources.records.requests.DeleteRequest.builder()
                .vaultId(config.getVaultId())
                .tableName(request.getTableName());
        if (request.getSkyflowIds() != null && !request.getSkyflowIds().isEmpty()) {
            builder.skyflowIDs(request.getSkyflowIds());
        }
        if (request.getUniqueValues() != null && !request.getUniqueValues().isEmpty()) {
            builder.uniqueValues(toUniqueValueList(request.getUniqueValues()));
        }
        return builder.build();
    }

    public static com.skyflow.generated.rest.resources.records.requests.UpdateRequest getUpdateRequestBody(UpdateRequest request, VaultConfig config) {
        List<UpdateRecordData> updateRecordDataList = new ArrayList<>();
        for (UpdateRequestRecord record : request.getRecords()) {
            UpdateRecordData._FinalStage data = UpdateRecordData.builder()
                    .skyflowId(record.getSkyflowId())
                    .data(record.getData());
            // The generated UpdateRecordData has no tokens field, but the API still accepts one.
            if (record.getTokens() != null && !record.getTokens().isEmpty()) {
                data.additionalProperty("tokens", record.getTokens());
            }
            // A blank record-level table name counts as absent, matching validateInsertRequest's
            // handling of the same table/record split.
            if (hasText(record.getTableName())) {
                data.tableName(record.getTableName());
            }
            updateRecordDataList.add(data.build());
        }

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("vaultID", config.getVaultId());
        if (hasText(request.getTableName())) {
            fields.put("tableName", request.getTableName());
        }
        fields.put("records", updateRecordDataList);
        UpdateType requestUpdateType = request.getUpdateType();
        if (requestUpdateType != null) {
            fields.put("updateType", UpdateRequestUpdateType.valueOf(requestUpdateType.name()));
        }
        return bindRequest(fields, com.skyflow.generated.rest.resources.records.requests.UpdateRequest.class);
    }

    // ── Bulk (batched/concurrent) request-body builders ──────────────────────

    // BulkInsertRequest is an InsertRequest, so the bulk body is built exactly the same way.
    public static com.skyflow.generated.rest.resources.records.requests.InsertRequest getBulkInsertRequestBody(BulkInsertRequest request, VaultConfig config) {
        return getInsertRequestBody(request, config);
    }

    public static com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest getDetokenizeRequestBody(DetokenizeRequest request, String vaultId) {
        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest._FinalStage builder = com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest.builder()
                .vaultId(vaultId)
                .tokens(request.getTokens());
        if (request.getTokenGroupRedactions() != null && !request.getTokenGroupRedactions().isEmpty()) {
            List<com.skyflow.generated.rest.types.TokenGroupRedactions> tokenGroupRedactionsList = new ArrayList<>();
            for (TokenGroupRedactions tokenGroupRedactions : request.getTokenGroupRedactions()) {
                tokenGroupRedactionsList.add(com.skyflow.generated.rest.types.TokenGroupRedactions.builder()
                        .tokenGroupName(tokenGroupRedactions.getTokenGroupName())
                        .redaction(tokenGroupRedactions.getRedaction())
                        .build());
            }
            builder.tokenGroupRedactions(tokenGroupRedactionsList);
        }
        return builder.build();
    }

    public static com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest getBulkDetokenizeRequestBody(BulkDetokenizeRequest request, String vaultId) {
        return getDetokenizeRequestBody(request, vaultId);
    }

    public static DeleteTokenRequest getBulkDeleteTokensRequestBody(BulkDeleteTokensRequest request, String vaultId) {
        return DeleteTokenRequest.builder()
                .vaultId(vaultId)
                .tokens(request.getTokens())
                .build();
    }

    public static ExecuteQueryRequest getQueryRequestBody(QueryRequest request, String vaultId) {
        return ExecuteQueryRequest.builder()
                .vaultId(vaultId)
                .query(request.getQuery())
                .build();
    }

    public static GetTokensFromValuesRequest getGetTokensRequestBody(GetTokensRequest request, String vaultId) {
        List<GetTokensFromValuesRequestObject> records = new ArrayList<>();
        for (GetTokensRequestRecord record : request.getRecords()) {
            records.add(GetTokensFromValuesRequestObject.builder()
                    .value(GoogleProtobufValue.of(record.getValue()))
                    .tokenGroupName(record.getTokenGroupName())
                    .build());
        }
        return GetTokensFromValuesRequest.builder()
                .vaultId(vaultId)
                .records(records)
                .build();
    }

    public static TokenizeRequest getBulkTokenizeRequestBody(
            List<BulkTokenizeRequestRecord> records, String vaultId) {
        List<TokenizeRequestObject> dataList = new ArrayList<>();
        for (BulkTokenizeRequestRecord record : records) {
            dataList.add(buildTokenizeRequestObject(record));
        }
        return TokenizeRequest.builder()
                .vaultId(vaultId)
                .data(dataList)
                .build();
    }

    // ── Bulk batching, exception-handling and response-formatting helpers ────

    public static List<List<InsertRecordData>> createBulkInsertBatches(List<InsertRecordData> records, int batchSize) {
        List<List<InsertRecordData>> batches = new ArrayList<>();
        for (int i = 0; i < records.size(); i += batchSize) {
            batches.add(records.subList(i, Math.min(i + batchSize, records.size())));
        }
        return batches;
    }

    public static List<com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest> createBulkDetokenizeBatches(com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest request, int batchSize) {
        List<com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest> detokenizeRequests = new ArrayList<>();
        List<String> tokens = request.getTokens();

        for (int i = 0; i < tokens.size(); i += batchSize) {
            List<String> batchTokens = tokens.subList(i, Math.min(i + batchSize, tokens.size()));
            com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest._FinalStage batchRequest = com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest.builder()
                    .vaultId(request.getVaultId())
                    .tokens(new ArrayList<>(batchTokens));
            if (request.getTokenGroupRedactions().isPresent() && !request.getTokenGroupRedactions().get().isEmpty()) {
                batchRequest.tokenGroupRedactions(request.getTokenGroupRedactions().get());
            }

            detokenizeRequests.add(batchRequest.build());
        }

        return detokenizeRequests;
    }

    public static List<DeleteTokenRequest> createBulkDeleteTokensBatches(DeleteTokenRequest request, int batchSize) {
        List<DeleteTokenRequest> batches = new ArrayList<>();
        List<String> tokens = request.getTokens();
        for (int i = 0; i < tokens.size(); i += batchSize) {
            List<String> batchTokens = tokens.subList(i, Math.min(i + batchSize, tokens.size()));
            DeleteTokenRequest batchRequest =
                    DeleteTokenRequest.builder()
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

    // ── Unary "records"-shaped exception fallback ─────────────────────────────
    //
    // A unary call's own (only) record can fail outright — e.g. an invalid column on the sole
    // record in an update/insert/get/delete request — and the vault reflects that as the overall
    // HTTP status, so the generated client throws ApiClientApiException instead of returning a
    // normal response body. When the exception body still has the familiar per-record shape
    // ({"records": [...]} for insert/update/get/delete, {"response": [...]} for detokenize), the
    // failure belongs on the response the same way a 200 partial-success does — not as a thrown
    // exception. Each handler below returns null when the body doesn't match that shape, so the
    // caller falls back to throwing a SkyflowException as before.

    /**
     * The exception body as a JSON object, or null when it is not one.
     *
     * <p>For the statuses the API documents (400, 401, 403, 404, 429, 500) the generated client
     * throws a typed error whose body is an {@code ErrorResponse} rather than the parsed map. That
     * type keeps every field it does not model (for example a per-record {@code "records"} array)
     * as an additional property, so converting it back yields the body the server sent.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> errorBodyAsMap(ApiClientApiException apiException) {
        Object rawBody = apiException.body();
        if (rawBody instanceof Map) {
            return (Map<String, Object>) rawBody;
        }
        if (rawBody == null || rawBody instanceof String) {
            return null;
        }
        try {
            Object converted = ObjectMappers.JSON_MAPPER.convertValue(rawBody, Object.class);
            return converted instanceof Map ? (Map<String, Object>) converted : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** The exception body in the form SkyflowException parses: the JSON object when there is one. */
    public static Object errorBody(ApiClientApiException apiException) {
        Map<String, Object> body = errorBodyAsMap(apiException);
        return body != null ? body : apiException.body();
    }

    /** Record maps under {@code key} in an exception body, or null if the shape doesn't match. */
    private static List<Map<String, Object>> extractExceptionRecords(ApiClientApiException apiException, String key) {
        Map<String, Object> body = errorBodyAsMap(apiException);
        if (body == null) {
            return null;
        }
        Object recordsField = body.get(key);
        if (!(recordsField instanceof List)) {
            return null;
        }
        List<Map<String, Object>> records = new ArrayList<>();
        for (Object recordObj : (List<?>) recordsField) {
            if (recordObj instanceof Map) {
                //noinspection unchecked
                records.add((Map<String, Object>) recordObj);
            }
        }
        return records.isEmpty() ? null : records;
    }

    public static InsertResponse handleInsertRequestException(ApiClientApiException apiException) {
        List<Map<String, Object>> recordMaps = extractExceptionRecords(apiException, "records");
        if (recordMaps == null) {
            return null;
        }
        String requestId = extractRequestId(apiException.headers());
        List<InsertResponseRecord> records = new ArrayList<>();
        for (Map<String, Object> recordMap : recordMaps) {
            records.add(new InsertResponseRecord(
                    readString(recordMap, "tableName"),
                    readString(recordMap, "skyflowID"),
                    null, null, null,
                    readHttpCode(recordMap, apiException.statusCode()),
                    readErrorMessage(recordMap),
                    requestId));
        }
        return new InsertResponse(records);
    }

    public static UpdateResponse handleUpdateRequestException(ApiClientApiException apiException) {
        List<Map<String, Object>> recordMaps = extractExceptionRecords(apiException, "records");
        if (recordMaps == null) {
            return null;
        }
        String requestId = extractRequestId(apiException.headers());
        List<UpdateResponseRecord> records = new ArrayList<>();
        for (Map<String, Object> recordMap : recordMaps) {
            records.add(new UpdateResponseRecord(
                    readString(recordMap, "tableName"),
                    readString(recordMap, "skyflowID"),
                    null, null, null,
                    readHttpCode(recordMap, apiException.statusCode()),
                    readErrorMessage(recordMap),
                    requestId));
        }
        return new UpdateResponse(records);
    }

    public static GetResponse handleGetRequestException(ApiClientApiException apiException) {
        List<Map<String, Object>> recordMaps = extractExceptionRecords(apiException, "records");
        if (recordMaps == null) {
            return null;
        }
        String requestId = extractRequestId(apiException.headers());
        List<GetResponseRecord> records = new ArrayList<>();
        for (Map<String, Object> recordMap : recordMaps) {
            records.add(new GetResponseRecord(
                    readString(recordMap, "tableName"),
                    readString(recordMap, "skyflowID"),
                    null, null, null,
                    readHttpCode(recordMap, apiException.statusCode()),
                    readErrorMessage(recordMap),
                    requestId));
        }
        return new GetResponse(records);
    }

    public static DeleteResponse handleDeleteRequestException(ApiClientApiException apiException) {
        List<Map<String, Object>> recordMaps = extractExceptionRecords(apiException, "records");
        if (recordMaps == null) {
            return null;
        }
        String requestId = extractRequestId(apiException.headers());
        List<DeleteResponseRecord> records = new ArrayList<>();
        for (Map<String, Object> recordMap : recordMaps) {
            records.add(new DeleteResponseRecord(
                    readString(recordMap, "skyflowID"),
                    readHttpCode(recordMap, apiException.statusCode()),
                    readErrorMessage(recordMap),
                    requestId));
        }
        return new DeleteResponse(records);
    }

    public static DetokenizeResponse handleDetokenizeRequestException(ApiClientApiException apiException) {
        List<Map<String, Object>> recordMaps = extractExceptionRecords(apiException, "response");
        if (recordMaps == null) {
            return null;
        }
        String requestId = extractRequestId(apiException.headers());
        List<DetokenizeResponseRecord> records = new ArrayList<>();
        for (Map<String, Object> recordMap : recordMaps) {
            records.add(new DetokenizeResponseRecord(
                    readString(recordMap, "token"),
                    null,
                    readString(recordMap, "tokenGroupName"),
                    null,
                    readHttpCode(recordMap, apiException.statusCode()),
                    readErrorMessage(recordMap),
                    requestId));
        }
        return new DetokenizeResponse(records);
    }

    public static GetTokensResponse handleGetTokensRequestException(ApiClientApiException apiException) {
        List<Map<String, Object>> recordMaps = extractExceptionRecords(apiException, "records");
        if (recordMaps == null) {
            return null;
        }
        String requestId = extractRequestId(apiException.headers());
        ArrayList<HashMap<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> recordMap : recordMaps) {
            records.add(getTokensRow(
                    recordMap.get("value"),
                    readString(recordMap, "tokenGroupName"),
                    asNonEmptyString(recordMap.get("token")),
                    readHttpCode(recordMap, apiException.statusCode()),
                    readErrorMessage(recordMap),
                    requestId));
        }
        return new GetTokensResponse(records);
    }

    // Errors are parsed into ErrorRecord (shared with the other bulk ops), then projected onto
    // the unified BulkInsertResponseRecord shape that bulk insert now returns.
    public static List<BulkInsertResponseRecord> handleBulkInsertBatchException(
            Throwable ex, List<InsertRecordData> batch, int batchNumber, int batchSize
    ) {
        List<BulkInsertResponseRecord> allRecords = new ArrayList<>();
        Throwable cause = ex.getCause();
        if (cause instanceof ApiClientApiException) {
            ApiClientApiException apiException = (ApiClientApiException) cause;
            String requestId = extractRequestId(apiException.headers());
            Map<String, Object> responseBody = errorBodyAsMap(apiException);
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
            Throwable ex, com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest batch, int batchNumber, int batchSize
    ) {
        List<BulkDetokenizeResponseRecord> allRecords = new ArrayList<>();
        Throwable cause = ex.getCause();
        if (cause instanceof ApiClientApiException) {
            ApiClientApiException apiException = (ApiClientApiException) cause;
            String requestId = extractRequestId(apiException.headers());
            Map<String, Object> responseBody = errorBodyAsMap(apiException);
            int indexNumber = batchNumber * batchSize;
            int tokenCount = batch.getTokens() != null ? batch.getTokens().size() : 0;
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
            for (int j = 0; j < batch.getTokens().size(); j++) {
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
            DeleteTokenRequest batch,
            int batchNumber, int batchSize
    ) {
        List<BulkDeleteTokensResponseRecord> errorRecords = new ArrayList<>();
        List<String> batchTokens = (batch != null && batch.getTokens() != null)
                ? batch.getTokens() : new ArrayList<>();
        int startIndex = batchNumber * batchSize;
        Throwable cause = ex.getCause();
        if (cause instanceof ApiClientApiException) {
            ApiClientApiException apiException = (ApiClientApiException) cause;
            String requestId = extractRequestId(apiException.headers());
            Map<String, Object> responseBody = errorBodyAsMap(apiException);
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
        Map<String, Object> body = errorBodyAsMap(apiException);
        if (body == null || !body.containsKey("response")) {
            return null;
        }
        try {
            TokenizeResponse parsed = ObjectMappers.JSON_MAPPER.convertValue(body, TokenizeResponse.class);
            if (parsed.getResponse() == null || parsed.getResponse().isEmpty()) {
                return null;
            }
            return groupTokenizeRows(parsed.getResponse(), batchRecords, startIndex,
                    extractRequestId(apiException.headers()));
        } catch (RuntimeException ignored) {
            // body did not deserialise into the shape we know; let the caller summarise instead
            return null;
        }
    }

    /** Pulls the most specific message available from a failed batch response body. */
    private static String extractBatchErrorMessage(ApiClientApiException apiException) {
        Map<String, Object> body = errorBodyAsMap(apiException);
        if (body != null) {
            Object errField = body.get("error");
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
    public static InsertResponse formatInsertResponse(
            com.skyflow.generated.rest.types.InsertResponse response, Map<String, List<String>> headers) {
        List<InsertResponseRecord> records = new ArrayList<>();
        if (response != null && response.getRecords() != null) {
            for (RecordResponseObject current : response.getRecords()) {
                String reqID = current.getError().isPresent() ? extractRequestId(headers) : null;
                records.add(new InsertResponseRecord(
                        current.getTableName().orElse(null),
                        current.getSkyflowId().orElse(null),
                        Token.parseTokens(current.getTokens().orElse(null)),
                        current.getData().orElse(null),
                        current.getHashedData().orElse(null),
                        recordHttpCode(current.getHttpCode(), current.getError().isPresent()),
                        current.getError().orElse(null),
                        reqID));
            }
        }
        return new InsertResponse(records);
    }

    // Update has no bulk/batched counterpart, so there is no index to attach here, but the call's
    // own requestId is still populated on error records. The wire response reuses
    // RecordResponseObject (the same shape as insert's), so the per-record mapping mirrors
    // formatInsertResponse.
    // Get has no bulk/batched counterpart, so there is no index to attach here, but the call's own
    // requestId is still populated on error records. The wire response reuses
    // RecordResponseObject (the same shape as insert's/update's), so the per-record mapping
    // mirrors formatInsertResponse.
    public static GetResponse formatGetResponse(
            com.skyflow.generated.rest.types.GetResponse response, Map<String, List<String>> headers) {
        List<GetResponseRecord> records = new ArrayList<>();
        if (response != null && response.getRecords() != null) {
            for (RecordResponseObject current : response.getRecords()) {
                String reqID = current.getError().isPresent() ? extractRequestId(headers) : null;
                records.add(new GetResponseRecord(
                        current.getTableName().orElse(null),
                        current.getSkyflowId().orElse(null),
                        Token.parseTokens(current.getTokens().orElse(null)),
                        current.getData().orElse(null),
                        current.getHashedData().orElse(null),
                        recordHttpCode(current.getHttpCode(), current.getError().isPresent()),
                        current.getError().orElse(null),
                        reqID));
            }
        }
        return new GetResponse(records);
    }

    // Delete has no bulk/batched counterpart, so there is no index to attach here, but the call's
    // own requestId is still populated on error records. Unlike insert/update/get, the wire
    // response (DeleteResponseObject) carries no data/tokens.
    public static DeleteResponse formatDeleteResponse(
            com.skyflow.generated.rest.types.DeleteResponse response, Map<String, List<String>> headers) {
        List<DeleteResponseRecord> records = new ArrayList<>();
        if (response != null && response.getRecords() != null) {
            for (DeleteResponseObject current : response.getRecords()) {
                String reqID = current.getError().isPresent() ? extractRequestId(headers) : null;
                records.add(new DeleteResponseRecord(
                        current.getSkyflowId(),
                        recordHttpCode(current.getHttpCode(), current.getError().isPresent()),
                        current.getError().orElse(null),
                        reqID));
            }
        }
        return new DeleteResponse(records);
    }

    public static UpdateResponse formatUpdateResponse(
            com.skyflow.generated.rest.types.UpdateResponse response, Map<String, List<String>> headers) {
        List<UpdateResponseRecord> records = new ArrayList<>();
        if (response != null && response.getRecords() != null) {
            for (RecordResponseObject current : response.getRecords()) {
                String reqID = current.getError().isPresent() ? extractRequestId(headers) : null;
                records.add(new UpdateResponseRecord(
                        current.getTableName().orElse(null),
                        current.getSkyflowId().orElse(null),
                        Token.parseTokens(current.getTokens().orElse(null)),
                        current.getData().orElse(null),
                        current.getHashedData().orElse(null),
                        recordHttpCode(current.getHttpCode(), current.getError().isPresent()),
                        current.getError().orElse(null),
                        reqID));
            }
        }
        return new UpdateResponse(records);
    }

    public static BulkInsertResponse formatBulkInsertResponse(
            com.skyflow.generated.rest.types.InsertResponse response, int batch, int batchSize, Map<String, List<String>> headers) {
        BulkInsertResponse formattedResponse = null;
        List<BulkInsertResponseRecord> records = new ArrayList<>();
        if (response != null && response.getRecords() != null) {
            List<RecordResponseObject> record = response.getRecords();
            int indexNumber = batch * batchSize;
            int recordsSize = record.size();
            for (int index = 0; index < recordsSize; index++) {
                RecordResponseObject current = record.get(index);
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
                        recordHttpCode(current.getHttpCode(), current.getError().isPresent()),
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
    public static DetokenizeResponse formatDetokenizeResponse(
            com.skyflow.generated.rest.types.DetokenizeResponse response, Map<String, List<String>> headers) {
        List<DetokenizeResponseRecord> records = new ArrayList<>();
        if (response != null && response.getResponse() != null) {
            for (DetokenizeResponseObject current : response.getResponse()) {
                DetokenizeResponseRecordMetadata metadata = DetokenizeResponseRecordMetadata.parseMetadata(current.getMetadata().orElse(null));
                String reqID = current.getError().isPresent() ? extractRequestId(headers) : null;
                records.add(new DetokenizeResponseRecord(
                        current.getToken(),
                        unwrap(current.getValue().orElse(null)),
                        current.getTokenGroupName().orElse(null),
                        metadata,
                        current.getHttpCode().orElse(current.getError().isPresent() ? 500 : 200),
                        current.getError().orElse(null),
                        reqID));
            }
        }
        return new DetokenizeResponse(records);
    }

    public static BulkDetokenizeResponse formatBulkDetokenizeResponse(
            com.skyflow.generated.rest.types.DetokenizeResponse response, int batch, int batchSize, Map<String, List<String>> headers) {
        if (response != null && response.getResponse() != null) {
            List<DetokenizeResponseObject> record = response.getResponse();
            List<BulkDetokenizeResponseRecord> records = new ArrayList<>();
            int indexNumber = batch * batchSize;
            int recordsSize = record.size();
            for (int index = 0; index < recordsSize; index++) {
                DetokenizeResponseObject current = record.get(index);
                DetokenizeResponseRecordMetadata metadata = DetokenizeResponseRecordMetadata.parseMetadata(current.getMetadata().orElse(null));
                String reqID = null;
                if(current.getError().isPresent()){
                    reqID = extractRequestId(headers);
                }
                records.add(new BulkDetokenizeResponseRecord(
                        indexNumber,
                        current.getToken(),
                        unwrap(current.getValue().orElse(null)),
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

    public static BulkDeleteTokensResponse formatBulkDeleteTokensResponse(
            DeleteTokenResponse response,
            DeleteTokenRequest batchRequest,
            int batch, int batchSize, Map<String, List<String>> headers) {
        if (response != null && response.getTokens() != null) {
            List<DeleteTokenResponseObject> records = response.getTokens();
            List<String> requestedTokens = batchRequest != null ? batchRequest.getTokens() : null;
            List<BulkDeleteTokensResponseRecord> responseRecords = new ArrayList<>();
            int indexNumber = batch * batchSize;
            // one id per API call, so every error this batch reports carries the same one
            String requestId = extractRequestId(headers);
            for (int position = 0; position < records.size(); position++) {
                DeleteTokenResponseObject record = records.get(position);
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
    // Query has no per-record status: a failure is thrown rather than returned, so each row is just
    // the record's data. The requestId is call-level and set on every response, not only on errors.
    public static QueryResponse formatQueryResponse(ExecuteQueryResponse response, Map<String, List<String>> headers) {
        ArrayList<HashMap<String, Object>> fields = new ArrayList<>();
        List<String> columns = null;
        if (response != null) {
            if (response.getRecords().isPresent()) {
                for (ExecuteQueryRecordResponse record : response.getRecords().get()) {
                    // LinkedHashMap keeps the column order the vault returned
                    fields.add(new LinkedHashMap<>(record.getData().orElse(new HashMap<>())));
                }
            }
            if (response.getMetadata().isPresent()) {
                columns = response.getMetadata().get().getColumns().orElse(null);
            }
        }
        return new QueryResponse(fields, new QueryResponseMetadata(columns), extractRequestId(headers));
    }

    // Unary, unbatched: one row per input entry in request order, with the call's requestId set
    // only on rows that carry an error, matching the other unary operations.
    public static GetTokensResponse formatGetTokensResponse(GetTokensFromValuesResponse response, Map<String, List<String>> headers) {
        ArrayList<HashMap<String, Object>> records = new ArrayList<>();
        if (response != null && response.getRecords() != null) {
            for (TokenizeResponseObject current : response.getRecords()) {
                String error = asNonEmptyString(current.getError().orElse(null));
                records.add(getTokensRow(
                        unwrap(current.getValue()),
                        asNonEmptyString(current.getTokenGroupName().orElse(null)),
                        asNonEmptyString(current.getToken()),
                        current.getHttpCode().orElse(error != null ? 500 : 200),
                        error,
                        error != null ? extractRequestId(headers) : null));
            }
        }
        return new GetTokensResponse(records);
    }

    private static HashMap<String, Object> getTokensRow(
            Object value, String tokenGroupName, String token, int httpCode, String error, String requestId) {
        HashMap<String, Object> row = new LinkedHashMap<>();
        row.put("value", value);
        row.put("tokenGroupName", tokenGroupName);
        row.put("token", token);
        row.put("httpCode", httpCode);
        row.put("error", error);
        row.put("requestId", requestId);
        return row;
    }

    private static TokenizeRequestObject buildTokenizeRequestObject(TokenizeRequestRecord record) {
        TokenizeRequestObject._FinalStage builder = TokenizeRequestObject.builder()
                .value(GoogleProtobufValue.of(record.getValue()))
                .tokenGroupNames(record.getTokenGroupNames());
        if (record.getToken() != null) {
            builder.token(GoogleProtobufValue.of(record.getToken()));
        }
        return builder.build();
    }

    private static BulkTokenizeResponseRecord buildTokenizeResponseRecord(
            int index, TokenizeResponseObject record, String requestId) {
        boolean failed = record.getError().isPresent()
                && record.getError().get() != null
                && !record.getError().get().isEmpty();
        return new BulkTokenizeResponseRecord(
                index,
                unwrap(record.getValue()),
                asNonEmptyString(record.getTokenGroupName().orElse(null)),
                asNonEmptyString(record.getToken()),
                record.getHttpCode().orElse(failed ? 500 : 200),
                failed ? record.getError().get() : null,
                requestId
        );
    }

    /** The plain JSON value inside a wire {@code google.protobuf.Value}, or null when absent. */
    private static Object unwrap(GoogleProtobufValue value) {
        return value == null ? null : value.get();
    }

    /**
     * The record's HTTP status. The wire field is a plain int, so an omitted status reads as 0;
     * fall back to the status the record's error implies, as when the field was optional.
     */
    private static int recordHttpCode(int httpCode, boolean failed) {
        return httpCode != 0 ? httpCode : (failed ? 500 : 200);
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
    private static boolean isFailedRecord(DeleteTokenResponseObject record) {
        return record.getError().isPresent()
                && record.getError().get() != null
                && !record.getError().get().isEmpty()
                && record.getHttpCode().orElse(200) != 200;
    }

    public static BulkTokenizeResponse formatBulkTokenizeResponse(
            TokenizeResponse response,
            List<BulkTokenizeRequestRecord> batchRecords,
            int startIndex,
            Map<String, List<String>> headers) {
        if (response != null && response.getResponse() != null) {
            List<TokenizeResponseObject> rows = response.getResponse();
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
            List<TokenizeResponseObject> rows,
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
        for (TokenizeResponseObject row : rows) {
            Object rowValue = unwrap(row.getValue());
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

    // ── Upload / delete files ────────────────────────────────────────────────

    public static final String UPLOAD_STATUS_UPLOADED = "UPLOADED";
    public static final String UPLOAD_STATUS_FAILED = "FAILED";
    public static final String UPLOAD_STATUS_SKIPPED = "SKIPPED";
    private static final String DEFAULT_UPLOAD_CONTENT_TYPE = "application/octet-stream";

    /** Phase A body: the file columns of each record, with the name each file will be stored under. */
    public static FileUploadRequest getUploadFilesRequestBody(UploadFilesRequest request, String vaultId) {
        List<FileUploadRecord> records = new ArrayList<>();
        for (UploadFilesRequestRecord record : request.getRecords()) {
            List<FileUploadColumn> columns = new ArrayList<>();
            for (UploadFilesRequestColumn column : record.getColumns()) {
                FileUploadColumn._FinalStage wireColumn = FileUploadColumn.builder().column(column.getColumn());
                String fileName = resolveUploadFileName(column);
                if (fileName != null) {
                    wireColumn.fileName(fileName);
                }
                columns.add(wireColumn.build());
            }
            FileUploadRecord._FinalStage wireRecord = FileUploadRecord.builder()
                    .tableName(record.getTableName())
                    .columns(columns);
            if (record.getSkyflowId() != null) {
                wireRecord.skyflowId(record.getSkyflowId());
            }
            records.add(wireRecord.build());
        }
        return FileUploadRequest.builder().vaultId(vaultId).records(records).build();
    }

    /**
     * The name the file is stored under: the explicit fileName, else the name of the column's file
     * source (filePath or fileObject; base64 always carries an explicit fileName).
     * Null leaves it to the server, which generates a random 16-byte UUID name.
     */
    public static String resolveUploadFileName(UploadFilesRequestColumn column) {
        if (hasText(column.getFileName())) {
            return column.getFileName();
        }
        if (hasText(column.getFilePath())) {
            return new File(column.getFilePath()).getName();
        }
        if (!hasText(column.getBase64()) && column.getFileObject() != null) {
            return column.getFileObject().getName();
        }
        return null;
    }

    /**
     * MIME types for common file extensions. The JDK's own table differs between releases — Java 8
     * knows none of the Office formats, csv, json, mp3 or mp4 — so the SDK keeps its own list and
     * the same file gets the same type on every JDK. Anything not listed falls back to the JDK.
     */
    private static final Map<String, String> UPLOAD_CONTENT_TYPES = new HashMap<>();

    static {
        String[][] types = {
                {"pdf", "application/pdf"},
                {"jpg", "image/jpeg"}, {"jpeg", "image/jpeg"}, {"png", "image/png"}, {"gif", "image/gif"},
                {"bmp", "image/bmp"}, {"tif", "image/tiff"}, {"tiff", "image/tiff"}, {"webp", "image/webp"},
                {"heic", "image/heic"}, {"heif", "image/heif"}, {"svg", "image/svg+xml"},
                {"txt", "text/plain"}, {"csv", "text/csv"}, {"tsv", "text/tab-separated-values"},
                {"json", "application/json"}, {"xml", "application/xml"}, {"html", "text/html"},
                {"htm", "text/html"}, {"rtf", "application/rtf"},
                {"doc", "application/msword"},
                {"docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
                {"xls", "application/vnd.ms-excel"},
                {"xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
                {"ppt", "application/vnd.ms-powerpoint"},
                {"pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"},
                {"odt", "application/vnd.oasis.opendocument.text"},
                {"ods", "application/vnd.oasis.opendocument.spreadsheet"},
                {"odp", "application/vnd.oasis.opendocument.presentation"},
                {"zip", "application/zip"}, {"gz", "application/gzip"}, {"tar", "application/x-tar"},
                {"7z", "application/x-7z-compressed"},
                {"mp3", "audio/mpeg"}, {"wav", "audio/wav"}, {"m4a", "audio/mp4"}, {"ogg", "audio/ogg"},
                {"flac", "audio/flac"},
                {"mp4", "video/mp4"}, {"mov", "video/quicktime"}, {"avi", "video/x-msvideo"},
                {"webm", "video/webm"}, {"mkv", "video/x-matroska"},
        };
        for (String[] type : types) {
            UPLOAD_CONTENT_TYPES.put(type[0], type[1]);
        }
    }

    /**
     * The caller's contentType, else the type for the file name's extension (case-insensitive),
     * else the JDK's guess, else application/octet-stream.
     */
    public static String resolveUploadContentType(UploadFilesRequestColumn column, String fileName) {
        if (hasText(column.getContentType())) {
            return column.getContentType();
        }
        if (fileName == null) {
            return DEFAULT_UPLOAD_CONTENT_TYPE;
        }
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0) {
            String known = UPLOAD_CONTENT_TYPES.get(fileName.substring(dot + 1).toLowerCase(java.util.Locale.ROOT));
            if (known != null) {
                return known;
            }
        }
        String guessed = URLConnection.guessContentTypeFromName(fileName);
        return guessed != null ? guessed : DEFAULT_UPLOAD_CONTENT_TYPE;
    }

    /**
     * Decodes each base64 column once, before Phase A, so an invalid value fails the call before
     * anything is sent. The decoded bytes are reused for the Phase B upload.
     */
    public static Map<UploadFilesRequestColumn, byte[]> decodeBase64Columns(UploadFilesRequest request)
            throws SkyflowException {
        Map<UploadFilesRequestColumn, byte[]> decoded = new IdentityHashMap<>();
        List<UploadFilesRequestRecord> records = request.getRecords();
        for (int i = 0; i < records.size(); i++) {
            for (UploadFilesRequestColumn column : records.get(i).getColumns()) {
                if (!hasText(column.getBase64())) {
                    continue;
                }
                try {
                    decoded.put(column, Base64.getDecoder().decode(column.getBase64()));
                } catch (IllegalArgumentException e) {
                    LogUtil.printErrorLog(parameterizedString(ErrorLogs.INVALID_BASE64_IN_UPLOAD_FILES_COLUMN.getLog(),
                            InterfaceName.UPLOAD_FILES.getName(), String.valueOf(i)));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                            ErrorMessage.InvalidBase64InUploadFilesColumn.getMessage());
                }
            }
        }
        return decoded;
    }

    /**
     * Phase B body: the bytes of the column's one file source. Files are streamed from disk rather
     * than read into memory; base64 content arrives already decoded.
     */
    public static RequestBody buildUploadFileBody(UploadFilesRequestColumn column, byte[] decodedBase64,
                                                  String contentType) {
        MediaType mediaType = MediaType.parse(contentType);
        if (hasText(column.getFilePath())) {
            return RequestBody.create(new File(column.getFilePath()), mediaType);
        }
        if (decodedBase64 != null) {
            return RequestBody.create(decodedBase64, mediaType);
        }
        return RequestBody.create(column.getFileObject(), mediaType);
    }

    public static HashMap<String, Object> uploadFilesColumnRow(
            String column, String fileName, String uploadStatus, String error) {
        HashMap<String, Object> row = new LinkedHashMap<>();
        row.put("column", column);
        row.put("fileName", fileName);
        row.put("uploadStatus", uploadStatus);
        row.put("error", error);
        return row;
    }

    public static HashMap<String, Object> uploadFilesRecordRow(String skyflowId, String tableName,
            List<HashMap<String, Object>> columns, int httpCode, String error, String requestId) {
        HashMap<String, Object> row = new LinkedHashMap<>();
        row.put("skyflowId", skyflowId);
        row.put("tableName", tableName);
        row.put("columns", columns);
        row.put("httpCode", httpCode);
        row.put("error", error);
        row.put("requestId", requestId);
        return row;
    }

    /** Every requested column of a record that failed in Phase A: nothing was uploaded. */
    public static List<HashMap<String, Object>> skippedUploadColumns(UploadFilesRequestRecord record, String error) {
        List<HashMap<String, Object>> columns = new ArrayList<>();
        if (record != null && record.getColumns() != null) {
            for (UploadFilesRequestColumn column : record.getColumns()) {
                columns.add(uploadFilesColumnRow(column.getColumn(), resolveUploadFileName(column),
                        UPLOAD_STATUS_SKIPPED, error));
            }
        }
        return columns;
    }

    public static FileDeleteRequest getDeleteFilesRequestBody(DeleteFilesRequest request, String vaultId) {
        List<FileDeleteRecord> records = new ArrayList<>();
        for (DeleteFilesRequestRecord record : request.getRecords()) {
            FileDeleteRecord._FinalStage wireRecord = FileDeleteRecord.builder()
                    .tableName(record.getTableName())
                    .columns(record.getColumns());
            // validation guarantees exactly one of skyflowId / uniqueValues is set
            if (record.getSkyflowId() != null) {
                wireRecord.skyflowId(record.getSkyflowId());
            }
            if (record.getUniqueValues() != null && !record.getUniqueValues().isEmpty()) {
                wireRecord.uniqueValues(toUniqueValueList(record.getUniqueValues()));
            }
            records.add(wireRecord.build());
        }
        return FileDeleteRequest.builder().vaultId(vaultId).records(records).build();
    }

    // One entry per resolved record (uniqueValues may resolve to several), with the call's
    // requestId set only on entries that carry an error, matching the other unary operations.
    public static DeleteFilesResponse formatDeleteFilesResponse(
            FileDeleteResponse response, Map<String, List<String>> headers) {
        ArrayList<HashMap<String, Object>> records = new ArrayList<>();
        if (response != null && response.getRecords() != null) {
            for (FileDeleteResponseObject current : response.getRecords()) {
                String error = asNonEmptyString(current.getError().orElse(null));
                records.add(deleteFilesRecordRow(
                        current.getSkyflowId().orElse(null),
                        current.getTableName(),
                        deletedColumns(current.getData().orElse(null)),
                        recordHttpCode(current.getHttpCode(), error != null),
                        error,
                        error != null ? extractRequestId(headers) : null));
            }
        }
        return new DeleteFilesResponse(records);
    }

    public static DeleteFilesResponse handleDeleteFilesRequestException(ApiClientApiException apiException) {
        List<Map<String, Object>> recordMaps = extractExceptionRecords(apiException, "records");
        if (recordMaps == null) {
            return null;
        }
        String requestId = extractRequestId(apiException.headers());
        ArrayList<HashMap<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> recordMap : recordMaps) {
            records.add(deleteFilesRecordRow(
                    readString(recordMap, "skyflowID"),
                    readString(recordMap, "tableName"),
                    null,
                    readHttpCode(recordMap, apiException.statusCode()),
                    readErrorMessage(recordMap),
                    requestId));
        }
        return new DeleteFilesResponse(records);
    }

    /** The deleted columns from the wire "data" map, or null when the record failed. */
    private static List<HashMap<String, Object>> deletedColumns(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        List<HashMap<String, Object>> columns = new ArrayList<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object result = entry.getValue();
            Object status = result instanceof Map ? ((Map<?, ?>) result).get("status") : result;
            HashMap<String, Object> column = new LinkedHashMap<>();
            column.put("column", entry.getKey());
            column.put("status", status == null ? null : status.toString());
            columns.add(column);
        }
        return columns;
    }

    private static HashMap<String, Object> deleteFilesRecordRow(String skyflowId, String tableName,
            List<HashMap<String, Object>> columns, int httpCode, String error, String requestId) {
        HashMap<String, Object> row = new LinkedHashMap<>();
        row.put("skyflowId", skyflowId);
        row.put("tableName", tableName);
        row.put("columns", columns);
        row.put("httpCode", httpCode);
        row.put("error", error);
        row.put("requestId", requestId);
        return row;
    }
}
