package com.skyflow.vault.data;

import com.skyflow.enums.CustomHeaderKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class RequestContextTests {

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
        context.addHeader(CustomHeaderKey.SkyflowAccountId, "account-id-value");

        Map<CustomHeaderKey, String> headers = context.getHeaders();

        Assert.assertEquals(1, headers.size());
        Assert.assertEquals("account-id-value", headers.get(CustomHeaderKey.SkyflowAccountId));
    }

    @Test
    public void testAddHeaderOverwritesExistingValueForSameKey() {
        RequestContext context = new RequestContext("INSERT");
        context.addHeader(CustomHeaderKey.SkyflowAccountId, "first-value");
        context.addHeader(CustomHeaderKey.SkyflowAccountId, "second-value");

        Assert.assertEquals(1, context.getHeaders().size());
        Assert.assertEquals("second-value", context.getHeaders().get(CustomHeaderKey.SkyflowAccountId));
    }

    @Test
    public void testAddMultipleDistinctHeaders() {
        RequestContext context = new RequestContext("DETOKENIZE");
        context.addHeader(CustomHeaderKey.SkyflowAccountId, "account-id-value");
        context.addHeader(CustomHeaderKey.SkyflowAccountName, "account-name-value");

        Assert.assertEquals(2, context.getHeaders().size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetHeadersReturnsUnmodifiableMap() {
        RequestContext context = new RequestContext("INSERT");

        context.getHeaders().put(CustomHeaderKey.RequestIdHeader, "request-id-value");
    }
}
