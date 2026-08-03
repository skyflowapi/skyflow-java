package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

public class BaseDetokenizeRecordResponseTests {

    @Test
    public void testGettersReturnConstructorValuesOnSuccess() {
        BaseDetokenizeRecordResponse response = new BaseDetokenizeRecordResponse("token-value", null);

        Assert.assertEquals("token-value", response.getToken());
        Assert.assertNull(response.getError());
    }

    @Test
    public void testGettersReturnConstructorValuesOnError() {
        BaseDetokenizeRecordResponse response = new BaseDetokenizeRecordResponse(null, "some error");

        Assert.assertNull(response.getToken());
        Assert.assertEquals("some error", response.getError());
    }

    @Test
    public void testBothTokenAndErrorNull() {
        BaseDetokenizeRecordResponse response = new BaseDetokenizeRecordResponse(null, null);

        Assert.assertNull(response.getToken());
        Assert.assertNull(response.getError());
    }

    @Test
    public void testBothTokenAndErrorPopulated() {
        BaseDetokenizeRecordResponse response = new BaseDetokenizeRecordResponse("token-value", "some error");

        Assert.assertEquals("token-value", response.getToken());
        Assert.assertEquals("some error", response.getError());
    }
}
