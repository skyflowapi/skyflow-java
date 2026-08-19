package com.skyflow.serviceaccount.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.auth.rest.core.ApiClientException;
import com.skyflow.generated.auth.rest.types.V1GetAuthTokenResponse;
import com.skyflow.utils.BaseConstants;
import com.skyflow.utils.BaseUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class BearerTokenTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String invalidJsonFilePath = null;
    private static String credentialsFilePath = null;
    private static String invalidFilePath = null;
    private static String credentialsString = null;
    private static String context = null;
    private static ArrayList<String> roles = null;
    private static String role = null;

    @BeforeClass
    public static void setup() {
        credentialsFilePath = "./credentials.json";
        invalidJsonFilePath = "./src/test/resources/notJson.txt";
        invalidFilePath = "./src/test/credentials.json";
        credentialsString = "{\"key\":\"value\"}";
        context = "test_context";
        roles = new ArrayList<>();
        role = "test_role";
    }

    @Test
    public void testBearerTokenBuilderWithCredentialsFile() {
        try {
            roles.add(role);
            File file = new File(credentialsFilePath);
            BearerToken.builder().setCredentials(file).setCtx(context).setRoles(roles).build();
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testBearerTokenBuilderWithCredentialsString() {
        try {
            BearerToken.builder().setCredentials(credentialsString).setCtx(context).setRoles(roles).build();
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testBearerTokenBuilderWithMapContext() {
        try {
            Map<String, Object> ctxMap = new HashMap<>();
            ctxMap.put("role", "admin");
            ctxMap.put("department", "finance");
            ctxMap.put("user_id", "user_12345");
            File file = new File(credentialsFilePath);
            BearerToken.builder().setCredentials(file).setCtx(ctxMap).build();
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testBearerTokenBuilderWithMapContextFromString() {
        try {
            Map<String, Object> ctxMap = new HashMap<>();
            ctxMap.put("role", "analyst");
            ctxMap.put("level", 3);
            BearerToken.builder().setCredentials(credentialsString).setCtx(ctxMap).build();
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testEmptyCredentialsFilePath() {
        try {
            File file = new File("");
            BearerToken bearerToken = BearerToken.builder().setCredentials(file).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    BaseUtils.parameterizedString(ErrorMessage.FileNotFound.getMessage(), ""), e.getMessage()
            );
        }
    }

    @Test
    public void testInvalidFilePath() {
        try {
            File file = new File(invalidFilePath);
            BearerToken bearerToken = BearerToken.builder().setCredentials(file).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    BaseUtils.parameterizedString(ErrorMessage.FileNotFound.getMessage(), invalidFilePath),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testInvalidCredentialsFile() {
        try {
            File file = new File(invalidJsonFilePath);
            BearerToken bearerToken = BearerToken.builder().setCredentials(file).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    BaseUtils.parameterizedString(ErrorMessage.FileInvalidJson.getMessage(), invalidJsonFilePath),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyCredentialsString() {
        try {
            BearerToken bearerToken = BearerToken.builder().setCredentials("").build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.InvalidCredentials.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInvalidCredentialsString() {
        try {
            BearerToken bearerToken = BearerToken.builder().setCredentials(invalidFilePath).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.CredentialsStringInvalidJson.getMessage(), e.getMessage());
        }
    }


    @Test
    public void testNoPrivateKeyInCredentialsForCredentials() {
        String filePath = "./src/test/resources/noPrivateKeyCredentials.json";
        File file = new File(filePath);
        try {
            BearerToken bearerToken = BearerToken.builder().setCredentials(file).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.MissingPrivateKey.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNoClientIDInCredentialsForCredentials() {
        String filePath = "./src/test/resources/noClientIDCredentials.json";
        File file = new File(filePath);
        try {
            BearerToken bearerToken = BearerToken.builder().setCredentials(file).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.MissingClientId.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNoKeyIDInCredentialsForCredentials() {
        String filePath = "./src/test/resources/noKeyIDCredentials.json";
        File file = new File(filePath);
        try {
            BearerToken bearerToken = BearerToken.builder().setCredentials(file).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.MissingKeyId.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNoTokenURIInCredentialsForCredentials() {
        String filePath = "./src/test/resources/noTokenURICredentials.json";
        File file = new File(filePath);
        try {
            BearerToken bearerToken = BearerToken.builder().setCredentials(file).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.MissingTokenUri.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInvalidPrivateKeyInCredentialsForCredentials() {
        String filePath = "./src/test/resources/invalidPrivateKeyCredentials.json";
        File file = new File(filePath);
        try {
            BearerToken bearerToken = BearerToken.builder().setCredentials(file).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.JwtInvalidFormat.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInvalidKeySpecInCredentialsForCredentials() {
        String credentialsString = "{\"privateKey\": \"-----BEGIN PRIVATE KEY-----\\ncHJpdmF0ZV9rZXlfdmFsdWU=\\n-----END PRIVATE KEY-----\", \"clientID\": \"client_id_value\", \"keyID\": \"key_id_value\", \"tokenURI\": \"invalid_token_uri\"}"; // gitleaks:allow
        try {
            BearerToken bearerToken = BearerToken.builder().setCredentials(credentialsString).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.InvalidKeySpec.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInvalidTokenURIInCredentialsForCredentials() throws SkyflowException {
        String filePath = "./src/test/resources/invalidTokenURICredentials.json";
        File file = new File(filePath);
        try {
            BearerToken bearerToken = BearerToken.builder().setCredentials(file).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.InvalidTokenUri.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testBearerTokenWithNewFormCredentialKeys() {
        try {
            // Fake key — fails at RSA parsing, not at field lookup, confirming new-form keys were accepted
            String credentialsString = "{\"privateKey\": \"-----BEGIN PRIVATE KEY-----\\ncHJpdmF0ZV9rZXlfdmFsdWU=\\n-----END PRIVATE KEY-----\", " // gitleaks:allow
                    + "\"clientId\": \"client_id_value\", \"keyId\": \"key_id_value\", \"tokenUri\": \"invalid_token_uri\"}";
            BearerToken bearerToken = BearerToken.builder().setCredentials(credentialsString).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            // InvalidKeySpec confirms all credential fields were resolved — failure is at RSA parsing, not field lookup
            Assert.assertEquals(
                    BaseUtils.parameterizedString(ErrorMessage.InvalidKeySpec.getMessage(), BaseConstants.SDK_PREFIX),
                    e.getMessage());
        }
    }

    /**
     * Generates a real, valid PKCS#8-encoded RSA private key PEM string, using the same
     * header/footer BaseUtils.getPrivateKeyFromPem expects, so tests can exercise JWT
     * signing (getSignedToken()) instead of always failing at key parsing.
     */
    private static String generateValidPkcs8PrivateKeyPem() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        String base64EncodedKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        return BaseConstants.PKCS8_PRIVATE_HEADER + "\n" + base64EncodedKey + "\n" + BaseConstants.PKCS8_PRIVATE_FOOTER;
    }

    @Test
    public void testGetSignedTokenAndScopeUsingRolesExecuteWithValidKeyRolesAndContext() {
        try {
            String privateKeyPem = generateValidPkcs8PrivateKeyPem();
            ArrayList<String> testRoles = new ArrayList<>();
            testRoles.add("test_role_one");
            testRoles.add("test_role_two");
            Map<String, Object> ctxMap = new HashMap<>();
            ctxMap.put("role", "admin");
            ctxMap.put("department", "finance");

            JsonObject credentials = new JsonObject();
            credentials.addProperty("privateKey", privateKeyPem);
            credentials.addProperty("clientId", "client_id_value");
            credentials.addProperty("keyId", "key_id_value");
            // Syntactically valid but unreachable, so failure surfaces at the network call,
            // proving getSignedToken() (with the ctx claim) and getScopeUsingRoles() (roles != null)
            // both executed successfully first.
            credentials.addProperty("tokenUri", "https://localhost:1");
            String credentialsString = new Gson().toJson(credentials);

            BearerToken bearerToken = BearerToken.builder()
                    .setCredentials(credentialsString)
                    .setCtx(ctxMap)
                    .setRoles(testRoles)
                    .build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            // Reaching this network-layer failure (rather than a SkyflowException raised
            // directly from key parsing) confirms getSignedToken() and getScopeUsingRoles()
            // both ran successfully. The SDK never leaks a raw ApiClientException — this one is
            // wrapped, with the original ApiClientException/IOException chain preserved as the cause.
            Assert.assertTrue(e.getCause() instanceof ApiClientException);
            Assert.assertTrue(e.getCause().getCause() instanceof IOException);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN + ": " + e);
        }
    }

    @Test
    public void testMalformedPrivateKeyTypeThrowsSkyflowException() {
        // privateKey present but a JSON object instead of a string — used to surface as a raw
        // UnsupportedOperationException from JsonElement.getAsString(); now wrapped.
        JsonObject credentials = new JsonObject();
        JsonObject notAString = new JsonObject();
        notAString.addProperty("nested", "value");
        credentials.add("privateKey", notAString);
        credentials.addProperty("clientId", "client_id_value");
        credentials.addProperty("keyId", "key_id_value");
        credentials.addProperty("tokenUri", "https://localhost:1");
        String credentialsString = new Gson().toJson(credentials);

        try {
            BearerToken bearerToken = BearerToken.builder().setCredentials(credentialsString).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            // httpCode 0 indicates a wrapped local failure (SkyflowException(Throwable) ctor),
            // distinct from a 400 validation error.
            Assert.assertEquals(0, e.getHttpCode());
            Assert.assertTrue(e.getCause() instanceof UnsupportedOperationException);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN + ": " + e);
        }
    }

    @Test
    public void testExtractAccessTokenMissingThrowsSkyflowException() throws Exception {
        V1GetAuthTokenResponse response = V1GetAuthTokenResponse.builder().build();
        Method method = BearerToken.class.getDeclaredMethod("extractAccessToken", V1GetAuthTokenResponse.class);
        method.setAccessible(true);
        try {
            method.invoke(null, response);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (InvocationTargetException e) {
            Assert.assertTrue(e.getCause() instanceof SkyflowException);
            SkyflowException skyflowException = (SkyflowException) e.getCause();
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), skyflowException.getHttpCode());
            Assert.assertEquals(ErrorMessage.MissingAccessToken.getMessage(), skyflowException.getMessage());
        }
    }

    @Test
    public void testExtractAccessTokenPresentReturnsToken() throws Exception {
        V1GetAuthTokenResponse response = V1GetAuthTokenResponse.builder().accessToken("test-access-token").build();
        Method method = BearerToken.class.getDeclaredMethod("extractAccessToken", V1GetAuthTokenResponse.class);
        method.setAccessible(true);
        Object result = method.invoke(null, response);
        Assert.assertEquals("test-access-token", result);
    }
}
