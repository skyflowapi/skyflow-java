package com.skyflow.enums;

import com.skyflow.generated.rest.types.V1Byot;
import org.junit.Assert;
import org.junit.Test;

public class TokenModeTest {

    @Test
    public void testGetByotDisable() {
        Assert.assertEquals(V1Byot.DISABLE, TokenMode.DISABLE.getByot());
    }

    @Test
    public void testGetByotEnable() {
        Assert.assertEquals(V1Byot.ENABLE, TokenMode.ENABLE.getByot());
    }

    @Test
    public void testGetByotEnableStrict() {
        Assert.assertEquals(V1Byot.ENABLE_STRICT, TokenMode.ENABLE_STRICT.getByot());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testGetBYOTDelegatestoGetByotDisable() {
        Assert.assertEquals(TokenMode.DISABLE.getByot(), TokenMode.DISABLE.getBYOT());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testGetBYOTDelegatestoGetByotEnable() {
        Assert.assertEquals(TokenMode.ENABLE.getByot(), TokenMode.ENABLE.getBYOT());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testGetBYOTDelegatestoGetByotEnableStrict() {
        Assert.assertEquals(TokenMode.ENABLE_STRICT.getByot(), TokenMode.ENABLE_STRICT.getBYOT());
    }

    @Test
    public void testToStringDisable() {
        Assert.assertEquals("DISABLE", TokenMode.DISABLE.toString());
    }

    @Test
    public void testToStringEnable() {
        Assert.assertEquals("ENABLE", TokenMode.ENABLE.toString());
    }

    @Test
    public void testToStringEnableStrict() {
        Assert.assertEquals("ENABLE_STRICT", TokenMode.ENABLE_STRICT.toString());
    }
}
