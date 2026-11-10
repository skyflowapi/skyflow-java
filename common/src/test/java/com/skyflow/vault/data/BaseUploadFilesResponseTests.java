package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class BaseUploadFilesResponseTests {

    @Test
    public void testGetRecordsReturnsConstructorValue() {
        HashMap<String, Object> record = new HashMap<>();
        record.put("skyflowId", "sky-1");
        ArrayList<HashMap<String, Object>> records = new ArrayList<>(Collections.singletonList(record));

        BaseUploadFilesResponse<HashMap<String, Object>> response = new BaseUploadFilesResponse<>(records);

        Assert.assertSame(records, response.getRecords());
    }

    @Test
    public void testNullRecordsAllowed() {
        BaseUploadFilesResponse<HashMap<String, Object>> response = new BaseUploadFilesResponse<>(null);

        Assert.assertNull(response.getRecords());
    }

    @Test
    public void testToStringSerializesNulls() {
        HashMap<String, Object> record = new HashMap<>();
        record.put("skyflowId", null);
        record.put("error", "Invalid request. skyflowID is invalid.");
        BaseUploadFilesResponse<HashMap<String, Object>> response = new BaseUploadFilesResponse<>(new ArrayList<>(Collections.singletonList(record)));

        String json = response.toString();

        Assert.assertTrue(json, json.contains("\"skyflowId\":null"));
        Assert.assertTrue(json, json.contains("\"error\":\"Invalid request. skyflowID is invalid.\""));
    }

    @Test
    public void testToStringWithNullRecords() {
        Assert.assertEquals("{\"records\":null}", new BaseUploadFilesResponse<HashMap<String, Object>>(null).toString());
    }
}
