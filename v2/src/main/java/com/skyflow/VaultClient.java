package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.api.RecordsApi;
import com.skyflow.generated.rest.api.TokensApi;
import com.skyflow.utils.Utils;
import io.github.cdimascio.dotenv.Dotenv;

public class VaultClient {
    private final RecordsApi recordsApi;
    private final TokensApi tokensApi;
    private final ApiClient apiClient;
    private final VaultConfig vaultConfig;
    private Credentials commonCredentials;
    private Credentials finalCredentials;

    protected VaultClient(VaultConfig vaultConfig, Credentials credentials) {
        super();
        this.vaultConfig = vaultConfig;
        this.commonCredentials = credentials;
        this.apiClient = new ApiClient();
        this.tokensApi = new TokensApi(this.apiClient);
        this.recordsApi = new RecordsApi(this.apiClient);
        updateVaultURL();
        prioritiseCredentials();
    }

    protected Credentials getFinalCredentials() {
        return finalCredentials;
    }

    protected RecordsApi getRecordsApi() {
        return recordsApi;
    }

    protected TokensApi getTokensApi() {
        return tokensApi;
    }

    protected ApiClient getApiClient() {
        return apiClient;
    }

    protected VaultConfig getVaultConfig() {
        return vaultConfig;
    }

    protected void setCommonCredentials(Credentials commonCredentials) {
        this.commonCredentials = commonCredentials;
        prioritiseCredentials();
    }

    protected void updateVaultConfig() {
        updateVaultURL();
        prioritiseCredentials();
    }

    private void updateVaultURL() {
        String vaultURL = Utils.getVaultURL(this.vaultConfig.getClusterId(), this.vaultConfig.getEnv());
        this.apiClient.setBasePath(vaultURL);
    }

    private void prioritiseCredentials() {
        try {
            if (this.vaultConfig.getCredentials() != null) {
                this.finalCredentials = this.vaultConfig.getCredentials();
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
