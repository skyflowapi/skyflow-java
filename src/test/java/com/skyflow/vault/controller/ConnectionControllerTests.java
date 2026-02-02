package com.skyflow.vault.controller;

import com.skyflow.Skyflow;
import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.connection.InvokeConnectionRequest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;

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
}
