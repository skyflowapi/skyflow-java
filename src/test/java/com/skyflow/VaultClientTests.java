package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.TokenMode;
import com.skyflow.generated.rest.auth.HttpBearerAuth;
import com.skyflow.generated.rest.models.*;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.tokens.ColumnValue;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.TokenizeRequest;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VaultClientTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static VaultClient vaultClient;
    private static String vaultID = null;
    private static String clusterID = null;
    private static String token = null;
    private static String table = null;
    private static String value = null;
    private static String columnGroup = null;
    private static String apiKey = null;
    private static ArrayList<String> tokens = null;
    private static ArrayList<HashMap<String, Object>> insertValues = null;
    private static ArrayList<HashMap<String, Object>> insertTokens = null;
    private static HashMap<String, Object> valueMap = null;
    private static HashMap<String, Object> tokenMap = null;
    private static VaultConfig vaultConfig;

    @BeforeClass
    public static void setup() {
        vaultID = "vault123";
        clusterID = "cluster123";
        token = "test_token";
        tokens = new ArrayList<>();
        table = "test_table";
        value = "test_value";
        columnGroup = "test_column_group";
        apiKey = "sky-ab123-abcd1234cdef1234abcd4321cdef4321";
        insertValues = new ArrayList<>();
        insertTokens = new ArrayList<>();
        valueMap = new HashMap<>();
        tokenMap = new HashMap<>();

        Credentials credentials = new Credentials();
        credentials.setApiKey(apiKey);

        vaultConfig = new VaultConfig();
        vaultConfig.setVaultId(vaultID);
        vaultConfig.setClusterId(clusterID);
        vaultConfig.setEnv(Env.PROD);
        vaultClient = new VaultClient(vaultConfig, credentials);
    }

    @Test
    public void testVaultClientGetApiClient() {
        try {
            Assert.assertNotNull(vaultClient.getApiClient());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testVaultClientGetRecordsAPI() {
        try {
            Assert.assertNotNull(vaultClient.getRecordsApi());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testVaultClientGetTokensAPI() {
        try {
            Assert.assertNotNull(vaultClient.getTokensApi());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testVaultClientGetQueryAPI() {
        try {
            Assert.assertNotNull(vaultClient.getQueryApi());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
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
    public void testGetDetokenizePayload() {
        try {
            tokens.add(token);
            tokens.add(token);
            DetokenizeRequest detokenizeRequest = DetokenizeRequest.builder().tokens(tokens).build();
            V1DetokenizePayload payload = vaultClient.getDetokenizePayload(detokenizeRequest);
            Assert.assertFalse(payload.getContinueOnError());
            Assert.assertEquals(2, payload.getDetokenizationParameters().size());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetBulkInsertRequestBody() {
        try {
            valueMap.clear();
            valueMap.put("test_column_1", "test_value_1");
            valueMap.put("test_column_2", "test_value_2");
            tokenMap.clear();
            tokenMap.put("test_column_1", "test_token_1");
            insertValues.clear();
            insertValues.add(valueMap);
            insertValues.add(valueMap);
            insertTokens.clear();
            insertTokens.add(tokenMap);
            InsertRequest insertRequest1 = InsertRequest.builder()
                    .table(table)
                    .values(insertValues)
                    .tokens(insertTokens)
                    .tokenMode(TokenMode.ENABLE)
                    .returnTokens(true)
                    .build();
            RecordServiceInsertRecordBody body1 = vaultClient.getBulkInsertRequestBody(insertRequest1);
            Assert.assertTrue(body1.getTokenization());
            Assert.assertNull(body1.getUpsert());
            Assert.assertEquals(2, body1.getRecords().size());

            InsertRequest insertRequest2 = InsertRequest.builder().table(table).values(insertValues).build();
            RecordServiceInsertRecordBody body2 = vaultClient.getBulkInsertRequestBody(insertRequest2);
            Assert.assertEquals(2, body2.getRecords().size());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetBatchInsertRequestBody() {
        try {
            valueMap.clear();
            valueMap.put("test_column_1", "test_value_1");
            valueMap.put("test_column_2", "test_value_2");
            tokenMap.clear();
            tokenMap.put("test_column_1", "test_token_1");
            insertValues.clear();
            insertValues.add(valueMap);
            insertValues.add(valueMap);
            insertTokens.clear();
            insertTokens.add(tokenMap);
            InsertRequest insertRequest1 = InsertRequest.builder()
                    .table(table)
                    .values(insertValues)
                    .tokens(insertTokens)
                    .tokenMode(TokenMode.ENABLE)
                    .returnTokens(false)
                    .build();
            RecordServiceBatchOperationBody body1 = vaultClient.getBatchInsertRequestBody(insertRequest1);
            Assert.assertTrue(body1.getContinueOnError());
            Assert.assertEquals(2, body1.getRecords().size());

            InsertRequest insertRequest2 = InsertRequest.builder().table(table).values(insertValues).build();
            RecordServiceBatchOperationBody body2 = vaultClient.getBatchInsertRequestBody(insertRequest2);
            Assert.assertEquals(2, body2.getRecords().size());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetUpdateRequestBodyWithTokens() {
        try {
            valueMap.clear();
            valueMap.put("test_column_1", "test_value_1");
            valueMap.put("test_column_2", "test_value_2");
            tokenMap.clear();
            tokenMap.put("test_column_1", "test_token_1");
            UpdateRequest updateRequest = UpdateRequest.builder()
                    .table(table)
                    .data(valueMap)
                    .tokens(tokenMap)
                    .returnTokens(true)
                    .build();
            RecordServiceUpdateRecordBody body = vaultClient.getUpdateRequestBody(updateRequest);
            Assert.assertTrue(body.getTokenization());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetUpdateRequestBodyWithoutTokens() {
        try {
            valueMap.clear();
            valueMap.put("test_column_1", "test_value_1");
            valueMap.put("test_column_2", "test_value_2");
            tokenMap.clear();
            tokenMap.put("test_column_1", "test_token_1");
            UpdateRequest updateRequest = UpdateRequest.builder()
                    .table(table)
                    .data(valueMap)
                    .returnTokens(false)
                    .build();
            RecordServiceUpdateRecordBody body = vaultClient.getUpdateRequestBody(updateRequest);
            Assert.assertFalse(body.getTokenization());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetTokenizePayload() {
        try {
            ColumnValue columnValue = ColumnValue.builder().value(value).columnGroup(columnGroup).build();
            List<ColumnValue> columnValues = new ArrayList<>();
            columnValues.add(columnValue);
            TokenizeRequest tokenizeRequest = TokenizeRequest.builder().values(columnValues).build();
            V1TokenizePayload payload = vaultClient.getTokenizePayload(tokenizeRequest);
            Assert.assertEquals(1, payload.getTokenizationParameters().size());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testSetBearerToken() {
        try {
            Dotenv dotenv = Dotenv.load();
            String bearerToken = dotenv.get("TEST_REUSABLE_TOKEN");
            Credentials credentials = new Credentials();
            credentials.setToken(bearerToken);
            vaultConfig.setCredentials(credentials);
            vaultClient.updateVaultConfig();

            // regular scenario
            vaultClient.setBearerToken();

            // re-use scenario
            vaultClient.setBearerToken();
            HttpBearerAuth auth = (HttpBearerAuth) vaultClient.getApiClient().getAuthentication("Bearer");
            Assert.assertEquals(bearerToken, auth.getBearerToken());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testSetBearerTokenWithApiKey() {
        try {
            Credentials credentials = new Credentials();
            credentials.setApiKey(apiKey);
            vaultConfig.setCredentials(null);
            vaultClient.updateVaultConfig();
            vaultClient.setCommonCredentials(credentials);

            // regular scenario
            vaultClient.setBearerToken();

            // re-use scenario
            vaultClient.setBearerToken();
            HttpBearerAuth auth = (HttpBearerAuth) vaultClient.getApiClient().getAuthentication("Bearer");
            Assert.assertEquals(apiKey, auth.getBearerToken());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testSetBearerTokenWithEnvCredentials() {
        try {
            vaultConfig.setCredentials(null);
            vaultClient.updateVaultConfig();
            vaultClient.setCommonCredentials(null);
            Assert.assertNull(vaultClient.getVaultConfig().getCredentials());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
