package com.skyflow.utils;

import com.google.gson.JsonObject;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.UpsertType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest;
import com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest;
import com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest;
import com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest;
import com.skyflow.generated.rest.types.FlowEnumUpdateType;
import com.skyflow.generated.rest.types.FlowTokenizeResponseObjectToken;
import com.skyflow.generated.rest.types.V1DeleteTokenResponseObject;
import com.skyflow.generated.rest.types.V1FlowDeleteTokenResponse;
import com.skyflow.generated.rest.types.V1FlowDetokenizeResponse;
import com.skyflow.generated.rest.types.V1FlowDetokenizeResponseObject;
import com.skyflow.generated.rest.types.V1FlowTokenizeResponse;
import com.skyflow.generated.rest.types.V1FlowTokenizeResponseObject;
import com.skyflow.generated.rest.types.V1InsertRecordData;
import com.skyflow.generated.rest.types.V1InsertResponse;
import com.skyflow.generated.rest.types.V1RecordResponseObject;
import com.skyflow.vault.data.BulkDeleteTokensRequest;
import com.skyflow.vault.data.BulkDeleteTokensResponse;
import com.skyflow.vault.data.BulkDetokenizeRequest;
import com.skyflow.vault.data.BulkDetokenizeResponse;
import com.skyflow.vault.data.BulkInsertRecord;
import com.skyflow.vault.data.BulkInsertRequest;
import com.skyflow.vault.data.BulkInsertResponse;
import com.skyflow.vault.data.BulkTokenGroupRedactions;
import com.skyflow.vault.data.BulkTokenizeRecord;
import com.skyflow.vault.data.BulkTokenizeRequest;
import com.skyflow.vault.data.BulkTokenizeResponse;
import com.skyflow.vault.data.DeleteTokensRequest;
import com.skyflow.vault.data.DeleteTokensResponse;
import com.skyflow.vault.data.DetokenizeData;
import com.skyflow.vault.data.DetokenizeRecordResponse;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.DetokenizeResponse;
import com.skyflow.vault.data.ErrorRecord;
import com.skyflow.vault.data.InsertRecord;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;
import com.skyflow.vault.data.TokenGroupRedactions;
import com.skyflow.vault.data.TokenizeData;
import com.skyflow.vault.data.TokenizeRecord;
import com.skyflow.vault.data.TokenizeRequest;
import com.skyflow.vault.data.TokenizeResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UtilsTests {

    private static final String ENV_FILE = ".env";
    private byte[] originalEnvContent;

    @Before
    public void saveEnvFileState() throws IOException {
        File f = new File(ENV_FILE);
        originalEnvContent = f.exists() ? Files.readAllBytes(Paths.get(ENV_FILE)) : null;
    }

    @After
    public void restoreEnvFile() throws IOException {
        if (originalEnvContent != null) {
            Files.write(Paths.get(ENV_FILE), originalEnvContent);
        } else {
            Files.deleteIfExists(Paths.get(ENV_FILE));
        }
    }

    // ── getVaultURL ───────────────────────────────────────────────────────────

    @Test
    public void testGetVaultURL_prodEnv() {
        String url = Utils.getVaultURL("cluster1", Env.PROD);
        Assert.assertEquals("https://cluster1.skyvault.skyflowapis.com", url);
    }

    @Test
    public void testGetVaultURL_devEnv() {
        String url = Utils.getVaultURL("cluster1", Env.DEV);
        Assert.assertEquals("https://cluster1.skyvault.skyflowapis.dev", url);
    }

    @Test
    public void testGetVaultURL_stageEnv() {
        String url = Utils.getVaultURL("cluster1", Env.STAGE);
        Assert.assertEquals("https://cluster1.skyvault.skyflowapis.tech", url);
    }

    @Test
    public void testGetVaultURL_sandboxEnv() {
        String url = Utils.getVaultURL("cluster1", Env.SANDBOX);
        Assert.assertEquals("https://cluster1.skyvault.skyflowapis-preview.com", url);
    }

    // ── getMetrics ────────────────────────────────────────────────────────────

    @Test
    public void testGetMetrics_containsSdkVersion() {
        JsonObject metrics = Utils.getMetrics();
        Assert.assertTrue(metrics.has(BaseConstants.SDK_METRIC_NAME_VERSION));
        String sdkVersionMetric = metrics.get(BaseConstants.SDK_METRIC_NAME_VERSION).getAsString();
        Assert.assertTrue(sdkVersionMetric.startsWith(Constants.SDK_METRIC_NAME_VERSION_PREFIX));
    }

    // ── getEnvVaultURL ────────────────────────────────────────────────────────

    @Test
    public void testGetEnvVaultURL_doesNotThrowUnexpectedException() {
        try {
            Utils.getEnvVaultURL();
        } catch (SkyflowException e) {
            // acceptable if this environment happens to have an invalid VAULT_URL set
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testGetEnvVaultURL_emptyValueThrowsEmptyVaultUrl() throws Exception {
        // No VAULT_URL system/real env var is set in the test environment, so this
        // exercises the Dotenv.load() fallback path with an empty value.
        try (FileWriter fw = new FileWriter(ENV_FILE)) {
            fw.write("VAULT_URL=\n");
        }

        try {
            Utils.getEnvVaultURL();
            Assert.fail("Should have thrown SkyflowException for empty VAULT_URL");
        } catch (SkyflowException e) {
            Assert.assertEquals(com.skyflow.errors.ErrorMessage.EmptyVaultUrl.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testGetEnvVaultURL_invalidFormatThrowsInvalidVaultUrlFormat() throws Exception {
        try (FileWriter fw = new FileWriter(ENV_FILE)) {
            fw.write("VAULT_URL=http://example.com\n");
        }

        try {
            Utils.getEnvVaultURL();
            Assert.fail("Should have thrown SkyflowException for invalid VAULT_URL format");
        } catch (SkyflowException e) {
            Assert.assertEquals(com.skyflow.errors.ErrorMessage.InvalidVaultUrlFormat.getMessage(), e.getMessage());
        }
    }

    // ── isValidURL ────────────────────────────────────────────────────────────

    @Test
    public void testIsValidURL_validHttpsUrl() {
        Assert.assertTrue(Utils.isValidURL("https://example.com"));
    }

    @Test
    public void testIsValidURL_httpUrlIsInvalid() {
        Assert.assertFalse(Utils.isValidURL("http://example.com"));
    }

    @Test
    public void testIsValidURL_malformedUrl() {
        Assert.assertFalse(Utils.isValidURL("not a url"));
    }

    @Test
    public void testIsValidURL_httpsUrlWithEmptyHostIsInvalid() {
        Assert.assertFalse(Utils.isValidURL("https:///path"));
    }

    // ── generateBearerToken ───────────────────────────────────────────────────

    @Test
    public void testGenerateBearerToken_withDirectToken() throws SkyflowException {
        Credentials credentials = new Credentials();
        credentials.setToken("direct-token-value");

        String token = Utils.generateBearerToken(credentials);

        Assert.assertEquals("direct-token-value", token);
    }

    @Test
    public void testGenerateBearerToken_withInvalidCredentialsStringThrows() {
        Credentials credentials = new Credentials();
        // A string that fails JSON syntax parsing (as opposed to e.g. a bare word,
        // which Gson's lenient parser accepts as a JSON primitive rather than rejecting outright).
        credentials.setCredentialsString("./src/test/credentials.json");
        try {
            Utils.generateBearerToken(credentials);
            Assert.fail("Should have thrown an exception");
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testGenerateBearerToken_withNonExistentPathThrows() {
        Credentials credentials = new Credentials();
        credentials.setPath("/nonexistent/path/credentials.json");
        try {
            Utils.generateBearerToken(credentials);
            Assert.fail("Should have thrown an exception");
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    // ── getBulkInsertRequestBody ──────────────────────────────────────────────

    @Test
    public void testGetBulkInsertRequestBody_buildsCorrectRequest() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        InsertRecord record = InsertRecord.builder().data(data).build();
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(record);
        InsertRequest request = InsertRequest.builder().table("table1").records(records).build();
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");

        V1InsertRequest body = Utils.getBulkInsertRequestBody(request, config);

        Assert.assertEquals("vault123", body.getVaultId().get());
        Assert.assertEquals("table1", body.getTableName().get());
        Assert.assertEquals(1, body.getRecords().get().size());
        Assert.assertEquals(data, body.getRecords().get().get(0).getData().get());
    }

    @Test
    public void testGetBulkInsertRequestBody_withUpsertAtRequestLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        InsertRecord record = InsertRecord.builder().data(data).build();
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(record);
        InsertRequest request = InsertRequest.builder()
                .table("table1")
                .records(records)
                .upsert(Collections.singletonList("email"))
                .upsertType(UpsertType.UPDATE)
                .build();
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");

        V1InsertRequest body = Utils.getBulkInsertRequestBody(request, config);

        Assert.assertTrue(body.getUpsert().isPresent());
        Assert.assertEquals(Collections.singletonList("email"), body.getUpsert().get().getUniqueColumns().get());
        Assert.assertEquals(FlowEnumUpdateType.UPDATE, body.getUpsert().get().getUpdateType().get());
    }

    @Test
    public void testGetBulkInsertRequestBody_withUpsertAtRecordLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        InsertRecord record = InsertRecord.builder()
                .table("table1")
                .data(data)
                .upsert(Collections.singletonList("email"))
                .upsertType(UpsertType.REPLACE)
                .build();
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(record);
        InsertRequest request = InsertRequest.builder().records(records).build();
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");

        V1InsertRequest body = Utils.getBulkInsertRequestBody(request, config);

        Assert.assertEquals("table1", body.getRecords().get().get(0).getTableName().get());
        Assert.assertTrue(body.getRecords().get().get(0).getUpsert().isPresent());
        Assert.assertEquals(FlowEnumUpdateType.REPLACE, body.getRecords().get().get(0).getUpsert().get().getUpdateType().get());
    }

    // ── buildInsertResponse ───────────────────────────────────────────────────

    @Test
    public void testBuildInsertResponse_success() {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "tok-abc");
        V1RecordResponseObject record = V1RecordResponseObject.builder()
                .skyflowId("sky-id-1")
                .tokens(tokens)
                .build();
        V1InsertResponse res = V1InsertResponse.builder().records(Collections.singletonList(record)).build();

        InsertResponse response = Utils.buildInsertResponse(res);

        Assert.assertEquals(1, response.getInsertedFields().size());
        Assert.assertEquals("sky-id-1", response.getInsertedFields().get(0).get("skyflowId"));
        Assert.assertEquals("tok-abc", response.getInsertedFields().get(0).get("name"));
        Assert.assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void testBuildInsertResponse_error() {
        V1RecordResponseObject record = V1RecordResponseObject.builder()
                .skyflowId("sky-id-1")
                .tableName("table1")
                .error("insert failed")
                .httpCode(400)
                .build();
        V1InsertResponse res = V1InsertResponse.builder().records(Collections.singletonList(record)).build();

        InsertResponse response = Utils.buildInsertResponse(res);

        Assert.assertTrue(response.getInsertedFields().isEmpty());
        Assert.assertEquals(1, response.getErrors().size());
        Assert.assertEquals("insert failed", response.getErrors().get(0).get("error"));
        Assert.assertEquals("table1", response.getErrors().get(0).get("tableName"));
        Assert.assertEquals(400, response.getErrors().get(0).get("httpCode"));
    }

    @Test
    public void testBuildInsertResponse_nullResponse() {
        InsertResponse response = Utils.buildInsertResponse(null);
        Assert.assertTrue(response.getInsertedFields().isEmpty());
        Assert.assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void testBuildInsertResponse_emptyResponse() {
        V1InsertResponse res = V1InsertResponse.builder().build();
        InsertResponse response = Utils.buildInsertResponse(res);
        Assert.assertTrue(response.getInsertedFields().isEmpty());
        Assert.assertTrue(response.getErrors().isEmpty());
    }

    // ── getDetokenizeRequestBody ──────────────────────────────────────────────

    @Test
    public void testGetDetokenizeRequestBody_buildsCorrectRequest() {
        ArrayList<DetokenizeData> data = new ArrayList<>();
        data.add(new DetokenizeData("token1"));
        DetokenizeRequest request = DetokenizeRequest.builder().detokenizeData(data).build();

        V1FlowDetokenizeRequest body = Utils.getDetokenizeRequestBody(request, "vault123");

        Assert.assertEquals("vault123", body.getVaultId().get());
        Assert.assertEquals(Collections.singletonList("token1"), body.getTokens().get());
        Assert.assertFalse(body.getTokenGroupRedactions().isPresent());
    }

    @Test
    public void testGetDetokenizeRequestBody_withTokenGroupRedactions() {
        ArrayList<DetokenizeData> data = new ArrayList<>();
        data.add(new DetokenizeData("token1"));
        List<TokenGroupRedactions> groupRedactions = Collections.singletonList(
                TokenGroupRedactions.builder().tokenGroupName("group1").redaction("MASKED").build());
        DetokenizeRequest request = DetokenizeRequest.builder()
                .detokenizeData(data)
                .tokenGroupRedactions(groupRedactions)
                .build();

        V1FlowDetokenizeRequest body = Utils.getDetokenizeRequestBody(request, "vault123");

        Assert.assertTrue(body.getTokenGroupRedactions().isPresent());
        Assert.assertEquals("group1", body.getTokenGroupRedactions().get().get(0).getTokenGroupName().get());
        Assert.assertEquals("MASKED", body.getTokenGroupRedactions().get().get(0).getRedaction().get());
    }

    // ── buildDetokenizeResponse ───────────────────────────────────────────────

    @Test
    public void testBuildDetokenizeResponse_success() {
        V1FlowDetokenizeResponseObject record = V1FlowDetokenizeResponseObject.builder()
                .token("token1")
                .value("secret-value")
                .tokenGroupName("group1")
                .build();
        V1FlowDetokenizeResponse res = V1FlowDetokenizeResponse.builder()
                .response(Collections.singletonList(record))
                .build();

        DetokenizeResponse response = Utils.buildDetokenizeResponse(res);

        Assert.assertEquals(1, response.getDetokenizedFields().size());
        DetokenizeRecordResponse detokenizedField = response.getDetokenizedFields().get(0);
        Assert.assertEquals("token1", detokenizedField.getToken());
        Assert.assertEquals("secret-value", detokenizedField.getValue());
        Assert.assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void testBuildDetokenizeResponse_error() {
        V1FlowDetokenizeResponseObject record = V1FlowDetokenizeResponseObject.builder()
                .token("token1")
                .error("detokenize failed")
                .build();
        V1FlowDetokenizeResponse res = V1FlowDetokenizeResponse.builder()
                .response(Collections.singletonList(record))
                .build();

        DetokenizeResponse response = Utils.buildDetokenizeResponse(res);

        Assert.assertTrue(response.getDetokenizedFields().isEmpty());
        Assert.assertEquals(1, response.getErrors().size());
        Assert.assertEquals("detokenize failed", response.getErrors().get(0).getError());
    }

    @Test
    public void testBuildDetokenizeResponse_nullResponse() {
        DetokenizeResponse response = Utils.buildDetokenizeResponse(null);
        Assert.assertTrue(response.getDetokenizedFields().isEmpty());
        Assert.assertTrue(response.getErrors().isEmpty());
    }

    // ── getTokenizeRequestBody ────────────────────────────────────────────────

    @Test
    public void testGetTokenizeRequestBody_buildsCorrectRequest() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(TokenizeRecord.builder()
                .value("value1")
                .tokenGroupNames(Collections.singletonList("group1"))
                .build());
        TokenizeRequest request = TokenizeRequest.builder().data(records).build();

        V1FlowTokenizeRequest body = Utils.getTokenizeRequestBody(request, "vault123");

        Assert.assertEquals("vault123", body.getVaultId().get());
        Assert.assertTrue(body.getData().isPresent());
        Assert.assertEquals(1, body.getData().get().size());
        Assert.assertEquals("value1", body.getData().get().get(0).getValue().get());
        Assert.assertEquals(Collections.singletonList("group1"), body.getData().get().get(0).getTokenGroupNames().get());
    }

    // ── buildTokenizeResponse ─────────────────────────────────────────────────

    @Test
    public void testBuildTokenizeResponse_success() {
        FlowTokenizeResponseObjectToken token = FlowTokenizeResponseObjectToken.builder()
                .tokenGroupName("group1")
                .token("tok-abc")
                .build();
        V1FlowTokenizeResponseObject responseObject = V1FlowTokenizeResponseObject.builder()
                .value("value1")
                .tokens(Collections.singletonList(token))
                .build();
        V1FlowTokenizeResponse res = V1FlowTokenizeResponse.builder()
                .response(Collections.singletonList(responseObject))
                .build();

        TokenizeResponse response = Utils.buildTokenizeResponse(res, new HashMap<>(), 1);

        Assert.assertEquals(1, response.getTokenizedData().size());
        TokenizeData data = response.getTokenizedData().get(0);
        Assert.assertEquals("value1", data.getValue());
        Assert.assertEquals("tok-abc", data.getTokens().get("group1"));
        Assert.assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void testBuildTokenizeResponse_error() {
        FlowTokenizeResponseObjectToken token = FlowTokenizeResponseObjectToken.builder()
                .tokenGroupName("group1")
                .error("tokenization failed")
                .httpCode(400)
                .build();
        V1FlowTokenizeResponseObject responseObject = V1FlowTokenizeResponseObject.builder()
                .value("value1")
                .tokens(Collections.singletonList(token))
                .build();
        V1FlowTokenizeResponse res = V1FlowTokenizeResponse.builder()
                .response(Collections.singletonList(responseObject))
                .build();

        TokenizeResponse response = Utils.buildTokenizeResponse(res, new HashMap<>(), 1);

        Assert.assertTrue(response.getTokenizedData().isEmpty());
        Assert.assertEquals(1, response.getErrors().size());
        Map<String, Object> errorRecord = response.getErrors().get(0);
        Assert.assertEquals("tokenization failed", errorRecord.get("error"));
        Assert.assertEquals(400, errorRecord.get("httpCode"));
        Assert.assertEquals("group1", errorRecord.get("tokenGroupName"));
        Assert.assertEquals(0, errorRecord.get("index"));
    }

    @Test
    public void testBuildTokenizeResponse_mixedSuccessAndError() {
        FlowTokenizeResponseObjectToken successToken = FlowTokenizeResponseObjectToken.builder()
                .tokenGroupName("group1")
                .token("tok-abc")
                .build();
        FlowTokenizeResponseObjectToken errorToken = FlowTokenizeResponseObjectToken.builder()
                .tokenGroupName("group2")
                .error("group2 failed")
                .httpCode(400)
                .build();
        V1FlowTokenizeResponseObject responseObject = V1FlowTokenizeResponseObject.builder()
                .value("value1")
                .tokens(java.util.Arrays.asList(successToken, errorToken))
                .build();
        V1FlowTokenizeResponse res = V1FlowTokenizeResponse.builder()
                .response(Collections.singletonList(responseObject))
                .build();

        TokenizeResponse response = Utils.buildTokenizeResponse(res, new HashMap<>(), 1);

        Assert.assertEquals(1, response.getTokenizedData().size());
        Assert.assertEquals("tok-abc", response.getTokenizedData().get(0).getTokens().get("group1"));
        Assert.assertEquals(1, response.getErrors().size());
        Assert.assertEquals("group2 failed", response.getErrors().get(0).get("error"));
    }

    @Test
    public void testBuildTokenizeResponse_emptyResponse() {
        V1FlowTokenizeResponse res = V1FlowTokenizeResponse.builder().build();

        TokenizeResponse response = Utils.buildTokenizeResponse(res, new HashMap<>(), 1);

        Assert.assertTrue(response.getTokenizedData().isEmpty());
        Assert.assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void testBuildTokenizeResponse_nullResponse() {
        TokenizeResponse response = Utils.buildTokenizeResponse(null, new HashMap<>(), 1);

        Assert.assertTrue(response.getTokenizedData().isEmpty());
        Assert.assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void testBuildTokenizeResponse_incompleteResponseDoesNotThrow() {
        FlowTokenizeResponseObjectToken token = FlowTokenizeResponseObjectToken.builder()
                .tokenGroupName("group1")
                .token("tok-abc")
                .build();
        V1FlowTokenizeResponseObject responseObject = V1FlowTokenizeResponseObject.builder()
                .value("value1")
                .tokens(Collections.singletonList(token))
                .build();
        V1FlowTokenizeResponse res = V1FlowTokenizeResponse.builder()
                .response(Collections.singletonList(responseObject))
                .build();

        // requestedRecordCount=2 but only 1 record came back — should just warn, not throw
        TokenizeResponse response = Utils.buildTokenizeResponse(res, new HashMap<>(), 2);

        Assert.assertEquals(1, response.getTokenizedData().size());
    }

    // ── getDeleteTokensRequestBody ────────────────────────────────────────────

    @Test
    public void testGetDeleteTokensRequestBody_buildsCorrectRequest() {
        DeleteTokensRequest request = DeleteTokensRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .build();

        V1FlowDeleteTokenRequest body = Utils.getDeleteTokensRequestBody(request, "vault123");

        Assert.assertEquals("vault123", body.getVaultId().get());
        Assert.assertEquals(Collections.singletonList("token1"), body.getTokens().get());
    }

    // ── buildDeleteTokensResponse ─────────────────────────────────────────────

    @Test
    public void testBuildDeleteTokensResponse_success() {
        V1DeleteTokenResponseObject record = V1DeleteTokenResponseObject.builder()
                .value("token1")
                .build();
        V1FlowDeleteTokenResponse res = V1FlowDeleteTokenResponse.builder()
                .tokens(Collections.singletonList(record))
                .build();

        DeleteTokensResponse response = Utils.buildDeleteTokensResponse(res, new HashMap<>(), 1);

        Assert.assertEquals(Collections.singletonList("token1"), response.getTokens());
        Assert.assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void testBuildDeleteTokensResponse_error() {
        V1DeleteTokenResponseObject record = V1DeleteTokenResponseObject.builder()
                .error("token not found")
                .httpCode(404)
                .build();
        V1FlowDeleteTokenResponse res = V1FlowDeleteTokenResponse.builder()
                .tokens(Collections.singletonList(record))
                .build();

        DeleteTokensResponse response = Utils.buildDeleteTokensResponse(res, new HashMap<>(), 1);

        Assert.assertTrue(response.getTokens().isEmpty());
        Assert.assertEquals(1, response.getErrors().size());
        Assert.assertEquals("token not found", response.getErrors().get(0).get("error"));
        Assert.assertEquals(404, response.getErrors().get(0).get("httpCode"));
    }

    @Test
    public void testBuildDeleteTokensResponse_emptyResponse() {
        V1FlowDeleteTokenResponse res = V1FlowDeleteTokenResponse.builder().build();

        DeleteTokensResponse response = Utils.buildDeleteTokensResponse(res, new HashMap<>(), 1);

        Assert.assertTrue(response.getTokens().isEmpty());
        Assert.assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void testBuildDeleteTokensResponse_nullResponse() {
        DeleteTokensResponse response = Utils.buildDeleteTokensResponse(null, new HashMap<>(), 1);

        Assert.assertTrue(response.getTokens().isEmpty());
        Assert.assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void testBuildDeleteTokensResponse_incompleteResponseDoesNotThrow() {
        V1DeleteTokenResponseObject record = V1DeleteTokenResponseObject.builder()
                .value("token1")
                .build();
        V1FlowDeleteTokenResponse res = V1FlowDeleteTokenResponse.builder()
                .tokens(Collections.singletonList(record))
                .build();

        // requestedTokenCount=2 but only 1 record came back — should just warn, not throw
        DeleteTokensResponse response = Utils.buildDeleteTokensResponse(res, new HashMap<>(), 2);

        Assert.assertEquals(1, response.getTokens().size());
    }

    // ── getBulkInsertRequestBody (bulk overload) ──────────────────────────────

    @Test
    public void testGetBulkInsertRequestBody_bulk_buildsCorrectRequest() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        BulkInsertRecord record = BulkInsertRecord.builder().data(data).build();
        ArrayList<BulkInsertRecord> records = new ArrayList<>();
        records.add(record);
        BulkInsertRequest request = BulkInsertRequest.builder().table("table1").records(records).build();
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");

        V1InsertRequest body = Utils.getBulkInsertRequestBody(request, config);

        Assert.assertEquals("vault123", body.getVaultId().get());
        Assert.assertEquals("table1", body.getTableName().get());
        Assert.assertEquals(1, body.getRecords().get().size());
        Assert.assertEquals(data, body.getRecords().get().get(0).getData().get());
    }

    @Test
    public void testGetBulkInsertRequestBody_bulk_withUpsertAtRequestLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        BulkInsertRecord record = BulkInsertRecord.builder().data(data).build();
        ArrayList<BulkInsertRecord> records = new ArrayList<>();
        records.add(record);
        BulkInsertRequest request = BulkInsertRequest.builder()
                .table("table1")
                .records(records)
                .upsert(Collections.singletonList("email"))
                .upsertType(UpsertType.UPDATE)
                .build();
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");

        V1InsertRequest body = Utils.getBulkInsertRequestBody(request, config);

        Assert.assertTrue(body.getUpsert().isPresent());
        Assert.assertEquals(Collections.singletonList("email"), body.getUpsert().get().getUniqueColumns().get());
        Assert.assertEquals(FlowEnumUpdateType.UPDATE, body.getUpsert().get().getUpdateType().get());
    }

    @Test
    public void testGetBulkInsertRequestBody_bulk_withUpsertAtRecordLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        BulkInsertRecord record = BulkInsertRecord.builder()
                .table("table1")
                .data(data)
                .upsert(Collections.singletonList("email"))
                .upsertType(UpsertType.REPLACE)
                .build();
        ArrayList<BulkInsertRecord> records = new ArrayList<>();
        records.add(record);
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");

        V1InsertRequest body = Utils.getBulkInsertRequestBody(request, config);

        Assert.assertEquals("table1", body.getRecords().get().get(0).getTableName().get());
        Assert.assertTrue(body.getRecords().get().get(0).getUpsert().isPresent());
        Assert.assertEquals(FlowEnumUpdateType.REPLACE, body.getRecords().get().get(0).getUpsert().get().getUpdateType().get());
    }

    // ── getBulkDetokenizeRequestBody ──────────────────────────────────────────

    @Test
    public void testGetBulkDetokenizeRequestBody_buildsCorrectRequest() {
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder()
                .tokens(Arrays.asList("token1", "token2"))
                .build();

        V1FlowDetokenizeRequest body = Utils.getBulkDetokenizeRequestBody(request, "vault123");

        Assert.assertEquals("vault123", body.getVaultId().get());
        Assert.assertEquals(Arrays.asList("token1", "token2"), body.getTokens().get());
        Assert.assertFalse(body.getTokenGroupRedactions().isPresent());
    }

    @Test
    public void testGetBulkDetokenizeRequestBody_withTokenGroupRedactions() {
        BulkTokenGroupRedactions redaction = BulkTokenGroupRedactions.builder()
                .tokenGroupName("group1")
                .redaction("MASKED")
                .build();
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .tokenGroupRedactions(Collections.singletonList(redaction))
                .build();

        V1FlowDetokenizeRequest body = Utils.getBulkDetokenizeRequestBody(request, "vault123");

        Assert.assertTrue(body.getTokenGroupRedactions().isPresent());
        Assert.assertEquals(1, body.getTokenGroupRedactions().get().size());
        Assert.assertEquals("group1", body.getTokenGroupRedactions().get().get(0).getTokenGroupName().get());
        Assert.assertEquals("MASKED", body.getTokenGroupRedactions().get().get(0).getRedaction().get());
    }

    // ── getBulkDeleteTokensRequestBody ─────────────────────────────────────────

    @Test
    public void testGetBulkDeleteTokensRequestBody_buildsCorrectRequest() {
        BulkDeleteTokensRequest request = BulkDeleteTokensRequest.builder()
                .tokens(Arrays.asList("token1", "token2"))
                .build();

        V1FlowDeleteTokenRequest body = Utils.getBulkDeleteTokensRequestBody(request, "vault123");

        Assert.assertEquals("vault123", body.getVaultId().get());
        Assert.assertEquals(Arrays.asList("token1", "token2"), body.getTokens().get());
    }

    // ── getBulkTokenizeRequestBody ─────────────────────────────────────────────

    @Test
    public void testGetBulkTokenizeRequestBody_buildsCorrectRequest() {
        BulkTokenizeRecord record = BulkTokenizeRecord.builder()
                .value("value1")
                .tokenGroupNames(Collections.singletonList("group1"))
                .build();
        ArrayList<BulkTokenizeRecord> data = new ArrayList<>();
        data.add(record);
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().data(data).build();

        V1FlowTokenizeRequest body = Utils.getBulkTokenizeRequestBody(request, "vault123");

        Assert.assertEquals("vault123", body.getVaultId().get());
        Assert.assertEquals(1, body.getData().get().size());
        Assert.assertEquals("value1", body.getData().get().get(0).getValue().get());
        Assert.assertEquals(Collections.singletonList("group1"), body.getData().get().get(0).getTokenGroupNames().get());
    }

    // ── createBulkInsertBatches ────────────────────────────────────────────────

    @Test
    public void testCreateBulkInsertBatches_splitsEvenly() {
        List<V1InsertRecordData> records = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            records.add(V1InsertRecordData.builder().data(new HashMap<>()).build());
        }

        List<List<V1InsertRecordData>> batches = Utils.createBulkInsertBatches(records, 2);

        Assert.assertEquals(2, batches.size());
        Assert.assertEquals(2, batches.get(0).size());
        Assert.assertEquals(2, batches.get(1).size());
    }

    @Test
    public void testCreateBulkInsertBatches_splitsWithRemainder() {
        List<V1InsertRecordData> records = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            records.add(V1InsertRecordData.builder().data(new HashMap<>()).build());
        }

        List<List<V1InsertRecordData>> batches = Utils.createBulkInsertBatches(records, 2);

        Assert.assertEquals(3, batches.size());
        Assert.assertEquals(1, batches.get(2).size());
    }

    // ── createBulkDetokenizeBatches ────────────────────────────────────────────

    @Test
    public void testCreateBulkDetokenizeBatches_splitsTokens() {
        V1FlowDetokenizeRequest request = V1FlowDetokenizeRequest.builder()
                .vaultId("vault123")
                .tokens(Arrays.asList("t1", "t2", "t3"))
                .build();

        List<V1FlowDetokenizeRequest> batches = Utils.createBulkDetokenizeBatches(request, 2);

        Assert.assertEquals(2, batches.size());
        Assert.assertEquals(Arrays.asList("t1", "t2"), batches.get(0).getTokens().get());
        Assert.assertEquals(Collections.singletonList("t3"), batches.get(1).getTokens().get());
        Assert.assertEquals("vault123", batches.get(0).getVaultId().get());
    }

    @Test
    public void testCreateBulkDetokenizeBatches_carriesTokenGroupRedactions() {
        com.skyflow.generated.rest.types.V1TokenGroupRedactions redaction =
                com.skyflow.generated.rest.types.V1TokenGroupRedactions.builder()
                        .tokenGroupName("group1")
                        .redaction("MASKED")
                        .build();
        V1FlowDetokenizeRequest request = V1FlowDetokenizeRequest.builder()
                .vaultId("vault123")
                .tokens(Arrays.asList("t1", "t2"))
                .tokenGroupRedactions(Collections.singletonList(redaction))
                .build();

        List<V1FlowDetokenizeRequest> batches = Utils.createBulkDetokenizeBatches(request, 5);

        Assert.assertEquals(1, batches.size());
        Assert.assertTrue(batches.get(0).getTokenGroupRedactions().isPresent());
        Assert.assertEquals("group1", batches.get(0).getTokenGroupRedactions().get().get(0).getTokenGroupName().get());
    }

    // ── createBulkDeleteTokensBatches ──────────────────────────────────────────

    @Test
    public void testCreateBulkDeleteTokensBatches_splitsTokens() {
        V1FlowDeleteTokenRequest request = V1FlowDeleteTokenRequest.builder()
                .vaultId("vault123")
                .tokens(Arrays.asList("t1", "t2", "t3"))
                .build();

        List<V1FlowDeleteTokenRequest> batches = Utils.createBulkDeleteTokensBatches(request, 2);

        Assert.assertEquals(2, batches.size());
        Assert.assertEquals(Arrays.asList("t1", "t2"), batches.get(0).getTokens().get());
        Assert.assertEquals(Collections.singletonList("t3"), batches.get(1).getTokens().get());
    }

    // ── createBulkTokenizeBatches ──────────────────────────────────────────────

    @Test
    public void testCreateBulkTokenizeBatches_splitsData() {
        com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject obj1 =
                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("v1").build();
        com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject obj2 =
                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("v2").build();
        V1FlowTokenizeRequest request = V1FlowTokenizeRequest.builder()
                .vaultId("vault123")
                .data(Arrays.asList(obj1, obj2))
                .build();

        List<V1FlowTokenizeRequest> batches = Utils.createBulkTokenizeBatches(request, 1);

        Assert.assertEquals(2, batches.size());
        Assert.assertEquals(1, batches.get(0).getData().get().size());
        Assert.assertEquals("v1", batches.get(0).getData().get().get(0).getValue().get());
        Assert.assertEquals("v2", batches.get(1).getData().get().get(0).getValue().get());
    }

    // ── createErrorRecord ──────────────────────────────────────────────────────

    @Test
    public void testCreateErrorRecord_withHttpCodeKey() {
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("http_code", 400);
        recordMap.put("error", "bad request");

        ErrorRecord err = Utils.createErrorRecord(recordMap, 0, "req-1");

        Assert.assertEquals(400, err.getCode());
        Assert.assertEquals("bad request", err.getError());
        Assert.assertEquals("req-1", err.getRequestId());
    }

    @Test
    public void testCreateErrorRecord_withStatusCodeKey() {
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("statusCode", 500);
        recordMap.put("message", "server error");

        ErrorRecord err = Utils.createErrorRecord(recordMap, 2, null);

        Assert.assertEquals(500, err.getCode());
        Assert.assertEquals("server error", err.getError());
        Assert.assertEquals(2, err.getIndex());
    }

    @Test
    public void testCreateErrorRecord_nullMapReturnsNull() {
        Assert.assertNull(Utils.createErrorRecord(null, 0, null));
    }

    @Test
    public void testCreateErrorRecord_noCodeKeyDefaultsTo500() {
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("error", "something went wrong");

        ErrorRecord err = Utils.createErrorRecord(recordMap, 1, "req-2");

        Assert.assertEquals(500, err.getCode());
        Assert.assertEquals("something went wrong", err.getError());
    }

    @Test
    public void testCreateErrorRecord_noErrorOrMessageKeyDefaultsToUnknownError() {
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("http_code", 403);

        ErrorRecord err = Utils.createErrorRecord(recordMap, 3, null);

        Assert.assertEquals(403, err.getCode());
        Assert.assertEquals("Unknown error", err.getError());
    }

    // ── handleBulkInsertBatchException ────────────────────────────────────────

    @Test
    public void testHandleBulkInsertBatchException_apiExceptionWithRecordsBody() {
        Map<String, Object> errorRecordMap = new HashMap<>();
        errorRecordMap.put("error", "duplicate");
        errorRecordMap.put("httpCode", 409);
        Map<String, Object> body = new HashMap<>();
        body.put("records", Collections.singletonList(errorRecordMap));
        ApiClientApiException apiEx = new ApiClientApiException("insert failed", 409, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<V1InsertRecordData> batch = Collections.singletonList(V1InsertRecordData.builder().data(new HashMap<>()).build());
        List<ErrorRecord> errors = Utils.handleBulkInsertBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(409, errors.get(0).getCode());
        Assert.assertEquals("duplicate", errors.get(0).getError());
    }

    @Test
    public void testHandleBulkInsertBatchException_apiExceptionWithNoParsableBody() {
        ApiClientApiException apiEx = new ApiClientApiException("insert failed", 401, "unauthorized");
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<V1InsertRecordData> batch = Arrays.asList(
                V1InsertRecordData.builder().data(new HashMap<>()).build(),
                V1InsertRecordData.builder().data(new HashMap<>()).build());
        List<ErrorRecord> errors = Utils.handleBulkInsertBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals(401, errors.get(0).getCode());
        Assert.assertEquals("insert failed", errors.get(0).getError());
    }

    @Test
    public void testHandleBulkInsertBatchException_apiExceptionWithErrorKeyBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "top level auth error");
        ApiClientApiException apiEx = new ApiClientApiException("insert failed", 401, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<V1InsertRecordData> batch = Collections.singletonList(V1InsertRecordData.builder().data(new HashMap<>()).build());
        List<ErrorRecord> errors = Utils.handleBulkInsertBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(401, errors.get(0).getCode());
        Assert.assertEquals("top level auth error", errors.get(0).getError());
    }

    @Test
    public void testHandleBulkInsertBatchException_genericException() {
        RuntimeException ex = new RuntimeException("boom");
        List<V1InsertRecordData> batch = Collections.singletonList(V1InsertRecordData.builder().data(new HashMap<>()).build());

        List<ErrorRecord> errors = Utils.handleBulkInsertBatchException(ex, batch, 1, 2);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(500, errors.get(0).getCode());
        Assert.assertEquals("boom", errors.get(0).getError());
        Assert.assertEquals(2, errors.get(0).getIndex());
    }

    // ── handleBulkDetokenizeBatchException ────────────────────────────────────

    @Test
    public void testHandleBulkDetokenizeBatchException_apiExceptionWithResponseBody() {
        Map<String, Object> errorRecordMap = new HashMap<>();
        errorRecordMap.put("error", "token not found");
        errorRecordMap.put("httpCode", 404);
        Map<String, Object> body = new HashMap<>();
        body.put("response", Collections.singletonList(errorRecordMap));
        ApiClientApiException apiEx = new ApiClientApiException("detokenize failed", 404, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        V1FlowDetokenizeRequest batch = V1FlowDetokenizeRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("t1"))
                .build();
        List<ErrorRecord> errors = Utils.handleBulkDetokenizeBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(404, errors.get(0).getCode());
        Assert.assertEquals("token not found", errors.get(0).getError());
    }

    @Test
    public void testHandleBulkDetokenizeBatchException_genericException() {
        RuntimeException ex = new RuntimeException("boom");
        V1FlowDetokenizeRequest batch = V1FlowDetokenizeRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("t1"))
                .build();

        List<ErrorRecord> errors = Utils.handleBulkDetokenizeBatchException(ex, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(500, errors.get(0).getCode());
    }

    // ── handleBulkDeleteTokensBatchException ──────────────────────────────────

    @Test
    public void testHandleBulkDeleteTokensBatchException_apiExceptionWithTokensBody() {
        Map<String, Object> errorRecordMap = new HashMap<>();
        errorRecordMap.put("error", "not found");
        errorRecordMap.put("httpCode", 404);
        Map<String, Object> body = new HashMap<>();
        body.put("tokens", Collections.singletonList(errorRecordMap));
        ApiClientApiException apiEx = new ApiClientApiException("delete failed", 404, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        V1FlowDeleteTokenRequest batch = V1FlowDeleteTokenRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("t1"))
                .build();
        List<ErrorRecord> errors = Utils.handleBulkDeleteTokensBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(404, errors.get(0).getCode());
        Assert.assertEquals("not found", errors.get(0).getError());
    }

    @Test
    public void testHandleBulkDeleteTokensBatchException_apiExceptionWithErrorKeyBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "top level delete error");
        ApiClientApiException apiEx = new ApiClientApiException("delete failed", 403, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        V1FlowDeleteTokenRequest batch = V1FlowDeleteTokenRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("t1"))
                .build();
        List<ErrorRecord> errors = Utils.handleBulkDeleteTokensBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(403, errors.get(0).getCode());
        Assert.assertEquals("top level delete error", errors.get(0).getError());
    }

    @Test
    public void testHandleBulkDeleteTokensBatchException_genericException() {
        RuntimeException ex = new RuntimeException("boom");
        V1FlowDeleteTokenRequest batch = V1FlowDeleteTokenRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("t1"))
                .build();

        List<ErrorRecord> errors = Utils.handleBulkDeleteTokensBatchException(ex, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(500, errors.get(0).getCode());
    }

    // ── handleBulkTokenizeBatchException ───────────────────────────────────────

    @Test
    public void testHandleBulkTokenizeBatchException_apiExceptionWithResponseBody() {
        Map<String, Object> errorRecordMap = new HashMap<>();
        errorRecordMap.put("error", "invalid value");
        errorRecordMap.put("httpCode", 400);
        Map<String, Object> body = new HashMap<>();
        body.put("response", Collections.singletonList(errorRecordMap));
        ApiClientApiException apiEx = new ApiClientApiException("tokenize failed", 400, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        V1FlowTokenizeRequest batch = V1FlowTokenizeRequest.builder()
                .vaultId("vault123")
                .data(Collections.singletonList(com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("v1").build()))
                .build();
        List<ErrorRecord> errors = Utils.handleBulkTokenizeBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(400, errors.get(0).getCode());
        Assert.assertEquals("invalid value", errors.get(0).getError());
    }

    @Test
    public void testHandleBulkTokenizeBatchException_genericException() {
        RuntimeException ex = new RuntimeException("boom");
        V1FlowTokenizeRequest batch = V1FlowTokenizeRequest.builder()
                .vaultId("vault123")
                .data(Collections.singletonList(com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("v1").build()))
                .build();

        List<ErrorRecord> errors = Utils.handleBulkTokenizeBatchException(ex, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(500, errors.get(0).getCode());
    }

    // ── formatBulkInsertResponse ───────────────────────────────────────────────

    @Test
    public void testFormatBulkInsertResponse_success() {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "tok-abc");
        V1RecordResponseObject record = V1RecordResponseObject.builder()
                .skyflowId("sky-id-1")
                .tokens(tokens)
                .build();
        V1InsertResponse response = V1InsertResponse.builder().records(Collections.singletonList(record)).build();

        BulkInsertResponse result = Utils.formatBulkInsertResponse(response, 0, 50, new HashMap<>());

        Assert.assertEquals(1, result.getSuccess().size());
        Assert.assertEquals("sky-id-1", result.getSuccess().get(0).getSkyflowId());
        Assert.assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testFormatBulkInsertResponse_errorWithMissingHttpCodeDefaultsTo500() {
        V1RecordResponseObject record = V1RecordResponseObject.builder()
                .error("insert failed")
                .build();
        V1InsertResponse response = V1InsertResponse.builder().records(Collections.singletonList(record)).build();

        BulkInsertResponse result = Utils.formatBulkInsertResponse(response, 0, 50, new HashMap<>());

        Assert.assertEquals(1, result.getErrors().size());
        Assert.assertEquals(500, result.getErrors().get(0).getCode());
        Assert.assertEquals("insert failed", result.getErrors().get(0).getError());
    }

    @Test
    public void testFormatBulkInsertResponse_nullResponseReturnsNull() {
        Assert.assertNull(Utils.formatBulkInsertResponse(null, 0, 50, new HashMap<>()));
    }

    // ── formatBulkDetokenizeResponse ───────────────────────────────────────────

    @Test
    public void testFormatBulkDetokenizeResponse_success() {
        V1FlowDetokenizeResponseObject record = V1FlowDetokenizeResponseObject.builder()
                .token("token1")
                .value("secret-value")
                .build();
        V1FlowDetokenizeResponse response = V1FlowDetokenizeResponse.builder()
                .response(Collections.singletonList(record))
                .build();

        BulkDetokenizeResponse result = Utils.formatBulkDetokenizeResponse(response, 0, 50, new HashMap<>());

        Assert.assertEquals(1, result.getSuccess().size());
        Assert.assertEquals("secret-value", result.getSuccess().get(0).getValue());
        Assert.assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testFormatBulkDetokenizeResponse_errorWithMissingHttpCodeDefaultsTo500() {
        V1FlowDetokenizeResponseObject record = V1FlowDetokenizeResponseObject.builder()
                .token("token1")
                .error("token not found")
                .build();
        V1FlowDetokenizeResponse response = V1FlowDetokenizeResponse.builder()
                .response(Collections.singletonList(record))
                .build();

        BulkDetokenizeResponse result = Utils.formatBulkDetokenizeResponse(response, 0, 50, new HashMap<>());

        Assert.assertEquals(1, result.getErrors().size());
        Assert.assertEquals(500, result.getErrors().get(0).getCode());
    }

    @Test
    public void testFormatBulkDetokenizeResponse_emptyResponseReturnsNull() {
        V1FlowDetokenizeResponse response = V1FlowDetokenizeResponse.builder().build();
        Assert.assertNull(Utils.formatBulkDetokenizeResponse(response, 0, 50, new HashMap<>()));
    }

    // ── formatBulkDeleteTokensResponse ─────────────────────────────────────────

    @Test
    public void testFormatBulkDeleteTokensResponse_success() {
        V1DeleteTokenResponseObject record = V1DeleteTokenResponseObject.builder()
                .value("token1")
                .build();
        V1FlowDeleteTokenResponse response = V1FlowDeleteTokenResponse.builder()
                .tokens(Collections.singletonList(record))
                .build();

        BulkDeleteTokensResponse result = Utils.formatBulkDeleteTokensResponse(response, 0, 50, new HashMap<>());

        Assert.assertEquals(1, result.getSuccess().size());
        Assert.assertEquals("token1", result.getSuccess().get(0).getToken());
        Assert.assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testFormatBulkDeleteTokensResponse_error() {
        V1DeleteTokenResponseObject record = V1DeleteTokenResponseObject.builder()
                .error("token not found")
                .httpCode(404)
                .build();
        V1FlowDeleteTokenResponse response = V1FlowDeleteTokenResponse.builder()
                .tokens(Collections.singletonList(record))
                .build();

        BulkDeleteTokensResponse result = Utils.formatBulkDeleteTokensResponse(response, 0, 50, new HashMap<>());

        Assert.assertEquals(1, result.getErrors().size());
        Assert.assertEquals(404, result.getErrors().get(0).getCode());
    }

    @Test
    public void testFormatBulkDeleteTokensResponse_errorTextWithoutHttpCodeTreatedAsSuccess() {
        V1DeleteTokenResponseObject record = V1DeleteTokenResponseObject.builder()
                .value("token1")
                .error("transient warning")
                .build();
        V1FlowDeleteTokenResponse response = V1FlowDeleteTokenResponse.builder()
                .tokens(Collections.singletonList(record))
                .build();

        BulkDeleteTokensResponse result = Utils.formatBulkDeleteTokensResponse(response, 0, 50, new HashMap<>());

        Assert.assertTrue(result.getErrors().isEmpty());
        Assert.assertEquals(1, result.getSuccess().size());
        Assert.assertEquals("token1", result.getSuccess().get(0).getToken());
    }

    @Test
    public void testFormatBulkDeleteTokensResponse_emptyResponseReturnsNull() {
        V1FlowDeleteTokenResponse response = V1FlowDeleteTokenResponse.builder().build();
        Assert.assertNull(Utils.formatBulkDeleteTokensResponse(response, 0, 50, new HashMap<>()));
    }

    // ── formatBulkTokenizeResponse ─────────────────────────────────────────────

    @Test
    public void testFormatBulkTokenizeResponse_success() {
        FlowTokenizeResponseObjectToken token = FlowTokenizeResponseObjectToken.builder()
                .tokenGroupName("group1")
                .token("tok-abc")
                .build();
        V1FlowTokenizeResponseObject responseObject = V1FlowTokenizeResponseObject.builder()
                .value("value1")
                .tokens(Collections.singletonList(token))
                .build();
        V1FlowTokenizeResponse response = V1FlowTokenizeResponse.builder()
                .response(Collections.singletonList(responseObject))
                .build();
        V1FlowTokenizeRequest batchRequest = V1FlowTokenizeRequest.builder()
                .vaultId("vault123")
                .data(Collections.singletonList(com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("value1").build()))
                .build();

        BulkTokenizeResponse result = Utils.formatBulkTokenizeResponse(response, batchRequest, 0, 50, new HashMap<>());

        Assert.assertEquals(1, result.getSuccess().size());
        Assert.assertEquals("tok-abc", result.getSuccess().get(0).getTokens().get("group1"));
        Assert.assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testFormatBulkTokenizeResponse_tokenError() {
        FlowTokenizeResponseObjectToken token = FlowTokenizeResponseObjectToken.builder()
                .tokenGroupName("group1")
                .error("invalid value")
                .httpCode(400)
                .build();
        V1FlowTokenizeResponseObject responseObject = V1FlowTokenizeResponseObject.builder()
                .value("value1")
                .tokens(Collections.singletonList(token))
                .build();
        V1FlowTokenizeResponse response = V1FlowTokenizeResponse.builder()
                .response(Collections.singletonList(responseObject))
                .build();
        V1FlowTokenizeRequest batchRequest = V1FlowTokenizeRequest.builder()
                .vaultId("vault123")
                .data(Collections.singletonList(com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("value1").build()))
                .build();

        BulkTokenizeResponse result = Utils.formatBulkTokenizeResponse(response, batchRequest, 0, 50, new HashMap<>());

        Assert.assertTrue(result.getSuccess().isEmpty());
        Assert.assertEquals(1, result.getErrors().size());
        Assert.assertEquals(400, result.getErrors().get(0).getCode());
    }

    @Test
    public void testFormatBulkTokenizeResponse_emptyResponseReturnsNull() {
        V1FlowTokenizeResponse response = V1FlowTokenizeResponse.builder().build();
        V1FlowTokenizeRequest batchRequest = V1FlowTokenizeRequest.builder().vaultId("vault123").build();
        Assert.assertNull(Utils.formatBulkTokenizeResponse(response, batchRequest, 0, 50, new HashMap<>()));
    }

    // ── getQueryRequestBody ────────────────────────────────────────────────────

    @Test
    public void testGetQueryRequestBody_buildsCorrectRequest() {
        com.skyflow.vault.data.QueryRequest request = com.skyflow.vault.data.QueryRequest.builder()
                .query("SELECT * FROM table1")
                .build();

        com.skyflow.generated.rest.resources.records.requests.V1ExecuteQueryRequest body =
                Utils.getQueryRequestBody(request, "vault123");

        Assert.assertEquals("vault123", body.getVaultId().get());
        Assert.assertEquals("SELECT * FROM table1", body.getQuery().get());
    }

    // ── buildQueryResponse ─────────────────────────────────────────────────────

    @Test
    public void testBuildQueryResponse_success() {
        Map<String, Object> data = new HashMap<>();
        data.put("skyflowId", "sky-id-1");
        data.put("name", "john");
        com.skyflow.generated.rest.types.V1ExecuteQueryRecordResponse record =
                com.skyflow.generated.rest.types.V1ExecuteQueryRecordResponse.builder().data(data).build();
        com.skyflow.generated.rest.types.V1ExecuteQueryResponse response =
                com.skyflow.generated.rest.types.V1ExecuteQueryResponse.builder()
                        .records(Collections.singletonList(record))
                        .build();

        com.skyflow.vault.data.QueryResponse result = Utils.buildQueryResponse(response);

        Assert.assertEquals(1, result.getFields().size());
        Assert.assertEquals("sky-id-1", result.getFields().get(0).get("skyflowId"));
        Assert.assertEquals("john", result.getFields().get(0).get("name"));
        Assert.assertNull(result.getErrors());
    }

    @Test
    public void testBuildQueryResponse_emptyResponse() {
        com.skyflow.generated.rest.types.V1ExecuteQueryResponse response =
                com.skyflow.generated.rest.types.V1ExecuteQueryResponse.builder().build();

        com.skyflow.vault.data.QueryResponse result = Utils.buildQueryResponse(response);

        Assert.assertTrue(result.getFields().isEmpty());
    }

    @Test
    public void testBuildQueryResponse_nullResponse() {
        com.skyflow.vault.data.QueryResponse result = Utils.buildQueryResponse(null);

        Assert.assertTrue(result.getFields().isEmpty());
    }

    // ── getGetRequestBody ──────────────────────────────────────────────────────

    @Test
    public void testGetGetRequestBody_withIds() {
        ArrayList<String> ids = new ArrayList<>();
        ids.add("id1");
        com.skyflow.vault.data.GetRequest request = com.skyflow.vault.data.GetRequest.builder()
                .table("table1")
                .ids(ids)
                .build();

        com.skyflow.generated.rest.resources.flowservice.requests.V1GetRequest body =
                Utils.getGetRequestBody(request, "vault123");

        Assert.assertEquals("vault123", body.getVaultId().get());
        Assert.assertEquals("table1", body.getTableName().get());
        Assert.assertEquals(ids, body.getSkyflowIDs().get());
        Assert.assertFalse(body.getUniqueValues().isPresent());
        Assert.assertFalse(body.getColumnRedactions().isPresent());
    }

    @Test
    public void testGetGetRequestBody_withUniqueValuesAndColumnRedactions() {
        Map<String, Object> uniqueValue = new HashMap<>();
        uniqueValue.put("email", "john@example.com");
        List<Map<String, Object>> uniqueValues = Collections.singletonList(uniqueValue);

        com.skyflow.vault.data.ColumnRedaction redaction = com.skyflow.vault.data.ColumnRedaction.builder()
                .columnName("email")
                .redaction("MASKED")
                .build();

        com.skyflow.vault.data.GetRequest request = com.skyflow.vault.data.GetRequest.builder()
                .table("table1")
                .uniqueValues(uniqueValues)
                .columnRedactions(Collections.singletonList(redaction))
                .fields(new ArrayList<>(Collections.singletonList("name")))
                .limit(10)
                .offset(0)
                .build();

        com.skyflow.generated.rest.resources.flowservice.requests.V1GetRequest body =
                Utils.getGetRequestBody(request, "vault123");

        Assert.assertTrue(body.getUniqueValues().isPresent());
        Assert.assertEquals(uniqueValue, body.getUniqueValues().get().get(0).getData().get());
        Assert.assertTrue(body.getColumnRedactions().isPresent());
        Assert.assertEquals("email", body.getColumnRedactions().get().get(0).getColumnName().get());
        Assert.assertEquals("MASKED", body.getColumnRedactions().get().get(0).getRedaction().get());
        Assert.assertEquals(Collections.singletonList("name"), body.getColumns().get());
        Assert.assertEquals(Integer.valueOf(10), body.getLimit().get());
        Assert.assertEquals(Integer.valueOf(0), body.getOffset().get());
    }

    // ── buildGetResponse ───────────────────────────────────────────────────────

    @Test
    public void testBuildGetResponse_success() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        V1RecordResponseObject record = V1RecordResponseObject.builder()
                .skyflowId("sky-id-1")
                .data(data)
                .build();
        com.skyflow.generated.rest.types.V1GetResponse response =
                com.skyflow.generated.rest.types.V1GetResponse.builder()
                        .records(Collections.singletonList(record))
                        .build();

        com.skyflow.vault.data.GetResponse result = Utils.buildGetResponse(response);

        Assert.assertEquals(1, result.getData().size());
        Assert.assertEquals("sky-id-1", result.getData().get(0).get("skyflowId"));
        Assert.assertEquals("john", result.getData().get(0).get("name"));
        Assert.assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testBuildGetResponse_error() {
        V1RecordResponseObject record = V1RecordResponseObject.builder()
                .skyflowId("sky-id-1")
                .error("not found")
                .httpCode(404)
                .build();
        com.skyflow.generated.rest.types.V1GetResponse response =
                com.skyflow.generated.rest.types.V1GetResponse.builder()
                        .records(Collections.singletonList(record))
                        .build();

        com.skyflow.vault.data.GetResponse result = Utils.buildGetResponse(response);

        Assert.assertTrue(result.getData().isEmpty());
        Assert.assertEquals(1, result.getErrors().size());
        Assert.assertEquals("not found", result.getErrors().get(0).get("error"));
        Assert.assertEquals(404, result.getErrors().get(0).get("httpCode"));
    }

    @Test
    public void testBuildGetResponse_nullResponse() {
        com.skyflow.vault.data.GetResponse result = Utils.buildGetResponse(null);

        Assert.assertTrue(result.getData().isEmpty());
        Assert.assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testBuildGetResponse_successIncludesTableName() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        V1RecordResponseObject record = V1RecordResponseObject.builder()
                .skyflowId("sky-id-1")
                .tableName("table1")
                .data(data)
                .build();
        com.skyflow.generated.rest.types.V1GetResponse response =
                com.skyflow.generated.rest.types.V1GetResponse.builder()
                        .records(Collections.singletonList(record))
                        .build();

        com.skyflow.vault.data.GetResponse result = Utils.buildGetResponse(response);

        Assert.assertEquals("table1", result.getData().get(0).get("tableName"));
    }

    // ── getGetRequestBody (multi-table records) ───────────────────────────────

    @Test
    public void testGetGetRequestBody_withRecords_buildsMultiTableRequest() {
        com.skyflow.vault.data.GetRecordRequest record1 = com.skyflow.vault.data.GetRecordRequest.builder()
                .table("table1")
                .ids(new ArrayList<>(Collections.singletonList("id1")))
                .build();
        Map<String, Object> uniqueValue = new HashMap<>();
        uniqueValue.put("email", "john@example.com");
        com.skyflow.vault.data.ColumnRedaction redaction = com.skyflow.vault.data.ColumnRedaction.builder()
                .columnName("email")
                .redaction("MASKED")
                .build();
        com.skyflow.vault.data.GetRecordRequest record2 = com.skyflow.vault.data.GetRecordRequest.builder()
                .table("table2")
                .uniqueValues(Collections.singletonList(uniqueValue))
                .columnRedactions(Collections.singletonList(redaction))
                .build();

        com.skyflow.vault.data.GetRequest request = com.skyflow.vault.data.GetRequest.builder()
                .records(java.util.Arrays.asList(record1, record2))
                .build();

        com.skyflow.generated.rest.resources.flowservice.requests.V1GetRequest body =
                Utils.getGetRequestBody(request, "vault123");

        Assert.assertEquals("vault123", body.getVaultId().get());
        Assert.assertFalse(body.getTableName().isPresent());
        Assert.assertTrue(body.getRecords().isPresent());
        Assert.assertEquals(2, body.getRecords().get().size());

        com.skyflow.generated.rest.types.V1GetRequestData data1 = body.getRecords().get().get(0);
        Assert.assertEquals("table1", data1.getTableName().get());
        Assert.assertEquals(Collections.singletonList("id1"), data1.getSkyflowIDs().get());

        com.skyflow.generated.rest.types.V1GetRequestData data2 = body.getRecords().get().get(1);
        Assert.assertEquals("table2", data2.getTableName().get());
        Assert.assertEquals(uniqueValue, data2.getUniqueValues().get().get(0).getData().get());
        Assert.assertEquals("email", data2.getColumnRedactions().get().get(0).getColumnName().get());
        Assert.assertEquals("MASKED", data2.getColumnRedactions().get().get(0).getRedaction().get());
    }

    @Test
    public void testGetGetRequestBody_withRecords_topLevelLimitOffsetStillApply() {
        com.skyflow.vault.data.GetRecordRequest record = com.skyflow.vault.data.GetRecordRequest.builder()
                .table("table1")
                .ids(new ArrayList<>(Collections.singletonList("id1")))
                .build();
        com.skyflow.vault.data.GetRequest request = com.skyflow.vault.data.GetRequest.builder()
                .records(Collections.singletonList(record))
                .limit(5)
                .offset(1)
                .build();

        com.skyflow.generated.rest.resources.flowservice.requests.V1GetRequest body =
                Utils.getGetRequestBody(request, "vault123");

        Assert.assertEquals(Integer.valueOf(5), body.getLimit().get());
        Assert.assertEquals(Integer.valueOf(1), body.getOffset().get());
    }
}
