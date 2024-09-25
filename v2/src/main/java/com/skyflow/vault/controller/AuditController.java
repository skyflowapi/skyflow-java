package com.skyflow.vault.controller;

import com.skyflow.config.VaultConfig;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.api.AuditApi;
import com.skyflow.vault.audit.ListEventRequest;
import com.skyflow.vault.audit.ListEventResponse;

public class AuditController {
    private VaultConfig vaultConfig;
    private ApiClient apiClient;
    private AuditApi auditApi;

    public AuditController(VaultConfig vaultConfig, ApiClient apiClient) {
        this.vaultConfig = vaultConfig;
        this.apiClient = apiClient;
        this.auditApi = new AuditApi(this.apiClient);
    }

    // Check for correct return type in python interfaces
    public ListEventResponse list(ListEventRequest listEventRequest) {
        //    return audit events
        return null;
    }
}
