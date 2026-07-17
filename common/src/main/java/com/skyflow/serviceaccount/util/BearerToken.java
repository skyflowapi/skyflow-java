package com.skyflow.serviceaccount.util;

import com.google.gson.*;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.auth.rest.ApiClient;
import com.skyflow.generated.auth.rest.ApiClientBuilder;
import com.skyflow.generated.auth.rest.core.ApiClientApiException;
import com.skyflow.generated.auth.rest.resources.authentication.AuthenticationClient;
import com.skyflow.generated.auth.rest.resources.authentication.requests.V1GetAuthTokenRequest;
import com.skyflow.generated.auth.rest.types.V1GetAuthTokenResponse;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.BaseConstants;
import com.skyflow.utils.BaseUtils;
import com.skyflow.utils.logger.LogUtil;
import io.jsonwebtoken.Jwts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

public class BearerToken {
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final ApiClientBuilder API_CLIENT_BUILDER = new ApiClientBuilder();
    private final File credentialsFile;
    private final String credentialsString;
    private final Object ctx;
    private final ArrayList<String> roles;
    private final String credentialsType;

    private BearerToken(BearerTokenBuilder builder) {
        this.credentialsFile = builder.credentialsFile;
        this.credentialsString = builder.credentialsString;
        this.ctx = builder.ctx;
        this.roles = builder.roles;
        this.credentialsType = builder.credentialsType;
    }

    public static BearerTokenBuilder builder() {
        return new BearerTokenBuilder();
    }

    private static V1GetAuthTokenResponse generateBearerTokenFromCredentials(
            File credentialsFile, Object context, ArrayList<String> roles
    ) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.GENERATE_BEARER_TOKEN_FROM_CREDENTIALS_TRIGGERED.getLog());
        try {
            if (credentialsFile == null) {
                LogUtil.printErrorLog(ErrorLogs.INVALID_CREDENTIALS_FILE.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidCredentials.getMessage());
            }
            FileReader reader = new FileReader(String.valueOf(credentialsFile));
            try {
                JsonObject serviceAccountCredentials = JsonParser.parseReader(reader).getAsJsonObject();
                return getBearerTokenFromCredentials(serviceAccountCredentials, context, roles);
            } finally {
                try { reader.close(); } catch (IOException ignored) {}
            }
        } catch (JsonSyntaxException | IllegalStateException e) {
            LogUtil.printErrorLog(ErrorLogs.INVALID_CREDENTIALS_FILE_FORMAT.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), BaseUtils.parameterizedString(
                    ErrorMessage.FileInvalidJson.getMessage(), credentialsFile.getPath()));
        } catch (FileNotFoundException e) {
            LogUtil.printErrorLog(ErrorLogs.CREDENTIALS_FILE_NOT_FOUND.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), BaseUtils.parameterizedString(
                    ErrorMessage.FileNotFound.getMessage(), credentialsFile.getPath()));
        }
    }

    private static V1GetAuthTokenResponse generateBearerTokenFromCredentialString(
            String credentials, Object context, ArrayList<String> roles
    ) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.GENERATE_BEARER_TOKEN_FROM_CREDENTIALS_STRING_TRIGGERED.getLog());
        try {
            if (credentials == null || credentials.isEmpty()) {
                LogUtil.printErrorLog(ErrorLogs.INVALID_CREDENTIALS_STRING.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidCredentials.getMessage());
            }
            JsonObject serviceAccountCredentials = JsonParser.parseString(credentials).getAsJsonObject();
            return getBearerTokenFromCredentials(serviceAccountCredentials, context, roles);
        } catch (JsonSyntaxException | IllegalStateException e) {
            LogUtil.printErrorLog(ErrorLogs.INVALID_CREDENTIALS_STRING_FORMAT.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                    ErrorMessage.CredentialsStringInvalidJson.getMessage());
        }
    }

    private static V1GetAuthTokenResponse getBearerTokenFromCredentials(
            JsonObject credentials, Object context, ArrayList<String> roles
    ) throws SkyflowException {
        try {
            JsonElement privateKey = credentials.get("privateKey");
            if (privateKey == null) {
                LogUtil.printErrorLog(ErrorLogs.PRIVATE_KEY_IS_REQUIRED.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MissingPrivateKey.getMessage());
            }

            // Accept both new-form keys (clientId/keyId/tokenUri) and legacy all-caps form for migration
            JsonElement clientId = credentials.get("clientId");
            if (clientId == null) {
                clientId = credentials.get("clientID");
                if (clientId != null) {
                    LogUtil.printWarningLog(InfoLogs.DEPRECATED_CREDENTIAL_CLIENT_ID.getLog());
                }
            }
            if (clientId == null) {
                LogUtil.printErrorLog(ErrorLogs.CLIENT_ID_IS_REQUIRED.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MissingClientId.getMessage());
            }

            JsonElement keyId = credentials.get("keyId");
            if (keyId == null) {
                keyId = credentials.get("keyID");
                if (keyId != null) {
                    LogUtil.printWarningLog(InfoLogs.DEPRECATED_CREDENTIAL_KEY_ID.getLog());
                }
            }
            if (keyId == null) {
                LogUtil.printErrorLog(ErrorLogs.KEY_ID_IS_REQUIRED.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MissingKeyId.getMessage());
            }

            JsonElement tokenUri = credentials.get("tokenUri");
            if (tokenUri == null) {
                tokenUri = credentials.get("tokenURI");
                if (tokenUri != null) {
                    LogUtil.printWarningLog(InfoLogs.DEPRECATED_CREDENTIAL_TOKEN_URI.getLog());
                }
            }
            if (tokenUri == null) {
                LogUtil.printErrorLog(ErrorLogs.TOKEN_URI_IS_REQUIRED.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MissingTokenUri.getMessage());
            }

            PrivateKey pvtKey = BaseUtils.getPrivateKeyFromPem(privateKey.getAsString());
            String signedUserJWT = getSignedToken(
                    clientId.getAsString(), keyId.getAsString(), tokenUri.getAsString(), pvtKey, context
            );

            String basePath = BaseUtils.getBaseURL(tokenUri.getAsString());
            API_CLIENT_BUILDER.url(basePath);
            ApiClient apiClient = API_CLIENT_BUILDER.token("token").build();
            AuthenticationClient authenticationApi = apiClient.authentication();

            V1GetAuthTokenRequest._FinalStage authTokenBuilder = V1GetAuthTokenRequest.builder().grantType(BaseConstants.GRANT_TYPE).assertion(signedUserJWT);

            if (roles != null) {
                String scopedRoles = getScopeUsingRoles(roles);
                authTokenBuilder.scope(scopedRoles);
            }
            return authenticationApi.authenticationServiceGetAuthToken(authTokenBuilder.build());
        } catch (MalformedURLException e) {
            LogUtil.printErrorLog(ErrorLogs.INVALID_TOKEN_URI.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidTokenUri.getMessage());
        } catch (ApiClientApiException e) {
            String bodyString = GSON.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.BEARER_TOKEN_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        }
    }

    private static String getSignedToken(
            String clientId, String keyId, String tokenUri, PrivateKey pvtKey, Object context
    ) {
        final Date createdDate = new Date();
        final Date expirationDate = new Date(createdDate.getTime() + (3600 * 1000));
        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .claim("iss", clientId)
                .claim("key", keyId)
                .claim("aud", tokenUri)
                .claim("sub", clientId)
                .expiration(expirationDate);

        if (context != null) {
            builder.claim("ctx", context);
        }

        return builder.signWith(pvtKey, Jwts.SIG.RS256).compact();
    }

    private static String getScopeUsingRoles(ArrayList<String> roles) {
        StringBuilder scope = new StringBuilder();
        if (roles != null) {
            for (String role : roles) {
                scope.append(" role:").append(role);
            }
        }
        return scope.toString();
    }

    public synchronized String getBearerToken() throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.GET_BEARER_TOKEN_TRIGGERED.getLog());
        V1GetAuthTokenResponse response;
        String accessToken = null;
        if (this.credentialsFile != null && Objects.equals(this.credentialsType, "FILE")) {
            response = generateBearerTokenFromCredentials(this.credentialsFile, this.ctx, this.roles);
            accessToken = response.getAccessToken().get();
        } else if (this.credentialsString != null && Objects.equals(this.credentialsType, "STRING")) {
            response = generateBearerTokenFromCredentialString(this.credentialsString, this.ctx, this.roles);
            accessToken = response.getAccessToken().get();
        }
        LogUtil.printInfoLog(InfoLogs.GET_BEARER_TOKEN_SUCCESS.getLog());
        return accessToken;
    }

    // Builder class
    public static class BearerTokenBuilder {
        private File credentialsFile;
        private String credentialsString;
        private Object ctx;
        private ArrayList<String> roles;
        private String credentialsType;

        private BearerTokenBuilder() {
        }

        private void setCredentialsType(String credentialsType) {
            this.credentialsType = credentialsType;
        }

        public BearerTokenBuilder setCredentials(File credentialsFile) {
            setCredentialsType("FILE");
            this.credentialsFile = credentialsFile;
            return this;
        }

        public BearerTokenBuilder setCredentials(String credentialsString) {
            setCredentialsType("STRING");
            this.credentialsString = credentialsString;
            return this;
        }

        public BearerTokenBuilder setCtx(String ctx) {
            this.ctx = ctx;
            return this;
        }

        public BearerTokenBuilder setCtx(Map<String, Object> ctx) {
            this.ctx = ctx;
            return this;
        }

        public BearerTokenBuilder setRoles(ArrayList<String> roles) {
            this.roles = roles;
            return this;
        }

        public BearerToken build() {
            return new BearerToken(this);
        }
    }
}
