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

import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.skyflow.common.utils.TokenUtils")
public class GetMethodTest {
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
    public void testGetMethodSuccess() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();

            JSONArray values = new JSONArray();
            values.add(columnValue);

            record.put("table", tableName);
            record.put("columnName", columnName);
            record.put("columnValues", values);
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
            JSONObject response = skyflowClient.get(records);
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
    public void testGetMethodErrorWhenInvalidInputIsPassed() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            records.put("invalidRecordsKey", recordsArray);

            JSONObject response = skyflowClient.get(records);
        } catch (SkyflowException skyflowException) {
            Assert.assertEquals(ErrorCode.InvalidGetInput.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    public void testGetMethodErrorWhenEmptyRecordsArePassed() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            records.put("records", recordsArray);

            JSONObject response = skyflowClient.get(records);

        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.EmptyRecords.getDescription(), e.getMessage());
        }
    }

    @Test
    public void testGetMethodErrorWhenNullRecordsArePassed() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            records.put("records", null);

            JSONObject response = skyflowClient.get(records);

        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.EmptyRecords.getDescription(), e.getMessage());
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.TokenUtils"})
    public void testGetMethodErrorWhenNullTableNameIsPassed() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();

            record.put("table", null);

            recordsArray.add(record);
            records.put("records", recordsArray);

            JSONObject response = skyflowClient.get(records);

        } catch (SkyflowException skyflowException) {
            JSONObject error = (JSONObject) ((JSONObject) (((JSONArray) skyflowException.getData().get("errors")).get(0))).get("error");
            Assert.assertEquals(ErrorCode.InvalidTable.getDescription(), error.get("description"));
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.TokenUtils"})
    public void testGetMethodErrorWhenEmptyTableNameIsPassed() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();

            record.put("table", "      ");

            recordsArray.add(record);
            records.put("records", recordsArray);

            JSONObject response = skyflowClient.get(records);

        } catch (SkyflowException skyflowException) {
            JSONObject error = (JSONObject) ((JSONObject) (((JSONArray) skyflowException.getData().get("errors")).get(0))).get("error");
            Assert.assertEquals(ErrorCode.InvalidTable.getDescription(), error.get("description"));
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testGetMethodErrorWhenWrongTableNameIsPassed() {
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

            JSONObject response = skyflowClient.get(records);

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
    public void testGetMethodPartialError() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject firstRecord = new JSONObject();
            JSONArray firstRecordColumnValues = new JSONArray();
            firstRecordColumnValues.add(columnValue);
            firstRecord.put("table", tableName);
            firstRecord.put("columnName", columnName);
            firstRecord.put("columnValues", firstRecordColumnValues);
            firstRecord.put("redaction", RedactionType.PLAIN_TEXT.toString());

            JSONObject secondRecord = new JSONObject();
            JSONArray ids = new JSONArray();
            ids.add(skyflowId);
            secondRecord.put("ids", ids);
            secondRecord.put("table", "invalidTable");
            secondRecord.put("redaction", RedactionType.PLAIN_TEXT.toString());

            JSONObject thirdRecord = new JSONObject();
            JSONArray thirdRecordColumnValues = new JSONArray();
            thirdRecordColumnValues.add(columnValue);
            thirdRecord.put("table", "invalidTable");
            thirdRecord.put("columnName", columnName);
            thirdRecord.put("columnValues", thirdRecordColumnValues);
            thirdRecord.put("redaction", RedactionType.PLAIN_TEXT.toString());

            recordsArray.add(firstRecord);
            recordsArray.add(secondRecord);
            recordsArray.add(thirdRecord);
            records.put("records", recordsArray);

            String firstRequestUrl = "https://test.com/v1/vaults/vault123/account_details?column_name=bank_account_number&column_values=123451234554321&redaction=PLAIN_TEXT";
            String secondRequestUrl = "https://test.com/v1/vaults/vault123/invalidTable?skyflow_ids=skyflowId123&redaction=PLAIN_TEXT";
            String thirdRequestUrl = "https://test.com/v1/vaults/vault123/invalidTable?column_name=bank_account_number&column_values=123451234554321&redaction=PLAIN_TEXT";

            PowerMockito.mockStatic(HttpUtility.class);
            String mockValidResponse = "{\"records\":[{\"fields\":{\"bank_account_number\":\"123455432112345\",\"pin_code\":\"123123\",\"name\":\"vivek jain\",\"id\":\"492c21a1-107f-4d10-ba2c-3482a411827d\"},\"table\":\"account_details\"}]}";
            PowerMockito.when(HttpUtility.sendRequest(ArgumentMatchers.anyString(),
                            ArgumentMatchers.eq(new URL(firstRequestUrl)),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenReturn(mockValidResponse);

            String mockInvalidResponse1 = "{\"error\":{\"code\":500,\"description\":\"Couldn't load data - requestId: 496c3a0b-3d24-9052-bbba-28bf3f04b97d\"},\"ids\":[\"skyflowId123\"]}";
            PowerMockito.when(HttpUtility.sendRequest(ArgumentMatchers.anyString(),
                            ArgumentMatchers.eq(new URL(secondRequestUrl)),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenThrow(new SkyflowException(500, mockInvalidResponse1));

            String mockInvalidResponse2 = "{\"error\":{\"code\":500,\"description\":\"Couldn't load data - requestId: 496c3a0b-3d24-9052-bbba-28bf3f04b97d\"},\"columnName\":\"bank_account_number\"}";
            PowerMockito.when(HttpUtility.sendRequest(ArgumentMatchers.anyString(),
                            ArgumentMatchers.eq(new URL(thirdRequestUrl)),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenThrow(new SkyflowException(500, mockInvalidResponse2));

            JSONObject response = skyflowClient.get(records);

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
    public void testGetMethodErrorWhenBothSkyflowIDsAndColumnDetailsArePassed() {
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
            record.put("columnName", columnName);
            record.put("columnValues", values);
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
            JSONObject response = skyflowClient.get(records);

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
    public void testGetMethodErrorWhenNeitherSkyflowIDsNorColumnDetailsArePassed() {
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
            JSONObject response = skyflowClient.get(records);

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
    public void testGetMethodErrorWhenColumnNameIsPassedWithoutColumnValues() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();

            record.put("table", tableName);
            record.put("columnName", columnName);
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
            JSONObject response = skyflowClient.get(records);

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
    public void testGetMethodErrorWhenColumnValuesArePassedWithoutColumnName() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();

            JSONArray values = new JSONArray();
            values.add(columnValue);

            record.put("table", tableName);
            record.put("columnValues", values);
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
            JSONObject response = skyflowClient.get(records);

        } catch (SkyflowException e) {
            JSONObject error = (JSONObject) ((JSONObject) (((JSONArray) e.getData().get("errors")).get(0))).get("error");
            Assert.assertEquals(ErrorCode.MissingRecordColumnName.getDescription(), error.get("description"));
        } catch (IOException e) {
            e.printStackTrace();
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

            JSONArray values = new JSONArray();
            values.add(columnValue);

            record.put("table", tableName);
            record.put("columnName", columnName);
            record.put("columnValues", values);
            record.put("redaction", RedactionType.PLAIN_TEXT.toString());

            recordsArray.add(record);
            records.put("records", recordsArray);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "a{\"records\":[{\"fields\":{\"bank_account_number\":\"123451234554321\",\"pin_code\":\"121342\",\"name\":\"vivek jain\",\"skyflow_id\":\"skyflowId123\"}}]}";
            PowerMockito.when(HttpUtility.sendRequest(ArgumentMatchers.anyString(),
                            ArgumentMatchers.<URL>any(),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenThrow(new IOException());

            JSONObject response = skyflowClient.get(records);

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
    public void testGetMethodParseException() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();

            JSONArray values = new JSONArray();
            values.add(columnValue);

            record.put("table", tableName);
            record.put("columnName", columnName);
            record.put("columnValues", values);
            record.put("redaction", RedactionType.PLAIN_TEXT.toString());

            recordsArray.add(record);
            records.put("records", recordsArray);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "a{\"records\":[{\"fields\":{\"bank_account_number\":\"123451234554321\",\"pin_code\":\"121342\",\"name\":\"vivek jain\",\"skyflow_id\":\"skyflowId123\"}}]}";
            PowerMockito.when(HttpUtility.sendRequest(ArgumentMatchers.anyString(),
                            ArgumentMatchers.<URL>any(),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenReturn(mockResponse);
            JSONObject response = skyflowClient.get(records);

        } catch (SkyflowException e) {
            String error = e.getMessage();
            assertEquals(ErrorCode.ThreadExecutionException.getDescription(), error);
        } catch (IOException exception) {
            exception.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
