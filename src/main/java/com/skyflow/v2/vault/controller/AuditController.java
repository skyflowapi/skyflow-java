package com.skyflow.v2.vault.controller;

import com.skyflow.common.generated.ApiClient;
import com.skyflow.v2.vault.audit.ListEventRequest;
import com.skyflow.v2.vault.audit.ListEventResponse;

public class AuditController {

    public AuditController(ApiClient apiClient) {

    }

    // Check for correct return type in python interfaces
    public ListEventResponse list(ListEventRequest listEventRequest) {
        // return audit events
        return null;
    }
}
