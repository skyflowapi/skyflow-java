/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.serviceaccount.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyflow.common.utils.Helpers;
import com.skyflow.common.utils.HttpUtility;
import com.skyflow.common.utils.LogUtil;
import com.skyflow.common.utils.TokenUtils;
import com.skyflow.entities.ResponseToken;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.logs.WarnLogs;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Date;

public final class  Token {

    /**
     * @param filepath
     * @deprecated use generateBearerToken(string filepath), GenerateToken will be removed in future
     */
    @Deprecated
    public static ResponseToken GenerateToken(String filepath) throws SkyflowException {
        LogUtil.printWarningLog(WarnLogs.GetTokenDeprecated.getLog());
        return generateBearerToken(filepath);
    }

    /**
     * Generates a Bearer Token from the given Service Account Credential file with a default timeout of 60minutes.
     *
     * @param filepath
     */
    public static ResponseToken generateBearerToken(String filepath) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.GenerateBearerTokenCalled.getLog());
        JSONParser parser = new JSONParser();
        ResponseToken responseToken = null;
        Path path = null;
        try {
            if (filepath == null || filepath.isEmpty()) {
                LogUtil.printErrorLog(ErrorLogs.EmptyFilePath.getLog());
                throw new SkyflowException(ErrorCode.EmptyFilePath);
            }
            path = Paths.get((filepath));
            Object obj = parser.parse(new FileReader(String.valueOf(path)));
            JSONObject saCreds = (JSONObject) obj;

            responseToken = getSATokenFromCredsFile(saCreds);

        } catch (FileNotFoundException e) {
            LogUtil.printErrorLog(Helpers.parameterizedString(ErrorLogs.InvalidCredentialsPath.getLog(), String.valueOf(path)));
            throw new SkyflowException(ErrorCode.InvalidCredentialsPath.getCode(), Helpers.parameterizedString(ErrorCode.InvalidCredentialsPath.getDescription(), String.valueOf(path)), e);
        } catch (IOException e) {
            LogUtil.printErrorLog(Helpers.parameterizedString(ErrorLogs.InvalidCredentialsPath.getLog(), String.valueOf(path)));
            throw new SkyflowException(ErrorCode.InvalidCredentialsPath.getCode(), Helpers.parameterizedString(ErrorCode.InvalidCredentialsPath.getDescription(), String.valueOf(path)), e);
        } catch (ParseException e) {
            LogUtil.printErrorLog(Helpers.parameterizedString(ErrorLogs.InvalidJsonFormat.getLog(), String.valueOf(path)));
            throw new SkyflowException(ErrorCode.InvalidJsonFormat.getCode(), Helpers.parameterizedString(ErrorCode.InvalidJsonFormat.getDescription(), String.valueOf(path)), e);
        }

        return responseToken;
    }

    /**
     * Generates a Bearer Token from the given Service Account Credential json string with a default timeout of 60minutes.
     *
     * @param credentials JSON string of credentials file
     */

    public static ResponseToken generateBearerTokenFromCreds(String credentials) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.GenerateBearerTokenFromCredsCalled.getLog());
        JSONParser parser = new JSONParser();
        ResponseToken responseToken = null;
        try {
            if (credentials == null || credentials.isEmpty()) {
                LogUtil.printErrorLog(ErrorLogs.EmptyJSONString.getLog());
                throw new SkyflowException(ErrorCode.EmptyJSONString);
            }

            Object obj = parser.parse(credentials);
            JSONObject saCreds = (JSONObject) obj;

            responseToken = getSATokenFromCredsFile(saCreds);

        } catch (ParseException e) {
            LogUtil.printErrorLog(ErrorLogs.InvalidJSONStringFormat.getLog());
            throw new SkyflowException(ErrorCode.InvalidJSONStringFormat, e);
        }

        return responseToken;
    }

    /**
     * getSATokenFromCredsFile gets bearer token from service account endpoint
     *
     * @param creds
     */
    private static ResponseToken getSATokenFromCredsFile(JSONObject creds) throws SkyflowException {
        ResponseToken responseToken = null;
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

            String signedUserJWT = getSignedUserToken(clientID, keyID, tokenURI, pvtKey);

            JSONObject parameters = new JSONObject();
            parameters.put("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
            parameters.put("assertion", signedUserJWT);

            String response = HttpUtility.sendRequest("POST", new URL(tokenURI), parameters, null);

            responseToken = new ObjectMapper().readValue(response, ResponseToken.class);

        } catch (JsonMappingException e) {
            LogUtil.printErrorLog(ErrorLogs.UnableToReadResponse.getLog());
            throw new SkyflowException(ErrorCode.UnableToReadResponse, e);
        } catch (JsonParseException e) {
            LogUtil.printErrorLog(ErrorLogs.UnableToReadResponse.getLog());
            throw new SkyflowException(ErrorCode.UnableToReadResponse, e);
        } catch (IOException e) {
            LogUtil.printErrorLog(ErrorLogs.UnableToReadResponse.getLog());
            throw new SkyflowException(ErrorCode.UnableToReadResponse, e);
        }

        return responseToken;
    }
//    /**
//     * Create a PrivateKey instance from raw PKCS#1 bytes.
//     */
//    private static PrivateKey parsePkcs1PrivateKey(byte[] pkcs1Bytes) throws SkyflowException {
//        int pkcs1Length = pkcs1Bytes.length;
//        int totalLength = pkcs1Length + 22;
//        byte[] pkcs8Header = new byte[]{
//                0x30, (byte) 0x82, (byte) ((totalLength >> 8) & 0xff), (byte) (totalLength & 0xff), // Sequence + total length
//                0x2, 0x1, 0x0, // Integer (0)
//                0x30, 0xD, 0x6, 0x9, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0xD, 0x1, 0x1, 0x1, 0x5, 0x0, // Sequence: 1.2.840.113549.1.1.1, NULL
//                0x4, (byte) 0x82, (byte) ((pkcs1Length >> 8) & 0xff), (byte) (pkcs1Length & 0xff) // Octet string + length
//        };
//        byte[] pkcs8bytes = joinBytes(pkcs8Header, pkcs1Bytes);
//        return parsePkcs8PrivateKey(pkcs8bytes);
//    }
//
//    private static byte[] joinBytes(byte[] a, byte[] b) {
//        byte[] bytes = new byte[a.length + b.length];
//        System.arraycopy(a, 0, bytes, 0, a.length);
//        System.arraycopy(b, 0, bytes, a.length, b.length);
//        return bytes;
//    }

    private static String getSignedUserToken(String clientID, String keyID, String tokenURI, PrivateKey pvtKey) {
        final Date createdDate = new Date();
        final Date expirationDate = new Date(createdDate.getTime() + (3600 * 1000));
        String signedToken = Jwts.builder()
                .claim("iss", clientID)
                .claim("key", keyID)
                .claim("aud", tokenURI)
                .claim("sub", clientID)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.RS256, pvtKey)
                .compact();

        return signedToken;
    }

    /**
     * @param token
     * @deprecated use isExpired(String token), isValid will be removed in future
     */
    @Deprecated
    public static boolean isValid(String token) {
        LogUtil.printWarningLog(WarnLogs.IsValidDeprecated.getLog());
        return !isExpired(token);
    }

    public static boolean isExpired(String token) {

        long expiryTime;
        long currentTime;
        try {
            if (token == null || token.isEmpty()) {
                LogUtil.printInfoLog(InfoLogs.EmptyBearerToken.getLog());
                return true;
            }

            currentTime = new Date().getTime() / 1000;
            expiryTime = (long) TokenUtils.decoded(token).get("exp");

        } catch (ParseException e) {
            LogUtil.printInfoLog(ErrorLogs.InvalidBearerToken.getLog());
            return true;
        } catch (SkyflowException e) {
            LogUtil.printInfoLog(ErrorLogs.InvalidBearerToken.getLog());
            return true;
        }
        return currentTime >= expiryTime;
    }
}

