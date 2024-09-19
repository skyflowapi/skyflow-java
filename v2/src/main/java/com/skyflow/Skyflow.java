package com.skyflow;

import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.LogLevel;
import com.skyflow.vault.controller.ConnectionController;
import com.skyflow.vault.controller.VaultController;

import java.util.ArrayList;

public class Skyflow {
    private final ArrayList<VaultController> vaults;
    private final ArrayList<ConnectionController> connections;
    private Credentials skyflowCredentials;
    private LogLevel logLevel;

    private Skyflow(SkyflowClientBuilder builder) {
        this.vaults = builder.vaults;
        this.connections = builder.connections;
        this.logLevel = builder.logLevel;
        this.skyflowCredentials = builder.skyflowCredentials;
    }

    public static SkyflowClientBuilder builder() {
        return new SkyflowClientBuilder();
    }

    public VaultConfig getVaultConfig(String vaultId) {
        // get vault config with vault id
        return null;
    }

    public ConnectionConfig getConnectionConfig(String connectionId) {
        // get connection config with connection id
        return null;
    }

    public LogLevel getLogLevel() {
        // get log level
        return this.logLevel;
    }

    // in case no id is passed, return first vault controller
    public VaultController vault() {
        return this.vaults.get(0);
    }

    public VaultController vault(String vaultId) {
        // (cache) - store the vault object in a list, don't create object if object already exits
        // return vault Object using static func
        return null;
    }

    // in case no id is passed, return first connection controller
    public ConnectionController connection() {
        return this.connections.get(0);
    }

    public ConnectionController connection(String connectionId) {
        // (cache) - store the connection object in a list, don't create object if object already exits
        // return connection Object static func
        return null;
    }

    public static final class SkyflowClientBuilder {
        private final ArrayList<VaultController> vaults;
        private final ArrayList<ConnectionController> connections;
        private Credentials skyflowCredentials;
        private LogLevel logLevel;

        public SkyflowClientBuilder() {
            this.vaults = new ArrayList<>();
            this.connections = new ArrayList<>();
            this.skyflowCredentials = null;
            this.logLevel = LogLevel.ERROR;
        }

        public SkyflowClientBuilder addVaultConfig(VaultConfig vaultConfig) {
            // check if vaultConfig already exists
            // display error log in case add was not successful, throw error, or both
            return this;
        }

        public SkyflowClientBuilder updateVaultConfig(VaultConfig vaultConfig) {
            // update vault config with vault config
            // display error log in case update was not successful, throw error, or both
            return this;
        }

        public SkyflowClientBuilder removeVaultConfig(String vaultId) {
            // remove vault config with vault id
            // display error log in case remove was not successful, throw error, or both
            return this;
        }


        public SkyflowClientBuilder addConnectionConfig(ConnectionConfig connectionConfig) {
            // add connection config
            return this;
        }

        public SkyflowClientBuilder updateConnectionConfig(ConnectionConfig connectionConfig) {
            // update connection config with connection id
            return this;
        }

        public SkyflowClientBuilder removeConnectionConfig(String connectionId) {
            // remove connection config with connection id
            return this;
        }

        public SkyflowClientBuilder addSkyflowCredentials(Credentials credentials) {
            // set credentials
            this.skyflowCredentials = credentials;
            return this;
        }

        public SkyflowClientBuilder updateSkyflowCredentials(Credentials credentials) {
            // set credentials
            this.skyflowCredentials = credentials;
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
