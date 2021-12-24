//package com.skyflow.vault;
//
//import com.skyflow.common.utils.HttpUtility;
//import com.skyflow.common.utils.TokenUtils;
//import com.skyflow.entities.RequestMethod;
//import com.skyflow.entities.SkyflowConfiguration;
//import com.skyflow.entities.TokenProvider;
//import com.skyflow.errors.ErrorCode;
//import com.skyflow.errors.SkyflowException;
//import org.json.simple.JSONObject;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.ArgumentMatchers;
//import org.powermock.api.mockito.PowerMockito;
//import org.powermock.core.classloader.annotations.PrepareForTest;
//import org.powermock.modules.junit4.PowerMockRunner;
//
//import java.io.IOException;
//
//import static org.mockito.ArgumentMatchers.anyString;
//
//
//class TestTokenProvider implements TokenProvider {
//    @Override
//    public String getBearerToken() throws Exception {
//        return "test_auth_token";
//    }
//}
//
//
//@RunWith(PowerMockRunner.class)
//@PrepareForTest(fullyQualifiedNames = "com.skyflow.common.utils.TokenUtils")
//public class InvokeConnectionTest {
//    private JSONObject testConfig;
//    private static Skyflow skyflowClient;
//
//    @BeforeClass
//    public static void init() throws Exception {
//        SkyflowConfiguration config = new SkyflowConfiguration(new TestTokenProvider());
//        skyflowClient = Skyflow.init(config);
//
//        PowerMockito.mockStatic(TokenUtils.class);
//        PowerMockito.when(TokenUtils.isTokenValid("test_auth_token")).thenReturn(true);
//    }
//
//    @Before
//    public void setup() {
//        testConfig = new JSONObject();
//        testConfig.put("connectionURL", "https://testgatewayurl.com/{card_number}/pay");
//        testConfig.put("methodName", RequestMethod.POST);
//
//        JSONObject pathParamsJson = new JSONObject();
//        pathParamsJson.put("card_number", "1234");
//        testConfig.put("pathParams", pathParamsJson);
//
//        JSONObject queryParamsJson = new JSONObject();
//        queryParamsJson.put("id", "1");
//        testConfig.put("queryParams", queryParamsJson);
//
//        JSONObject requestHeadersJson = new JSONObject();
//        requestHeadersJson.put("content-type", "application/json");
//        testConfig.put("requestHeader", requestHeadersJson);
//
//        JSONObject requestBodyJson = new JSONObject();
//        requestBodyJson.put("userName", "testUser");
//        requestBodyJson.put("itemName", "item1");
//        testConfig.put("requestBody", requestBodyJson);
//
//    }
//
//    @Test
//    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
//    public void testInvokeConnectionValidInput() {
//        try {
//            PowerMockito.mockStatic(HttpUtility.class);
//            String mockResponse = "{\"processingTimeinMs\":\"116\"}";
//            PowerMockito.when(HttpUtility.sendRequest(anyString(), anyString(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);
//            JSONObject gatewayResponse = skyflowClient.invokeConnection(testConfig);
//
//            Assert.assertNotNull(gatewayResponse);
//            Assert.assertEquals(gatewayResponse.toJSONString(), mockResponse);
//        } catch (SkyflowException exception) {
//            Assert.assertNull(exception);
//        } catch (IOException exception) {
//            exception.printStackTrace();
//        }
//
//
//    }
//
//    @Test
//    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
//    public void testInvokeConnectionThrowsErrorResponse() {
//        String mockErrorResponse = "{\"error\":{\"code\":\"400\",\"message\":\"missing required field\"}}";
//        try {
//            PowerMockito.mockStatic(HttpUtility.class);
//            PowerMockito.when(HttpUtility.sendRequest(anyString(), anyString(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenThrow(new SkyflowException(400, mockErrorResponse));
//            JSONObject gatewayResponse = skyflowClient.invokeConnection(testConfig);
//            Assert.assertNull(gatewayResponse);
//        } catch (SkyflowException exception) {
//            Assert.assertEquals(exception.getCode(), 400);
//            Assert.assertEquals(exception.getMessage(), mockErrorResponse);
//        } catch (IOException exception) {
//            exception.printStackTrace();
//        }
//    }
//
//
//    @Test
//    public void testInvokeConnectionInvalidConnectionURL() {
//        JSONObject testConnectionConfig = new JSONObject();
//        testConnectionConfig.put("connectionURL", "INVALID_CONNECTION_URL");
//        try {
//            skyflowClient.invokeConnection(testConnectionConfig);
//        } catch (SkyflowException exception) {
//            Assert.assertEquals(exception.getCode(), ErrorCode.InvalidConnectionURL.getCode());
//            Assert.assertEquals(exception.getMessage(), ErrorCode.InvalidConnectionURL.getDescription());
//        }
//
//    }
//
//    @Test
//    public void testInvokeConnectionMissingConnectionURL() {
//        JSONObject testConnectionConfig = new JSONObject();
//        try {
//            skyflowClient.invokeConnection(testConnectionConfig);
//        } catch (SkyflowException exception) {
//            Assert.assertEquals(exception.getCode(), ErrorCode.ConnectionURLMissing.getCode());
//            Assert.assertEquals(exception.getMessage(), ErrorCode.ConnectionURLMissing.getDescription());
//        }
//    }
//
//    @Test
//    public void testInvokeConnectionMissingMethodName() {
//        JSONObject testConnectionConfig = new JSONObject();
//        testConnectionConfig.put("connectionURL", "https://testgatewayurl.com/{card_number}/pay");
//        try {
//            skyflowClient.invokeConnection(testConnectionConfig);
//        } catch (SkyflowException exception) {
//            Assert.assertEquals(exception.getCode(), ErrorCode.MethodNameMissing.getCode());
//            Assert.assertEquals(exception.getMessage(), ErrorCode.MethodNameMissing.getDescription());
//        }
//    }
//
//    @Test
//    public void testInvokeConnectionInvalidMethodName() {
//        JSONObject testConnectionConfig = new JSONObject();
//        testConnectionConfig.put("connectionURL", "https://testgatewayurl.com/{card_number}/pay");
//        testConnectionConfig.put("methodName", "INVALID_METHOD_NAME");
//        try {
//            Skyflow.init(new SkyflowConfiguration(new TestTokenProvider())).invokeConnection(testConnectionConfig);
//        } catch (SkyflowException exception) {
//            Assert.assertEquals(exception.getCode(), ErrorCode.InvalidMethodName.getCode());
//            Assert.assertEquals(exception.getMessage(), ErrorCode.InvalidMethodName.getDescription());
//        }
//    }
//
//}
