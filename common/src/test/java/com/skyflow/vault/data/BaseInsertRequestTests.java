package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

public class BaseInsertRequestTests {

    @Test
    public void testInstantiationDoesNotThrow() {
        BaseInsertRequest request = new BaseInsertRequest();

        Assert.assertNotNull(request);
    }

    @Test
    public void testUsableAsExtensionPointForSubclasses() {
        // BaseInsertRequest carries no state of its own; it exists purely so module-specific
        // InsertRequest classes (v2's and flowvault's) share a supertype. Verify the subtype relationship holds.
        BaseInsertRequest request = new BaseInsertRequest() {
        };

        Assert.assertTrue(request instanceof BaseInsertRequest);
    }
}
