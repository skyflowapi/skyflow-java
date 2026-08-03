package com.skyflow.vault.data;

import com.skyflow.enums.CustomHeaderKey;

import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * The interceptor context: its operation, its custom headers, and the batch position.
 *
 * <p>Batch position matters without it every batch of a bulk call presents an
 * identical context, so a caller cannot tag them apart — no per-batch correlation id, no
 * "batch 3 of 12" logging.
 */
public class RequestContextTests {

    @Test
    public void testBatchedConstructor_reportsThePosition() {
        RequestContext context = new RequestContext("INSERT", 2, 5);

        Assert.assertEquals("INSERT", context.getOperation());
        Assert.assertEquals(2, context.getBatchIndex());
        Assert.assertEquals(5, context.getTotalBatches());
    }

    @Test
    public void testSingleArgConstructor_reportsNotBatched() {
        // Kept for source compatibility; -1 distinguishes "not batched" from "the first batch".
        RequestContext context = new RequestContext("INSERT");

        Assert.assertEquals("INSERT", context.getOperation());
        Assert.assertEquals(-1, context.getBatchIndex());
        Assert.assertEquals(-1, context.getTotalBatches());
    }

    @Test
    public void testFirstBatchIsZeroNotMinusOne() {
        Assert.assertEquals(0, new RequestContext("INSERT", 0, 1).getBatchIndex());
    }

    @Test
    public void testHeadersStillWorkAlongsideTheBatchPosition() {
        RequestContext context = new RequestContext("DETOKENIZE", 1, 3);
        context.addHeader(CustomHeaderKey.REQUEST_ID_HEADER, "req-" + context.getBatchIndex());

        Assert.assertEquals("req-1", context.getHeaders().get(CustomHeaderKey.REQUEST_ID_HEADER));
    }

    @Test
    public void testHeadersRemainUnmodifiable() {
        RequestContext context = new RequestContext("INSERT", 0, 1);
        try {
            context.getHeaders().put(CustomHeaderKey.REQUEST_ID_HEADER, "x");
            Assert.fail("the exposed header map must not be mutable");
        } catch (UnsupportedOperationException expected) {
            Assert.assertTrue(true);
        }
    }

    // ── operation and headers (moved here with the class, from common) ──────────
    @Test
    public void testGetOperationReturnsConstructorValue() {
        RequestContext context = new RequestContext("INSERT");

        Assert.assertEquals("INSERT", context.getOperation());
    }

    @Test
    public void testNullOperation() {
        RequestContext context = new RequestContext(null);

        Assert.assertNull(context.getOperation());
    }

    @Test
    public void testGetHeadersReturnsEmptyMapByDefault() {
        RequestContext context = new RequestContext("INSERT");

        Assert.assertTrue(context.getHeaders().isEmpty());
    }

    @Test
    public void testAddHeaderIsReflectedInGetHeaders() {
        RequestContext context = new RequestContext("INSERT");
        context.addHeader(CustomHeaderKey.SKYFLOW_ACCOUNT_ID, "account-id-value");

        Map<CustomHeaderKey, String> headers = context.getHeaders();

        Assert.assertEquals(1, headers.size());
        Assert.assertEquals("account-id-value", headers.get(CustomHeaderKey.SKYFLOW_ACCOUNT_ID));
    }

    @Test
    public void testAddHeaderOverwritesExistingValueForSameKey() {
        RequestContext context = new RequestContext("INSERT");
        context.addHeader(CustomHeaderKey.SKYFLOW_ACCOUNT_ID, "first-value");
        context.addHeader(CustomHeaderKey.SKYFLOW_ACCOUNT_ID, "second-value");

        Assert.assertEquals(1, context.getHeaders().size());
        Assert.assertEquals("second-value", context.getHeaders().get(CustomHeaderKey.SKYFLOW_ACCOUNT_ID));
    }

    @Test
    public void testAddMultipleDistinctHeaders() {
        RequestContext context = new RequestContext("DETOKENIZE");
        context.addHeader(CustomHeaderKey.SKYFLOW_ACCOUNT_ID, "account-id-value");
        context.addHeader(CustomHeaderKey.SKYFLOW_ACCOUNT_NAME, "account-name-value");

        Assert.assertEquals(2, context.getHeaders().size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetHeadersReturnsUnmodifiableMap() {
        RequestContext context = new RequestContext("INSERT");

        context.getHeaders().put(CustomHeaderKey.REQUEST_ID_HEADER, "request-id-value");
    }
}
