package com.skyflow.vault.controller;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.detect.DeidentifyFileSource;
import com.skyflow.detect.DeidentifySourceType;
import com.skyflow.detect.ReidentifyFileSource;
import com.skyflow.detect.ReidentifySourceType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Utils;

/**
 * Skyflow Detect operations for a flowvault vault, returned by {@code Skyflow.detect()} and
 * {@code Skyflow.detect(vaultId)}. Every Detect V2 method ({@code deidentifyString},
 * {@code deidentifyFile}, {@code getRun}, {@code reidentifyString}, {@code reidentifyFile},
 * {@code checkGuardrails}) is inherited from {@link BaseDetectController} and shared with the
 * skyvault SDK; this class supplies the flowvault URL rules, the SDK metrics, the vault-level timeout
 * and retry overrides from {@link VaultConfig}, and the mapping of a response's {@code outputType} onto
 * this SDK's {@code DeidentifySourceType} / {@code ReidentifySourceType}.
 *
 * <p>Vault-specific types for flowvault: {@code DeidentifySourceType} and {@code ReidentifySourceType}
 * both offer {@code BASE64} and {@code PRESIGNED_URL} only (flowvault vaults do not accept a skyflow id);
 * {@code RedactionLevel} rules use {@code tokenGroupName} and {@code redactionPattern}; a
 * {@code VAULT_TOKEN} entity's {@code destination} is a token-group name.
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
    protected DeidentifyFileSource toDeidentifySource(String wireValue) {
        return enumOrNull(DeidentifySourceType.class, wireValue);
    }

    @Override
    protected ReidentifyFileSource toReidentifySource(String wireValue) {
        return enumOrNull(ReidentifySourceType.class, wireValue);
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
