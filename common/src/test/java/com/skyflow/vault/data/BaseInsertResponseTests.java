package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

public class BaseInsertResponseTests {

    @Test
    public void testInstantiationDoesNotThrow() {
        BaseInsertResponse response = new BaseInsertResponse();

        Assert.assertNotNull(response);
    }

    @Test
    public void testUsableAsExtensionPointForSubclasses() {
        // BaseInsertResponse carries no state of its own; it exists purely so module-specific
        // InsertResponse classes (v2's and flowvault's) share a supertype. Verify the subtype relationship holds.
        BaseInsertResponse response = new BaseInsertResponse() {
        };

        Assert.assertTrue(response instanceof BaseInsertResponse);
    }
}
