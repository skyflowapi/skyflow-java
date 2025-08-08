package com.skyflow.vaultLH.vault.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skyflow.common.config.Credentials;
import com.skyflow.common.config.VaultConfig;
import com.skyflow.vaultLH.VaultClient;

public final class VaultController extends VaultClient {
    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    public VaultController(VaultConfig vaultConfig, Credentials credentials) {
        super(vaultConfig, credentials);
    }
}
