package com.skyflow.vault.controller;

import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.vault.connection.InvokeConnectionRequest;
import com.skyflow.vault.connection.InvokeConnectionResponse;

public class ConnectionController {
    private ConnectionConfig connectionConfig;
    private Credentials commonCredentials;

    public ConnectionController(ConnectionConfig connectionConfig, Credentials credentials) {
        this.connectionConfig = connectionConfig;
        this.commonCredentials = credentials;
    }

    public void setCommonCredentials(Credentials commonCredentials) {
        this.commonCredentials = commonCredentials;
    }

    public ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public void setConnectionConfig(ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    // check in python interfaces
    public InvokeConnectionResponse invoke(InvokeConnectionRequest invokeConnectionRequest) {
        // invoke the connection
        return null;
    }
}
