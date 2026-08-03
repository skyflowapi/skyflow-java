package com.skyflow.utils;

import okhttp3.Interceptor;
import okhttp3.Response;

import java.io.IOException;
import java.util.Random;

/**
 * Retries failed requests with exponential backoff and jitter.
 * <p>
 * This exists as hand-written code rather than using the generated
 * {@code com.skyflow.generated.rest.core.RetryInterceptor} because that one only accepts a retry
 * count — it has no way to configure the backoff delays that {@code VaultConfig} exposes. It also
 * keeps its backoff counter on the interceptor instance, so a single shared instance exhausts its
 * retry budget once for the whole client rather than once per request; this implementation keeps
 * that state per call.
 * <p>
 * Retries the same statuses the generated interceptor does: 408, 429, and any 5xx.
 */
public final class SkyflowRetryInterceptor implements Interceptor {

    /** Fraction of the computed delay applied as random jitter, so retries do not align. */
    private static final double JITTER_FACTOR = 0.2;

    private final int maxRetries;
    private final long initialRetryDelayMillis;
    private final long maxRetryDelayMillis;
    private final Random random = new Random();

    public SkyflowRetryInterceptor(int maxRetries, long initialRetryDelayMillis, long maxRetryDelayMillis) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be non-negative");
        }
        if (initialRetryDelayMillis < 0) {
            throw new IllegalArgumentException("initialRetryDelayMillis must be non-negative");
        }
        if (maxRetryDelayMillis < 0) {
            throw new IllegalArgumentException("maxRetryDelayMillis must be non-negative");
        }
        this.maxRetries = maxRetries;
        this.initialRetryDelayMillis = initialRetryDelayMillis;
        this.maxRetryDelayMillis = maxRetryDelayMillis;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        // Retry budget is scoped to this call, not to the interceptor instance.
        for (int attempt = 1; attempt <= maxRetries && shouldRetry(response.code()); attempt++) {
            sleep(backoffMillis(attempt));
            response.close();
            response = chain.proceed(chain.request());
        }
        return response;
    }

    /** Exponential growth from the initial delay, capped at the maximum, then jittered. */
    long backoffMillis(int attempt) {
        long delay = initialRetryDelayMillis;
        for (int i = 1; i < attempt && delay < maxRetryDelayMillis; i++) {
            delay = delay > maxRetryDelayMillis / 2 ? maxRetryDelayMillis : delay * 2;
        }
        delay = Math.min(delay, maxRetryDelayMillis);
        long jitter = (long) (delay * JITTER_FACTOR);
        if (jitter <= 0) {
            return delay;
        }
        // delay +/- up to JITTER_FACTOR, never negative.
        return Math.max(0, delay - jitter + random.nextInt((int) Math.min(2 * jitter + 1, Integer.MAX_VALUE)));
    }

    static boolean shouldRetry(int statusCode) {
        return statusCode == 408 || statusCode == 429 || statusCode >= 500;
    }

    private static void sleep(long millis) throws IOException {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting to retry request", e);
        }
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getInitialRetryDelayMillis() {
        return initialRetryDelayMillis;
    }

    public long getMaxRetryDelayMillis() {
        return maxRetryDelayMillis;
    }
}
