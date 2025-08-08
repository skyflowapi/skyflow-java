package com.skyflow.v2.utils;

import com.google.gson.JsonObject;
import com.skyflow.common.errors.SkyflowException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@RunWith(PowerMockRunner.class)
@PrepareForTest({URL.class, HttpURLConnection.class})
public class HttpUtilityTests {

    private static String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    @InjectMocks
    HttpUtility httpUtility;
    @Mock
    OutputStream outputStream;
    private String expected;
    private String expectedError;
    private URL url;
    private HttpURLConnection mockConnection;

    @Before
    public void setup() throws IOException {
        expected = "{\"status\":\"success\"}";
        expectedError = "{\"error\":{\"grpc_code\":123,\"http_code\":500,\"message\":\"something went wrong\",\"http_status\":\"internal server error\",\"details\":[]}}\n";
        mockConnection = Mockito.mock(HttpURLConnection.class);
        given(mockConnection.getInputStream()).willReturn(new ByteArrayInputStream(expected.getBytes()));
        given(mockConnection.getErrorStream()).willReturn(new ByteArrayInputStream(expectedError.getBytes()));
        given(mockConnection.getOutputStream()).willReturn(outputStream);
        given(mockConnection.getResponseCode()).willReturn(200);
        given(mockConnection.getHeaderField(anyString())).willReturn("id");
        final URLStreamHandler handler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(final URL arg0) throws IOException {
                return mockConnection;
            }
        };
        url = new URL("https://google.com", "google.com", 80, "", handler);
    }

    @Test
    @PrepareForTest({URL.class, HttpURLConnection.class})
    public void testSendRequest() {
        try {
            given(mockConnection.getRequestProperty("content-type")).willReturn("application/json");
            Map<String, String> headers = new HashMap<>();
            headers.put("content-type", "application/json");
            JsonObject params = new JsonObject();
            params.addProperty("key", "value");
            String response = httpUtility.sendRequest("GET", url, params, headers);
            Assert.assertEquals(expected, response);
        } catch (Exception e) {
            fail(INVALID_EXCEPTION_THROWN);
        }
    }


    @Test
    @PrepareForTest({URL.class, HttpURLConnection.class})
    public void testSendRequestFormData() {
        try {
            given(mockConnection.getRequestProperty("content-type")).willReturn("multipart/form-data");
            Map<String, String> headers = new HashMap<>();
            headers.put("content-type", "multipart/form-data");
            JsonObject params = new JsonObject();
            params.addProperty("key", "value");
            String response = httpUtility.sendRequest("GET", url, params, headers);
            Assert.assertEquals(expected, response);
        } catch (Exception e) {
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest({URL.class, HttpURLConnection.class})
    public void testSendRequestFormURLEncoded() {
        try {
            given(mockConnection.getRequestProperty("content-type")).willReturn("application/x-www-form-urlencoded");
            Map<String, String> headers = new HashMap<>();
            headers.put("content-type", "application/x-www-form-urlencoded");
            JsonObject params = new JsonObject();
            params.addProperty("key", "value");
            String response = httpUtility.sendRequest("GET", url, params, headers);
            Assert.assertEquals(expected, response);
        } catch (Exception e) {
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest({URL.class, HttpURLConnection.class})
    public void testSendRequestError() {
        try {
            given(mockConnection.getResponseCode()).willReturn(500);
            String response = httpUtility.sendRequest("GET", url, null, null);
        } catch (SkyflowException e) {
            Assert.assertEquals(500, e.getHttpCode());
            Assert.assertEquals(new Integer(123), e.getGrpcCode());
            Assert.assertEquals("internal server error", e.getHttpStatus());
            Assert.assertEquals("something went wrong", e.getMessage());
            Assert.assertTrue(e.getDetails().isEmpty());
        } catch (Exception e) {
            fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
