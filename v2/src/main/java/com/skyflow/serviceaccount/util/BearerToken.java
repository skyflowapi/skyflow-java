package com.skyflow.serviceaccount.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.ApiException;
import com.skyflow.generated.rest.api.AuthenticationApi;
import com.skyflow.generated.rest.models.V1GetAuthTokenRequest;
import com.skyflow.generated.rest.models.V1GetAuthTokenResponse;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
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
    private static final ApiClient apiClient = new ApiClient();
    private static final AuthenticationApi authenticationApi = new AuthenticationApi(apiClient);
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
        try {
            if (credentialsFile == null || !credentialsFile.isFile()) {
                throw new SkyflowException();
            }
            FileReader reader = new FileReader(String.valueOf(credentialsFile));
            JsonObject serviceAccountCredentials = JsonParser.parseReader(reader).getAsJsonObject();
            return getBearerTokenFromCredentials(serviceAccountCredentials, context, roles);
        } catch (JsonSyntaxException e) {
            throw new SkyflowException();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static V1GetAuthTokenResponse generateBearerTokenFromCredentialString(
            String credentials, String context, ArrayList<String> roles
    ) throws SkyflowException {
        try {
            if (credentials == null || credentials.isEmpty()) {
                throw new SkyflowException();
            }
            JsonObject serviceAccountCredentials = JsonParser.parseString(credentials).getAsJsonObject();
            return getBearerTokenFromCredentials(serviceAccountCredentials, context, roles);
        } catch (JsonSyntaxException e) {
            throw new SkyflowException();
        }
    }

    private static V1GetAuthTokenResponse getBearerTokenFromCredentials(
            JsonObject credentials, String context, ArrayList<String> roles
    ) throws SkyflowException {
        try {
            JsonElement privateKey = credentials.get("privateKey");
            if (privateKey == null) {
                throw new SkyflowException();
            }

            JsonElement clientID = credentials.get("clientID");
            if (clientID == null) {
                throw new SkyflowException();
            }

            JsonElement keyID = credentials.get("keyID");
            if (keyID == null) {
                throw new SkyflowException();
            }

            JsonElement tokenURI = credentials.get("tokenURI");
            if (tokenURI == null) {
                throw new SkyflowException();
            }

            PrivateKey pvtKey = Utils.getPrivateKeyFromPem(privateKey.getAsString());
            String signedUserJWT = getSignedToken(
                    clientID.getAsString(), keyID.getAsString(), tokenURI.getAsString(), pvtKey, context
            );

            String basePath = Utils.getBaseURL(tokenURI.getAsString());
            apiClient.setBasePath(basePath);

            V1GetAuthTokenRequest body = new V1GetAuthTokenRequest();
            body.setGrantType(Constants.GRANT_TYPE);
            body.setAssertion(signedUserJWT);

            if (roles != null) {
                String scopedRoles = getScopeUsingRoles(roles);
                body.setScope(scopedRoles);
            }
            return authenticationApi.authenticationServiceGetAuthToken(body);
        } catch (MalformedURLException e) {
            throw new SkyflowException();
        } catch (ApiException e) {
            throw new RuntimeException(e);
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
        V1GetAuthTokenResponse response;
        String accessToken = null;
        try {
            if (this.credentialsFile != null && Objects.equals(this.credentialsType, "FILE")) {
                response = generateBearerTokenFromCredentials(this.credentialsFile, this.ctx, this.roles);
                accessToken = response.getAccessToken();
            } else if (this.credentialsString != null && Objects.equals(this.credentialsType, "STRING")) {
                response = generateBearerTokenFromCredentialString(this.credentialsString, this.ctx, this.roles);
                accessToken = response.getAccessToken();
            }
        } catch (SkyflowException e) {
            e.printStackTrace();
        }
        return accessToken;
    }

    // Builder class
    public static class BearerTokenBuilder {
        private File credentialsFile;
        private String credentialsString;
        private String ctx;
        private ArrayList<String> roles;
        private String credentialsType;

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
