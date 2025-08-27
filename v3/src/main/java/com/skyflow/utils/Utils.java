package com.skyflow.utils;

import com.google.gson.JsonObject;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.types.InsertRecordData;
import com.skyflow.generated.rest.types.InsertResponse;
import com.skyflow.generated.rest.types.RecordResponseObject;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.vault.data.ErrorRecord;
import com.skyflow.vault.data.Success;
import com.skyflow.vault.data.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Utils extends BaseUtils {

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
        ErrorRecord err = new ErrorRecord();
        err.setIndex(indexNumber);
        if (recordMap.containsKey("error")) {
            err.setError((String) recordMap.get("error"));
        }
        if (recordMap.containsKey("message")) {
            err.setError((String) recordMap.get("message"));
        }
        if (recordMap.containsKey("http_code")) {
            err.setCode((Integer) recordMap.get("http_code"));
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
                ErrorRecord err = new ErrorRecord();
                err.setIndex(indexNumber);
                err.setError(ex.getMessage());
                err.setCode(500);
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
//                    errorRecord.setIndex(indexNumber);
//                    errorRecord.setError(record.get(index).getError().get());
//                    errorRecord.setCode(record.get(index).getHttpCode().get());
                    errorRecords.add(errorRecord);
//                    errorRecord.setCode(record.get(index).getError().get().getCode());
                } else {
                    Success success = new Success(index, record.get(index).getSkyflowId().get(), null, null);
//                    success.setIndex(indexNumber);
//                    success.setSkyflowId(record.get(index).getSkyflowId().get());
//                    success.setData(record.get(index).getData().get());
                    if (record.get(index).getTokens().isPresent()) {
                        List<Token> tokens = null;
                        Map<String, Object> tok = record.get(index).getTokens().get();
                        for (int i = 0; i < tok.size(); i++) {
                            Token token = new Token();
                            Object obj = tok.get(i);
//                            token.setToken();
//                            token.setTokenGroupName("");
                        }
                    }
//                    success.setTokens(record.get(index).getTokens().get());

                    successRecords.add(success);
                }
                indexNumber++;
            }

            formattedResponse = new com.skyflow.vault.data.InsertResponse(successRecords, errorRecords);
//            formattedResponse.setSuccessRecords(successRecords);
//            formattedResponse.setErrorRecords(errorRecords);
        }
        return formattedResponse;
    }

}
