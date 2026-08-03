package com.skyflow.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Internals of the retry interceptor. Lives in com.skyflow.utils so the package-private
 * backoff/should-retry helpers stay off the public API surface.
 */
public class SkyflowRetryInterceptorTests {

    @Test
    public void testConstructor_rejectsNegativeMaxRetries() {
        try {
            new SkyflowRetryInterceptor(-1, 500L, 2000L);
            Assert.fail("negative maxRetries should be rejected");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("maxRetries"));
        }
    }

    @Test
    public void testConstructor_rejectsNegativeInitialDelay() {
        try {
            new SkyflowRetryInterceptor(1, -1L, 2000L);
            Assert.fail("negative initialRetryDelayMillis should be rejected");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("initialRetryDelayMillis"));
        }
    }

    @Test
    public void testConstructor_rejectsNegativeMaxDelay() {
        try {
            new SkyflowRetryInterceptor(1, 500L, -1L);
            Assert.fail("negative maxRetryDelayMillis should be rejected");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("maxRetryDelayMillis"));
        }
    }

    @Test
    public void testBackoff_growsExponentiallyThenCaps() {
        // Jitter is +/-20%, so assert bands rather than exact values.
        SkyflowRetryInterceptor retry = new SkyflowRetryInterceptor(5, 100L, 400L);

        assertWithinJitter(100L, retry.backoffMillis(1));
        assertWithinJitter(200L, retry.backoffMillis(2));
        assertWithinJitter(400L, retry.backoffMillis(3));
        assertWithinJitter(400L, retry.backoffMillis(4));
        assertWithinJitter(400L, retry.backoffMillis(10));
    }

    @Test
    public void testBackoff_neverExceedsTheCapAcrossManyDraws() {
        SkyflowRetryInterceptor retry = new SkyflowRetryInterceptor(5, 100L, 400L);

        for (int i = 0; i < 200; i++) {
            long delay = retry.backoffMillis(3);
            Assert.assertTrue("jittered delay went negative: " + delay, delay >= 0);
            Assert.assertTrue("jittered delay exceeded cap + jitter: " + delay, delay <= 480L);
        }
    }

    @Test
    public void testBackoff_zeroDelayStaysZero() {
        SkyflowRetryInterceptor retry = new SkyflowRetryInterceptor(3, 0L, 0L);

        Assert.assertEquals(0L, retry.backoffMillis(1));
        Assert.assertEquals(0L, retry.backoffMillis(5));
    }

    @Test
    public void testBackoff_initialDelayAboveCapIsClampedToCap() {
        SkyflowRetryInterceptor retry = new SkyflowRetryInterceptor(3, 5000L, 1000L);

        assertWithinJitter(1000L, retry.backoffMillis(1));
        assertWithinJitter(1000L, retry.backoffMillis(3));
    }

    @Test
    public void testShouldRetry_retryableStatuses() {
        Assert.assertTrue(SkyflowRetryInterceptor.shouldRetry(408));
        Assert.assertTrue(SkyflowRetryInterceptor.shouldRetry(429));
        Assert.assertTrue(SkyflowRetryInterceptor.shouldRetry(500));
        Assert.assertTrue(SkyflowRetryInterceptor.shouldRetry(502));
        Assert.assertTrue(SkyflowRetryInterceptor.shouldRetry(503));
    }

    @Test
    public void testShouldRetry_nonRetryableStatuses() {
        Assert.assertFalse(SkyflowRetryInterceptor.shouldRetry(200));
        Assert.assertFalse(SkyflowRetryInterceptor.shouldRetry(201));
        Assert.assertFalse(SkyflowRetryInterceptor.shouldRetry(400));
        Assert.assertFalse(SkyflowRetryInterceptor.shouldRetry(401));
        Assert.assertFalse(SkyflowRetryInterceptor.shouldRetry(404));
        Assert.assertFalse(SkyflowRetryInterceptor.shouldRetry(409));
    }

    @Test
    public void testAccessors_reportWhatWasConfigured() {
        SkyflowRetryInterceptor retry = new SkyflowRetryInterceptor(3, 100L, 900L);

        Assert.assertEquals(3, retry.getMaxRetries());
        Assert.assertEquals(100L, retry.getInitialRetryDelayMillis());
        Assert.assertEquals(900L, retry.getMaxRetryDelayMillis());
    }

    private static void assertWithinJitter(long expected, long actual) {
        long jitter = (long) (expected * 0.2);
        Assert.assertTrue("expected ~" + expected + " (+/-" + jitter + ") but got " + actual,
                actual >= expected - jitter && actual <= expected + jitter);
    }
}
