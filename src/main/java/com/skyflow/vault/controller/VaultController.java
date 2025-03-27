package com.skyflow.vault.controller;

import com.google.gson.*;
import com.skyflow.VaultClient;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.resources.query.requests.QueryServiceExecuteQueryBody;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceBatchOperationBody;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceBulkDeleteRecordBody;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceInsertRecordBody;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceUpdateRecordBody;
import com.skyflow.generated.rest.resources.tokens.requests.V1DetokenizePayload;
import com.skyflow.generated.rest.resources.tokens.requests.V1TokenizePayload;
import com.skyflow.generated.rest.types.*;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.*;
import com.skyflow.vault.tokens.*;

import java.util.*;

public final class VaultController extends VaultClient {
    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    public VaultController(VaultConfig vaultConfig, Credentials credentials) {
        super(vaultConfig, credentials);
    }

    private static synchronized HashMap<String, Object> getFormattedBatchInsertRecord(Object record, int requestIndex) {
        HashMap<String, Object> insertRecord = new HashMap<>();
        String jsonString = gson.toJson(record);
        JsonObject bodyObject = JsonParser.parseString(jsonString).getAsJsonObject().get("Body").getAsJsonObject();
        JsonArray records = bodyObject.getAsJsonArray("records");
        JsonPrimitive error = bodyObject.getAsJsonPrimitive("error");

        if (records != null) {
            for (JsonElement recordElement : records) {
                JsonObject recordObject = recordElement.getAsJsonObject();
                insertRecord.put("skyflowId", recordObject.get("skyflow_id").getAsString());
                JsonElement tokensElement = recordObject.get("tokens");
                if (tokensElement != null) {
                    insertRecord.putAll(tokensElement.getAsJsonObject().asMap());
                }
            }
        }

        if (error != null) {
            insertRecord.put("error", error.getAsString());
        }
        insertRecord.put("requestIndex", requestIndex);
        return insertRecord;
    }

    private static synchronized HashMap<String, Object> getFormattedBulkInsertRecord(V1RecordMetaProperties record) {
        HashMap<String, Object> insertRecord = new HashMap<>();
        String skyflowId = String.valueOf(record.getSkyflowId());
        Object tokens = record.getTokens();
        insertRecord.put("skyflowId", skyflowId);
        if (tokens != null) {
            String tokensString = gson.toJson(tokens);
            JsonObject tokensObject = JsonParser.parseString(tokensString).getAsJsonObject();
            insertRecord.putAll(tokensObject.asMap());
        }
        return insertRecord;
    }

//    private static synchronized HashMap<String, Object> getFormattedGetRecord(V1FieldRecords record) {
//        HashMap<String, Object> getRecord = new HashMap<>();
//        Object fields = record.getFields();
//        Object tokens = record.getTokens();
//        if (fields != null) {
//            String fieldsString = gson.toJson(fields);
//            JsonObject fieldsObject = JsonParser.parseString(fieldsString).getAsJsonObject();
//            getRecord.putAll(fieldsObject.asMap());
//        } else if (tokens != null) {
//            String tokensString = gson.toJson(tokens);
//            JsonObject tokensObject = JsonParser.parseString(tokensString).getAsJsonObject();
//            getRecord.putAll(tokensObject.asMap());
//        }
//        return getRecord;
//    }
//
    private static synchronized HashMap<String, Object> getFormattedUpdateRecord(V1UpdateRecordResponse record) {
        HashMap<String, Object> updateTokens = new HashMap<>();
        Object tokens = record.getTokens();
        if (tokens != null) {
            String tokensString = gson.toJson(tokens);
            JsonObject tokensObject = JsonParser.parseString(tokensString).getAsJsonObject();
            updateTokens.putAll(tokensObject.asMap());
        }
        return updateTokens;
    }

    private static synchronized HashMap<String, Object> getFormattedQueryRecord(V1FieldRecords record) {
        HashMap<String, Object> queryRecord = new HashMap<>();
        Object fields = record.getFields();
        if (fields != null) {
            String fieldsString = gson.toJson(fields);
            JsonObject fieldsObject = JsonParser.parseString(fieldsString).getAsJsonObject();
            queryRecord.putAll(fieldsObject.asMap());
        }
        return queryRecord;
    }

    public InsertResponse insert(InsertRequest insertRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.INSERT_TRIGGERED.getLog());
        V1InsertRecordResponse bulkInsertResult = null;
        V1BatchOperationResponse batchInsertResult = null;
        ArrayList<HashMap<String, Object>> insertedFields = new ArrayList<>();
        ArrayList<HashMap<String, Object>> errorFields = new ArrayList<>();
        Boolean continueOnError = insertRequest.getContinueOnError();
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_INSERT_REQUEST.getLog());
            Validations.validateInsertRequest(insertRequest);
            setBearerToken();
            if (continueOnError) {
                RecordServiceBatchOperationBody insertBody = super.getBatchInsertRequestBody(insertRequest);
                batchInsertResult = super.getRecordsApi().recordServiceBatchOperation(super.getVaultConfig().getVaultId(), insertBody);
                LogUtil.printInfoLog(InfoLogs.INSERT_REQUEST_RESOLVED.getLog());
                Optional<List<Map<String, Object>>> records = batchInsertResult.getResponses();

                if (records.isPresent()) {
                    List<Map<String, Object>> recordList = records.get();

                    for (int index = 0; index < recordList.size(); index++) {
                        Map<String, Object> record = recordList.get(index);
                        HashMap<String, Object> insertRecord = getFormattedBatchInsertRecord(record, index);

                        if (insertRecord.containsKey("skyflowId")) {
                            insertedFields.add(insertRecord);
                        } else {
                            errorFields.add(insertRecord);
                        }
                    }
                }
            } else {
                RecordServiceInsertRecordBody insertBody = super.getBulkInsertRequestBody(insertRequest);
                bulkInsertResult = super.getRecordsApi().recordServiceInsertRecord(
                        super.getVaultConfig().getVaultId(), insertRequest.getTable(), insertBody);
                LogUtil.printInfoLog(InfoLogs.INSERT_REQUEST_RESOLVED.getLog());
                Optional<List<V1RecordMetaProperties>> records = bulkInsertResult.getRecords();
                if (records.isPresent()) {
                    for (V1RecordMetaProperties record : records.get()) {
                        HashMap<String, Object> insertRecord = getFormattedBulkInsertRecord(record);
                        insertedFields.add(insertRecord);
                    }
                }
            }
        } catch (ApiClientApiException e) {
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            throw e;
        }
        LogUtil.printInfoLog(InfoLogs.INSERT_SUCCESS.getLog());
        return new InsertResponse(insertedFields, errorFields);
    }

    public DetokenizeResponse detokenize(DetokenizeRequest detokenizeRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DETOKENIZE_TRIGGERED.getLog());
        V1DetokenizeResponse result = null;
        ArrayList<DetokenizeRecordResponse> detokenizedFields = new ArrayList<>();
        ArrayList<DetokenizeRecordResponse> errorRecords = new ArrayList<>();
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_DETOKENIZE_REQUEST.getLog());
            Validations.validateDetokenizeRequest(detokenizeRequest);
            setBearerToken();
            V1DetokenizePayload payload = super.getDetokenizePayload(detokenizeRequest);
            result = super.getTokensApi().recordServiceDetokenize(super.getVaultConfig().getVaultId(), payload);
            LogUtil.printInfoLog(InfoLogs.DETOKENIZE_REQUEST_RESOLVED.getLog());
//            Map<String, List<String>> responseHeaders = result.getHeaders();
//            String requestId = responseHeaders.get(Constants.REQUEST_ID_HEADER_KEY).get(0);
            Optional<List<V1DetokenizeRecordResponse>> records = result.getRecords();

            if (records.isPresent()) {
                List<V1DetokenizeRecordResponse> recordList = records.get();

                for (V1DetokenizeRecordResponse record : recordList) {
                    if (record.getError().isPresent()) {
                        DetokenizeRecordResponse recordResponse = new DetokenizeRecordResponse(record, "requestId");
                        errorRecords.add(recordResponse);
                    } else {
                        DetokenizeRecordResponse recordResponse = new DetokenizeRecordResponse(record);
                        detokenizedFields.add(recordResponse);
                    }
                }
            }
        } catch (ApiClientApiException e) {
            LogUtil.printErrorLog(ErrorLogs.DETOKENIZE_REQUEST_REJECTED.getLog());
//            throw new SkyflowException(e.getCode(), e, e.getResponseHeaders(), e.getResponseBody());
            throw e;
        }

        if (!errorRecords.isEmpty()) {
            LogUtil.printInfoLog(InfoLogs.DETOKENIZE_PARTIAL_SUCCESS.getLog());
        } else {
            LogUtil.printInfoLog(InfoLogs.DETOKENIZE_SUCCESS.getLog());
        }
        return new DetokenizeResponse(detokenizedFields, errorRecords);
    }

//    public GetResponse get(GetRequest getRequest) throws SkyflowException {
//        LogUtil.printInfoLog(InfoLogs.GET_TRIGGERED.getLog());
//        V1BulkGetRecordResponse result = null;
//        ArrayList<HashMap<String, Object>> data = new ArrayList<>();
//        ArrayList<HashMap<String, Object>> errors = new ArrayList<>();
//        try {
//            LogUtil.printInfoLog(InfoLogs.VALIDATE_GET_REQUEST.getLog());
//            Validations.validateGetRequest(getRequest);
//            setBearerToken();
//            RedactionType redactionType = getRequest.getRedactionType();
//            result = super.getRecordsApi().recordServiceBulkGetRecord(
//                    super.getVaultConfig().getVaultId(),
//                    getRequest.getTable(),
//                    getRequest.getIds(),
//                    redactionType != null ? redactionType.toString() : null,
//                    getRequest.getReturnTokens(),
//                    getRequest.getFields(),
//                    getRequest.getOffset(),
//                    getRequest.getLimit(),
//                    getRequest.getDownloadURL(),
//                    getRequest.getColumnName(),
//                    getRequest.getColumnValues(),
//                    getRequest.getOrderBy()
//            );
//            LogUtil.printInfoLog(InfoLogs.GET_REQUEST_RESOLVED.getLog());
//            List<V1FieldRecords> records = result.getRecords();
//            if (records != null) {
//                for (V1FieldRecords record : records) {
//                    data.add(getFormattedGetRecord(record));
//                }
//            }
//        } catch (ApiException e) {
//            LogUtil.printErrorLog(ErrorLogs.GET_REQUEST_REJECTED.getLog());
//            throw new SkyflowException(e.getCode(), e, e.getResponseHeaders(), e.getResponseBody());
//        }
//        LogUtil.printInfoLog(InfoLogs.GET_SUCCESS.getLog());
//        return new GetResponse(data, errors);
//    }

    public UpdateResponse update(UpdateRequest updateRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.UPDATE_TRIGGERED.getLog());
        V1UpdateRecordResponse result;
        String skyflowId;
        HashMap<String, Object> tokensMap;
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_UPDATE_REQUEST.getLog());
            Validations.validateUpdateRequest(updateRequest);
            setBearerToken();
            RecordServiceUpdateRecordBody updateBody = super.getUpdateRequestBody(updateRequest);
            result = super.getRecordsApi().recordServiceUpdateRecord(
                    super.getVaultConfig().getVaultId(),
                    updateRequest.getTable(),
                    updateRequest.getData().remove("skyflow_id").toString(),
                    updateBody
            );
            LogUtil.printInfoLog(InfoLogs.UPDATE_REQUEST_RESOLVED.getLog());
            skyflowId = String.valueOf(result.getSkyflowId());
            tokensMap = getFormattedUpdateRecord(result);
        } catch (ApiClientApiException e) {
            LogUtil.printErrorLog(ErrorLogs.UPDATE_REQUEST_REJECTED.getLog());
//            throw new SkyflowException(e.getCode(), e, e.getResponseHeaders(), e.getResponseBody());
            throw e;
        }
        LogUtil.printInfoLog(InfoLogs.UPDATE_SUCCESS.getLog());
        return new UpdateResponse(skyflowId, tokensMap);
    }

    public DeleteResponse delete(DeleteRequest deleteRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DELETE_TRIGGERED.getLog());
        V1BulkDeleteRecordResponse result;
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_DELETE_REQUEST.getLog());
            Validations.validateDeleteRequest(deleteRequest);
            setBearerToken();
            RecordServiceBulkDeleteRecordBody deleteBody = RecordServiceBulkDeleteRecordBody.builder().skyflowIds(deleteRequest.getIds())
                                                                    .build();

            result = super.getRecordsApi().recordServiceBulkDeleteRecord(
                    super.getVaultConfig().getVaultId(), deleteRequest.getTable(), deleteBody);
            LogUtil.printInfoLog(InfoLogs.DELETE_REQUEST_RESOLVED.getLog());
        } catch (ApiClientApiException e) {
            LogUtil.printErrorLog(ErrorLogs.DELETE_REQUEST_REJECTED.getLog());
//            throw new SkyflowException(e.getCode(), e, e.getResponseHeaders(), e.getResponseBody());
            throw e;
        }
        LogUtil.printInfoLog(InfoLogs.DELETE_SUCCESS.getLog());
        return new DeleteResponse(result.getRecordIdResponse());
    }

    public Object uploadFile(Object uploadFileRequest) {
        return null;
    }

    public QueryResponse query(QueryRequest queryRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.QUERY_TRIGGERED.getLog());
        V1GetQueryResponse result;
        ArrayList<HashMap<String, Object>> fields = new ArrayList<>();
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_QUERY_REQUEST.getLog());
            Validations.validateQueryRequest(queryRequest);
            setBearerToken();
            result = super.getQueryApi().queryServiceExecuteQuery(
                    super.getVaultConfig().getVaultId(), QueryServiceExecuteQueryBody.builder().query(queryRequest.getQuery()).build());
            LogUtil.printInfoLog(InfoLogs.QUERY_REQUEST_RESOLVED.getLog());
            if (result.getRecords().isPresent()) {
                List<V1FieldRecords> records = result.getRecords().get(); // Extract the List from Optional
                for (V1FieldRecords record : records) {
                    fields.add(getFormattedQueryRecord(record));
                }
            }
        } catch (ApiClientApiException e) {
            LogUtil.printErrorLog(ErrorLogs.QUERY_REQUEST_REJECTED.getLog());
//            throw new SkyflowException(e.getCode(), e, e.getResponseHeaders(), e.getResponseBody());
            throw  e;
        }
        LogUtil.printInfoLog(InfoLogs.QUERY_SUCCESS.getLog());
        return new QueryResponse(fields);
    }

    public TokenizeResponse tokenize(TokenizeRequest tokenizeRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.TOKENIZE_TRIGGERED.getLog());
        V1TokenizeResponse result = null;
        List<String> list = new ArrayList<>();
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_TOKENIZE_REQUEST.getLog());
            Validations.validateTokenizeRequest(tokenizeRequest);
            setBearerToken();
            V1TokenizePayload payload = super.getTokenizePayload(tokenizeRequest);
            result = super.getTokensApi().recordServiceTokenize(super.getVaultConfig().getVaultId(), payload);
            LogUtil.printInfoLog(InfoLogs.TOKENIZE_REQUEST_RESOLVED.getLog());
            if (result != null && result.getRecords().isPresent() && !result.getRecords().get().isEmpty()) {
                for (V1TokenizeRecordResponse response : result.getRecords().get()) {
                    if (response.getToken().isPresent()) {
                        list.add(response.getToken().get());
                    }
                }
            }
        } catch (ApiClientApiException e) {
            LogUtil.printErrorLog(ErrorLogs.TOKENIZE_REQUEST_REJECTED.getLog());
//            throw new SkyflowException(e.getCode(), e, e.getResponseHeaders(), e.getResponseBody());
            throw e;
        }
        LogUtil.printInfoLog(InfoLogs.TOKENIZE_SUCCESS.getLog());
        return new TokenizeResponse(list);
    }

}
