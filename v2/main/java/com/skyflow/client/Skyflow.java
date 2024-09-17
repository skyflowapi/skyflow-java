package com.skyflow.client;

import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.config.ManagementConfig;
import com.skyflow.config.VaultConfig;
import com.skyflow.management.controller.management.ManagementController;
import com.skyflow.utils.Builder;
import com.skyflow.utils.LogLevel;
import com.skyflow.vault.controller.connections.ConnectionsController;
import com.skyflow.vault.controller.vault.VaultController;

public class Skyflow {
    Builder builder;
    // other members

    public Skyflow(VaultConfig vaultConfig, ConnectionConfig connectionConfig, ManagementConfig managementConfig, Credentials credentials) {
        // set members accordingly
    }

    public Builder Builder() {
        builder = new Builder();
        return builder;
    }

    public Builder addVaultConfig(VaultConfig vaultConfig) {
        // add vault config
        return builder;
    }

    public VaultConfig getVaultConfig(String vaultId) {
        // get vault config with vault id
        return null;
    }

    public Builder updateVaultConfig(VaultConfig vaultConfig) {
        // update vault config with vault id
        return builder;
    }

    public Builder removeVaultConfig(String vaultId) {
        // remove vault config with vault id
        return builder;
    }

    public Builder addConnectionConfig(ConnectionConfig connectionConfig) {
        // add connection config
        return builder;
    }

    public ConnectionConfig getConnectionConfig(String connectionId) {
        // get connection config with connection id
        return null;
    }

    public Builder updateConnectionConfig(ConnectionConfig connectionConfig) {
        // update connection config with connection id
        return builder;
    }

    public Builder removeConnectionConfig(String connectionId) {
        // remove connection config with connection id
        return builder;
    }

    public Builder addManagementConfig(ManagementConfig managementConfig) {
        // add management config
        return builder;
    }

    public ManagementConfig getManagementConfig(String managementId) {
        // get management config with management id
        return null;
    }

    public Builder updateManagementConfig(ManagementConfig managementConfig) {
        // update management config with management id
        return builder;
    }

    public Builder removeManagementConfig(String managementId) {
        // remove management config with management id
        return builder;
    }

    public Builder addSkyflowCredentials(Credentials credentials) {
        // set credentials
        return builder;
    }

    public Builder updateSkyflowCredentials(Credentials credentials) {
        // set credentials
        return builder;
    }

    public Builder setLogLevel(LogLevel logLevel) {
        // set log level
        return builder;
    }

    public LogLevel getLogLevel() {
        // get log level
        return null;
    }

    public Builder updateLogLevel(LogLevel logLevel) {
        // update log level
        return builder;
    }

    public Skyflow build() {
        // return built skyflow client instance
        return this;
    }

    public VaultController vault(String vaultId) {
        // (cache) - store the vault object in a list, don't create object if object already exits
        // return vault Object using static func
        return null;
    }

    public ConnectionsController connection(String connectionId) {
        // (cache) - store the connection object in a list, don't create object if object already exits
        // return connection Object static func
        return null;
    }

    public ManagementController management() {
        // cache management object if created
        // return management object using static func
        return null;
    }
}
