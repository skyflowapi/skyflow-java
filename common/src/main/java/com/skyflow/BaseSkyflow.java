package com.skyflow;

import com.skyflow.config.BaseVaultConfig;
import com.skyflow.config.Credentials;
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
import java.util.Map;


abstract class BaseSkyflow<Self extends BaseSkyflow, V extends BaseVaultConfig> implements ISkyflow<Self, V, Credentials> {
    protected final BaseSkyflowClientBuilder<V> builder;

    protected BaseSkyflow(BaseSkyflowClientBuilder<V> builder) {
        this.builder = builder;
        LogUtil.printInfoLog(InfoLogs.CLIENT_INITIALIZED.getLog());
    }

    protected abstract Self self();

    @Override
    public Self addVaultConfig(V vaultConfig) throws SkyflowException {
        this.builder.addVaultConfigTemplate(vaultConfig);
        return self();
    }

    public V getVaultConfig(String vaultId) {
        return this.builder.vaultConfigMap.get(vaultId);
    }

    @Override
    public Self updateVaultConfig(V vaultConfig) throws SkyflowException {
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

    protected static <T> T resolveOrThrow(Map<String, T> map, String key,
                                           ErrorLogs errorLog, ErrorMessage errorMessage) throws SkyflowException {
        T value = key != null ? map.get(key) : map.values().stream().findFirst().orElse(null);
        if (value == null) {
            LogUtil.printErrorLog(errorLog.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), errorMessage.getMessage());
        }
        return value;
    }

    abstract static class BaseSkyflowClientBuilder<V extends BaseVaultConfig> {
        protected final LinkedHashMap<String, V> vaultConfigMap = new LinkedHashMap<>();
        protected Credentials skyflowCredentials;
        protected LogLevel logLevel = LogLevel.ERROR;

        protected BaseSkyflowClientBuilder() {
        }

        public BaseSkyflowClientBuilder<V> addVaultConfig(V vaultConfig) throws SkyflowException {
            addVaultConfigTemplate(vaultConfig);
            return this;
        }

        public BaseSkyflowClientBuilder<V> updateVaultConfig(V vaultConfig) throws SkyflowException {
            updateVaultConfigTemplate(vaultConfig);
            return this;
        }

        public BaseSkyflowClientBuilder<V> removeVaultConfig(String vaultId) throws SkyflowException {
            removeVaultConfigTemplate(vaultId);
            return this;
        }

        public BaseSkyflowClientBuilder<V> addSkyflowCredentials(Credentials credentials) throws SkyflowException {
            addSkyflowCredentialsTemplate(credentials);
            return this;
        }

        protected BaseSkyflowClientBuilder<V> setLogLevel(LogLevel logLevel) {
            this.logLevel = logLevel == null ? LogLevel.ERROR : logLevel;
            LogUtil.setupLogger(this.logLevel);
            LogUtil.printInfoLog(BaseUtils.parameterizedString(
                    InfoLogs.CURRENT_LOG_LEVEL.getLog(), String.valueOf(this.logLevel)
            ));
            return this;
        }

        protected final void addVaultConfigTemplate(V vaultConfig) throws SkyflowException {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_VAULT_CONFIG.getLog());
            validateVaultConfig(vaultConfig);
            V vaultConfigCopy = cloneVaultConfig(vaultConfig);
            String vaultId = extractVaultId(vaultConfigCopy);
            if (this.vaultConfigMap.containsKey(vaultId)) {
                LogUtil.printErrorLog(BaseUtils.parameterizedString(
                        ErrorLogs.VAULT_CONFIG_EXISTS.getLog(), vaultId
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                        ErrorMessage.VaultIdAlreadyInConfigList.getMessage());
            }
            this.vaultConfigMap.put(vaultId, vaultConfigCopy);
            onVaultConfigAdded(vaultConfigCopy);
        }

        protected final void updateVaultConfigTemplate(V vaultConfig) throws SkyflowException {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_VAULT_CONFIG.getLog());
            validateVaultConfig(vaultConfig);
            String vaultId = extractVaultId(vaultConfig);
            if (!this.vaultConfigMap.containsKey(vaultId)) {
                LogUtil.printErrorLog(BaseUtils.parameterizedString(
                        ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST.getLog(), vaultId
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.VaultIdNotInConfigList.getMessage());
            }
            V previousConfig = this.vaultConfigMap.get(vaultId);
            V merged = mergeVaultConfig(vaultConfig, previousConfig);
            onVaultConfigUpdated(merged);
        }

        protected final void removeVaultConfigTemplate(String vaultId) throws SkyflowException {
            if (!this.vaultConfigMap.containsKey(vaultId)) {
                LogUtil.printErrorLog(BaseUtils.parameterizedString(ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST.getLog(), vaultId));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.VaultIdNotInConfigList.getMessage());
            }
            onVaultConfigRemoved(vaultId);
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

        protected abstract void validateVaultConfig(V vaultConfig) throws SkyflowException;

        @SuppressWarnings("unchecked")
        protected final V cloneVaultConfig(V vaultConfig) {
            try {
                return (V) vaultConfig.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        protected final String extractVaultId(V vaultConfig) {
            return vaultConfig.getVaultId();
        }

        protected final V mergeVaultConfig(V incoming, V existing) {
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

        protected abstract void onVaultConfigAdded(V vaultConfig) throws SkyflowException;

        protected abstract void onVaultConfigUpdated(V updatedConfig) throws SkyflowException;

        protected abstract void onVaultConfigRemoved(String vaultId) throws SkyflowException;

        protected abstract void onCredentialsUpdated(Credentials credentials) throws SkyflowException;
    }

}
