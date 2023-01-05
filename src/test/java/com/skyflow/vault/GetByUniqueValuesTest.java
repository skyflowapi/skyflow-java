package com.skyflow.vault;


import com.skyflow.common.utils.HttpUtility;
import com.skyflow.common.utils.TokenUtils;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.skyflow.common.utils.TokenUtils")
public class GetByUniqueValuesTest {
    private static String vaultID = null;
    private static String vaultURL = null;
    private static String skyflowId = null;
    private static String token = null;
    private static String tableName = null;
    private static String columnName = null;
    private static String columnValue = null;
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
        tableName = "account_details";
        columnName = "bank_account_number";
        columnValue = "123451234554321";
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testGetByUniqueValuesSuccess() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();

            JSONArray values = new JSONArray();
            values.add(columnValue);

            record.put("table", tableName);
            record.put("column_name", columnName);
            record.put("column_values", values);
            record.put("redaction", RedactionType.PLAIN_TEXT.toString());

            recordsArray.add(record);
            records.put("records", recordsArray);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"records\":[{\"fields\":{\"bank_account_number\":\"123451234554321\",\"pin_code\":\"121342\",\"name\":\"vivek jain\",\"skyflow_id\":\"skyflowId123\"}}]}";
            PowerMockito.when(HttpUtility.sendRequest(ArgumentMatchers.anyString(),
                            ArgumentMatchers.<URL>any(),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenReturn(mockResponse);
            JSONObject response = skyflowClient.getById(records);
            JSONArray responseRecords = (JSONArray) response.get("records");

            Assert.assertEquals(1, responseRecords.size());
            Assert.assertEquals(tableName, ((JSONObject) responseRecords.get(0)).get("table"));
            Assert.assertNotNull(((JSONObject) responseRecords.get(0)).get("fields"));

        } catch (SkyflowException | IOException exception) {
            exception.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testGetByUniqueValuesErrorWhenTableNameIsNotPassed() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();

            JSONArray ids = new JSONArray();
            ids.add(skyflowId);

            record.put("ids", ids);
            record.put("table", "invalidTable");
            record.put("redaction", RedactionType.PLAIN_TEXT.toString());

            recordsArray.add(record);
            records.put("records", recordsArray);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"error\":{\"grpc_code\":13,\"http_code\":500,\"message\":\"Couldn't load data\",\"http_status\":\"Internal Server Error\"}}";
            PowerMockito.when(HttpUtility.sendRequest(ArgumentMatchers.anyString(),
                            ArgumentMatchers.<URL>any(),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenThrow(new SkyflowException(500, mockResponse));

            JSONObject response = skyflowClient.getById(records);

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
    public void testGetByUniqueValuesErrorWhenBothSkyflowIDsAndColumnDetailsArePassed() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();

            JSONArray values = new JSONArray();
            values.add(columnValue);

            JSONArray ids = new JSONArray();
            ids.add(skyflowId);

            record.put("ids", ids);
            record.put("table", tableName);
            record.put("column_name", columnName);
            record.put("column_values", values);
            record.put("redaction", RedactionType.PLAIN_TEXT.toString());

            recordsArray.add(record);
            records.put("records", recordsArray);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"error\":{\"grpc_code\":13,\"http_code\":500,\"message\":\"Couldn't load data\",\"http_status\":\"Internal Server Error\"}}";
            PowerMockito.when(HttpUtility.sendRequest(ArgumentMatchers.anyString(),
                            ArgumentMatchers.<URL>any(),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenThrow(new SkyflowException(500, mockResponse));
            JSONObject response = skyflowClient.getById(records);

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
    public void testGetByUniqueValuesErrorWhenNeitherSkyflowIDsNorColumnDetailsArePassed() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();

            record.put("table", tableName);
            record.put("redaction", RedactionType.PLAIN_TEXT.toString());

            recordsArray.add(record);
            records.put("records", recordsArray);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"errors\":[{\"error\":{\"code\":400,\"description\":\"Provide either Ids or column name to get records.\"}}]}";
            PowerMockito.when(HttpUtility.sendRequest(ArgumentMatchers.anyString(),
                            ArgumentMatchers.<URL>any(),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenThrow(new SkyflowException(400, mockResponse));
            JSONObject response = skyflowClient.getById(records);

        } catch (SkyflowException e) {
            JSONObject error = (JSONObject) ((JSONObject) (((JSONArray) e.getData().get("errors")).get(0))).get("error");
            Assert.assertEquals(ErrorCode.MissingIdAndColumnName.getDescription(), error.get("description"));
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testGetByUniqueValuesErrorWhenColumnNameIsPassedWithoutColumnValues() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();

            record.put("table", tableName);
            record.put("column_name", columnName);
            record.put("redaction", RedactionType.PLAIN_TEXT.toString());

            recordsArray.add(record);
            records.put("records", recordsArray);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"errors\":[{\"error\":{\"code\":400,\"description\":\"Column Values can not be empty when Column Name is specified.\"}}]}";
            PowerMockito.when(HttpUtility.sendRequest(ArgumentMatchers.anyString(),
                            ArgumentMatchers.<URL>any(),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenThrow(new SkyflowException(500, mockResponse));
            JSONObject response = skyflowClient.getById(records);

        } catch (SkyflowException e) {
            JSONObject error = (JSONObject) ((JSONObject) (((JSONArray) e.getData().get("errors")).get(0))).get("error");
            Assert.assertEquals(ErrorCode.MissingRecordColumnValue.getDescription(), error.get("description"));
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testGetByUniqueValuesErrorWhenColumnValuesArePassedWithoutColumnName() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();

            JSONArray values = new JSONArray();
            values.add(columnValue);

            record.put("table", tableName);
            record.put("column_values", values);
            record.put("redaction", RedactionType.PLAIN_TEXT.toString());

            recordsArray.add(record);
            records.put("records", recordsArray);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"errors\":[{\"error\":{\"code\":400,\"description\":\"Column Name can not be empty when Column Values are specified.\"}}]}";
            PowerMockito.when(HttpUtility.sendRequest(ArgumentMatchers.anyString(),
                            ArgumentMatchers.<URL>any(),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenThrow(new SkyflowException(500, mockResponse));
            JSONObject response = skyflowClient.getById(records);

        } catch (SkyflowException e) {
            JSONObject error = (JSONObject) ((JSONObject) (((JSONArray) e.getData().get("errors")).get(0))).get("error");
            Assert.assertEquals(ErrorCode.MissingRecordColumnName.getDescription(), error.get("description"));
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
