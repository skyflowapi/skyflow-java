package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

public class QueryResponseTest {

    @Test
    public void testGetErrorsReturnsNull() {
        ArrayList<HashMap<String, Object>> fields = new ArrayList<>();
        HashMap<String, Object> record = new HashMap<>();
        record.put("skyflowId", "abc-123");
        fields.add(record);

        QueryResponse response = new QueryResponse(fields);

        Assert.assertNull("getErrors() should return null when no errors", response.getErrors());
    }

    @Test
    public void testGetErrorsIsPresentInToString() {
        QueryResponse response = new QueryResponse(new ArrayList<>());
        String json = response.toString();
        Assert.assertTrue("toString() should include errors:null", json.contains("\"errors\":null"));
    }
}
