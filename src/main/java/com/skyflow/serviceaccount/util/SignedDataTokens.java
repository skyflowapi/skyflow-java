/*
	Copyright (c) 2022 Skyflow, Inc.
*/
package com.skyflow.serviceaccount.util;

import com.skyflow.common.utils.Helpers;
import com.skyflow.common.utils.LogUtil;
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
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class SignedDataTokens {
    private final File credentialsFile;
    private final String credentialsString;
    private final String context;

    private final String[] dataTokens;
    private final Double timeToLive;

    private final String credentialsType;

    private SignedDataTokens(SignedDataTokensBuilder builder) {
        this.credentialsFile = builder.credentialsFile;
        this.credentialsString = builder.credentialsString;
        this.context = builder.context;
        this.timeToLive = builder.timeToLive;
        this.dataTokens = builder.dataTokens;
        this.credentialsType = builder.credentialsType;
    }

    // Builder class
    public static class SignedDataTokensBuilder {
        private File credentialsFile;
        private String credentialsString;
        private String context;

        private String[] dataTokens;
        private Double timeToLive;

        private String credentialsType;

        private void setCredentialsType(String credentialsType) {
            this.credentialsType = credentialsType;
        }

        public SignedDataTokensBuilder setCredentials(File credentialsFile) {
            setCredentialsType("FILE");
            this.credentialsFile = credentialsFile;
            return this;
        }

        public SignedDataTokensBuilder setCredentials(String credentialsString) {
            setCredentialsType("STRING");
            this.credentialsString = credentialsString;
            return this;
        }

        public SignedDataTokensBuilder setContext(String context) {
            this.context = context;
            return this;
        }

        public SignedDataTokensBuilder setDataTokens(String[] dataTokens) {
            this.dataTokens = dataTokens;
            return this;
        }

        public SignedDataTokensBuilder setTimeToLive(Double timeToLive) {
            this.timeToLive = timeToLive;
            return this;
        }

        public SignedDataTokens build() {
            return new SignedDataTokens(this);
        }
    }

    public synchronized List<SignedDataTokenResponse> getSignedDataTokens() throws SkyflowException {
        List<SignedDataTokenResponse> signedToken = new ArrayList<>();

        try {
            if (this.credentialsFile != null && Objects.equals(this.credentialsType, "FILE")) {
                signedToken = generateSignedTokens(this.credentialsFile, this.dataTokens, this.timeToLive,
                        this.context);
            } else if (this.credentialsString != null && Objects.equals(this.credentialsType, "STRING")) {
                signedToken = generateSignedTokensFromCredentialsString(this.credentialsString, this.dataTokens,
                        this.timeToLive, this.context);

            }
        } catch (SkyflowException e) {
            e.printStackTrace();
        }
        return signedToken;
    }

    private static List<SignedDataTokenResponse> generateSignedTokens(File credentialsPath, String[] dataTokens,
            Double timeToLive, String context) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.GenerateBearerTokenFromCredsCalled.getLog());
        JSONParser parser = new JSONParser();
        List<SignedDataTokenResponse> responseToken;

        try {
            if (credentialsPath == null || !credentialsPath.isFile()) {
                LogUtil.printErrorLog(ErrorLogs.EmptyJSONString.getLog());
                throw new SkyflowException(ErrorCode.EmptyJSONString);
            }

            Object obj = parser.parse(new FileReader(String.valueOf(credentialsPath)));
            JSONObject saCreds = (JSONObject) obj;

            responseToken = getSignedTokenFromCredsFile(saCreds, dataTokens, timeToLive, context);

        } catch (ParseException e) {
            LogUtil.printErrorLog(ErrorLogs.InvalidJSONStringFormat.getLog());
            throw new SkyflowException(ErrorCode.InvalidJSONStringFormat, e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return responseToken;
    }

    private static List<SignedDataTokenResponse> generateSignedTokensFromCredentialsString(String credentials,
            String[] dataTokens, Double timeToLive, String context) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.GenerateBearerTokenFromCredsCalled.getLog());
        JSONParser parser = new JSONParser();
        List<SignedDataTokenResponse> responseToken;
        LogUtil.printInfoLog(InfoLogs.GenerateBearerTokenFromCredsCalled.getLog());

        try {
            if (credentials == null || credentials.isEmpty()) {
                LogUtil.printErrorLog(ErrorLogs.EmptyJSONString.getLog());
                throw new SkyflowException(ErrorCode.EmptyJSONString);
            }

            Object obj = parser.parse(credentials);
            JSONObject saCreds = (JSONObject) obj;

            responseToken = getSignedTokenFromCredsFile(saCreds, dataTokens, timeToLive, context);

        } catch (ParseException e) {
            LogUtil.printErrorLog(ErrorLogs.InvalidJSONStringFormat.getLog());
            throw new SkyflowException(ErrorCode.InvalidJSONStringFormat, e);
        }

        return responseToken;
    }

    private static List<SignedDataTokenResponse> getSignedTokenFromCredsFile(JSONObject creds, String[] dataTokens,
            Double timeToLive, String context) throws SkyflowException {
        List<SignedDataTokenResponse> responseToken;

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

            PrivateKey pvtKey = Helpers.getPrivateKeyFromPem((String) creds.get("privateKey"));

            responseToken = getSignedToken(clientID, keyID, pvtKey, dataTokens, timeToLive, context);

        } catch (RuntimeException e) {
            throw new SkyflowException(ErrorCode.IncorrectCredentials, e);
        }

        return responseToken;
    }

    private static List<SignedDataTokenResponse> getSignedToken(String clientID, String keyID, PrivateKey pvtKey,
            String[] dataTokens, Double timeToLive, String context) {
        final Date createdDate = new Date();
        final Date expirationDate;

        if (timeToLive != null) {
            expirationDate = new Date((long) (createdDate.getTime() + (timeToLive * 1000)));
        } else {
            expirationDate = new Date(createdDate.getTime() + 60000); // Valid for 60 seconds
        }

        List<SignedDataTokenResponse> list = new ArrayList<>();
        String prefix = "signed_token_";
        for (String dataToken : dataTokens) {

            String eachSignedDataToken = Jwts.builder()
                    .claim("iss", "sdk")
                    .claim("iat", createdDate.getTime())
                    .claim("key", keyID)
                    .claim("sub", clientID)
                    .claim("ctx", context)
                    .claim("tok", dataToken)
                    .setExpiration(expirationDate)
                    .signWith(SignatureAlgorithm.RS256, pvtKey)
                    .compact();

            SignedDataTokenResponse responseObject = new SignedDataTokenResponse(dataToken,
                    prefix + eachSignedDataToken);

            list.add(responseObject);
        }
        return list;
    }
}
