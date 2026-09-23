package com.skyflow.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

import com.google.gson.JsonObject;
import com.skyflow.config.BaseCredentials;
import com.skyflow.enums.Env;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.serviceaccount.util.BearerToken;
import com.skyflow.utils.logger.LogUtil;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

public class BaseUtils {

    // Memoized .env: Dotenv.load() does a filesystem read every time it's called, and several
    // call sites resolve a setting this way on every single SDK request -- re-reading a file
    // whose contents never change for the life of the process turned those into repeated,
    // uncached, blocking disk I/O on the hot path. Loaded at most once per JVM; `dotenvAttempted`
    // also memoizes the "no .env file present" outcome so a missing file isn't retried either.
    private static volatile boolean dotenvAttempted = false;
    private static volatile Dotenv cachedDotenv = null;

    private static Dotenv memoizedDotenv() {
        if (!dotenvAttempted) {
            synchronized (BaseUtils.class) {
                if (!dotenvAttempted) {
                    try {
                        cachedDotenv = Dotenv.load();
                    } catch (DotenvException e) {
                        cachedDotenv = null; // no .env file in the working directory
                    }
                    dotenvAttempted = true;
                }
            }
        }
        return cachedDotenv;
    }

    /**
     * Resolves {@code key} from the process environment first, falling back to the (memoized)
     * {@code .env} file if present. Returns null if found in neither.
     */
    public static String resolveEnvOrDotenv(String key) {
        String value = System.getenv(key);
        if (value == null) {
            Dotenv dotenv = memoizedDotenv();
            if (dotenv != null) {
                value = dotenv.get(key);
            }
        }
        return value;
    }

    /**
     * Test-only: forces the next {@link #resolveEnvOrDotenv} call to re-read the {@code .env}
     * file from disk instead of reusing the memoized one. Production code always wants the
     * memoized behavior (a project's {@code .env} doesn't change while the process is running);
     * this exists purely so tests that rewrite {@code .env} mid-run can observe the new content
     * without restarting the JVM.
     */
    public static void resetDotenvCacheForTests() {
        synchronized (BaseUtils.class) {
            dotenvAttempted = false;
            cachedDotenv = null;
        }
    }
    public static String generateBearerToken(BaseCredentials credentials) throws SkyflowException {
        if (credentials.getPath() != null) {
            BearerToken.BearerTokenBuilder builder = BearerToken.builder()
                    .setCredentials(new File(credentials.getPath()))
                    .setRoles(credentials.getRoles());
            Object ctx = credentials.getContext();
            if (ctx instanceof String) {
                builder.setCtx((String) ctx);
            } else if (ctx instanceof Map) {
                builder.setCtx((Map<String, Object>) ctx);
            }
            return builder.build().getBearerToken();
        } else if (credentials.getCredentialsString() != null) {
            BearerToken.BearerTokenBuilder builder = BearerToken.builder()
                    .setCredentials(credentials.getCredentialsString())
                    .setRoles(credentials.getRoles());
            Object ctx = credentials.getContext();
            if (ctx instanceof String) {
                builder.setCtx((String) ctx);
            } else if (ctx instanceof Map) {
                builder.setCtx((Map<String, Object>) ctx);
            }
            return builder.build().getBearerToken();
        } else {
            return credentials.getToken();
        }
    }

    public static String getVaultURL(String clusterId, Env env, String vaultDomain) {
        StringBuilder sb = buildBaseUrl(clusterId, vaultDomain);
        switch (env) {
            case DEV:
                sb.append(BaseConstants.DEV_DOMAIN);
                break;
            case STAGE:
                sb.append(BaseConstants.STAGE_DOMAIN);
                break;
            case SANDBOX:
                sb.append(BaseConstants.SANDBOX_DOMAIN);
                break;
            case PROD:
            default:
                sb.append(BaseConstants.PROD_DOMAIN);
                break;
        }
        return sb.toString();
    }

    public static PrivateKey getPrivateKeyFromPem(String pemKey) throws SkyflowException {
        String PKCS8PrivateHeader = BaseConstants.PKCS8_PRIVATE_HEADER;
        String PKCS8PrivateFooter = BaseConstants.PKCS8_PRIVATE_FOOTER;

        String privateKeyContent = pemKey;
        PrivateKey privateKey = null;

        if (pemKey.contains(PKCS8PrivateHeader)) {
            privateKeyContent = privateKeyContent.replace(PKCS8PrivateHeader, "");
            privateKeyContent = privateKeyContent.replace(PKCS8PrivateFooter, "");
            privateKeyContent = privateKeyContent.replace("\r\n", "");
            privateKeyContent = privateKeyContent.replace("\n", "");
            try {
                privateKey = parsePkcs8PrivateKey(Base64.getDecoder().decode(privateKeyContent));
            } catch (IllegalArgumentException e) {
                LogUtil.printErrorLog(ErrorLogs.INVALID_KEY_SPEC.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidKeySpec.getMessage());
            }
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

    protected static JsonObject getCommonMetrics() {
        JsonObject details = new JsonObject();
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
                    BaseConstants.SDK_METRIC_CLIENT_DEVICE_MODEL
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
                    BaseConstants.SDK_METRIC_CLIENT_OS_DETAILS
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
                    BaseConstants.SDK_METRIC_RUNTIME_DETAILS
            ));
            javaVersion = "";
        }
        details.addProperty(BaseConstants.SDK_METRIC_CLIENT_DEVICE_MODEL, deviceModel);
        details.addProperty(BaseConstants.SDK_METRIC_RUNTIME_DETAILS, BaseConstants.SDK_METRIC_RUNTIME_DETAILS_PREFIX + javaVersion);
        details.addProperty(BaseConstants.SDK_METRIC_CLIENT_OS_DETAILS, osDetails);
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

    private static StringBuilder buildBaseUrl(String clusterId, String vaultDomain) {
        StringBuilder sb = new StringBuilder(BaseConstants.SECURE_PROTOCOL);
        sb.append(clusterId);
        sb.append(vaultDomain);
        return sb;
    }
}
