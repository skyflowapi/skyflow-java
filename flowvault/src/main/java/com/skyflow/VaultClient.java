package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.ApiClientBuilder;
import com.skyflow.generated.rest.resources.flowservice.FlowserviceClient;
import com.skyflow.generated.rest.resources.records.RecordsClient;
import com.skyflow.utils.SkyflowRetryInterceptor;
import com.skyflow.utils.Utils;

import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class VaultClient extends BaseVaultClient<VaultConfig> {
    private final ApiClientBuilder apiClientBuilder;
    private ApiClient apiClient;
    // Client-wide (Skyflow builder) HTTP config; null => fall back to the SDK defaults below.
    private Integer commonTimeout;
    private Integer commonConnectTimeout;
    private Integer commonReadTimeout;
    private Integer commonWriteTimeout;
    private Integer commonMaxRetries;
    private Long commonInitialRetryDelayMillis;
    private Long commonMaxRetryDelayMillis;
    // SDK defaults, used when neither the vault-level nor the client-wide value is set.
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;
    // Retries OFF by default (opt-in) so non-idempotent bulk writes aren't replayed automatically.
    private static final int DEFAULT_MAX_RETRIES = 0;
    private static final long DEFAULT_INITIAL_RETRY_DELAY_MILLIS = 500L;
    private static final long DEFAULT_MAX_RETRY_DELAY_MILLIS = 2000L;

    protected VaultClient(VaultConfig vaultConfig, Credentials credentials) throws SkyflowException {
        super(vaultConfig, credentials);
        this.apiClientBuilder = new ApiClientBuilder();
        this.apiClient = null;
        updateVaultUrl();
    }

    /**
     * Applies the client-wide HTTP settings from the Skyflow builder. Discards the cached HTTP
     * client and ApiClient so the next call rebuilds them with the new values.
     */
    protected void setCommonHttpConfig(Integer timeout, Integer connectTimeout, Integer readTimeout,
                                       Integer writeTimeout, Integer maxRetries,
                                       Long initialRetryDelayMillis, Long maxRetryDelayMillis) {
        this.commonTimeout = timeout;
        this.commonConnectTimeout = connectTimeout;
        this.commonReadTimeout = readTimeout;
        this.commonWriteTimeout = writeTimeout;
        this.commonMaxRetries = maxRetries;
        this.commonInitialRetryDelayMillis = initialRetryDelayMillis;
        this.commonMaxRetryDelayMillis = maxRetryDelayMillis;
        this.sharedHttpClient = null;
        this.apiClient = null;
    }

    /** Resolve a setting: vault-level override, else client-wide default, else the SDK default. */
    private static int resolveInt(Integer vaultLevel, Integer clientLevel, int defaultValue) {
        if (vaultLevel != null) {
            return vaultLevel;
        }
        return clientLevel != null ? clientLevel : defaultValue;
    }

    /** Resolve a long setting: vault-level override, else client-wide default, else the SDK default. */
    private static long resolveLong(Long vaultLevel, Long clientLevel, long defaultValue) {
        if (vaultLevel != null) {
            return vaultLevel;
        }
        return clientLevel != null ? clientLevel : defaultValue;
    }

    /**
     * Resolve an optional setting: vault-level override, else client-wide default, else null.
     * Null means "not configured" — the caller leaves the underlying HTTP client default in place.
     */
    private static Integer resolveNullableInt(Integer vaultLevel, Integer clientLevel) {
        return vaultLevel != null ? vaultLevel : clientLevel;
    }

    protected FlowserviceClient getRecordsApi() {
        return this.apiClient.flowservice();
    }

    protected RecordsClient getQueryApi() {
        return this.apiClient.records();
    }

    protected void setCommonCredentials(Credentials commonCredentials) throws SkyflowException {
        this.commonCredentials = commonCredentials;
        super.prioritiseCredentials(this.vaultConfig.getCredentials());
    }

    protected synchronized void setBearerToken() throws SkyflowException {
        super.setBearerToken(this.vaultConfig.getCredentials());
        if (apiClient == null) {
            updateExecutorInHTTP();
            this.apiClient = this.apiClientBuilder.build();
        }
    }

    /**
     * Adopts an updated config in place, so a VaultController reference the caller already holds
     * keeps working instead of silently serving the previous config. Discards the cached HTTP and
     * API clients; the bearer token is re-resolved by setBearerToken, which drops it when the
     * effective credentials changed.
     */
    protected void setVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
        this.vaultConfig = vaultConfig;
        this.sharedHttpClient = null;
        this.apiClient = null;
        updateVaultUrl();
    }

    /**
     * Re-resolves the vault URL from the current config. The constructor resolves it once, so a
     * vaultUrl supplied later through updateVaultConfig would otherwise never take effect.
     */
    protected void refreshVaultUrl() throws SkyflowException {
        updateVaultUrl();
    }

    private void updateVaultUrl() throws SkyflowException {
        // Fetch vaultUrl from ENV
        String vaultUrl = Utils.getEnvVaultUrl();

        // If vaultUrl from ENV is null or empty, fetch vaultUrl from vault config
        if (vaultUrl == null || vaultUrl.isEmpty()) {
            vaultUrl = this.vaultConfig.getVaultUrl();
        }

        // If vaultUrl from vault config is also null or empty, construct vaultUrl from clusterId passed in vault config
        if (vaultUrl == null || vaultUrl.isEmpty()) {
            vaultUrl = Utils.getVaultUrl(this.vaultConfig.getClusterId(), this.vaultConfig.getEnv());
        }
        this.apiClientBuilder.url(vaultUrl);
        if (!vaultUrl.equals(this.currentVaultURL)) {
            this.currentVaultURL = vaultUrl;
            this.apiClient = null;
        }
    }

    protected void updateExecutorInHTTP() {
        if (sharedHttpClient == null) {
            int timeoutSeconds = resolveInt(vaultConfig.getTimeout(), commonTimeout, DEFAULT_TIMEOUT_SECONDS);
            int maxRetries = resolveInt(vaultConfig.getMaxRetries(), commonMaxRetries, DEFAULT_MAX_RETRIES);
            long initialRetryDelayMillis = resolveLong(vaultConfig.getInitialRetryDelayMillis(),
                    commonInitialRetryDelayMillis, DEFAULT_INITIAL_RETRY_DELAY_MILLIS);
            long maxRetryDelayMillis = resolveLong(vaultConfig.getMaxRetryDelayMillis(),
                    commonMaxRetryDelayMillis, DEFAULT_MAX_RETRY_DELAY_MILLIS);
            // Per-attempt timeouts: null => leave OkHttp's built-in default (backward compatible).
            Integer connectTimeout = resolveNullableInt(vaultConfig.getConnectTimeout(), commonConnectTimeout);
            Integer readTimeout = resolveNullableInt(vaultConfig.getReadTimeout(), commonReadTimeout);
            Integer writeTimeout = resolveNullableInt(vaultConfig.getWriteTimeout(), commonWriteTimeout);

            OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder()
                    .connectionPool(new ConnectionPool(10, 1, TimeUnit.MINUTES))
                    // Overall ceiling; bounds the whole call including retries.
                    .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    // OUTER: retries. Must wrap the auth interceptor so each attempt re-reads the
                    // (possibly refreshed) bearer token rather than replaying a stale one.
                    .addInterceptor(new SkyflowRetryInterceptor(maxRetries, initialRetryDelayMillis, maxRetryDelayMillis))
                    .addInterceptor(chain -> {  // INNER: auth
                        Request requestWithAuth = chain.request().newBuilder()
                                .header("Authorization", "Bearer " + this.token)
                                .build();
                        return chain.proceed(requestWithAuth);
                    });
            if (connectTimeout != null) {
                httpBuilder.connectTimeout(connectTimeout, TimeUnit.SECONDS);
            }
            if (readTimeout != null) {
                httpBuilder.readTimeout(readTimeout, TimeUnit.SECONDS);
            }
            if (writeTimeout != null) {
                httpBuilder.writeTimeout(writeTimeout, TimeUnit.SECONDS);
            }
            sharedHttpClient = httpBuilder.build();
            apiClientBuilder.httpClient(sharedHttpClient);
        }
    }

}
