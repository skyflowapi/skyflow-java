package com.skyflow.v2;

import com.skyflow.common.serviceaccount.util.Token;
import com.skyflow.v2.config.ConnectionConfig;
import com.skyflow.v2.config.Credentials;
import com.skyflow.v2.errors.ErrorCode;
import com.skyflow.v2.errors.ErrorMessage;
import com.skyflow.v2.errors.SkyflowException;
import com.skyflow.v2.logs.InfoLogs;
import com.skyflow.v2.utils.Constants;
import com.skyflow.v2.utils.Utils;
import com.skyflow.v2.utils.logger.LogUtil;
import com.skyflow.v2.utils.validations.Validations;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

public class ConnectionClient {
    private final ConnectionConfig connectionConfig;
    protected String token;
    protected String apiKey;
    private Credentials commonCredentials;
    private Credentials finalCredentials;

    protected ConnectionClient(ConnectionConfig connectionConfig, Credentials credentials) {
        super();
        this.connectionConfig = connectionConfig;
        this.commonCredentials = credentials;
    }

    protected ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    protected void setCommonCredentials(Credentials commonCredentials) throws SkyflowException {
        this.commonCredentials = commonCredentials;
        prioritiseCredentials();
    }

    protected void updateConnectionConfig(ConnectionConfig connectionConfig) throws SkyflowException {
        prioritiseCredentials();
    }

    protected void setBearerToken() throws SkyflowException {
        prioritiseCredentials();
        Validations.validateCredentials(this.finalCredentials);
        if (this.finalCredentials.getApiKey() != null) {
            setApiKey();
        } else if (Token.isExpired(token)) {
            LogUtil.printInfoLog(InfoLogs.BEARER_TOKEN_EXPIRED.getLog());
            token = Utils.generateBearerToken(this.finalCredentials);
        } else {
            LogUtil.printInfoLog(InfoLogs.REUSE_BEARER_TOKEN.getLog());
        }
    }

    private void setApiKey() {
        if (apiKey == null) {
            apiKey = this.finalCredentials.getApiKey();
        } else {
            LogUtil.printInfoLog(InfoLogs.REUSE_API_KEY.getLog());
        }
    }

    private void prioritiseCredentials() throws SkyflowException {
        try {
            Credentials original = this.finalCredentials;
            if (this.connectionConfig.getCredentials() != null) {
                this.finalCredentials = this.connectionConfig.getCredentials();
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
