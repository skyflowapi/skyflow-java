package com.skyflow.vaultLH;

import com.skyflow.common.config.Credentials;
import com.skyflow.common.config.VaultConfig;
import com.skyflow.common.logger.LogUtil;
import com.skyflow.common.serviceaccount.util.Token;
import com.skyflow.common.errors.ErrorCode;
import com.skyflow.common.errors.ErrorMessage;
import com.skyflow.common.errors.SkyflowException;
import com.skyflow.vaultLH.generated.ApiClient;
import com.skyflow.vaultLH.generated.ApiClientBuilder;
import com.skyflow.common.logs.InfoLogs;
import com.skyflow.common.utils.Constants;
import com.skyflow.common.utils.CommonUtils;
import com.skyflow.common.utils.Validations;
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

//    protected RecordsClient getRecordsApi() {
//        return this.apiClient.records();
//    }
//
//    protected TokensClient getTokensApi() {
//        return this.apiClient.tokens();
//    }
//
//    protected QueryClient getQueryApi() {
//        return this.apiClient.query();
//    }

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
            token=this.finalCredentials.getApiKey();
        } else if (Token.isExpired(token)) {
            LogUtil.printInfoLog(InfoLogs.BEARER_TOKEN_EXPIRED.getLog());
            token = CommonUtils.generateBearerToken(this.finalCredentials);
        } else {
            LogUtil.printInfoLog(InfoLogs.REUSE_BEARER_TOKEN.getLog());
        }
//        this.apiClientBuilder.token(token);
        this.apiClient = this.apiClientBuilder.build();
    }

       private void updateVaultURL() {
        String vaultURL = CommonUtils.getVaultURL(this.vaultConfig.getClusterId(), this.vaultConfig.getEnv());
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
