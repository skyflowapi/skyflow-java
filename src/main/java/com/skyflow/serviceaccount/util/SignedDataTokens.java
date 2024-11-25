package com.skyflow.serviceaccount.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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

    private static List<SignedDataTokenResponse> generateSignedTokenFromCredentialsFile(
            File credentialsFile, ArrayList<String> dataTokens, Integer timeToLive, String context
    ) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.GENERATE_SIGNED_TOKENS_FROM_CREDENTIALS_FILE_TRIGGERED.getLog());
        List<SignedDataTokenResponse> responseToken;
        try {
            if (credentialsFile == null || !credentialsFile.isFile()) {
                LogUtil.printErrorLog(ErrorLogs.INVALID_CREDENTIALS_FILE.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidCredentials.getMessage());
            }
            FileReader reader = new FileReader(String.valueOf(credentialsFile));
            JsonObject serviceAccountCredentials = JsonParser.parseReader(reader).getAsJsonObject();
            responseToken = generateSignedTokensFromCredentials(serviceAccountCredentials, dataTokens, timeToLive, context);
        } catch (JsonSyntaxException e) {
            LogUtil.printErrorLog(ErrorLogs.INVALID_CREDENTIALS_FILE_FORMAT.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), Utils.parameterizedString(
                    ErrorMessage.FileInvalidJson.getMessage(), credentialsFile.getPath()));
        } catch (FileNotFoundException e) {
            LogUtil.printErrorLog(ErrorLogs.CREDENTIALS_FILE_NOT_FOUND.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), Utils.parameterizedString(
                    ErrorMessage.FileNotFound.getMessage(), credentialsFile.getPath()));
        }
        return responseToken;
    }

    private static List<SignedDataTokenResponse> generateSignedTokensFromCredentialsString(
            String credentials, ArrayList<String> dataTokens, Integer timeToLive, String context
    ) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.GENERATE_SIGNED_TOKENS_FROM_CREDENTIALS_STRING_TRIGGERED.getLog());
        List<SignedDataTokenResponse> responseToken;
        try {
            if (credentials == null || credentials.isEmpty()) {
                LogUtil.printErrorLog(ErrorLogs.INVALID_CREDENTIALS_STRING.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidCredentials.getMessage());
            }
            JsonObject serviceAccountCredentials = JsonParser.parseString(credentials).getAsJsonObject();
            responseToken = generateSignedTokensFromCredentials(serviceAccountCredentials, dataTokens, timeToLive, context);
        } catch (JsonSyntaxException e) {
            LogUtil.printErrorLog(ErrorLogs.INVALID_CREDENTIALS_STRING_FORMAT.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                    ErrorMessage.CredentialsStringInvalidJson.getMessage());
        }
        return responseToken;
    }

    private static List<SignedDataTokenResponse> generateSignedTokensFromCredentials(
            JsonObject credentials, ArrayList<String> dataTokens, Integer timeToLive, String context
    ) throws SkyflowException {
        List<SignedDataTokenResponse> signedDataTokens = null;
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
            PrivateKey pvtKey = Utils.getPrivateKeyFromPem(privateKey.getAsString());
            signedDataTokens = getSignedToken(
                    clientID.getAsString(), keyID.getAsString(), pvtKey, dataTokens, timeToLive, context);
        } catch (RuntimeException e) {
            LogUtil.printErrorLog(ErrorLogs.SIGNED_DATA_TOKENS_REJECTED.getLog());
            throw new SkyflowException(e);
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
        LogUtil.printInfoLog(InfoLogs.GET_SIGNED_DATA_TOKENS_TRIGGERED.getLog());
        List<SignedDataTokenResponse> signedToken = new ArrayList<>();
        if (this.credentialsFile != null && Objects.equals(this.credentialsType, "FILE")) {
            signedToken = generateSignedTokenFromCredentialsFile(this.credentialsFile, this.dataTokens, this.timeToLive, this.ctx);
        } else if (this.credentialsString != null && Objects.equals(this.credentialsType, "STRING")) {
            signedToken = generateSignedTokensFromCredentialsString(this.credentialsString, this.dataTokens, this.timeToLive, this.ctx);
        }
        LogUtil.printInfoLog(InfoLogs.GET_SIGNED_DATA_TOKEN_SUCCESS.getLog());
        return signedToken;
    }

    public static class SignedDataTokensBuilder {
        private ArrayList<String> dataTokens;
        private Integer timeToLive;
        private File credentialsFile;
        private String credentialsString;
        private String ctx;
        private String credentialsType;

        private SignedDataTokensBuilder() {
        }

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
