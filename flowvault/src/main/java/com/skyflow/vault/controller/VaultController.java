package com.skyflow.vault.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.skyflow.VaultClient;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Utils;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.DetokenizeResponse;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;

public final class VaultController extends VaultClient
        implements IVaultController<InsertRequest, InsertResponse, DetokenizeRequest, DetokenizeResponse> {
    private static final Gson gson = new GsonBuilder().serializeNulls().create();
    private JsonObject metrics = Utils.getMetrics();

    public VaultController(VaultConfig vaultConfig, Credentials credentials) throws SkyflowException {
        super(vaultConfig, credentials);
    }

    @Override
    public InsertResponse insert(InsertRequest request) throws SkyflowException {
        return null;
    }

    @Override
    public DetokenizeResponse detokenize(DetokenizeRequest request) throws SkyflowException {
        return null;
    }
}