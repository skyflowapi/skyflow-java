package com.skyflow.v2.serviceaccount.util;

import com.skyflow.v2.errors.ErrorCode;
import com.skyflow.v2.errors.ErrorMessage;
import com.skyflow.v2.errors.SkyflowException;
import com.skyflow.v2.utils.Utils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;

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
    public void testEmptyCredentialsFilePath() {
        try {
            File file = new File("");
            BearerToken bearerToken = BearerToken.builder().setCredentials(file).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.FileNotFound.getMessage(), ""), e.getMessage()
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
                    Utils.parameterizedString(ErrorMessage.FileNotFound.getMessage(), invalidFilePath),
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
                    Utils.parameterizedString(ErrorMessage.FileInvalidJson.getMessage(), invalidJsonFilePath),
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
        String credentialsString = "{\"privateKey\": \"-----BEGIN PRIVATE KEY-----\\ncHJpdmF0ZV9rZXlfdmFsdWU=\\n-----END PRIVATE KEY-----\", \"clientID\": \"client_id_value\", \"keyID\": \"key_id_value\", \"tokenURI\": \"invalid_token_uri\"}";
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
}
