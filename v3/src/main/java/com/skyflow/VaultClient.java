package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.ApiClientBuilder;
import com.skyflow.generated.rest.resources.recordservice.RecordserviceClient;
import com.skyflow.logs.InfoLogs;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;


public class VaultClient {
    private final VaultConfig vaultConfig;
    private final ApiClientBuilder apiClientBuilder;
    private ApiClient apiClient;
    private Credentials commonCredentials;
    private Credentials finalCredentials;
    private String token;
    private String apiKey;

    protected VaultClient(VaultConfig vaultConfig, Credentials credentials) {
        super();
        this.vaultConfig = vaultConfig;
        this.commonCredentials = credentials;
        this.apiClientBuilder = new ApiClientBuilder();
        this.apiClient = null;
        updateVaultURL();
    }

    protected RecordserviceClient getRecordsApi() {
        return this.apiClient.recordservice();
    }

    protected VaultConfig getVaultConfig() {
        return vaultConfig;
    }

    protected void setCommonCredentials(Credentials commonCredentials) throws SkyflowException {
        this.commonCredentials = commonCredentials;
        prioritiseCredentials();
    }

    protected void updateVaultConfig() throws SkyflowException {
        updateVaultURL();
        prioritiseCredentials();
    }

    protected void setBearerToken() throws SkyflowException {
        prioritiseCredentials();
        Validations.validateCredentials(this.finalCredentials);
        if (this.finalCredentials.getApiKey() != null) {
            LogUtil.printInfoLog(InfoLogs.REUSE_API_KEY.getLog());
            token = this.finalCredentials.getApiKey();
        } else if (Token.isExpired(token)) {
            LogUtil.printInfoLog(InfoLogs.BEARER_TOKEN_EXPIRED.getLog());
            token = Utils.generateBearerToken(this.finalCredentials);
        } else {
            LogUtil.printInfoLog(InfoLogs.REUSE_BEARER_TOKEN.getLog());
        }
        this.apiClient = this.apiClientBuilder.build();
    }

    private void updateVaultURL() {
        String vaultURL = Utils.getVaultURL(this.vaultConfig.getClusterId(), this.vaultConfig.getEnv());
        this.apiClientBuilder.url(vaultURL);
    }

    private void prioritiseCredentials() throws SkyflowException {
        try {
            Credentials original = this.finalCredentials;
            if (this.vaultConfig.getCredentials() != null) {
                this.finalCredentials = this.vaultConfig.getCredentials();
            } else if (this.commonCredentials != null) {
                this.finalCredentials = this.commonCredentials;
            } else {
                Dotenv dotenv = Dotenv.load();
                String sysCredentials = dotenv.get(Constants.ENV_CREDENTIALS_KEY_NAME);
                if (sysCredentials == null) {
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                            ErrorMessage.EmptyCredentials.getMessage());
                } else {
                    this.finalCredentials = new Credentials();
                    this.finalCredentials.setCredentialsString(sysCredentials);
                }
            }
            if (original != null && !original.equals(this.finalCredentials)) {
                token = null;
                apiKey = null;
            }
        } catch (DotenvException e) {
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                    ErrorMessage.EmptyCredentials.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
