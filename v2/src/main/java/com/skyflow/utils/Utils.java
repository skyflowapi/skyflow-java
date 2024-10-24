package com.skyflow.utils;

import com.skyflow.config.Credentials;
import com.skyflow.enums.Env;
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.BearerToken;
import org.apache.commons.codec.binary.Base64;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public final class Utils {
    public static String getVaultURL(String clusterId, Env env) {
        StringBuilder sb = new StringBuilder(Constants.SECURE_PROTOCOL);
        sb.append(clusterId);
        switch (env) {
            case DEV:
                sb.append(Constants.DEV_DOMAIN);
                break;
            case STAGE:
                sb.append(Constants.STAGE_DOMAIN);
                break;
            case SANDBOX:
                sb.append(Constants.SANDBOX_DOMAIN);
                break;
            case PROD:
            default:
                sb.append(Constants.PROD_DOMAIN);
                break;
        }
        return sb.toString();
    }

    public static String generateBearerToken(Credentials credentials) throws SkyflowException {
        if (credentials.getPath() != null) {
            return BearerToken.builder()
                    .setCredentials(new File(credentials.getPath()))
                    .setRoles(credentials.getRoles())
                    .setCtx(credentials.getContext())
                    .build()
                    .getBearerToken();
        } else if (credentials.getCredentialsString() != null) {
            return BearerToken.builder()
                    .setCredentials(credentials.getCredentialsString())
                    .setRoles(credentials.getRoles())
                    .setCtx(credentials.getContext())
                    .build()
                    .getBearerToken();
        } else if (credentials.getToken() != null) {
            return credentials.getToken();
        } else {
            return credentials.getApiKey();
        }
    }

    public static PrivateKey getPrivateKeyFromPem(String pemKey) throws SkyflowException {
        String PKCS8PrivateHeader = Constants.PKCS8_PRIVATE_HEADER;
        String PKCS8PrivateFooter = Constants.PKCS8_PRIVATE_FOOTER;

        String privateKeyContent = pemKey;
        PrivateKey privateKey = null;

        if (pemKey.contains(PKCS8PrivateHeader)) {
            privateKeyContent = privateKeyContent.replace(PKCS8PrivateHeader, "");
            privateKeyContent = privateKeyContent.replace(PKCS8PrivateFooter, "");
            privateKeyContent = privateKeyContent.replace("\n", "");
            privateKeyContent = privateKeyContent.replace("\r\n", "");
            privateKey = parsePkcs8PrivateKey(Base64.decodeBase64(privateKeyContent));
        } else {
            // display error log and throw error
        }
        return privateKey;
    }

    public static String getBaseURL(String url) throws MalformedURLException {
        URL parsedUrl = new URL(url);
        String protocol = parsedUrl.getProtocol();
        String host = parsedUrl.getHost();
        return String.format("%s://%s", protocol, host);
    }

    private static PrivateKey parsePkcs8PrivateKey(byte[] pkcs8Bytes) throws SkyflowException {
        KeyFactory keyFactory;
        PrivateKey privateKey = null;
        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
            keyFactory = KeyFactory.getInstance("RSA");
            privateKey = keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            // display error log and throw error
        } catch (InvalidKeySpecException e) {
            // display error log and throw error
        }
        return privateKey;
    }
}
