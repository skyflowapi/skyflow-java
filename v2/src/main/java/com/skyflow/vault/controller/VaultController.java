package com.skyflow.vault.controller;

import com.google.gson.*;
import com.google.gson.internal.LinkedTreeMap;
import com.skyflow.VaultClient;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiException;
import com.skyflow.generated.rest.auth.HttpBearerAuth;
import com.skyflow.generated.rest.models.*;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.utils.Utils;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.*;
import com.skyflow.vault.tokens.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VaultController extends VaultClient {
    private DetectController detectController;
    private AuditController auditController;
    private BinLookupController binLookupController;

    private String token;

    public VaultController(VaultConfig vaultConfig, Credentials credentials) {
        super(vaultConfig, credentials);
        this.auditController = null;
        this.binLookupController = null;
        this.detectController = null;
    }

    private static synchronized HashMap<String, Object> getFormattedBatchInsertRecord(Object record, int requestIndex) {
        HashMap<String, Object> insertRecord = new HashMap<>();
        Gson gson = new Gson();
        String jsonString = gson.toJson(record);
        JsonObject bodyObject = JsonParser.parseString(jsonString).getAsJsonObject().get("Body").getAsJsonObject();
        JsonArray records = bodyObject.getAsJsonArray("records");
        JsonPrimitive error = bodyObject.getAsJsonPrimitive("error");

        if (records != null) {
            for (JsonElement recordElement : records) {
                JsonObject recordObject = recordElement.getAsJsonObject();
                insertRecord.put("skyflow_id", recordObject.get("skyflow_id").getAsString());

                Map<String, JsonElement> tokensMap = recordObject.get("tokens").getAsJsonObject().asMap();
                for (String key : tokensMap.keySet()) {
                    insertRecord.put(key, tokensMap.get(key));
                }
            }
        }

        if (error != null) {
            insertRecord.put("error", error.getAsString());
        }
        insertRecord.put("request_index", requestIndex);
        return insertRecord;
    }

    private static synchronized HashMap<String, Object> getFormattedBulkInsertRecord(V1RecordMetaProperties record) {
        HashMap<String, Object> insertRecord = new HashMap<>();
        String skyflowId = record.getSkyflowId();
        insertRecord.put("skyflowId", skyflowId);

        /*
        Getting unchecked cast warning, however, this type is inferred
        from an exception trying to cast into another type. Therefore,
        this type cast will not fail.
        */
        LinkedTreeMap<String, Object> tokensMap = (LinkedTreeMap<String, Object>) record.getTokens();
        if (tokensMap != null) {
            for (String key : tokensMap.keySet()) {
                insertRecord.put(key, tokensMap.get(key));
            }
        }
        return insertRecord;
    }

    private static synchronized HashMap<String, Object> getFormattedGetRecord(V1FieldRecords record) {
        HashMap<String, Object> getRecord = new HashMap<>();

        /*
        Getting unchecked cast warning, however, this type is inferred
        from an exception trying to cast into another type. Therefore,
        this type cast will not fail.
        */
        LinkedTreeMap<String, Object> map;
        if (record.getTokens() != null) {
            map = (LinkedTreeMap<String, Object>) record.getTokens();
        } else {
            map = (LinkedTreeMap<String, Object>) record.getFields();
        }
        if (map != null) {
            for (String key : map.keySet()) {
                getRecord.put(key, map.get(key));
            }
        }
        return getRecord;
    }

    private static synchronized HashMap<String, Object> getFormattedUpdateRecord(V1UpdateRecordResponse record) {
        HashMap<String, Object> updateTokens = new HashMap<>();
        /*
        Getting unchecked cast warning, however, this type is inferred
        from an exception trying to cast into another type. Therefore,
        this type cast will not fail.
        */
        LinkedTreeMap<String, Object> map = null;
        if (record.getTokens() != null) {
            map = (LinkedTreeMap<String, Object>) record.getTokens();
        }
        if (map != null) {
            for (String key : map.keySet()) {
                updateTokens.put(key, map.get(key));
            }
        }
        return updateTokens;
    }

    private static synchronized HashMap<String, Object> getFormattedQueryRecord(V1FieldRecords record) {
        HashMap<String, Object> queryRecord = new HashMap<>();
        /*
        Getting unchecked cast warning, however, this type is inferred
        from an exception trying to cast into another type. Therefore,
        this type cast will not fail.
        */
        LinkedTreeMap<String, Object> map = null;
        if (record.getFields() != null) {
            map = (LinkedTreeMap<String, Object>) record.getFields();
        }
        if (map != null) {
            for (String key : map.keySet()) {
                queryRecord.put(key, map.get(key));
            }
        }
        return queryRecord;
    }

    public InsertResponse insert(InsertRequest insertRequest) throws SkyflowException {
        V1InsertRecordResponse bulkInsertResult = null;
        V1BatchOperationResponse batchInsertResult = null;
        ArrayList<HashMap<String, Object>> insertedFields = new ArrayList<>();
        ArrayList<HashMap<String, Object>> errorFields = new ArrayList<>();
        Boolean continueOnError = insertRequest.getContinueOnError();
        try {
            Validations.validateInsertRequest(insertRequest);
            setBearerToken();
            if (continueOnError) {
                RecordServiceBatchOperationBody insertBody = super.getBatchInsertRequestBody(insertRequest);
                batchInsertResult = super.getRecordsApi().recordServiceBatchOperation(super.getVaultConfig().getVaultId(), insertBody);
                List<Object> records = batchInsertResult.getResponses();
                for (int index = 0; index < records.size(); index++) {
                    Object record = records.get(index);
                    HashMap<String, Object> insertRecord = getFormattedBatchInsertRecord(record, index);
                    if (insertRecord.containsKey("skyflow_id")) {
                        insertedFields.add(insertRecord);
                    } else {
                        errorFields.add(insertRecord);
                    }
                }
            } else {
                RecordServiceInsertRecordBody insertBody = super.getBulkInsertRequestBody(insertRequest);
                bulkInsertResult = super.getRecordsApi().recordServiceInsertRecord(
                        super.getVaultConfig().getVaultId(), insertRequest.getTable(), insertBody);
                List<V1RecordMetaProperties> records = bulkInsertResult.getRecords();
                if (records != null) {
                    for (V1RecordMetaProperties record : records) {
                        HashMap<String, Object> insertRecord = getFormattedBulkInsertRecord(record);
                        insertedFields.add(insertRecord);
                    }
                }
            }
        } catch (ApiException e) {
            throw new SkyflowException(e.getCode(), e, e.getResponseHeaders(), e.getResponseBody());
        }
        return new InsertResponse(insertedFields, errorFields);
    }

    public DetokenizeResponse detokenize(DetokenizeRequest detokenizeRequest) throws SkyflowException {
        V1DetokenizeResponse result = null;
        ArrayList<DetokenizeRecordResponse> detokenizedFields = new ArrayList<>();
        ArrayList<DetokenizeRecordResponse> errorRecords = new ArrayList<>();
        try {
            Validations.validateDetokenizeRequest(detokenizeRequest);
            setBearerToken();
            V1DetokenizePayload payload = super.getDetokenizePayload(detokenizeRequest);
            result = super.getTokensApi().recordServiceDetokenize(super.getVaultConfig().getVaultId(), payload);
            List<V1DetokenizeRecordResponse> records = result.getRecords();
            if (records != null) {
                for (V1DetokenizeRecordResponse record : records) {
                    DetokenizeRecordResponse recordResponse = new DetokenizeRecordResponse(record);
                    if (record.getError() != null) {
                        errorRecords.add(recordResponse);
                    } else {
                        detokenizedFields.add(recordResponse);
                    }
                }
            }
        } catch (ApiException e) {
            throw new SkyflowException(e.getCode(), e, e.getResponseHeaders(), e.getResponseBody());
        }

        if (!errorRecords.isEmpty()) {
            // handle partial case, throw error and send data in error
            // or simply log as a partial success and return proper response
        }
        return new DetokenizeResponse(detokenizedFields, errorRecords);
    }

    public GetResponse get(GetRequest getRequest) throws SkyflowException {
        V1BulkGetRecordResponse result = null;
        ArrayList<HashMap<String, Object>> data = new ArrayList<>();
        ArrayList<HashMap<String, Object>> errors = new ArrayList<>();
        try {
            Validations.validateGetRequest(getRequest);
            setBearerToken();
            RedactionType redactionType = getRequest.getRedactionType();
            result = super.getRecordsApi().recordServiceBulkGetRecord(
                    super.getVaultConfig().getVaultId(),
                    getRequest.getTable(),
                    getRequest.getIds(),
                    redactionType != null ? redactionType.toString() : null,
                    getRequest.getTokenization(),
                    getRequest.getFields(),
                    getRequest.getOffset(),
                    getRequest.getLimit(),
                    getRequest.getDownloadURL(),
                    getRequest.getColumnName(),
                    getRequest.getColumnValues(),
                    getRequest.getOrderBy()
            );
            List<V1FieldRecords> records = result.getRecords();
            if (records != null) {
                for (V1FieldRecords record : records) {
                    data.add(getFormattedGetRecord(record));
                }
            }
        } catch (ApiException e) {
            throw new SkyflowException(e.getCode(), e, e.getResponseHeaders(), e.getResponseBody());
        }
        return new GetResponse(data, errors);
    }

    public UpdateResponse update(UpdateRequest updateRequest) throws SkyflowException {
        V1UpdateRecordResponse result;
        String skyflowId;
        HashMap<String, Object> tokensMap;
        try {
            Validations.validateUpdateRequest(updateRequest);
            setBearerToken();
            RecordServiceUpdateRecordBody updateBody = super.getUpdateRequestBody(updateRequest);
            result = super.getRecordsApi().recordServiceUpdateRecord(
                    super.getVaultConfig().getVaultId(),
                    updateRequest.getTable(),
                    updateRequest.getId(),
                    updateBody
            );
            skyflowId = result.getSkyflowId();
            tokensMap = getFormattedUpdateRecord(result);
        } catch (ApiException e) {
            throw new SkyflowException(e.getCode(), e, e.getResponseHeaders(), e.getResponseBody());
        }
        return new UpdateResponse(skyflowId, tokensMap);
    }

    public DeleteResponse delete(DeleteRequest deleteRequest) throws SkyflowException {
        V1BulkDeleteRecordResponse result;
        try {
            Validations.validateDeleteRequest(deleteRequest);
            setBearerToken();
            RecordServiceBulkDeleteRecordBody deleteBody = new RecordServiceBulkDeleteRecordBody();
            for (String id : deleteRequest.getIds()) {
                deleteBody.addSkyflowIdsItem(id);
            }
            result = super.getRecordsApi().recordServiceBulkDeleteRecord(
                    super.getVaultConfig().getVaultId(), deleteRequest.getTable(), deleteBody);
        } catch (ApiException e) {
            throw new SkyflowException(e.getCode(), e, e.getResponseHeaders(), e.getResponseBody());
        }
        return new DeleteResponse((ArrayList<String>) result.getRecordIDResponse());
    }

    public Object uploadFile(Object uploadFileRequest) {
        return null;
    }

    public QueryResponse query(QueryRequest queryRequest) throws SkyflowException {
        V1GetQueryResponse result;
        ArrayList<HashMap<String, Object>> fields = new ArrayList<>();
        try {
            Validations.validateQueryRequest(queryRequest);
            setBearerToken();
            result = super.getQueryApi().queryServiceExecuteQuery(
                    super.getVaultConfig().getVaultId(), new QueryServiceExecuteQueryBody().query(queryRequest.getQuery()));
            if (result.getRecords() != null) {
                for (V1FieldRecords record : result.getRecords()) {
                    fields.add(getFormattedQueryRecord(record));
                }
            }
        } catch (ApiException e) {
            throw new SkyflowException(e.getCode(), e, e.getResponseHeaders(), e.getResponseBody());
        }
        return new QueryResponse(fields);
    }

    public TokenizeResponse tokenize(TokenizeRequest tokenizeRequest) throws SkyflowException {
        V1TokenizeResponse result = null;
        List<String> list = new ArrayList<>();
        try {
            Validations.validateTokenizeRequest(tokenizeRequest);
            setBearerToken();
            V1TokenizePayload payload = super.getTokenizePayload(tokenizeRequest);
            result = super.getTokensApi().recordServiceTokenize(super.getVaultConfig().getVaultId(), payload);
            if (result != null && result.getRecords().size() > 0) {

                for (V1TokenizeRecordResponse response : result.getRecords()) {
                    if (response.getToken() != null) {
                        list.add(response.getToken());
                    }
                }
            }
        } catch (ApiException e) {
            throw new SkyflowException(e.getResponseBody());
        }
        return new TokenizeResponse(list);
    }

    public BinLookupController lookUpBin() {
        if (this.binLookupController == null) {
            this.binLookupController = new BinLookupController(super.getApiClient());
        }
        return this.binLookupController;
    }

    public AuditController audit() {
        if (this.auditController == null) {
            this.auditController = new AuditController(super.getApiClient());
        }
        return this.auditController;
    }

    public DetectController detect() {
        if (this.detectController == null) {
            this.detectController = new DetectController(super.getApiClient());
        }
        return this.detectController;
    }

    private void setBearerToken() throws SkyflowException {
        Validations.validateCredentials(super.getFinalCredentials());
        if (token == null || Token.isExpired(token)) {
            token = Utils.generateBearerToken(super.getFinalCredentials());
        }
        HttpBearerAuth Bearer = (HttpBearerAuth) super.getApiClient().getAuthentication("Bearer");
        Bearer.setBearerToken(token);
    }
}
