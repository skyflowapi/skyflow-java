/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.common.utils;

import com.skyflow.Configuration;
import com.skyflow.entities.LogLevel;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
    @Test
    public void testGetMetrics() {
        JSONObject metrics = Helpers.getMetrics();
        assertEquals(true, metrics.containsKey("sdk_name_version"));
        assertEquals(true, metrics.containsKey("sdk_client_device_model"));
        assertEquals(true, metrics.containsKey("sdk_client_os_details"));
        assertEquals(true, metrics.containsKey("sdk_runtime_details"));

        // Check the values of each key
        assertEquals("skyflow-java@1.8.3-beta.1", metrics.get("sdk_name_version"));

        // Note: Since the system properties may vary on different environments,
        // we can only perform basic validation here.

        // Device model should not be null or empty
        assertNotNull(metrics.get("sdk_client_device_model"));

        // OS details should not be null or empty
        assertNotNull(metrics.get("sdk_client_os_details"));

        // Runtime details should start with "Java@" and have a version number
        String runtimeDetails = (String) metrics.get("sdk_runtime_details");
        assertEquals(true, runtimeDetails.startsWith("Java@"));
        assertEquals(true, runtimeDetails.contains("."));

        // Print the metrics for debugging
        System.out.println(metrics);

    }

}
