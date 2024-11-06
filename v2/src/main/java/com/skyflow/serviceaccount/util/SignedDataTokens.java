package com.skyflow.serviceaccount.util;

import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Utils;
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
    private final String credentialsType;
    private final String ctx;
    private final ArrayList<String> dataTokens;
    private final Integer timeToLive;

    private SignedDataTokens(SignedDataTokensBuilder builder) {
        this.credentialsFile = builder.credentialsFile;
        this.credentialsString = builder.credentialsString;
        this.credentialsType = builder.credentialsType;
        this.ctx = builder.ctx;
        this.dataTokens = builder.dataTokens;
        this.timeToLive = builder.timeToLive;
    }

    public static SignedDataTokensBuilder builder() {
        return new SignedDataTokensBuilder();
    }

    private static List<SignedDataTokenResponse> generateSignedTokens(
            File credentialsPath, ArrayList<String> dataTokens, Integer timeToLive, String context
    ) throws SkyflowException {
        // print info log
        JSONParser parser = new JSONParser();
        List<SignedDataTokenResponse> responseToken;
        try {
            if (credentialsPath == null || !credentialsPath.isFile()) {
                // print error log
                throw new SkyflowException("credentials string cannot be empty");
            }
            Object obj = parser.parse(new FileReader(String.valueOf(credentialsPath)));
            JSONObject serviceAccountCredentials = (JSONObject) obj;
            responseToken = getSignedTokenFromCredentialsFile(serviceAccountCredentials, dataTokens, timeToLive, context);
        } catch (ParseException e) {
            // print error log
            throw new SkyflowException("credentials string is not in valid json string format");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return responseToken;
    }

    private static List<SignedDataTokenResponse> generateSignedTokensFromCredentialsString(
            String credentials, ArrayList<String> dataTokens, Integer timeToLive, String context
    ) throws SkyflowException {
        // print info log
        JSONParser parser = new JSONParser();
        List<SignedDataTokenResponse> responseToken;
        // print info log
        try {
            if (credentials == null || credentials.isEmpty()) {
                // print error log
                throw new SkyflowException("credentials string cannot by empty");
            }
            Object obj = parser.parse(credentials);
            JSONObject serviceAccountCredentials = (JSONObject) obj;
            responseToken = getSignedTokenFromCredentialsFile(serviceAccountCredentials, dataTokens, timeToLive, context);

        } catch (ParseException e) {
            // print error log
            throw new SkyflowException("credentials string is not in valid json string format");
        }

        return responseToken;
    }

    private static List<SignedDataTokenResponse> getSignedTokenFromCredentialsFile(
            JSONObject credentials, ArrayList<String> dataTokens, Integer timeToLive, String context
    ) throws SkyflowException {
        List<SignedDataTokenResponse> signedDataTokens = null;
        try {
            String privateKey = (String) credentials.get("privateKey");
            if (privateKey == null) {
                throw new SkyflowException("privateKey not found");
            }

            String clientID = (String) credentials.get("clientID");
            if (clientID == null) {
                // print error log
                throw new SkyflowException("invalid clientID");
            }
            String keyID = (String) credentials.get("keyID");
            if (keyID == null) {
                // print error log
                throw new SkyflowException("invalid KeyID");
            }
            PrivateKey pvtKey = Utils.getPrivateKeyFromPem(privateKey);
            signedDataTokens = getSignedToken(clientID, keyID, pvtKey, dataTokens, timeToLive, context);
        } catch (RuntimeException e) {
            // throw error
        }
        return signedDataTokens;
    }

    private static List<SignedDataTokenResponse> getSignedToken(
            String clientID, String keyID, PrivateKey pvtKey,
            ArrayList<String> dataTokens, Integer timeToLive, String context
    ) {
        final Date createdDate = new Date();
        final Date expirationDate;

        if (timeToLive != null) {
            expirationDate = new Date(createdDate.getTime() + (timeToLive * 1000));
        } else {
            expirationDate = new Date(createdDate.getTime() + 60000); // Valid for 60 seconds
        }

        List<SignedDataTokenResponse> list = new ArrayList<>();
        for (String dataToken : dataTokens) {
            String eachSignedDataToken = Jwts.builder()
                    .claim("iss", "sdk")
                    .claim("iat", (createdDate.getTime() / 1000))
                    .claim("key", keyID)
                    .claim("sub", clientID)
                    .claim("ctx", context)
                    .claim("tok", dataToken)
                    .setExpiration(expirationDate)
                    .signWith(SignatureAlgorithm.RS256, pvtKey)
                    .compact();
            SignedDataTokenResponse responseObject = new SignedDataTokenResponse(dataToken, eachSignedDataToken);
            list.add(responseObject);
        }
        return list;
    }

    public synchronized List<SignedDataTokenResponse> getSignedDataTokens() throws SkyflowException {
        List<SignedDataTokenResponse> signedToken = new ArrayList<>();
        try {
            if (this.credentialsFile != null && Objects.equals(this.credentialsType, "FILE")) {
                signedToken = generateSignedTokens(this.credentialsFile, this.dataTokens, this.timeToLive, this.ctx);
            } else if (this.credentialsString != null && Objects.equals(this.credentialsType, "STRING")) {
                signedToken = generateSignedTokensFromCredentialsString(this.credentialsString, this.dataTokens, this.timeToLive, this.ctx);
            }
        } catch (SkyflowException e) {
            e.printStackTrace();
        }
        return signedToken;
    }

    public static class SignedDataTokensBuilder {
        private ArrayList<String> dataTokens;
        private Integer timeToLive;
        private File credentialsFile;
        private String credentialsString;
        private String ctx;
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

        public SignedDataTokensBuilder setCtx(String ctx) {
            this.ctx = ctx;
            return this;
        }

        public SignedDataTokensBuilder setDataTokens(ArrayList<String> dataTokens) {
            this.dataTokens = dataTokens;
            return this;
        }

        public SignedDataTokensBuilder setTimeToLive(Integer timeToLive) {
            this.timeToLive = timeToLive;
            return this;
        }

        public SignedDataTokens build() {
            return new SignedDataTokens(this);
        }
    }
}
