package com.skyflow.vault.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyflow.Skyflow;
import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.enums.LogLevel;
import com.skyflow.enums.RequestMethod;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.HttpUtility;
import com.skyflow.vault.connection.InvokeConnectionRequest;
import com.skyflow.vault.connection.InvokeConnectionResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpUtility.class})
public class ConnectionControllerTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String connectionID = null;
    private static String connectionURL = null;
    private static ConnectionConfig connectionConfig = null;
    private static Skyflow skyflowClient = null;

    @BeforeClass
    public static void setup() {
        connectionID = "vault123";
        connectionURL = "https://test.connection.url";

        Credentials credentials = new Credentials();
        credentials.setToken("valid-token");

        connectionConfig = new ConnectionConfig();
        connectionConfig.setConnectionId(connectionID);
        connectionConfig.setConnectionUrl(connectionURL);
        connectionConfig.setCredentials(credentials);
    }

    @Before
    public void setupMocks() throws Exception {
        PowerMockito.mockStatic(HttpUtility.class);
        when(HttpUtility.getRequestID()).thenReturn("test-request-id");
    }

    @Test
    public void testInvalidRequestInInvokeConnectionMethod() {
        try {
            HashMap<String, String> requestBody = new HashMap<>();
            InvokeConnectionRequest connectionRequest = InvokeConnectionRequest.builder().requestBody(requestBody).build();
            Skyflow skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addConnectionConfig(connectionConfig).build();
            skyflowClient.connection().invoke(connectionRequest);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyRequestBody.getMessage(), e.getMessage());
        }
    }

    // New test cases for content-type handling changes

    @Test
    public void testInvokeConnectionWithStringRequestBody() {
        try {
            String xmlBody = "<xml><data>test</data></xml>";
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/xml");
            InvokeConnectionRequest connectionRequest = InvokeConnectionRequest.builder()
                    .requestBody(xmlBody)
                    .requestHeaders(headers)
                    .build();
            Assert.assertNotNull(connectionRequest.getRequestBody());
            Assert.assertTrue(connectionRequest.getRequestBody() instanceof String);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testInvokeConnectionWithEmptyStringRequestBody() {
        try {
            String emptyBody = "";
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/xml");
            InvokeConnectionRequest connectionRequest = InvokeConnectionRequest.builder()
                    .requestBody(emptyBody)
                    .requestHeaders(headers)
                    .build();
            Skyflow skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addConnectionConfig(connectionConfig).build();
            skyflowClient.connection().invoke(connectionRequest);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyRequestBody.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInvokeConnectionWithXMLContentType() {
        try {
            String xmlBody = "<?xml version=\"1.0\"?><payment><card>4111111111111111</card></payment>";
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/xml");
            InvokeConnectionRequest connectionRequest = InvokeConnectionRequest.builder()
                    .requestBody(xmlBody)
                    .requestHeaders(headers)
                    .build();
            Assert.assertEquals("application/xml", connectionRequest.getRequestHeaders().get("Content-Type"));
            Assert.assertTrue(connectionRequest.getRequestBody() instanceof String);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testInvokeConnectionWithPlainTextContentType() {
        try {
            String plainTextBody = "This is a plain text request body";
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "text/plain");
            InvokeConnectionRequest connectionRequest = InvokeConnectionRequest.builder()
                    .requestBody(plainTextBody)
                    .requestHeaders(headers)
                    .build();
            Assert.assertEquals("text/plain", connectionRequest.getRequestHeaders().get("Content-Type"));
            Assert.assertTrue(connectionRequest.getRequestBody() instanceof String);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testInvokeConnectionWithHTMLContentType() {
        try {
            String htmlBody = "<html><body><h1>Test</h1></body></html>";
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "text/html");
            InvokeConnectionRequest connectionRequest = InvokeConnectionRequest.builder()
                    .requestBody(htmlBody)
                    .requestHeaders(headers)
                    .build();
            Assert.assertEquals("text/html", connectionRequest.getRequestHeaders().get("Content-Type"));
            Assert.assertTrue(connectionRequest.getRequestBody() instanceof String);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    // Tests for new content-type handling logic with actual controller invocation

    @Test
    public void testInvokeConnectionWithStringBodyAndJsonContentType() throws Exception {
        String jsonResponse = "{\"success\":true}";
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(JsonObject.class), anyMap()))
                .thenReturn(jsonResponse);

        Skyflow client = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)
                .addConnectionConfig(connectionConfig)
                .build();

        String stringBody = "{\"key\":\"value\"}";
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json");

        InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                .method(RequestMethod.POST)
                .requestBody(stringBody)
                .requestHeaders(headers)
                .build();

        InvokeConnectionResponse response = client.connection().invoke(request);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getData() instanceof JsonObject);
    }

    @Test
    public void testInvokeConnectionWithStringBodyAndXmlContentType() throws Exception {
        String xmlResponse = "<response><status>success</status></response>";
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(JsonObject.class), anyMap()))
                .thenReturn(xmlResponse);

        Skyflow client = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)
                .addConnectionConfig(connectionConfig)
                .build();

        String xmlBody = "<xml><data>test</data></xml>";
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/xml");

        InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                .method(RequestMethod.POST)
                .requestBody(xmlBody)
                .requestHeaders(headers)
                .build();

        InvokeConnectionResponse response = client.connection().invoke(request);
        Assert.assertNotNull(response);
        // When response is not valid JSON, it should be returned as String
        Assert.assertTrue(response.getData() instanceof String);
        Assert.assertEquals(xmlResponse, response.getData());
    }

    @Test
    public void testInvokeConnectionWithJsonObjectBody() throws Exception {
        String jsonResponse = "{\"result\":\"ok\"}";
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(JsonObject.class), anyMap()))
                .thenReturn(jsonResponse);

        Skyflow client = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)
                .addConnectionConfig(connectionConfig)
                .build();

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("test", "value");

        InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                .method(RequestMethod.POST)
                .requestBody(jsonBody)
                .build();

        InvokeConnectionResponse response = client.connection().invoke(request);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getData() instanceof JsonObject);
    }

    @Test
    public void testInvokeConnectionWithNullRequestBody() throws Exception {
        String jsonResponse = "{\"status\":\"success\"}";
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(JsonObject.class), anyMap()))
                .thenReturn(jsonResponse);

        Skyflow client = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)
                .addConnectionConfig(connectionConfig)
                .build();

        InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                .method(RequestMethod.GET)
                .requestBody(null)
                .build();

        InvokeConnectionResponse response = client.connection().invoke(request);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getData() instanceof JsonObject);
    }

    @Test
    public void testInvokeConnectionWithNonJsonResponse() throws Exception {
        String plainTextResponse = "This is a plain text response";
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(JsonObject.class), anyMap()))
                .thenReturn(plainTextResponse);

        Skyflow client = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)
                .addConnectionConfig(connectionConfig)
                .build();

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("key", "value");

        InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                .method(RequestMethod.POST)
                .requestBody(jsonBody)
                .build();

        InvokeConnectionResponse response = client.connection().invoke(request);
        Assert.assertNotNull(response);
        // Non-JSON response should be returned as String
        Assert.assertTrue(response.getData() instanceof String);
        Assert.assertEquals(plainTextResponse, response.getData());
    }

    @Test
    public void testInvokeConnectionWithContentTypeCaseInsensitive() throws Exception {
        String jsonResponse = "{\"data\":\"test\"}";
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(JsonObject.class), anyMap()))
                .thenReturn(jsonResponse);

        Skyflow client = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)
                .addConnectionConfig(connectionConfig)
                .build();

        String stringBody = "test data";
        Map<String, String> headers = new HashMap<>();
        // Test with uppercase Content-Type
        headers.put("Content-Type", "APPLICATION/JSON");

        InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                .method(RequestMethod.POST)
                .requestBody(stringBody)
                .requestHeaders(headers)
                .build();

        InvokeConnectionResponse response = client.connection().invoke(request);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getData() instanceof JsonObject);
    }

    @Test
    public void testInvokeConnectionWithMixedCaseContentType() throws Exception {
        String xmlResponse = "<xml>data</xml>";
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(JsonObject.class), anyMap()))
                .thenReturn(xmlResponse);

        Skyflow client = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)
                .addConnectionConfig(connectionConfig)
                .build();

        String stringBody = "<data>test</data>";
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "Application/Xml");

        InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                .method(RequestMethod.POST)
                .requestBody(stringBody)
                .requestHeaders(headers)
                .build();

        InvokeConnectionResponse response = client.connection().invoke(request);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getData() instanceof String);
    }

    @Test
    public void testInvokeConnectionWithMapRequestBody() throws Exception {
        String jsonResponse = "{\"created\":true}";
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(JsonObject.class), anyMap()))
                .thenReturn(jsonResponse);

        Skyflow client = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)
                .addConnectionConfig(connectionConfig)
                .build();

        Map<String, String> mapBody = new HashMap<>();
        mapBody.put("name", "John");
        mapBody.put("age", "30");

        InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                .method(RequestMethod.POST)
                .requestBody(mapBody)
                .build();

        InvokeConnectionResponse response = client.connection().invoke(request);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getData() instanceof JsonObject);
    }

    @Test
    public void testInvokeConnectionWithDefaultContentType() throws Exception {
        String jsonResponse = "{\"message\":\"success\"}";
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(JsonObject.class), anyMap()))
                .thenReturn(jsonResponse);

        Skyflow client = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)
                .addConnectionConfig(connectionConfig)
                .build();

        String stringBody = "string data";
        // No content-type header - should default to application/json

        InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                .method(RequestMethod.POST)
                .requestBody(stringBody)
                .build();

        InvokeConnectionResponse response = client.connection().invoke(request);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getData() instanceof JsonObject);
    }

    @Test
    public void testInvokeConnectionWithFormUrlEncodedContentType() throws Exception {
        String jsonResponse = "{\"submitted\":true}";
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(JsonObject.class), anyMap()))
                .thenReturn(jsonResponse);

        Skyflow client = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)
                .addConnectionConfig(connectionConfig)
                .build();

        String formBody = "param1=value1&param2=value2";
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/x-www-form-urlencoded");

        InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                .method(RequestMethod.POST)
                .requestBody(formBody)
                .requestHeaders(headers)
                .build();

        InvokeConnectionResponse response = client.connection().invoke(request);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getData() instanceof JsonObject);
    }

    @Test
    public void testInvokeConnectionMetadataContainsRequestId() throws Exception {
        String jsonResponse = "{\"data\":\"value\"}";
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(JsonObject.class), anyMap()))
                .thenReturn(jsonResponse);

        Skyflow client = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)
                .addConnectionConfig(connectionConfig)
                .build();

        InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                .method(RequestMethod.GET)
                .build();

        InvokeConnectionResponse response = client.connection().invoke(request);
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getMetadata());
        Assert.assertEquals("test-request-id", response.getMetadata().get("requestId"));
    }
}
