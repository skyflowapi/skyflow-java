package com.skyflow.config;

import org.junit.Assert;
import org.junit.Test;

public class ManagementConfigTest {

    @Test
    public void testInstantiation() {
        // Package-private constructor — accessible from same package
        ManagementConfig config = new ManagementConfig();
        Assert.assertNotNull(config);
    }
}
