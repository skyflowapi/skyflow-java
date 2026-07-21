package com.skyflow.vault.controller;

import com.skyflow.generated.rest.ApiClient;
import com.skyflow.vault.audit.ListEventRequest;
import com.skyflow.vault.audit.ListEventResponse;

public class AuditController {

    public AuditController(ApiClient apiClient) {

    }

    // Check for correct return type in python interfaces
    public ListEventResponse list(ListEventRequest listEventRequest) {
        // return audit events
        return null;
    }
}
