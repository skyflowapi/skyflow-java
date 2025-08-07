package com.skyflow.v2.utils;

import com.google.gson.JsonObject;
import com.skyflow.common.serviceaccount.util.BearerToken;
import com.skyflow.v2.config.ConnectionConfig;
import com.skyflow.v2.config.Credentials;
import com.skyflow.v2.enums.Env;
import com.skyflow.v2.errors.ErrorCode;
import com.skyflow.v2.errors.ErrorMessage;
import com.skyflow.v2.errors.SkyflowException;
import com.skyflow.v2.logs.ErrorLogs;
import com.skyflow.v2.logs.InfoLogs;
import com.skyflow.v2.utils.logger.LogUtil;
import com.skyflow.v2.vault.connection.InvokeConnectionRequest;
import org.apache.commons.codec.binary.Base64;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

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

    public static String constructConnectionURL(ConnectionConfig config, InvokeConnectionRequest invokeConnectionRequest) {
        StringBuilder filledURL = new StringBuilder(config.getConnectionUrl());

        if (invokeConnectionRequest.getPathParams() != null && !invokeConnectionRequest.getPathParams().isEmpty()) {
            for (Map.Entry<String, String> entry : invokeConnectionRequest.getPathParams().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                filledURL = new StringBuilder(filledURL.toString().replace(String.format("{%s}", key), value));
            }
        }

        if (invokeConnectionRequest.getQueryParams() != null && !invokeConnectionRequest.getQueryParams().isEmpty()) {
            filledURL.append("?");
            for (Map.Entry<String, String> entry : invokeConnectionRequest.getQueryParams().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                filledURL.append(key).append("=").append(value).append("&");
            }
            filledURL = new StringBuilder(filledURL.substring(0, filledURL.length() - 1));
        }

        return filledURL.toString();
    }

    public static Map<String, String> constructConnectionHeadersMap(Map<String, String> requestHeaders) {
        Map<String, String> headersMap = new HashMap<>();
        for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            headersMap.put(key.toLowerCase(), value);
        }
        return headersMap;
    }

    public static JsonObject getMetrics() {
        JsonObject details = new JsonObject();
        String sdkVersion = Constants.SDK_VERSION;
        String deviceModel;
        String osDetails;
        String javaVersion;
        // Retrieve device model
        try {
            deviceModel = System.getProperty("os.name");
            if (deviceModel == null) throw new Exception();
        } catch (Exception e) {
            LogUtil.printInfoLog(parameterizedString(
                    InfoLogs.UNABLE_TO_GENERATE_SDK_METRIC.getLog(),
                    Constants.SDK_METRIC_CLIENT_DEVICE_MODEL
            ));
            deviceModel = "";
        }

        // Retrieve OS details
        try {
            osDetails = System.getProperty("os.version");
            if (osDetails == null) throw new Exception();
        } catch (Exception e) {
            LogUtil.printInfoLog(parameterizedString(
                    InfoLogs.UNABLE_TO_GENERATE_SDK_METRIC.getLog(),
                    Constants.SDK_METRIC_CLIENT_OS_DETAILS
            ));
            osDetails = "";
        }

        // Retrieve Java version details
        try {
            javaVersion = System.getProperty("java.version");
            if (javaVersion == null) throw new Exception();
        } catch (Exception e) {
            LogUtil.printInfoLog(parameterizedString(
                    InfoLogs.UNABLE_TO_GENERATE_SDK_METRIC.getLog(),
                    Constants.SDK_METRIC_RUNTIME_DETAILS
            ));
            javaVersion = "";
        }
        details.addProperty(Constants.SDK_METRIC_NAME_VERSION, Constants.SDK_METRIC_NAME_VERSION_PREFIX + sdkVersion);
        details.addProperty(Constants.SDK_METRIC_CLIENT_DEVICE_MODEL, deviceModel);
        details.addProperty(Constants.SDK_METRIC_RUNTIME_DETAILS, Constants.SDK_METRIC_RUNTIME_DETAILS_PREFIX + javaVersion);
        details.addProperty(Constants.SDK_METRIC_CLIENT_OS_DETAILS, osDetails);
        return details;
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
