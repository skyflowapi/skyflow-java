package com.skyflow.vault.controller;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.vault.detect.DeidentifyFileSource;
import com.skyflow.vault.detect.DeidentifySourceType;
import com.skyflow.vault.detect.ReidentifyFileSource;
import com.skyflow.vault.detect.ReidentifySourceType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Utils;

/**
 * Skyflow Detect operations for a skyvault vault, returned by {@code Skyflow.detect()} and
 * {@code Skyflow.detect(vaultId)}. Every Detect V2 method ({@code deidentifyString},
 * {@code deidentifyFile}, {@code getRun}, {@code reidentifyString}, {@code reidentifyFile},
 * {@code checkGuardrails}) is inherited from {@link BaseDetectController} and shared with the
 * flowvault SDK; this class supplies the skyvault URL rule, the SDK metrics, and the mapping of a
 * response's {@code outputType} onto this SDK's {@code DeidentifySourceType} / {@code ReidentifySourceType}.
 *
 * <p>Vault-specific types for skyvault: {@code DeidentifySourceType} offers {@code BASE64},
 * {@code SKYFLOW_ID} and {@code PRESIGNED_URL}; {@code ReidentifySourceType} offers {@code BASE64} and
 * {@code PRESIGNED_URL}; {@code RedactionLevel} rules use {@code entityName} and {@code redactionType};
 * a {@code VAULT_TOKEN} entity's {@code destination} is a {@code table.column} pair.
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
}
