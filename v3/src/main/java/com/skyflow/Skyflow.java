package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.ErrorCode;
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

public final class Skyflow extends BaseSkyflow {
    private final SkyflowClientBuilder builder;

    private Skyflow(SkyflowClientBuilder builder) {
        super(builder);
        this.builder = builder;
        com.skyflow.utils.logger.LogUtil.printInfoLog(InfoLogs.CLIENT_INITIALIZED.getLog());
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
        Object[] array = this.builder.vaultClientsMap.keySet().toArray();
        if (array.length < 1) {
            LogUtil.printErrorLog(ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.VaultIdNotInConfigList.getMessage());
        }
        String vaultId = (String) array[0];
        VaultController controller = this.builder.vaultClientsMap.get(vaultId);
        if (controller == null) {
            LogUtil.printErrorLog(ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.VaultIdNotInConfigList.getMessage());
        }
        
        return controller;
    }

    public static final class SkyflowClientBuilder extends BaseSkyflowClientBuilder {
        private final LinkedHashMap<String, VaultConfig> vaultConfigMap;
        private final LinkedHashMap<String, VaultController> vaultClientsMap;

        public SkyflowClientBuilder() {
            this.vaultConfigMap = new LinkedHashMap<>();
            this.vaultClientsMap = new LinkedHashMap<>();
        }

        public SkyflowClientBuilder addVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_VAULT_CONFIG.getLog());
            Validations.validateVaultConfiguration(vaultConfig);
            VaultConfig vaultConfigCopy;
            try {
                vaultConfigCopy = (VaultConfig) vaultConfig.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
            if (!this.vaultClientsMap.isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.VAULT_CONFIG_EXISTS.getLog(), vaultConfigCopy.getVaultId()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                        ErrorMessage.VaultIdAlreadyInConfigList.getMessage());
            } else {
                this.vaultConfigMap.put(vaultConfigCopy.getVaultId(), vaultConfigCopy); // add new config in map
                this.vaultClientsMap.put(vaultConfigCopy.getVaultId(), new VaultController(vaultConfigCopy, this.skyflowCredentials)); // add new controller with new config
                LogUtil.printInfoLog(Utils.parameterizedString(
                        InfoLogs.VAULT_CONTROLLER_INITIALIZED.getLog(), vaultConfigCopy.getVaultId()));
            }
            return this;
        }

        public SkyflowClientBuilder addSkyflowCredentials(Credentials credentials) throws SkyflowException {
            Validations.validateCredentials(credentials);
            Credentials credentialsCopy;
            try {
                credentialsCopy = (Credentials) credentials.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
            this.skyflowCredentials = credentialsCopy;
            for (VaultController vault : this.vaultClientsMap.values()) {
                vault.setCommonCredentials(this.skyflowCredentials);
            }
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
