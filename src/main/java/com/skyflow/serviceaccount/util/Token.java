package com.skyflow.serviceaccount.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyflow.common.utils.HttpUtility;
import com.skyflow.entities.ResponseToken;
import com.skyflow.errors.SkyflowException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

import static com.skyflow.errors.ErrorCodesEnum.InvalidInput;
import static com.skyflow.errors.ErrorCodesEnum.Server;

public class Token {

    /**
     * GenerateToken - Generates a Service Account Token from the given Service Account Credential file with a default timeout of 60minutes.
     *
     * @param filepath
     */
    public static ResponseToken GenerateToken(String filepath) throws SkyflowException {

        JSONParser parser = new JSONParser();
        ResponseToken responseToken = null;
        Path path = null;
        try {
            path = Paths.get((filepath));
            Object obj = parser.parse(new FileReader(String.valueOf(path)));
            JSONObject saCreds = (JSONObject) obj;

            responseToken = getSATokenFromCredsFile(saCreds);

        } catch (FileNotFoundException e) {
            throw new SkyflowException(InvalidInput, String.format("Unable to open credentials - file %s", path), e);
        } catch (IOException e) {
            throw new SkyflowException(InvalidInput, String.format("Unable to open credentials - file %s", path), e);
        } catch (ParseException e) {
            throw new SkyflowException(InvalidInput, String.format("Provided json file is in wrong format - file %s", path), e);
        }

        return responseToken;
    }

    /**
     * getSATokenFromCredsFile gets bearer token from service account endpoint
     *
     * @param creds
     *
     */
    public static ResponseToken getSATokenFromCredsFile(JSONObject creds) throws SkyflowException {
        ResponseToken responseToken = null;
        try {
            String clientID = (String) creds.get("clientID");
            if (clientID == null) {
                throw new SkyflowException(InvalidInput, "Unable to read clientID");
            }
            String keyID = (String) creds.get("keyID");
            if (keyID == null) {
                throw new SkyflowException(InvalidInput, "Unable to read keyID");
            }
            String tokenURI = (String) creds.get("tokenURI");
            if (tokenURI == null) {
                throw new SkyflowException(InvalidInput, "Unable to read tokenURI");
            }

            PrivateKey pvtKey = getPrivateKeyFromPem((String) creds.get("privateKey"));

            String signedUserJWT = getSignedUserToken(clientID, keyID, tokenURI, pvtKey);

            JSONObject parameters = new JSONObject();
            parameters.put("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
            parameters.put("assertion", signedUserJWT);

            String response = HttpUtility.sendRequest("POST", tokenURI, parameters);

            responseToken = new ObjectMapper().readValue(response, ResponseToken.class);

        } catch (JsonMappingException e) {
            throw new SkyflowException(Server, "Unable to read response payload", e);
        } catch (JsonParseException e) {
            throw new SkyflowException(Server, "Unable to read response payload", e);
        } catch (IOException e) {
            throw new SkyflowException(Server, "Unable to read response payload", e);
        }

        return responseToken;
    }

    public static PrivateKey getPrivateKeyFromPem(String pemKey) throws SkyflowException {

        String PKCS1PrivateHeader = "-----BEGIN RSA PRIVATE KEY-----";
        String PKCS1PrivateFooter = "-----END RSA PRIVATE KEY-----";

        String PKCS8PrivateHeader = "-----BEGIN PRIVATE KEY-----";
        String PKCS8PrivateFooter = "-----END PRIVATE KEY-----";

        String privateKeyContent = pemKey;
        PrivateKey privateKey = null;
        if (pemKey.contains(PKCS1PrivateHeader)) {
            privateKeyContent = privateKeyContent.replace(PKCS1PrivateHeader, "");
            privateKeyContent = privateKeyContent.replace(PKCS1PrivateFooter, "");
            privateKeyContent = privateKeyContent.replace("\n", "");
            privateKeyContent = privateKeyContent.replace("\r\n", "");
            privateKey = parsePkcs1PrivateKey(Base64.getDecoder().decode(privateKeyContent));
        } else if (pemKey.contains(PKCS8PrivateHeader)) {
            privateKeyContent = privateKeyContent.replace(PKCS8PrivateHeader, "");
            privateKeyContent = privateKeyContent.replace(PKCS8PrivateFooter, "");
            privateKeyContent = privateKeyContent.replace("\n", "");
            privateKeyContent = privateKeyContent.replace("\r\n", "");
            privateKey = parsePkcs8PrivateKey(Base64.getDecoder().decode(privateKeyContent));
        } else {
            throw new SkyflowException(InvalidInput, "Unable to retrieve RSA private key");
        }
        return privateKey;
    }

    /**
     * Create a PrivateKey instance from raw PKCS#8 bytes.
     */
    private static PrivateKey parsePkcs8PrivateKey(byte[] pkcs8Bytes) throws SkyflowException {
        KeyFactory keyFactory;
        PrivateKey privateKey = null;
        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
            keyFactory = KeyFactory.getInstance("RSA");
            privateKey = keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new SkyflowException(InvalidInput, "Invalid algorithm", e);
        } catch (InvalidKeySpecException e) {
            throw new SkyflowException(InvalidInput, "Unable to parse RSA private key", e);
        }
        return privateKey;
    }

    /**
     * Create a PrivateKey instance from raw PKCS#1 bytes.
     */
    private static PrivateKey parsePkcs1PrivateKey(byte[] pkcs1Bytes) throws SkyflowException {
        int pkcs1Length = pkcs1Bytes.length;
        int totalLength = pkcs1Length + 22;
        byte[] pkcs8Header = new byte[]{
                0x30, (byte) 0x82, (byte) ((totalLength >> 8) & 0xff), (byte) (totalLength & 0xff), // Sequence + total length
                0x2, 0x1, 0x0, // Integer (0)
                0x30, 0xD, 0x6, 0x9, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0xD, 0x1, 0x1, 0x1, 0x5, 0x0, // Sequence: 1.2.840.113549.1.1.1, NULL
                0x4, (byte) 0x82, (byte) ((pkcs1Length >> 8) & 0xff), (byte) (pkcs1Length & 0xff) // Octet string + length
        };
        byte[] pkcs8bytes = joinBytes(pkcs8Header, pkcs1Bytes);
        return parsePkcs8PrivateKey(pkcs8bytes);
    }

    private static byte[] joinBytes(byte[] a, byte[] b) {
        byte[] bytes = new byte[a.length + b.length];
        System.arraycopy(a, 0, bytes, 0, a.length);
        System.arraycopy(b, 0, bytes, a.length, b.length);
        return bytes;
    }

    public static String getSignedUserToken(String clientID, String keyID, String tokenURI, PrivateKey pvtKey) {
        Instant now = Instant.now();

        String signedToken = Jwts.builder()
                .claim("iss", clientID)
                .claim("key", keyID)
                .claim("aud", tokenURI)
                .claim("sub", clientID)
                .setExpiration(Date.from(now.plus(60l, ChronoUnit.MINUTES)))
                .signWith(SignatureAlgorithm.RS256, pvtKey)
                .compact();

        return signedToken;
    }
}

