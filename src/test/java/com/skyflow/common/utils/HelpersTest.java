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
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.security.PrivateKey;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;


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

        assertEquals("skyflow-java@"+ Constants.SDK_VERSION, metrics.get("sdk_name_version"));
        assertNotNull(metrics.get("sdk_client_device_model"));

        assertNotNull(metrics.get("sdk_client_os_details"));
        assertNotNull(metrics.get("sdk_runtime_details"));
        assertNotNull(metrics.get("sdk_client_device_model"));

        String runtimeDetails = (String) metrics.get("sdk_runtime_details");
        assertEquals(true, runtimeDetails.startsWith("Java@"));

    }
    @Test
    public void testGetMetricsWithException() {
        // Arrange
        String expectedSdkVersion = Constants.SDK_VERSION;
        String expectedDeviceModel = "";
        String expectedOsDetails = "";
        String expectedJavaVersion = "";

        // Mocking the System.getProperty() method to throw an exception
        System.setProperty("os.name", "");
        System.setProperty("os.version", "");
        System.setProperty("java.version", "");

        // Act
        JSONObject metrics = Helpers.getMetrics();
        assertEquals(true, metrics.containsKey("sdk_name_version"));
        assertEquals(true, metrics.containsKey("sdk_client_device_model"));
        assertEquals(true, metrics.containsKey("sdk_client_os_details"));
        assertEquals(true, metrics.containsKey("sdk_runtime_details"));
        // Assert
        assertEquals("skyflow-java@" + expectedSdkVersion, metrics.get("sdk_name_version"));
        assertEquals(expectedDeviceModel, metrics.get("sdk_client_device_model"));
        assertEquals(expectedOsDetails, metrics.get("sdk_client_os_details"));
        assertEquals("Java@" + expectedJavaVersion, metrics.get("sdk_runtime_details"));
    }
}

