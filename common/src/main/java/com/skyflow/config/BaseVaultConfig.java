package com.skyflow.config;

import com.skyflow.enums.Env;

public class BaseVaultConfig implements Cloneable {
    private String vaultId;
    private String clusterId;
    private Env env;
    private Credentials credentials;

    public BaseVaultConfig() {
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

    @Override
    public Object clone() throws CloneNotSupportedException {
        BaseVaultConfig cloned = (BaseVaultConfig) super.clone();
        if (this.credentials != null) {
            cloned.credentials = (Credentials) this.credentials.clone();
        }
        return cloned;
    }

}
