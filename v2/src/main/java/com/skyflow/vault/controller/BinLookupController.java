package com.skyflow.vault.controller;

import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.api.BinLookupApi;
import com.skyflow.vault.bin.GetBinRequest;
import com.skyflow.vault.bin.GetBinResponse;

public class BinLookupController {
    private BinLookupApi binLookupApi;

    public BinLookupController(ApiClient apiClient) {
        this.binLookupApi = new BinLookupApi(apiClient);
    }

    public GetBinResponse get(GetBinRequest getBinRequest) {
        // return bin lookup response (card metadata associated with BIN)
        return null;
    }
}
