package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.Constants;
import com.skyflow.utils.SdkVersion;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.controller.VaultController;

import java.util.LinkedHashMap;

public final class Skyflow extends BaseSkyflow<Skyflow, VaultConfig> {

    private final SkyflowClientBuilder builder;

    private Skyflow(SkyflowClientBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    @Override
    protected Skyflow self() {
        return this;
    }

    public static SkyflowClientBuilder builder() {
        SdkVersion.setSdkPrefix(Constants.SDK_PREFIX);
        return new SkyflowClientBuilder();
    }

    public VaultConfig getVaultConfig() {
        Object[] array = this.builder.vaultConfigMap.values().toArray();
        return (VaultConfig) array[0];
    }

    /**
     * Updates a vault's configuration on an already-built client.
     * <p>
     * BaseSkyflow.updateVaultConfig goes straight to the template, bypassing the builder's own
     * override, so the flowvault-specific fields have to be carried across here too — otherwise
     * a vaultUrl or HTTP setting supplied through this entry point would be silently dropped
     * while the same call on the builder honoured it.
     */
    @Override
    public Skyflow updateVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
        super.updateVaultConfig(vaultConfig);
        this.builder.carryVaultOverrides(vaultConfig);
        return this;
    }

    public VaultController vault() throws SkyflowException {
        return resolveOrThrow(this.builder.vaultClientsMap, null, ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST, ErrorMessage.VaultIdNotInConfigList);
    }

    public VaultController vault(String vaultId) throws SkyflowException {
        return resolveOrThrow(this.builder.vaultClientsMap, vaultId, ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST, ErrorMessage.VaultIdNotInConfigList);
    }


    public static final class SkyflowClientBuilder extends BaseSkyflowClientBuilder<VaultConfig> {
        private final LinkedHashMap<String, VaultController> vaultClientsMap = new LinkedHashMap<>();
        // Client-wide HTTP config. Resolution per vault, most specific first:
        //   VaultConfig value -> the value set here -> SDK default (60s call timeout, 0 retries).
        // null here means "not set", so the SDK default applies to vaults that don't override it.
        // Only null means inherit: an explicit 0 is a real value and wins over the level below.
        private Integer timeout;
        private Integer connectTimeout;
        private Integer readTimeout;
        private Integer writeTimeout;
        private Integer maxRetries;
        private Long initialRetryDelayMillis;
        private Long maxRetryDelayMillis;

        @Override
        protected void validateVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
            Validations.validateVaultConfiguration(vaultConfig);
        }

        @Override
        protected void onVaultConfigAdded(VaultConfig vaultConfig) throws SkyflowException {
            VaultController controller = new VaultController(vaultConfig, this.skyflowCredentials);
            controller.setCommonHttpConfig(this.timeout, this.connectTimeout, this.readTimeout,
                    this.writeTimeout, this.maxRetries, this.initialRetryDelayMillis,
                    this.maxRetryDelayMillis);
            this.vaultClientsMap.put(vaultConfig.getVaultId(), controller);
            LogUtil.printInfoLog(Utils.parameterizedString(InfoLogs.VAULT_CONTROLLER_INITIALIZED.getLog(), vaultConfig.getVaultId()));
        }

        @Override
        protected void onVaultConfigUpdated(VaultConfig updatedConfig) throws SkyflowException {
            // Update the existing controller in place — replacing it would leave any VaultController
            // reference the caller already holds pointing at the previous config.
            VaultController updated = this.vaultClientsMap.get(updatedConfig.getVaultId());
            if (updated == null) {
                updated = new VaultController(updatedConfig, this.skyflowCredentials);
                this.vaultClientsMap.put(updatedConfig.getVaultId(), updated);
            } else {
                updated.setVaultConfig(updatedConfig);
            }
            updated.setCommonHttpConfig(this.timeout, this.connectTimeout, this.readTimeout,
                    this.writeTimeout, this.maxRetries, this.initialRetryDelayMillis,
                    this.maxRetryDelayMillis);
        }

        @Override
        protected void onVaultConfigRemoved(String vaultId) throws SkyflowException {
            this.vaultClientsMap.remove(vaultId);
        }

        @Override
        protected boolean hasVaultClient(String vaultId) {
            return this.vaultClientsMap.containsKey(vaultId);
        }

        @Override
        protected void onCredentialsUpdated(Credentials credentials) throws SkyflowException {
            for (VaultController vault : this.vaultClientsMap.values()) {
                vault.setCommonCredentials(credentials);
            }
        }

        @Override
        public SkyflowClientBuilder addVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
            super.addVaultConfig(vaultConfig);
            return this;
        }

        @Override
        public SkyflowClientBuilder updateVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
            super.updateVaultConfig(vaultConfig);
            carryVaultOverrides(vaultConfig);
            return this;
        }

        /**
         * BaseSkyflow.mergeVaultConfig() only carries env, clusterId and credentials across, so the
         * flowvault-specific fields on an incoming update — vaultUrl and the HTTP settings — would
         * be dropped silently. Apply them to the merged config the new controller is holding. A null
         * on the incoming config means "leave as is", matching how the base class merges every
         * other field.
         */
        private void carryVaultOverrides(VaultConfig incoming) throws SkyflowException {
            VaultConfig merged = this.vaultConfigMap.get(incoming.getVaultId());
            if (merged == null || merged == incoming) {
                return;
            }
            if (incoming.getTimeout() != null) {
                merged.setTimeout(incoming.getTimeout());
            }
            if (incoming.getConnectTimeout() != null) {
                merged.setConnectTimeout(incoming.getConnectTimeout());
            }
            if (incoming.getReadTimeout() != null) {
                merged.setReadTimeout(incoming.getReadTimeout());
            }
            if (incoming.getWriteTimeout() != null) {
                merged.setWriteTimeout(incoming.getWriteTimeout());
            }
            if (incoming.getMaxRetries() != null) {
                merged.setMaxRetries(incoming.getMaxRetries());
            }
            if (incoming.getInitialRetryDelayMillis() != null) {
                merged.setInitialRetryDelayMillis(incoming.getInitialRetryDelayMillis());
            }
            if (incoming.getMaxRetryDelayMillis() != null) {
                merged.setMaxRetryDelayMillis(incoming.getMaxRetryDelayMillis());
            }
            // The HTTP settings above are resolved lazily on the next request, but the URL is
            // resolved once in the VaultClient constructor — which already ran with the old value.
            if (incoming.getVaultUrl() != null) {
                merged.setVaultUrl(incoming.getVaultUrl());
                VaultController controller = this.vaultClientsMap.get(incoming.getVaultId());
                if (controller != null) {
                    controller.refreshVaultUrl();
                }
            }
        }

        @Override
        public SkyflowClientBuilder removeVaultConfig(String vaultId) throws SkyflowException {
            super.removeVaultConfig(vaultId);
            return this;
        }

        @Override
        public SkyflowClientBuilder addSkyflowCredentials(Credentials credentials) throws SkyflowException {
            super.addSkyflowCredentials(credentials);
            return this;
        }

        @Override
        public SkyflowClientBuilder setLogLevel(LogLevel logLevel) {
            super.setLogLevel(logLevel);
            return this;
        }

        /**
         * Overall call timeout in seconds, including retries. Default 60.
         * <p>
         * <b>Precedence:</b> a vault that sets {@link VaultConfig#setTimeout(Integer)} wins; this
         * value applies only to vaults that leave it unset.
         */
        public SkyflowClientBuilder timeout(int timeout) {
            this.timeout = timeout;
            propagateHttpConfig();
            return this;
        }

        /**
         * Per-attempt connection-establishment timeout in seconds. Unset =&gt; HTTP client default (10s).
         * <p>
         * <b>Precedence:</b> a vault that sets {@link VaultConfig#setConnectTimeout(Integer)} wins;
         * this value applies only to vaults that leave it unset.
         */
        public SkyflowClientBuilder connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            propagateHttpConfig();
            return this;
        }

        /**
         * Per-attempt response-read timeout in seconds. Unset =&gt; HTTP client default (10s).
         * <p>
         * <b>Precedence:</b> a vault that sets {@link VaultConfig#setReadTimeout(Integer)} wins;
         * this value applies only to vaults that leave it unset.
         */
        public SkyflowClientBuilder readTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            propagateHttpConfig();
            return this;
        }

        /**
         * Per-attempt request-write timeout in seconds. Unset =&gt; HTTP client default (10s).
         * <p>
         * <b>Precedence:</b> a vault that sets {@link VaultConfig#setWriteTimeout(Integer)} wins;
         * this value applies only to vaults that leave it unset.
         */
        public SkyflowClientBuilder writeTimeout(int writeTimeout) {
            this.writeTimeout = writeTimeout;
            propagateHttpConfig();
            return this;
        }

        /**
         * Retry attempts after the first failure. Default 0 — retries are opt-in so non-idempotent
         * bulk writes are not replayed automatically.
         * <p>
         * <b>Precedence:</b> a vault that sets {@link VaultConfig#setMaxRetries(Integer)} wins;
         * this value applies only to vaults that leave it unset.
         */
        public SkyflowClientBuilder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            propagateHttpConfig();
            return this;
        }

        /**
         * Backoff before the first retry, in milliseconds. Default 500. Only applies when
         * {@code maxRetries} is greater than zero.
         * <p>
         * <b>Precedence:</b> a vault that sets {@link VaultConfig#setInitialRetryDelayMillis(Long)}
         * wins; this value applies only to vaults that leave it unset.
         */
        public SkyflowClientBuilder initialRetryDelayMillis(long initialRetryDelayMillis) {
            this.initialRetryDelayMillis = initialRetryDelayMillis;
            propagateHttpConfig();
            return this;
        }

        /**
         * Ceiling the exponential backoff grows to, in milliseconds. Default 2000. Only applies
         * when {@code maxRetries} is greater than zero.
         * <p>
         * <b>Precedence:</b> a vault that sets {@link VaultConfig#setMaxRetryDelayMillis(Long)}
         * wins; this value applies only to vaults that leave it unset.
         */
        public SkyflowClientBuilder maxRetryDelayMillis(long maxRetryDelayMillis) {
            this.maxRetryDelayMillis = maxRetryDelayMillis;
            propagateHttpConfig();
            return this;
        }

        /** Push the current client-wide HTTP settings onto every vault controller built so far. */
        private void propagateHttpConfig() {
            for (VaultController vault : this.vaultClientsMap.values()) {
                vault.setCommonHttpConfig(this.timeout, this.connectTimeout, this.readTimeout,
                        this.writeTimeout, this.maxRetries, this.initialRetryDelayMillis,
                        this.maxRetryDelayMillis);
            }
        }

        public Skyflow build() {
            return new Skyflow(this);
        }
    }

}