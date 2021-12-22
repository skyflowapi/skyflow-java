package com.skyflow.serviceaccount.util;

import com.skyflow.common.utils.Helpers;
import com.skyflow.entities.ResponseToken;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class TokenTest {

    @Test
    public void testInvalidFilePath() {
        Exception exception = Assert.assertThrows(SkyflowException.class, () -> {
            Token.GenerateBearerToken("");
        });
        Assert.assertTrue(exception.getMessage().contains(ErrorCode.EmptyFilePath.getDescription()));

    }

    @Test
    public void testNullFilePath() {
        Exception exception = Assert.assertThrows(SkyflowException.class, () -> {
            Token.GenerateBearerToken(null);
        });
        Assert.assertTrue(exception.getMessage().contains(ErrorCode.EmptyFilePath.getDescription()));
    }

    @Test
    public void testInvalidFileContent() {
        Exception exception = Assert.assertThrows(SkyflowException.class, () -> {
            Token.GenerateBearerToken(Paths.get("./src/test/resources/invalidCredentials.json").toString());
        });
        Assert.assertTrue(exception.getMessage().contains(ErrorCode.InvalidClientID.getDescription()));
    }

    @Test
    public void testCallingDeprecatedMethod() {
        try {
            ResponseToken token = Token.GenerateToken(Paths.get(System.getProperty("TEST_CREDENTIALS_PATH")).toString());
            Assert.assertNotNull(token.getAccessToken());
            Assert.assertEquals("Bearer", token.getTokenType());
        } catch (SkyflowException skyflowException) {
            Assert.assertNull(skyflowException);
            skyflowException.printStackTrace();
        }
    }

    @Test
    public void testValidFileContent() {
        try {
            ResponseToken token = Token.GenerateBearerToken(Paths.get(System.getProperty("TEST_CREDENTIALS_PATH")).toString());
            Assert.assertNotNull(token.getAccessToken());
            Assert.assertEquals("Bearer", token.getTokenType());
        } catch (SkyflowException skyflowException) {
            Assert.assertNull(skyflowException);
            skyflowException.printStackTrace();
        }
    }

    @Test
    public void testValidString() {
        try {
            String creds = System.getProperty("TEST_CREDENTIALS");
            ResponseToken token = Token.GenerateBearerTokenFromCreds(creds);
            Assert.assertNotNull(token.getAccessToken());
            Assert.assertEquals("Bearer", token.getTokenType());
        } catch (SkyflowException skyflowException) {
            Assert.assertNull(skyflowException);
            skyflowException.printStackTrace();
        }
    }

    @Test
    public void testEmptyString() {
        try {
            ResponseToken token = Token.GenerateBearerTokenFromCreds("");
        } catch (SkyflowException exception) {
            assertEquals(ErrorCode.EmptyJSONString.getDescription(), exception.getMessage());
        }
    }

    @Test
    public void testNullString() {
        try {
            ResponseToken token = Token.GenerateBearerTokenFromCreds(null);
        } catch (SkyflowException exception) {
            assertEquals(ErrorCode.EmptyJSONString.getDescription(), exception.getMessage());
        }
    }

    @Test
    public void testInvalidString() {
        String creds = "key:'not_a_json'";
        try {
            ResponseToken token = Token.GenerateBearerTokenFromCreds(creds);
        } catch (SkyflowException exception) {
            assertEquals(Helpers.parameterizedString(ErrorCode.InvalidJSONStringFormat.getDescription(), creds), exception.getMessage());
        }
    }

    @Test
    public void testInvalidKeyId() {
        String creds = "{\"clientID\":\"test_client_ID\"}";
        try {
            ResponseToken token = Token.GenerateBearerTokenFromCreds(creds);
        } catch (SkyflowException exception) {
            assertEquals(ErrorCode.InvalidKeyID.getDescription(), exception.getMessage());
        }
    }

    @Test
    public void testInvalidTokenURI() {
        String creds = "{\"clientID\":\"test_client_ID\",\"keyID\":\"test_key_id\"}";
        try {
            ResponseToken token = Token.GenerateBearerTokenFromCreds(creds);
        } catch (SkyflowException exception) {
            assertEquals(ErrorCode.InvalidTokenURI.getDescription(), exception.getMessage());
        }
    }

}