package com.skyflow.config;

import com.skyflow.enums.Env;

public class VaultConfig implements Cloneable {
    private String vaultId;
    private String clusterId;
    private String vaultURL;
    private Env env;
    private Credentials credentials;
    // HTTP timeout & retry config (vault-level overrides). null => inherit client-wide default, then SDK default.
    private Integer timeout;                 // overall call timeout, in seconds
    private Integer maxRetries;              // retry attempts after the first failure
    private Long initialRetryDelay;    // base backoff before the first retry, in ms
    private Long maxRetryDelay;        // cap on the (exponentially growing) backoff, in ms

    public VaultConfig() {
        this.vaultId = null;
        this.clusterId = null;
        this.vaultURL = null;
        this.env = Env.PROD;
        this.credentials = null;
        this.timeout = null;
        this.maxRetries = null;
        this.initialRetryDelay = null;
        this.maxRetryDelay = null;
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

    public String getVaultURL() {
        return vaultURL;
    }

    public void setVaultURL(String vaultURL) {
        this.vaultURL = vaultURL;
    }

    public Integer getTimeout() {
        return timeout;
    }

    /** Overall call timeout in seconds for this vault. Overrides the client-wide default. */
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    /** Number of retry attempts after the first failure for this vault. Overrides the client-wide default. */
    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Long getInitialRetryDelay() {
        return initialRetryDelay;
    }

    /** Base retry backoff in milliseconds for this vault. Overrides the client-wide default. */
    public void setInitialRetryDelay(Long initialRetryDelay) {
        this.initialRetryDelay = initialRetryDelay;
    }

    public Long getMaxRetryDelay() {
        return maxRetryDelay;
    }

    /** Cap on retry backoff in milliseconds for this vault. Overrides the client-wide default. */
    public void setMaxRetryDelay(Long maxRetryDelay) {
        this.maxRetryDelay = maxRetryDelay;
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
