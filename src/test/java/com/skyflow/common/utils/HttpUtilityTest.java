package com.skyflow.common.utils;

import com.skyflow.errors.SkyflowException;
import org.json.simple.JSONObject;
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

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ URL.class, HttpURLConnection.class })
public class HttpUtilityTest {

    @InjectMocks
    HttpUtility httpUtility;

    @Mock
    OutputStream outputStream;

    private String expected;

    private String expectedError;
    private URL url;

    private HttpURLConnection mockConnection;
    private static String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";

    @Before
    public void setup() throws IOException {
        expected = "{\"status\":\"success\"}";
        expectedError = "{\"status\":\"something went wrong\"}";
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
    @PrepareForTest({ URL.class, HttpURLConnection.class })
    public void testSendRequest() {
        try {
            given(mockConnection.getRequestProperty("content-type")).willReturn("application/json");

            JSONObject headers = new JSONObject();
            headers.put("content-type", "application/json");

            JSONObject params = new JSONObject();
            params.put("key", "value");

            String response = httpUtility.sendRequest("GET", url, params, headers);

            Assert.assertEquals(expected, response);
        } catch (IOException e) {
            fail(INVALID_EXCEPTION_THROWN);
        } catch (SkyflowException e) {
            fail(INVALID_EXCEPTION_THROWN);
        } catch (Exception e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }


    @Test
    @PrepareForTest({ URL.class, HttpURLConnection.class })
    public void testSendRequestFormData() {
        try {
            given(mockConnection.getRequestProperty("content-type")).willReturn("multipart/form-data");

            JSONObject headers = new JSONObject();
            headers.put("content-type", "multipart/form-data");

            JSONObject params = new JSONObject();
            params.put("key", "value");

            String response = httpUtility.sendRequest("GET", url, params, headers);
            Assert.assertEquals(expected, response);
        } catch (IOException e) {
            fail(INVALID_EXCEPTION_THROWN);
        } catch (SkyflowException e) {
            fail(INVALID_EXCEPTION_THROWN);
        } catch (Exception e) {
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest({ URL.class, HttpURLConnection.class })
    public void testSendRequestFormURLEncoded() {
        try {
            given(mockConnection.getRequestProperty("content-type")).willReturn("application/x-www-form-urlencoded");

            JSONObject headers = new JSONObject();
            headers.put("content-type", "application/x-www-form-urlencoded");

            JSONObject params = new JSONObject();
            params.put("key", "value");

            String response = httpUtility.sendRequest("GET", url, params, headers);
            Assert.assertEquals(expected, response);
        } catch (IOException e) {
            fail(INVALID_EXCEPTION_THROWN);
        } catch (SkyflowException e) {
            fail(INVALID_EXCEPTION_THROWN);
        } catch (Exception e) {
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest({ URL.class, HttpURLConnection.class })
    public void testSendRequestError() {
        try {
            given(mockConnection.getResponseCode()).willReturn(500);

            String response = httpUtility.sendRequest("GET", url, null, null);
        } catch (IOException e) {
            fail(INVALID_EXCEPTION_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(expectedError, e.getMessage());
        } catch (Exception e) {
            fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
