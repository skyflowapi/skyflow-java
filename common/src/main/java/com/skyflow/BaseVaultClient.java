package com.skyflow;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skyflow.config.BaseCredentials;
import com.skyflow.config.BaseVaultConfig;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.logs.WarningLogs;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.utils.BaseConstants;
import com.skyflow.utils.BaseUtils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.BaseValidations;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class BaseVaultClient<V extends BaseVaultConfig> {
    protected V vaultConfig;
    protected OkHttpClient sharedHttpClient;
    protected String currentVaultURL;
    protected BaseCredentials commonCredentials;
    protected BaseCredentials finalCredentials;
    protected String token;
    protected String apiKey;

    /**
     * A 401 is treated as token expiry only if the token the SDK sent has an {@code exp} at or
     * before now plus this window. By the time the 401 arrives the gateway has already judged the
     * token expired, so the window only has to absorb clock skew between this host and the gateway.
     */
    protected static final long BEARER_TOKEN_EXPIRY_WINDOW_SECONDS = 60;

    protected BaseVaultClient(V vaultConfig, BaseCredentials credentials) {
        this.vaultConfig = vaultConfig;
        this.commonCredentials = credentials;
    }

    protected V getVaultConfig() {
        return vaultConfig;
    }

    /**
     * The bearer token to send. Synchronized on the same monitor as {@link #setBearerToken(BaseCredentials)}
     * and {@link #refreshBearerTokenAfterUnauthorized(String)}, so a request attempt on an OkHttp or
     * batch thread sees the token those methods wrote, and waits for a regeneration in progress
     * instead of sending the token it is replacing.
     */
    protected synchronized String getToken() {
        return token;
    }

    protected OkHttpClient buildSharedHttpClient(Supplier<String> tokenSupplier) {
        return new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(10, 1, TimeUnit.MINUTES))
                .addInterceptor(authInterceptor(tokenSupplier))
                .build();
    }

    /**
     * The auth interceptor shared by every OkHttp client in the SDK. It attaches the current bearer
     * token to each attempt, reading it fresh from {@code tokenSupplier} so a token refreshed between
     * attempts is picked up. When the vault answers {@code 401} and
     * {@link #refreshBearerTokenAfterUnauthorized(String)} can supply a replacement, the rejected
     * response is closed and the same request is sent once more with the new token. Any other
     * status, and a {@code 401} the refresh declines, is returned as is.
     */
    protected Interceptor authInterceptor(Supplier<String> tokenSupplier) {
        return chain -> {
            String sent = tokenSupplier.get();
            Response response = chain.proceed(withBearer(chain.request(), sent));
            if (response.code() != HttpURLConnection.HTTP_UNAUTHORIZED) {
                return response;
            }
            String fresh = refreshBearerTokenAfterUnauthorized(sent);
            if (fresh == null) {
                return response;
            }
            response.close();
            return chain.proceed(withBearer(chain.request(), fresh));
        };
    }

    private static Request withBearer(Request request, String bearer) {
        return request.newBuilder().header("Authorization", "Bearer " + bearer).build();
    }

    /**
     * Called when the vault rejected {@code rejected} with {@code 401}. Returns the token to resend
     * the request with, or {@code null} when the request must fail as it stands:
     * <ul>
     *   <li>the effective credentials are a static token or an API key, which the SDK cannot replace;</li>
     *   <li>the rejected token is not at or past expiry (within {@link #BEARER_TOKEN_EXPIRY_WINDOW_SECONDS}),
     *       so the {@code 401} was not caused by expiry and a new token would not help;</li>
     *   <li>generating a replacement failed, or produced the very token that was rejected.</li>
     * </ul>
     * Synchronized with {@link #setBearerToken(BaseCredentials)} and {@link #getToken()} so concurrent
     * {@code 401}s on one token share a single regeneration: a caller that finds the cached token
     * already different from the one it sent reuses it without generating again.
     */
    protected synchronized String refreshBearerTokenAfterUnauthorized(String rejected) {
        BaseCredentials credentials = this.finalCredentials;
        if (credentials == null || credentials.getApiKey() != null || credentials.getToken() != null) {
            return null;
        }
        if (!Token.isExpiringWithin(rejected, BEARER_TOKEN_EXPIRY_WINDOW_SECONDS)) {
            LogUtil.printInfoLog(InfoLogs.BEARER_TOKEN_REJECTED_NOT_DUE.getLog());
            return null;
        }
        String cached = this.token;
        if (cached != null && !cached.equals(rejected)) {
            LogUtil.printInfoLog(InfoLogs.BEARER_TOKEN_REJECTED_REUSING_REGENERATED.getLog());
            return cached;
        }
        LogUtil.printInfoLog(InfoLogs.BEARER_TOKEN_REJECTED_REGENERATING.getLog());
        try {
            String fresh = generateBearerToken(credentials);
            if (fresh == null || fresh.equals(rejected)) {
                LogUtil.printWarningLog(WarningLogs.BEARER_TOKEN_REGENERATION_RETURNED_REJECTED_TOKEN.getLog());
                return null;
            }
            this.token = fresh;
            return fresh;
        } catch (SkyflowException e) {
            LogUtil.printWarningLog(WarningLogs.BEARER_TOKEN_REGENERATION_AFTER_401_FAILED.getLog() + " " + e.getMessage());
            return null;
        }
    }

    /** Single point that turns credentials into a bearer token; tests substitute it. */
    protected String generateBearerToken(BaseCredentials credentials) throws SkyflowException {
        return BaseUtils.generateBearerToken(credentials);
    }

    protected synchronized void prioritiseCredentials(BaseCredentials vaultSpecificCredentials) throws SkyflowException {
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

    protected synchronized void setBearerToken(BaseCredentials vaultSpecificCredentials) throws SkyflowException {
        prioritiseCredentials(vaultSpecificCredentials);
        BaseValidations.validateCredentials(this.finalCredentials);
        if (this.finalCredentials.getApiKey() != null) {
            LogUtil.printInfoLog(InfoLogs.USE_API_KEY.getLog());
            token = this.finalCredentials.getApiKey();
        } else if (token == null || token.trim().isEmpty()) {
            token = generateBearerToken(this.finalCredentials);
        } else if (Token.isExpired(token)) {
            LogUtil.printInfoLog(InfoLogs.BEARER_TOKEN_EXPIRED.getLog());
            token = generateBearerToken(this.finalCredentials);
        } else {
            LogUtil.printInfoLog(InfoLogs.REUSE_BEARER_TOKEN.getLog());
        }
    }

    protected static SkyflowException wrapApiException(int statusCode, Throwable cause,
                                                         Map<String, List<String>> headers,
                                                         Object responseBody, ErrorLogs errorLog) {
        LogUtil.printErrorLog(errorLog.getLog());
        Gson gson = new GsonBuilder().serializeNulls().create();
        return new SkyflowException(statusCode, cause, headers, gson.toJson(responseBody));
    }
}
