package com.skyflow.config;

import com.skyflow.enums.Env;

public class BaseVaultConfig {
    private String vaultId;
    private String clusterId;
    private String vaultURL;
    private Env env;

    public BaseVaultConfig() {
        this.vaultId = null;
        this.clusterId = null;
        this.vaultURL = null;
        this.env = Env.PROD;
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


    public String getVaultURL() {
        return vaultURL;
    }

    public void setVaultURL(String vaultURL) {
        this.vaultURL = vaultURL;
    }

}
