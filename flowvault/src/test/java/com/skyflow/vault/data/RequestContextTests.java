package com.skyflow.vault.data;

import com.skyflow.enums.CustomHeaderKey;
import org.junit.Assert;
import org.junit.Test;

/**
 * Batch position on the interceptor context. Without it every batch of a bulk call presents an
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
        context.addHeader(CustomHeaderKey.RequestIdHeader, "req-" + context.getBatchIndex());

        Assert.assertEquals("req-1", context.getHeaders().get(CustomHeaderKey.RequestIdHeader));
    }

    @Test
    public void testHeadersRemainUnmodifiable() {
        RequestContext context = new RequestContext("INSERT", 0, 1);
        try {
            context.getHeaders().put(CustomHeaderKey.RequestIdHeader, "x");
            Assert.fail("the exposed header map must not be mutable");
        } catch (UnsupportedOperationException expected) {
            Assert.assertTrue(true);
        }
    }
}
