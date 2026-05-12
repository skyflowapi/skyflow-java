package com.skyflow.enums;

import org.junit.Assert;
import org.junit.Test;

public class CustomHeaderKeyTests {

    @Test
    public void values_hasExactlyThreeEntries() {
        Assert.assertEquals(3, CustomHeaderKey.values().length);
    }

    @Test
    public void skyflowAccountID_toStringReturnsCorrectHeader() {
        Assert.assertEquals("x-skyflow-account-id", CustomHeaderKey.SkyflowAccountID.toString());
    }

    @Test
    public void skyflowAccountName_toStringReturnsCorrectHeader() {
        Assert.assertEquals("x-skyflow-account-name", CustomHeaderKey.SkyflowAccountName.toString());
    }

    @Test
    public void requestIDHeader_toStringReturnsCorrectHeader() {
        Assert.assertEquals("x-request-id", CustomHeaderKey.RequestIDHeader.toString());
    }

    @Test
    public void valueOf_returnsCorrectConstants() {
        Assert.assertEquals(CustomHeaderKey.SkyflowAccountID, CustomHeaderKey.valueOf("SkyflowAccountID"));
        Assert.assertEquals(CustomHeaderKey.SkyflowAccountName, CustomHeaderKey.valueOf("SkyflowAccountName"));
        Assert.assertEquals(CustomHeaderKey.RequestIDHeader, CustomHeaderKey.valueOf("RequestIDHeader"));
    }
}