package com.skyflow.errors;

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
        Assert.assertNotNull(ex.getDetails());
        Assert.assertEquals(0, ex.getDetails().size());
    }

    @Test
    public void testToStringFormat() {
        SkyflowException ex = new SkyflowException(404, "Not Found");
        String str = ex.toString();
        Assert.assertTrue(str.contains("httpCode: 404"));
        Assert.assertTrue(str.contains("message: Not Found"));
    }

    @Test
    public void testConstructorWithJsonErrorBodyArrayDetailsNonEmpty() {
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
    public void testConstructorWithJsonErrorBodyArrayDetailsEmpty() {
        String json = "{\"error\":{\"message\":\"json error\",\"grpc_code\":7,\"http_status\":\"NOT_FOUND\",\"details\":[]}}";
        Map<String, List<String>> headers = new HashMap<>();
        SkyflowException ex = new SkyflowException(404, new RuntimeException("fail"), headers, json);
        Assert.assertEquals("json error", ex.getMessage());
        Assert.assertEquals(Integer.valueOf(7), ex.getGrpcCode());
        Assert.assertEquals("NOT_FOUND", ex.getHttpStatus());
        Assert.assertNotNull(ex.getDetails());
        Assert.assertEquals(0, ex.getDetails().size());
    }

    @Test
    public void testConstructorWithJsonErrorBodyObjectDetailsEmpty() {
        String json = "{\"error\":{\"message\":\"json error\",\"grpc_code\":7,\"http_status\":\"NOT_FOUND\",\"details\":{}}}";
        Map<String, List<String>> headers = new HashMap<>();
        SkyflowException ex = new SkyflowException(404, new RuntimeException("fail"), headers, json);
        Assert.assertEquals("json error", ex.getMessage());
        Assert.assertEquals(Integer.valueOf(7), ex.getGrpcCode());
        Assert.assertEquals("NOT_FOUND", ex.getHttpStatus());
        Assert.assertNotNull(ex.getDetails());
        Assert.assertEquals(0, ex.getDetails().size());
    }

    @Test
    public void testConstructorWithJsonErrorBodyObjectDetailsAllEmptyArrays() {
        String json = "{\"error\":{\"message\":\"json error\",\"grpc_code\":7,\"http_status\":\"NOT_FOUND\",\"details\":{\"a\":[],\"b\":[]}}}";
        Map<String, List<String>> headers = new HashMap<>();
        SkyflowException ex = new SkyflowException(404, new RuntimeException("fail"), headers, json);
        Assert.assertEquals("json error", ex.getMessage());
        Assert.assertEquals(Integer.valueOf(7), ex.getGrpcCode());
        Assert.assertEquals("NOT_FOUND", ex.getHttpStatus());
        Assert.assertNotNull(ex.getDetails());
        Assert.assertEquals(0, ex.getDetails().size());
    }

    @Test
    public void testConstructorWithJsonErrorBodyObjectDetailsAllNulls() {
        String json = "{\"error\":{\"message\":\"json error\",\"grpc_code\":7,\"http_status\":\"NOT_FOUND\",\"details\":{\"a\":null,\"b\":null}}}";
        Map<String, List<String>> headers = new HashMap<>();
        SkyflowException ex = new SkyflowException(404, new RuntimeException("fail"), headers, json);
        Assert.assertEquals("json error", ex.getMessage());
        Assert.assertEquals(Integer.valueOf(7), ex.getGrpcCode());
        Assert.assertEquals("NOT_FOUND", ex.getHttpStatus());
        Assert.assertNotNull(ex.getDetails());
        Assert.assertEquals(0, ex.getDetails().size());
    }

    @Test
    public void testConstructorWithJsonErrorBodyObjectDetailsNonEmpty() {
        String json = "{\"error\":{\"message\":\"json error\",\"grpc_code\":7,\"http_status\":\"NOT_FOUND\",\"details\":{\"a\":[1],\"b\":null}}}";
        Map<String, List<String>> headers = new HashMap<>();
        SkyflowException ex = new SkyflowException(404, new RuntimeException("fail"), headers, json);
        Assert.assertEquals("json error", ex.getMessage());
        Assert.assertEquals(Integer.valueOf(7), ex.getGrpcCode());
        Assert.assertEquals("NOT_FOUND", ex.getHttpStatus());
        Assert.assertNotNull(ex.getDetails());
        Assert.assertEquals(1, ex.getDetails().size());
        Assert.assertTrue(ex.getDetails().get(0).getAsJsonObject().has("a"));
    }

    @Test
    public void testConstructorWithJsonErrorBodyNullDetails() {
        String json = "{\"error\":{\"message\":\"json error\",\"grpc_code\":7,\"http_status\":\"NOT_FOUND\"}}";
        Map<String, List<String>> headers = new HashMap<>();
        SkyflowException ex = new SkyflowException(404, new RuntimeException("fail"), headers, json);
        Assert.assertEquals("json error", ex.getMessage());
        Assert.assertEquals(Integer.valueOf(7), ex.getGrpcCode());
        Assert.assertEquals("NOT_FOUND", ex.getHttpStatus());
        Assert.assertNotNull(ex.getDetails());
        Assert.assertEquals(0, ex.getDetails().size());
    }

    @Test
    public void testConstructorWithNullErrorBody() {
        Map<String, List<String>> headers = new HashMap<>();
        SkyflowException ex = new SkyflowException(500, new RuntimeException("fail"), headers, null);
        Assert.assertEquals("fail", ex.getMessage());
        Assert.assertNull(ex.getGrpcCode());
    }

    @Test
    public void testGettersAndSetters() {
        SkyflowException ex = new SkyflowException("msg");
        Assert.assertNull(ex.getRequestId());
        Assert.assertNull(ex.getDetails());
        Assert.assertNull(ex.getGrpcCode());
        Assert.assertNull(ex.getHttpStatus());
    }

    @Test
    public void testSetDetailsWithErrorFromClientHeader() {
        String json = "{\"error\":{\"message\":\"test error\",\"grpc_code\":13,\"details\":[]}}";
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("error-from-client", Collections.singletonList("client error"));

        SkyflowException ex = new SkyflowException(500, new RuntimeException("fail"), headers, json);

        Assert.assertNotNull(ex.getDetails());
        Assert.assertEquals(1, ex.getDetails().size());
        Assert.assertEquals("client error", ex.getDetails().get(0).getAsJsonObject().get("errorFromClient").getAsString());
    }

    @Test
    public void testSetDetailsWithErrorFromClientHeaderAndNullDetails() {
        String json = "{\"error\":{\"message\":\"test error\",\"grpc_code\":13}}";
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("error-from-client", Collections.singletonList("client error"));

        SkyflowException ex = new SkyflowException(500, new RuntimeException("fail"), headers, json);

        Assert.assertNotNull(ex.getDetails());
        Assert.assertEquals(1, ex.getDetails().size());
        Assert.assertEquals("client error", ex.getDetails().get(0).getAsJsonObject().get("errorFromClient").getAsString());
    }

    @Test
    public void testSetDetailsWithNoErrorFromClientHeaderAndNullDetails() {
        String json = "{\"error\":{\"message\":\"test error\",\"grpc_code\":13}}";
        Map<String, List<String>> headers = new HashMap<>();

        SkyflowException ex = new SkyflowException(500, new RuntimeException("fail"), headers, json);

        Assert.assertNotNull(ex.getDetails());
        Assert.assertEquals(0, ex.getDetails().size());
    }

    @Test
    public void testSetDetailsWithNoErrorFromClientHeaderAndEmptyDetails() {
        String json = "{\"error\":{\"message\":\"test error\",\"grpc_code\":13,\"details\":[]}}";
        Map<String, List<String>> headers = new HashMap<>();

        SkyflowException ex = new SkyflowException(500, new RuntimeException("fail"), headers, json);

        Assert.assertNotNull(ex.getDetails());
        Assert.assertEquals(0, ex.getDetails().size());
    }

    @Test
    public void testToStringWithNullFields() {
        SkyflowException ex = new SkyflowException("msg");
        String str = ex.toString();
        Assert.assertTrue(str.contains("requestId: null"));
        Assert.assertTrue(str.contains("grpcCode: null"));
        Assert.assertTrue(str.contains("httpCode: null"));
        Assert.assertTrue(str.contains("httpStatus: null"));
        Assert.assertTrue(str.contains("details: null"));
    }
}