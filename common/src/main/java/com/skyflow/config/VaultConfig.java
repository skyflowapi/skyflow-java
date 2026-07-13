package com.skyflow.config;

public class VaultConfig extends BaseVaultConfig implements Cloneable {

    private Credentials credentials;

    public VaultConfig() {
        super();
        this.credentials = null;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        VaultConfig cloned = (VaultConfig) super.clone();
        if (this.credentials != null) {
            cloned.credentials = (Credentials) this.credentials.clone();
        }

        return cloned;
    }

}
