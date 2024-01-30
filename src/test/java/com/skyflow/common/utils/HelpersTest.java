/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.common.utils;

import com.skyflow.Configuration;
import com.skyflow.entities.GetOptions;
import com.skyflow.entities.GetRecordInput;
import com.skyflow.entities.LogLevel;
import com.skyflow.entities.RedactionType;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;

import static com.skyflow.common.utils.Helpers.constructGetRequestURLParams;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.security.PrivateKey;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.TokenUtils", "com.skyflow.common.utils.HttpUtility"})
public class HelpersTest {

    private static String tableName = null;
    private static String columnName = null;
    private static String reqId = null;
    private static String[] columnValue = new String[1];
    private static String[] ids =  new String[1];
    private static String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";

    @BeforeClass
    public static void setup() throws SkyflowException {
        PowerMockito.mockStatic(TokenUtils.class);
        PowerMockito.when(TokenUtils.isTokenValid("valid_token")).thenReturn(true);
        PowerMockito.when(TokenUtils.isTokenValid("not_a_valid_token")).thenReturn(false);

        PowerMockito.mockStatic(HttpUtility.class);
        PowerMockito.when(HttpUtility.getRequestID()).thenReturn("abc");
        reqId = HttpUtility.getRequestID();
        tableName = "account_details";
        columnName = "card_number";
        columnValue[0] = "123451234554321";
        ids[0] = "123451234554321";
    }

    @Test
    public void testMessageWithRequestID(){
        String message = Helpers.appendRequestId("message", reqId);
        String expectedMessage = "message" + " - requestId: " + "abc";
        assertEquals(expectedMessage, message);
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
    public void constructGetRequestURLParamsColumnValueTest(){
        GetRecordInput recordInput = new GetRecordInput();

        recordInput.setTable(tableName);
        recordInput.setColumnValues(columnValue);
        recordInput.setColumnName(columnName);
        recordInput.setRedaction(RedactionType.PLAIN_TEXT);
        StringBuilder paramsList = constructGetRequestURLParams(recordInput, new GetOptions(false));

        Assert.assertTrue(paramsList.toString().contains("&"));

        Assert.assertTrue(paramsList.toString().contains("column_name="+ columnName));
        Assert.assertTrue(paramsList.toString().contains("column_values="+ columnValue[0]));
        Assert.assertTrue(paramsList.toString().contains("redaction="+ RedactionType.PLAIN_TEXT.toString()));
    }
    @Test
    public void constructGetRequestURLParamsIdTest(){
        GetRecordInput recordInput = new GetRecordInput();

        recordInput.setTable(tableName);
        recordInput.setIds(ids);
        recordInput.setRedaction(RedactionType.PLAIN_TEXT);
        StringBuilder paramsList =   constructGetRequestURLParams(recordInput, new GetOptions(false));
        Assert.assertTrue(paramsList.toString().contains("&"));

        Assert.assertTrue(paramsList.toString().contains("skyflow_ids="+ ids[0]));
        Assert.assertTrue(paramsList.toString().contains("redaction="+"PLAIN_TEXT"));

    }
    @Test
    public void constructGetRequestURLParamsIdTokenTrueTest(){
        GetRecordInput recordInput = new GetRecordInput();

        recordInput.setTable(tableName);
        recordInput.setIds(ids);
        StringBuilder paramsList =   constructGetRequestURLParams(recordInput, new GetOptions(true));

        Assert.assertTrue(paramsList.toString().contains("&"));
        Assert.assertFalse(paramsList.toString().contains("redaction=PLAIN_TEXT"));

        Assert.assertTrue(paramsList.toString().contains("skyflow_ids="+ ids[0]));
        Assert.assertTrue(paramsList.toString().contains("tokenization="+"true"));

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

