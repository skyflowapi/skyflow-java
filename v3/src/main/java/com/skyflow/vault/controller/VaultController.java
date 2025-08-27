package com.skyflow.vault.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skyflow.VaultClient;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.generated.rest.types.InsertResponse;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.vault.RequestOptions;
import com.skyflow.vault.data.BaseInsertRequest;

import java.util.concurrent.CompletableFuture;

public final class VaultController extends VaultClient {
    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    public VaultController(VaultConfig vaultConfig, Credentials credentials) {
        super(vaultConfig, credentials);
    }

    // add methods for v3 SDK
    public InsertResponse bulkInsert(BaseInsertRequest insertRequest, RequestOptions requestOptions) {
        InsertResponse response = null;
        LogUtil.printInfoLog(InfoLogs.INSERT_TRIGGERED.getLog());
        return response;
    }

    public CompletableFuture<InsertResponse> bulkInsertAsync(BaseInsertRequest insertRequest, RequestOptions requestOptions){
        CompletableFuture<InsertResponse> future = null;
        return future;
    }
}
