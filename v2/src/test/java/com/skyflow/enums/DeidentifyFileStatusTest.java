package com.skyflow.enums;

import org.junit.Assert;
import org.junit.Test;

public class DeidentifyFileStatusTest {

    @Test
    public void testInProgress() {
        Assert.assertEquals("IN_PROGRESS", DeidentifyFileStatus.IN_PROGRESS.value());
    }

    @Test
    public void testFailed() {
        Assert.assertEquals("FAILED", DeidentifyFileStatus.FAILED.value());
    }

    @Test
    public void testSuccess() {
        Assert.assertEquals("SUCCESS", DeidentifyFileStatus.SUCCESS.value());
    }

    @Test
    public void testUnknown() {
        Assert.assertEquals("UNKNOWN", DeidentifyFileStatus.UNKNOWN.value());
    }
}
