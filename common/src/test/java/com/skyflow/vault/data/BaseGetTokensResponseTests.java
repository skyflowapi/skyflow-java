package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class BaseGetTokensResponseTests {

    @Test
    public void testGetRecordsReturnsConstructorValue() {
        HashMap<String, Object> record = new HashMap<>();
        record.put("token", "tok-1");
        ArrayList<HashMap<String, Object>> records = new ArrayList<>(Collections.singletonList(record));

        BaseGetTokensResponse response = new BaseGetTokensResponse(records);

        Assert.assertSame(records, response.getRecords());
    }

    @Test
    public void testNullRecordsAllowed() {
        BaseGetTokensResponse response = new BaseGetTokensResponse(null);

        Assert.assertNull(response.getRecords());
    }

    @Test
    public void testToStringSerializesNulls() {
        HashMap<String, Object> record = new HashMap<>();
        record.put("token", null);
        record.put("error", "Token not found.");
        BaseGetTokensResponse response = new BaseGetTokensResponse(new ArrayList<>(Collections.singletonList(record)));

        String json = response.toString();

        Assert.assertTrue(json, json.contains("\"token\":null"));
        Assert.assertTrue(json, json.contains("\"error\":\"Token not found.\""));
    }

    @Test
    public void testToStringWithNullRecords() {
        Assert.assertEquals("{\"records\":null}", new BaseGetTokensResponse(null).toString());
    }
}
