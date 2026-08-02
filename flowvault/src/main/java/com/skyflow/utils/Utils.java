package com.skyflow.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest;
import com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest;
import com.skyflow.generated.rest.types.FlowEnumUpdateType;
import com.skyflow.generated.rest.types.FlowTokenizeResponseObjectToken;
import com.skyflow.generated.rest.types.V1DeleteTokenResponseObject;
import com.skyflow.generated.rest.types.V1FlowDeleteTokenResponse;
import com.skyflow.generated.rest.types.V1FlowDetokenizeResponse;
import com.skyflow.generated.rest.types.V1FlowDetokenizeResponseObject;
import com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject;
import com.skyflow.generated.rest.types.V1FlowTokenizeResponse;
import com.skyflow.generated.rest.types.V1FlowTokenizeResponseObject;
import com.skyflow.generated.rest.types.V1InsertRecordData;
import com.skyflow.generated.rest.types.V1InsertResponse;
import com.skyflow.generated.rest.types.V1RecordResponseObject;
import com.skyflow.generated.rest.types.V1TokenGroupRedactions;
import com.skyflow.generated.rest.types.V1Upsert;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.vault.data.BulkDeleteTokensRequest;
import com.skyflow.vault.data.BulkDeleteTokensResponse;
import com.skyflow.vault.data.BulkDetokenizeRequest;
import com.skyflow.vault.data.BulkDetokenizeResponse;
import com.skyflow.vault.data.BulkDetokenizeResponseRecord;
import com.skyflow.vault.data.BulkInsertRequest;
import com.skyflow.vault.data.BulkInsertResponse;
import com.skyflow.vault.data.BulkInsertResponseRecord;
import com.skyflow.vault.data.BulkTokenizeRecord;
import com.skyflow.vault.data.BulkTokenizeRequest;
import com.skyflow.vault.data.BulkTokenizeResponse;
import com.skyflow.vault.data.DeleteTokensSuccess;
import com.skyflow.vault.data.ErrorRecord;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertRequestRecord;
import com.skyflow.vault.data.TokenGroupRedactions;
import com.skyflow.vault.data.TokenizeSuccess;
import com.skyflow.vault.data.UpsertOptions;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

public final class Utils extends BaseUtils {

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
        for (InsertRequestRecord record : records) {
            V1InsertRecordData.Builder data = V1InsertRecordData.builder()
                    .data(record.getData())
                    // A blank record-level table name counts as absent, matching
                    // validateInsertRequest, so it falls back to the request-level one.
                    .tableName(hasText(record.getTableName()) ? record.getTableName() : request.getTableName());
            if (record.getTokens() != null && !record.getTokens().isEmpty()) {
                data.tokens(record.getTokens());
            }
            UpsertOptions upsert = record.getUpsert() != null ? record.getUpsert() : request.getUpsert();
            if (upsert != null && upsert.getUniqueColumns() != null && !upsert.getUniqueColumns().isEmpty()) {
                data.upsert(toV1Upsert(upsert));
            }
            insertRecordDataList.add(data.build());
        }

        V1InsertRequest.Builder builder = V1InsertRequest.builder()
                .vaultId(config.getVaultId())
                .records(insertRecordDataList);
        if (hasText(request.getTableName())) {
            builder.tableName(request.getTableName());
        }
        return builder.build();
    }

    private static String extractRequestId(Map<String, List<String>> headers) {
        if (headers == null) return null;
        List<String> ids = headers.get(BaseConstants.REQUEST_ID_HEADER_KEY);
        return (ids == null || ids.isEmpty()) ? null : ids.get(0);
    }

    // ── Bulk (batched/concurrent) request-body builders ──────────────────────

    // BulkInsertRequest is an InsertRequest, so the bulk body is built exactly the same way.
    public static com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest getBulkInsertRequestBody(BulkInsertRequest request, VaultConfig config) {
        return getInsertRequestBody(request, config);
    }

    public static V1FlowDetokenizeRequest getBulkDetokenizeRequestBody(BulkDetokenizeRequest request, String vaultId) {
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
    public static BulkInsertResponseRecord createInsertErrorRecord(Map<String, Object> recordMap, int indexNumber, String requestId) {
        BulkInsertResponseRecord err = null;
        if (recordMap != null) {
            int code = 500;
            if (recordMap.containsKey("http_code")) {
                code = (Integer) recordMap.get("http_code");
            } else if (recordMap.containsKey("httpCode")) {
                code = (Integer) recordMap.get("httpCode");
            } else if (recordMap.containsKey("statusCode")) {
                code = (Integer) recordMap.get("statusCode");
            }
            // check if skyflowID is present
            String skyflowID = null;
            if (recordMap.containsKey("skyflowID")) {
                skyflowID = recordMap.get("skyflowID").toString();
            }
            String tableName = null;
            if (recordMap.containsKey("tableName")) {
                tableName = recordMap.get("tableName").toString();
            }
            String message = recordMap.containsKey("error") ? (String) recordMap.get("error") :
                    recordMap.containsKey("message") ? (String) recordMap.get("message") : "Unknown error";
            err = new BulkInsertResponseRecord(indexNumber, tableName, skyflowID, null, null, code, message, requestId);
        }
        return err;
    }

    public static BulkDetokenizeResponseRecord createDetokenizeErrorRecord(Map<String, Object> recordMap, int indexNumber, String requestId) {
        BulkDetokenizeResponseRecord err = null;
        if (recordMap != null) {
            int code = 500;
            if (recordMap.containsKey("http_code")) {
                code = (Integer) recordMap.get("http_code");
            } else if (recordMap.containsKey("httpCode")) {
                code = (Integer) recordMap.get("httpCode");
            } else if (recordMap.containsKey("statusCode")) {
                code = (Integer) recordMap.get("statusCode");
            }
            // the failing token is echoed back so the caller can tell which one it was
            String token = null;
            if (recordMap.containsKey("token")) {
                token = recordMap.get("token").toString();
            }
            String tokenGroupName = null;
            if (recordMap.containsKey("tokenGroupName")) {
                tokenGroupName = recordMap.get("tokenGroupName").toString();
            }
            String message = recordMap.containsKey("error") ? (String) recordMap.get("error") :
                    recordMap.containsKey("message") ? (String) recordMap.get("message") : "Unknown error";
            err = new BulkDetokenizeResponseRecord(indexNumber, token, null, tokenGroupName, null, code, message, requestId);
        }
        return err;
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
                            err = new BulkInsertResponseRecord(indexNumber, null, null, null, null, apiException.statusCode(), errorMessage, requestId);

                        }
                        allRecords.add(err);
                        indexNumber++;
                    }
                }
            }

            if (allRecords.isEmpty()) {
                for (int j = 0; j < batch.size(); j++) {
                    allRecords.add(new BulkInsertResponseRecord(indexNumber, null, null, null, null, apiException.statusCode(), apiException.getMessage(), requestId));
                    indexNumber++;
                }
            }
        } else {
            int indexNumber = batchNumber > 0 ? batchNumber * batchSize : 0;
            for (int j = 0; j < batch.size(); j++) {
                BulkInsertResponseRecord err = new BulkInsertResponseRecord(indexNumber, null, null, null, null, 500, ex.getMessage(), null);
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
            for (int j = 0; j < batch.getTokens().get().size(); j++) {
                BulkDetokenizeResponseRecord err = new BulkDetokenizeResponseRecord(indexNumber, null, null, null, null, 500, ex.getMessage(), null);
                allRecords.add(err);
                indexNumber++;
            }
        }
        return allRecords;
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
        List<BulkInsertResponseRecord> records = new ArrayList<>();
        if (response != null && response.getRecords().isPresent()) {
            List<V1RecordResponseObject> record = response.getRecords().get();
            int indexNumber = batch * batchSize;
            int recordsSize = record.size();
            for (int index = 0; index < recordsSize; index++) {
                V1RecordResponseObject current = record.get(index);
                records.add(new BulkInsertResponseRecord(
                        indexNumber,
                        current.getTableName().orElse(null),
                        current.getSkyflowId().orElse(null),
                        current.getTokens().orElse(null),
                        current.getHashedData().orElse(null),
                        current.getHttpCode().orElse(current.getError().isPresent() ? 500 : 200),
                        current.getError().orElse(null),
                        null));
                indexNumber++;
            }
            formattedResponse = new BulkInsertResponse(records);
        }
        return formattedResponse;
    }

    public static BulkDetokenizeResponse formatBulkDetokenizeResponse(V1FlowDetokenizeResponse response, int batch, int batchSize, Map<String, List<String>> headers) {
        if (response != null && response.getResponse().isPresent()) {
            List<V1FlowDetokenizeResponseObject> record = response.getResponse().get();
            List<BulkDetokenizeResponseRecord> records = new ArrayList<>();
            int indexNumber = batch * batchSize;
            int recordsSize = record.size();
            for (int index = 0; index < recordsSize; index++) {
                V1FlowDetokenizeResponseObject current = record.get(index);
                records.add(new BulkDetokenizeResponseRecord(
                        indexNumber,
                        current.getToken().orElse(null),
                        current.getValue().orElse(null),
                        current.getTokenGroupName().orElse(null),
                        current.getMetadata().orElse(null),
                        current.getHttpCode().orElse(current.getError().isPresent() ? 500 : 200),
                        current.getError().orElse(null),
                        null));
                indexNumber++;
            }
            return new BulkDetokenizeResponse(records);
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
