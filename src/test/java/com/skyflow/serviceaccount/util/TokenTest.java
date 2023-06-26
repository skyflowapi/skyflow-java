/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.serviceaccount.util;

import com.skyflow.Configuration;
import com.skyflow.common.utils.Helpers;
import com.skyflow.common.utils.HttpUtility;
import com.skyflow.common.utils.TokenUtils;
import com.skyflow.entities.LogLevel;
import com.skyflow.entities.ResponseToken;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
public class TokenTest {
    // replace the path, when running local, do not commit
    private final String VALID_CREDENTIALS_FILE_PATH = "./credentials.json";

    private static String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    @Test
    public void testInvalidFilePath() {
        try {
            Configuration.setLogLevel(LogLevel.DEBUG);
            Token.GenerateToken("");
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
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInvalidFileContent() {
        try {
            Token.generateBearerToken(Paths.get("./src/test/resources/invalidCredentials.json").toString());
        } catch (SkyflowException exception) {
            assertEquals(exception.getMessage(), ErrorCode.InvalidClientID.getDescription());
        }

    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testFileNotFoundPath() {
        String fileNotFoundPath = "./src/test/resources/nofile.json";
        try {
            Token.generateBearerToken(Paths.get(fileNotFoundPath).toString());
        } catch (SkyflowException exception) {
            assertEquals(exception.getMessage(), Helpers.parameterizedString(ErrorCode.InvalidCredentialsPath.getDescription(), fileNotFoundPath));
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testFiledNotJsonFile() {
        String notJsonFilePath = "./src/test/resources/notJson.txt";
        try {
            Token.generateBearerToken(Paths.get(notJsonFilePath).toString());
        } catch (SkyflowException exception) {
            assertEquals(exception.getMessage(), Helpers.parameterizedString(ErrorCode.InvalidJsonFormat.getDescription(), notJsonFilePath));
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testCallingDeprecatedMethod() {
        try {
            PowerMockito.mockStatic(TokenUtils.class);
            PowerMockito.when(TokenUtils.isTokenValid("a.b.c")).thenReturn(true);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"accessToken\":\"a.b.c\",\"tokenType\":\"Bearer\"}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);
            ResponseToken token = Token.GenerateToken(VALID_CREDENTIALS_FILE_PATH);
            Assert.assertNotNull(token.getAccessToken());
            Assert.assertEquals("Bearer", token.getTokenType());
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
            Assert.assertNull(skyflowException);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testValidFileContent() {
        try {
            PowerMockito.mockStatic(TokenUtils.class);
            PowerMockito.when(TokenUtils.isTokenValid("a.b.c")).thenReturn(true);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"accessToken\":\"a.b.c\",\"tokenType\":\"Bearer\"}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);
            ResponseToken token = Token.generateBearerToken(VALID_CREDENTIALS_FILE_PATH);
            Assert.assertNotNull(token.getAccessToken());
            Assert.assertEquals("Bearer", token.getTokenType());
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
            Assert.assertNull(skyflowException);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testValidString() {
        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(new FileReader(VALID_CREDENTIALS_FILE_PATH));
            JSONObject saCreds = (JSONObject) obj;

            PowerMockito.mockStatic(TokenUtils.class);
            PowerMockito.when(TokenUtils.isTokenValid("a.b.c")).thenReturn(true);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"accessToken\":\"a.b.c\",\"tokenType\":\"Bearer\"}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);

            ResponseToken token = Token.generateBearerTokenFromCreds(saCreds.toJSONString());
            Assert.assertNotNull(token.getAccessToken());
            Assert.assertEquals("Bearer", token.getTokenType());
        } catch (SkyflowException | IOException | ParseException skyflowException) {
            skyflowException.printStackTrace();
            Assert.assertNull(skyflowException);
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

    @Test
    public void testIsValidForEmptyToken() {
        String token = "";
        assertEquals(false, Token.isValid(token));
    }

    @Test
    public void testIsValidForInvalidToken() {
        String token = "invalidToken";
        assertEquals(false, Token.isValid(token));
    }

    @Test
    public void testIsExpiredForExpiredToken() {
        String token = System.getProperty("TEST_EXPIRED_TOKEN");
        assertEquals(true, Token.isExpired(token));
    }

}