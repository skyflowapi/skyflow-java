package com.skyflow.config;

public class VaultConfig extends BaseVaultConfig {

    private String vaultURL;

    public VaultConfig() {
        super();
        this.vaultURL = null;
    }

    public String getVaultURL() {
        return vaultURL;
    }

    public void setVaultURL(String vaultURL) {
        this.vaultURL = vaultURL;
    }

}
