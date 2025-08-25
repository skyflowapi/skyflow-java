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
        JsonObject details = new JsonObject();
        String sdkVersion = Constants.SDK_VERSION;
        String deviceModel;
        String osDetails;
        String javaVersion;
        // Retrieve device model
        try {
            deviceModel = System.getProperty("os.name");
            if (deviceModel == null) throw new Exception();
        } catch (Exception e) {
            LogUtil.printInfoLog(parameterizedString(
                    InfoLogs.UNABLE_TO_GENERATE_SDK_METRIC.getLog(),
                    Constants.SDK_METRIC_CLIENT_DEVICE_MODEL
            ));
            deviceModel = "";
        }

        // Retrieve OS details
        try {
            osDetails = System.getProperty("os.version");
            if (osDetails == null) throw new Exception();
        } catch (Exception e) {
            LogUtil.printInfoLog(parameterizedString(
                    InfoLogs.UNABLE_TO_GENERATE_SDK_METRIC.getLog(),
                    Constants.SDK_METRIC_CLIENT_OS_DETAILS
            ));
            osDetails = "";
        }

        // Retrieve Java version details
        try {
            javaVersion = System.getProperty("java.version");
            if (javaVersion == null) throw new Exception();
        } catch (Exception e) {
            LogUtil.printInfoLog(parameterizedString(
                    InfoLogs.UNABLE_TO_GENERATE_SDK_METRIC.getLog(),
                    Constants.SDK_METRIC_RUNTIME_DETAILS
            ));
            javaVersion = "";
        }
        details.addProperty(Constants.SDK_METRIC_NAME_VERSION, Constants.SDK_METRIC_NAME_VERSION_PREFIX + sdkVersion);
        details.addProperty(Constants.SDK_METRIC_CLIENT_DEVICE_MODEL, deviceModel);
        details.addProperty(Constants.SDK_METRIC_RUNTIME_DETAILS, Constants.SDK_METRIC_RUNTIME_DETAILS_PREFIX + javaVersion);
        details.addProperty(Constants.SDK_METRIC_CLIENT_OS_DETAILS, osDetails);
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
            int indexNumber = batchNumber > 0 ? batchNumber * batches.get(batchNumber - 1).size() : 0;

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
            int indexNumber = batchNumber > 0 ? batchNumber * batches.get(batchNumber - 1).size() : 0;
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
    public static com.skyflow.vault.data.InsertResponse formatResponse(InsertResponse response, int batch, int batchSize){
        com.skyflow.vault.data.InsertResponse response1 = new com.skyflow.vault.data.InsertResponse();
        List<Success> successRecords = new ArrayList<>();
        List<ErrorRecord> errorRecords = new ArrayList<>();
        if (response != null) {
            List<RecordResponseObject> record = response.getRecords().get();
            int indexNumber = (batch) * batchSize;
            for(int index = 0; index < response.getRecords().get().size(); index++) {
                if (record.get(index).getError().isPresent()){
                    ErrorRecord errorRecord = new ErrorRecord();
                    errorRecord.setIndex(indexNumber);
                    errorRecord.setError(record.get(index).getError().get());
                    errorRecord.setCode(record.get(index).getHttpCode().get());
                    errorRecords.add(errorRecord);
//                    errorRecord.setCode(record.get(index).getError().get().getCode());
                } else {
                    Success success = new Success();
                    success.setIndex(indexNumber);
                    success.setSkyflowId(record.get(index).getSkyflowId().get());
//                    success.setData(record.get(index).getData().get());
                    if(record.get(index).getTokens().isPresent()) {
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
            response1.setSuccess(successRecords);
            response1.setErrors(errorRecords);
        }
        return response1;
    }

}
