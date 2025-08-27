package com.skyflow.serviceaccount.util;

import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.BaseUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;

public class SignedDataTokensTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String invalidJsonFilePath = null;
    private static String credentialsFilePath = null;
    private static String invalidFilePath = null;
    private static String credentialsString = null;
    private static String context = null;
    private static ArrayList<String> dataTokens = null;
    private static String dataToken = null;
    private static Integer ttl = null;

    @BeforeClass
    public static void setup() {
        credentialsFilePath = "./credentials.json";
        invalidJsonFilePath = "./test/resources/notJson.txt";
        invalidFilePath = "./src/test/credentials.json";
        credentialsString = "{\"key\":\"value\"}";
        context = "test_context";
        dataTokens = new ArrayList<>();
        dataToken = "test_data_token";
        ttl = 60;
    }

    @Test
    public void testSignedDataTokensBuilderWithCredentialsFile() {
        try {
            File file = new File(credentialsFilePath);
            dataTokens.add(dataToken);
            SignedDataTokens.builder()
                    .setCredentials(file).setCtx(context).setDataTokens(dataTokens).setTimeToLive(ttl)
                    .build();
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testSignedDataTokensBuilderWithCredentialsString() {
        try {
            SignedDataTokens.builder()
                    .setCredentials(credentialsString).setCtx(context).setDataTokens(dataTokens).setTimeToLive(ttl)
                    .build();
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }

    }

    @Test
    public void testEmptyCredentialsFilePath() {
        try {
            File file = new File("");
            SignedDataTokens signedTokens = SignedDataTokens.builder().setCredentials(file).build();
            signedTokens.getSignedDataTokens();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(BaseUtils.parameterizedString(ErrorMessage.FileNotFound.getMessage(), ""), e.getMessage());
        }
    }

    @Test
    public void testInvalidFilePath() {
        try {
            File file = new File(invalidFilePath);
            SignedDataTokens signedTokens = SignedDataTokens.builder().setCredentials(file).build();
            signedTokens.getSignedDataTokens();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    BaseUtils.parameterizedString(ErrorMessage.FileNotFound.getMessage(), invalidFilePath),
                    e.getMessage());
        }
    }

    @Test
    public void testInvalidCredentialsFile() {
        try {
            File file = new File(invalidJsonFilePath);
            SignedDataTokens signedTokens = SignedDataTokens.builder().setCredentials(file).build();
            signedTokens.getSignedDataTokens();
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
            SignedDataTokens signedTokens = SignedDataTokens.builder().setCredentials("").build();
            signedTokens.getSignedDataTokens();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    BaseUtils.parameterizedString(ErrorMessage.InvalidCredentials.getMessage(), invalidJsonFilePath),
                    e.getMessage()
            );
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testInvalidCredentialsString() {
        try {
            SignedDataTokens signedTokens = SignedDataTokens.builder().setCredentials(invalidFilePath).build();
            signedTokens.getSignedDataTokens();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    BaseUtils.parameterizedString(ErrorMessage.CredentialsStringInvalidJson.getMessage(), invalidJsonFilePath),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testNoPrivateKeyInCredentials() {
        String filePath = "./test/resources/noPrivateKeyCredentials.json";
        File file = new File(filePath);
        try {
            SignedDataTokens signedTokens = SignedDataTokens.builder().setCredentials(file).build();
            signedTokens.getSignedDataTokens();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.MissingPrivateKey.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNoClientIDInCredentials() {
        String filePath = "./test/resources/noClientIDCredentials.json";
        File file = new File(filePath);
        try {
            SignedDataTokens signedTokens = SignedDataTokens.builder().setCredentials(file).build();
            signedTokens.getSignedDataTokens();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.MissingClientId.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNoKeyIDInCredentials() {
        String filePath = "./test/resources/noKeyIDCredentials.json";
        File file = new File(filePath);
        try {
            SignedDataTokens signedTokens = SignedDataTokens.builder().setCredentials(file).build();
            signedTokens.getSignedDataTokens();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.MissingKeyId.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInvalidPrivateKeyInCredentials() {
        String filePath = "./test/resources/invalidPrivateKeyCredentials.json";
        File file = new File(filePath);
        try {
            SignedDataTokens signedTokens = SignedDataTokens.builder().setCredentials(file).build();
            signedTokens.getSignedDataTokens();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.JwtInvalidFormat.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInvalidKeySpecInCredentials() {
        String credentialsString = "{\"privateKey\": \"-----BEGIN PRIVATE KEY-----\\ncHJpdmF0ZV9rZXlfdmFsdWU=\\n-----END PRIVATE KEY-----\", \"clientID\": \"client_id_value\", \"keyID\": \"key_id_value\", \"tokenURI\": \"invalid_token_uri\"}";
        try {
            SignedDataTokens signedTokens = SignedDataTokens.builder().setCredentials(credentialsString).build();
            signedTokens.getSignedDataTokens();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.InvalidKeySpec.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testSignedDataTokenResponse() {
        try {
            String signedToken = "test_signed_data_token";
            SignedDataTokenResponse response = new SignedDataTokenResponse(dataToken, signedToken);
            String responseString = "{\"token\":\"" + dataToken + "\"," +
                    "\"signedToken\":\"signed_token_" + signedToken + "\"}";
            Assert.assertEquals(responseString, response.toString());
            Assert.assertEquals(dataToken, response.getToken());
            Assert.assertEquals("signed_token_" + signedToken, response.getSignedToken());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
