package com.skyflow.client;

import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.config.ManagementConfig;
import com.skyflow.config.VaultConfig;
import com.skyflow.management.service.ManagementService;
import com.skyflow.utils.Builder;
import com.skyflow.utils.LogLevel;
import com.skyflow.vault.controller.connections.ConnectionsController;
import com.skyflow.vault.service.VaultService;

import java.util.ArrayList;

public class SkyflowClient {
    ArrayList<VaultConfig> vaultConfigs = new ArrayList<>();
    ArrayList<ConnectionConfig> connectionConfigs = new ArrayList<>();
    ArrayList<ManagementConfig> managementConfigs = new ArrayList<>();
    Credentials credentials;
    Builder builder;
    // other members

    public SkyflowClient(VaultConfig vaultConfig, ConnectionConfig connectionConfig, ManagementConfig managementConfig, Credentials credentials) {
        this.vaultConfigs.add(vaultConfig);
        this.connectionConfigs.add(connectionConfig);
        this.managementConfigs.add(managementConfig);
        this.credentials = credentials;
    }

    public Builder Builder() {
        builder = new Builder();
        return builder;
    }

    public Builder addVaultConfig(VaultConfig vaultConfig) {
        // add vault config
        return builder;
    }

    public Builder removeVaultConfig(String vaultId) {
        // remove vault config with vault id
        return builder;
    }

    public Builder updateVaultConfig(String vaultId, VaultConfig vaultConfig) {
        // update vault config with vault id
        return builder;
    }

    public Builder addConnectionConfig(ConnectionConfig connectionConfig) {
        // add connection config
        return builder;
    }

    public Builder removeConnectionConfig(String connectionId, ConnectionConfig connectionConfig) {
        // remove connection config with connection id
        return builder;
    }

    public Builder updateConnectionConfig(String connectionId, ConnectionConfig connectionConfig) {
        // update connection config with connection id
        return builder;
    }

    public Builder addManagementConfig(ManagementConfig managementConfig) {
        // add management config
        return builder;
    }

    public Builder removeManagementConfig(String managementId, ManagementConfig managementConfig) {
        // remove management config with management id
        return builder;
    }

    public Builder updateManagementConfig(String managementId, ManagementConfig managementConfig) {
        // update management config with management id
        return builder;
    }

    public Builder setCredentials(Credentials credentials) {
        // set credentials
        return builder;
    }

    public Builder logLevel(LogLevel logLevel) {
        // set log level
        return builder;
    }

    public Builder updateLogLevel(LogLevel logLevel) {
        // update log level
        return builder;
    }

    public SkyflowClient build() {
        // return built skyflow client instance
        return this;
    }

    public VaultService vault(String vaultId) {
        // (cache) - store the vault object in a list, don't create object if object already exits
        // return vault Object using static func
        return null;
    }

    public ConnectionsController connection(String connectionId) {
        // (cache) - store the connection object in a list, don't create object if object already exits
        // return connection Object static func
        return null;
    }

    public ManagementService management() {
        // cache management object if created
        // return management object using static func
        return null;
    }
}
