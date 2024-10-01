package com.skyflow.vault.controller;

import com.skyflow.VaultClient;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.generated.rest.models.V1DetokenizePayload;
import com.skyflow.generated.rest.models.V1DetokenizeResponse;
import com.skyflow.utils.Utils;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.DetokenizeResponse;

public final class VaultController extends VaultClient {
    private DetectController detectController;
    private AuditController auditController;
    private BinLookupController binLookupController;

    public VaultController(VaultConfig vaultConfig, Credentials credentials) {
        super(vaultConfig, credentials);
        this.auditController = null;
        this.binLookupController = null;
        this.detectController = null;
    }

    public InsertResponse insert(InsertRequest insertRequest) {
        return null;
    }

    public DetokenizeResponse detokenize(DetokenizeRequest detokenizeRequest) {
        System.out.println(super.getVaultConfig());
        V1DetokenizeResponse result = null;
        try {
            Utils.updateBearerTokenIfExpired(super.getApiClient(), super.getFinalCredentials());
            V1DetokenizePayload payload = detokenizeRequest.getDetokenizePayload();
            result = super.getTokensApi().recordServiceDetokenize(super.getVaultConfig().getVaultId(), payload);
        } catch (Exception e) {
            e.printStackTrace();
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
}
