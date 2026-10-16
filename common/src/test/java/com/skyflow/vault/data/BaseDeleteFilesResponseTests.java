package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class BaseDeleteFilesResponseTests {

    @Test
    public void testGetRecordsReturnsConstructorValue() {
        HashMap<String, Object> record = new HashMap<>();
        record.put("skyflowId", "sky-1");
        ArrayList<HashMap<String, Object>> records = new ArrayList<>(Collections.singletonList(record));

        BaseDeleteFilesResponse response = new BaseDeleteFilesResponse(records);

        Assert.assertSame(records, response.getRecords());
    }

    @Test
    public void testNullRecordsAllowed() {
        BaseDeleteFilesResponse response = new BaseDeleteFilesResponse(null);

        Assert.assertNull(response.getRecords());
    }

    @Test
    public void testToStringSerializesNulls() {
        HashMap<String, Object> record = new HashMap<>();
        record.put("columns", null);
        record.put("error", "Invalid request. skyflowID is invalid.");
        BaseDeleteFilesResponse response = new BaseDeleteFilesResponse(new ArrayList<>(Collections.singletonList(record)));

        String json = response.toString();

        Assert.assertTrue(json, json.contains("\"columns\":null"));
        Assert.assertTrue(json, json.contains("\"error\":\"Invalid request. skyflowID is invalid.\""));
    }

    @Test
    public void testToStringWithNullRecords() {
        Assert.assertEquals("{\"records\":null}", new BaseDeleteFilesResponse(null).toString());
    }
}
