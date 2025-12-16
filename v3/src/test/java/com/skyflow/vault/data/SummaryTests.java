package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

public class SummaryTests {

    @Test
    public void testConstructorAndGetters() {
        Summary summary = new Summary(10, 7, 3);
        Assert.assertEquals(10, summary.getTotalRecords());
        Assert.assertEquals(7, summary.getTotalInserted());
        Assert.assertEquals(3, summary.getTotalFailed());
    }

    @Test
    public void testDefaultConstructor() {
        Summary summary = new Summary();
        Assert.assertEquals(0, summary.getTotalRecords());
        Assert.assertEquals(0, summary.getTotalInserted());
        Assert.assertEquals(0, summary.getTotalFailed());
    }

    @Test
    public void testToStringJsonFormat() {
        Summary summary = new Summary(5, 4, 1);
        String json = summary.toString();
        Assert.assertTrue(json.contains("\"totalRecords\":5"));
        Assert.assertTrue(json.contains("\"totalInserted\":4"));
        Assert.assertTrue(json.contains("\"totalFailed\":1"));
    }
}