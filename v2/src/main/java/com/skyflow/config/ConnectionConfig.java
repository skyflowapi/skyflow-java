package com.skyflow.config;

public class ConnectionConfig {
    private String connectionId;
    private String connectionUrl;
    private Credentials credentials;

    public ConnectionConfig() {
        this.connectionId = null;
        this.connectionUrl = null;
        this.credentials = null;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }
}
