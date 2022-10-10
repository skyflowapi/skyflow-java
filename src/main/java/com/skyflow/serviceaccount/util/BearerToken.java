/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.serviceaccount.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyflow.common.utils.Helpers;
import com.skyflow.common.utils.HttpUtility;
import com.skyflow.common.utils.LogUtil;
import com.skyflow.entities.ResponseToken;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.security.PrivateKey;
import java.util.Date;
import java.util.Objects;

public class BearerToken {
    private final File credentialsFile;
    private final String credentialsString;
    private final String context;

    private final String[] roles;

    private final String credentialsType;

    private BearerToken(BearerTokenBuilder builder) {
        this.credentialsFile = builder.credentialsFile;
        this.credentialsString = builder.credentialsString;
        this.context = builder.context;
        this.roles = builder.roles;
        this.credentialsType = builder.credentialsType;
    }

    // Builder class
    public static class BearerTokenBuilder {
        private File credentialsFile;
        private String credentialsString;
        private String context;
        private String[] roles;

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

        public BearerTokenBuilder setContext(String context) {
            this.context = context;
            return this;
        }

        public BearerTokenBuilder setRoles(String[] roles) {
            this.roles = roles;
            return this;
        }

        public BearerToken build() {
            return new BearerToken(this);
        }
    }

    public String getBearerToken() throws SkyflowException {
        // Make API call in generateBearerToken function to get the token
        ResponseToken response;
        String accessToken = null;

        try {
            if (this.credentialsFile != null && Objects.equals(this.credentialsType, "FILE")) {
                response = generateBearerTokenFromCredentials(this.credentialsFile, this.context, this.roles);
                accessToken = response.getAccessToken();

            } else if (this.credentialsString != null && Objects.equals(this.credentialsType, "STRING")) {

                response = generateBearerTokenFromCredentialString(this.credentialsString, this.context, this.roles);
                accessToken = response.getAccessToken();
            }
        } catch (SkyflowException e) {
            e.printStackTrace();
        }
        return accessToken;
    }

    private static ResponseToken generateBearerTokenFromCredentials(File credentialsPath, String context,
            String[] roles) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.GenerateBearerTokenFromCredsCalled.getLog());
        JSONParser parser = new JSONParser();
        ResponseToken responseToken;
        try {
            if (credentialsPath == null || !credentialsPath.isFile()) {
                LogUtil.printErrorLog(ErrorLogs.EmptyJSONString.getLog());
                throw new SkyflowException(ErrorCode.EmptyJSONString);
            }

            Object obj = parser.parse(new FileReader(String.valueOf(credentialsPath)));
            JSONObject saCreds = (JSONObject) obj;

            responseToken = getBearerTokenFromCreds(saCreds, context, roles);

        } catch (ParseException e) {
            LogUtil.printErrorLog(ErrorLogs.InvalidJSONStringFormat.getLog());
            throw new SkyflowException(ErrorCode.InvalidJSONStringFormat, e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return responseToken;
    }

    private static ResponseToken generateBearerTokenFromCredentialString(String credentials, String context,
            String[] roles) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.GenerateBearerTokenFromCredsCalled.getLog());
        JSONParser parser = new JSONParser();
        ResponseToken responseToken;
        try {
            if (credentials == null || credentials.isEmpty()) {
                LogUtil.printErrorLog(ErrorLogs.EmptyJSONString.getLog());
                throw new SkyflowException(ErrorCode.EmptyJSONString);
            }

            Object obj = parser.parse(credentials);
            JSONObject saCreds = (JSONObject) obj;

            responseToken = getBearerTokenFromCreds(saCreds, context, roles);

        } catch (ParseException e) {
            LogUtil.printErrorLog(ErrorLogs.InvalidJSONStringFormat.getLog());
            throw new SkyflowException(ErrorCode.InvalidJSONStringFormat, e);
        }
        return responseToken;
    }

    private static ResponseToken getBearerTokenFromCreds(JSONObject creds, String context, String[] roles)
            throws SkyflowException {
        ResponseToken responseToken;
        try {
            String clientID = (String) creds.get("clientID");
            if (clientID == null) {
                LogUtil.printErrorLog(ErrorLogs.InvalidClientID.getLog());
                throw new SkyflowException(ErrorCode.InvalidClientID);
            }
            String keyID = (String) creds.get("keyID");
            if (keyID == null) {
                LogUtil.printErrorLog(ErrorLogs.InvalidKeyID.getLog());
                throw new SkyflowException(ErrorCode.InvalidKeyID);
            }
            String tokenURI = (String) creds.get("tokenURI");
            if (tokenURI == null) {
                LogUtil.printErrorLog(ErrorLogs.InvalidTokenURI.getLog());
                throw new SkyflowException(ErrorCode.InvalidTokenURI);
            }

            PrivateKey pvtKey = Helpers.getPrivateKeyFromPem((String) creds.get("privateKey"));

            String signedUserJWT = getSignedToken(clientID, keyID, tokenURI, pvtKey, context);

            JSONObject parameters = new JSONObject();
            parameters.put("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
            parameters.put("assertion", signedUserJWT);
            if (roles != null) {
                String scopedRoles = getScopeUsingRoles(roles);
                parameters.put("scope", scopedRoles);
            }

            String response = HttpUtility.sendRequest("POST", new URL(tokenURI), parameters, null);

            responseToken = new ObjectMapper().readValue(response, ResponseToken.class);

        } catch (IOException e) {
            LogUtil.printErrorLog(ErrorLogs.UnableToReadResponse.getLog());
            throw new SkyflowException(ErrorCode.UnableToReadResponse, e);
        }

        return responseToken;
    }

    private static String getSignedToken(String clientID, String keyID, String tokenURI, PrivateKey pvtKey,
            String context) {
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

    private static String getScopeUsingRoles(String[] roles) {
        StringBuilder scope = new StringBuilder();
        if (roles != null) {
            for (String role : roles) {
                scope.append(" role:").append(role);
            }
        }
        return scope.toString();
    }
}
