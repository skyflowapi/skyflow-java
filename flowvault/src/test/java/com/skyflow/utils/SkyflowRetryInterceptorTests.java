package com.skyflow.utils;

import okhttp3.Response;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

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

    // ── intercept(): the retry loop ───────────────────────────────────────────
    // Delays are set to 0 so these do not actually sleep.

    private static SkyflowRetryInterceptor retrying(int maxRetries) {
        return new SkyflowRetryInterceptor(maxRetries, 0L, 0L);
    }

    @Test
    public void testIntercept_successFirstTimeIsNotRetried() throws IOException {
        FakeChain chain = new FakeChain(200);

        Response response = retrying(3).intercept(chain);

        Assert.assertEquals(1, chain.calls());
        Assert.assertEquals(200, response.code());
    }

    @Test
    public void testIntercept_nonRetryableFailureIsNotRetried() throws IOException {
        FakeChain chain = new FakeChain(400);

        Response response = retrying(3).intercept(chain);

        Assert.assertEquals("a 400 must not be replayed", 1, chain.calls());
        Assert.assertEquals(400, response.code());
    }

    @Test
    public void testIntercept_retriesUpToTheBudgetThenReturnsTheLastFailure() throws IOException {
        FakeChain chain = new FakeChain(500);

        Response response = retrying(2).intercept(chain);

        Assert.assertEquals("1 initial attempt + 2 retries", 3, chain.calls());
        Assert.assertEquals(500, response.code());
    }

    @Test
    public void testIntercept_stopsAsSoonAsAnAttemptSucceeds() throws IOException {
        FakeChain chain = new FakeChain(503, 200, 200);

        Response response = retrying(5).intercept(chain);

        Assert.assertEquals("must not keep retrying after success", 2, chain.calls());
        Assert.assertEquals(200, response.code());
    }

    @Test
    public void testIntercept_stopsOnANonRetryableStatusMidWay() throws IOException {
        FakeChain chain = new FakeChain(500, 404, 200);

        Response response = retrying(5).intercept(chain);

        Assert.assertEquals(2, chain.calls());
        Assert.assertEquals(404, response.code());
    }

    @Test
    public void testIntercept_zeroBudgetMeansNoRetryAtAll() throws IOException {
        FakeChain chain = new FakeChain(500);

        Response response = retrying(0).intercept(chain);

        Assert.assertEquals(1, chain.calls());
        Assert.assertEquals(500, response.code());
    }

    @Test
    public void testIntercept_retriesEachRetryableStatus() throws IOException {
        for (int code : new int[] {408, 429, 500, 502, 503}) {
            FakeChain chain = new FakeChain(code, 200);

            Response response = retrying(1).intercept(chain);

            Assert.assertEquals("should have retried a " + code, 2, chain.calls());
            Assert.assertEquals(200, response.code());
        }
    }

    @Test
    public void testIntercept_closesEverySupersededResponse() throws IOException {
        // Leaking the body of a response we are about to discard would leak the connection.
        FakeChain chain = new FakeChain(500, 500, 200);

        retrying(2).intercept(chain);

        Assert.assertEquals(3, chain.bodies().size());
        Assert.assertTrue("first failed response not closed", chain.bodies().get(0).closed);
        Assert.assertTrue("second failed response not closed", chain.bodies().get(1).closed);
        Assert.assertFalse("the returned response must stay open", chain.bodies().get(2).closed);
    }

    @Test
    public void testIntercept_retryBudgetIsPerCallNotPerInterceptorInstance() throws IOException {
        // The generated RetryInterceptor keeps its backoff counter on the instance, so one shared
        // instance exhausts the budget once for the whole client. A single interceptor is installed
        // on a shared OkHttpClient, so every call must get its own full budget.
        SkyflowRetryInterceptor retry = retrying(2);

        FakeChain first = new FakeChain(500);
        retry.intercept(first);
        FakeChain second = new FakeChain(500);
        retry.intercept(second);
        FakeChain third = new FakeChain(500);
        retry.intercept(third);

        Assert.assertEquals(3, first.calls());
        Assert.assertEquals("second call lost its retry budget", 3, second.calls());
        Assert.assertEquals("third call lost its retry budget", 3, third.calls());
    }

    @Test
    public void testIntercept_interruptionSurfacesAsIOException() {
        SkyflowRetryInterceptor retry = new SkyflowRetryInterceptor(2, 5_000L, 5_000L);
        FakeChain chain = new FakeChain(500);

        Thread.currentThread().interrupt();
        try {
            retry.intercept(chain);
            Assert.fail("an interrupt while backing off should surface as IOException");
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().contains("Interrupted"));
            Assert.assertTrue("the interrupt flag must be restored", Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();  // clear the flag so it cannot leak into another test
        }
    }

    @Test
    public void testBackoff_growsPastHalfTheCapInOneStep() {
        // initial > max/2, so the next step clamps straight to the cap instead of doubling past it.
        SkyflowRetryInterceptor retry = new SkyflowRetryInterceptor(3, 300L, 400L);

        assertWithinJitter(300L, retry.backoffMillis(1));
        assertWithinJitter(400L, retry.backoffMillis(2));
    }

    private static void assertWithinJitter(long expected, long actual) {
        long jitter = (long) (expected * 0.2);
        Assert.assertTrue("expected ~" + expected + " (+/-" + jitter + ") but got " + actual,
                actual >= expected - jitter && actual <= expected + jitter);
    }
}
