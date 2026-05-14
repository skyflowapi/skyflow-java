package com.skyflow.vault.data;

import com.skyflow.enums.CustomHeaderKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class RequestContextTests {

    @Test
    public void constructor_setsOperationBatchIndexAndTotalBatches() {
        RequestContext ctx = new RequestContext("INSERT", 2, 5);
        Assert.assertEquals("INSERT", ctx.getOperation());
        Assert.assertEquals(2, ctx.getBatchIndex());
        Assert.assertEquals(5, ctx.getTotalBatches());
    }

    @Test
    public void constructor_headersMapIsInitiallyEmpty() {
        RequestContext ctx = new RequestContext("DETOKENIZE", 0, 1);
        Assert.assertTrue(ctx.getHeaders().isEmpty());
    }

    @Test
    public void addHeader_singleEntry_appearsInGetHeaders() {
        RequestContext ctx = new RequestContext("TOKENIZE", 0, 1);
        ctx.addHeader(CustomHeaderKey.RequestIDHeader, "req-abc");
        Map<CustomHeaderKey, String> headers = ctx.getHeaders();
        Assert.assertEquals(1, headers.size());
        Assert.assertEquals("req-abc", headers.get(CustomHeaderKey.RequestIDHeader));
    }

    @Test
    public void addHeader_multipleEntries_allPresentInGetHeaders() {
        RequestContext ctx = new RequestContext("DELETE_TOKENS", 1, 3);
        ctx.addHeader(CustomHeaderKey.SkyflowAccountID, "acct-1");
        ctx.addHeader(CustomHeaderKey.SkyflowAccountName, "my-account");
        ctx.addHeader(CustomHeaderKey.RequestIDHeader, "req-xyz");
        Map<CustomHeaderKey, String> headers = ctx.getHeaders();
        Assert.assertEquals(3, headers.size());
        Assert.assertEquals("acct-1", headers.get(CustomHeaderKey.SkyflowAccountID));
        Assert.assertEquals("my-account", headers.get(CustomHeaderKey.SkyflowAccountName));
        Assert.assertEquals("req-xyz", headers.get(CustomHeaderKey.RequestIDHeader));
    }

    @Test
    public void getHeaders_returnsUnmodifiableView() {
        RequestContext ctx = new RequestContext("INSERT", 0, 1);
        ctx.addHeader(CustomHeaderKey.RequestIDHeader, "val");
        Map<CustomHeaderKey, String> headers = ctx.getHeaders();
        try {
            headers.put(CustomHeaderKey.SkyflowAccountID, "extra");
            Assert.fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected — map is unmodifiable
        }
    }

    @Test
    public void addHeader_overwrites_existingKey() {
        RequestContext ctx = new RequestContext("INSERT", 0, 1);
        ctx.addHeader(CustomHeaderKey.RequestIDHeader, "first");
        ctx.addHeader(CustomHeaderKey.RequestIDHeader, "second");
        Assert.assertEquals("second", ctx.getHeaders().get(CustomHeaderKey.RequestIDHeader));
    }

    @Test
    public void interceptor_receivesCorrectContext() {
        RequestContext[] captured = new RequestContext[1];
        RequestInterceptor interceptor = ctx -> captured[0] = ctx;

        RequestContext ctx = new RequestContext("TOKENIZE", 3, 7);
        interceptor.intercept(ctx);

        Assert.assertSame(ctx, captured[0]);
        Assert.assertEquals("TOKENIZE", captured[0].getOperation());
        Assert.assertEquals(3, captured[0].getBatchIndex());
        Assert.assertEquals(7, captured[0].getTotalBatches());
    }

    @Test
    public void interceptor_canAddHeadersToContext() {
        RequestInterceptor interceptor = ctx ->
                ctx.addHeader(CustomHeaderKey.SkyflowAccountID, "injected-account");

        RequestContext ctx = new RequestContext("INSERT", 0, 2);
        interceptor.intercept(ctx);

        Assert.assertEquals("injected-account", ctx.getHeaders().get(CustomHeaderKey.SkyflowAccountID));
    }
}
