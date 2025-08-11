package com.skyflow.common.serviceaccount.util;

import com.google.gson.*;
import com.skyflow.common.ApiClient;
import com.skyflow.common.ApiClientBuilder;
import com.skyflow.common.core.ApiClientApiException;
import com.skyflow.common.resources.authentication.AuthenticationClient;
import com.skyflow.common.resources.authentication.requests.V1GetAuthTokenRequest;
import com.skyflow.common.types.V1GetAuthTokenResponse;
import com.skyflow.common.errors.ErrorCode;
import com.skyflow.common.errors.ErrorMessage;
import com.skyflow.common.errors.SkyflowException;
import com.skyflow.common.logs.ErrorLogs;
import com.skyflow.common.logs.InfoLogs;
import com.skyflow.common.utils.Constants;
import com.skyflow.common.utils.Utils;
import com.skyflow.common.utils.logger.LogUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

public class BearerToken {
    private static final Gson gson = new GsonBuilder().serializeNulls().create();
    private static final ApiClientBuilder apiClientBuilder = new ApiClientBuilder();
    private final File credentialsFile;
    private final String credentialsString;
    private final String ctx;
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
            File credentialsFile, String context, ArrayList<String> roles
    ) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.GENERATE_BEARER_TOKEN_FROM_CREDENTIALS_TRIGGERED.getLog());
        try {
            if (credentialsFile == null) {
                LogUtil.printErrorLog(ErrorLogs.INVALID_CREDENTIALS_FILE.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidCredentials.getMessage());
            }
            FileReader reader = new FileReader(String.valueOf(credentialsFile));
            JsonObject serviceAccountCredentials = JsonParser.parseReader(reader).getAsJsonObject();
            return getBearerTokenFromCredentials(serviceAccountCredentials, context, roles);
        } catch (JsonSyntaxException e) {
            LogUtil.printErrorLog(ErrorLogs.INVALID_CREDENTIALS_FILE_FORMAT.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), Utils.parameterizedString(
                    ErrorMessage.FileInvalidJson.getMessage(), credentialsFile.getPath()));
        } catch (FileNotFoundException e) {
            LogUtil.printErrorLog(ErrorLogs.CREDENTIALS_FILE_NOT_FOUND.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), Utils.parameterizedString(
                    ErrorMessage.FileNotFound.getMessage(), credentialsFile.getPath()));
        }
    }

    private static V1GetAuthTokenResponse generateBearerTokenFromCredentialString(
            String credentials, String context, ArrayList<String> roles
    ) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.GENERATE_BEARER_TOKEN_FROM_CREDENTIALS_STRING_TRIGGERED.getLog());
        try {
            if (credentials == null || credentials.isEmpty()) {
                LogUtil.printErrorLog(ErrorLogs.INVALID_CREDENTIALS_STRING.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidCredentials.getMessage());
            }
            JsonObject serviceAccountCredentials = JsonParser.parseString(credentials).getAsJsonObject();
            return getBearerTokenFromCredentials(serviceAccountCredentials, context, roles);
        } catch (JsonSyntaxException e) {
            LogUtil.printErrorLog(ErrorLogs.INVALID_CREDENTIALS_STRING_FORMAT.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                    ErrorMessage.CredentialsStringInvalidJson.getMessage());
        }
    }

    private static V1GetAuthTokenResponse getBearerTokenFromCredentials(
            JsonObject credentials, String context, ArrayList<String> roles
    ) throws SkyflowException {
        try {
            JsonElement privateKey = credentials.get("privateKey");
            if (privateKey == null) {
                LogUtil.printErrorLog(ErrorLogs.PRIVATE_KEY_IS_REQUIRED.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MissingPrivateKey.getMessage());
            }

            JsonElement clientID = credentials.get("clientID");
            if (clientID == null) {
                LogUtil.printErrorLog(ErrorLogs.CLIENT_ID_IS_REQUIRED.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MissingClientId.getMessage());
            }

            JsonElement keyID = credentials.get("keyID");
            if (keyID == null) {
                LogUtil.printErrorLog(ErrorLogs.KEY_ID_IS_REQUIRED.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MissingKeyId.getMessage());
            }

            JsonElement tokenURI = credentials.get("tokenURI");
            if (tokenURI == null) {
                LogUtil.printErrorLog(ErrorLogs.TOKEN_URI_IS_REQUIRED.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MissingTokenUri.getMessage());
            }

            PrivateKey pvtKey = Utils.getPrivateKeyFromPem(privateKey.getAsString());
            String signedUserJWT = getSignedToken(
                    clientID.getAsString(), keyID.getAsString(), tokenURI.getAsString(), pvtKey, context
            );

            String basePath = Utils.getBaseURL(tokenURI.getAsString());
            apiClientBuilder.url(basePath);
            ApiClient apiClient = apiClientBuilder.token("token").build();
            AuthenticationClient authenticationApi = apiClient.authentication();

            V1GetAuthTokenRequest._FinalStage authTokenBuilder = V1GetAuthTokenRequest.builder().grantType(Constants.GRANT_TYPE).assertion(signedUserJWT);

            if (roles != null) {
                String scopedRoles = getScopeUsingRoles(roles);
                authTokenBuilder.scope(scopedRoles);
            }
            return authenticationApi.authenticationServiceGetAuthToken(authTokenBuilder.build());
        } catch (MalformedURLException e) {
            LogUtil.printErrorLog(ErrorLogs.INVALID_TOKEN_URI.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidTokenUri.getMessage());
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.BEARER_TOKEN_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        }
    }

    private static String getSignedToken(
            String clientID, String keyID, String tokenURI, PrivateKey pvtKey, String context
    ) {
        final Date createdDate = new Date();
        final Date expirationDate = new Date(createdDate.getTime() + (3600 * 1000));
        return Jwts.builder()
                .claim("iss", clientID)
                .claim("key", keyID)
                .claim("aud", tokenURI)
                .claim("sub", clientID)
                .claim("ctx", context)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.RS256, pvtKey)
                .compact();
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
        private String ctx;
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

        public BearerTokenBuilder setRoles(ArrayList<String> roles) {
            this.roles = roles;
            return this;
        }

        public BearerToken build() {
            return new BearerToken(this);
        }
    }
}
