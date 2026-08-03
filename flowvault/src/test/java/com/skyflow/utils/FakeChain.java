package com.skyflow.utils;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Minimal Interceptor.Chain that replays a scripted list of status codes, so the retry loop can
 * be driven without a network or a mock-server dependency. Records how many times it was called
 * and which responses were closed.
 */
public final class FakeChain implements Interceptor.Chain {

    private final Request request =
            new Request.Builder().url("https://cluster1.example.com/v1/flows").build();
    private final int[] statusCodes;
    private final List<TrackingBody> bodies = new ArrayList<>();
    private int calls;
    private Request lastProceeded;

    public FakeChain(int... statusCodes) {
        this.statusCodes = statusCodes;
    }

    public int calls() {
        return calls;
    }

    /** Responses the interceptor superseded and should have closed. */
    public List<TrackingBody> bodies() {
        return bodies;
    }

    /** The request as it reached the next interceptor, i.e. after any rewriting. */
    public Request lastProceeded() {
        return lastProceeded;
    }

    @Override
    public Request request() {
        return request;
    }

    @Override
    public Response proceed(Request request) throws IOException {
        this.lastProceeded = request;
        // Past the end of the script, keep returning the last code.
        int code = statusCodes[Math.min(calls, statusCodes.length - 1)];
        calls++;
        TrackingBody body = new TrackingBody();
        bodies.add(body);
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("status " + code)
                .body(body)
                .build();
    }

    @Override
    public Connection connection() {
        return null;
    }

    @Override
    public Call call() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int connectTimeoutMillis() {
        return 0;
    }

    @Override
    public Interceptor.Chain withConnectTimeout(int timeout, TimeUnit unit) {
        return this;
    }

    @Override
    public int readTimeoutMillis() {
        return 0;
    }

    @Override
    public Interceptor.Chain withReadTimeout(int timeout, TimeUnit unit) {
        return this;
    }

    @Override
    public int writeTimeoutMillis() {
        return 0;
    }

    @Override
    public Interceptor.Chain withWriteTimeout(int timeout, TimeUnit unit) {
        return this;
    }

    public static final class TrackingBody extends ResponseBody {
        boolean closed;

        @Override
        public MediaType contentType() {
            return null;
        }

        @Override
        public long contentLength() {
            return 0;
        }

        @Override
        public BufferedSource source() {
            return new Buffer();
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
