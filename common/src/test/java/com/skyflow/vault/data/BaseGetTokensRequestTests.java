package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

public class BaseGetTokensRequestTests {

    @Test
    public void testInstantiationDoesNotThrow() {
        BaseGetTokensRequest request = new BaseGetTokensRequest(new BaseGetTokensRequest.BaseGetTokensRequestBuilder());

        Assert.assertNotNull(request);
    }

    @Test
    public void testUsableAsExtensionPointForSubclasses() {
        // BaseGetTokensRequest carries no state of its own; it exists so module-specific
        // GetTokensRequest classes share a supertype and builder. Verify the subtype relationship holds.
        BaseGetTokensRequest.BaseGetTokensRequestBuilder builder = new BaseGetTokensRequest.BaseGetTokensRequestBuilder() {
        };
        BaseGetTokensRequest request = new BaseGetTokensRequest(builder) {
        };

        Assert.assertTrue(request instanceof BaseGetTokensRequest);
    }
}
