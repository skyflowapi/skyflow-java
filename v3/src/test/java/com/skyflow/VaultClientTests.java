package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.UpsertType;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.resources.recordservice.RecordserviceClient;
import com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest;
import com.skyflow.generated.rest.types.InsertRecordData;
import com.skyflow.utils.Constants;
import com.skyflow.utils.SdkVersion;
import com.skyflow.vault.data.InsertRecord;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

public class VaultClientTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static VaultClient vaultClient;
    private static String vaultID = null;
    private static String clusterID = null;
    private static VaultConfig vaultConfig;

    @BeforeClass
    public static void setup() throws SkyflowException {
        vaultID = "vault123";
        clusterID = "cluster123";
        vaultConfig = new VaultConfig();
        vaultConfig.setVaultId(vaultID);
        vaultConfig.setClusterId(clusterID);
        vaultConfig.setEnv(Env.PROD);

        Credentials credentials = new Credentials();
        credentials.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        vaultConfig.setCredentials(credentials);
        vaultClient = new VaultClient(vaultConfig, credentials);
        vaultClient.setBearerToken();
        SdkVersion.setSdkPrefix(Constants.SDK_PREFIX);
    }

    @Test
    public void testVaultClientGetRecordsAPI() {
        try {
            RecordserviceClient recordsClient = vaultClient.getRecordsApi();
            Assert.assertNotNull(recordsClient);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN + e.getMessage());
        }
    }

    @Test
    public void testVaultClientGetVaultConfig() {
        try {
            VaultConfig config = vaultClient.getVaultConfig();
            Assert.assertNotNull(config);
            Assert.assertEquals(vaultID, config.getVaultId());
            Assert.assertEquals(clusterID, config.getClusterId());
            Assert.assertEquals(Env.PROD, config.getEnv());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testSetBearerToken() {
        try {
            Credentials credentials = new Credentials();
            credentials.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
            vaultConfig.setCredentials(credentials);
            vaultClient = new VaultClient(vaultConfig, credentials);
            vaultClient.setBearerToken();
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN + ": " + e.getMessage());
        }
    }

    @Test
    public void testSetBearerTokenWithApiKey() {
        try {
            Credentials credentials = new Credentials();
            credentials.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321"); // Use a non-null dummy API key
//            vaultConfig.setCredentials(credentials);
//            vaultClient.updateVaultConfig();
            vaultClient.setCommonCredentials(credentials);

            // regular scenario
            vaultClient.setBearerToken();

            // re-use scenario
            vaultClient.setBearerToken();

            // If no exception is thrown, the test passes
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN + ": " + e.getMessage());
        }
    }

    @Test
    public void testSetBearerTokenWithEnvCredentials() {
        try {
            Dotenv dotenv = Dotenv.load();
            Credentials credentials = new Credentials();
            credentials.setCredentialsString(dotenv.get("SKYFLOW_CREDENTIALS"));

            // no credentials set at vault config and skyflow levels
            vaultConfig.setCredentials(null);
            vaultClient.setCommonCredentials(null);

            vaultClient.setBearerToken();

            // Credentials at ENV level should be prioritised
            Assert.assertEquals(
                    credentials.getCredentialsString(),
                    ((Credentials) getPrivateField(vaultClient, "finalCredentials")).getCredentialsString()
            );

        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
            Assert.assertNull(vaultClient.getVaultConfig().getCredentials());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testPrioritiseCredentialsWithVaultConfigCredentials() throws Exception {
        // set credentials at vault config level
        Credentials credentials = new Credentials();
        credentials.setApiKey("test_api_key");
        vaultConfig.setCredentials(credentials);

        // set credentials at skyflow level
        Credentials commonCredentials = new Credentials();
        commonCredentials.setToken("test_common_token");
        vaultClient.setCommonCredentials(commonCredentials);

        // vault config credentials should be prioritised
        Assert.assertEquals(credentials, getPrivateField(vaultClient, "finalCredentials"));
    }

    @Test
    public void testPrioritiseCredentialsWithCommonCredentials() throws Exception {
        // no credentials in vault config level
        vaultConfig.setCredentials(null);

        // set credentials at skyflow level
        Credentials credentials = new Credentials();
        credentials.setApiKey("common_api_key");
        vaultClient.setCommonCredentials(credentials);

        // common credentials should be prioritised
        Assert.assertEquals(credentials, getPrivateField(vaultClient, "finalCredentials"));
    }

    @Test
    public void testEmptyRecords() {
        com.skyflow.vault.data.InsertRequest request =
                com.skyflow.vault.data.InsertRequest.builder().records(new ArrayList<>()).build();
        InsertRequest result = vaultClient.getBulkInsertRequestBody(request, vaultConfig);
        Assert.assertTrue(result.getRecords().get().isEmpty());
    }

    @Test
    public void testTableAtRequestLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        InsertRecord record = InsertRecord.builder().data(data).build();
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(record);

        com.skyflow.vault.data.InsertRequest request =
                com.skyflow.vault.data.InsertRequest.builder()
                        .table("table1")
                        .records(records)
                        .build();

        InsertRequest result = vaultClient.getBulkInsertRequestBody(request, vaultConfig);
        Assert.assertEquals("table1", result.getTableName().get());
        List<InsertRecordData> recordData = result.getRecords().get();
        Assert.assertEquals("value", recordData.get(0).getData().get().get("key"));
    }

    @Test
    public void testTableAtRecordLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        InsertRecord record = InsertRecord.builder().data(data).table("table2").build();

        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(record);

        com.skyflow.vault.data.InsertRequest request =
                com.skyflow.vault.data.InsertRequest.builder()
                        .records(records)
                        .build();

        InsertRequest result = vaultClient.getBulkInsertRequestBody(request, vaultConfig);
        Assert.assertEquals("table2", result.getRecords().get().get(0).getTableName().get());
    }

    @Test
    public void testUpsertAtRequestLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        InsertRecord record = InsertRecord.builder().data(data).build();
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(record);


        List<String> upsertColumns = Arrays.asList("col1");
        com.skyflow.vault.data.InsertRequest request =
                com.skyflow.vault.data.InsertRequest.builder()
                        .records(records)
                        .upsert(upsertColumns)
                        .upsertType(UpsertType.REPLACE)
                        .build();

        InsertRequest result = vaultClient.getBulkInsertRequestBody(request, vaultConfig);
        Assert.assertNotNull(result.getUpsert());
        Assert.assertEquals("col1", result.getUpsert().get().getUniqueColumns().get().get(0));
        Assert.assertEquals("REPLACE", result.getUpsert().get().getUpdateType().get().name());
    }

    @Test
    public void testUpsertAtRecordLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        List<String> upsertColumns = Arrays.asList("col2");
        InsertRecord record = InsertRecord.builder().data(data).upsert(upsertColumns).upsertType(UpsertType.UPDATE).build();
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(record);

        com.skyflow.vault.data.InsertRequest request =
                com.skyflow.vault.data.InsertRequest.builder()
                        .records(records)
                        .build();

        InsertRequest result = vaultClient.getBulkInsertRequestBody(request, vaultConfig);
        Assert.assertNotNull(result.getRecords().get().get(0).getUpsert());
        Assert.assertEquals("col2", result.getRecords().get().get(0).getUpsert().get().getUniqueColumns().get().get(0));
        Assert.assertEquals("UPDATE", result.getRecords().get().get(0).getUpsert().get().getUpdateType().get().name());
    }

    @Test
    public void testMixedTableAndUpsertLevels() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        List<String> upsertColumns = Arrays.asList("col3");
        InsertRecord record = InsertRecord.builder().data(data).table("table3").upsert(upsertColumns).build();
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(record);


        com.skyflow.vault.data.InsertRequest request =
                com.skyflow.vault.data.InsertRequest.builder()
                        .table("table4")
                        .upsert(Arrays.asList("col4"))
                        .records(records)
                        .build();

        InsertRequest result = vaultClient.getBulkInsertRequestBody(request, vaultConfig);
        Assert.assertEquals("table4", result.getTableName().get());
        Assert.assertEquals("table3", result.getRecords().get().get(0).getTableName().get());
        Assert.assertEquals("col3", result.getRecords().get().get(0).getUpsert().get().getUniqueColumns().get().get(0));
        Assert.assertEquals("col4", result.getUpsert().get().getUniqueColumns().get().get(0));
    }


    // Helper methods for reflection field access
    private Object getPrivateField(Object obj, String fieldName) throws Exception {
        java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

}
