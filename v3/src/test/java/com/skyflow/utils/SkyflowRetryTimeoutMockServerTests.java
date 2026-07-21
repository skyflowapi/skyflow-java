package com.skyflow.utils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Live socket-level proof of the timeout + retry behavior: builds an OkHttpClient the same way
 * {@code VaultClient.updateExecutorInHTTP()} does (callTimeout + {@link SkyflowRetryInterceptor})
 * and drives it against a real loopback {@link MockWebServer}.
 */
public class SkyflowRetryTimeoutMockServerTests {

    private MockWebServer server;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    /** Mirrors VaultClient's client build: overall callTimeout + our retry interceptor. */
    private OkHttpClient client(int timeoutSeconds, int maxRetries, long initialBackoffMs, long maxBackoffMs) {
        return new OkHttpClient.Builder()
                .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .addInterceptor(new SkyflowRetryInterceptor(maxRetries, initialBackoffMs, maxBackoffMs))
                .build();
    }

    private Request request() {
        return new Request.Builder().url(server.url("/v1/detokenize")).build();
    }

    @Test
    public void retriesTransientFailuresThenSucceeds() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}"));

        try (Response response = client(10, 3, 10L, 40L).newCall(request()).execute()) {
            Assert.assertEquals(200, response.code());
        }
        // 1 initial attempt + 2 retries = 3 requests actually hit the server
        Assert.assertEquals(3, server.getRequestCount());
    }

    @Test
    public void exhaustsRetriesAndReturnsLastFailure() throws Exception {
        for (int i = 0; i < 4; i++) {
            server.enqueue(new MockResponse().setResponseCode(503));
        }
        try (Response response = client(10, 3, 10L, 40L).newCall(request()).execute()) {
            Assert.assertEquals(503, response.code());
        }
        // 1 initial attempt + 3 retries = 4 requests
        Assert.assertEquals(4, server.getRequestCount());
    }

    @Test
    public void doesNotRetryNonRetryableStatus() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(400));
        try (Response response = client(10, 3, 10L, 40L).newCall(request()).execute()) {
            Assert.assertEquals(400, response.code());
        }
        Assert.assertEquals(1, server.getRequestCount()); // no retry on 400
    }

    @Test
    public void callTimeoutAbortsHangingResponse() {
        // Server accepts the connection but never responds -> the call must abort at callTimeout.
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

        long startMs = System.currentTimeMillis();
        try {
            client(1, 0, 10L, 40L).newCall(request()).execute(); // callTimeout = 1s, no retries
            Assert.fail("expected the hanging request to time out");
        } catch (IOException expected) {
            long elapsedMs = System.currentTimeMillis() - startMs;
            // Bounded near the 1s callTimeout, NOT hanging indefinitely (the reported bug).
            Assert.assertTrue("call should abort near callTimeout; took " + elapsedMs + "ms", elapsedMs < 4000);
        }
    }

    @Test
    public void backoffDelaysBetweenRetries() throws Exception {
        // Two failures then success; with initialBackoff=200ms, total backoff >= ~200+400 (minus jitter).
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setResponseCode(200));

        long startMs = System.currentTimeMillis();
        try (Response response = client(10, 3, 200L, 2000L).newCall(request()).execute()) {
            Assert.assertEquals(200, response.code());
        }
        long elapsedMs = System.currentTimeMillis() - startMs;
        // Proportional jitter is 0.9-1.1x, so lower bound of two backoffs ~ (200 + 400) * 0.9 = 540ms.
        Assert.assertTrue("expected observable backoff between retries; took " + elapsedMs + "ms", elapsedMs >= 400);
    }
}
