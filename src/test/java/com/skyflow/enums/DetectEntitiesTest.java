package com.skyflow.enums;

import org.junit.Assert;
import org.junit.Test;

public class DetectEntitiesTest {

    @Test
    public void testGetDetectEntities() {
        Assert.assertEquals("account_number", DetectEntities.ACCOUNT_NUMBER.getDetectEntities());
        Assert.assertEquals("account_number", DetectEntities.ACCOUNT_NUMBER.toString());
    }

    @Test
    public void testAll() {
        Assert.assertEquals("all", DetectEntities.ALL.getDetectEntities());
    }

    @Test
    public void testName() {
        Assert.assertEquals("name", DetectEntities.NAME.getDetectEntities());
        Assert.assertEquals("name", DetectEntities.NAME.toString());
    }
}
