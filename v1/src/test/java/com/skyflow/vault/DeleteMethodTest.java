package com.skyflow.vault;

import com.skyflow.common.utils.HttpUtility;
import com.skyflow.common.utils.TokenUtils;
import com.skyflow.common.utils.Validators;
import com.skyflow.entities.DeleteRecordInput;
import com.skyflow.entities.RedactionType;
import com.skyflow.entities.SkyflowConfiguration;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.skyflow.common.utils.TokenUtils")
public class DeleteMethodTest {
    private static String vaultID = null;
    private static String vaultURL = null;
    private static String skyflowId = null;
    private static String token = null;
    private static String tableName = null;
    private static String columnName = null;
    private static String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";


    @BeforeClass
    public static void setup() throws SkyflowException {
        PowerMockito.mockStatic(TokenUtils.class);
        PowerMockito.when(TokenUtils.isTokenValid("valid_token")).thenReturn(true);
        PowerMockito.when(TokenUtils.isTokenValid("not_a_valid_token")).thenReturn(false);

        vaultID = "vault123";
        vaultURL = "https://test.com";
        skyflowId = "skyflowId123";
        token = "token123";
        tableName = "pii_fields";
        columnName = "first_name";
    }
    @Test
    public void testValidInput() {
        DeleteRecordInput validInput = new DeleteRecordInput();
        validInput.setTable("users");
        validInput.setId("12345");

        try {
            Validators.validateDeleteBySkyflowId(validInput);
        } catch (SkyflowException e) {
            fail("Unexpected exception thrown");
        }
    }
    @Test
    public void testValidInputCase2() {
        DeleteRecordInput validInput = new DeleteRecordInput();
        validInput.setTable("");
        validInput.setId("12345");

        try {
            Validators.validateDeleteBySkyflowId(validInput);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.InvalidTable.getDescription(), e.getMessage());
        }
    }
    @Test
    public void testValidInputCase3() {
        DeleteRecordInput validInput = new DeleteRecordInput();
        validInput.setTable(null);
        validInput.setId("12345");

        try {
            Validators.validateDeleteBySkyflowId(validInput);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.InvalidTable.getDescription(), e.getMessage());
        }
    }
    @Test
    public void testValidInputCase4() {
        DeleteRecordInput validInput = new DeleteRecordInput();
        validInput.setTable("table");
        validInput.setId(null);

        try {
            Validators.validateDeleteBySkyflowId(validInput);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.InvalidId.getDescription(), e.getMessage());
        }
    }
    @Test
    public void testValidInputInvalidInput() {
        DeleteRecordInput validInput = new DeleteRecordInput();
        validInput.setTable("table");
        validInput.setId("");

        try {
            Validators.validateDeleteBySkyflowId(validInput);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.InvalidId.getDescription(), e.getMessage());
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testDeleteEmptyRecords() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            records.put("records", recordsArray);
            JSONObject response = skyflowClient.delete(records);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.EmptyRecords.getDescription(), skyflowException.getMessage());
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testDeleteEmptyRecordsCase2() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            records.put("", recordsArray);
            JSONObject response = skyflowClient.delete(records);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidDeleteInput.getDescription(), skyflowException.getMessage());
        }
    }
    @Test
    public void testDeleteMethodErrorWhenNullRecordsArePassed() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            records.put("records", null);

            JSONObject response = skyflowClient.delete(records);

        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.EmptyRecords.getDescription(), e.getMessage());
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.TokenUtils"})
    public void testDeleteMethodErrorWhenNullTableNameIsPassed() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();

            record.put("table", null);

            recordsArray.add(record);
            records.put("records", recordsArray);

            JSONObject response = skyflowClient.delete(records);

        } catch (SkyflowException skyflowException) {
            JSONObject error = (JSONObject) ((JSONObject) (((JSONArray) skyflowException.getData().get("errors")).get(0))).get("error");
            Assert.assertEquals(ErrorCode.InvalidTable.getDescription(), error.get("description"));
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testDeleteEmptyRecordsCase3() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONObject record = new JSONObject();

            JSONArray recordsArray = new JSONArray();
            recordsArray.add(record);
            record.put("table", "name");
            records.put("records", recordsArray);
            JSONObject response = skyflowClient.delete(records);
        } catch (SkyflowException skyflowException) {
            JSONObject error = (JSONObject) ((JSONObject) (((JSONArray) skyflowException.getData().get("errors")).get(0))).get("error");
            Assert.assertEquals(ErrorCode.InvalidId.getDescription(), error.get("description"));
        }

    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testDeleteMethodErrorWhenWrongTableNameIsPassed() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();

            record.put("id", "skyflowid");
            record.put("table", "invalidTable");

            recordsArray.add(record);
            records.put("records", recordsArray);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"error\":{\"grpc_code\":13,\"http_code\":500,\"message\":\"Couldn't load data\",\"http_status\":\"Internal Server Error\"}}";
            PowerMockito.when(HttpUtility.sendRequest(ArgumentMatchers.anyString(),
                            ArgumentMatchers.<URL>any(),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenThrow(new SkyflowException(500, mockResponse));

            JSONObject response = skyflowClient.delete(records);

        } catch (SkyflowException e) {
            JSONArray errors = (JSONArray) e.getData().get("errors");
            Assert.assertEquals(1, errors.size());
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testDeleteMethodPartialError() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject firstRecord = new JSONObject();
            firstRecord.put("id", "id1");
            firstRecord.put("table", "table1");


            JSONObject secondRecord = new JSONObject();
            secondRecord.put("id", "id2");
            secondRecord.put("table", "invalidTable1");

            JSONObject thirdRecord = new JSONObject();
            thirdRecord.put("id", "id3");
            thirdRecord.put("table", "invalidTable2");


            recordsArray.add(firstRecord);
            recordsArray.add(secondRecord);
            recordsArray.add(thirdRecord);
            records.put("records", recordsArray);

            String firstRequestUrl = "https://test.com/v1/vaults/vault123/table1/id1";
            String secondRequestUrl = "https://test.com/v1/vaults/vault123/invalidTable1/id2";
            String thirdRequestUrl = "https://test.com/v1/vaults/vault123/invalidTable2/id3";

            PowerMockito.mockStatic(HttpUtility.class);
            String mockValidResponse = "{\"skyflow_id\":\"id1\",\"deleted\":\"true\"}";
            PowerMockito.when(HttpUtility.sendRequest(ArgumentMatchers.anyString(),
                            ArgumentMatchers.eq(new URL(firstRequestUrl)),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenReturn(mockValidResponse);

            String mockInvalidResponse1 = "{\"error\":{\"http_code\":404,\"http_status\":\"Not Found\",\"details\":[],\"message\":\"No Records Found - requestId: 968fc4a8-53ef-92fb-9c0d-3393d3361790\",\"grpc_code\":5}}";
            PowerMockito.when(HttpUtility.sendRequest(ArgumentMatchers.anyString(),
                            ArgumentMatchers.eq(new URL(secondRequestUrl)),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenThrow(new SkyflowException(500, mockInvalidResponse1));

            String mockInvalidResponse2 = "{\"error\":{\"http_code\":404,\"http_status\":\"Not Found\",\"details\":[],\"message\":\"No Records Found - requestId: 968fc4a8-53ef-92fb-9c0d-3393d3361790\",\"grpc_code\":5}}";
            PowerMockito.when(HttpUtility.sendRequest(ArgumentMatchers.anyString(),
                            ArgumentMatchers.eq(new URL(thirdRequestUrl)),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenThrow(new SkyflowException(500, mockInvalidResponse2));

            JSONObject response = skyflowClient.delete(records);

        } catch (SkyflowException e) {
            JSONObject partialError = e.getData();
            Assert.assertTrue(partialError.containsKey("records"));
            Assert.assertTrue(partialError.containsKey("errors"));

            JSONArray errors = (JSONArray) e.getData().get("errors");
            Assert.assertEquals(2, errors.size());
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testDeleteMethodSuccess() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();

            record.put("table", tableName);
            record.put("id", "id1");


            recordsArray.add(record);
            records.put("records", recordsArray);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"skyflow_id\":\"id1\",\"deleted\":\"true\"}";
            PowerMockito.when(HttpUtility.sendRequest(ArgumentMatchers.anyString(),
                            ArgumentMatchers.<URL>any(),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenReturn(mockResponse);
            JSONObject response = skyflowClient.delete(records);
            JSONArray responseRecords = (JSONArray) response.get("records");
            Assert.assertEquals(1, responseRecords.size());
            Assert.assertEquals("id1", ((JSONObject) responseRecords.get(0)).get("skyflow_id"));
            Assert.assertEquals("true", ((JSONObject) responseRecords.get(0)).get("deleted"));

        } catch (SkyflowException | IOException exception) {
            exception.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testDeleteMethodParseException() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();

            record.put("table", tableName);
            record.put("id", "id1");


            recordsArray.add(record);
            records.put("records", recordsArray);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"skyflow_id\":\"id1\",\"deleted\":\"true\"}";
            PowerMockito.when(HttpUtility.sendRequest(ArgumentMatchers.anyString(),
                            ArgumentMatchers.<URL>any(),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenReturn(mockResponse);
            JSONObject response = skyflowClient.delete(records);

        } catch (SkyflowException e) {
            String error = e.getMessage();
            assertEquals(ErrorCode.ThreadExecutionException.getDescription(), error);
        } catch (IOException exception) {
            exception.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testGetMethodIOException() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();

            record.put("table", tableName);
            record.put("id", "id1");


            recordsArray.add(record);
            records.put("records", recordsArray);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"skyflow_id\":\"id1\",\"deleted\":\"true\"}";
            PowerMockito.when(HttpUtility.sendRequest(ArgumentMatchers.anyString(),
                            ArgumentMatchers.<URL>any(),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenThrow(new IOException());

            JSONObject response = skyflowClient.delete(records);
        } catch (SkyflowException e) {
            String error = e.getMessage();
            assertEquals(ErrorCode.ThreadExecutionException.getDescription(), error);
        } catch (IOException exception) {
            exception.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
