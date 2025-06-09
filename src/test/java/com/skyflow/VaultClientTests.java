package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.DetectEntities;
import com.skyflow.enums.Env;
import com.skyflow.enums.TokenMode;
import com.skyflow.generated.rest.resources.records.RecordsClient;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceBatchOperationBody;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceInsertRecordBody;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceUpdateRecordBody;
import com.skyflow.generated.rest.resources.tokens.requests.V1DetokenizePayload;
import com.skyflow.generated.rest.resources.tokens.requests.V1TokenizePayload;
import com.skyflow.generated.rest.types.DeidentifyStringResponse;
import com.skyflow.generated.rest.types.DetectedEntity;
import com.skyflow.generated.rest.types.EntityLocation;
import com.skyflow.generated.rest.types.V1Byot;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.detect.*;
import com.skyflow.vault.tokens.ColumnValue;
import com.skyflow.vault.tokens.DetokenizeData;
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
    private static ArrayList<DetokenizeData> detokenizeData = null;
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
        detokenizeData = new ArrayList<>();
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
    public void testVaultClientGetRecordsAPI() {
        try {
            Dotenv dotenv = Dotenv.load();
            String bearerToken = dotenv.get("TEST_REUSABLE_TOKEN");
            if (bearerToken == null) {
                Assert.fail("TEST_REUSABLE_TOKEN not set in environment variables");
            }

            Credentials credentials = new Credentials();
            credentials.setCredentialsString("{\"bearer_token\": \"" + bearerToken + "\"}");
            vaultConfig.setCredentials(credentials);
            vaultClient.updateVaultConfig();
            vaultClient.setBearerToken();

            Assert.assertNotNull("ApiClient should not be null after setting the bearer token", vaultClient.getRecordsApi());

            RecordsClient recordsClient = vaultClient.getRecordsApi();
            Assert.assertNotNull("RecordsClient should not be null", recordsClient);
        } catch (Exception e) {

            e.printStackTrace();
            Assert.fail("Should not have thrown any exception: " + e.getMessage());
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
            DetokenizeData detokenizeDataRecord1 = new DetokenizeData(token);
            DetokenizeData detokenizeDataRecord2 = new DetokenizeData(token);
            detokenizeData.add(detokenizeDataRecord1);
            detokenizeData.add(detokenizeDataRecord2);
            DetokenizeRequest detokenizeRequest = DetokenizeRequest.builder()
                    .detokenizeData(detokenizeData)
                    .downloadURL(true)
                    .continueOnError(false)
                    .build();
            V1DetokenizePayload payload = vaultClient.getDetokenizePayload(detokenizeRequest);
            Assert.assertFalse(payload.getContinueOnError().get());
            Assert.assertTrue(payload.getDownloadUrl().get());
            Assert.assertEquals(2, payload.getDetokenizationParameters().get().size());
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
            Assert.assertTrue(body1.getTokenization().get());
            Assert.assertEquals(V1Byot.ENABLE, body1.getByot().get());
            Assert.assertEquals(2, body1.getRecords().get().size());

            InsertRequest insertRequest2 = InsertRequest.builder()
                    .table(table)
                    .values(insertValues)
                    .build();
            RecordServiceInsertRecordBody body2 = vaultClient.getBulkInsertRequestBody(insertRequest2);
            Assert.assertEquals(2, body2.getRecords().get().size());
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
            Assert.assertTrue(body1.getContinueOnError().get());
            Assert.assertEquals(2, body1.getRecords().get().size());

            InsertRequest insertRequest2 = InsertRequest.builder().table(table).values(insertValues).build();
            RecordServiceBatchOperationBody body2 = vaultClient.getBatchInsertRequestBody(insertRequest2);
            Assert.assertEquals(2, body2.getRecords().get().size());
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
            Assert.assertTrue(body.getTokenization().get());
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
            Assert.assertFalse(body.getTokenization().get());
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
            Assert.assertEquals(1, payload.getTokenizationParameters().get().size());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testSetBearerToken() {
        try {
            Dotenv dotenv = Dotenv.load();
            String bearerToken = dotenv.get("TEST_REUSABLE_TOKEN");
            if (bearerToken == null) {
                Assert.fail("TEST_REUSABLE_TOKEN not set in environment variables");
            }

            Credentials credentials = new Credentials();
            credentials.setCredentialsString("{\"bearer_token\": \"" + bearerToken + "\"}");
            vaultConfig.setCredentials(credentials);
            vaultClient.updateVaultConfig();

            vaultClient.setBearerToken();

            Assert.assertNotNull(vaultClient.getTokensApi());

            vaultClient.setBearerToken();
            Assert.assertNotNull(vaultClient.getTokensApi());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN + ": " + e.getMessage());
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
            // Note: We can't directly access the token from apiClient anymore; this test assumes the token is set correctly
            Assert.assertNotNull(vaultClient.getTokensApi()); // Indirectly verifies client is built
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

    @Test
    public void testGetDeIdentifyTextResponse() {
        List<DetectedEntity> entities = new ArrayList<>();
        EntityLocation location = EntityLocation.builder()
                .startIndex(2)
                .endIndex(10)
                .startIndexProcessed(3)
                .endIndexProcessed(8)
                .build();

        DetectedEntity detectedEntity = DetectedEntity.builder()
                .token("token123")
                .value("value123")
                .location(location)
                .build();
        entities.add(detectedEntity);

        DeidentifyStringResponse response = DeidentifyStringResponse.builder()
                .processedText("processed text")
                .wordCount(2)
                .characterCount(13)
                .entities(entities)
                .build();


        DeidentifyTextResponse result = vaultClient.getDeIdentifyTextResponse(response);

        Assert.assertNotNull(result);
        Assert.assertEquals("processed text", result.getProcessedText());
        Assert.assertEquals(2, result.getWordCount());
        Assert.assertEquals(13, result.getCharCount());
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("token123", result.getEntities().get(0).getToken());
        Assert.assertEquals("value123", result.getEntities().get(0).getValue());
        Assert.assertEquals(2, result.getEntities().get(0).getTextIndex().getStart());
        Assert.assertEquals(10, result.getEntities().get(0).getTextIndex().getEnd());
        Assert.assertEquals(3, result.getEntities().get(0).getProcessedIndex().getStart());
        Assert.assertEquals(8, result.getEntities().get(0).getProcessedIndex().getEnd());
    }

    @Test
    public void testGetDeidentifyStringRequest() {

        List<DetectEntities> detectEntitiesList = new ArrayList<>();
        detectEntitiesList.add(DetectEntities.NAME);

        List<DetectEntities> vaultTokenList = new ArrayList<>();
        vaultTokenList.add(DetectEntities.SSN);


        List<DetectEntities> entityOnlyList = new ArrayList<>();
        entityOnlyList.add(DetectEntities.DOB);

        List<DetectEntities> entityUniqueCounterList = new ArrayList<>();
        entityUniqueCounterList.add(DetectEntities.NAME);


        List<String> restrictRegexList = new ArrayList<>();
        restrictRegexList.add("([0-9]{3}-[0-9]{2}-[0-9]{4})");

        TokenFormat tokenFormat = TokenFormat.builder()
                .vaultToken(vaultTokenList)
                .entityOnly(entityOnlyList)
                .entityUniqueCounter(entityUniqueCounterList)
                .build();


        List<DetectEntities> detectEntitiesTransformationList = new ArrayList<>();
        detectEntitiesTransformationList.add(DetectEntities.DOB);
        detectEntitiesTransformationList.add(DetectEntities.DATE);

        DateTransformation dateTransformation = new DateTransformation(20, 5, detectEntitiesTransformationList);
        Transformations transformations = new Transformations(dateTransformation);


        DeidentifyTextRequest req = DeidentifyTextRequest.builder()
                .text("Sensitive data to deidentify, like Name: Joy SSN 123-45-6789 and DOB 01-01-2000.")
                .entities(detectEntitiesList)
                .restrictRegexList(restrictRegexList)
                .tokenFormat(tokenFormat)
                .transformations(transformations)
                .build();

    }
}
