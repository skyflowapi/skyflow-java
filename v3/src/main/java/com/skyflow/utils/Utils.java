package com.skyflow.utils;

import com.google.gson.JsonObject;
import com.skyflow.enums.Env;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.types.InsertRecordData;
import com.skyflow.generated.rest.types.InsertResponse;
import com.skyflow.generated.rest.types.RecordResponseObject;
import com.skyflow.vault.data.ErrorRecord;
import com.skyflow.vault.data.Success;
import com.skyflow.vault.data.Token;

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

    public static ErrorRecord createErrorRecord(Map<String, Object> recordMap, int indexNumber) {
        ErrorRecord err = null;
        if (recordMap != null) {
            int code = recordMap.containsKey("http_code") ? (Integer) recordMap.get("http_code") : 500;
            String message = recordMap.containsKey("error") ? (String) recordMap.get("error") :
                    recordMap.containsKey("message") ? (String) recordMap.get("message") : "Unknown error";
            err = new ErrorRecord(indexNumber, message, code);
        }
        return err;
    }

    public static List<ErrorRecord> handleBatchException(
            Throwable ex, List<InsertRecordData> batch, int batchNumber, List<List<InsertRecordData>> batches
    ) {
        List<ErrorRecord> errorRecords = new ArrayList<>();
        Throwable cause = ex.getCause();
        if (cause instanceof ApiClientApiException) {
            ApiClientApiException apiException = (ApiClientApiException) cause;
            Map<String, Object> responseBody = (Map<String, Object>) apiException.body();
            int indexNumber = batchNumber > 0 ? batchNumber * batch.size() : 0;
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
            int indexNumber = batchNumber > 0 ? batchNumber * batch.size() : 0;
            for (int j = 0; j < batch.size(); j++) {
                ErrorRecord err = new ErrorRecord(indexNumber, ex.getMessage(), 500);
                errorRecords.add(err);
                indexNumber++;
            }
        }
        return errorRecords;
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
                                    if(item instanceof Map) {
                                        Map<String, Object> tokenMap = (Map<String, Object>) item;
                                        Token token = new Token((String) tokenMap.get("token"), (String) tokenMap.get("tokenGroupName"));
                                        tokenList.add(token);
                                    }
                                }
                            }
                            tokensMap.put(key, tokenList);
                        }
                    }
                    Success success = new Success(index, record.get(index).getSkyflowId().get(), tokensMap, null);
                    successRecords.add(success);
                }
                indexNumber++;
            }

            formattedResponse = new com.skyflow.vault.data.InsertResponse(successRecords, errorRecords);
        }
        return formattedResponse;
    }

}
