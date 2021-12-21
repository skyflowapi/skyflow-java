package com.skyflow.serviceaccount.util;

import com.skyflow.common.utils.Helpers;
import com.skyflow.entities.ResponseToken;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

import java.nio.file.Paths;

public class TokenTest {

    @Test
    public void testInvalidFilePath() {
        Exception exception = Assert.assertThrows(SkyflowException.class, () -> {
            Token.GenerateBearerToken("");
        });
        String expectedMessage = "Unable to open credentials";
        Assert.assertTrue(exception.getMessage().contains(expectedMessage));
    }

    @Test
    public void testInvalidFileContent() {
        Exception exception = Assert.assertThrows(SkyflowException.class, () -> {
            Token.GenerateBearerToken(Paths.get("./src/test/resources/invalidCredentials.json").toString());
        });
        String expectedMessage = "Unable to read clientID";
        Assert.assertTrue(exception.getMessage().contains(expectedMessage));
    }

    @Test
    public void testValidFileContent() {
        try{
            ResponseToken token = Token.GenerateBearerToken(Paths.get(System.getProperty("TEST_CREDENTIALS_PATH")).toString());
            Assert.assertNotNull(token.getAccessToken());
            Assert.assertEquals("Bearer", token.getTokenType());
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
        }
    }
    @Test
    public void testValidString(){
        try{
            String creds = System.getProperty("TEST_CREDENTIALS");
            ResponseToken token = Token.GenerateBearerTokenFromCreds(creds);
            Assert.assertNotNull(token.getAccessToken());
            Assert.assertEquals("Bearer", token.getTokenType());
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
        }
    }
    @Test
    public void testEmptyString(){
        try{
            ResponseToken token = Token.GenerateBearerTokenFromCreds("");
        }catch (SkyflowException exception){
            assertEquals(ErrorCode.EmptyJSONString.getDescription(),exception.getMessage());
        }
    }

    @Test
    public void testInvalidString(){
        String creds = "key:'not_a_json'";
        try{
            ResponseToken token = Token.GenerateBearerTokenFromCreds(creds);
        }catch (SkyflowException exception){
            assertEquals(Helpers.parameterizedString(ErrorCode.InvalidJSONStringFormat.getDescription(),creds),exception.getMessage());
        }
    }

}