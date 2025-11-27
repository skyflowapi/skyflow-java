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
import com.skyflow.utils.Constants;
import com.skyflow.utils.SdkVersion;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.controller.ConnectionController;
import com.skyflow.vault.controller.DetectController;
import com.skyflow.vault.controller.VaultController;

import java.util.LinkedHashMap;

public final class Skyflow extends BaseSkyflow {
    private final SkyflowClientBuilder builder;

    private Skyflow(SkyflowClientBuilder builder) {
        super(builder);
        this.builder = builder;
        LogUtil.printInfoLog(InfoLogs.CLIENT_INITIALIZED.getLog());
    }

    public static SkyflowClientBuilder builder() {
        SdkVersion.setSdkPrefix(Constants.SDK_PREFIX);
        return new SkyflowClientBuilder();
    }

    public Skyflow addVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
        this.builder.addVaultConfig(vaultConfig);
        return this;
    }

    public VaultConfig getVaultConfig() {
        Object[] array = this.builder.vaultConfigMap.values().toArray();
        return (VaultConfig) array[0];
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

    public ConnectionController connection() throws SkyflowException {
        Object[] array = this.builder.connectionsMap.keySet().toArray();
        if (array.length < 1) {
            LogUtil.printErrorLog(ErrorLogs.CONNECTION_CONFIG_DOES_NOT_EXIST.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.ConnectionIdNotInConfigList.getMessage());
        }
        String connectionId = (String) array[0];
        return this.connection(connectionId);
    }

    public ConnectionController connection(String connectionId) throws SkyflowException {
        ConnectionController controller = this.builder.connectionsMap.get(connectionId);
        if (controller == null) {
            LogUtil.printErrorLog(ErrorLogs.CONNECTION_CONFIG_DOES_NOT_EXIST.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.ConnectionIdNotInConfigList.getMessage());
        }
        return controller;
    }

    public DetectController detect() throws SkyflowException {
        Object[] array = this.builder.detectClientsMap.keySet().toArray();
        if (array.length < 1) {
            LogUtil.printErrorLog(ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.VaultIdNotInConfigList.getMessage());
        }
        String detectId = (String) array[0];
        return this.detect(detectId);
    }

    public DetectController detect(String vaultId) throws SkyflowException {
        DetectController controller = this.builder.detectClientsMap.get(vaultId);
        if (controller == null) {
            LogUtil.printErrorLog(ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.VaultIdNotInConfigList.getMessage());
        }
        return controller;
    }

    public static final class SkyflowClientBuilder extends BaseSkyflowClientBuilder {
        private final LinkedHashMap<String, VaultConfig> vaultConfigMap;
        private final LinkedHashMap<String, ConnectionController> connectionsMap;
        private final LinkedHashMap<String, VaultController> vaultClientsMap;
        private final LinkedHashMap<String, DetectController> detectClientsMap;
        private final LinkedHashMap<String, ConnectionConfig> connectionConfigMap;

        public SkyflowClientBuilder() {
            super();
            this.vaultConfigMap = new LinkedHashMap<>();
            this.vaultClientsMap = new LinkedHashMap<>();
            this.detectClientsMap = new LinkedHashMap<>();
            this.connectionsMap = new LinkedHashMap<>();
            this.connectionConfigMap = new LinkedHashMap<>();
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
                this.detectClientsMap.put(vaultConfig.getVaultId(), new DetectController(vaultConfig, this.skyflowCredentials));
                LogUtil.printInfoLog(Utils.parameterizedString(
                        InfoLogs.VAULT_CONTROLLER_INITIALIZED.getLog(), vaultConfig.getVaultId()));
                LogUtil.printInfoLog(Utils.parameterizedString(
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
            for (DetectController detect : this.detectClientsMap.values()) {
                detect.setCommonCredentials(this.skyflowCredentials);
            }
            for (ConnectionController connection : this.connectionsMap.values()) {
                connection.setCommonCredentials(this.skyflowCredentials);
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
