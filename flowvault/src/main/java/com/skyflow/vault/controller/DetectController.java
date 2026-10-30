package com.skyflow.vault.controller;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Utils;

/**
 * Skyflow Detect operations for a flowvault vault, returned by {@code Skyflow.detect()} and
 * {@code Skyflow.detect(vaultId)}. The shared Detect V2 methods are inherited from
 * {@link BaseDetectController} and shared with the skyvault SDK; this class only supplies the
 * flowvault URL rules, SDK metrics and the vault-level timeout and retry overrides from
 * {@link VaultConfig}.
 */
public final class DetectController extends BaseDetectController<VaultConfig> {
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_MAX_RETRIES = 0;
    private static final long DEFAULT_INITIAL_RETRY_DELAY_MILLIS = 500L;
    private static final long DEFAULT_MAX_RETRY_DELAY_MILLIS = 2000L;

    public DetectController(VaultConfig vaultConfig, Credentials credentials) {
        super(vaultConfig, credentials);
    }

    /** Same precedence as the vault controller: environment variable, explicit vaultUrl, then cluster id and env. */
    @Override
    protected String resolveVaultUrl() throws SkyflowException {
        String vaultUrl = Utils.getEnvVaultUrl();
        if (vaultUrl == null || vaultUrl.isEmpty()) {
            vaultUrl = getVaultConfig().getVaultUrl();
        }
        if (vaultUrl == null || vaultUrl.isEmpty()) {
            vaultUrl = Utils.getVaultUrl(getVaultConfig().getClusterId(), getVaultConfig().getEnv());
        }
        return vaultUrl;
    }

    @Override
    protected String getSdkMetrics() {
        return Utils.getMetrics().toString();
    }

    @Override
    protected Integer effectiveTimeoutSeconds() {
        return firstNonNull(getVaultConfig().getTimeout(), super.effectiveTimeoutSeconds(), DEFAULT_TIMEOUT_SECONDS);
    }

    @Override
    protected Integer effectiveMaxRetries() {
        return firstNonNull(getVaultConfig().getMaxRetries(), super.effectiveMaxRetries(), DEFAULT_MAX_RETRIES);
    }

    @Override
    protected Long effectiveInitialRetryDelayMillis() {
        return firstNonNull(getVaultConfig().getInitialRetryDelayMillis(), super.effectiveInitialRetryDelayMillis(),
                DEFAULT_INITIAL_RETRY_DELAY_MILLIS);
    }

    @Override
    protected Long effectiveMaxRetryDelayMillis() {
        return firstNonNull(getVaultConfig().getMaxRetryDelayMillis(), super.effectiveMaxRetryDelayMillis(),
                DEFAULT_MAX_RETRY_DELAY_MILLIS);
    }

    private static <T> T firstNonNull(T vaultLevel, T clientLevel, T defaultValue) {
        if (vaultLevel != null) {
            return vaultLevel;
        }
        return clientLevel != null ? clientLevel : defaultValue;
    }
}
