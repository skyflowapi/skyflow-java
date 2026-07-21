package com.skyflow.utils;

import java.io.IOException;
import java.util.Random;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * Retry interceptor for the v3 SDK's shared {@link okhttp3.OkHttpClient}.
 *
 * <p>Replicates Fern's retry algorithm (exponential backoff + proportional jitter) so behavior
 * stays familiar, while exposing {@code maxRetries}, {@code initialRetryDelayMillis} and
 * {@code maxRetryDelayMillis} as configurable knobs. The jitter factor is fixed at 0.2 (±10%) and
 * intentionally not exposed. Retries fire on HTTP 408 / 429 / 5xx responses only (same triggers as
 * Fern); it does not retry connection failures / {@link IOException}.
 *
 * <p><b>Why this is hand-written instead of reusing Fern's generated {@code RetryInterceptor}:</b>
 * the SDK injects its own {@code OkHttpClient} (for the auth header + connection pool), so Fern's
 * generated interceptor is never wired by {@code ClientOptions.build()} (it is only added on the
 * no-custom-client branch); and the currently vendored Fern interceptor hardcodes its backoff
 * (only {@code maxRetries} is configurable), so it cannot honor the delay knobs above. See
 * {@code docs/superpowers/specs/2026-07-21-CUST-4311-v3-configurable-timeouts-retries-design.md}.
 *
 * <p><b>TODO(CUST-4311):</b> replace this class with Fern's generated {@code RetryInterceptor}
 * once the SDK is regenerated with a Fern version whose interceptor (a) exposes
 * {@code initialRetryDelayMillis} / {@code maxRetryDelayMillis} and (b) is constructable
 * standalone so it can be attached to our custom client. Target Fern/generator version: v_____
 * (fill in when known).
 */
public final class SkyflowRetryInterceptor implements Interceptor {

    /** Fixed jitter factor (±10%). Intentionally not exposed as a public knob (matches Fern's default). */
    private static final double JITTER_FACTOR = 0.2;

    private final int maxRetries;
    private final long initialRetryDelayMillis;
    private final long maxRetryDelayMillis;
    private final Random random = new Random();

    public SkyflowRetryInterceptor(int maxRetries, long initialRetryDelayMillis, long maxRetryDelayMillis) {
        this.maxRetries = maxRetries;
        this.initialRetryDelayMillis = initialRetryDelayMillis;
        this.maxRetryDelayMillis = maxRetryDelayMillis;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());

        int attempt = 0;
        while (shouldRetry(response.code()) && attempt < maxRetries) {
            attempt++;
            sleep(backoffMillis(attempt));
            response.close(); // release the failed response/connection before retrying
            response = chain.proceed(chain.request());
        }
        return response;
    }

    /**
     * Exponential backoff, capped, then proportional jitter (same shape as Fern):
     * {@code base(n) = min(initialRetryDelayMillis * 2^(n-1), maxRetryDelayMillis)},
     * {@code delay = base * (1 + (rand - 0.5) * JITTER_FACTOR)} — i.e. {@code base * [0.9, 1.1]}.
     * Jitter is applied after the cap, so the returned delay can exceed {@code maxRetryDelayMillis}
     * by up to the jitter factor.
     */
    private long backoffMillis(int attempt) {
        double base = Math.min(initialRetryDelayMillis * Math.pow(2, attempt - 1), maxRetryDelayMillis);
        double jitter = 1.0 + ((random.nextDouble() - 0.5) * JITTER_FACTOR);
        return (long) (base * jitter);
    }

    private static boolean shouldRetry(int statusCode) {
        return statusCode == 408 || statusCode == 429 || statusCode >= 500;
    }

    private void sleep(long millis) throws IOException {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted during retry backoff", e);
        }
    }
}
