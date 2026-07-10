package com.skyflow.vault.controller;

import com.skyflow.Skyflow;
import com.skyflow.config.ConnectionConfig;
import com.skyflow.enums.LogLevel;
import com.skyflow.enums.RequestMethod;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.HttpUtility;
import com.skyflow.vault.connection.InvokeConnectionRequest;
import com.skyflow.vault.connection.InvokeConnectionResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpUtility.class})
public class ConnectionControllerTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static final String API_KEY = "sky-ab123-abcd1234cdef1234abcd4321cdef4321"; // gitleaks:allow
    private static final String REQUEST_ID = "req-test-123";

    private static ConnectionConfig connectionConfig;
    private static Credentials credentials;
    private ConnectionController controller;

    @BeforeClass
    public static void setupClass() {
        credentials = new Credentials();
        credentials.setApiKey(API_KEY);

        connectionConfig = new ConnectionConfig();
        connectionConfig.setConnectionId("conn123");
        connectionConfig.setConnectionUrl("https://test.connection.url");
        connectionConfig.setCredentials(credentials);
    }

    @Before
    public void setup() {
        controller = new ConnectionController(connectionConfig, credentials);
        PowerMockito.mockStatic(HttpUtility.class);
    }

    // --- existing validation test (kept) ---

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

    // --- happy-path tests ---

    @Test
    public void testInvoke_successWithDefaultRequest() throws Exception {
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(), any()))
                .thenReturn("{\"data\":\"test-value\"}");
        when(HttpUtility.getRequestID()).thenReturn(REQUEST_ID);

        InvokeConnectionRequest request = InvokeConnectionRequest.builder().build();
        InvokeConnectionResponse response = controller.invoke(request);

        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getData());
    }

    @Test
    public void testInvoke_successWithGetMethod() throws Exception {
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(), any()))
                .thenReturn("{\"result\":\"ok\"}");
        when(HttpUtility.getRequestID()).thenReturn(REQUEST_ID);

        InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                .method(RequestMethod.GET)
                .build();
        InvokeConnectionResponse response = controller.invoke(request);

        Assert.assertNotNull(response);
    }

    @Test
    public void testInvoke_successWithDeleteMethod() throws Exception {
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(), any()))
                .thenReturn("{\"deleted\":true}");
        when(HttpUtility.getRequestID()).thenReturn(REQUEST_ID);

        InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                .method(RequestMethod.DELETE)
                .build();
        InvokeConnectionResponse response = controller.invoke(request);

        Assert.assertNotNull(response);
    }

    @Test
    public void testInvoke_successWithPutMethod() throws Exception {
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(), any()))
                .thenReturn("{\"updated\":true}");
        when(HttpUtility.getRequestID()).thenReturn(REQUEST_ID);

        Map<String, Object> body = new HashMap<>();
        body.put("field", "value");

        InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                .method(RequestMethod.PUT)
                .requestBody(body)
                .build();
        InvokeConnectionResponse response = controller.invoke(request);

        Assert.assertNotNull(response);
    }

    @Test
    public void testInvoke_successWithObjectBody() throws Exception {
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(), any()))
                .thenReturn("{\"result\":\"ok\"}");
        when(HttpUtility.getRequestID()).thenReturn(REQUEST_ID);

        Map<String, Object> body = new HashMap<>();
        body.put("card_number", "4111111111111111");

        InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                .method(RequestMethod.POST)
                .requestBody(body)
                .build();
        InvokeConnectionResponse response = controller.invoke(request);

        Assert.assertNotNull(response);
    }

    @Test
    public void testInvoke_withPathParams() throws Exception {
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(), any()))
                .thenReturn("{\"data\":\"ok\"}");
        when(HttpUtility.getRequestID()).thenReturn(REQUEST_ID);

        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "record-123");

        InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                .method(RequestMethod.GET)
                .pathParams(pathParams)
                .build();
        InvokeConnectionResponse response = controller.invoke(request);

        Assert.assertNotNull(response);
    }

    @Test
    public void testInvoke_withQueryParams() throws Exception {
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(), any()))
                .thenReturn("{\"data\":\"ok\"}");
        when(HttpUtility.getRequestID()).thenReturn(REQUEST_ID);

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("limit", "10");

        InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                .method(RequestMethod.GET)
                .queryParams(queryParams)
                .build();
        InvokeConnectionResponse response = controller.invoke(request);

        Assert.assertNotNull(response);
    }

    @Test
    public void testInvoke_withRequestHeaders() throws Exception {
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(), any()))
                .thenReturn("{\"ok\":true}");
        when(HttpUtility.getRequestID()).thenReturn(REQUEST_ID);

        Map<String, String> headers = new HashMap<>();
        headers.put("x-custom-header", "custom-value");

        InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                .method(RequestMethod.GET)
                .requestHeaders(headers)
                .build();
        InvokeConnectionResponse response = controller.invoke(request);

        Assert.assertNotNull(response);
    }

    @Test
    public void testInvoke_nonJsonResponseWrappedUnderResponseKey() throws Exception {
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(), any()))
                .thenReturn("plain-text-response");
        when(HttpUtility.getRequestID()).thenReturn(REQUEST_ID);

        InvokeConnectionRequest request = InvokeConnectionRequest.builder().build();
        InvokeConnectionResponse response = controller.invoke(request);

        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getData());
        JsonObject data = JsonParser.parseString(response.getData().toString()).getAsJsonObject();
        Assert.assertTrue(data.has("response"));
        Assert.assertEquals("plain-text-response", data.get("response").getAsString());
    }

    @Test
    public void testInvoke_responseContainsRequestId() throws Exception {
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(), any()))
                .thenReturn("{\"data\":\"ok\"}");
        when(HttpUtility.getRequestID()).thenReturn(REQUEST_ID);

        InvokeConnectionRequest request = InvokeConnectionRequest.builder().build();
        InvokeConnectionResponse response = controller.invoke(request);

        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getMetadata());
        Assert.assertEquals(REQUEST_ID, response.getMetadata().get("requestId"));
    }

    @Test
    public void testInvoke_errorsNullOnSuccess() throws Exception {
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(), any()))
                .thenReturn("{\"data\":\"ok\"}");
        when(HttpUtility.getRequestID()).thenReturn(REQUEST_ID);

        InvokeConnectionRequest request = InvokeConnectionRequest.builder().build();
        InvokeConnectionResponse response = controller.invoke(request);

        Assert.assertNotNull(response);
        Assert.assertNull(response.getErrors());
    }

    // --- error / validation-failure tests ---

    @Test
    public void testInvoke_ioExceptionThrowsSkyflowException() throws Exception {
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(), any()))
                .thenThrow(new IOException("connection refused"));
        when(HttpUtility.getRequestID()).thenReturn(REQUEST_ID);

        try {
            InvokeConnectionRequest request = InvokeConnectionRequest.builder().build();
            controller.invoke(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testInvoke_skyflowExceptionFromSendRequestPropagates() throws Exception {
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(), any()))
                .thenThrow(new SkyflowException("upstream error", new RuntimeException()));
        when(HttpUtility.getRequestID()).thenReturn(REQUEST_ID);

        try {
            InvokeConnectionRequest request = InvokeConnectionRequest.builder().build();
            controller.invoke(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testInvoke_emptyRequestHeadersThrowsSkyflowException() {
        try {
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .requestHeaders(new HashMap<>())
                    .build();
            controller.invoke(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyRequestHeaders.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInvoke_emptyPathParamsThrowsSkyflowException() {
        try {
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .pathParams(new HashMap<>())
                    .build();
            controller.invoke(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyPathParams.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInvoke_emptyQueryParamsThrowsSkyflowException() {
        try {
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .queryParams(new HashMap<>())
                    .build();
            controller.invoke(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyQueryParams.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInvoke_emptyHashMapBodyThrowsSkyflowException() {
        try {
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .requestBody(new HashMap<>())
                    .build();
            controller.invoke(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyRequestBody.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInvoke_nullHeaderValueThrowsSkyflowException() {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("x-header", null);
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .requestHeaders(headers)
                    .build();
            controller.invoke(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.InvalidRequestHeaders.getMessage(), e.getMessage());
        }
    }
}
