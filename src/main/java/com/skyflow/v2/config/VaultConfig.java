package com.skyflow.v2.config;

import com.skyflow.v2.enums.Env;

public class VaultConfig {
    private String vaultId;
    private String clusterId;
    private Env env;
    private Credentials credentials;

    public VaultConfig() {
        this.vaultId = null;
        this.clusterId = null;
        this.env = Env.PROD;
        this.credentials = null;
    }

    public String getVaultId() {
        return vaultId;
    }

    public void setVaultId(String vaultId) {
        this.vaultId = vaultId;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public Env getEnv() {
        return env;
    }

    public void setEnv(Env env) {
        this.env = env == null ? Env.PROD : env;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }
}
