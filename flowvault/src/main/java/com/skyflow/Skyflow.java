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

    public VaultController vault() throws SkyflowException {
        return resolveOrThrow(this.builder.vaultClientsMap, null, ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST, ErrorMessage.VaultIdNotInConfigList);
    }

    public static final class SkyflowClientBuilder extends BaseSkyflowClientBuilder<VaultConfig> {
        private final LinkedHashMap<String, VaultController> vaultClientsMap = new LinkedHashMap<>();

        @Override
        protected void validateVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
            Validations.validateVaultConfiguration(vaultConfig);
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