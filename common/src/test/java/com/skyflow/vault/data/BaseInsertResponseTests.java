package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

public class BaseInsertResponseTests {

    @Test
    public void testGettersReturnConstructorValues() {
        ArrayList<HashMap<String, Object>> insertedFields = new ArrayList<>();
        HashMap<String, Object> field = new HashMap<>();
        field.put("skyflow_id", "id-1");
        insertedFields.add(field);

        ArrayList<HashMap<String, Object>> errors = new ArrayList<>();
        HashMap<String, Object> error = new HashMap<>();
        error.put("error", "some error");
        errors.add(error);

        BaseInsertResponse response = new BaseInsertResponse(insertedFields, errors);

        Assert.assertEquals(insertedFields, response.getInsertedFields());
        Assert.assertEquals(errors, response.getErrors());
    }

    @Test
    public void testNullInsertedFieldsAndErrors() {
        BaseInsertResponse response = new BaseInsertResponse(null, null);

        Assert.assertNull(response.getInsertedFields());
        Assert.assertNull(response.getErrors());
    }

    @Test
    public void testEmptyInsertedFieldsAndErrors() {
        BaseInsertResponse response = new BaseInsertResponse(new ArrayList<>(), new ArrayList<>());

        Assert.assertTrue(response.getInsertedFields().isEmpty());
        Assert.assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void testToStringWithPopulatedFieldsContainsValues() {
        ArrayList<HashMap<String, Object>> insertedFields = new ArrayList<>();
        HashMap<String, Object> field = new HashMap<>();
        field.put("skyflow_id", "id-1");
        insertedFields.add(field);

        BaseInsertResponse response = new BaseInsertResponse(insertedFields, new ArrayList<>());
        String result = response.toString();

        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("insertedFields"));
        Assert.assertTrue(result.contains("skyflow_id"));
        Assert.assertTrue(result.contains("id-1"));
    }

    @Test
    public void testToStringWithNullFieldsDoesNotThrowAndSerializesNulls() {
        BaseInsertResponse response = new BaseInsertResponse(null, null);
        String result = response.toString();

        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("null"));
    }
}
