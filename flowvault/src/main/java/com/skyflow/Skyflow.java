package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.Constants;
import com.skyflow.utils.SdkVersion;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.controller.VaultController;

public final class Skyflow extends BaseSkyflow<Skyflow, VaultConfig, VaultController> {

    private Skyflow(SkyflowClientBuilder builder) {
        super(builder);
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

    public static final class SkyflowClientBuilder extends BaseSkyflowClientBuilder<VaultConfig, VaultController> {

        @Override
        protected void validateVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
            Validations.validateVaultConfiguration(vaultConfig);
        }

        @Override
        protected VaultConfig cloneVaultConfig(VaultConfig vaultConfig) {
            try {
                return (VaultConfig) vaultConfig.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected String extractVaultId(VaultConfig vaultConfig) {
            return vaultConfig.getVaultId();
        }

        @Override
        protected VaultConfig mergeVaultConfig(VaultConfig incoming, VaultConfig existing) {
            if (incoming.getEnv() != null) {
                existing.setEnv(incoming.getEnv());
            }
            if (incoming.getClusterId() != null) {
                existing.setClusterId(incoming.getClusterId());
            }
            if (incoming.getCredentials() != null) {
                try {
                    existing.setCredentials((Credentials) incoming.getCredentials().clone());
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            }
            return existing;
        }

        @Override
        protected void onVaultConfigAdded(VaultConfig vaultConfig) throws SkyflowException {
            this.vaultClientsMap.put(vaultConfig.getVaultId(), new VaultController(vaultConfig, this.skyflowCredentials));
            LogUtil.printInfoLog(Utils.parameterizedString(InfoLogs.VAULT_CONTROLLER_INITIALIZED.getLog(), vaultConfig.getVaultId()));
        }

        @Override
        protected void onVaultConfigUpdated(VaultConfig updatedConfig) throws SkyflowException {
            this.vaultClientsMap.put(updatedConfig.getVaultId(), new VaultController(updatedConfig, this.skyflowCredentials));
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
            return this;
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

        public Skyflow build() {
            return new Skyflow(this);
        }
    }

}