package com.skyflow.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SkyflowRetryInterceptorTests {

    private Request request;

    @Before
    public void setUp() {
        request = new Request.Builder().url("https://example.com").build();
    }

    private Response response(int code) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("mock")
                .body(ResponseBody.create("", (MediaType) null))
                .build();
    }

    // Tiny backoff so tests stay fast (1-2ms sleeps).
    private SkyflowRetryInterceptor interceptor(int maxRetries) {
        return new SkyflowRetryInterceptor(maxRetries, 1L, 2L);
    }

    @Test
    public void retriesUpToMaxRetriesThenReturnsLastResponse() throws IOException {
        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        when(chain.request()).thenReturn(request);
        // always 503 -> initial attempt + 2 retries = 3 proceeds
        when(chain.proceed(any(Request.class))).thenReturn(response(503), response(503), response(503));

        Response result = interceptor(2).intercept(chain);

        verify(chain, times(3)).proceed(any(Request.class));
        Assert.assertEquals(503, result.code());
    }

    @Test
    public void stopsRetryingOnFirstSuccess() throws IOException {
        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        when(chain.request()).thenReturn(request);
        when(chain.proceed(any(Request.class))).thenReturn(response(503), response(200));

        Response result = interceptor(2).intercept(chain);

        verify(chain, times(2)).proceed(any(Request.class)); // 1 retry, then success
        Assert.assertEquals(200, result.code());
    }

    @Test
    public void doesNotRetryNonRetryableStatus() throws IOException {
        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        when(chain.request()).thenReturn(request);
        when(chain.proceed(any(Request.class))).thenReturn(response(400));

        Response result = interceptor(2).intercept(chain);

        verify(chain, times(1)).proceed(any(Request.class)); // no retry on 400
        Assert.assertEquals(400, result.code());
    }

    @Test
    public void retriesOn429() throws IOException {
        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        when(chain.request()).thenReturn(request);
        when(chain.proceed(any(Request.class))).thenReturn(response(429), response(200));

        Response result = interceptor(2).intercept(chain);

        verify(chain, times(2)).proceed(any(Request.class));
        Assert.assertEquals(200, result.code());
    }

    @Test
    public void retriesOn408() throws IOException {
        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        when(chain.request()).thenReturn(request);
        when(chain.proceed(any(Request.class))).thenReturn(response(408), response(200));

        Response result = interceptor(2).intercept(chain);

        verify(chain, times(2)).proceed(any(Request.class));
        Assert.assertEquals(200, result.code());
    }

    @Test
    public void zeroMaxRetriesMeansSingleAttempt() throws IOException {
        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        when(chain.request()).thenReturn(request);
        when(chain.proceed(any(Request.class))).thenReturn(response(503));

        Response result = interceptor(0).intercept(chain);

        verify(chain, times(1)).proceed(any(Request.class)); // no retries
        Assert.assertEquals(503, result.code());
    }
}
