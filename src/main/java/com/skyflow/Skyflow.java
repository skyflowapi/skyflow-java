package com.skyflow;

import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.controller.ConnectionController;
import com.skyflow.vault.controller.VaultController;

import java.util.LinkedHashMap;

public final class Skyflow {
    private final SkyflowClientBuilder builder;

    private Skyflow(SkyflowClientBuilder builder) {
        this.builder = builder;
        LogUtil.printInfoLog(InfoLogs.CLIENT_INITIALIZED.getLog());
    }

    public static SkyflowClientBuilder builder() {
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

    public Skyflow addConnectionConfig(ConnectionConfig connectionConfig) throws SkyflowException {
        this.builder.addConnectionConfig(connectionConfig);
        return this;
    }

    public ConnectionConfig getConnectionConfig(String connectionId) {
        return this.builder.connectionConfigMap.get(connectionId);
    }

    public Skyflow updateConnectionConfig(ConnectionConfig connectionConfig) throws SkyflowException {
        this.builder.updateConnectionConfig(connectionConfig);
        return this;
    }

    public Skyflow removeConnectionConfig(String connectionId) throws SkyflowException {
        this.builder.removeConnectionConfig(connectionId);
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

    public VaultController vault() {
        String vaultId = (String) this.builder.vaultClientsMap.keySet().toArray()[0];
        return this.vault(vaultId);
    }

    public VaultController vault(String vaultId) {
        return this.builder.vaultClientsMap.get(vaultId);
    }

    public ConnectionController connection() {
        String connectionId = (String) this.builder.connectionsMap.keySet().toArray()[0];
        return this.connection(connectionId);
    }

    public ConnectionController connection(String connectionId) {
        return this.builder.connectionsMap.get(connectionId);
    }

    public static final class SkyflowClientBuilder {
        private final LinkedHashMap<String, ConnectionController> connectionsMap;
        private final LinkedHashMap<String, VaultController> vaultClientsMap;
        private final LinkedHashMap<String, VaultConfig> vaultConfigMap;
        private final LinkedHashMap<String, ConnectionConfig> connectionConfigMap;
        private Credentials skyflowCredentials;
        private LogLevel logLevel;

        public SkyflowClientBuilder() {
            this.vaultClientsMap = new LinkedHashMap<>();
            this.vaultConfigMap = new LinkedHashMap<>();
            this.connectionsMap = new LinkedHashMap<>();
            this.connectionConfigMap = new LinkedHashMap<>();
            this.skyflowCredentials = null;
            this.logLevel = LogLevel.ERROR;
        }

        public SkyflowClientBuilder addVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_VAULT_CONFIG.getLog());
            Validations.validateVaultConfig(vaultConfig);
            if (this.vaultClientsMap.containsKey(vaultConfig.getVaultId())) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.VAULT_CONFIG_EXISTS.getLog(), vaultConfig.getVaultId()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                        ErrorMessage.VaultIdAlreadyInConfigList.getMessage());
            } else {
                this.vaultConfigMap.put(vaultConfig.getVaultId(), vaultConfig);
                this.vaultClientsMap.put(vaultConfig.getVaultId(), new VaultController(vaultConfig, this.skyflowCredentials));
                LogUtil.printInfoLog(Utils.parameterizedString(
                        InfoLogs.VAULT_CONTROLLER_INITIALIZED.getLog(), vaultConfig.getVaultId()));
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
                LogUtil.printErrorLog(Utils.parameterizedString(
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
                LogUtil.printErrorLog(Utils.parameterizedString(ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST.getLog(), vaultId));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.VaultIdNotInConfigList.getMessage());
            }
            return this;
        }

        public SkyflowClientBuilder addConnectionConfig(ConnectionConfig connectionConfig) throws SkyflowException {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_CONNECTION_CONFIG.getLog());
            Validations.validateConnectionConfig(connectionConfig);
            if (this.connectionsMap.containsKey(connectionConfig.getConnectionId())) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.CONNECTION_CONFIG_EXISTS.getLog(), connectionConfig.getConnectionId()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                        ErrorMessage.ConnectionIdAlreadyInConfigList.getMessage());
            } else {
                this.connectionConfigMap.put(connectionConfig.getConnectionId(), connectionConfig);
                ConnectionController controller = new ConnectionController(connectionConfig, this.skyflowCredentials);
                this.connectionsMap.put(connectionConfig.getConnectionId(), controller);
                LogUtil.printInfoLog(Utils.parameterizedString(
                        InfoLogs.CONNECTION_CONTROLLER_INITIALIZED.getLog(), connectionConfig.getConnectionId()));
            }
            return this;
        }

        public SkyflowClientBuilder updateConnectionConfig(ConnectionConfig connectionConfig) throws SkyflowException {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_CONNECTION_CONFIG.getLog());
            Validations.validateConnectionConfig(connectionConfig);
            if (this.connectionsMap.containsKey(connectionConfig.getConnectionId())) {
                ConnectionConfig updatedConfig = findAndUpdateConnectionConfig(connectionConfig);
                this.connectionsMap.get(updatedConfig.getConnectionId()).updateConnectionConfig(connectionConfig);
            } else {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.CONNECTION_CONFIG_DOES_NOT_EXIST.getLog(), connectionConfig.getConnectionId()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                        ErrorMessage.ConnectionIdNotInConfigList.getMessage());
            }
            return this;
        }

        public SkyflowClientBuilder removeConnectionConfig(String connectionId) throws SkyflowException {
            if (this.connectionsMap.containsKey(connectionId)) {
                this.connectionsMap.remove(connectionId);
                this.connectionConfigMap.remove(connectionId);
            } else {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.CONNECTION_CONFIG_DOES_NOT_EXIST.getLog(), connectionId
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                        ErrorMessage.ConnectionIdNotInConfigList.getMessage());
            }
            return this;
        }

        public SkyflowClientBuilder addSkyflowCredentials(Credentials credentials) throws SkyflowException {
            Validations.validateCredentials(credentials);
            this.skyflowCredentials = credentials;
            for (VaultController vault : this.vaultClientsMap.values()) {
                vault.setCommonCredentials(this.skyflowCredentials);
            }
            for (ConnectionController connection : this.connectionsMap.values()) {
                connection.setCommonCredentials(this.skyflowCredentials);
            }
            return this;
        }

        public SkyflowClientBuilder setLogLevel(LogLevel logLevel) {
            this.logLevel = logLevel == null ? LogLevel.ERROR : logLevel;
            LogUtil.setupLogger(this.logLevel);
            LogUtil.printInfoLog(Utils.parameterizedString(
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

        private ConnectionConfig findAndUpdateConnectionConfig(ConnectionConfig connectionConfig) {
            ConnectionConfig previousConfig = this.connectionConfigMap.get(connectionConfig.getConnectionId());
            String connectionURL = connectionConfig.getConnectionUrl() != null ? connectionConfig.getConnectionUrl() : previousConfig.getConnectionUrl();
            Credentials credentials = connectionConfig.getCredentials() != null ? connectionConfig.getCredentials() : previousConfig.getCredentials();
            previousConfig.setConnectionUrl(connectionURL);
            previousConfig.setCredentials(credentials);
            return previousConfig;
        }
    }
}