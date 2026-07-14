package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.BaseUtils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.BaseValidations;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

abstract class BaseSkyflow<Self, VC> implements ISkyflow<Self, VaultConfig, Credentials, VC> {
    protected final BaseSkyflowClientBuilder<VC> builder;

    protected BaseSkyflow(BaseSkyflowClientBuilder<VC> builder) {
        this.builder = builder;
        LogUtil.printInfoLog(InfoLogs.CLIENT_INITIALIZED.getLog());
    }

    protected abstract Self self();

    @Override
    public Self addVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
        this.builder.addVaultConfigTemplate(vaultConfig);
        return self();
    }

    public VaultConfig getVaultConfig(String vaultId) {
        return this.builder.vaultConfigMap.get(vaultId);
    }

    @Override
    public Self updateVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
        this.builder.updateVaultConfigTemplate(vaultConfig);
        return self();
    }

    @Override
    public Self removeVaultConfig(String vaultId) throws SkyflowException {
        this.builder.removeVaultConfigTemplate(vaultId);
        return self();
    }

    @Override
    public Self updateSkyflowCredentials(Credentials credentials) throws SkyflowException {
        this.builder.addSkyflowCredentialsTemplate(credentials);
        return self();
    }

    @Override
    public Self setLogLevel(LogLevel logLevel) {
        this.builder.setLogLevel(logLevel);
        return self();
    }

    @Override
    public LogLevel getLogLevel() {
        return this.builder.logLevel;
    }

    @Override
    public VC vault() throws SkyflowException {
        return resolveOrThrow(this.builder.vaultClientsMap, null, ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST, ErrorMessage.VaultIdNotInConfigList);
    }

    protected static <T> T resolveOrThrow(Map<String, T> map, String key,
                                           ErrorLogs errorLog, ErrorMessage errorMessage) throws SkyflowException {
        T value = key != null ? map.get(key) : map.values().stream().findFirst().orElse(null);
        if (value == null) {
            LogUtil.printErrorLog(errorLog.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), errorMessage.getMessage());
        }
        return value;
    }

    abstract static class BaseSkyflowClientBuilder<VC> {
        protected final LinkedHashMap<String, VaultConfig> vaultConfigMap = new LinkedHashMap<>();
        protected final LinkedHashMap<String, VC> vaultClientsMap = new LinkedHashMap<>();
        protected Credentials skyflowCredentials;
        protected LogLevel logLevel = LogLevel.ERROR;

        protected BaseSkyflowClientBuilder() {
        }

        public BaseSkyflowClientBuilder<VC> addVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
            addVaultConfigTemplate(vaultConfig);
            return this;
        }

        public BaseSkyflowClientBuilder<VC> updateVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
            updateVaultConfigTemplate(vaultConfig);
            return this;
        }

        public BaseSkyflowClientBuilder<VC> removeVaultConfig(String vaultId) throws SkyflowException {
            removeVaultConfigTemplate(vaultId);
            return this;
        }

        public BaseSkyflowClientBuilder<VC> addSkyflowCredentials(Credentials credentials) throws SkyflowException {
            addSkyflowCredentialsTemplate(credentials);
            return this;
        }

        protected BaseSkyflowClientBuilder<VC> setLogLevel(LogLevel logLevel) {
            this.logLevel = logLevel == null ? LogLevel.ERROR : logLevel;
            LogUtil.setupLogger(this.logLevel);
            LogUtil.printInfoLog(BaseUtils.parameterizedString(
                    InfoLogs.CURRENT_LOG_LEVEL.getLog(), String.valueOf(this.logLevel)
            ));
            return this;
        }

        protected final void addVaultConfigTemplate(VaultConfig vaultConfig) throws SkyflowException {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_VAULT_CONFIG.getLog());
            validateVaultConfig(vaultConfig);
            VaultConfig vaultConfigCopy;
            try {
                vaultConfigCopy = (VaultConfig) vaultConfig.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
            if (this.vaultClientsMap.containsKey(vaultConfigCopy.getVaultId())) {
                LogUtil.printErrorLog(BaseUtils.parameterizedString(
                        ErrorLogs.VAULT_CONFIG_EXISTS.getLog(), vaultConfigCopy.getVaultId()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                        ErrorMessage.VaultIdAlreadyInConfigList.getMessage());
            }
            this.vaultConfigMap.put(vaultConfigCopy.getVaultId(), vaultConfigCopy);
            onVaultConfigAdded(vaultConfigCopy);
        }

        protected final void updateVaultConfigTemplate(VaultConfig vaultConfig) throws SkyflowException {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_VAULT_CONFIG.getLog());
            validateVaultConfig(vaultConfig);
            if (!this.vaultClientsMap.containsKey(vaultConfig.getVaultId())) {
                LogUtil.printErrorLog(BaseUtils.parameterizedString(
                        ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST.getLog(), vaultConfig.getVaultId()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.VaultIdNotInConfigList.getMessage());
            }
            VaultConfig previousConfig = this.vaultConfigMap.get(vaultConfig.getVaultId());
            if (vaultConfig.getEnv() != null) {
                previousConfig.setEnv(vaultConfig.getEnv());
            }
            if (vaultConfig.getClusterId() != null) {
                previousConfig.setClusterId(vaultConfig.getClusterId());
            }
            if (vaultConfig.getCredentials() != null) {
                try {
                    previousConfig.setCredentials((Credentials) vaultConfig.getCredentials().clone());
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            }
            onVaultConfigUpdated(previousConfig);
        }

        protected final void removeVaultConfigTemplate(String vaultId) throws SkyflowException {
            if (!this.vaultClientsMap.containsKey(vaultId)) {
                LogUtil.printErrorLog(BaseUtils.parameterizedString(ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST.getLog(), vaultId));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.VaultIdNotInConfigList.getMessage());
            }
            this.vaultClientsMap.remove(vaultId);
            this.vaultConfigMap.remove(vaultId);
        }

        protected final void addSkyflowCredentialsTemplate(Credentials credentials) throws SkyflowException {
            BaseValidations.validateCredentials(credentials);
            Credentials credentialsCopy;
            try {
                credentialsCopy = (Credentials) credentials.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
            this.skyflowCredentials = credentialsCopy;
            onCredentialsUpdated(credentialsCopy);
        }

        protected abstract void validateVaultConfig(VaultConfig vaultConfig) throws SkyflowException;

        protected abstract void onVaultConfigAdded(VaultConfig vaultConfig) throws SkyflowException;

        protected abstract void onVaultConfigUpdated(VaultConfig updatedConfig) throws SkyflowException;

        protected abstract void onCredentialsUpdated(Credentials credentials) throws SkyflowException;
    }

}
