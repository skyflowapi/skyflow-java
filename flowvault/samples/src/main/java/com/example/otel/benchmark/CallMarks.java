package com.example.otel.benchmark;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

/**
 * An OkHttp {@link EventListener} that does nothing but stamp {@link System#nanoTime()} into
 * fields.
 *
 * <p>This is the raw-HTTP path's equivalent of a profiler, and it is deliberately as close to
 * free as instrumentation gets: no allocation per event, no locking, no formatting. Spans are
 * built from these fields after the call returns, so the listener never puts OpenTelemetry work
 * inside the window being measured.
 *
 * <p>A pooled connection means {@code dns*} and {@code connect*} never fire, so those fields stay
 * at {@link #UNSET}. That is the normal case once the pool warms up, and it is what makes the
 * comparison with the SDK fair — the SDK reuses connections from its own pool too.
 */
public final class CallMarks extends EventListener {

    public static final long UNSET = Long.MIN_VALUE;

    /** Set by the factory on the calling thread; read back by the caller after execute(). */
    private static final ThreadLocal<CallMarks> CURRENT = new ThreadLocal<>();

    public long callStart = UNSET;
    public long dnsStart = UNSET;
    public long dnsEnd = UNSET;
    public long connectStart = UNSET;
    public long connectEnd = UNSET;
    public long connectionAcquired = UNSET;
    public long requestHeadersStart = UNSET;
    public long requestBodyEnd = UNSET;
    public long requestEnd = UNSET;
    public long responseHeadersStart = UNSET;
    public long responseBodyEnd = UNSET;
    public long callEnd = UNSET;

    /**
     * Install on an {@link okhttp3.OkHttpClient.Builder}. Synchronous calls run the factory and
     * the whole exchange on one thread, so a ThreadLocal is enough to hand the marks back.
     */
    public static EventListener.Factory factory() {
        return call -> {
            CallMarks marks = new CallMarks();
            CURRENT.set(marks);
            return marks;
        };
    }

    /** The marks for the call this thread just executed, or null if none were recorded. */
    public static CallMarks takeCurrent() {
        CallMarks marks = CURRENT.get();
        CURRENT.remove();
        return marks;
    }

    /** True when both ends of an interval were observed, so a span can be built from it. */
    public boolean has(long start, long end) {
        return start != UNSET && end != UNSET && end >= start;
    }

    /**
     * The last moment the client was still doing work before waiting on the server. Falls back
     * through the request-side marks because a request without a body never reports a body end.
     */
    public long requestFinished() {
        if (requestEnd != UNSET) {
            return requestEnd;
        }
        if (requestBodyEnd != UNSET) {
            return requestBodyEnd;
        }
        return requestHeadersStart;
    }

    @Override public void callStart(Call call) { callStart = System.nanoTime(); }
    @Override public void dnsStart(Call call, String domainName) { dnsStart = System.nanoTime(); }

    @Override
    public void dnsEnd(Call call, String domainName, List<InetAddress> inetAddressList) {
        dnsEnd = System.nanoTime();
    }

    @Override
    public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
        connectStart = System.nanoTime();
    }

    @Override
    public void connectEnd(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol) {
        connectEnd = System.nanoTime();
    }

    @Override
    public void connectFailed(Call call, InetSocketAddress inetSocketAddress, Proxy proxy,
                              Protocol protocol, IOException ioe) {
        connectEnd = System.nanoTime();
    }

    @Override
    public void secureConnectStart(Call call) { /* TLS timing folds into connectStart..connectEnd */ }

    @Override
    public void secureConnectEnd(Call call, Handshake handshake) { /* see secureConnectStart */ }

    @Override
    public void connectionAcquired(Call call, Connection connection) {
        connectionAcquired = System.nanoTime();
    }

    @Override public void requestHeadersStart(Call call) { requestHeadersStart = System.nanoTime(); }
    @Override public void requestHeadersEnd(Call call, Request request) { requestEnd = System.nanoTime(); }
    @Override public void requestBodyEnd(Call call, long byteCount) {
        requestBodyEnd = System.nanoTime();
        requestEnd = requestBodyEnd;
    }

    @Override public void responseHeadersStart(Call call) { responseHeadersStart = System.nanoTime(); }
    @Override public void responseBodyEnd(Call call, long byteCount) { responseBodyEnd = System.nanoTime(); }

    @Override public void callEnd(Call call) { callEnd = System.nanoTime(); }
    @Override public void callFailed(Call call, IOException ioe) { callEnd = System.nanoTime(); }
}
