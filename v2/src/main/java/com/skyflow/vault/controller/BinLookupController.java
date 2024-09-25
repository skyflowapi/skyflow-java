package com.skyflow.vault.controller;

import com.skyflow.config.VaultConfig;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.api.BinLookupApi;
import com.skyflow.vault.bin.GetBinRequest;
import com.skyflow.vault.bin.GetBinResponse;

public class BinLookupController {
    private VaultConfig vaultConfig;
    private ApiClient apiClient;
    private BinLookupApi binLookupApi;

    public BinLookupController(VaultConfig vaultConfig, ApiClient apiClient) {
        this.vaultConfig = vaultConfig;
        this.apiClient = apiClient;
        this.binLookupApi = new BinLookupApi(this.apiClient);
    }

    public GetBinResponse get(GetBinRequest getBinRequest) {
        //    return bin lookup response (card metadata associated with BIN)
        return null;
    }
}
