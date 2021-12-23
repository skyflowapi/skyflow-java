package com.skyflow.vault;

import com.skyflow.common.utils.HttpUtility;
import com.skyflow.entities.InsertOptions;
import com.skyflow.entities.RedactionType;
import com.skyflow.entities.SkyflowConfiguration;
import com.skyflow.entities.TokenProvider;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

class DemoTokenProvider implements TokenProvider {
    @Override
    public String getBearerToken() throws Exception {
        String response = HttpUtility.sendRequest("GET", System.getProperty("TEST_TOKEN_URL"), null, null);
        return (String) ((JSONObject) (new JSONParser().parse(response))).get("accessToken");
    }
}

public class SkyflowTest {

    private static String vaultID = null;
    private static String vaultURL = null;
    private static String skyflowId = null;
    private static String token = null;
    private static String tableName = null;
    private static String columnName = null;


    @BeforeClass
    public static void setup() {
        vaultID = System.getProperty("TEST_VAULT_ID");
        vaultURL = System.getProperty("TEST_VAULT_URL");
        skyflowId = System.getProperty("TEST_SKYFLOW_ID");
        token = System.getProperty("TEST_TOKEN");
        tableName = "pii_fields";
        columnName = "first_name";
    }

    @Test
    public void testValidConfig() {
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
            JSONObject res = skyflowClient.insert(records, insertOptions);
            JSONArray responseRecords = (JSONArray) res.get("records");

            assertEquals(1, responseRecords.size());
            assertEquals(tableName, ((JSONObject) responseRecords.get(0)).get("table"));
            assertNotNull(((JSONObject) responseRecords.get(0)).get("fields"));
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
        }
    }

    @Test
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
            JSONObject res = skyflowClient.insert(records, insertOptions);
            JSONArray responseRecords = (JSONArray) res.get("records");

            assertEquals(1, responseRecords.size());
            assertEquals(tableName, ((JSONObject) responseRecords.get(0)).get("table"));
            assertNotNull(((JSONObject) responseRecords.get(0)).get("skyflow_id"));
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
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

            JSONObject res = skyflowClient.detokenize(records);
            JSONArray responseRecords = (JSONArray) res.get("records");

            assertEquals(1, responseRecords.size());
            assertEquals(token, ((JSONObject) responseRecords.get(0)).get("token"));
            assertTrue(((JSONObject) responseRecords.get(0)).containsKey("value"));
        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
        }
    }

    @Test
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

            JSONObject res = skyflowClient.detokenize(records);
            JSONArray responseRecords = (JSONArray) res.get("records");

            assertEquals(1, responseRecords.size());
            assertEquals(token, ((JSONObject) responseRecords.get(0)).get("token"));
            assertTrue(((JSONObject) responseRecords.get(0)).containsKey("value"));
        } catch (SkyflowException skyflowException) {
            JSONArray errors = (JSONArray) skyflowException.getData().get("errors");
            assertEquals(1, errors.size());
        }
    }

    @Test
    public void testDetokenizePartialError() {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("token", "invalidToken");
            JSONObject validRecord = new JSONObject();
            validRecord.put("token", token);
            recordsArray.add(record);
            recordsArray.add(validRecord);
            records.put("records", recordsArray);

            JSONObject res = skyflowClient.detokenize(records);
            JSONArray responseRecords = (JSONArray) res.get("records");

            assertEquals(1, responseRecords.size());
            assertEquals(token, ((JSONObject) responseRecords.get(0)).get("token"));
            assertTrue(((JSONObject) responseRecords.get(0)).containsKey("value"));
        } catch (SkyflowException skyflowException) {
            JSONArray responseRecords = (JSONArray) skyflowException.getData().get("records");
            JSONArray errors = (JSONArray) skyflowException.getData().get("errors");
            assertEquals(1, errors.size());
            assertEquals(1, responseRecords.size());
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

            JSONObject response = skyflowClient.getById(records);
            JSONArray responseRecords = (JSONArray) response.get("records");

            assertEquals(1, responseRecords.size());
            assertEquals(tableName, ((JSONObject) responseRecords.get(0)).get("table"));
            assertNotNull(((JSONObject) responseRecords.get(0)).get("fields"));

        } catch (SkyflowException skyflowException) {
            skyflowException.printStackTrace();
        }
    }

    @Test
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

            JSONObject response = skyflowClient.getById(records);

        } catch (SkyflowException e) {
            JSONArray errors = (JSONArray) e.getData().get("errors");
            assertEquals(1, errors.size());
        }
    }

    @Test
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
            ids.add("id");
            record2.put("ids", id2);
            record2.put("table", "invalidTable");
            record2.put("redaction", RedactionType.PLAIN_TEXT.toString());
            recordsArray.add(record2);

            records.put("records", recordsArray);

            JSONObject response = skyflowClient.getById(records);

        } catch (SkyflowException e) {
            JSONObject partialError = e.getData();
            assertTrue(partialError.containsKey("records"));
            assertTrue(partialError.containsKey("errors"));
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


