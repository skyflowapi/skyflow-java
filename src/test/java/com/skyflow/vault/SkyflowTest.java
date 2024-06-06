/*
	Copyright (c) 2022 Skyflow, Inc.
*/
package com.skyflow.vault;

import com.skyflow.Configuration;
import com.skyflow.common.utils.Helpers;
import com.skyflow.common.utils.HttpUtility;
import com.skyflow.common.utils.TokenUtils;
import com.skyflow.entities.*;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DemoTokenProvider implements TokenProvider {
    @Override
    public String getBearerToken() throws Exception {
        return "valid_token";
    }
}

class InvalidTokenProvider implements TokenProvider {
    @Override
    public String getBearerToken() throws Exception {
        return "not_a_valid_token";
    }
}

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.skyflow.common.utils.TokenUtils")
public class SkyflowTest {

    private static String vaultID = null;
    private static String vaultURL = null;
    private static String skyflowId = null;
    private static String token = null;
    private static String tableName = null;
    private static String columnName = null;
    private static String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";


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
    public void testValidConfig() {
        Configuration.setLogLevel(LogLevel.INFO);
        SkyflowConfiguration testConfig = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());
        try {
            Skyflow skyflow = Skyflow.init(testConfig);
            assert (skyflow instanceof Skyflow);
        } catch (SkyflowException e) {
            assertNotNull(e);
        }
    }


    @Test
    public void testInValidConfigWithNullValues() {
        Configuration.setLogLevel(LogLevel.ERROR);
        SkyflowConfiguration testConfig = new SkyflowConfiguration(null, null, new DemoTokenProvider());
        try {
            Skyflow skyflow = Skyflow.init(testConfig);
            skyflow.detokenize(new JSONObject());

        } catch (SkyflowException e) {
            assertEquals(e.getCode(), ErrorCode.EmptyVaultID.getCode());
            assertEquals(e.getMessage(), ErrorCode.EmptyVaultID.getDescription());
        }
    }

    @Test
    public void testInValidConfigWithNullValues2() {
        Configuration.setLogLevel(LogLevel.DEBUG);
        SkyflowConfiguration testConfig = new SkyflowConfiguration(vaultID, null, new DemoTokenProvider());
        try {
            Skyflow skyflow = Skyflow.init(testConfig);
            skyflow.detokenize(new JSONObject());

        } catch (SkyflowException e) {
            assertEquals(e.getCode(), ErrorCode.InvalidVaultURL.getCode());
            assertEquals(e.getMessage(), ErrorCode.InvalidVaultURL.getDescription());
        }
    }

    @Test
    public void testInvalidConfigWithEmptyVaultID() {
        Configuration.setLogLevel(LogLevel.WARN);
        SkyflowConfiguration testConfig = new SkyflowConfiguration("", vaultURL, new DemoTokenProvider());
        try {
            Skyflow skyflow = Skyflow.init(testConfig);
            skyflow.insert(new JSONObject(), new InsertOptions());
        } catch (SkyflowException e) {
            assertEquals(e.getCode(), ErrorCode.EmptyVaultID.getCode());
            assertEquals(e.getMessage(), ErrorCode.EmptyVaultID.getDescription());
        }
    }

    @Test
    public void testInvalidConfigWithInvalidVaultURL() {
        Configuration.setLogLevel(LogLevel.OFF);
        SkyflowConfiguration testConfig = new SkyflowConfiguration(vaultID, "//valid.url.com", new DemoTokenProvider());
        try {
            Skyflow skyflow = Skyflow.init(testConfig);
            skyflow.insert(new JSONObject(), new InsertOptions());
        } catch (SkyflowException e) {
            assertEquals(e.getCode(), ErrorCode.InvalidVaultURL.getCode());
            assertEquals(e.getMessage(), ErrorCode.InvalidVaultURL.getDescription());
        }
    }

    @Test
    public void testInvalidConfigWithHttpVaultURL() {
        SkyflowConfiguration testConfig = new SkyflowConfiguration(vaultID, "http://valid.url.com", new DemoTokenProvider());
        try {
            Skyflow skyflow = Skyflow.init(testConfig);
        } catch (SkyflowException e) {
            assertEquals(e.getCode(), ErrorCode.InvalidVaultURL.getCode());
            assertEquals(e.getMessage(), ErrorCode.InvalidVaultURL.getDescription());
        }
    }

    @Test
    public void testInsertWithInvalidSkyflowConfig() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, null);

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();

            JSONObject res = skyflowClient.insert(records);
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.InvalidTokenProvider.getDescription(), e.getMessage());
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertSuccessWithTokens() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration("vaultID", "https://vaulturl.com", new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("table", tableName);
            JSONObject fields = new JSONObject();
            fields.put(columnName, "first");
            record.put("fields", fields);

            recordsArray.add(record);

            records.put("records", recordsArray);

            InsertOptions insertOptions = new InsertOptions();

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse =
                    "{\"vaultID\":\"vault123\",\"responses\":[{\"records\":[{\"skyflow_id\":\"id1\", \"tokens\":{\"first_name\":\"token1\"}}]}]}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);

            JSONObject res = skyflowClient.insert(records, insertOptions);
            JSONArray responseRecords = (JSONArray) res.get("records");

            assertEquals(1, responseRecords.size());
            assertEquals(tableName, ((JSONObject) responseRecords.get(0)).get("table"));
            assertNotNull(((JSONObject) responseRecords.get(0)).get("fields"));
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertSuccessWithoutInsertOptions() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("table", tableName);
            JSONObject fields = new JSONObject();
            fields.put(columnName, "first");
            record.put("fields", fields);

            recordsArray.add(record);

            records.put("records", recordsArray);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse =
                    "{\"vaultID\":\"vault123\",\"responses\":[{\"records\":[{\"skyflow_id\":\"id1\", \"tokens\":{\"first_name\":\"token1\"}}]}]}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);
            JSONObject res = skyflowClient.insert(records);
            JSONArray responseRecords = (JSONArray) res.get("records");

            assertEquals(1, responseRecords.size());
            assertEquals(tableName, ((JSONObject) responseRecords.get(0)).get("table"));
            assertNotNull(((JSONObject) responseRecords.get(0)).get("fields"));
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertSuccessWithUpsertOptions() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("table", tableName);
            JSONObject fields = new JSONObject();
            fields.put(columnName, "first");
            record.put("fields", fields);

            recordsArray.add(record);

            records.put("records", recordsArray);

            UpsertOption upsertOption = new UpsertOption(tableName, columnName);
            InsertOptions insertOptions = new InsertOptions(new UpsertOption[]{upsertOption});

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse =
                    "{\"vaultID\":\"vault123\",\"responses\":[{\"records\":[{\"skyflow_id\":\"id1\", \"tokens\":{\"first_name\":\"token1\"}}]}]}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);

            JSONObject res = skyflowClient.insert(records, insertOptions);
            JSONArray responseRecords = (JSONArray) res.get("records");

            assertEquals(1, responseRecords.size());
            assertEquals(tableName, ((JSONObject) responseRecords.get(0)).get("table"));
            assertNotNull(((JSONObject) responseRecords.get(0)).get("fields"));
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertSuccessWithoutTokens() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("table", tableName);
            JSONObject fields = new JSONObject();
            fields.put(columnName, "first");
            record.put("fields", fields);

            recordsArray.add(record);

            records.put("records", recordsArray);

            InsertOptions insertOptions = new InsertOptions(false);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"vaultID\":\"vault123\",\"responses\":[{\"records\":[{\"skyflow_id\":\"id1\"}]}]}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);
            JSONObject res = skyflowClient.insert(records, insertOptions);
            JSONArray responseRecords = (JSONArray) res.get("records");

            assertEquals(1, responseRecords.size());
            assertEquals(tableName, ((JSONObject) responseRecords.get(0)).get("table"));
            assertNotNull(((JSONObject) responseRecords.get(0)).get("skyflow_id"));
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertWithContinueOnErrorAsTrueWithTokens() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record1 = new JSONObject();
            record1.put("table", tableName);
            JSONObject fields1 = new JSONObject();
            fields1.put(columnName, "john");
            record1.put("fields", fields1);

            JSONObject record2 = new JSONObject();
            record2.put("table", tableName);
            JSONObject fields2 = new JSONObject();
            fields2.put(columnName, "jane");
            record2.put("fields", fields2);

            recordsArray.add(record1);
            recordsArray.add(record2);
            records.put("records", recordsArray);

            InsertOptions insertOptions = new InsertOptions(true, true);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"vaultID\":\"vault123\",\"responses\":[{\"Status\":400,\"Body\":{\"error\":\"Error Inserting Records due to unique constraint violation\"}},{\"Status\":200,\"Body\":{\"records\":[{\"skyflow_id\":\"id1\",\"tokens\":{\"first_name\":\"token1\"}}]}}]}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);
            JSONObject res = skyflowClient.insert(records, insertOptions);
            JSONArray responseSuccessRecords = (JSONArray) res.get("records");
            JSONArray responseErrorRecords = (JSONArray) res.get("errors");

            assertEquals(1, responseSuccessRecords.size());
            assertEquals(tableName, ((JSONObject) responseSuccessRecords.get(0)).get("table"));
            assertEquals(1, ((JSONObject) responseSuccessRecords.get(0)).get("request_index"));
            assertNotNull(((JSONObject) responseSuccessRecords.get(0)).get("fields"));

            assertEquals(1, responseErrorRecords.size());
            assertEquals(0, ((JSONObject) ((JSONObject) responseErrorRecords.get(0)).get("error")).get("request_index"));
            assertNotNull(((JSONObject) ((JSONObject) responseErrorRecords.get(0)).get("error")).get("description"));
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertWithContinueOnErrorAsTrueWithoutTokens() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record1 = new JSONObject();
            record1.put("table", tableName);
            JSONObject fields1 = new JSONObject();
            fields1.put(columnName, "first");
            record1.put("fields", fields1);

            JSONObject record2 = new JSONObject();
            record2.put("table", tableName);
            JSONObject fields2 = new JSONObject();
            fields2.put(columnName, "second");
            record2.put("fields", fields2);

            recordsArray.add(record1);
            recordsArray.add(record2);
            records.put("records", recordsArray);

            InsertOptions insertOptions = new InsertOptions(false, true);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"vaultID\":\"vault123\",\"responses\":[{\"Status\":400,\"Body\":{\"error\":\"Error Inserting Records due to unique constraint violation\"}},{\"Status\":200,\"Body\":{\"records\":[{\"skyflow_id\":\"id1\"}]}}]}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);
            JSONObject res = skyflowClient.insert(records, insertOptions);
            JSONArray responseSuccessRecords = (JSONArray) res.get("records");
            JSONArray responseErrorRecords = (JSONArray) res.get("errors");

            assertEquals(1, responseSuccessRecords.size());
            assertEquals(tableName, ((JSONObject) responseSuccessRecords.get(0)).get("table"));
            assertEquals(1, ((JSONObject) responseSuccessRecords.get(0)).get("request_index"));
            assertNotNull(((JSONObject) responseSuccessRecords.get(0)).get("skyflow_id"));
            assertNull(((JSONObject) responseSuccessRecords.get(0)).get("tokens"));

            assertEquals(1, responseErrorRecords.size());
            assertEquals(0, ((JSONObject) ((JSONObject) responseErrorRecords.get(0)).get("error")).get("request_index"));
            assertNotNull(((JSONObject) ((JSONObject) responseErrorRecords.get(0)).get("error")).get("description"));
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertWithContinueOnErrorAsTrueWithUpsert() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record1 = new JSONObject();
            record1.put("table", tableName);
            JSONObject fields1 = new JSONObject();
            fields1.put(columnName, "first");
            record1.put("fields", fields1);

            JSONObject record2 = new JSONObject();
            record2.put("table", tableName);
            JSONObject fields2 = new JSONObject();
            fields2.put(columnName, "second");
            record2.put("fields", fields2);

            recordsArray.add(record1);
            recordsArray.add(record2);
            records.put("records", recordsArray);

            UpsertOption upsertOption = new UpsertOption(tableName, columnName);
            InsertOptions insertOptions = new InsertOptions(new UpsertOption[]{upsertOption}, true);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"vaultID\":\"vault123\",\"responses\":[{\"Status\":400,\"Body\":{\"error\":\"Error Inserting Records due to unique constraint violation\"}},{\"Status\":200,\"Body\":{\"records\":[{\"skyflow_id\":\"id1\"}]}}]}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);
            JSONObject res = skyflowClient.insert(records, insertOptions);
            JSONArray responseSuccessRecords = (JSONArray) res.get("records");
            JSONArray responseErrorRecords = (JSONArray) res.get("errors");

            assertEquals(1, responseSuccessRecords.size());
            assertEquals(tableName, ((JSONObject) responseSuccessRecords.get(0)).get("table"));
            assertEquals(1, ((JSONObject) responseSuccessRecords.get(0)).get("request_index"));
            assertNotNull(((JSONObject) responseSuccessRecords.get(0)).get("skyflow_id"));
            assertNull(((JSONObject) responseSuccessRecords.get(0)).get("tokens"));

            assertEquals(1, responseErrorRecords.size());
            assertEquals(0, ((JSONObject) ((JSONObject) responseErrorRecords.get(0)).get("error")).get("request_index"));
            assertNotNull(((JSONObject) ((JSONObject) responseErrorRecords.get(0)).get("error")).get("description"));
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertWithContinueOnErrorWithUpsertAndTokens() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record1 = new JSONObject();
            record1.put("table", tableName);
            JSONObject fields1 = new JSONObject();
            fields1.put(columnName, "first");
            record1.put("fields", fields1);

            JSONObject record2 = new JSONObject();
            record2.put("table", tableName);
            JSONObject fields2 = new JSONObject();
            fields2.put(columnName, "second");
            record2.put("fields", fields2);

            recordsArray.add(record1);
            recordsArray.add(record2);
            records.put("records", recordsArray);

            UpsertOption upsertOption = new UpsertOption(tableName, columnName);
            InsertOptions insertOptions = new InsertOptions(false, new UpsertOption[]{upsertOption}, true);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"vaultID\":\"vault123\",\"responses\":[{\"Status\":400,\"Body\":{\"error\":\"Error Inserting Records due to unique constraint violation\"}},{\"Status\":200,\"Body\":{\"records\":[{\"skyflow_id\":\"id1\"}]}}]}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);
            JSONObject res = skyflowClient.insert(records, insertOptions);
            JSONArray responseSuccessRecords = (JSONArray) res.get("records");
            JSONArray responseErrorRecords = (JSONArray) res.get("errors");

            assertEquals(1, responseSuccessRecords.size());
            assertEquals(tableName, ((JSONObject) responseSuccessRecords.get(0)).get("table"));
            assertEquals(1, ((JSONObject) responseSuccessRecords.get(0)).get("request_index"));
            assertNotNull(((JSONObject) responseSuccessRecords.get(0)).get("skyflow_id"));
            assertNull(((JSONObject) responseSuccessRecords.get(0)).get("tokens"));

            assertEquals(1, responseErrorRecords.size());
            assertEquals(0, ((JSONObject) ((JSONObject) responseErrorRecords.get(0)).get("error")).get("request_index"));
            assertNotNull(((JSONObject) ((JSONObject) responseErrorRecords.get(0)).get("error")).get("description"));
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertParseException() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record1 = new JSONObject();
            record1.put("table", tableName);
            JSONObject fields1 = new JSONObject();
            fields1.put(columnName, "first");
            record1.put("fields", fields1);

            recordsArray.add(record1);
            records.put("records", recordsArray);

            InsertOptions insertOptions = new InsertOptions(false, true);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"vaultID\":\"vault123\",\"responses\":[{\"Status\":200,\"Body\":{\"records\":[{\"skyflow_id\":\"id1\"]}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);
            JSONObject res = skyflowClient.insert(records, insertOptions);
            fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.ResponseParsingError.getDescription(), skyflowException.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testInsertEmptyRecords() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            records.put("records", recordsArray);

            InsertOptions insertOptions = new InsertOptions(false);
            JSONObject res = skyflowClient.insert(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.EmptyRecords.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    public void testInsertEmptyTable() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            JSONObject fields = new JSONObject();
            fields.put(columnName, "first");
            record.put("fields", fields);

            recordsArray.add(record);

            records.put("records", recordsArray);

            InsertOptions insertOptions = new InsertOptions(false);
            JSONObject res = skyflowClient.insert(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidTable.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    public void testInsertEmptyFields() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("table", tableName);
            recordsArray.add(record);

            records.put("records", recordsArray);

            InsertOptions insertOptions = new InsertOptions(true);
            JSONObject res = skyflowClient.insert(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidFields.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    public void testInsertInvalidInput() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("invalidTableKey", tableName);
            recordsArray.add(record);

            records.put("records", recordsArray);

            InsertOptions insertOptions = new InsertOptions(true);
            JSONObject res = skyflowClient.insert(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidInsertInput.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    public void testInsertWithEmptyArrayUpsertOptions() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            UpsertOption[] upsertOptions = new UpsertOption[]{};
            InsertOptions insertOptions = new InsertOptions(true, upsertOptions);
            JSONObject records = new JSONObject();
            JSONObject res = skyflowClient.insert(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidUpsertOptionType.getDescription(), skyflowException.getMessage());
        }

        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            UpsertOption[] upsertOptions = new UpsertOption[3];
            InsertOptions insertOptions = new InsertOptions(true, upsertOptions);
            JSONObject records = new JSONObject();
            JSONObject res = skyflowClient.insert(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidUpsertObjectType.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    public void testInsertWithInvalidTableInUpsertOptions() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            InsertOptions insertOptions = new InsertOptions(true, new UpsertOption[]{new UpsertOption(null, "column")});
            JSONObject records = new JSONObject();
            JSONObject res = skyflowClient.insert(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidTableInUpsertOption.getDescription(), skyflowException.getMessage());
        }
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            InsertOptions insertOptions = new InsertOptions(true, new UpsertOption[]{new UpsertOption("", "column")});
            JSONObject records = new JSONObject();
            JSONObject res = skyflowClient.insert(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidTableInUpsertOption.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    public void testInsertWithInvalidColumnInUpsertOptions() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            InsertOptions insertOptions = new InsertOptions(true, new UpsertOption[]{new UpsertOption("table1", null)});
            JSONObject records = new JSONObject();
            JSONObject res = skyflowClient.insert(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidColumnInUpsertOption.getDescription(), skyflowException.getMessage());
        }
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            InsertOptions insertOptions = new InsertOptions(true, new UpsertOption[]{new UpsertOption("table2", "")});
            JSONObject records = new JSONObject();
            JSONObject res = skyflowClient.insert(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidColumnInUpsertOption.getDescription(), skyflowException.getMessage());
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testQuerySuccess() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());
            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject queryInput = new JSONObject();
            queryInput.put("query", "SELECT * FROM users");
            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"response_key\":\"response_value\"}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);
            JSONObject response = skyflowClient.query(queryInput, new QueryOptions());
            assertNotNull(response);
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }
    @Test
    public void testQueryMissing() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());
            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject queryInput = new JSONObject();
            queryInput.put("query", "");
            QueryOptions queryOptions = new QueryOptions();
            boolean exceptionThrown = false;
            try {
                JSONObject res = skyflowClient.query(queryInput, queryOptions);
            } catch (SkyflowException skyflowException) {
                exceptionThrown = true;
                assertEquals(ErrorCode.InvalidQuery.getCode(), skyflowException.getCode());
                assertEquals("Query is missing", skyflowException.getMessage());
            }
            assertTrue(exceptionThrown);
        } catch (Exception e) {
            fail("Caught unexpected exception: " + e.getMessage());
        }
    }
    @Test
    public void testInvalidQueryInput() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());
            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject queryInput = new JSONObject();
            queryInput.put("invalidQueryKey", "select * from cards");
            QueryOptions queryOptions = new QueryOptions();
            boolean exceptionThrown = false;
            try {
                JSONObject res = skyflowClient.query(queryInput, queryOptions);
            } catch (SkyflowException skyflowException) {
                exceptionThrown = true;
                assertEquals("Invalid query input", skyflowException.getMessage());
            }
            assertTrue(exceptionThrown);
        } catch (Exception e) {
        }
    }
    @Test
    public void testValidJsonParsing() {
        String jsonMessage = "{\"error\":{\"http_code\":404,\"message\":\"Not Found\"}}";
        SkyflowException skyflowException = new SkyflowException(
                ErrorCode.InvalidVaultURL.getCode(),
                jsonMessage
        );
        JSONObject result = Helpers.constructQueryErrorObject(skyflowException);
    }

    @Test
    public void testValidSkyflowExceptionWithoutJson() {
        SkyflowException skyflowException = new SkyflowException(
                ErrorCode.EmptyVaultID.getCode(),
                "Empty vaultID"
        );
        JSONObject result = Helpers.constructQueryErrorObject(skyflowException);
    }
    @Test
    public void testInvalidJsonParsing() {
        String invalidJsonMessage = "Invalid JSON";
        SkyflowException skyflowException = new SkyflowException(
                ErrorCode.InvalidVaultURL.getCode(),
                invalidJsonMessage
        );
        JSONObject result = Helpers.constructQueryErrorObject(skyflowException);
    }
    @Test
    public void testMissingErrorKeyInJson() {
        String jsonMessage = "{\"status\":\"error\",\"http_code\":404,\"message\":\"Not Found\"}";
        SkyflowException skyflowException = new SkyflowException(
                ErrorCode.InvalidVaultURL.getCode(),
                jsonMessage
        );
        JSONObject result = Helpers.constructQueryErrorObject(skyflowException);
    }
    @Test
    public void testMissingHttpCodeInErrorJson() {
        String jsonMessage = "{\"error\":{\"message\":\"Not Found\"}}";
        SkyflowException skyflowException = new SkyflowException(
                ErrorCode.InvalidVaultURL.getCode(),
                jsonMessage
        );
        JSONObject result = Helpers.constructQueryErrorObject(skyflowException);
    }
    @Test
    public void testMissingMessageInErrorJson() {
        String jsonMessage = "{\"error\":{\"http_code\":404}}";
        SkyflowException skyflowException = new SkyflowException(
                ErrorCode.InvalidVaultURL.getCode(),
                jsonMessage
        );
        JSONObject result = Helpers.constructQueryErrorObject(skyflowException);
    }
    @Test
    public void testEmptyErrorObjectInJson() {
        String jsonMessage = "{\"error\":{}}";
        SkyflowException skyflowException = new SkyflowException(
                ErrorCode.InvalidVaultURL.getCode(),
                jsonMessage
        );
        JSONObject result = Helpers.constructQueryErrorObject(skyflowException);
    }
    @Test
    public void testConstructQueryRequest() throws SkyflowException {
        QueryRecordInput record = new QueryRecordInput();
        record.setQuery("SELECT * FROM users");
        QueryOptions options = new QueryOptions();

        JSONObject result = Helpers.constructQueryRequest(record, options);
        assertTrue(result.containsKey("query"));
        assertEquals("SELECT * FROM users", result.get("query"));
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testQueryIOException() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());
            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject queryInput = new JSONObject();
            queryInput.put("query", "SELECT * FROM table");
            QueryOptions queryOptions = new QueryOptions();
            PowerMockito.mockStatic(HttpUtility.class);
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenThrow(new IOException());

            try {
                skyflowClient.query(queryInput, queryOptions);
                fail("Expected SkyflowException to be thrown");
            } catch (SkyflowException e) {
                assertEquals(ErrorCode.InvalidQueryInput.getCode(), e.getCode());
            }
        } catch (SkyflowException e) {
            fail("Caught unexpected SkyflowException: " + e.getMessage());
        } catch (IOException ioException) {
            fail("Caught unexpected IOException: " + ioException.getMessage());
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testQueryParseException() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());
            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject queryInput = new JSONObject();
            queryInput.put("query", "SELECT * FROM table");
            QueryOptions queryOptions = new QueryOptions();

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "Invalid JSON";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);

            try {
                skyflowClient.query(queryInput, queryOptions);
                fail("Expected SkyflowException to be thrown");
            } catch (SkyflowException e) {
                assertEquals(ErrorCode.ResponseParsingError.getCode(), e.getCode());
            }
        } catch (SkyflowException e) {
            fail("Caught unexpected SkyflowException: " + e.getMessage());
        } catch (IOException e) {
            fail("Caught unexpected IOException: " + e.getMessage());
        }
    }
    @Test
    public void testGetAndSetRecords() {
        QueryRecordInput inputRecord = new QueryRecordInput();
        QueryInput queryInput = new QueryInput();

        assertNull(queryInput.getQueryInput());

        queryInput.setQueryInput(inputRecord);
        assertEquals(inputRecord, queryInput.getQueryInput());
    }
    @Test
    public void testGetAndSetQuery() {
        QueryRecordInput queryRecordInput = new QueryRecordInput();
        assertNull(queryRecordInput.getQuery());

        String testQuery = "SELECT * FROM table";
        queryRecordInput.setQuery(testQuery);
        assertEquals(testQuery, queryRecordInput.getQuery());
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testDetokenizeSuccess() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("token", token);
            recordsArray.add(record);
            records.put("records", recordsArray);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"records\":[{\"token\":\"token123\",\"valueType\":\"INTEGER\",\"value\":\"10\"}]}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);

            JSONObject res = skyflowClient.detokenize(records);
            JSONArray responseRecords = (JSONArray) res.get("records");
            assertEquals(1, responseRecords.size());
            assertEquals(token, ((JSONObject) responseRecords.get(0)).get("token"));
            assertTrue(((JSONObject) responseRecords.get(0)).containsKey("value"));
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testFormattedDetokenizeSuccess() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("token", token);
            recordsArray.add(record);
            records.put("records", recordsArray);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"records\":[{\"token\":\"token123\",\"valueType\":\"INTEGER\",\"value\":\"10\",\"test\":\"test123\"}]}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);

            JSONObject res = skyflowClient.detokenize(records);
            JSONArray responseRecords = (JSONArray) res.get("records");
            assertEquals(1, responseRecords.size());
            assertEquals(token, ((JSONObject) responseRecords.get(0)).get("token"));
            assertEquals(null, ((JSONObject) responseRecords.get(0)).get("test"));
            assertTrue(((JSONObject) responseRecords.get(0)).containsKey("value"));
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testDetokenizeError() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("token", "invalidToken");
            recordsArray.add(record);
            records.put("records", recordsArray);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse =
                    "{\"error\":{\"grpc_code\":5,\"http_code\":404,\"message\":\"Token not found for invalidToken\",\"http_status\":\"Not Found\",\"details\":[]}}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenThrow(new SkyflowException(404, mockResponse));

            JSONObject res = skyflowClient.detokenize(records);
        } catch (SkyflowException skyflowException) {
            JSONArray errors = (JSONArray) skyflowException.getData().get("errors");
            assertEquals(1, errors.size());
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testDetokenizePartialError() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("token", "invalidToken1");
            JSONObject validRecord = new JSONObject();
            validRecord.put("token", token);
            recordsArray.add(record);
            recordsArray.add(validRecord);
            records.put("records", recordsArray);

            JSONParser jsonParser = new JSONParser();
            JSONObject validRequest = (JSONObject)
                    jsonParser.parse("{\"detokenizationParameters\":[{\"token\":\"token123\",\"redaction\":\"PLAIN_TEXT\"}]}");
            PowerMockito.mockStatic(HttpUtility.class);
            String mockValidResponse = "{\"records\":[{\"token\":\"token123\",\"valueType\":\"INTEGER\",\"value\":\"10\"}]}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), eq(validRequest), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockValidResponse);

            JSONObject invalidRequest = (JSONObject)
                    jsonParser.parse("{\"detokenizationParameters\":[{\"token\":\"invalidToken1\",\"redaction\":\"PLAIN_TEXT\"}]}");
            String mockInvalidResponse =
                    "{\"error\":{\"grpc_code\":5,\"http_code\":404,\"message\":\"Token not found for invalidToken1\",\"http_status\":\"Not Found\",\"details\":[]}}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), eq(invalidRequest), ArgumentMatchers.<String, String>anyMap())).thenThrow(new SkyflowException(404, mockInvalidResponse));


            JSONObject res = skyflowClient.detokenize(records);
        } catch (SkyflowException skyflowException) {
            JSONArray responseRecords = (JSONArray) skyflowException.getData().get("records");
            JSONArray errors = (JSONArray) skyflowException.getData().get("errors");
            assertEquals(1, errors.size());
            assertEquals(1, responseRecords.size());
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        } catch (ParseException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testDetokenizeInvalidInput() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            records.put("invalidRecordsKey", recordsArray);

            JSONObject res = skyflowClient.detokenize(records);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidDetokenizeInput.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    public void testDetokenizeEmptyRecords() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            records.put("records", recordsArray);

            JSONObject res = skyflowClient.detokenize(records);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.EmptyRecords.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    public void testDetokenizeEmptyRecords2() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            records.put("records", null);

            JSONObject res = skyflowClient.detokenize(records);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.EmptyRecords.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.TokenUtils"})
    public void testDetokenizeInvalidToken() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("token", "");
            recordsArray.add(record);
            records.put("records", recordsArray);

            JSONObject res = skyflowClient.detokenize(records);
        } catch (SkyflowException skyflowException) {
            JSONObject error = (JSONObject) ((JSONObject) (((JSONArray) skyflowException.getData().get("errors")).get(0))).get("error");
            assertEquals(ErrorCode.InvalidToken.getDescription(), error.get("description"));
        }
    }

    @Test
    public void testDetokenizeInvalidRedaction() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("token", "3423-4671-5420-2425");
            record.put("redaction", "invalid");
            recordsArray.add(record);
            records.put("records", recordsArray);

            JSONObject res = skyflowClient.detokenize(records);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidDetokenizeInput.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testDetokenizeWhenNullRedactionIsPassed() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("token", "3423-4671-5420-2425");
            record.put("redaction", null);
            recordsArray.add(record);
            records.put("records", recordsArray);

            JSONObject res = skyflowClient.detokenize(records);
        } catch (SkyflowException skyflowException) {
            JSONObject error = (JSONObject) ((JSONObject) (((JSONArray) skyflowException.getData().get("errors")).get(0))).get("error");
            assertEquals(ErrorCode.InvalidDetokenizeInput.getDescription(), error.get("description"));
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testGetByIdSuccess() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record = new JSONObject();
            JSONArray ids = new JSONArray();
            ids.add(skyflowId);
            record.put("ids", ids);
            record.put("table", tableName);
            record.put("redaction", RedactionType.PLAIN_TEXT.toString());
            recordsArray.add(record);

            records.put("records", recordsArray);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"records\":[{\"fields\":{\"age\":10,\"skyflow_id\":\"skyflowId123\"}}]}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);
            JSONObject response = skyflowClient.getById(records);
            JSONArray responseRecords = (JSONArray) response.get("records");

            assertEquals(1, responseRecords.size());
            assertEquals(tableName, ((JSONObject) responseRecords.get(0)).get("table"));
            assertNotNull(((JSONObject) responseRecords.get(0)).get("fields"));

        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testGetByIdError() {
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
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenThrow(new SkyflowException(500, mockResponse));
            JSONObject response = skyflowClient.getById(records);

        } catch (SkyflowException e) {
            JSONArray errors = (JSONArray) e.getData().get("errors");
            assertEquals(1, errors.size());
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }


    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testGetByIdPartialError() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record = new JSONObject();
            JSONArray ids = new JSONArray();
            ids.add(skyflowId);
            record.put("ids", ids);
            record.put("table", tableName);
            record.put("redaction", RedactionType.PLAIN_TEXT.toString());
            recordsArray.add(record);

            JSONObject record2 = new JSONObject();
            JSONArray id2 = new JSONArray();
            id2.add("invalidId");
            record2.put("ids", id2);
            record2.put("table", "invalidTable2");
            record2.put("redaction", RedactionType.PLAIN_TEXT.toString());
            recordsArray.add(record2);

            records.put("records", recordsArray);

            String validRequestUrl = "https://test.com/v1/vaults/vault123/pii_fields?skyflow_ids=skyflowId123&redaction=PLAIN_TEXT";
            String invalidRequestUrl = "https://test.com/v1/vaults/vault123/invalidTable2?skyflow_ids=invalidId&redaction=PLAIN_TEXT";

            PowerMockito.mockStatic(HttpUtility.class);
            String mockValidResponse = "{\"records\":[{\"fields\":{\"age\":10,\"skyflow_id\":\"skyflowId123\"}}]}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), eq(new URL(validRequestUrl)), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockValidResponse);

            String mockInvalidResponse = "{\"error\":{\"grpc_code\":13,\"http_code\":500,\"message\":\"Couldn't load data\",\"http_status\":\"Internal Server Error\"}}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), eq(new URL(invalidRequestUrl)), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenThrow(new SkyflowException(500, mockInvalidResponse));

            JSONObject response = skyflowClient.getById(records);

        } catch (SkyflowException e) {
            JSONObject partialError = e.getData();
            assertTrue(partialError.containsKey("records"));
            assertTrue(partialError.containsKey("errors"));
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetByIdInvalidInput() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            records.put("invalidRecordsKey", recordsArray);

            JSONObject response = skyflowClient.getById(records);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidGetByIdInput.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    public void testGetByIdEmptyRecords() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            records.put("records", recordsArray);

            JSONObject response = skyflowClient.getById(records);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.EmptyRecords.getDescription(), skyflowException.getMessage());
        }
    }


    @Test
    public void testGetByIdEmptyRecords2() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            records.put("records", null);

            JSONObject response = skyflowClient.getById(records);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.EmptyRecords.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.TokenUtils"})
    public void testGetByIdEmptyTable() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record = new JSONObject();
            JSONArray ids = new JSONArray();
            ids.add(skyflowId);
            record.put("ids", ids);
            record.put("table", "");
            record.put("redaction", RedactionType.PLAIN_TEXT.toString());
            recordsArray.add(record);

            records.put("records", recordsArray);

            JSONObject response = skyflowClient.getById(records);
        } catch (SkyflowException skyflowException) {
            JSONObject error = (JSONObject) ((JSONObject) (((JSONArray) skyflowException.getData().get("errors")).get(0))).get("error");
            assertEquals(ErrorCode.InvalidTable.getDescription(), error.get("description"));
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testUpdateSuccessWithToken() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());
            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record = new JSONObject();
            record.put("table", tableName);
            record.put("id", skyflowId);
            JSONObject fields = new JSONObject();
            fields.put(columnName, "name");
            record.put("fields", fields);
            recordsArray.add(record);
            records.put("records", recordsArray);

            UpdateOptions updateOptions = new UpdateOptions(true);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"skyflow_id\":\"skyflowId123\",\"tokens\":{\"first_name\":\"token2\"}}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);

            JSONObject res = skyflowClient.update(records, updateOptions);
            JSONArray responseRecords = (JSONArray) res.get("records");

            assertEquals(1, responseRecords.size());
            assertEquals(tableName, ((JSONObject) responseRecords.get(0)).get("table"));
            assertNotNull(((JSONObject) responseRecords.get(0)).get("fields"));
        } catch (SkyflowException e) {
            JSONArray errors = (JSONArray) e.getData().get("errors");
            assertEquals(1, errors.size());
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }

    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testUpdateSuccessWithOutToken() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());
            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record = new JSONObject();
            record.put("table", tableName);
            record.put("id", skyflowId);
            JSONObject fields = new JSONObject();
            fields.put(columnName, "name");
            record.put("fields", fields);
            recordsArray.add(record);
            records.put("records", recordsArray);

            UpdateOptions updateOptions = new UpdateOptions(false);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"skyflow_id\":\"skyflowId123\",\"tokens\":null}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);

            JSONObject res = skyflowClient.update(records, updateOptions);
            JSONArray responseRecords = (JSONArray) res.get("records");

            assertEquals(1, responseRecords.size());
            assertEquals(tableName, ((JSONObject) responseRecords.get(0)).get("table"));
            assertNull(((JSONObject) responseRecords.get(0)).get("fields"));
        } catch (SkyflowException e) {
            JSONArray errors = (JSONArray) e.getData().get("errors");
            assertEquals(1, errors.size());
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }

    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testUpdateSuccessWithOutUpdateOptions() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());
            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record = new JSONObject();
            record.put("table", tableName);
            record.put("id", skyflowId);
            JSONObject fields = new JSONObject();
            fields.put(columnName, "name");
            record.put("fields", fields);
            recordsArray.add(record);
            records.put("records", recordsArray);


            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"skyflow_id\":\"skyflowId123\",\"tokens\":{\"first_name\":\"token2\"}}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);

            JSONObject res = skyflowClient.update(records);
            JSONArray responseRecords = (JSONArray) res.get("records");

            assertEquals(1, responseRecords.size());
            assertEquals(tableName, ((JSONObject) responseRecords.get(0)).get("table"));
            assertNotNull(((JSONObject) responseRecords.get(0)).get("fields"));
        } catch (SkyflowException e) {
            JSONArray errors = (JSONArray) e.getData().get("errors");
            assertEquals(1, errors.size());
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }

    }

    @Test
    public void testUpdateEmptyRecords() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            records.put("records", recordsArray);
            UpdateOptions updateOptions = new UpdateOptions();
            JSONObject response = skyflowClient.update(records, updateOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.EmptyRecords.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.TokenUtils"})
    public void testUpdateEmptyFields() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            JSONObject fields = new JSONObject();
            record.put("table", tableName);
            record.put("id", skyflowId);
            record.put("fields", fields);
            recordsArray.add(record);

            records.put("records", recordsArray);
            UpdateOptions updateOptions = new UpdateOptions();
            JSONObject response = skyflowClient.update(records, updateOptions);
        } catch (SkyflowException skyflowException) {
            JSONObject error = (JSONObject) ((JSONObject) (((JSONArray) skyflowException.getData().get("error")).get(0))).get("error");
            assertEquals(ErrorCode.InvalidFields.getDescription(), error.get("description"));
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.TokenUtils"})
    public void testUpdateInvalidTable() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            JSONObject fields = new JSONObject();
            record.put("table", null);
            record.put("id", skyflowId);
            record.put("fields", fields);
            recordsArray.add(record);

            records.put("records", recordsArray);
            UpdateOptions updateOptions = new UpdateOptions();
            JSONObject response = skyflowClient.update(records, updateOptions);
        } catch (SkyflowException skyflowException) {
            JSONObject error = (JSONObject) ((JSONObject) (((JSONArray) skyflowException.getData().get("error")).get(0))).get("error");
            assertEquals(ErrorCode.InvalidTable.getDescription(), error.get("description"));
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.TokenUtils"})
    public void testUpdateEmptySkyflowId() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            JSONObject fields = new JSONObject();
            record.put("table", tableName);
            record.put("id", "");
            record.put("fields", fields);
            recordsArray.add(record);

            records.put("records", recordsArray);
            UpdateOptions updateOptions = new UpdateOptions();
            JSONObject response = skyflowClient.update(records, updateOptions);
        } catch (SkyflowException skyflowException) {
            JSONObject error = (JSONObject) ((JSONObject) (((JSONArray) skyflowException.getData().get("error")).get(0))).get("error");
            assertEquals(ErrorCode.InvalidSkyflowId.getDescription(), error.get("description"));
        }
    }

    @Test
    public void testUpdateEmptyRecords2() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            records.put("records", null);

            JSONObject response = skyflowClient.getById(records);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.EmptyRecords.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.TokenUtils"})
    public void testUpdateEmptyTable() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());
            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record = new JSONObject();
            record.put("table", "");
            record.put("id", skyflowId);
            JSONObject fields = new JSONObject();
            fields.put(columnName, "name");
            record.put("fields", fields);
            recordsArray.add(record);
            records.put("records", recordsArray);

            UpdateOptions updateOptions = new UpdateOptions(true);

            JSONObject res = skyflowClient.update(records, updateOptions);
        } catch (SkyflowException e) {
            JSONObject error = (JSONObject) ((JSONObject) (((JSONArray) e.getData().get("error")).get(0))).get("error");
            assertEquals(ErrorCode.InvalidTable.getDescription(), error.get("description"));
        }

    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testUpdateError() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());
            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record = new JSONObject();
            record.put("table", "invalid_table");
            record.put("id", skyflowId);
            JSONObject fields = new JSONObject();
            fields.put(columnName, "name");
            record.put("fields", fields);
            recordsArray.add(record);
            records.put("records", recordsArray);

            UpdateOptions updateOptions = new UpdateOptions(true);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"error\":{\"grpc_code\":3,\"http_code\":400,\"message\":\"Object Name table was not found for Vault vault123\",\"http_status\":\"Bad Request\",\"details\":[]}}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);

            JSONObject res = skyflowClient.update(records, updateOptions);

        } catch (SkyflowException e) {
            JSONArray errors = (JSONArray) e.getData().get("error");
            assertEquals(1, errors.size());
            JSONObject error = (JSONObject) ((JSONObject) (((JSONArray) e.getData().get("error")).get(0))).get("error");
            assertEquals(ErrorCode.InvalidTable.getDescription(), error.get("description"));
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }

    }

    @Test
    public void testUpdateInvalidInput() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            records.put("invalidRecordsKey", recordsArray);

            UpdateOptions updateOptions = new UpdateOptions(true);
            JSONObject response = skyflowClient.update(records, updateOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidUpdateInput.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testUpdatePartialError() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record = new JSONObject();
            record.put("table", tableName);
            record.put("id", skyflowId);
            JSONObject fields = new JSONObject();
            fields.put(columnName, "name");
            record.put("fields", fields);
            recordsArray.add(record);

            JSONObject record2 = new JSONObject();
            record2.put("table", "invalidTable");
            record2.put("id", "invalidId");
            JSONObject fields2 = new JSONObject();
            fields2.put(columnName, "name");
            record2.put("fields", fields2);
            recordsArray.add(record2);
            records.put("records", recordsArray);

            String validRequestUrl = "https://test.com/v1/vaults/vault123/pii_fields/skyflowId123";
            String invalidRequestUrl = "https://test.com/v1/vaults/vault123/invalidTable/invalidId";

            PowerMockito.mockStatic(HttpUtility.class);

            String mockInvalidResponse = "{\"error\":{\"grpc_code\":3,\"http_code\":400,\"message\":\"Object Name table was not found for Vault vault123\",\"http_status\":\"Bad Request\",\"details\":[]}}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), eq(new URL(invalidRequestUrl)), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenThrow(new SkyflowException(400, mockInvalidResponse));

            String mockValidResponse = "{\"skyflow_id\":\"skyflowId123\",\"tokens\":{\"first_name\":\"token2\"}}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), eq(new URL(validRequestUrl)), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockValidResponse);

            JSONObject response = skyflowClient.update(records, new UpdateOptions(true));

        } catch (SkyflowException e) {
            JSONObject partialError = e.getData();
            assertTrue(partialError.containsKey("records"));
            assertTrue(partialError.containsKey("error"));
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testUpdateWrongVaultError() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record = new JSONObject();
            record.put("table", tableName);
            record.put("id", skyflowId);
            JSONObject fields = new JSONObject();
            fields.put(columnName, "name");
            record.put("fields", fields);
            recordsArray.add(record);
            records.put("records", recordsArray);

            String invalidRequestUrl = "https://test.com/v1/vaults/vault1234/invalidTable/vault123/invalidId";

            PowerMockito.mockStatic(HttpUtility.class);

            String mockInvalidResponse = "ThreadExecution exception";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), eq(new URL(invalidRequestUrl)), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenThrow(new SkyflowException(500, mockInvalidResponse));

            JSONObject response = skyflowClient.update(records, new UpdateOptions(true));

        } catch (SkyflowException e) {
            String error = e.getMessage();
            assertEquals(ErrorCode.ThreadExecutionException.getDescription(), error);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testUpdateIOException() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record = new JSONObject();
            record.put("table", tableName);
            record.put("id", skyflowId);
            JSONObject fields = new JSONObject();
            fields.put(columnName, "name");
            record.put("fields", fields);
            recordsArray.add(record);
            records.put("records", recordsArray);

            PowerMockito.mockStatic(HttpUtility.class);
            PowerMockito.when(HttpUtility.sendRequest(anyString(),
                            ArgumentMatchers.<URL>any(),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenThrow(new IOException());

            JSONObject response = skyflowClient.update(records, new UpdateOptions(true));

        } catch (SkyflowException e) {
            String error = e.getMessage();
            assertEquals(ErrorCode.ThreadExecutionException.getDescription(), error);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testUpdateParseException() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record = new JSONObject();
            record.put("table", tableName);
            record.put("id", skyflowId);
            JSONObject fields = new JSONObject();
            fields.put(columnName, "name");
            record.put("fields", fields);
            recordsArray.add(record);
            records.put("records", recordsArray);

            String invalidMockResponse = "a{\"skyflow_id\":\"skyflowId123\",\"tokens\":{\"first_name\":\"token2\"}}";

            PowerMockito.mockStatic(HttpUtility.class);
            PowerMockito.when(HttpUtility.sendRequest(anyString(),
                            ArgumentMatchers.<URL>any(),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenReturn(invalidMockResponse);

            JSONObject response = skyflowClient.update(records, new UpdateOptions(true));

        } catch (SkyflowException e) {
            String error = e.getMessage();
            assertEquals(ErrorCode.ThreadExecutionException.getDescription(), error);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertBulkEmptyTable() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            JSONObject fields = new JSONObject();
            fields.put(columnName, "first");
            record.put("fields", fields);

            recordsArray.add(record);

            records.put("records", recordsArray);

            InsertBulkOptions insertOptions = new InsertBulkOptions(false);
            JSONObject res = skyflowClient.insertBulk(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidTable.getDescription(), skyflowException.getMessage());
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertBulkEmptyTableCase2() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            JSONObject fields = new JSONObject();
            fields.put(columnName, "first");
            record.put("table", null);
            record.put("fields", fields);

            recordsArray.add(record);

            records.put("records", recordsArray);

            InsertBulkOptions insertOptions = new InsertBulkOptions(false);
            JSONObject res = skyflowClient.insertBulk(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidTable.getDescription(), skyflowException.getMessage());
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertBulkEmptyTableCase3() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            JSONObject fields = new JSONObject();
            fields.put(columnName, "first");
            record.put("table", "");
            record.put("fields", fields);

            recordsArray.add(record);

            records.put("records", recordsArray);

            InsertBulkOptions insertOptions = new InsertBulkOptions(false);
            JSONObject res = skyflowClient.insertBulk(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidTable.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertBulkEmptyFields() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("table", tableName);
            recordsArray.add(record);

            records.put("records", recordsArray);

            InsertBulkOptions insertOptions = new InsertBulkOptions(true);
            JSONObject res = skyflowClient.insertBulk(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidFields.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    public void testInsertBulkInvalidInput() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("invalidTableKey", tableName);
            recordsArray.add(record);

            records.put("records", recordsArray);

            InsertBulkOptions insertOptions = new InsertBulkOptions(true);
            JSONObject res = skyflowClient.insertBulk(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidInsertInput.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    public void testInsertBulkWithEmptyArrayUpsertOptions() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            UpsertOption[] upsertOptions = new UpsertOption[]{};
            InsertBulkOptions insertOptions = new InsertBulkOptions(true, upsertOptions);
            JSONObject records = new JSONObject();
            JSONObject res = skyflowClient.insertBulk(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidUpsertOptionType.getDescription(), skyflowException.getMessage());
        }

        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            UpsertOption[] upsertOptions = new UpsertOption[3];
            InsertBulkOptions insertOptions = new InsertBulkOptions(true, upsertOptions);
            JSONObject records = new JSONObject();
            JSONObject res = skyflowClient.insertBulk(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidUpsertObjectType.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    public void testInsertBulkWithInvalidTableInUpsertOptions() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            InsertBulkOptions insertOptions = new InsertBulkOptions(true, new UpsertOption[]{new UpsertOption(null, "column")});
            JSONObject records = new JSONObject();
            JSONObject res = skyflowClient.insertBulk(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidTableInUpsertOption.getDescription(), skyflowException.getMessage());
        }
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            InsertBulkOptions insertOptions = new InsertBulkOptions(true, new UpsertOption[]{new UpsertOption("", "column")});
            JSONObject records = new JSONObject();
            JSONObject res = skyflowClient.insertBulk(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidTableInUpsertOption.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    public void testInsertBulkWithInvalidColumnInUpsertOptions() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            InsertBulkOptions insertOptions = new InsertBulkOptions(true, new UpsertOption[]{new UpsertOption("table1", null)});
            JSONObject records = new JSONObject();
            JSONObject res = skyflowClient.insertBulk(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidColumnInUpsertOption.getDescription(), skyflowException.getMessage());
        }
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            InsertBulkOptions insertOptions = new InsertBulkOptions(true, new UpsertOption[]{new UpsertOption("table2", "")});
            JSONObject records = new JSONObject();
            JSONObject res = skyflowClient.insertBulk(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidColumnInUpsertOption.getDescription(), skyflowException.getMessage());
        }
    }


    @Test
    public void testInvalidConfigWithEmptyVaultIDInInsertBulk() {
        Configuration.setLogLevel(LogLevel.WARN);
        SkyflowConfiguration testConfig = new SkyflowConfiguration("", vaultURL, new DemoTokenProvider());
        try {
            Skyflow skyflow = Skyflow.init(testConfig);
            skyflow.insertBulk(new JSONObject(), new InsertBulkOptions());
        } catch (SkyflowException e) {
            assertEquals(e.getCode(), ErrorCode.EmptyVaultID.getCode());
            assertEquals(e.getMessage(), ErrorCode.EmptyVaultID.getDescription());
        }
    }

    @Test
    public void testInvalidConfigWithInvalidVaultURLInInsertBulk() {
        Configuration.setLogLevel(LogLevel.OFF);
        SkyflowConfiguration testConfig = new SkyflowConfiguration(vaultID, "//valid.url.com", new DemoTokenProvider());
        try {
            Skyflow skyflow = Skyflow.init(testConfig);
            skyflow.insertBulk(new JSONObject(), new InsertBulkOptions());
        } catch (SkyflowException e) {
            assertEquals(e.getCode(), ErrorCode.InvalidVaultURL.getCode());
            assertEquals(e.getMessage(), ErrorCode.InvalidVaultURL.getDescription());
        }
    }
        @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertBulkSuccessWithUpsertOptions() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("table", tableName);
            JSONObject fields = new JSONObject();
            fields.put(columnName, "first");
            record.put("fields", fields);

            recordsArray.add(record);

            records.put("records", recordsArray);

            UpsertOption upsertOption = new UpsertOption(tableName, columnName);
            InsertBulkOptions insertOptions = new InsertBulkOptions(new UpsertOption[]{upsertOption});

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse =
                    "{\"records\":[{\"skyflow_id\":\"id1\", \"tokens\":{\"first_name\":\"token1\"}}]}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);

            JSONObject res = skyflowClient.insertBulk(records, insertOptions);
            JSONArray responseRecords = (JSONArray) res.get("records");

            assertEquals(1, responseRecords.size());
            assertEquals(tableName, ((JSONObject) responseRecords.get(0)).get("table"));
            assertNotNull(((JSONObject) responseRecords.get(0)).get("fields"));
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertBulkSuccessWithoutTokens() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("table", tableName);
            JSONObject fields = new JSONObject();
            fields.put(columnName, "first");
            record.put("fields", fields);

            recordsArray.add(record);

            records.put("records", recordsArray);

            InsertBulkOptions insertOptions = new InsertBulkOptions(false);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse = "{\"records\":[{\"skyflow_id\":\"id1\"}]}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);
            JSONObject res = skyflowClient.insertBulk(records, insertOptions);
            JSONArray responseRecords = (JSONArray) res.get("records");

            assertEquals(1, responseRecords.size());
            assertEquals(tableName, ((JSONObject) responseRecords.get(0)).get("table"));
            assertNotNull(((JSONObject) responseRecords.get(0)).get("skyflow_id"));
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }
        @Test
    public void testInsertBulkWithInvalidSkyflowConfig() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, null);

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();

            JSONObject res = skyflowClient.insertBulk(records);
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.InvalidTokenProvider.getDescription(), e.getMessage());
        }
    } 
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertBulkSuccessWithTokens() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration("vaultID", "https://vaulturl.com", new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("table", tableName);
            JSONObject fields = new JSONObject();
            fields.put(columnName, "first");
            record.put("fields", fields);

            recordsArray.add(record);

            records.put("records", recordsArray);

            InsertBulkOptions insertOptions = new InsertBulkOptions();

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse =
                    "{\"records\":[{\"skyflow_id\":\"id1\", \"tokens\":{\"first_name\":\"token1\"}}]}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);

            JSONObject res = skyflowClient.insertBulk(records, insertOptions);
            JSONArray responseRecords = (JSONArray) res.get("records");

            assertEquals(1, responseRecords.size());
            assertEquals(tableName, ((JSONObject) responseRecords.get(0)).get("table"));
            assertNotNull(((JSONObject) responseRecords.get(0)).get("fields"));
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertBulkErrorsWithTokens() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration("vaultID", "https://vaulturl.com", new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("table", tableName);
            JSONObject fields = new JSONObject();
            fields.put(columnName, "first");
            record.put("fields", fields);

            recordsArray.add(record);

            records.put("records", recordsArray);

            InsertBulkOptions insertOptions = new InsertBulkOptions();

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse =
                    "{\"error\":{\"grpc_code\":3,\"http_code\":400,\"message\":\"Invalid field present in JSON cardholder_nam\",\"http_status\":\"Bad Request\",\"details\":[]}}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenThrow(new SkyflowException(500, mockResponse));

            JSONObject res = skyflowClient.insertBulk(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            JSONArray errors = (JSONArray) skyflowException.getData().get("errors");
            Assert.assertEquals(1, errors.size());
            skyflowException.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertBulkSuccessWithoutInsertOptions() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("table", tableName);
            JSONObject fields = new JSONObject();
            fields.put(columnName, "first");
            record.put("fields", fields);

            recordsArray.add(record);

            records.put("records", recordsArray);

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse =
                    "{\"records\":[{\"skyflow_id\":\"id1\", \"tokens\":{\"first_name\":\"token1\"}}]}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse);
            JSONObject res = skyflowClient.insertBulk(records);
            JSONArray responseRecords = (JSONArray) res.get("records");

            assertEquals(1, responseRecords.size());
            assertEquals(tableName, ((JSONObject) responseRecords.get(0)).get("table"));
            assertNotNull(((JSONObject) responseRecords.get(0)).get("fields"));
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertBulkErrorsWithTokensParseException() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration("vaultID", "https://vaulturl.com", new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("table", tableName);
            JSONObject fields = new JSONObject();
            fields.put(columnName, "first");
            record.put("fields", fields);

            recordsArray.add(record);

            records.put("records", recordsArray);

            InsertBulkOptions insertOptions = new InsertBulkOptions();

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse =
                    "{\"grpc_code\":3,\"http_code\":400,\"message\":\"Invalid field present in JSON cardholder_nam\",\"http_status\":\"Bad Request\",\"details\":";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenThrow(new SkyflowException(500, mockResponse));

            JSONObject res = skyflowClient.insertBulk(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            JSONArray errors = (JSONArray) skyflowException.getData().get("errors");
            Assert.assertEquals(1, errors.size());
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertBulkPartialSuccess() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("table", "pii_fields");
            JSONObject fields = new JSONObject();
            fields.put(columnName, "first");
            record.put("fields", fields);

            recordsArray.add(record);

            JSONObject record2 = new JSONObject();
            record2.put("table", "cards");
            JSONObject fields2 = new JSONObject();
            fields.put(columnName, "first");
            record2.put("fields", fields2);
            recordsArray.add(record2);


            records.put("records", recordsArray);
            String firstRequestUrl = "https://test.com/v1/vaults/vault123/pii_fields";
            String secondRequestUrl = "https://test.com/v1/vaults/vault123/cards";


            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse2 =
                    "{\"records\":[{\"skyflow_id\":\"id1\", \"tokens\":{\"first_name\":\"token1\"}}]}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.eq(new URL(firstRequestUrl)), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockResponse2);

            String mockResponse =
                    "{\"error\":{\"grpc_code\":3,\"http_code\":400,\"message\":\"Invalid field present in JSON cardholder_nam\",\"http_status\":\"Bad Request\",\"details\":[]}}";
            PowerMockito.when(HttpUtility.sendRequest(ArgumentMatchers.anyString(),
                            ArgumentMatchers.eq(new URL(secondRequestUrl)),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenThrow(new SkyflowException(500, mockResponse
                    ));

            JSONObject res = skyflowClient.insertBulk(records);

        } catch (SkyflowException e) {
            JSONObject partialError = e.getData();
            Assert.assertTrue(partialError.containsKey("records"));
            Assert.assertTrue(partialError.containsKey("errors"));
            JSONArray records = (JSONArray) e.getData().get("records");

            Assert.assertEquals(1, records.size());

            JSONArray errors = (JSONArray) e.getData().get("errors");
            Assert.assertEquals(1, errors.size());
        } catch (IOException e) {
            e.printStackTrace();
            fail(INVALID_EXCEPTION_THROWN);
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertBulkInterruptedException() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration("vaultID", "https://demo.com", new DemoTokenProvider());
            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("table", tableName);
            JSONObject fields = new JSONObject();
            fields.put(columnName, "first");
            record.put("fields", fields);

            recordsArray.add(record);

            records.put("records", recordsArray);
            PowerMockito.mockStatic(HttpUtility.class);

            InsertBulkOptions insertOptions = new InsertBulkOptions(true);
            PowerMockito.when(HttpUtility.sendRequest(ArgumentMatchers.anyString(),
                            ArgumentMatchers.<URL>any(),
                            ArgumentMatchers.<JSONObject>any(),
                            ArgumentMatchers.<String, String>anyMap()))
                    .thenThrow(new IOException("Exception occurred"));
            Callable<String> mockCallable = mock(Callable.class);
            when(mockCallable.call()).thenThrow(new InterruptedException("Thread was interrupted"));

            FutureTask<String> mockFutureTask = new FutureTask<>(mockCallable);
            mockFutureTask.run();

            when(mockFutureTask.get()).thenThrow(new InterruptedException("Thread was interrupted"));

            PowerMockito.mockStatic(HttpUtility.class);
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenThrow(new InterruptedException("Thread was interrupted"));


            JSONObject response = skyflowClient.insertBulk(records, insertOptions);
        } catch (SkyflowException e) {
            String error = e.getMessage();
            assertEquals(ErrorCode.ThreadExecutionException.getDescription(), error);
        } catch (IOException exception) {
            exception.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN);
        } catch (Exception e) {
            Assert.assertTrue((e.toString()).contains(ErrorCode.ThreadInterruptedException.getDescription()));
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void testInsertBulkEmptyRecords() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            records.put("records", recordsArray);

            InsertBulkOptions insertOptions = new InsertBulkOptions(false);
            JSONObject res = skyflowClient.insertBulk(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.EmptyRecords.getDescription(), skyflowException.getMessage());
        }
    }
}

