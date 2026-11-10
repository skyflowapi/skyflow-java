package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

public class BaseUploadFilesRequestTests {

    @Test
    public void testInstantiationDoesNotThrow() {
        BaseUploadFilesRequest request = new BaseUploadFilesRequest(new BaseUploadFilesRequest.BaseUploadFilesRequestBuilder());

        Assert.assertNotNull(request);
    }

    @Test
    public void testUsableAsExtensionPointForSubclasses() {
        // BaseUploadFilesRequest carries no state of its own; it exists so module-specific
        // UploadFilesRequest classes share a supertype and builder. Verify the subtype relationship holds.
        BaseUploadFilesRequest.BaseUploadFilesRequestBuilder builder = new BaseUploadFilesRequest.BaseUploadFilesRequestBuilder() {
        };
        BaseUploadFilesRequest request = new BaseUploadFilesRequest(builder) {
        };

        Assert.assertTrue(request instanceof BaseUploadFilesRequest);
    }
}
