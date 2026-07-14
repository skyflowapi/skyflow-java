package com.skyflow;

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


abstract class BaseSkyflow<Self, V, VC> implements ISkyflow<Self, V, Credentials, VC> {
    protected final BaseSkyflowClientBuilder<V, VC> builder;

    protected BaseSkyflow(BaseSkyflowClientBuilder<V, VC> builder) {
        this.builder = builder;
        LogUtil.printInfoLog(InfoLogs.CLIENT_INITIALIZED.getLog());
    }

    protected abstract Self self();

    public Self addVaultConfig(V vaultConfig) throws SkyflowException {
        this.builder.addVaultConfigTemplate(vaultConfig);
        return self();
    }

    public V getVaultConfig(String vaultId) {
        return this.builder.vaultConfigMap.get(vaultId);
    }

    public Self updateVaultConfig(V vaultConfig) throws SkyflowException {
        this.builder.updateVaultConfigTemplate(vaultConfig);
        return self();
    }

    public Self removeVaultConfig(String vaultId) throws SkyflowException {
        this.builder.removeVaultConfigTemplate(vaultId);
        return self();
    }

    public Self updateSkyflowCredentials(Credentials credentials) throws SkyflowException {
        this.builder.addSkyflowCredentialsTemplate(credentials);
        return self();
    }

    public Self setLogLevel(LogLevel logLevel) {
        this.builder.setLogLevel(logLevel);
        return self();
    }

    public LogLevel getLogLevel() {
        return this.builder.logLevel;
    }

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

    abstract static class BaseSkyflowClientBuilder<V, VC> {
        protected final LinkedHashMap<String, V> vaultConfigMap = new LinkedHashMap<>();
        protected final LinkedHashMap<String, VC> vaultClientsMap = new LinkedHashMap<>();
        protected Credentials skyflowCredentials;
        protected LogLevel logLevel = LogLevel.ERROR;

        protected BaseSkyflowClientBuilder() {
        }

        public BaseSkyflowClientBuilder<V, VC> addVaultConfig(V vaultConfig) throws SkyflowException {
            addVaultConfigTemplate(vaultConfig);
            return this;
        }

        public BaseSkyflowClientBuilder<V, VC> updateVaultConfig(V vaultConfig) throws SkyflowException {
            updateVaultConfigTemplate(vaultConfig);
            return this;
        }

        public BaseSkyflowClientBuilder<V, VC> removeVaultConfig(String vaultId) throws SkyflowException {
            removeVaultConfigTemplate(vaultId);
            return this;
        }

        public BaseSkyflowClientBuilder<V, VC> addSkyflowCredentials(Credentials credentials) throws SkyflowException {
            addSkyflowCredentialsTemplate(credentials);
            return this;
        }

        protected BaseSkyflowClientBuilder<V, VC> setLogLevel(LogLevel logLevel) {
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
            if (this.vaultClientsMap.containsKey(vaultId)) {
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
            if (!this.vaultClientsMap.containsKey(vaultId)) {
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

        protected abstract void validateVaultConfig(V vaultConfig) throws SkyflowException;

        protected abstract V cloneVaultConfig(V vaultConfig);

        protected abstract String extractVaultId(V vaultConfig);

        protected abstract V mergeVaultConfig(V incoming, V existing);

        protected abstract void onVaultConfigAdded(V vaultConfig) throws SkyflowException;

        protected abstract void onVaultConfigUpdated(V updatedConfig) throws SkyflowException;

        protected abstract void onCredentialsUpdated(Credentials credentials) throws SkyflowException;
    }

}
