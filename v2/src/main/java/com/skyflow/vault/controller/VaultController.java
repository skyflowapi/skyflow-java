package com.skyflow.vault.controller;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.api.RecordsApi;
import com.skyflow.generated.rest.api.TokensApi;
import com.skyflow.generated.rest.models.V1DetokenizePayload;
import com.skyflow.generated.rest.models.V1DetokenizeResponse;
import com.skyflow.utils.Utils;
import com.skyflow.vault.audit.ListEventRequest;
import com.skyflow.vault.bin.GetBinRequest;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;
import com.skyflow.vault.detect.DeIdentifyRequest;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.DetokenizeResponse;

public class VaultController {
    private final RecordsApi recordsApi;
    private final TokensApi tokensApi;
    private final ApiClient apiClient;
    private DetectController detectController;
    private AuditController auditController;
    private BinLookupController binLookupController;
    private VaultConfig vaultConfig;
    private Credentials commonCredentials;

    public VaultController(VaultConfig vaultConfig, Credentials credentials) {
        this.vaultConfig = vaultConfig;
        this.commonCredentials = credentials;
        this.auditController = null;
        this.binLookupController = null;
        this.detectController = null;

        this.apiClient = new ApiClient();
        this.apiClient.setBasePath(this.vaultConfig.getVaultURL());
        this.tokensApi = new TokensApi(this.apiClient);
        this.recordsApi = new RecordsApi(this.apiClient);
    }

    public void setCommonCredentials(Credentials commonCredentials) {
        this.commonCredentials = commonCredentials;
    }

    public VaultConfig getVaultConfig() {
        return vaultConfig;
    }

    public void setVaultConfig(VaultConfig vaultConfig) {
        this.vaultConfig = vaultConfig;
    }

    public InsertResponse insert(InsertRequest insertRequest) {
        return null;
    }

    public DetokenizeResponse detokenize(DetokenizeRequest detokenizeRequest) {
        V1DetokenizeResponse result = null;
        try {
            // prioritise credentials here
            Utils.updateBearerTokenIfExpired(this.apiClient, this.commonCredentials);
            V1DetokenizePayload payload = detokenizeRequest.getDetokenizePayload();
            result = this.tokensApi.recordServiceDetokenize(this.vaultConfig.getVaultId(), payload);
            System.out.println(result);
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

    public BinLookupController lookUpBin(GetBinRequest getBinRequest) {
        if (this.binLookupController == null) {
            this.binLookupController = new BinLookupController(this.vaultConfig, this.apiClient);
        }
        return this.binLookupController;
    }

    public AuditController audit(ListEventRequest listEventRequest) {
        if (this.auditController == null) {
            this.auditController = new AuditController(this.vaultConfig, this.apiClient);
        }
        return this.auditController;
    }

    public DetectController detect(DeIdentifyRequest deIdentifyRequest) {
        if (this.detectController == null) {
            this.detectController = new DetectController(this.vaultConfig, this.apiClient);
        }
        return this.detectController;
    }
}
