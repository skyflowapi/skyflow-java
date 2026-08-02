package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.ApiClientBuilder;
import com.skyflow.generated.rest.resources.flowservice.FlowserviceClient;
import com.skyflow.utils.Utils;

public class VaultClient extends BaseVaultClient<VaultConfig> {
    private final ApiClientBuilder apiClientBuilder;
    private ApiClient apiClient;

    protected VaultClient(VaultConfig vaultConfig, Credentials credentials) throws SkyflowException {
        super(vaultConfig, credentials);
        this.apiClientBuilder = new ApiClientBuilder();
        this.apiClient = null;
        updateVaultURL();
    }

    protected FlowserviceClient getRecordsApi() {
        return this.apiClient.flowservice();
    }

    protected void setCommonCredentials(Credentials commonCredentials) throws SkyflowException {
        this.commonCredentials = commonCredentials;
        super.prioritiseCredentials(this.vaultConfig.getCredentials());
    }

    protected synchronized void setBearerToken() throws SkyflowException {
        super.setBearerToken(this.vaultConfig.getCredentials());
        if (apiClient == null) {
            updateExecutorInHTTP();
            this.apiClient = this.apiClientBuilder.build();
        }
    }

    private void updateVaultURL() throws SkyflowException {
        // Fetch vaultURL from ENV
        String vaultURL = Utils.getEnvVaultURL();

        // If vaultURL from ENV is null or empty, fetch vaultURL from vault config
        if (vaultURL == null || vaultURL.isEmpty()) {
            vaultURL = this.vaultConfig.getVaultURL();
        }

        // If vaultURL from vault config is also null or empty, construct vaultURL from clusterId passed in vault config
        if (vaultURL == null || vaultURL.isEmpty()) {
            vaultURL = Utils.getVaultURL(this.vaultConfig.getClusterId(), this.vaultConfig.getEnv());
        }
        this.apiClientBuilder.url(vaultURL);
        if (!vaultURL.equals(this.currentVaultURL)) {
            this.currentVaultURL = vaultURL;
            this.apiClient = null;
        }
    }

    protected void updateExecutorInHTTP() {
        if (sharedHttpClient == null) {
            sharedHttpClient = buildSharedHttpClient(() -> this.token);
            apiClientBuilder.httpClient(sharedHttpClient);
        }
    }

}
