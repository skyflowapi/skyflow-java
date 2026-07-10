package com.skyflow;

import com.skyflow.config.BaseCredentials;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.InfoLogs;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.utils.BaseConstants;
import com.skyflow.utils.BaseUtils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.BaseValidations;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class BaseVaultClient {
    protected OkHttpClient sharedHttpClient;
    protected String currentVaultURL;
    protected BaseCredentials commonCredentials;
    protected BaseCredentials finalCredentials;
    protected String token;
    protected String apiKey;

    public BaseVaultClient() {
    }

    protected OkHttpClient buildSharedHttpClient(Supplier<String> tokenSupplier) {
        return new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(10, 1, TimeUnit.MINUTES))
                .addInterceptor(chain -> {
                    Request requestWithAuth = chain.request().newBuilder()
                            .header("Authorization", "Bearer " + tokenSupplier.get())
                            .build();
                    return chain.proceed(requestWithAuth);
                })
                .build();
    }

    protected void prioritiseCredentials(BaseCredentials vaultSpecificCredentials) throws SkyflowException {
        try {
            BaseCredentials original = this.finalCredentials;
            if (vaultSpecificCredentials != null) {
                this.finalCredentials = vaultSpecificCredentials;
            } else if (this.commonCredentials != null) {
                this.finalCredentials = this.commonCredentials;
            } else {
                String sysCredentials = System.getenv(BaseConstants.ENV_CREDENTIALS_KEY_NAME);
                if (sysCredentials == null) {
                    Dotenv dotenv = Dotenv.load();
                    sysCredentials = dotenv.get(BaseConstants.ENV_CREDENTIALS_KEY_NAME);
                }
                if (sysCredentials == null) {
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyCredentials.getMessage());
                } else {
                    this.finalCredentials = new BaseCredentials();
                    this.finalCredentials.setCredentialsString(sysCredentials);
                }
            }
            if (original != null && !original.equals(this.finalCredentials)) {
                token = null;
                apiKey = null;
            }
        } catch (DotenvException e) {
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyCredentials.getMessage());
        } catch (SkyflowException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void setBearerToken(BaseCredentials vaultSpecificCredentials) throws SkyflowException {
        prioritiseCredentials(vaultSpecificCredentials);
        BaseValidations.validateCredentials(this.finalCredentials);
        if (this.finalCredentials.getApiKey() != null) {
            LogUtil.printInfoLog(InfoLogs.USE_API_KEY.getLog());
            token = this.finalCredentials.getApiKey();
        } else if (token == null || token.trim().isEmpty()) {
            token = BaseUtils.generateBearerToken(this.finalCredentials);
        } else if (Token.isExpired(token)) {
            LogUtil.printInfoLog(InfoLogs.BEARER_TOKEN_EXPIRED.getLog());
            token = BaseUtils.generateBearerToken(this.finalCredentials);
        } else {
            LogUtil.printInfoLog(InfoLogs.REUSE_BEARER_TOKEN.getLog());
        }
    }
}
