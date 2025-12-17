package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

public class ErrorRecordTests {

    @Test
    public void testConstructorAndGetters() {
        ErrorRecord record = new ErrorRecord(5, "Some error", 404);
        Assert.assertEquals(5, record.getIndex());
        Assert.assertEquals("Some error", record.getError());
        Assert.assertEquals(404, record.getCode());
    }

    @Test
    public void testToStringJsonFormat() {
        ErrorRecord record = new ErrorRecord(2, "Error occurred", 500);
        String json = record.toString();
        Assert.assertTrue(json.contains("\"index\":2"));
        Assert.assertTrue(json.contains("\"error\":\"Error occurred\""));
        Assert.assertTrue(json.contains("\"code\":500"));
    }
}