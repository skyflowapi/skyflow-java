package com.skyflow.utils;

import com.google.gson.JsonObject;
import com.skyflow.enums.Env;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest;
import com.skyflow.generated.rest.types.InsertRecordData;
import com.skyflow.generated.rest.types.InsertResponse;
import com.skyflow.generated.rest.types.RecordResponseObject;
import com.skyflow.generated.rest.types.TokenGroupRedactions;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.vault.data.DetokenizeResponse;
import com.skyflow.vault.data.ErrorRecord;
import com.skyflow.vault.data.Success;
import com.skyflow.vault.data.Token;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

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

    public static List<List<InsertRecordData>> createBatches(List<InsertRecordData> records, int batchSize) {
        List<List<InsertRecordData>> batches = new ArrayList<>();
        for (int i = 0; i < records.size(); i += batchSize) {
            batches.add(records.subList(i, Math.min(i + batchSize, records.size())));
        }
        return batches;
    }

    public static List<DetokenizeRequest> createDetokenizeBatches(DetokenizeRequest request, int batchSize) {
        List<DetokenizeRequest> detokenizeRequests = new ArrayList<>();
        List<String> tokens = request.getTokens().get();

        for (int i = 0; i < tokens.size(); i += batchSize) {
            // Create a sublist for the current batch
            List<String> batchTokens = tokens.subList(i, Math.min(i + batchSize, tokens.size()));
            List<TokenGroupRedactions> tokenGroupRedactions = null;
            if (request.getTokenGroupRedactions().isPresent() && !request.getTokenGroupRedactions().get().isEmpty()) {
                tokenGroupRedactions = request.getTokenGroupRedactions().get();
            }
            // Build a new DetokenizeRequest for the current batch
            DetokenizeRequest batchRequest = DetokenizeRequest.builder()
                    .vaultId(request.getVaultId())
                    .tokens(new ArrayList<>(batchTokens))
                    .tokenGroupRedactions(tokenGroupRedactions)
                    .build();

            detokenizeRequests.add(batchRequest);
        }

        return detokenizeRequests;
    }

    public static ErrorRecord createErrorRecord(Map<String, Object> recordMap, int indexNumber) {
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
            err = new ErrorRecord(indexNumber, message, code);
        }
        return err;
    }

    public static List<ErrorRecord> handleBatchException(
            Throwable ex, List<InsertRecordData> batch, int batchNumber, int batchSize
    ) {
        List<ErrorRecord> errorRecords = new ArrayList<>();
        Throwable cause = ex.getCause();
        if (cause instanceof ApiClientApiException) {
            ApiClientApiException apiException = (ApiClientApiException) cause;
            Map<String, Object> responseBody = (Map<String, Object>) apiException.body();
            int indexNumber = batchNumber > 0 ? batchNumber * batchSize : 0;
            if (responseBody != null) {
                if (responseBody.containsKey("records")) {
                    Object recordss = responseBody.get("records");
                    if (recordss instanceof List) {
                        List<?> recordsList = (List<?>) recordss;
                        for (Object record : recordsList) {
                            if (record instanceof Map) {
                                Map<String, Object> recordMap = (Map<String, Object>) record;
                                ErrorRecord err = Utils.createErrorRecord(recordMap, indexNumber);
                                errorRecords.add(err);
                                indexNumber++;
                            }
                        }
                    }
                } else if (responseBody.containsKey("error")) {
                    Map<String, Object> recordMap = (Map<String, Object>) responseBody.get("error");
                    for (int j = 0; j < batch.size(); j++) {
                        ErrorRecord err = Utils.createErrorRecord(recordMap, indexNumber);
                        errorRecords.add(err);
                        indexNumber++;
                    }
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

    public static List<ErrorRecord> handleDetokenizeBatchException(
            Throwable ex, DetokenizeRequest batch, int batchNumber, int batchSize
    ) {
        List<ErrorRecord> errorRecords = new ArrayList<>();
        Throwable cause = ex.getCause();
        if (cause instanceof ApiClientApiException) {
            ApiClientApiException apiException = (ApiClientApiException) cause;
            Map<String, Object> responseBody = (Map<String, Object>) apiException.body();
            int indexNumber = batchNumber * batchSize;
            if (responseBody != null) {
                if (responseBody.containsKey("response")) {
                    Object recordss = responseBody.get("response");
                    if (recordss instanceof List) {
                        List<?> recordsList = (List<?>) recordss;
                        for (Object record : recordsList) {
                            if (record instanceof Map) {
                                Map<String, Object> recordMap = (Map<String, Object>) record;
                                ErrorRecord err = Utils.createErrorRecord(recordMap, indexNumber);
                                errorRecords.add(err);
                                indexNumber++;
                            }
                        }
                    }
                } else if (responseBody.containsKey("error")) {
                    Map<String, Object> recordMap = (Map<String, Object>) responseBody.get("error");
                    for (int j = 0; j < batch.getTokens().get().size(); j++) {
                        ErrorRecord err = Utils.createErrorRecord(recordMap, indexNumber);
                        errorRecords.add(err);
                        indexNumber++;
                    }
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

    public static DetokenizeResponse formatDetokenizeResponse(com.skyflow.generated.rest.types.DetokenizeResponse response, int batch, int batchSize) {
        if (response != null) {
            List<com.skyflow.generated.rest.types.DetokenizeResponseObject> record = response.getResponse().get();
            List<ErrorRecord> errorRecords = new ArrayList<>();
            List<com.skyflow.vault.data.DetokenizeResponseObject> successRecords = new ArrayList<>();
            int indexNumber = batch * batchSize;
            int recordsSize = response.getResponse().get().size();
            for (int index = 0; index < recordsSize; index++) {
                if (record.get(index).getError().isPresent()) {
                    ErrorRecord errorRecord = new ErrorRecord(indexNumber, record.get(index).getError().get(), record.get(index).getHttpCode().get());
                    errorRecords.add(errorRecord);
                } else {
                    com.skyflow.vault.data.DetokenizeResponseObject success = new com.skyflow.vault.data.DetokenizeResponseObject(indexNumber, record.get(index).getToken().orElse(null), record.get(index).getValue().orElse(null), record.get(index).getTokenGroupName().orElse(null), record.get(index).getError().orElse(null), record.get(index).getMetadata().orElse(null));
                    successRecords.add(success);
                }
                indexNumber++;
            }
            DetokenizeResponse formattedResponse = new DetokenizeResponse(successRecords, errorRecords);
            return formattedResponse;
        }
        return null;
    }

    public static com.skyflow.vault.data.InsertResponse formatResponse(InsertResponse response, int batch, int batchSize) {
        com.skyflow.vault.data.InsertResponse formattedResponse = null;
        List<Success> successRecords = new ArrayList<>();
        List<ErrorRecord> errorRecords = new ArrayList<>();
        if (response != null) {
            List<RecordResponseObject> record = response.getRecords().get();
            int indexNumber = batch * batchSize;
            int recordsSize = response.getRecords().get().size();
            for (int index = 0; index < recordsSize; index++) {
                if (record.get(index).getError().isPresent()) {
                    ErrorRecord errorRecord = new ErrorRecord(indexNumber, record.get(index).getError().get(), record.get(index).getHttpCode().get());
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
}
