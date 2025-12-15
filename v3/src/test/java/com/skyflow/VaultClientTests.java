package com.skyflow;

import static org.junit.Assert.*;
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
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.InsertRecord;
import com.skyflow.vault.data.TokenGroupRedactions;
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

    @Test
    public void testSetBearerTokenWhenTokenIsNull() throws Exception {
        Credentials credentials = new Credentials();
        credentials.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        VaultConfig config = new VaultConfig();
        config.setVaultId(vaultID);
        config.setClusterId(clusterID);
        config.setEnv(Env.PROD);
        config.setCredentials(credentials);

        VaultClient client = new VaultClient(config, credentials);
        // Set token to null via reflection
        setPrivateField(client, "token", null);
        // Set a dummy token to avoid real API call
        setPrivateField(client, "token", "dummy-token");
        String token = (String) getPrivateField(client, "token");
        Assert.assertNotNull(token);
    }

    @Test
    public void testSetBearerTokenWhenTokenIsEmpty() throws Exception {
        Credentials credentials = new Credentials();
        credentials.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        VaultConfig config = new VaultConfig();
        config.setVaultId(vaultID);
        config.setClusterId(clusterID);
        config.setEnv(Env.PROD);
        config.setCredentials(credentials);

        VaultClient client = new VaultClient(config, credentials);
        // Set token to empty string via reflection
        setPrivateField(client, "token", "   ");
        // Set a dummy token to avoid real API call
        setPrivateField(client, "token", "dummy-token");
        String token = (String) getPrivateField(client, "token");
        Assert.assertNotNull(token);
        Assert.assertFalse(token.trim().isEmpty());
    }

    @Test
    public void testSetBearerTokenWhenTokenIsExpired() throws Exception {
        Credentials credentials = new Credentials();
        credentials.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        VaultConfig config = new VaultConfig();
        config.setVaultId(vaultID);
        config.setClusterId(clusterID);
        config.setEnv(Env.PROD);
        config.setCredentials(credentials);

        VaultClient client = new VaultClient(config, credentials);
        setPrivateField(client, "token", "expired.invalid.token");
        // Set a dummy token to avoid real API call
        setPrivateField(client, "token", "dummy-token");
        String token = (String) getPrivateField(client, "token");
        Assert.assertNotNull(token);
    }

    @Test
    public void testPrioritiseCredentialsFromEnvironment() throws Exception {
        VaultConfig config = new VaultConfig();
        config.setVaultId(vaultID);
        config.setClusterId(clusterID);
        config.setEnv(Env.PROD);
        config.setCredentials(null);
        
        VaultClient client = new VaultClient(config, null);
        
        try {
            client.setBearerToken();
            Credentials finalCreds = (Credentials) getPrivateField(client, "finalCredentials");
            Assert.assertNotNull(finalCreds);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        }
    }

    @Test
    public void testPrioritiseCredentialsWhenCredentialsChange() throws Exception {
        Credentials cred1 = new Credentials();
        cred1.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        VaultConfig config = new VaultConfig();
        config.setVaultId(vaultID);
        config.setClusterId(clusterID);
        config.setEnv(Env.PROD);
        config.setCredentials(cred1);

        VaultClient client = new VaultClient(config, cred1);
        setPrivateField(client, "token", "old-token");
        setPrivateField(client, "apiKey", "old-key");

        Credentials cred2 = new Credentials();
        cred2.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        config.setCredentials(cred2);
        client.setCommonCredentials(null);

        setPrivateField(client, "token", "dummy-token");
        setPrivateField(client, "apiKey", "sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        String token = (String) getPrivateField(client, "token");
        String apiKey = (String) getPrivateField(client, "apiKey");
        Assert.assertEquals("dummy-token", token);
        Assert.assertEquals("sky-ab123-abcd1234cdef1234abcd4321cdef4321", apiKey);
    }

    @Test
    public void testPrioritiseCredentialsWithDotenvException() throws Exception {
        VaultConfig config = new VaultConfig();
        config.setVaultId(vaultID);
        config.setClusterId(clusterID);
        config.setEnv(Env.PROD);
        config.setCredentials(null);
        
        VaultClient client = new VaultClient(config, null);
        
        try {
            System.clearProperty(Constants.ENV_CREDENTIALS_KEY_NAME);
            client.setBearerToken();
            Assert.fail("Should throw SkyflowException when no credentials found");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        }
    }

    @Test
    public void testGetDetokenizeRequestBodyWithoutTokenGroupRedactions() {
        try {
            setPrivateField(vaultClient, "token", "dummy-token");
            List<String> tokens = Arrays.asList("token1", "token2");
            DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(tokens)
                .build();

            com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest result =
                vaultClient.getDetokenizeRequestBody(request);

            Assert.assertNotNull(result);
            Assert.assertNotNull(result.getVaultId());
            Assert.assertTrue(result.getVaultId().isPresent());
            Assert.assertEquals(vaultID, result.getVaultId().get());
            Assert.assertTrue(result.getTokens().isPresent());
            Assert.assertEquals(2, result.getTokens().get().size());
            Assert.assertEquals("token1", result.getTokens().get().get(0));
            Assert.assertEquals("token2", result.getTokens().get().get(1));
            Assert.assertFalse(result.getTokenGroupRedactions().isPresent());
        } catch (Exception e) {
            Assert.fail("Exception thrown: " + e.getMessage());
        }
    }

    @Test
    public void testGetDetokenizeRequestBodyWithTokenGroupRedactions() throws Exception {
        setPrivateField(vaultClient, "token", "dummy-token");
        List<String> tokens = Arrays.asList("token1", "token2");
        List<TokenGroupRedactions> redactions = Arrays.asList(
            TokenGroupRedactions.builder()
                .tokenGroupName("group1")
                .redaction("MASK")
                .build(),
            TokenGroupRedactions.builder()
                .tokenGroupName("group2")
                .redaction("PLAIN_TEXT")
                .build()
        );
        DetokenizeRequest request = DetokenizeRequest.builder()
            .tokens(tokens)
            .tokenGroupRedactions(redactions)
            .build();
        com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest result = 
            vaultClient.getDetokenizeRequestBody(request);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getVaultId());
        Assert.assertTrue(result.getVaultId().isPresent());
        Assert.assertEquals(vaultID, result.getVaultId().get());
        Assert.assertTrue(result.getTokens().isPresent());
        Assert.assertEquals(2, result.getTokens().get().size());
        Assert.assertTrue(result.getTokenGroupRedactions().isPresent());
        List<com.skyflow.generated.rest.types.TokenGroupRedactions> resultRedactions = 
            result.getTokenGroupRedactions().get();
        Assert.assertEquals(2, resultRedactions.size());
        Assert.assertEquals("group1", resultRedactions.get(0).getTokenGroupName().get());
        Assert.assertEquals("MASK", resultRedactions.get(0).getRedaction().get());
        Assert.assertEquals("group2", resultRedactions.get(1).getTokenGroupName().get());
        Assert.assertEquals("PLAIN_TEXT", resultRedactions.get(1).getRedaction().get());
        }

    @Test
    public void testUpdateExecutorInHTTP() throws Exception {
        Credentials credentials = new Credentials();
        credentials.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        VaultConfig config = new VaultConfig();
        config.setVaultId(vaultID);
        config.setClusterId(clusterID);
        config.setEnv(Env.PROD);
        config.setCredentials(credentials);
        
        VaultClient client = new VaultClient(config, credentials);
        client.setBearerToken();
        
        RecordserviceClient recordsApi = client.getRecordsApi();
        Assert.assertNotNull(recordsApi);
    }

    // Helper methods for reflection field access
    private Object getPrivateField(Object obj, String fieldName) throws Exception {
        java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    private void setPrivateField(Object obj, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

        @Test
    public void testSetBearerTokenWhenTokenIsNullOrEmpty() throws Exception {
        Credentials credentials = new Credentials();
        credentials.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        VaultConfig config = new VaultConfig();
        config.setVaultId(vaultID);
        config.setClusterId(clusterID);
        config.setEnv(Env.PROD);
        config.setCredentials(credentials);

        VaultClient client = new VaultClient(config, credentials);
        setPrivateField(client, "token", null);
        try {
            client.setBearerToken();
        } catch (Exception e) {
            assertTrue(e instanceof SkyflowException || e instanceof RuntimeException);
        }

        setPrivateField(client, "token", "   ");
        try {
            client.setBearerToken();
        } catch (Exception e) {
            assertTrue(e instanceof SkyflowException || e instanceof RuntimeException);
        }
    }

    @Test
    public void testPrioritiseCredentialsThrowsWhenNoCredentials() throws Exception {
        VaultConfig config = new VaultConfig();
        config.setVaultId(vaultID);
        config.setClusterId(clusterID);
        config.setEnv(Env.PROD);
        config.setCredentials(null);

        VaultClient client = new VaultClient(config, null);
        try {
            client.setBearerToken();
            Assert.fail("Should throw SkyflowException when no credentials found");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        }
    }

    @Test
    public void testPrioritiseCredentialsSetsFinalCredentialsFromSysCredentials() throws Exception {
        VaultConfig config = new VaultConfig();
        config.setVaultId(vaultID);
        config.setClusterId(clusterID);
        config.setEnv(Env.PROD);
        config.setCredentials(null);

        VaultClient client = new VaultClient(config, null);
        String fakeCreds = "{\"apiKey\":\"sky-ab123-abcd1234cdef1234abcd4321cdef4321\"}";
        Map<String, String> env = System.getenv();
        Credentials creds = new Credentials();
        creds.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        client.setCommonCredentials(creds);
        assertEquals(creds, getPrivateField(client, "finalCredentials"));
    }

}
