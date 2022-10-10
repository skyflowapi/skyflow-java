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
import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SignedDataTokensTest {

    private final String VALID_CREDENTIALS_FILE_PATH = "./credentials.json";

    @Test
    public void testEmptyFilePath() {
        Configuration.setLogLevel(LogLevel.DEBUG);
        SignedDataTokens token = new SignedDataTokens.SignedDataTokensBuilder()
                .setCredentials(new File(""))
                .build();
        try {
            token.getSignedDataTokens();
        } catch (SkyflowException exception) {
            assertEquals(exception.getMessage(), ErrorCode.EmptyFilePath.getDescription());
        }
    }

    @Test
    public void testNullFile() {
        Configuration.setLogLevel(LogLevel.DEBUG);
        SignedDataTokens token = new SignedDataTokens.SignedDataTokensBuilder()
                .setCredentials((File) null)
                .build();
        try {
            token.getSignedDataTokens();

        } catch (SkyflowException exception) {
            assertEquals(ErrorCode.EmptyJSONString.getDescription(), exception.getMessage());
        }
    }

    @Test
    public void testInvalidFileContent() {
        SignedDataTokens token = new SignedDataTokens.SignedDataTokensBuilder()
                .setCredentials(new File("./src/test/resources/invalidCredentials.json"))
                .build();
        try {
            token.getSignedDataTokens();
        } catch (SkyflowException exception) {
            assertEquals(exception.getMessage(), ErrorCode.InvalidClientID.getDescription());
        }
    }

    @Test
    public void testFileNotFound() {
        String fileNotFoundPath = "./src/test/resources/nofile.json";
        SignedDataTokens token = new SignedDataTokens.SignedDataTokensBuilder()
                .setCredentials(new File(fileNotFoundPath))
                .build();
        try {
            token.getSignedDataTokens();
        } catch (SkyflowException exception) {
            assertEquals(exception.getMessage(),
                    Helpers.parameterizedString(ErrorCode.InvalidCredentialsPath.getDescription(), fileNotFoundPath));
        }
    }

    @Test
    public void testFiledIsNotJsonFile() {
        String notJsonFilePath = "./src/test/resources/notJson.txt";
        SignedDataTokens token = new SignedDataTokens.SignedDataTokensBuilder()
                .setCredentials(new File(notJsonFilePath))
                .build();
        try {
            token.getSignedDataTokens();
        } catch (SkyflowException exception) {
            assertEquals(exception.getMessage(),
                    Helpers.parameterizedString(ErrorCode.InvalidJsonFormat.getDescription(), notJsonFilePath));
        }
    }

    @Test
    public void testEmptyString() {
        Configuration.setLogLevel(LogLevel.DEBUG);
        SignedDataTokens token = new SignedDataTokens.SignedDataTokensBuilder()
                .setCredentials("")
                .build();
        try {
            token.getSignedDataTokens();
        } catch (SkyflowException exception) {
            assertEquals(exception.getMessage(), ErrorCode.EmptyFilePath.getDescription());
        }
    }

    @Test
    public void testNullString() {
        Configuration.setLogLevel(LogLevel.DEBUG);
        SignedDataTokens token = new SignedDataTokens.SignedDataTokensBuilder()
                .setCredentials((String) null)
                .build();
        try {
            token.getSignedDataTokens();

        } catch (SkyflowException exception) {
            assertEquals(ErrorCode.EmptyJSONString.getDescription(), exception.getMessage());
        }
    }

    @Test
    public void testInvalidString() {
        String creds = "key:'not_a_json'";
        Configuration.setLogLevel(LogLevel.DEBUG);
        SignedDataTokens token = new SignedDataTokens.SignedDataTokensBuilder()
                .setCredentials(creds)
                .build();
        try {
            token.getSignedDataTokens();
        } catch (SkyflowException exception) {
            assertEquals(ErrorCode.InvalidJSONStringFormat.getDescription(), exception.getMessage());
        }
    }

    @Test
    public void testInvalidKeyId() {
        String creds = "{\"clientID\":\"test_client_ID\"}";
        Configuration.setLogLevel(LogLevel.DEBUG);
        SignedDataTokens token = new SignedDataTokens.SignedDataTokensBuilder()
                .setCredentials(creds)
                .build();
        try {
            token.getSignedDataTokens();
        } catch (SkyflowException exception) {
            assertEquals(ErrorCode.InvalidKeyID.getDescription(), exception.getMessage());
        }
    }

    @Test
    public void testNoDataTokenProvided() {
        String creds = System.getProperty("TEST_CREDENTIALS_FILE_STRING");
        SignedDataTokens token = new SignedDataTokens.SignedDataTokensBuilder()
                .setCredentials(creds)
                .setDataTokens(new String[] {}).build();
        try {
            assertTrue(token.getSignedDataTokens().isEmpty());

        } catch (SkyflowException exception) {
            System.out.println(exception);
        }
    }

    @Test
    public void testValidSignedToken() {
        String creds = System.getProperty("TEST_DATA_CREDENTIALS_FILE");
        SignedDataTokens token = new SignedDataTokens.SignedDataTokensBuilder()
                .setCredentials(creds)
                .setDataTokens(new String[] { "5230-0316-0674-5728" }).build();
        try {
            List<SignedDataTokenResponse> signedToken = token.getSignedDataTokens();
            Assert.assertNotNull(signedToken);

        } catch (SkyflowException exception) {
            Assert.assertNull(exception);
        }
    }

}
