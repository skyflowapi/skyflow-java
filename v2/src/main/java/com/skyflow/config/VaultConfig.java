package com.skyflow.config;

import com.skyflow.enums.ENV;

public class VaultConfig {
    //    members
    private String vaultId;
    private String clusterId;
    private ENV env;
    private Credentials credentials;

    public VaultConfig() {
        this.vaultId = null;
        this.clusterId = null;
        this.env = ENV.PROD;
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

    public ENV getEnv() {
        return env;
    }

    public void setEnv(ENV env) {
        this.env = env;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public String toString() {
        return "VaultConfig{" +
                "vaultId='" + vaultId + '\'' +
                ", clusterId='" + clusterId + '\'' +
                ", env=" + env +
                '}';
    }
}
