package com.skyflow.config;

/**
 * Per-vault configuration.
 * <p>
 * The HTTP timeout and retry settings below are <b>vault-level overrides</b>. Each one resolves
 * most-specific-first: the value set here, else the client-wide value set on
 * {@code Skyflow.builder()}, else the SDK default. So when the same setting is supplied at both
 * levels, <b>the value on this VaultConfig takes precedence</b> and the client-wide value is
 * ignored for this vault.
 * <p>
 * Only {@code null} means "inherit" — an explicit {@code 0} is a real value and wins over the
 * client-wide setting.
 */
public class VaultConfig extends BaseVaultConfig {

    private String vaultUrl;
    // HTTP timeout & retry config (vault-level overrides). null => inherit client-wide default, then SDK default.
    private Integer timeout;         // overall call timeout, in seconds
    private Integer connectTimeout;  // per-attempt connection-establishment timeout, in seconds
    private Integer readTimeout;     // per-attempt response-read timeout, in seconds
    private Integer writeTimeout;    // per-attempt request-write timeout, in seconds
    private Integer maxRetries;      // retry attempts after the first failure
    private Long initialRetryDelayMillis; // backoff before the first retry, in milliseconds
    private Long maxRetryDelayMillis;     // ceiling the exponential backoff grows to, in milliseconds

    public VaultConfig() {
        super();
        this.vaultUrl = null;
        this.timeout = null;
        this.connectTimeout = null;
        this.readTimeout = null;
        this.writeTimeout = null;
        this.maxRetries = null;
        this.initialRetryDelayMillis = null;
        this.maxRetryDelayMillis = null;
    }

    public String getVaultUrl() {
        return vaultUrl;
    }

    public void setVaultUrl(String vaultUrl) {
        this.vaultUrl = vaultUrl;
    }

    public Integer getTimeout() {
        return timeout;
    }

    /**
     * Overall call timeout in seconds for this vault, including retries.
     * <p>
     * Takes precedence over the client-wide {@code Skyflow.builder().timeout(...)}. Leave unset
     * (null) to inherit that value, or the SDK default of 60s if it is also unset.
     */
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Per-attempt connection-establishment timeout in seconds for this vault.
     * <p>
     * Takes precedence over the client-wide {@code Skyflow.builder().connectTimeout(...)}. Leave
     * unset (null) to inherit that value; if neither is set, the underlying HTTP client default
     * (10s) applies. Note the overall {@code timeout} still bounds the whole call, including retries.
     */
    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    /**
     * Per-attempt response-read timeout in seconds for this vault.
     * <p>
     * Takes precedence over the client-wide {@code Skyflow.builder().readTimeout(...)}. Leave
     * unset (null) to inherit that value; if neither is set, the underlying HTTP client default
     * (10s) applies. Note the overall {@code timeout} still bounds the whole call, including retries.
     */
    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Integer getWriteTimeout() {
        return writeTimeout;
    }

    /**
     * Per-attempt request-write timeout in seconds for this vault.
     * <p>
     * Takes precedence over the client-wide {@code Skyflow.builder().writeTimeout(...)}. Leave
     * unset (null) to inherit that value; if neither is set, the underlying HTTP client default
     * (10s) applies. Note the overall {@code timeout} still bounds the whole call, including retries.
     */
    public void setWriteTimeout(Integer writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    /**
     * Retry attempts after the first failure for this vault.
     * <p>
     * Takes precedence over the client-wide {@code Skyflow.builder().maxRetries(...)}. Leave unset
     * (null) to inherit that value, or the SDK default of 0 if it is also unset — retries are
     * opt-in, so non-idempotent bulk writes are not replayed silently.
     */
    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }


    public Long getInitialRetryDelayMillis() {
        return initialRetryDelayMillis;
    }

    /**
     * Backoff before the first retry, in milliseconds, for this vault.
     * <p>
     * Takes precedence over the client-wide {@code Skyflow.builder().initialRetryDelayMillis(...)}.
     * Leave unset (null) to inherit that value, or the SDK default of 500 ms if it is also unset.
     * Only applies when {@code maxRetries} is greater than zero.
     */
    public void setInitialRetryDelayMillis(Long initialRetryDelayMillis) {
        this.initialRetryDelayMillis = initialRetryDelayMillis;
    }

    public Long getMaxRetryDelayMillis() {
        return maxRetryDelayMillis;
    }

    /**
     * Ceiling the exponential backoff grows to, in milliseconds, for this vault.
     * <p>
     * Takes precedence over the client-wide {@code Skyflow.builder().maxRetryDelayMillis(...)}.
     * Leave unset (null) to inherit that value, or the SDK default of 2000 ms if it is also unset.
     * Only applies when {@code maxRetries} is greater than zero.
     */
    public void setMaxRetryDelayMillis(Long maxRetryDelayMillis) {
        this.maxRetryDelayMillis = maxRetryDelayMillis;
    }

}
