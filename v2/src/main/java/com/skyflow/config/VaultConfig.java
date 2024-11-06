package com.skyflow.config;

import com.skyflow.enums.Env;
import com.skyflow.utils.Utils;

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
        this.env = env;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public String getVaultURL() {
        return Utils.getVaultURL(this.clusterId, this.env);
    }

    @Override
    public String toString() {
        return "VaultConfig {" +
                "\n\tvaultId: '" + vaultId + "'," +
                "\n\tclusterId: '" + clusterId + "'," +
                "\n\tenv: " + env +
                "\n}";
    }
}
