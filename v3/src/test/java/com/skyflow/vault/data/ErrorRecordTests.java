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

    // ── requestId field ───────────────────────────────────────────────────────

    @Test
    public void testConstructorWithRequestId_setsAllFields() {
        ErrorRecord record = new ErrorRecord(3, "auth error", 401, "req-id-abc");
        Assert.assertEquals(3, record.getIndex());
        Assert.assertEquals("auth error", record.getError());
        Assert.assertEquals(401, record.getCode());
        Assert.assertEquals("req-id-abc", record.getRequestId());
    }

    @Test
    public void testThreeArgConstructor_requestIdIsNull() {
        ErrorRecord record = new ErrorRecord(1, "error", 500);
        Assert.assertNull(record.getRequestId());
    }

    @Test
    public void testFourArgConstructor_nullRequestId() {
        ErrorRecord record = new ErrorRecord(1, "error", 500, null);
        Assert.assertNull(record.getRequestId());
    }

    @Test
    public void testToString_includesRequestId() {
        ErrorRecord record = new ErrorRecord(1, "err", 400, "req-xyz");
        String json = record.toString();
        Assert.assertTrue(json.contains("\"requestId\":\"req-xyz\""));
        Assert.assertTrue(json.contains("\"index\":1"));
        Assert.assertTrue(json.contains("\"code\":400"));
    }

    @Test
    public void testToString_nullRequestIdNotSerialized() {
        ErrorRecord record = new ErrorRecord(1, "err", 400);
        String json = record.toString();
        Assert.assertTrue(json.contains("\"index\":1"));
        Assert.assertFalse(json.contains("\"requestId\""));
    }
}