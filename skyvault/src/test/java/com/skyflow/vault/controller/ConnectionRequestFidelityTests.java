package com.skyflow.vault.controller;

import com.google.gson.JsonObject;
import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.enums.RequestMethod;
import com.skyflow.utils.Constants;
import com.skyflow.utils.HttpUtility;
import com.skyflow.vault.connection.InvokeConnectionRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Request-fidelity tests for {@code ConnectionController.invoke} at the transport boundary: every
 * value the user sets on {@link InvokeConnectionRequest} is captured as it is handed to
 * {@link HttpUtility#sendRequest}.
 *
 * <p>Tests suffixed {@code _knownGap} pin confirmed defects and assert the CURRENT behaviour.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpUtility.class})
public class ConnectionRequestFidelityTests {

    private static final String API_KEY = "sky-ab123-abcd1234cdef1234abcd4321cdef4321"; // gitleaks:allow
    private static final String CONNECTION_URL = "https://conn.example.com/api/{resource}/details";
    private static final String NON_ASCII_NAME = "日本語 テスト";

    private static ConnectionConfig connectionConfig;
    private static Credentials credentials;
    private ConnectionController controller;

    @BeforeClass
    public static void setupClass() {
        credentials = new Credentials();
        credentials.setApiKey(API_KEY);

        connectionConfig = new ConnectionConfig();
        connectionConfig.setConnectionId("conn123");
        connectionConfig.setConnectionUrl(CONNECTION_URL);
        connectionConfig.setCredentials(credentials);
    }

    @Before
    public void setup() throws Exception {
        controller = new ConnectionController(connectionConfig, credentials);
        PowerMockito.mockStatic(HttpUtility.class);
        when(HttpUtility.sendRequest(anyString(), any(URL.class), any(), any())).thenReturn("{}");
        when(HttpUtility.getRequestID()).thenReturn("req-fidelity-1");
    }

    private static final class Captured {
        private String method;
        private URL url;
        private JsonObject body;
        private Map<String, String> headers;
    }

    @SuppressWarnings("unchecked")
    private static Captured capture() throws Exception {
        ArgumentCaptor<String> methodCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<URL> urlCaptor = ArgumentCaptor.forClass(URL.class);
        ArgumentCaptor<JsonObject> bodyCaptor = ArgumentCaptor.forClass(JsonObject.class);
        ArgumentCaptor<Map> headersCaptor = ArgumentCaptor.forClass(Map.class);

        PowerMockito.verifyStatic(HttpUtility.class);
        HttpUtility.sendRequest(methodCaptor.capture(), urlCaptor.capture(),
                bodyCaptor.capture(), headersCaptor.capture());

        Captured captured = new Captured();
        captured.method = methodCaptor.getValue();
        captured.url = urlCaptor.getValue();
        captured.body = bodyCaptor.getValue();
        captured.headers = headersCaptor.getValue();
        return captured;
    }

    @Test
    public void testInvoke_methodPathParamsQueryParamsHeadersAndBodyAllReachTransport() throws Exception {
        Map<String, String> pathParams = new LinkedHashMap<>();
        pathParams.put("resource", "cards");

        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("limit", "10");
        queryParams.put("offset", "20");

        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("X-Custom-Header", NON_ASCII_NAME);
        requestHeaders.put("content-type", "application/json");

        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("city", "北京市 朝阳区");
        nested.put("zip", "100000");

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("name", NON_ASCII_NAME);
        requestBody.put("age", 42);
        requestBody.put("active", true);
        requestBody.put("address", nested);

        controller.invoke(InvokeConnectionRequest.builder()
                .method(RequestMethod.PUT)
                .pathParams(pathParams)
                .queryParams(queryParams)
                .requestHeaders(requestHeaders)
                .requestBody(requestBody)
                .build());

        Captured captured = capture();

        Assert.assertEquals("PUT", captured.method);
        Assert.assertEquals("https://conn.example.com/api/cards/details?limit=10&offset=20",
                captured.url.toString());

        Assert.assertEquals(NON_ASCII_NAME, captured.headers.get("x-custom-header"));
        Assert.assertEquals("application/json", captured.headers.get("content-type"));
        Assert.assertEquals("auth header carries the api key",
                API_KEY, captured.headers.get(Constants.SDK_AUTH_HEADER_KEY));

        Assert.assertEquals(NON_ASCII_NAME, captured.body.get("name").getAsString());
        Assert.assertEquals(42, captured.body.get("age").getAsInt());
        Assert.assertTrue(captured.body.get("active").getAsBoolean());
        Assert.assertEquals("北京市 朝阳区",
                captured.body.getAsJsonObject("address").get("city").getAsString());
        Assert.assertEquals("100000",
                captured.body.getAsJsonObject("address").get("zip").getAsString());
    }

    @Test
    public void testInvoke_defaultMethodIsPost() throws Exception {
        controller.invoke(InvokeConnectionRequest.builder().build());
        Assert.assertEquals("POST", capture().method);
    }

    @Test
    public void testInvoke_eachRequestMethodIsForwardedByName() throws Exception {
        for (RequestMethod method : new RequestMethod[]{
                RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE}) {
            ConnectionController freshController = new ConnectionController(connectionConfig, credentials);
            PowerMockito.mockStatic(HttpUtility.class);
            when(HttpUtility.sendRequest(anyString(), any(URL.class), any(), any())).thenReturn("{}");
            when(HttpUtility.getRequestID()).thenReturn("req-fidelity-1");

            freshController.invoke(InvokeConnectionRequest.builder().method(method).build());
            Assert.assertEquals(method.name(), capture().method);
        }
    }

    /**
     * NEW FINDING (not one of the 11 audited gaps): a non-object {@code requestBody} — a String,
     * a number, a List — never reaches the transport. {@code Validations} calls
     * {@code gson.toJsonTree(body).getAsJsonObject()} and blows up with a raw
     * {@link IllegalStateException} (not a {@code SkyflowException}), which also makes
     * {@code ConnectionController.convertObjectToJson}'s "wrap scalars under a value key" branch
     * unreachable for scalars.
     */
    @Test
    public void testInvoke_scalarRequestBodyThrowsRawIllegalStateException_knownGap() throws Exception {
        for (Object body : new Object[]{"a plain string", 42, java.util.Arrays.asList("a", "b")}) {
            try {
                controller.invoke(InvokeConnectionRequest.builder()
                        .method(RequestMethod.POST)
                        .requestBody(body)
                        .build());
                Assert.fail("expected an exception for a non-object request body: " + body);
            } catch (IllegalStateException expected) {
                Assert.assertTrue(expected.getMessage().contains("Not a JSON Object"));
            }
        }
        PowerMockito.verifyStatic(HttpUtility.class, org.mockito.Mockito.never());
        HttpUtility.sendRequest(anyString(), any(URL.class), any(), any());
    }

    /**
     * KNOWN GAP: a user-supplied {@code sky-metadata} header is unconditionally overwritten by the
     * SDK's own metrics blob.
     */
    @Test
    public void testInvoke_userSuppliedSkyMetadataHeaderIsOverwritten_knownGap() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(Constants.SDK_METRICS_HEADER_KEY, "user-supplied-value");

        controller.invoke(InvokeConnectionRequest.builder()
                .method(RequestMethod.GET)
                .requestHeaders(requestHeaders)
                .build());

        String sent = capture().headers.get(Constants.SDK_METRICS_HEADER_KEY);
        Assert.assertNotEquals("user value is discarded", "user-supplied-value", sent);
        Assert.assertTrue("replaced by the SDK metrics blob",
                sent.contains(Constants.SDK_METRIC_NAME_VERSION));
    }

    /**
     * KNOWN GAP: two request headers differing only in case collapse into one entry before they
     * reach the transport (last writer wins), so one of the user's headers is lost.
     */
    @Test
    public void testInvoke_headersDifferingOnlyInCaseCollapse_knownGap() throws Exception {
        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("X-Trace", "first-value");
        requestHeaders.put("x-trace", "second-value");

        controller.invoke(InvokeConnectionRequest.builder()
                .method(RequestMethod.GET)
                .requestHeaders(requestHeaders)
                .build());

        Map<String, String> headers = capture().headers;
        Assert.assertEquals("second-value", headers.get("x-trace"));
        Assert.assertFalse("the original-cased key is gone", headers.containsKey("X-Trace"));
    }

    /**
     * KNOWN GAP: query-param values are not percent-encoded, so a value containing {@code &} and
     * {@code =} re-partitions the query string on the wire.
     */
    @Test
    public void testInvoke_queryParamValuesReachTransportUnencoded_knownGap() throws Exception {
        Map<String, String> pathParams = new LinkedHashMap<>();
        pathParams.put("resource", "cards");
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("q", "a&b=c");

        controller.invoke(InvokeConnectionRequest.builder()
                .method(RequestMethod.GET)
                .pathParams(pathParams)
                .queryParams(queryParams)
                .build());

        URL url = capture().url;
        Assert.assertEquals("q=a&b=c", url.getQuery());
        Assert.assertEquals("one user param became two wire params", 2, url.getQuery().split("&").length);
    }

    /**
     * KNOWN GAP: path-param values are not percent-encoded, so a {@code /} in the value changes the
     * request path on the wire.
     */
    @Test
    public void testInvoke_pathParamValuesReachTransportUnencoded_knownGap() throws Exception {
        Map<String, String> pathParams = new LinkedHashMap<>();
        pathParams.put("resource", "cards/123");

        controller.invoke(InvokeConnectionRequest.builder()
                .method(RequestMethod.GET)
                .pathParams(pathParams)
                .build());

        Assert.assertEquals("/api/cards/123/details", capture().url.getPath());
    }
}
