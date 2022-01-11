package com.skyflow.serviceaccount.util;

import com.skyflow.common.utils.Helpers;
import com.skyflow.entities.ResponseToken;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class TokenTest {
    // replace the path, when running local, do not commit
    private final String VALID_CREDENTIALS_FILE_PATH = "./credentials.json";

    @Test
    public void testInvalidFilePath() {
        try {
            Token.generateBearerToken("");
        } catch (SkyflowException exception) {
            assertEquals(exception.getMessage(), ErrorCode.EmptyFilePath.getDescription());
        }

    }

    @Test
    public void testNullFilePath() {
        try {
            Token.generateBearerToken("");
        } catch (SkyflowException exception) {
            assertEquals(exception.getMessage(), ErrorCode.EmptyFilePath.getDescription());
        }
    }

    @Test
    public void testInvalidFileContent() {
        try {
            Token.generateBearerToken(Paths.get("./src/test/resources/invalidCredentials.json").toString());
        } catch (SkyflowException exception) {
            assertEquals(exception.getMessage(), ErrorCode.InvalidClientID.getDescription());
        }

    }

    @Test
    public void testFileNotFoundPath() {
        String fileNotFoundPath = "./src/test/resources/nofile.json";
        try {
            Token.generateBearerToken(Paths.get(fileNotFoundPath).toString());
        } catch (SkyflowException exception) {
            assertEquals(exception.getMessage(), Helpers.parameterizedString(ErrorCode.InvalidCredentialsPath.getDescription(), fileNotFoundPath));
        }
    }

    @Test
    public void testFiledNotJsonFile() {
        String notJsonFilePath = "./src/test/resources/notJson.txt";
        try {
            Token.generateBearerToken(Paths.get(notJsonFilePath).toString());
        } catch (SkyflowException exception) {
            assertEquals(exception.getMessage(), Helpers.parameterizedString(ErrorCode.InvalidJsonFormat.getDescription(), notJsonFilePath));
        }
    }

    @Test
    public void testCallingDeprecatedMethod() {
        try {
            ResponseToken token = Token.GenerateToken(VALID_CREDENTIALS_FILE_PATH);
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
            ResponseToken token = Token.generateBearerToken(VALID_CREDENTIALS_FILE_PATH);
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
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(new FileReader(VALID_CREDENTIALS_FILE_PATH));
            JSONObject saCreds = (JSONObject) obj;

            ResponseToken token = Token.generateBearerTokenFromCreds(saCreds.toJSONString());
            Assert.assertNotNull(token.getAccessToken());
            Assert.assertEquals("Bearer", token.getTokenType());
        } catch (SkyflowException | IOException | ParseException skyflowException) {
            Assert.assertNull(skyflowException);
            skyflowException.printStackTrace();
        }
    }

    @Test
    public void testEmptyString() {
        try {
            ResponseToken token = Token.generateBearerTokenFromCreds("");
        } catch (SkyflowException exception) {
            assertEquals(ErrorCode.EmptyJSONString.getDescription(), exception.getMessage());
        }
    }

    @Test
    public void testNullString() {
        try {
            ResponseToken token = Token.generateBearerTokenFromCreds(null);
        } catch (SkyflowException exception) {
            assertEquals(ErrorCode.EmptyJSONString.getDescription(), exception.getMessage());
        }
    }

    @Test
    public void testInvalidString() {
        String creds = "key:'not_a_json'";
        try {
            ResponseToken token = Token.generateBearerTokenFromCreds(creds);
        } catch (SkyflowException exception) {
            assertEquals(ErrorCode.InvalidJSONStringFormat.getDescription(), exception.getMessage());
        }
    }

    @Test
    public void testInvalidKeyId() {
        String creds = "{\"clientID\":\"test_client_ID\"}";
        try {
            ResponseToken token = Token.generateBearerTokenFromCreds(creds);
        } catch (SkyflowException exception) {
            assertEquals(ErrorCode.InvalidKeyID.getDescription(), exception.getMessage());
        }
    }

    @Test
    public void testInvalidTokenURI() {
        String creds = "{\"clientID\":\"test_client_ID\",\"keyID\":\"test_key_id\"}";
        try {
            ResponseToken token = Token.generateBearerTokenFromCreds(creds);
        } catch (SkyflowException exception) {
            assertEquals(ErrorCode.InvalidTokenURI.getDescription(), exception.getMessage());
        }
    }

}