package com.skyflow.vault.controller;

import com.skyflow.config.VaultConfig;
import com.skyflow.vault.audit.ListEventRequest;
import com.skyflow.vault.bin.GetBinRequest;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;
import com.skyflow.vault.detect.DeIdentifyRequest;
import com.skyflow.vault.tokens.DetokenizeRequest;

public class VaultController {
    private final AuditController auditController;
    private final BinLookupController binLookupController;
    private final DetectController detectController;
    // members
    private VaultConfig vaultConfig;

    public VaultController(VaultConfig vaultConfig) {
        this.vaultConfig = vaultConfig;
        this.auditController = new AuditController();
        this.binLookupController = new BinLookupController();
        this.detectController = new DetectController();
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

    public Object detokenize(DetokenizeRequest detokenizeRequest) {
        return null;
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
        // cache bin lookup object if created
        // return bin lookup object
        return this.binLookupController;
    }

    public AuditController audit(ListEventRequest listEventRequest) {
        // cache audit object if created
        // return audit object
        return this.auditController;
    }

    public DetectController detect(DeIdentifyRequest deIdentifyRequest) {
        // cache detect object if created
        // return detect object
        return this.detectController;
    }
}
