package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

public class BaseDetokenizeDataTests {

    @Test
    public void testGetTokenReturnsConstructorValue() {
        BaseDetokenizeData data = new BaseDetokenizeData("token-value");

        Assert.assertEquals("token-value", data.getToken());
    }

    @Test
    public void testNullToken() {
        BaseDetokenizeData data = new BaseDetokenizeData(null);

        Assert.assertNull(data.getToken());
    }

    @Test
    public void testEmptyToken() {
        BaseDetokenizeData data = new BaseDetokenizeData("");

        Assert.assertEquals("", data.getToken());
    }
}
