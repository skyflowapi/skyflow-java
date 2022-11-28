/*
	Copyright (c) 2022 Skyflow, Inc.
*/
package com.skyflow.serviceaccount.util;

import com.skyflow.Configuration;
import com.skyflow.common.utils.Helpers;
import com.skyflow.entities.LogLevel;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;

import org.junit.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.io.File;
import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest
public class BearerTokenTest {

    @Test
    public void testEmptyFilePath() {
        Configuration.setLogLevel(LogLevel.DEBUG);
        BearerToken token = new BearerToken.BearerTokenBuilder()
                .setCredentials(new File(""))
                .build();
        try {
            token.getBearerToken();
        } catch (SkyflowException exception) {
            assertEquals(exception.getMessage(), ErrorCode.EmptyFilePath.getDescription());
        }
    }

    @Test
    public void testNullFile() {
        Configuration.setLogLevel(LogLevel.DEBUG);
        BearerToken token = new BearerToken.BearerTokenBuilder()
                .setCredentials((File) null)
                .build();
        try {
            token.getBearerToken();

        } catch (SkyflowException exception) {
            assertEquals(ErrorCode.EmptyJSONString.getDescription(), exception.getMessage());
        }
    }

    @Test
    public void testInvalidFileContent() {
        BearerToken token = new BearerToken.BearerTokenBuilder()
                .setCredentials(new File("./src/test/resources/invalidCredentials.json"))
                .build();
        try {
            token.getBearerToken();
        } catch (SkyflowException exception) {
            assertEquals(exception.getMessage(), ErrorCode.InvalidClientID.getDescription());
        }

    }

    @Test
    public void testFileNotFound() {
        String fileNotFoundPath = "./src/test/resources/nofile.json";
        BearerToken token = new BearerToken.BearerTokenBuilder()
                .setCredentials(new File(fileNotFoundPath))
                .build();
        try {
            token.getBearerToken();
        } catch (SkyflowException exception) {
            assertEquals(exception.getMessage(),
                    Helpers.parameterizedString(ErrorCode.InvalidCredentialsPath.getDescription(), fileNotFoundPath));
        }
    }

    @Test
    public void testFiledIsNotJsonFile() {
        String notJsonFilePath = "./src/test/resources/notJson.txt";
        BearerToken token = new BearerToken.BearerTokenBuilder()
                .setCredentials(new File(notJsonFilePath))
                .build();
        try {
            token.getBearerToken();
        } catch (SkyflowException exception) {
            assertEquals(exception.getMessage(),
                    Helpers.parameterizedString(ErrorCode.InvalidJsonFormat.getDescription(), notJsonFilePath));
        }
    }

    @Test
    public void testEmptyString() {
        Configuration.setLogLevel(LogLevel.DEBUG);
        BearerToken token = new BearerToken.BearerTokenBuilder()
                .setCredentials("")
                .build();
        try {
            token.getBearerToken();
        } catch (SkyflowException exception) {
            assertEquals(exception.getMessage(), ErrorCode.EmptyFilePath.getDescription());
        }
    }

    @Test
    public void testNullString() {
        Configuration.setLogLevel(LogLevel.DEBUG);
        BearerToken token = new BearerToken.BearerTokenBuilder()
                .setCredentials((String) null)
                .build();
        try {
            token.getBearerToken();

        } catch (SkyflowException exception) {
            assertEquals(ErrorCode.EmptyJSONString.getDescription(), exception.getMessage());
        }
    }

    @Test
    public void testInvalidString() {
        String creds = "key:'not_a_json'";
        Configuration.setLogLevel(LogLevel.DEBUG);
        BearerToken token = new BearerToken.BearerTokenBuilder()
                .setCredentials(creds)
                .build();
        try {
            token.getBearerToken();
        } catch (SkyflowException exception) {
            assertEquals(ErrorCode.InvalidJSONStringFormat.getDescription(), exception.getMessage());
        }
    }

    @Test
    public void testInvalidKeyId() {
        String creds = "{\"clientID\":\"test_client_ID\"}";
        Configuration.setLogLevel(LogLevel.DEBUG);
        BearerToken token = new BearerToken.BearerTokenBuilder()
                .setCredentials(creds)
                .build();
        try {
            token.getBearerToken();
        } catch (SkyflowException exception) {
            assertEquals(ErrorCode.InvalidKeyID.getDescription(), exception.getMessage());
        }
    }

    @Test
    public void testInvalidTokenURI() {
        String creds = "{\"clientID\":\"test_client_ID\",\"keyID\":\"test_key_id\"}";
        Configuration.setLogLevel(LogLevel.DEBUG);
        BearerToken token = new BearerToken.BearerTokenBuilder()
                .setCredentials(creds)
                .build();
        try {
            token.getBearerToken();
        } catch (SkyflowException exception) {
            assertEquals(ErrorCode.InvalidTokenURI.getDescription(), exception.getMessage());
        }
    }

    @Test
    public void testEmptyContext() {
        Configuration.setLogLevel(LogLevel.DEBUG);
        String creds = System.getProperty("TEST_CREDENTIALS_FILE_STRING_WITH_CONTEXT");
        BearerToken token = new BearerToken.BearerTokenBuilder()
                .setCredentials(creds)
                .setCtx("")
                .build();
        try {
            token.getBearerToken();
        } catch (SkyflowException exception) {
            assertEquals(ErrorCode.EmptyContext.getDescription(), exception.getMessage());
        }
    }

    @Test
    public void testEmptyRoleProvided() {
        Configuration.setLogLevel(LogLevel.DEBUG);
        String creds = System.getProperty("TEST_CREDENTIALS_FILE_STRING_WITH_CONTEXT");
        BearerToken token = new BearerToken.BearerTokenBuilder()
                .setCredentials(creds)
                .setCtx("abcd")
                .setRoles(new String[] { "" })
                .build();
        try {
            token.getBearerToken();
        } catch (SkyflowException exception) {
            assertEquals(ErrorCode.IncorrectRole.getDescription(), exception.getMessage());
        }
    }

}
