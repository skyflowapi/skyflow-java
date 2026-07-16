package com.skyflow;

import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
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
import com.skyflow.vault.controller.DetectController;
import com.skyflow.vault.controller.VaultController;

import java.util.LinkedHashMap;

public final class Skyflow extends BaseSkyflow<Skyflow, VaultConfig, VaultController> {
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
        return new SkyflowClientBuilder();
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

    /** @deprecated Use {@link #setLogLevel(LogLevel)} instead. */
    @Deprecated(since = "2.1", forRemoval = true)
    public Skyflow updateLogLevel(LogLevel logLevel) {
        LogUtil.printWarningLog(InfoLogs.DEPRECATED_UPDATE_LOG_LEVEL.getLog());
        return setLogLevel(logLevel);
    }

    public VaultController vault(String vaultId) throws SkyflowException {
        return resolveOrThrow(this.builder.vaultClientsMap, vaultId, ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST, ErrorMessage.VaultIdNotInConfigList);
    }

    public ConnectionController connection() throws SkyflowException {
        return resolveOrThrow(this.builder.connectionsMap, null, ErrorLogs.CONNECTION_CONFIG_DOES_NOT_EXIST, ErrorMessage.ConnectionIdNotInConfigList);
    }

    public ConnectionController connection(String connectionId) throws SkyflowException {
        return resolveOrThrow(this.builder.connectionsMap, connectionId, ErrorLogs.CONNECTION_CONFIG_DOES_NOT_EXIST, ErrorMessage.ConnectionIdNotInConfigList);
    }

    public DetectController detect() throws SkyflowException {
        return resolveOrThrow(this.builder.detectClientsMap, null, ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST, ErrorMessage.VaultIdNotInConfigList);
    }

    public DetectController detect(String vaultId) throws SkyflowException {
        return resolveOrThrow(this.builder.detectClientsMap, vaultId, ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST, ErrorMessage.VaultIdNotInConfigList);
    }

    public static final class SkyflowClientBuilder extends BaseSkyflowClientBuilder<VaultConfig, VaultController> {
        private final LinkedHashMap<String, ConnectionController> connectionsMap = new LinkedHashMap<>();
        private final LinkedHashMap<String, DetectController> detectClientsMap = new LinkedHashMap<>();
        private final LinkedHashMap<String, ConnectionConfig> connectionConfigMap = new LinkedHashMap<>();

        @Override
        protected void validateVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
            Validations.validateVaultConfig(vaultConfig);
        }

        @Override
        protected void onVaultConfigAdded(VaultConfig vaultConfig) throws SkyflowException {
            this.vaultClientsMap.put(vaultConfig.getVaultId(), new VaultController(vaultConfig, this.skyflowCredentials));
            this.detectClientsMap.put(vaultConfig.getVaultId(), new DetectController(vaultConfig, this.skyflowCredentials));
            LogUtil.printInfoLog(Utils.parameterizedString(InfoLogs.VAULT_CONTROLLER_INITIALIZED.getLog(), vaultConfig.getVaultId()));
            LogUtil.printInfoLog(Utils.parameterizedString(InfoLogs.DETECT_CONTROLLER_INITIALIZED.getLog(), vaultConfig.getVaultId()));
        }

        @Override
        protected void onVaultConfigUpdated(VaultConfig updatedConfig) throws SkyflowException {
            this.vaultClientsMap.get(updatedConfig.getVaultId()).updateVaultConfig();
        }

        @Override
        protected void onCredentialsUpdated(Credentials credentials) throws SkyflowException {
            for (VaultController vault : this.vaultClientsMap.values()) {
                vault.setCommonCredentials(credentials);
            }
            for (DetectController detect : this.detectClientsMap.values()) {
                detect.setCommonCredentials(credentials);
            }
            for (ConnectionController connection : this.connectionsMap.values()) {
                connection.setCommonCredentials(credentials);
            }
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