package com.skyflow.vault.controller;

import com.skyflow.config.VaultConfig;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.vault.detect.DeIdentifyRequest;
import com.skyflow.vault.detect.DeIdentifyResponse;

public class DetectController {
    private VaultConfig vaultConfig;
    private ApiClient apiClient;

    public DetectController(VaultConfig vaultConfig, ApiClient apiClient) {
        this.vaultConfig = vaultConfig;
        this.apiClient = apiClient;
    }

    // should return detect controller
    public DetectController deIdentify() {
        //    return detect controller
        return null;
    }

    public DeIdentifyResponse text(DeIdentifyRequest deIdentifyRequest) {
        //    return detect response
        return null;
    }

    public DeIdentifyResponse file(DeIdentifyRequest deIdentifyRequest) {
        //    return detect response
        return null;
    }
}
