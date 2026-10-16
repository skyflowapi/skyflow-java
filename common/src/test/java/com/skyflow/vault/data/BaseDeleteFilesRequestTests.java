package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

public class BaseDeleteFilesRequestTests {

    @Test
    public void testInstantiationDoesNotThrow() {
        BaseDeleteFilesRequest request = new BaseDeleteFilesRequest(new BaseDeleteFilesRequest.BaseDeleteFilesRequestBuilder());

        Assert.assertNotNull(request);
    }

    @Test
    public void testUsableAsExtensionPointForSubclasses() {
        // BaseDeleteFilesRequest carries no state of its own; it exists so module-specific
        // DeleteFilesRequest classes share a supertype and builder. Verify the subtype relationship holds.
        BaseDeleteFilesRequest.BaseDeleteFilesRequestBuilder builder = new BaseDeleteFilesRequest.BaseDeleteFilesRequestBuilder() {
        };
        BaseDeleteFilesRequest request = new BaseDeleteFilesRequest(builder) {
        };

        Assert.assertTrue(request instanceof BaseDeleteFilesRequest);
    }
}
