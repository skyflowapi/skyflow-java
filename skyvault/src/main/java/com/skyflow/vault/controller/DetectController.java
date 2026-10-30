package com.skyflow.vault.controller;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Utils;

/**
 * Skyflow Detect operations for a skyvault vault, returned by {@code Skyflow.detect()} and
 * {@code Skyflow.detect(vaultId)}. Every Detect V2 method ({@code deidentifyString},
 * {@code deidentifyFile}, {@code reidentifyString}, {@code getRun}, {@code checkGuardrails}) is
 * inherited from {@link BaseDetectController} and shared with the flowvault SDK; this class only
 * supplies the skyvault URL rule and SDK metrics.
 *
 * <p>For skyvault vaults a {@code VAULT_TOKEN} entity's {@code destination} is a {@code table.column}
 * pair, and {@code RedactionLevel} rules use {@code entityName} and {@code redactionType}.
 */
public final class DetectController extends BaseDetectController<VaultConfig> {
    private final String vaultUrlOverride;

    public DetectController(VaultConfig vaultConfig, Credentials credentials) {
        this(vaultConfig, credentials, null);
    }

    /** Test seam: points the controller at an explicit base URL instead of the cluster-derived one. */
    DetectController(VaultConfig vaultConfig, Credentials credentials, String vaultUrlOverride) {
        super(vaultConfig, credentials);
        this.vaultUrlOverride = vaultUrlOverride;
    }

    @Override
    protected String resolveVaultUrl() throws SkyflowException {
        if (vaultUrlOverride != null) {
            return vaultUrlOverride;
        }
        return Utils.getVaultURL(getVaultConfig().getClusterId(), getVaultConfig().getEnv());
    }

    @Override
    protected String getSdkMetrics() {
        return Utils.getMetrics().toString();
    }
}
