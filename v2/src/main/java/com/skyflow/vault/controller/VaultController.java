package com.skyflow.vault.controller;

import com.skyflow.VaultClient;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiException;
import com.skyflow.generated.rest.auth.HttpBearerAuth;
import com.skyflow.generated.rest.models.RecordServiceInsertRecordBody;
import com.skyflow.generated.rest.models.V1DetokenizePayload;
import com.skyflow.generated.rest.models.V1DetokenizeResponse;
import com.skyflow.generated.rest.models.V1InsertRecordResponse;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.utils.Utils;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.DetokenizeResponse;

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

    public InsertResponse insert(InsertRequest insertRequest) throws SkyflowException {
        V1InsertRecordResponse result = null;
        try {
            Validations.validateInsertRequest(insertRequest);
            setBearerToken();
            RecordServiceInsertRecordBody insertBody = super.getInsertRequestBody(insertRequest);
            result = super.getRecordsApi().recordServiceInsertRecord(
                    super.getVaultConfig().getVaultId(), insertRequest.getTable(), insertBody);
        } catch (ApiException e) {
            throw new SkyflowException(e.getCode(), e, e.getResponseHeaders(), e.getResponseBody());
        }
        return new InsertResponse(result);
    }

    public DetokenizeResponse detokenize(DetokenizeRequest detokenizeRequest) throws SkyflowException {
        V1DetokenizeResponse result = null;
        try {
            Validations.validateDetokenizeRequest(detokenizeRequest);
            setBearerToken();
            V1DetokenizePayload payload = super.getDetokenizePayload(detokenizeRequest);
            result = super.getTokensApi().recordServiceDetokenize(super.getVaultConfig().getVaultId(), payload);
        } catch (ApiException e) {
            throw new SkyflowException(e.getCode(), e, e.getResponseHeaders(), e.getResponseBody());
        }
        return new DetokenizeResponse(result);
    }

    public Object get(Object getRequest) {
        return null;
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
