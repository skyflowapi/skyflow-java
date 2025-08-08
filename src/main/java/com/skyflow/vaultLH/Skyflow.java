package com.skyflow.vaultLH;

import com.skyflow.common.config.Credentials;
import com.skyflow.common.config.VaultConfig;
import com.skyflow.common.enums.Env;
import com.skyflow.common.enums.LogLevel;
import com.skyflow.common.errors.ErrorCode;
import com.skyflow.common.errors.ErrorMessage;
import com.skyflow.common.errors.SkyflowException;
import com.skyflow.common.logs.ErrorLogs;
import com.skyflow.common.logs.InfoLogs;
import com.skyflow.common.utils.CommonUtils;
import com.skyflow.common.logger.LogUtil;
import com.skyflow.common.utils.Constants;
import com.skyflow.common.utils.SdkVersion;
import com.skyflow.common.utils.Validations;
import com.skyflow.vaultLH.vault.controller.VaultController;

import java.util.LinkedHashMap;

public final class Skyflow {
    private final SkyflowClientBuilder builder;

    private Skyflow(SkyflowClientBuilder builder) {
        this.builder = builder;
        LogUtil.printInfoLog(InfoLogs.CLIENT_INITIALIZED.getLog());
    }

    public static SkyflowClientBuilder builder() {
        SdkVersion.setSdkPrefix(Constants.SDK_PREFIX_V3);
        return new SkyflowClientBuilder();
    }

    public Skyflow addVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
        this.builder.addVaultConfig(vaultConfig);
        return this;
    }

    public VaultConfig getVaultConfig(String vaultId) {
        return this.builder.vaultConfigMap.get(vaultId);
    }

    public Skyflow updateVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
        this.builder.updateVaultConfig(vaultConfig);
        return this;
    }

    public Skyflow removeVaultConfig(String vaultId) throws SkyflowException {
        this.builder.removeVaultConfig(vaultId);
        return this;
    }

    public Skyflow updateSkyflowCredentials(Credentials credentials) throws SkyflowException {
        this.builder.addSkyflowCredentials(credentials);
        return this;
    }

    public Skyflow updateLogLevel(LogLevel logLevel) {
        this.builder.setLogLevel(logLevel);
        return this;
    }

    public LogLevel getLogLevel() {
        return this.builder.logLevel;
    }

    public VaultController vault() throws SkyflowException {
        Object[] array = this.builder.vaultClientsMap.keySet().toArray();
        if (array.length < 1) {
            LogUtil.printErrorLog(ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.VaultIdNotInConfigList.getMessage());
        }
        String vaultId = (String) array[0];
        return this.vault(vaultId);
    }

    public VaultController vault(String vaultId) throws SkyflowException {
        VaultController controller = this.builder.vaultClientsMap.get(vaultId);
        if (controller == null) {
            LogUtil.printErrorLog(ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.VaultIdNotInConfigList.getMessage());
        }
        return controller;
    }
    public static final class SkyflowClientBuilder {
        private final LinkedHashMap<String, VaultController> vaultClientsMap;
        private final LinkedHashMap<String, VaultConfig> vaultConfigMap;
        private Credentials skyflowCredentials;
        private LogLevel logLevel;

        public SkyflowClientBuilder() {
            this.vaultClientsMap = new LinkedHashMap<>();
            this.vaultConfigMap = new LinkedHashMap<>();
            this.skyflowCredentials = null;
            this.logLevel = LogLevel.ERROR;
        }

        public SkyflowClientBuilder addVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_VAULT_CONFIG.getLog());
            Validations.validateVaultConfig(vaultConfig);
            if (this.vaultClientsMap.containsKey(vaultConfig.getVaultId())) {
                LogUtil.printErrorLog(CommonUtils.parameterizedString(
                        ErrorLogs.VAULT_CONFIG_EXISTS.getLog(), vaultConfig.getVaultId()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                        ErrorMessage.VaultIdAlreadyInConfigList.getMessage());
            } else {
                this.vaultConfigMap.put(vaultConfig.getVaultId(), vaultConfig);
                this.vaultClientsMap.put(vaultConfig.getVaultId(), new VaultController(vaultConfig, this.skyflowCredentials));
                LogUtil.printInfoLog(CommonUtils.parameterizedString(
                        InfoLogs.VAULT_CONTROLLER_INITIALIZED.getLog(), vaultConfig.getVaultId()));
                LogUtil.printInfoLog(CommonUtils.parameterizedString(
                        InfoLogs.DETECT_CONTROLLER_INITIALIZED.getLog(), vaultConfig.getVaultId()));
            }
            return this;
        }

        public SkyflowClientBuilder updateVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_VAULT_CONFIG.getLog());
            Validations.validateVaultConfig(vaultConfig);
            if (this.vaultClientsMap.containsKey(vaultConfig.getVaultId())) {
                VaultConfig updatedConfig = findAndUpdateVaultConfig(vaultConfig);
                this.vaultClientsMap.get(updatedConfig.getVaultId()).updateVaultConfig();
            } else {
                LogUtil.printErrorLog(CommonUtils.parameterizedString(
                        ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST.getLog(), vaultConfig.getVaultId()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.VaultIdNotInConfigList.getMessage());
            }
            return this;
        }

        public SkyflowClientBuilder removeVaultConfig(String vaultId) throws SkyflowException {
            if (this.vaultClientsMap.containsKey(vaultId)) {
                this.vaultClientsMap.remove(vaultId);
                this.vaultConfigMap.remove(vaultId);
            } else {
                LogUtil.printErrorLog(CommonUtils.parameterizedString(ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST.getLog(), vaultId));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.VaultIdNotInConfigList.getMessage());
            }
            return this;
        }
        public SkyflowClientBuilder addSkyflowCredentials(Credentials credentials) throws SkyflowException {
            Validations.validateCredentials(credentials);
            this.skyflowCredentials = credentials;
            for (VaultController vault : this.vaultClientsMap.values()) {
                vault.setCommonCredentials(this.skyflowCredentials);
            }
            return this;
        }

        public SkyflowClientBuilder setLogLevel(LogLevel logLevel) {
            this.logLevel = logLevel == null ? LogLevel.ERROR : logLevel;
            LogUtil.setupLogger(this.logLevel);
            LogUtil.printInfoLog(CommonUtils.parameterizedString(
                    InfoLogs.CURRENT_LOG_LEVEL.getLog(), String.valueOf(logLevel)
            ));
            return this;
        }

        public Skyflow build() {
            return new Skyflow(this);
        }

        private VaultConfig findAndUpdateVaultConfig(VaultConfig vaultConfig) {
            VaultConfig previousConfig = this.vaultConfigMap.get(vaultConfig.getVaultId());
            Env env = vaultConfig.getEnv() != null ? vaultConfig.getEnv() : previousConfig.getEnv();
            String clusterId = vaultConfig.getClusterId() != null ? vaultConfig.getClusterId() : previousConfig.getClusterId();
            Credentials credentials = vaultConfig.getCredentials() != null ? vaultConfig.getCredentials() : previousConfig.getCredentials();
            previousConfig.setEnv(env);
            previousConfig.setClusterId(clusterId);
            previousConfig.setCredentials(credentials);
            return previousConfig;
        }
    }
}
