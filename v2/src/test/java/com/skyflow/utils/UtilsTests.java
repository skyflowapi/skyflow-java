package com.skyflow.utils;

import com.google.gson.JsonObject;
import com.skyflow.config.ConnectionConfig;
import com.skyflow.enums.RequestMethod;
import com.skyflow.vault.connection.InvokeConnectionRequest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class UtilsTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static String connectionId = null;
    private static String connectionUrl = null;
    private static Map<String, String> queryParams;
    private static Map<String, String> pathParams;
    private static Map<String, String> requestHeaders;

    @BeforeClass
    public static void setup() {
        connectionId = "test_connection_id";
        connectionUrl = "https://test.connection.url";
        pathParams = new HashMap<>();
        queryParams = new HashMap<>();
        requestHeaders = new HashMap<>();
    }

    @Test
    public void testConstructConnectionURL() {
        try {
            queryParams.put("query_param", "value");
            pathParams.put("path_param", "value");

            ConnectionConfig connectionConfig = new ConnectionConfig();
            connectionConfig.setConnectionId(connectionId);
            connectionConfig.setConnectionUrl(connectionUrl);

            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .method(RequestMethod.POST).pathParams(pathParams).queryParams(queryParams).build();
            String filledUrl = Utils.constructConnectionURL(connectionConfig, request);
            Assert.assertEquals(connectionUrl + "?" + "query_param=value", filledUrl);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testConstructConnectionHeaderMap() {
        try {
            requestHeaders.put("HEADER", "value");
            Map<String, String> headers = Utils.constructConnectionHeadersMap(requestHeaders);
            Assert.assertEquals(1, headers.size());
            Assert.assertTrue(headers.containsKey("header"));
            Assert.assertFalse(headers.containsKey("HEADER"));
            Assert.assertEquals("value", headers.get("header"));
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetMetrics() {
        try {
            JsonObject metrics = Utils.getMetrics();
            Assert.assertNotNull(metrics.get(Constants.SDK_METRIC_NAME_VERSION));
            Assert.assertNotNull(metrics.get(Constants.SDK_METRIC_CLIENT_DEVICE_MODEL));
            Assert.assertNotNull(metrics.get(Constants.SDK_METRIC_CLIENT_OS_DETAILS));
            Assert.assertNotNull(metrics.get(Constants.SDK_METRIC_RUNTIME_DETAILS));
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetMetricsWithException() {
        try {
            // Clearing System Properties explicitly to throw exception
            System.clearProperty("os.name");
            System.clearProperty("os.version");
            System.clearProperty("java.version");

            JsonObject metrics = Utils.getMetrics();
            Assert.assertEquals("skyflow-java@" + Constants.SDK_VERSION, metrics.get(Constants.SDK_METRIC_NAME_VERSION).getAsString());
            Assert.assertEquals("Java@", metrics.get(Constants.SDK_METRIC_RUNTIME_DETAILS).getAsString());
            Assert.assertTrue(metrics.get(Constants.SDK_METRIC_CLIENT_DEVICE_MODEL).getAsString().isEmpty());
            Assert.assertTrue(metrics.get(Constants.SDK_METRIC_CLIENT_OS_DETAILS).getAsString().isEmpty());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
