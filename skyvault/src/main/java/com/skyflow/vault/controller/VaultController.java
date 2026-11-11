package com.skyflow.vault.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.skyflow.VaultClient;
import com.skyflow.config.BaseVaultConfig;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.core.ApiClientException;
import com.skyflow.generated.rest.core.ApiClientHttpResponse;
import com.skyflow.generated.rest.core.RequestOptions;
import com.skyflow.generated.rest.resources.files.requests.UploadFileRequestV2;
import com.skyflow.generated.rest.resources.query.requests.GetQueryRequest;
import com.skyflow.generated.rest.resources.records.requests.BatchOperationRequest;
import com.skyflow.generated.rest.resources.records.requests.BulkDeleteRecordRequest;
import com.skyflow.generated.rest.resources.records.requests.GetRecordsRequest;
import com.skyflow.generated.rest.resources.records.requests.InsertRecordRequest;
import com.skyflow.generated.rest.resources.records.requests.UpdateRecordRequest;
import com.skyflow.generated.rest.resources.records.types.GetRecordsRequestOrderBy;
import com.skyflow.generated.rest.resources.records.types.GetRecordsRequestRedaction;
import com.skyflow.generated.rest.resources.tokens.requests.DetokenizePayload;
import com.skyflow.generated.rest.resources.tokens.requests.TokenizePayload;
import com.skyflow.generated.rest.types.BatchOperationResponse;
import com.skyflow.generated.rest.types.BulkDeleteRecordResponse;
import com.skyflow.generated.rest.types.BulkGetRecordResponse;
import com.skyflow.generated.rest.types.FieldRecords;
import com.skyflow.generated.rest.types.GetFieldRecords;
import com.skyflow.generated.rest.types.GetQueryResponse;
import com.skyflow.generated.rest.types.InsertRecordResponse;
import com.skyflow.generated.rest.types.RecordMetaProperties;
import com.skyflow.generated.rest.types.TokenizeRecordResponse;
import com.skyflow.generated.rest.types.UpdateRecordResponse;
import com.skyflow.generated.rest.types.UploadFileResponseV2;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.DeleteRequest;
import com.skyflow.vault.data.DeleteResponse;
import com.skyflow.vault.data.FileUploadRequest;
import com.skyflow.vault.data.FileUploadResponse;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.QueryResponse;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateResponse;
import com.skyflow.vault.tokens.DetokenizeRecordResponse;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.DetokenizeResponse;
import com.skyflow.vault.tokens.TokenizeRequest;
import com.skyflow.vault.tokens.TokenizeResponse;

public final class VaultController extends VaultClient
        implements IVaultController<InsertRequest, InsertResponse, DetokenizeRequest, DetokenizeResponse> {
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final JsonObject SKY_METADATA = Utils.getMetrics();

    private static RequestOptions buildRequestOptions() {
        return RequestOptions.builder().addHeader(Constants.SDK_METRICS_HEADER_KEY, SKY_METADATA.toString()).build();
    }

    public VaultController(VaultConfig vaultConfig, Credentials credentials) {
        super(vaultConfig, credentials);
    }

    private static String extractUpdateSkyflowId(HashMap<String, Object> data) {
        if (data.containsKey("skyflowId")) {
            if (data.containsKey("skyflow_id")) {
                data.remove("skyflow_id");
                LogUtil.printWarningLog(InfoLogs.DEPRECATED_SKYFLOW_ID_REQUEST_KEY.getLog());
            }
            return data.remove("skyflowId").toString();
        }
        LogUtil.printWarningLog(InfoLogs.DEPRECATED_SKYFLOW_ID_REQUEST_KEY.getLog());
        return data.remove("skyflow_id").toString();
    }

    private static String extractRequestId(Map<String, List<String>> headers) {
        List<String> values = headers != null ? headers.get(Constants.REQUEST_ID_HEADER_KEY) : null;
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    private static synchronized HashMap<String, Object> getFormattedBatchInsertRecord(Object record, Integer requestIndex) {
        HashMap<String, Object> insertRecord = new HashMap<>();
        String jsonString = GSON.toJson(record);
        JsonElement bodyElement = JsonParser.parseString(jsonString).getAsJsonObject().get("Body");
        JsonObject bodyObject = bodyElement != null && bodyElement.isJsonObject() ? bodyElement.getAsJsonObject() : new JsonObject();
        JsonArray records = bodyObject.getAsJsonArray("records");
        JsonPrimitive error = bodyObject.getAsJsonPrimitive("error");

        if (records != null) {
            for (JsonElement recordElement : records) {
                JsonObject recordObject = recordElement.getAsJsonObject();
                if (recordObject.has("skyflowId")) {
                    insertRecord.put("skyflowId", recordObject.get("skyflowId").getAsString());
                } else if (recordObject.has("skyflow_id")) {
                    insertRecord.put("skyflowId", recordObject.get("skyflow_id").getAsString());
                    LogUtil.printWarningLog(InfoLogs.DEPRECATED_SKYFLOW_ID_KEY.getLog());
                }
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

    private static synchronized HashMap<String, Object> getFormattedBulkInsertRecord(RecordMetaProperties record) {
        HashMap<String, Object> insertRecord = new HashMap<>();
        if (record.getSkyflowId().isPresent()) {
            insertRecord.put("skyflowId", record.getSkyflowId().get());
        }

        if (record.getTokens().isPresent()) {
            Map<String, Object> tokensMap = record.getTokens().get();
            insertRecord.putAll(tokensMap);
        }
        return insertRecord;
    }

    private static synchronized HashMap<String, Object> getFormattedGetRecord(GetFieldRecords record) {
        HashMap<String, Object> getRecord = new HashMap<>();

        Optional<Map<String, Object>> fieldsOpt = record.getFields();
        Optional<Map<String, Object>> tokensOpt = record.getTokens();

        if (fieldsOpt.isPresent()) {
            getRecord.putAll(fieldsOpt.get());
        } else if (tokensOpt.isPresent()) {
            getRecord.putAll(tokensOpt.get());
        }

        if (getRecord.containsKey("skyflow_id")) {
            getRecord.put("skyflowId", getRecord.get("skyflow_id"));
            LogUtil.printWarningLog(InfoLogs.DEPRECATED_SKYFLOW_ID_KEY.getLog());
        }

        return getRecord;
    }

    private static synchronized HashMap<String, Object> getFormattedUpdateRecord(UpdateRecordResponse record) {
        HashMap<String, Object> updateTokens = new HashMap<>();

        record.getSkyflowId().ifPresent(skyflowId -> updateTokens.put("skyflowId", skyflowId));

        record.getTokens().ifPresent(tokensMap -> updateTokens.putAll(tokensMap));

        return updateTokens;
    }

    private static synchronized HashMap<String, Object> getFormattedQueryRecord(FieldRecords record) {
        HashMap<String, Object> queryRecord = new HashMap<>();
        Optional<Map<String, Object>> fieldsOpt = record.getFields();
        if (fieldsOpt.isPresent()) {
            queryRecord.putAll(fieldsOpt.get());
        }

        if (queryRecord.containsKey("skyflow_id")) {
            queryRecord.put("skyflowId", queryRecord.get("skyflow_id"));
            LogUtil.printWarningLog(InfoLogs.DEPRECATED_SKYFLOW_ID_KEY.getLog());
        }

        return queryRecord;
    }

    @Override
    public InsertResponse insert(InsertRequest insertRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.INSERT_TRIGGERED.getLog());
        InsertRecordResponse bulkInsertResult = null;
        ApiClientHttpResponse<BatchOperationResponse> batchInsertResult = null;
        ArrayList<HashMap<String, Object>> insertedFields = new ArrayList<>();
        ArrayList<HashMap<String, Object>> errorFields = new ArrayList<>();
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_INSERT_REQUEST.getLog());
            Validations.validateInsertRequest(insertRequest);
            Boolean continueOnError = insertRequest.getContinueOnError();
            setBearerToken();
            if (continueOnError) {
                BatchOperationRequest insertBody = super.getBatchInsertRequestBody(insertRequest);
                RequestOptions requestOptions = buildRequestOptions();
                batchInsertResult = super.getRecordsApi().withRawResponse().batchOperation(super.getVaultConfig().getVaultId(), insertBody, requestOptions);
                LogUtil.printInfoLog(InfoLogs.INSERT_REQUEST_RESOLVED.getLog());
                Optional<List<Map<String, Object>>> records = batchInsertResult.body().getResponses();

                if (records.isPresent()) {
                    List<Map<String, Object>> recordList = records.get();

                    for (Integer index = (Integer) 0; index < recordList.size(); index++) {
                        Map<String, Object> record = recordList.get(index);
                        HashMap<String, Object> insertRecord = getFormattedBatchInsertRecord(record, index);

                        if (insertRecord.containsKey("skyflowId")) {
                            insertedFields.add(insertRecord);
                        } else {
                            insertRecord.put("requestId", extractRequestId(batchInsertResult.headers()));
                            insertRecord.put("httpCode", ErrorCode.INVALID_INPUT.getCode());
                            errorFields.add(insertRecord);
                        }
                    }
                }
            } else {
                InsertRecordRequest insertBody = super.getBulkInsertRequestBody(insertRequest);
                bulkInsertResult = super.getRecordsApi().insertRecords(
                        super.getVaultConfig().getVaultId(), insertRequest.getTable(), insertBody);
                LogUtil.printInfoLog(InfoLogs.INSERT_REQUEST_RESOLVED.getLog());
                Optional<List<RecordMetaProperties>> records = bulkInsertResult.getRecords();
                if (records.isPresent()) {
                    for (RecordMetaProperties record : records.get()) {
                        HashMap<String, Object> insertRecord = getFormattedBulkInsertRecord(record);
                        insertedFields.add(insertRecord);
                    }
                }
            }
        } catch (ApiClientApiException e) {
            throw wrapApiException(e.statusCode(), e, e.headers(), e.body(), ErrorLogs.INSERT_RECORDS_REJECTED);
        } catch (ApiClientException e) {
            throw new SkyflowException(e);
        }
        LogUtil.printInfoLog(InfoLogs.INSERT_SUCCESS.getLog());
        if (insertedFields.isEmpty()) {
            return new InsertResponse(null, errorFields.isEmpty() ? null : errorFields);
        }
        if (errorFields.isEmpty()) {
            return new InsertResponse(insertedFields, null);
        }
        return new InsertResponse(insertedFields, errorFields);
    }

    public DetokenizeResponse detokenize(DetokenizeRequest detokenizeRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DETOKENIZE_TRIGGERED.getLog());
        ApiClientHttpResponse<com.skyflow.generated.rest.types.DetokenizeResponse> result = null;
        ArrayList<DetokenizeRecordResponse> detokenizedFields = new ArrayList<>();
        ArrayList<DetokenizeRecordResponse> errorRecords = new ArrayList<>();
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_DETOKENIZE_REQUEST.getLog());
            Validations.validateDetokenizeRequest(detokenizeRequest);
            setBearerToken();
            DetokenizePayload payload = super.getDetokenizePayload(detokenizeRequest);
            RequestOptions requestOptions = buildRequestOptions();
            result = super.getTokensApi().withRawResponse().detokenize(super.getVaultConfig().getVaultId(), payload, requestOptions);
            LogUtil.printInfoLog(InfoLogs.DETOKENIZE_REQUEST_RESOLVED.getLog());
            Map<String, List<String>> responseHeaders = result.headers();
            String requestId = extractRequestId(responseHeaders);
            Optional<List<com.skyflow.generated.rest.types.DetokenizeRecordResponse>> records = result.body().getRecords();

            if (records.isPresent()) {
                List<com.skyflow.generated.rest.types.DetokenizeRecordResponse> recordList = records.get();

                for (com.skyflow.generated.rest.types.DetokenizeRecordResponse record : recordList) {
                    if (record.getError().isPresent()) {
                        DetokenizeRecordResponse recordResponse = new DetokenizeRecordResponse(record, requestId);
                        errorRecords.add(recordResponse);
                    } else {
                        DetokenizeRecordResponse recordResponse = new DetokenizeRecordResponse(record);
                        detokenizedFields.add(recordResponse);
                    }
                }
            }
        } catch (ApiClientApiException e) {
            throw wrapApiException(e.statusCode(), e, e.headers(), e.body(), ErrorLogs.DETOKENIZE_REQUEST_REJECTED);
        } catch (ApiClientException e) {
            throw new SkyflowException(e);
        }

        if (!errorRecords.isEmpty()) {
            LogUtil.printInfoLog(InfoLogs.DETOKENIZE_PARTIAL_SUCCESS.getLog());
        } else {
            LogUtil.printInfoLog(InfoLogs.DETOKENIZE_SUCCESS.getLog());
        }
        if (detokenizedFields.isEmpty()) {
            return new DetokenizeResponse(null, errorRecords.isEmpty() ? null : errorRecords);
        }
        if (errorRecords.isEmpty()) {
            return new DetokenizeResponse(detokenizedFields, null);
        }
        return new DetokenizeResponse(detokenizedFields, errorRecords);
    }

    public GetResponse get(GetRequest getRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.GET_TRIGGERED.getLog());
        BulkGetRecordResponse result = null;
        ArrayList<HashMap<String, Object>> data = new ArrayList<>();
        ArrayList<HashMap<String, Object>> errors = new ArrayList<>();
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_GET_REQUEST.getLog());
            Validations.validateGetRequest(getRequest);
            setBearerToken();
            RedactionType redactionType = getRequest.getRedactionType();
            GetRecordsRequest getRecordsRequest = GetRecordsRequest.builder()
                    .skyflowIds(getRequest.getIds())
                    .redaction(redactionType != null ? GetRecordsRequestRedaction.valueOf(redactionType.toString()) : null)
                    .tokenization(getRequest.getReturnTokens())
                    .offset(getRequest.getOffset())
                    .limit(getRequest.getLimit())
                    .downloadUrl(getRequest.getDownloadUrl())
                    .columnName(getRequest.getColumnName())
                    .columnValues(getRequest.getColumnValues())
                    .fields(getRequest.getFields())
                    .orderBy(GetRecordsRequestOrderBy.valueOf(getRequest.getOrderBy()))
                    .build();

            RequestOptions requestOptions = buildRequestOptions();
            result = super.getRecordsApi().getRecords(
                    super.getVaultConfig().getVaultId(),
                    getRequest.getTable(),
                    getRecordsRequest,
                    requestOptions
            );
            LogUtil.printInfoLog(InfoLogs.GET_REQUEST_RESOLVED.getLog());
            if (result.getRecords().isPresent()) {
                List<GetFieldRecords> records = result.getRecords().get();
                for (GetFieldRecords record : records) {
                    data.add(getFormattedGetRecord(record));
                }
            }
        } catch (ApiClientApiException e) {
            throw wrapApiException(e.statusCode(), e, e.headers(), e.body(), ErrorLogs.GET_REQUEST_REJECTED);
        } catch (ApiClientException e) {
            throw new SkyflowException(e);
        }
        LogUtil.printInfoLog(InfoLogs.GET_SUCCESS.getLog());
        return new GetResponse(data, null);
    }

    public UpdateResponse update(UpdateRequest updateRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.UPDATE_TRIGGERED.getLog());
        UpdateRecordResponse result;
        String skyflowId;
        HashMap<String, Object> tokensMap;
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_UPDATE_REQUEST.getLog());
            Validations.validateUpdateRequest(updateRequest);
            setBearerToken();
            UpdateRecordRequest updateBody = super.getUpdateRequestBody(updateRequest);
            RequestOptions requestOptions = buildRequestOptions();
            result = super.getRecordsApi().updateRecord(
                    super.getVaultConfig().getVaultId(),
                    updateRequest.getTable(),
                    extractUpdateSkyflowId(updateRequest.getData()),
                    updateBody,
                    requestOptions
            );
            LogUtil.printInfoLog(InfoLogs.UPDATE_REQUEST_RESOLVED.getLog());
            skyflowId = String.valueOf(result.getSkyflowId());
            tokensMap = getFormattedUpdateRecord(result);
        } catch (ApiClientApiException e) {
            throw wrapApiException(e.statusCode(), e, e.headers(), e.body(), ErrorLogs.UPDATE_REQUEST_REJECTED);
        } catch (ApiClientException e) {
            throw new SkyflowException(e);
        }
        LogUtil.printInfoLog(InfoLogs.UPDATE_SUCCESS.getLog());
        return new UpdateResponse(skyflowId, tokensMap);
    }

    public DeleteResponse delete(DeleteRequest deleteRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DELETE_TRIGGERED.getLog());
        BulkDeleteRecordResponse result;
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_DELETE_REQUEST.getLog());
            Validations.validateDeleteRequest(deleteRequest);
            setBearerToken();
            BulkDeleteRecordRequest deleteBody = BulkDeleteRecordRequest.builder().skyflowIds(deleteRequest.getIds())
                    .build();

            RequestOptions requestOptions = buildRequestOptions();
            result = super.getRecordsApi().deleteRecords(
                    super.getVaultConfig().getVaultId(), deleteRequest.getTable(), deleteBody, requestOptions);
            LogUtil.printInfoLog(InfoLogs.DELETE_REQUEST_RESOLVED.getLog());
        } catch (ApiClientApiException e) {
            throw wrapApiException(e.statusCode(), e, e.headers(), e.body(), ErrorLogs.DELETE_REQUEST_REJECTED);
        } catch (ApiClientException e) {
            throw new SkyflowException(e);
        }
        LogUtil.printInfoLog(InfoLogs.DELETE_SUCCESS.getLog());
        return new DeleteResponse(result.getRecordIdResponse().orElse(Collections.emptyList()));
    }

    public QueryResponse query(QueryRequest queryRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.QUERY_TRIGGERED.getLog());
        GetQueryResponse result;
        ArrayList<HashMap<String, Object>> fields = new ArrayList<>();
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_QUERY_REQUEST.getLog());
            Validations.validateQueryRequest(queryRequest);
            setBearerToken();
            RequestOptions requestOptions = buildRequestOptions();
            result = super.getQueryApi().executeQuery(
                    super.getVaultConfig().getVaultId(),
                    GetQueryRequest.builder().query(queryRequest.getQuery()).build(),
                    requestOptions
            );
            LogUtil.printInfoLog(InfoLogs.QUERY_REQUEST_RESOLVED.getLog());
            if (result.getRecords().isPresent()) {
                List<FieldRecords> records = result.getRecords().get(); // Extract the List from Optional
                for (FieldRecords record : records) {
                    fields.add(getFormattedQueryRecord(record));
                }
            }
        } catch (ApiClientApiException e) {
            throw wrapApiException(e.statusCode(), e, e.headers(), e.body(), ErrorLogs.QUERY_REQUEST_REJECTED);
        } catch (ApiClientException e) {
            throw new SkyflowException(e);
        }
        LogUtil.printInfoLog(InfoLogs.QUERY_SUCCESS.getLog());
        return new QueryResponse(fields);
    }

    public TokenizeResponse tokenize(TokenizeRequest tokenizeRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.TOKENIZE_TRIGGERED.getLog());
        com.skyflow.generated.rest.types.TokenizeResponse result = null;
        List<String> list = new ArrayList<>();
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_TOKENIZE_REQUEST.getLog());
            Validations.validateTokenizeRequest(tokenizeRequest);
            setBearerToken();
            TokenizePayload payload = super.getTokenizePayload(tokenizeRequest);
            RequestOptions requestOptions = buildRequestOptions();
            result = super.getTokensApi().tokenize(super.getVaultConfig().getVaultId(), payload, requestOptions);
            LogUtil.printInfoLog(InfoLogs.TOKENIZE_REQUEST_RESOLVED.getLog());
            if (result != null && result.getRecords() != null && !result.getRecords().isEmpty()) {
                for (TokenizeRecordResponse response : result.getRecords()) {
                    if (response.getToken() != null) {
                        list.add(response.getToken());
                    }
                }
            }
        } catch (ApiClientApiException e) {
            throw wrapApiException(e.statusCode(), e, e.headers(), e.body(), ErrorLogs.TOKENIZE_REQUEST_REJECTED);
        } catch (ApiClientException e) {
            throw new SkyflowException(e);
        }
        LogUtil.printInfoLog(InfoLogs.TOKENIZE_SUCCESS.getLog());
        return new TokenizeResponse(list);
    }

    public FileUploadResponse uploadFile(FileUploadRequest fileUploadRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.FILE_UPLOAD_TRIGGERED.getLog());
        FileUploadResponse fileUploadResponse = null;

        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_FILE_UPLOAD_REQUEST.getLog());
            Validations.validateFileUploadRequest(fileUploadRequest);
            setBearerToken();
            File file = super.getFileForFileUpload(fileUploadRequest);

            UploadFileRequestV2 uploadFileRequest = UploadFileRequestV2.builder()
                    .tableName(fileUploadRequest.getTable())
                    .columnName(fileUploadRequest.getColumnName())
                    .skyflowId(fileUploadRequest.getSkyflowId())
                    .returnFileMetadata(false)
                    .build();

            RequestOptions requestOptions = buildRequestOptions();
            UploadFileResponseV2 uploadFileResponse = super.getFilesApi().uploadFileV2(
                    super.getVaultConfig().getVaultId(),
                    file,
                    uploadFileRequest,
                    requestOptions
            );

            fileUploadResponse = new FileUploadResponse(
                    uploadFileResponse.getSkyflowId().orElse(null),
                    null
            );

        } catch (ApiClientApiException e) {
            throw wrapApiException(e.statusCode(), e, e.headers(), e.body(), ErrorLogs.UPLOAD_FILE_REQUEST_REJECTED);
        } catch (ApiClientException e) {
            throw new SkyflowException(e);
        } catch (IOException e) {
            LogUtil.printErrorLog(ErrorLogs.UPLOAD_FILE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage(), e);
        }
        LogUtil.printInfoLog(InfoLogs.FILE_UPLOAD_SUCCESS.getLog());
        return fileUploadResponse;
    }
}
