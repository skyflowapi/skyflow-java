package com.skyflow;

import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.api.RecordsApi;
import com.skyflow.generated.rest.api.TokensApi;
import io.github.cdimascio.dotenv.Dotenv;

public class ConnectionClient {

    private final ConnectionConfig connectionConfig;
    private Credentials commonCredentials;
    private Credentials finalCredentials;
    private final ApiClient apiClient;


    protected ConnectionClient(ConnectionConfig connectionConfig, Credentials credentials) {
        super();
        this.connectionConfig = connectionConfig;
        this.commonCredentials = credentials;
        this.apiClient = new ApiClient();
        prioritiseCredentials();
    }

    protected Credentials getFinalCredentials() {
        return finalCredentials;
    }

    protected ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    protected void setCommonCredentials(Credentials commonCredentials) {
        this.commonCredentials = commonCredentials;
        prioritiseCredentials();
    }

    protected ApiClient getApiClient() {
        return apiClient;
    }

    private void prioritiseCredentials() {
        try {
            if (this.connectionConfig.getCredentials() != null) {
                this.finalCredentials = this.connectionConfig.getCredentials();
            } else if (this.commonCredentials != null) {
                this.finalCredentials = this.commonCredentials;
            } else {
                Dotenv dotenv = Dotenv.load();
                String sysCredentials = dotenv.get("SKYFLOW_CREDENTIALS");
                if (sysCredentials == null) {
                    // throw error for not passing any credentials
                } else {
                    this.finalCredentials = new Credentials();
                    this.finalCredentials.setCredentialsString(sysCredentials);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
