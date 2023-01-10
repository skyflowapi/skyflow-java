/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.common.utils;

import com.skyflow.Configuration;
import com.skyflow.entities.LogLevel;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import static org.junit.Assert.assertEquals;
import java.security.PrivateKey;
import org.json.simple.JSONObject;
import org.junit.Test;


public class HelpersTest {

    @Test
    public void testMessageWithRequestID(){
        String message = Helpers.appendRequestId("message", "abc");
        String expectedMessage = "message" + " - requestId: " + "abc";
        assertEquals(message,expectedMessage);
    }

    @Test
    public void testFormatJsonToFormEncodedString(){
        Configuration.setLogLevel(LogLevel.DEBUG);
        JSONObject testJson = new JSONObject();
        testJson.put("key1","value1");
        JSONObject nestedObj = new JSONObject();
        nestedObj.put("key2","value2");
        testJson.put("nest",nestedObj);

        String testResponse = Helpers.formatJsonToFormEncodedString(testJson);
        assert testResponse.contains("key1=value1");
        assert testResponse.contains("nest[key2]=value2");
    }

    @Test
    public void testFormatJsonToMultiPartFormDataString(){
        JSONObject testJson = new JSONObject();
        testJson.put("key1","value1");
        JSONObject nestedObj = new JSONObject();
        nestedObj.put("key2","value2");
        testJson.put("nest",nestedObj);
        String testBoundary = "123";
        String testResponse = Helpers.formatJsonToMultiPartFormDataString(testJson,testBoundary);
        assert testResponse.contains("--"+testBoundary);
        assert testResponse.contains("--"+testBoundary+"--");
        assert testResponse.contains("Content-Disposition: form-data; name=\"key1\"");
        assert testResponse.contains("value1");
        assert testResponse.contains("Content-Disposition: form-data; name=\"nest[key2]\"");
        assert testResponse.contains("value2");
    }

    @Test
    public void testInvalidPrivateKey(){
        String pemKey = "abc";

        try{
            PrivateKey key = Helpers.getPrivateKeyFromPem(pemKey);
        }catch (SkyflowException exception) {
            assertEquals(exception.getMessage(), ErrorCode.UnableToRetrieveRSA.getDescription());
        }
    }
    @Test
    public void testInvalidKeySpec(){
        byte[] pkcs8Bytes = {};
        try{
             Helpers.parsePkcs8PrivateKey(pkcs8Bytes);
        }catch (SkyflowException exception) {
            assertEquals(exception.getMessage(), ErrorCode.InvalidKeySpec.getDescription());
        }
    }
}
