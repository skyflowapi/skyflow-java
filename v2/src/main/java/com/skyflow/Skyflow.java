package com.skyflow;

import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.controller.ConnectionController;
import com.skyflow.vault.controller.VaultController;

import java.util.LinkedHashMap;

public class Skyflow {
    private final SkyflowClientBuilder builder;

    private Skyflow(SkyflowClientBuilder builder) {
        this.builder = builder;
    }

    public static SkyflowClientBuilder builder() {
        return new SkyflowClientBuilder();
    }

    public Skyflow addVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
        this.builder.addVaultConfig(vaultConfig);
        return this;
    }

    public VaultConfig getVaultConfig(String vaultId) {
        // get vault config with vault id
        return this.builder.vaultsMap.get(vaultId).getVaultConfig();
    }

    public Skyflow addConnectionConfig(ConnectionConfig connectionConfig) {
        this.builder.addConnectionConfig(connectionConfig);
        return this;
    }

    public ConnectionConfig getConnectionConfig(String connectionId) {
        // get connection config with connection id
        return this.builder.connectionsMap.get(connectionId).getConnectionConfig();
    }

    public Skyflow addSkyflowCredentials(Credentials credentials) {
        this.builder.addSkyflowCredentials(credentials);
        return this;
    }

    public LogLevel getLogLevel() {
        return this.builder.logLevel;
    }

    // in case no id is passed, return first vault controller
    public VaultController vault() {
        String vaultId = (String) this.builder.vaultsMap.keySet().toArray()[0];
        return this.vault(vaultId);
    }

    public VaultController vault(String vaultId) {
        return this.builder.vaultsMap.get(vaultId);
    }

    // in case no id is passed, return first connection controller
    public ConnectionController connection() {
        String connectionId = (String) this.builder.connectionsMap.keySet().toArray()[0];
        return this.connection(connectionId);
    }

    public ConnectionController connection(String connectionId) {
        return this.builder.connectionsMap.get(connectionId);
    }

    public static final class SkyflowClientBuilder {
        private final LinkedHashMap<String, VaultController> vaultsMap;
        private final LinkedHashMap<String, ConnectionController> connectionsMap;
        private Credentials skyflowCredentials;
        private LogLevel logLevel;

        public SkyflowClientBuilder() {
            this.vaultsMap = new LinkedHashMap<>();
            this.connectionsMap = new LinkedHashMap<>();
            this.skyflowCredentials = null;
            this.logLevel = LogLevel.ERROR;
        }

        public SkyflowClientBuilder addVaultConfig(VaultConfig vaultConfig) {
            // check if vaultConfig already exists
            if (this.vaultsMap.containsKey(vaultConfig.getVaultId())) {
                // display error log, throw error, or both
            } else {
                this.vaultsMap.put(vaultConfig.getVaultId(), new VaultController(vaultConfig, this.skyflowCredentials));
            }
            return this;
        }

        public SkyflowClientBuilder updateVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
            if (this.vaultsMap.containsKey(vaultConfig.getVaultId())) {
                VaultController controller = this.vaultsMap.get(vaultConfig.getVaultId());
                controller.setVaultConfig(vaultConfig);
            } else {
                // display error log, throw error, or both
            }
            return this;
        }

        public SkyflowClientBuilder removeVaultConfig(String vaultId) {
            if (this.vaultsMap.containsKey(vaultId)) {
                this.vaultsMap.remove(vaultId);
            } else {
                // display error log, throw error, or both
            }
            return this;
        }


        public SkyflowClientBuilder addConnectionConfig(ConnectionConfig connectionConfig) {
            // check if connectionConfig already exists
            if (this.connectionsMap.containsKey(connectionConfig.getConnectionId())) {
                // display error log, throw error, or both
            } else {
                ConnectionController controller = new ConnectionController(connectionConfig, this.skyflowCredentials);
                this.connectionsMap.put(connectionConfig.getConnectionId(), controller);
            }
            return this;
        }

        public SkyflowClientBuilder updateConnectionConfig(ConnectionConfig connectionConfig) {
            if (this.connectionsMap.containsKey(connectionConfig.getConnectionId())) {
                ConnectionController controller = this.connectionsMap.get(connectionConfig.getConnectionId());
                controller.setConnectionConfig(connectionConfig);
            } else {
                // display error log, throw error, or both
            }
            return this;
        }

        public SkyflowClientBuilder removeConnectionConfig(String connectionId) {
            if (this.connectionsMap.containsKey(connectionId)) {
                this.connectionsMap.remove(connectionId);
            } else {
                // display error log, throw error, or both
            }
            return this;
        }

        public SkyflowClientBuilder addSkyflowCredentials(Credentials credentials) {
            // set credentials and update in vault controllers
            this.skyflowCredentials = credentials;
            for (VaultController vault : this.vaultsMap.values()) {
                vault.setCommonCredentials(this.skyflowCredentials);
            }
            for (ConnectionController controller : this.connectionsMap.values()) {
                controller.setCommonCredentials(this.skyflowCredentials);
            }
            return this;
        }

        public SkyflowClientBuilder updateSkyflowCredentials(Credentials credentials) {
            // set credentials and update in vault and connection controllers
            this.skyflowCredentials = credentials;
            for (VaultController vault : this.vaultsMap.values()) {
                vault.setCommonCredentials(this.skyflowCredentials);
            }
            for (ConnectionController controller : this.connectionsMap.values()) {
                controller.setCommonCredentials(this.skyflowCredentials);
            }
            return this;
        }

        public SkyflowClientBuilder setLogLevel(LogLevel logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public SkyflowClientBuilder updateLogLevel(LogLevel logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public Skyflow build() {
            // return built skyflow client instance
            return new Skyflow(this);
        }
    }
}
