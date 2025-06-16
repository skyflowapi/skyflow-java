package com.skyflow.errors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class SkyflowExceptionTest {

    @Test
    public void testConstructorWithMessage() {
        SkyflowException ex = new SkyflowException("Test message");
        Assert.assertEquals("Test message", ex.getMessage());
        Assert.assertNull(ex.getHttpStatus());
        Assert.assertNull(ex.getGrpcCode());
    }

    @Test
    public void testConstructorWithThrowable() {
        Throwable cause = new RuntimeException("Root cause");
        SkyflowException ex = new SkyflowException(cause);
        Assert.assertEquals("Root cause", ex.getMessage());
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        Throwable cause = new RuntimeException("Root cause");
        SkyflowException ex = new SkyflowException("Test message", cause);
        Assert.assertEquals("Test message", ex.getMessage());
    }

    @Test
    public void testConstructorWithCodeAndMessage() {
        SkyflowException ex = new SkyflowException(400, "Bad Request");
        Assert.assertEquals(Integer.valueOf(400), Integer.valueOf(ex.getHttpCode()));
        Assert.assertEquals("Bad Request", ex.getMessage());
        Assert.assertEquals("Bad Request", ex.toString().contains("Bad Request") ? "Bad Request" : null);
    }

    @Test
    public void testToStringFormat() {
        SkyflowException ex = new SkyflowException(404, "Not Found");
        String str = ex.toString();
        Assert.assertTrue(str.contains("httpCode: 404"));
        Assert.assertTrue(str.contains("message: Not Found"));
    }

    @Test
    public void testConstructorWithJsonErrorBody() {
        String json = "{\"error\":{\"message\":\"json error\",\"grpc_code\":7,\"http_status\":\"NOT_FOUND\",\"details\":[{\"info\":\"detail1\"}]}}";
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("x-request-id", Collections.singletonList("req-123"));
        SkyflowException ex = new SkyflowException(404, new RuntimeException("fail"), headers, json);
        Assert.assertEquals("json error", ex.getMessage());
        Assert.assertEquals(Integer.valueOf(7), ex.getGrpcCode());
        Assert.assertEquals("NOT_FOUND", ex.getHttpStatus());
        Assert.assertEquals("req-123", ex.getRequestId());
        Assert.assertNotNull(ex.getDetails());
        Assert.assertTrue(ex.getDetails().size() > 0);
    }

    @Test
    public void testConstructorWithNonJsonErrorBody() {
        String errorMsg = "plain error";
        Map<String, List<String>> headers = new HashMap<>();
        SkyflowException ex = new SkyflowException(500, new RuntimeException("fail"), headers, errorMsg);
        Assert.assertEquals(errorMsg, ex.getMessage());
        Assert.assertNull(ex.getGrpcCode());
        Assert.assertNull(ex.getHttpStatus());
    }

    @Test
    public void testGettersAndSetters() {
        SkyflowException ex = new SkyflowException("msg");
        // Simulate setting fields via reflection or constructor
        // (getters are already tested above)
        Assert.assertNull(ex.getRequestId());
        Assert.assertNull(ex.getDetails());
        Assert.assertNull(ex.getGrpcCode());
        Assert.assertNull(ex.getHttpStatus());
    }
}