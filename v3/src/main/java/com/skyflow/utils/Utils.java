package com.skyflow.utils;

import com.google.gson.JsonObject;
import com.skyflow.enums.Env;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest;
import com.skyflow.generated.rest.types.V1InsertRecordData;
import com.skyflow.generated.rest.types.V1InsertResponse;
import com.skyflow.generated.rest.types.V1RecordResponseObject;
import com.skyflow.generated.rest.types.V1TokenGroupRedactions;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.vault.data.DetokenizeResponse;
import com.skyflow.vault.data.ErrorRecord;
import com.skyflow.vault.data.Success;
import com.skyflow.vault.data.Token;
import com.skyflow.vault.data.DeleteTokensResponse;
import com.skyflow.vault.data.DeleteTokensSuccess;
import com.skyflow.vault.data.TokenizeResponse;
import com.skyflow.vault.data.TokenizeSuccess;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

import java.io.InterruptedIOException;
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

    public static List<List<V1InsertRecordData>> createBatches(List<V1InsertRecordData> records, int batchSize) {
        List<List<V1InsertRecordData>> batches = new ArrayList<>();
        for (int i = 0; i < records.size(); i += batchSize) {
            batches.add(records.subList(i, Math.min(i + batchSize, records.size())));
        }
        return batches;
    }

    public static List<V1FlowDetokenizeRequest> createDetokenizeBatches(V1FlowDetokenizeRequest request, int batchSize) {
        List<V1FlowDetokenizeRequest> detokenizeRequests = new ArrayList<>();
        List<String> tokens = request.getTokens().get();

        for (int i = 0; i < tokens.size(); i += batchSize) {
            // Create a sublist for the current batch
            List<String> batchTokens = tokens.subList(i, Math.min(i + batchSize, tokens.size()));
            List<V1TokenGroupRedactions> tokenGroupRedactions = null;
            if (request.getTokenGroupRedactions().isPresent() && !request.getTokenGroupRedactions().get().isEmpty()) {
                tokenGroupRedactions = request.getTokenGroupRedactions().get();
            }
            // Build a new DetokenizeRequest for the current batch
            V1FlowDetokenizeRequest batchRequest = V1FlowDetokenizeRequest.builder()
                    .vaultId(request.getVaultId())
                    .tokens(new ArrayList<>(batchTokens))
                    .tokenGroupRedactions(tokenGroupRedactions)
                    .build();

            detokenizeRequests.add(batchRequest);
        }

        return detokenizeRequests;
    }

    public static ErrorRecord createErrorRecord(Map<String, Object> recordMap, int indexNumber) {
        return createErrorRecord(recordMap, indexNumber, null);
    }

    public static ErrorRecord createErrorRecord(Map<String, Object> recordMap, int indexNumber, String requestId) {
        ErrorRecord err = null;
        if (recordMap != null) {
            int code = 500;
            if (recordMap.containsKey("http_code")) {
                code = (Integer) recordMap.get("http_code");
            } else if (recordMap.containsKey("httpCode")) {
                code = (Integer) recordMap.get("httpCode");
            } else {
                if (recordMap.containsKey("statusCode")) {
                    code = (Integer) recordMap.get("statusCode");
                }
            }
            String message = recordMap.containsKey("error") ? (String) recordMap.get("error") :
                    recordMap.containsKey("message") ? (String) recordMap.get("message") : "Unknown error";
            err = new ErrorRecord(indexNumber, message, code, requestId);
        }
        return err;
    }

    private static String extractRequestId(Map<String, List<String>> headers) {
        if (headers == null) return null;
        List<String> ids = headers.get(BaseConstants.REQUEST_ID_HEADER_KEY);
        return (ids == null || ids.isEmpty()) ? null : ids.get(0);
    }

    private static final int HTTP_REQUEST_TIMEOUT = 408;
    private static final String REQUEST_TIMED_OUT_MESSAGE = "Request timed out.";

    /**
     * Builds the {@link ErrorRecord} for a network-layer failure (a batch future that
     * completed exceptionally with a non-API exception). A client-side timeout — OkHttp's
     * call timeout surfaces as {@link InterruptedIOException} ({@link java.net.SocketTimeoutException}
     * is a subclass) anywhere in the cause chain — is reported as HTTP 408 with a clean
     * message, so callers can tell a timeout apart from a server 500. Any other network
     * error keeps the existing 500 behaviour.
     */
    private static ErrorRecord networkErrorRecord(Throwable ex, int index) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof InterruptedIOException) {
                return new ErrorRecord(index, REQUEST_TIMED_OUT_MESSAGE, HTTP_REQUEST_TIMEOUT);
            }
        }
        return new ErrorRecord(index, ex.getMessage(), 500);
    }

    public static List<ErrorRecord> handleBatchException(
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
                                ErrorRecord err = Utils.createErrorRecord(recordMap, indexNumber, requestId);
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
                                ? Utils.createErrorRecord(recordMap, indexNumber, requestId)
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
                errorRecords.add(networkErrorRecord(ex, indexNumber));
                indexNumber++;
            }
        }
        return errorRecords;
    }

    public static List<ErrorRecord> handleDetokenizeBatchException(
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
                                ErrorRecord err = Utils.createErrorRecord(recordMap, indexNumber, requestId);
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
                                ? Utils.createErrorRecord(recordMap, indexNumber, requestId)
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
                errorRecords.add(networkErrorRecord(ex, indexNumber));
                indexNumber++;
            }
        }
        return errorRecords;
    }

    public static DetokenizeResponse formatDetokenizeResponse(com.skyflow.generated.rest.types.V1FlowDetokenizeResponse response, int batch, int batchSize) {
        return formatDetokenizeResponse(response, batch, batchSize, null);
    }

    public static DetokenizeResponse formatDetokenizeResponse(com.skyflow.generated.rest.types.V1FlowDetokenizeResponse response, int batch, int batchSize, Map<String, List<String>> headers) {
        if (response != null && response.getResponse().isPresent()) {
            String requestId = extractRequestId(headers);
            List<com.skyflow.generated.rest.types.V1FlowDetokenizeResponseObject> record = response.getResponse().get();
            List<ErrorRecord> errorRecords = new ArrayList<>();
            List<com.skyflow.vault.data.DetokenizeResponseObject> successRecords = new ArrayList<>();
            int indexNumber = batch * batchSize;
            int recordsSize = record.size();
            for (int index = 0; index < recordsSize; index++) {
                if (record.get(index).getError().isPresent()) {
                    ErrorRecord errorRecord = new ErrorRecord(indexNumber, record.get(index).getError().get(), record.get(index).getHttpCode().get(), requestId);
                    errorRecords.add(errorRecord);
                } else {
                    com.skyflow.vault.data.DetokenizeResponseObject success = new com.skyflow.vault.data.DetokenizeResponseObject(indexNumber, record.get(index).getToken().orElse(null), record.get(index).getValue().orElse(null), record.get(index).getTokenGroupName().orElse(null), record.get(index).getError().orElse(null), record.get(index).getMetadata().orElse(null));
                    successRecords.add(success);
                }
                indexNumber++;
            }
            return new DetokenizeResponse(successRecords, errorRecords);
        }
        return null;
    }

    public static com.skyflow.vault.data.InsertResponse formatResponse(V1InsertResponse response, int batch, int batchSize) {
        return formatResponse(response, batch, batchSize, null);
    }

    public static com.skyflow.vault.data.InsertResponse formatResponse(V1InsertResponse response, int batch, int batchSize, Map<String, List<String>> headers) {
        com.skyflow.vault.data.InsertResponse formattedResponse = null;
        List<Success> successRecords = new ArrayList<>();
        List<ErrorRecord> errorRecords = new ArrayList<>();
        if (response != null) {
            String requestId = extractRequestId(headers);
            List<V1RecordResponseObject> record = response.getRecords().get();
            int indexNumber = batch * batchSize;
            int recordsSize = response.getRecords().get().size();
            for (int index = 0; index < recordsSize; index++) {
                if (record.get(index).getError().isPresent()) {
                    ErrorRecord errorRecord = new ErrorRecord(indexNumber, record.get(index).getError().get(), record.get(index).getHttpCode().get(), requestId);
                    errorRecords.add(errorRecord);
                } else {
                    Map<String, List<Token>> tokensMap = null;

                    if (record.get(index).getTokens().isPresent()) {
                        tokensMap = new HashMap<>();
                        Map<String, Object> tok = record.get(index).getTokens().get();
                        for (Map.Entry<String, Object> entry : tok.entrySet()) {
                            String key = entry.getKey();
                            Object value = entry.getValue();
                            List<Token> tokenList = new ArrayList<>();
                            if (value instanceof List) {
                                List<?> valueList = (List<?>) value;
                                for (Object item : valueList) {
                                    if (item instanceof Map) {
                                        Map<String, Object> tokenMap = (Map<String, Object>) item;
                                        Token token = new Token((String) tokenMap.get("token"), (String) tokenMap.get("tokenGroupName"));
                                        tokenList.add(token);
                                    }
                                }
                            }
                            tokensMap.put(key, tokenList);
                        }
                    }
                    Success success = new Success(indexNumber, record.get(index).getSkyflowId().get(), tokensMap, record.get(index).getData().isPresent() ? record.get(index).getData().get() : null, record.get(index).getTableName().isPresent() ? record.get(index).getTableName().get() : null);
                    successRecords.add(success);
                }
                indexNumber++;
            }

            formattedResponse = new com.skyflow.vault.data.InsertResponse(successRecords, errorRecords);
        }
        return formattedResponse;
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

    public static List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest> createDeleteTokensBatches(
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

    public static List<ErrorRecord> handleDeleteTokensBatchException(
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
                                ErrorRecord err = Utils.createErrorRecord(recordMap, indexNumber, requestId);
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
                                ? Utils.createErrorRecord(recordMap, indexNumber, requestId)
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
                errorRecords.add(networkErrorRecord(ex, indexNumber));
                indexNumber++;
            }
        }
        return errorRecords;
    }

    public static DeleteTokensResponse formatDeleteTokensResponse(
            com.skyflow.generated.rest.types.V1FlowDeleteTokenResponse response, int batch, int batchSize) {
        return formatDeleteTokensResponse(response, batch, batchSize, null);
    }

    public static DeleteTokensResponse formatDeleteTokensResponse(
            com.skyflow.generated.rest.types.V1FlowDeleteTokenResponse response, int batch, int batchSize, Map<String, List<String>> headers) {
        if (response != null && response.getTokens().isPresent()) {
            String requestId = extractRequestId(headers);
            List<com.skyflow.generated.rest.types.V1DeleteTokenResponseObject> records = response.getTokens().get();
            List<ErrorRecord> errorRecords = new ArrayList<>();
            List<DeleteTokensSuccess> successRecords = new ArrayList<>();
            int indexNumber = batch * batchSize;
            for (com.skyflow.generated.rest.types.V1DeleteTokenResponseObject record : records) {
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
            return new DeleteTokensResponse(successRecords, errorRecords);
        }
        return null;
    }

    public static List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest> createTokenizeBatches(
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest request, int batchSize) {
        List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest> batches = new ArrayList<>();
        List<com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject> data = request.getData().get();
        for (int i = 0; i < data.size(); i += batchSize) {
            List<com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject> batchData =
                    data.subList(i, Math.min(i + batchSize, data.size()));
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batchRequest =
                    com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.builder()
                            .vaultId(request.getVaultId())
                            .data(new ArrayList<>(batchData))
                            .build();
            batches.add(batchRequest);
        }
        return batches;
    }

    public static List<ErrorRecord> handleTokenizeBatchException(
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
                                ErrorRecord err = Utils.createErrorRecord(recordMap, indexNumber, requestId);
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
                                ? Utils.createErrorRecord(recordMap, indexNumber, requestId)
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
                errorRecords.add(networkErrorRecord(ex, indexNumber));
                indexNumber++;
            }
        }
        return errorRecords;
    }

    public static TokenizeResponse formatTokenizeResponse(
            com.skyflow.generated.rest.types.V1FlowTokenizeResponse response,
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batchRequest,
            int batchNumber, int batchSize) {
        return formatTokenizeResponse(response, batchRequest, batchNumber, batchSize, null);
    }

    public static TokenizeResponse formatTokenizeResponse(
            com.skyflow.generated.rest.types.V1FlowTokenizeResponse response,
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batchRequest,
            int batchNumber, int batchSize, Map<String, List<String>> headers) {
        if (response != null && response.getResponse().isPresent()) {
            String requestId = extractRequestId(headers);
            List<com.skyflow.generated.rest.types.V1FlowTokenizeResponseObject> flatList =
                    response.getResponse().get();
            List<com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject> requestData =
                    batchRequest.getData().isPresent() ? batchRequest.getData().get() : new ArrayList<>();

            List<TokenizeSuccess> successRecords = new ArrayList<>();
            List<ErrorRecord> errorRecords = new ArrayList<>();

            int flatIndex = 0;
            for (int i = 0; i < requestData.size(); i++) {
                int inputRecordIndex = batchNumber * batchSize + i;
                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject reqObj = requestData.get(i);
                int groupCount = reqObj.getTokenGroupNames().isPresent()
                        ? reqObj.getTokenGroupNames().get().size() : 0;
                int entriesToConsume = groupCount > 0 ? groupCount : 1;

                TokenizeSuccess successEntry = null;
                for (int j = 0; j < entriesToConsume && flatIndex < flatList.size(); j++) {
                    com.skyflow.generated.rest.types.V1FlowTokenizeResponseObject obj = flatList.get(flatIndex++);
                    Map<String, Object> props = obj.getAdditionalProperties();
                    Object value = obj.getValue().isPresent() ? obj.getValue().get()
                            : (props != null ? props.get("value") : null);
                    String tokenGroupName = props != null ? (String) props.get("tokenGroupName") : null;
                    String token = props != null ? (String) props.get("token") : null;
                    String errorMsg = (props != null && props.containsKey("error") && props.get("error") != null)
                            ? String.valueOf(props.get("error")) : null;
                    int httpCode = (props != null && props.containsKey("httpCode") && props.get("httpCode") instanceof Number)
                            ? ((Number) props.get("httpCode")).intValue() : 200;

                    if (errorMsg != null) {
                        errorRecords.add(new ErrorRecord(inputRecordIndex, errorMsg, httpCode, requestId));
                    } else {
                        if (successEntry == null) {
                            successEntry = new TokenizeSuccess(inputRecordIndex, value);
                        }
                        successEntry.addToken(tokenGroupName, token);
                    }
                }
                if (successEntry != null) {
                    successRecords.add(successEntry);
                }
            }

            return new TokenizeResponse(successRecords, errorRecords);
        }
        return null;
    }
}
