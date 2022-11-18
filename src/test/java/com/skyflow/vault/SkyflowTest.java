/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.vault;

import com.skyflow.Configuration;
import com.skyflow.common.utils.HttpUtility;
import com.skyflow.common.utils.TokenUtils;
import com.skyflow.entities.*;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class DemoTokenProvider implements TokenProvider {
    @Override
    public String getBearerToken() throws Exception {
        return "valid_token";
    }
}

class InvalidTokenProvider implements TokenProvider{
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
        SkyflowConfiguration testConfig = new SkyflowConfiguration(vaultID, vaultURL,new DemoTokenProvider());
        try {
            Skyflow skyflow = Skyflow.init(testConfig);
            assert(skyflow instanceof Skyflow);
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
        try{
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

            InsertOptions insertOptions = new InsertOptions();

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse =
              "{\"vaultID\":\"vault123\",\"responses\":[{\"records\":[{\"skyflow_id\":\"id1\"}]},{\"fields\":{\"first_name\":\"token1\"}}]}";
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
                    "{\"vaultID\":\"vault123\",\"responses\":[{\"records\":[{\"skyflow_id\":\"id1\"}]},{\"fields\":{\"first_name\":\"token1\"}}]}";
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

            UpsertOption upsertOption = new UpsertOption(tableName,columnName);
            InsertOptions insertOptions = new InsertOptions(new UpsertOption[]{upsertOption});

            PowerMockito.mockStatic(HttpUtility.class);
            String mockResponse =
                    "{\"vaultID\":\"vault123\",\"responses\":[{\"records\":[{\"skyflow_id\":\"id1\"}]},{\"fields\":{\"first_name\":\"token1\"}}]}";
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
            InsertOptions insertOptions = new InsertOptions(true,upsertOptions);
            JSONObject records = new JSONObject();
            JSONObject res = skyflowClient.insert(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidUpsertOptionType.getDescription(), skyflowException.getMessage());
        }
    }

    @Test
    public void testInsertWithInvalidTableInUpsertOptions() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            InsertOptions insertOptions = new InsertOptions(true,new UpsertOption[]{new UpsertOption(null,"column")});
            JSONObject records = new JSONObject();
            JSONObject res = skyflowClient.insert(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidTableInUpsertOption.getDescription(), skyflowException.getMessage());
        }
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            InsertOptions insertOptions = new InsertOptions(true,new UpsertOption[]{new UpsertOption("","column")});
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
            InsertOptions insertOptions = new InsertOptions(true,new UpsertOption[]{new UpsertOption("table1",null)});
            JSONObject records = new JSONObject();
            JSONObject res = skyflowClient.insert(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidColumnInUpsertOption.getDescription(), skyflowException.getMessage());
        }
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            InsertOptions insertOptions = new InsertOptions(true,new UpsertOption[]{new UpsertOption("table2","")});
            JSONObject records = new JSONObject();
            JSONObject res = skyflowClient.insert(records, insertOptions);
        } catch (SkyflowException skyflowException) {
            assertEquals(ErrorCode.InvalidColumnInUpsertOption.getDescription(), skyflowException.getMessage());
        }
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
            String mockResponse =  "{\"records\":[{\"token\":\"token123\",\"valueType\":\"INTEGER\",\"value\":\"10\"}]}";
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
            PowerMockito.when(HttpUtility.sendRequest(anyString(),ArgumentMatchers.<URL>any(), ArgumentMatchers.<JSONObject>any(), ArgumentMatchers.<String, String>anyMap())).thenThrow(new SkyflowException(404, mockResponse));

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

            JSONParser jsonParser=new JSONParser();
            JSONObject validRequest = (JSONObject)
              jsonParser.parse("{\"detokenizationParameters\":[{\"token\":\"token123\"}]}");
            PowerMockito.mockStatic(HttpUtility.class);
            String mockValidResponse = "{\"records\":[{\"token\":\"token123\",\"valueType\":\"INTEGER\",\"value\":\"10\"}]}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(), ArgumentMatchers.<URL>any(), eq(validRequest), ArgumentMatchers.<String, String>anyMap())).thenReturn(mockValidResponse);

            JSONObject invalidRequest = (JSONObject)
                    jsonParser.parse("{\"detokenizationParameters\":[{\"token\":\"invalidToken1\"}]}");
            String mockInvalidResponse =
                    "{\"error\":{\"grpc_code\":5,\"http_code\":404,\"message\":\"Token not found for invalidToken1\",\"http_status\":\"Not Found\",\"details\":[]}}";
            PowerMockito.when(HttpUtility.sendRequest(anyString(),ArgumentMatchers.<URL>any(), eq(invalidRequest), ArgumentMatchers.<String, String>anyMap())).thenThrow(new SkyflowException(404, mockInvalidResponse));


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
            String mockResponse =  "{\"records\":[{\"fields\":{\"age\":10,\"skyflow_id\":\"skyflowId123\"}}]}";
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
            String mockValidResponse =  "{\"records\":[{\"fields\":{\"age\":10,\"skyflow_id\":\"skyflowId123\"}}]}";
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

}


