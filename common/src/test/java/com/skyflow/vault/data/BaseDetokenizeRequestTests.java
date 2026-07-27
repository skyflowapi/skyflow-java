package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

public class BaseDetokenizeRequestTests {

    @Test
    public void testInstantiationDoesNotThrow() {
        BaseDetokenizeRequest request = new BaseDetokenizeRequest();

        Assert.assertNotNull(request);
    }

    @Test
    public void testUsableAsExtensionPointForSubclasses() {
        // BaseDetokenizeRequest carries no state of its own; it exists purely so module-specific
        // DetokenizeRequest classes (e.g. flowvault's) can extend it. Verify the subtype relationship holds.
        BaseDetokenizeRequest request = new BaseDetokenizeRequest() {
        };

        Assert.assertTrue(request instanceof BaseDetokenizeRequest);
    }
}
