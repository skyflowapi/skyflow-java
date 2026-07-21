package com.skyflow.enums;

import org.junit.Assert;
import org.junit.Test;

public class MaskingMethodTest {

    @Test
    public void testBlackbox() {
        Assert.assertEquals("blackbox", MaskingMethod.BLACKBOX.getMaskingMethod());
        Assert.assertEquals("blackbox", MaskingMethod.BLACKBOX.toString());
    }

    @Test
    public void testBlur() {
        Assert.assertEquals("blur", MaskingMethod.BLUR.getMaskingMethod());
        Assert.assertEquals("blur", MaskingMethod.BLUR.toString());
    }
}
