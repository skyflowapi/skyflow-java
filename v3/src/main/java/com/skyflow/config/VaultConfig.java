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
    private Integer connectTimeout;          // per-attempt connection-establishment timeout, in seconds
    private Integer readTimeout;             // per-attempt response-read timeout, in seconds
    private Integer writeTimeout;            // per-attempt request-write timeout, in seconds
    private Integer maxRetries;              // retry attempts after the first failure
    private Long initialRetryDelayMillis;    // base backoff before the first retry, in ms
    private Long maxRetryDelayMillis;        // cap on the (exponentially growing) backoff, in ms

    public VaultConfig() {
        this.vaultId = null;
        this.clusterId = null;
        this.vaultURL = null;
        this.env = Env.PROD;
        this.credentials = null;
        this.timeout = null;
        this.connectTimeout = null;
        this.readTimeout = null;
        this.writeTimeout = null;
        this.maxRetries = null;
        this.initialRetryDelayMillis = null;
        this.maxRetryDelayMillis = null;
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

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Per-attempt connection-establishment timeout in seconds for this vault. Overrides the
     * client-wide default; when unset the underlying HTTP client default (10s) applies. Note the
     * overall {@code timeout} still bounds the whole call, including retries.
     */
    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    /**
     * Per-attempt response-read timeout in seconds for this vault. Overrides the client-wide
     * default; when unset the underlying HTTP client default (10s) applies. Note the overall
     * {@code timeout} still bounds the whole call, including retries.
     */
    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Integer getWriteTimeout() {
        return writeTimeout;
    }

    /**
     * Per-attempt request-write timeout in seconds for this vault. Overrides the client-wide
     * default; when unset the underlying HTTP client default (10s) applies. Note the overall
     * {@code timeout} still bounds the whole call, including retries.
     */
    public void setWriteTimeout(Integer writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    /** Number of retry attempts after the first failure for this vault. Overrides the client-wide default. */
    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Long getInitialRetryDelayMillis() {
        return initialRetryDelayMillis;
    }

    /** Base retry backoff in milliseconds for this vault. Overrides the client-wide default. */
    public void setInitialRetryDelayMillis(Long initialRetryDelayMillis) {
        this.initialRetryDelayMillis = initialRetryDelayMillis;
    }

    public Long getMaxRetryDelayMillis() {
        return maxRetryDelayMillis;
    }

    /** Cap on retry backoff in milliseconds for this vault. Overrides the client-wide default. */
    public void setMaxRetryDelayMillis(Long maxRetryDelayMillis) {
        this.maxRetryDelayMillis = maxRetryDelayMillis;
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
