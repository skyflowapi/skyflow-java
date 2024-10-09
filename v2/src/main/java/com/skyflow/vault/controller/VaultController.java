package com.skyflow.vault.controller;

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
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;
import com.skyflow.vault.tokens.DetokenizeRecordResponse;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.DetokenizeResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    private static synchronized HashMap<String, String> getFormattedInsertRecord(V1RecordMetaProperties record) {
        HashMap<String, String> insertRecord = new HashMap<>();
        String skyflowId = record.getSkyflowId();
        insertRecord.put("skyflowId", skyflowId);

        /*
        Getting unchecked cast warning, however, this type is inferred
        from an exception trying to cast into another type. Therefore,
        this type cast will not fail.
        */
        LinkedTreeMap<String, String> tokensMap = (LinkedTreeMap<String, String>) record.getTokens();
        if (tokensMap != null) {
            for (String key : tokensMap.keySet()) {
                insertRecord.put(key, tokensMap.get(key));
            }
        }
        return insertRecord;
    }

    private static synchronized HashMap<String, String> getFormattedGetRecord(V1FieldRecords record) {
        HashMap<String, String> getRecord = new HashMap<>();

        /*
        Getting unchecked cast warning, however, this type is inferred
        from an exception trying to cast into another type. Therefore,
        this type cast will not fail.
        */
        LinkedTreeMap<String, String> map = null;
        if (record.getTokens() != null) {
            map = (LinkedTreeMap<String, String>) record.getTokens();
        } else {
            map = (LinkedTreeMap<String, String>) record.getFields();
        }
        if (map != null) {
            for (String key : map.keySet()) {
                getRecord.put(key, map.get(key));
            }
        }
        return getRecord;
    }

    public InsertResponse insert(InsertRequest insertRequest) throws SkyflowException {
        V1InsertRecordResponse result = null;
        ArrayList<HashMap<String, String>> insertedFields = new ArrayList<>();
        ArrayList<HashMap<String, String>> errorFields = new ArrayList<>();
        try {
            Validations.validateInsertRequest(insertRequest);
            setBearerToken();
            RecordServiceInsertRecordBody insertBody = super.getInsertRequestBody(insertRequest);
            result = super.getRecordsApi().recordServiceInsertRecord(
                    super.getVaultConfig().getVaultId(), insertRequest.getTable(), insertBody);
            List<V1RecordMetaProperties> records = result.getRecords();
            if (records != null) {
                for (V1RecordMetaProperties record : records) {
                    HashMap<String, String> insertRecord = getFormattedInsertRecord(record);
                    insertedFields.add(insertRecord);
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
        ArrayList<HashMap<String, String>> data = new ArrayList<>();
        ArrayList<HashMap<String, String>> errors = new ArrayList<>();
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

    public Object update(Object updateRequest) {
        return null;
    }

    public Object delete(Object deleteRequest) {
        return null;
    }

    public Object uploadFile(Object uploadFileRequest) {
        return null;
    }

    public Object query(Object queryRequest) {
        return null;
    }

    public Object tokenize(Object tokenizeRequest) {
        return null;
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
