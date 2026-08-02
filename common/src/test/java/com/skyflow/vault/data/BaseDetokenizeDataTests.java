package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

public class BaseDetokenizeDataTests {

    @Test
    public void testInstantiationDoesNotThrow() {
        BaseDetokenizeData data = new BaseDetokenizeData();

        Assert.assertNotNull(data);
    }

    @Test
    public void testUsableAsExtensionPointForSubclasses() {
        // BaseDetokenizeData carries no state of its own; it exists purely so module-specific
        // classes (v2's DetokenizeData, flowvault's TokenGroupRedactions) share a supertype.
        BaseDetokenizeData data = new BaseDetokenizeData() {
        };

        Assert.assertTrue(data instanceof BaseDetokenizeData);
    }
}
