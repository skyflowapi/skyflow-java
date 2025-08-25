package com.skyflow.utils;

import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;

public class UtilsTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";

    @Test
    public void testGetMetrics() {
        try {
            JsonObject metrics = Utils.getMetrics();
            String sdkVersion = Constants.SDK_VERSION;
            Assert.assertNotNull(metrics.get(Constants.SDK_METRIC_NAME_VERSION));
            Assert.assertEquals("skyflow-java@" + sdkVersion, metrics.get(Constants.SDK_METRIC_NAME_VERSION).getAsString());
            Assert.assertNotNull(metrics.get(Constants.SDK_METRIC_CLIENT_DEVICE_MODEL));
            Assert.assertNotNull(metrics.get(Constants.SDK_METRIC_CLIENT_OS_DETAILS));
            Assert.assertNotNull(metrics.get(Constants.SDK_METRIC_RUNTIME_DETAILS));
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
