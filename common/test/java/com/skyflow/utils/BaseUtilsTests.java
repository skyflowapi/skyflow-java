package com.skyflow.utils;

import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;

public class BaseUtilsTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";

    @Test
    public void testGetBaseMetrics() {
        try {
            JsonObject metrics = BaseUtils.getCommonMetrics();
            Assert.assertNotNull(metrics.get(BaseConstants.SDK_METRIC_RUNTIME_DETAILS).getAsString());
            Assert.assertNotNull(metrics.get(BaseConstants.SDK_METRIC_CLIENT_DEVICE_MODEL).getAsString());
            Assert.assertNotNull(metrics.get(BaseConstants.SDK_METRIC_CLIENT_OS_DETAILS).getAsString());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
