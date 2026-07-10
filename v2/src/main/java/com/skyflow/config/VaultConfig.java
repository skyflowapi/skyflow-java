package com.skyflow.config;

public class VaultConfig extends BaseVaultConfig {
    Credentials credentials;
    public VaultConfig() {
        super();
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }
}
