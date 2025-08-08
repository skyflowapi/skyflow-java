package com.skyflow.common.utils;

import com.skyflow.common.config.Credentials;
import com.skyflow.common.enums.Env;
import com.skyflow.common.errors.ErrorCode;
import com.skyflow.common.errors.ErrorMessage;
import com.skyflow.common.errors.SkyflowException;
import com.skyflow.common.logger.LogUtil;
import com.skyflow.common.logs.ErrorLogs;
import com.skyflow.common.serviceaccount.util.BearerToken;
import org.apache.commons.codec.binary.Base64;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class CommonUtils {
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
        } else {
            return credentials.getToken();
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
            LogUtil.printErrorLog(ErrorLogs.JWT_INVALID_FORMAT.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.JwtInvalidFormat.getMessage());
        }
        return privateKey;
    }
    public static String getBaseURL(String url) throws MalformedURLException {
        URL parsedUrl = new URL(url);
        String protocol = parsedUrl.getProtocol();
        String host = parsedUrl.getHost();
        return String.format("%s://%s", protocol, host);
    }

    public static String parameterizedString(String base, String... args) {
        for (int index = 0; index < args.length; index++) {
            base = base.replace("%s" + (index + 1), args[index]);
        }
        return base;
    }
    private static PrivateKey parsePkcs8PrivateKey(byte[] pkcs8Bytes) throws SkyflowException {
        KeyFactory keyFactory;
        PrivateKey privateKey = null;
        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
            keyFactory = KeyFactory.getInstance("RSA");
            privateKey = keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            LogUtil.printErrorLog(ErrorLogs.INVALID_ALGORITHM.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidAlgorithm.getMessage());
        } catch (InvalidKeySpecException e) {
            LogUtil.printErrorLog(ErrorLogs.INVALID_KEY_SPEC.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidKeySpec.getMessage());
        }
        return privateKey;
    }
}
