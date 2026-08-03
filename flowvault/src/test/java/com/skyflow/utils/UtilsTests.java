package com.skyflow.utils;

import com.google.gson.JsonObject;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
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
import com.skyflow.vault.data.BulkDeleteTokensResponseRecord;
import com.skyflow.vault.data.BulkDetokenizeRequest;
import com.skyflow.vault.data.BulkDetokenizeResponse;
import com.skyflow.vault.data.BulkDetokenizeResponseRecord;
import com.skyflow.vault.data.BulkInsertRequestRecord;
import com.skyflow.vault.data.BulkInsertRequest;
import com.skyflow.vault.data.BulkInsertResponse;
import com.skyflow.vault.data.BulkTokenizeRequestRecord;
import com.skyflow.vault.data.BulkInsertResponseRecord;
import com.skyflow.vault.data.BulkTokenizeRequest;
import com.skyflow.vault.data.BulkTokenizeResponse;
import com.skyflow.vault.data.ErrorRecord;
import com.skyflow.vault.data.InsertRequestRecord;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.TokenGroupRedactions;
import com.skyflow.vault.data.BulkTokenizeResponseRecord;
import com.skyflow.vault.data.TokenizeResponseRecord;
import com.skyflow.vault.data.TokenizeRequestRecord;
import com.skyflow.vault.data.TokenizeRequest;
import com.skyflow.vault.data.TokenizeResponse;
import com.skyflow.vault.data.UpsertOptions;
import org.junit.After;
import com.skyflow.vault.data.TokenizeResponseToken;
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

    // ── getVaultUrl ───────────────────────────────────────────────────────────

    @Test
    public void testGetVaultURL_prodEnv() {
        String url = Utils.getVaultUrl("cluster1", Env.PROD);
        Assert.assertEquals("https://cluster1.skyvault.skyflowapis.com", url);
    }

    @Test
    public void testGetVaultURL_devEnv() {
        String url = Utils.getVaultUrl("cluster1", Env.DEV);
        Assert.assertEquals("https://cluster1.skyvault.skyflowapis.dev", url);
    }

    @Test
    public void testGetVaultURL_stageEnv() {
        String url = Utils.getVaultUrl("cluster1", Env.STAGE);
        Assert.assertEquals("https://cluster1.skyvault.skyflowapis.tech", url);
    }

    @Test
    public void testGetVaultURL_sandboxEnv() {
        String url = Utils.getVaultUrl("cluster1", Env.SANDBOX);
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

    // ── getEnvVaultUrl ────────────────────────────────────────────────────────

    @Test
    public void testGetEnvVaultURL_doesNotThrowUnexpectedException() {
        try {
            Utils.getEnvVaultUrl();
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
            Utils.getEnvVaultUrl();
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
            Utils.getEnvVaultUrl();
            Assert.fail("Should have thrown SkyflowException for invalid VAULT_URL format");
        } catch (SkyflowException e) {
            Assert.assertEquals(com.skyflow.errors.ErrorMessage.InvalidVaultUrlFormat.getMessage(), e.getMessage());
        }
    }

    // ── isValidUrl ────────────────────────────────────────────────────────────

    @Test
    public void testIsValidURL_validHttpsUrl() {
        Assert.assertTrue(Utils.isValidUrl("https://example.com"));
    }

    @Test
    public void testIsValidURL_httpUrlIsInvalid() {
        Assert.assertFalse(Utils.isValidUrl("http://example.com"));
    }

    @Test
    public void testIsValidURL_malformedUrl() {
        Assert.assertFalse(Utils.isValidUrl("not a url"));
    }

    @Test
    public void testIsValidURL_httpsUrlWithEmptyHostIsInvalid() {
        Assert.assertFalse(Utils.isValidUrl("https:///path"));
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

    // ── getInsertRequestBody (unary) ──────────────────────────────────────────

    @Test
    public void testGetInsertRequestBody_buildsCorrectRequest() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        InsertRequestRecord record = InsertRequestRecord.builder().tableName("table1").data(data).build();
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(record);
        InsertRequest request = InsertRequest.builder().records(records).build();
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");

        V1InsertRequest body = Utils.getInsertRequestBody(request, config);

        Assert.assertEquals("vault123", body.getVaultId().get());
        Assert.assertEquals(1, body.getRecords().get().size());
        Assert.assertEquals("table1", body.getRecords().get().get(0).getTableName().get());
        Assert.assertEquals(data, body.getRecords().get().get(0).getData().get());
    }

    @Test
    public void testGetInsertRequestBody_keepsRequestLevelTableNameOnEnvelopeOnly() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        InsertRequestRecord record = InsertRequestRecord.builder().data(data).build();
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(record);
        InsertRequest request = InsertRequest.builder().tableName("table1").records(records).build();
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");

        V1InsertRequest body = Utils.getInsertRequestBody(request, config);

        Assert.assertEquals("table1", body.getTableName().get());
        Assert.assertFalse(body.getRecords().get().get(0).getTableName().isPresent());
    }

    @Test
    public void testGetInsertRequestBody_withUpsertAtRequestLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        InsertRequestRecord record = InsertRequestRecord.builder().tableName("table1").data(data).build();
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(record);
        UpsertOptions upsert = UpsertOptions.builder()
                .uniqueColumns(Collections.singletonList("email"))
                .updateType("UPDATE")
                .build();
        InsertRequest request = InsertRequest.builder()
                .records(records)
                .upsert(upsert)
                .build();
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");

        V1InsertRequest body = Utils.getInsertRequestBody(request, config);

        // Request-level upsert stays on the envelope; it is not copied onto the records.
        Assert.assertFalse(body.getRecords().get().get(0).getUpsert().isPresent());
        Assert.assertTrue(body.getUpsert().isPresent());
        Assert.assertEquals(Collections.singletonList("email"), body.getUpsert().get().getUniqueColumns().get());
        Assert.assertEquals(FlowEnumUpdateType.UPDATE, body.getUpsert().get().getUpdateType().get());
    }

    @Test
    public void testGetInsertRequestBody_withUpsertAtRecordLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        UpsertOptions upsert = UpsertOptions.builder()
                .uniqueColumns(Collections.singletonList("email"))
                .updateType("REPLACE")
                .build();
        InsertRequestRecord record = InsertRequestRecord.builder()
                .tableName("table1")
                .data(data)
                .upsert(upsert)
                .build();
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(record);
        InsertRequest request = InsertRequest.builder().records(records).build();
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");

        V1InsertRequest body = Utils.getInsertRequestBody(request, config);

        Assert.assertEquals("table1", body.getRecords().get().get(0).getTableName().get());
        Assert.assertTrue(body.getRecords().get().get(0).getUpsert().isPresent());
        Assert.assertEquals(FlowEnumUpdateType.REPLACE, body.getRecords().get().get(0).getUpsert().get().getUpdateType().get());
    }

    // Tests for buildInsertResponse / getDetokenizeRequestBody / buildDetokenizeResponse /
    // getTokenizeRequestBody / buildTokenizeResponse / getDeleteTokensRequestBody /
    // buildDeleteTokensResponse were removed: those unary Utils helpers no longer exist (bulk-only module).

    // ── getBulkInsertRequestBody (bulk overload) ──────────────────────────────

    @Test
    public void testGetBulkInsertRequestBody_bulk_buildsCorrectRequest() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        BulkInsertRequestRecord record = BulkInsertRequestRecord.builder().data(data).build();
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(record);
        BulkInsertRequest request = BulkInsertRequest.builder().tableName("table1").records(records).build();
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");

        V1InsertRequest body = Utils.getBulkInsertRequestBody(request, config);

        Assert.assertEquals("vault123", body.getVaultId().get());
        Assert.assertEquals("table1", body.getTableName().get());
        Assert.assertEquals(1, body.getRecords().get().size());
        Assert.assertFalse(body.getRecords().get().get(0).getTableName().isPresent());
        Assert.assertEquals(data, body.getRecords().get().get(0).getData().get());
    }

    @Test
    public void testGetBulkInsertRequestBody_bulk_withUpsertAtRequestLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        BulkInsertRequestRecord record = BulkInsertRequestRecord.builder().data(data).build();
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(record);
        BulkInsertRequest request = BulkInsertRequest.builder()
                .tableName("table1")
                .records(records)
                .upsert(UpsertOptions.builder()
                        .uniqueColumns(Collections.singletonList("email"))
                        .updateType("UPDATE")
                        .build())
                .build();
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");

        V1InsertRequest body = Utils.getBulkInsertRequestBody(request, config);

        // Request-level upsert stays on the envelope; it is not copied onto the records.
        Assert.assertFalse(body.getRecords().get().get(0).getUpsert().isPresent());
        Assert.assertTrue(body.getUpsert().isPresent());
        Assert.assertEquals(Collections.singletonList("email"), body.getUpsert().get().getUniqueColumns().get());
        Assert.assertEquals(FlowEnumUpdateType.UPDATE, body.getUpsert().get().getUpdateType().get());
    }

    @Test
    public void testGetBulkInsertRequestBody_bulk_withUpsertAtRecordLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        BulkInsertRequestRecord record = BulkInsertRequestRecord.builder()
                .tableName("table1")
                .data(data)
                .upsert(UpsertOptions.builder()
                        .uniqueColumns(Collections.singletonList("email"))
                        .updateType("REPLACE")
                        .build())
                .build();
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
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
        TokenGroupRedactions redaction = TokenGroupRedactions.builder()
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
        List<BulkTokenizeRequestRecord> records = Collections.singletonList(
                BulkTokenizeRequestRecord.builder()
                        .value("value1")
                        .tokenGroupNames(Collections.singletonList("group1"))
                        .build());

        V1FlowTokenizeRequest body = Utils.getBulkTokenizeRequestBody(records, "vault123");

        Assert.assertEquals("vault123", body.getVaultId().get());
        Assert.assertEquals(1, body.getData().get().size());
        Assert.assertEquals("value1", body.getData().get().get(0).getValue().get());
        Assert.assertEquals(Collections.singletonList("group1"), body.getData().get().get(0).getTokenGroupNames().get());
        Assert.assertFalse(body.getData().get().get(0).getToken().isPresent());
    }

    @Test
    public void testGetBulkTokenizeRequestBody_carriesByotToken() {
        List<BulkTokenizeRequestRecord> records = Collections.singletonList(
                BulkTokenizeRequestRecord.builder()
                        .value("value1")
                        .token("my-own-token")
                        .tokenGroupNames(Collections.singletonList("group1"))
                        .build());

        V1FlowTokenizeRequest body = Utils.getBulkTokenizeRequestBody(records, "vault123");

        Assert.assertEquals("my-own-token", body.getData().get().get(0).getToken().get());
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
        List<BulkTokenizeRequestRecord> records = Arrays.asList(
                BulkTokenizeRequestRecord.builder().value("v1").build(),
                BulkTokenizeRequestRecord.builder().value("v2").build());

        List<List<BulkTokenizeRequestRecord>> batches = Utils.createBulkTokenizeBatches(records, 1);

        Assert.assertEquals(2, batches.size());
        Assert.assertEquals(1, batches.get(0).size());
        Assert.assertEquals("v1", batches.get(0).get(0).getValue());
        Assert.assertEquals("v2", batches.get(1).get(0).getValue());
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
        List<BulkInsertResponseRecord> errors = Utils.handleBulkInsertBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(409, errors.get(0).getHttpCode());
        Assert.assertEquals("duplicate", errors.get(0).getError());
    }

    @Test
    public void testHandleBulkInsertBatchException_apiExceptionWithNoParsableBody() {
        ApiClientApiException apiEx = new ApiClientApiException("insert failed", 401, "unauthorized");
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<V1InsertRecordData> batch = Arrays.asList(
                V1InsertRecordData.builder().data(new HashMap<>()).build(),
                V1InsertRecordData.builder().data(new HashMap<>()).build());
        List<BulkInsertResponseRecord> errors = Utils.handleBulkInsertBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals(401, errors.get(0).getHttpCode());
        Assert.assertEquals("insert failed", errors.get(0).getError());
    }

    @Test
    public void testHandleBulkInsertBatchException_apiExceptionWithErrorKeyBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "top level auth error");
        ApiClientApiException apiEx = new ApiClientApiException("insert failed", 401, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<V1InsertRecordData> batch = Collections.singletonList(V1InsertRecordData.builder().data(new HashMap<>()).build());
        List<BulkInsertResponseRecord> errors = Utils.handleBulkInsertBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(401, errors.get(0).getHttpCode());
        Assert.assertEquals("top level auth error", errors.get(0).getError());
    }

    @Test
    public void testHandleBulkInsertBatchException_genericException() {
        RuntimeException ex = new RuntimeException("boom");
        List<V1InsertRecordData> batch = Collections.singletonList(V1InsertRecordData.builder().data(new HashMap<>()).build());

        List<BulkInsertResponseRecord> errors = Utils.handleBulkInsertBatchException(ex, batch, 1, 2);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(500, errors.get(0).getHttpCode());
        Assert.assertEquals("boom", errors.get(0).getError());
        Assert.assertEquals(2, errors.get(0).getIndex());
        // Projected error records carry no table/id/field data.
        Assert.assertNull(errors.get(0).getTableName());
        Assert.assertNull(errors.get(0).getSkyflowId());
        Assert.assertNull(errors.get(0).getFields());
        Assert.assertNull(errors.get(0).getHashedData());
    }

    @Test
    public void testHandleBulkInsertBatchException_recordsBodyCarriesTableAndSkyflowId() {
        // createInsertErrorRecord now builds a BulkInsertResponseRecord directly, so per-record
        // table/id data from the error body survives instead of being nulled out.
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("tableName", "cards");
        recordMap.put("error", "duplicate");
        recordMap.put("httpCode", 409);
        Map<String, Object> body = new HashMap<>();
        body.put("records", Collections.singletonList(recordMap));
        ApiClientApiException apiEx = new ApiClientApiException("insert failed", 409, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<V1InsertRecordData> batch = Collections.singletonList(
                V1InsertRecordData.builder().data(new HashMap<>()).build());
        List<BulkInsertResponseRecord> records = Utils.handleBulkInsertBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, records.size());
        Assert.assertEquals("cards", records.get(0).getTableName());
        Assert.assertEquals("duplicate", records.get(0).getError());
        Assert.assertEquals(409, records.get(0).getHttpCode());
    }

    @Test
    public void testHandleBulkInsertBatchException_recordsBodyFallsBackToUnknownError() {
        // An entry with no error/message key still produces a record rather than being skipped.
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("httpCode", 500);
        Map<String, Object> body = new HashMap<>();
        body.put("records", Collections.singletonList(recordMap));
        ApiClientApiException apiEx = new ApiClientApiException("insert failed", 500, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<V1InsertRecordData> batch = Collections.singletonList(
                V1InsertRecordData.builder().data(new HashMap<>()).build());
        List<BulkInsertResponseRecord> records = Utils.handleBulkInsertBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, records.size());
        Assert.assertEquals("Unknown error", records.get(0).getError());
        Assert.assertEquals(500, records.get(0).getHttpCode());
    }

    @Test
    public void testHandleBulkInsertBatchException_indexOffsetAcrossBatches() {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "auth error");
        ApiClientApiException apiEx = new ApiClientApiException("insert failed", 401, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<V1InsertRecordData> batch = Arrays.asList(
                V1InsertRecordData.builder().data(new HashMap<>()).build(),
                V1InsertRecordData.builder().data(new HashMap<>()).build());
        List<BulkInsertResponseRecord> records = Utils.handleBulkInsertBatchException(wrapper, batch, 2, 50);

        Assert.assertEquals(100, records.get(0).getIndex());
        Assert.assertEquals(101, records.get(1).getIndex());
    }

    @Test
    public void testHandleBulkInsertBatchException_genericExceptionHasNoRequestId() {
        // A non-API failure has no response headers to read a request id from.
        RuntimeException ex = new RuntimeException("boom");
        List<V1InsertRecordData> batch = Collections.singletonList(
                V1InsertRecordData.builder().data(new HashMap<>()).build());

        List<BulkInsertResponseRecord> records = Utils.handleBulkInsertBatchException(ex, batch, 0, 50);

        Assert.assertEquals(1, records.size());
        Assert.assertNull(records.get(0).getRequestId());
        Assert.assertEquals("boom", records.get(0).getError());
    }

    @Test
    public void testHandleBulkInsertBatchException_nonApiCauseUsesCauseMessage() {
        // Cause is non-null but not an ApiClientApiException: the message ladder should
        // pick up the cause's own message rather than the outer wrapper's.
        RuntimeException ex = new RuntimeException("wrapper", new IllegalStateException("inner boom"));
        List<V1InsertRecordData> batch = Collections.singletonList(
                V1InsertRecordData.builder().data(new HashMap<>()).build());

        List<BulkInsertResponseRecord> records = Utils.handleBulkInsertBatchException(ex, batch, 0, 50);

        Assert.assertEquals(1, records.size());
        Assert.assertEquals(500, records.get(0).getHttpCode());
        Assert.assertEquals("inner boom", records.get(0).getError());
    }

    @Test
    public void testHandleBulkInsertBatchException_nonApiCauseWithNestedCauseUsesNestedToString() {
        // When the cause itself wraps another throwable, the ladder resolves the message
        // down to the nested cause's toString().
        RuntimeException ex = new RuntimeException("wrapper",
                new IllegalStateException("inner boom", new IllegalArgumentException("root cause")));
        List<V1InsertRecordData> batch = Collections.singletonList(
                V1InsertRecordData.builder().data(new HashMap<>()).build());

        List<BulkInsertResponseRecord> records = Utils.handleBulkInsertBatchException(ex, batch, 0, 50);

        Assert.assertEquals(1, records.size());
        Assert.assertEquals(500, records.get(0).getHttpCode());
        Assert.assertEquals("java.lang.IllegalArgumentException: root cause", records.get(0).getError());
    }

    // ── createInsertErrorRecord / createDetokenizeErrorRecord branch coverage ─

    @Test
    public void testCreateInsertErrorRecord_nullRecordMapReturnsNull() {
        Assert.assertNull(Utils.createInsertErrorRecord(null, 0, "req-1"));
        Assert.assertNull(Utils.createDetokenizeErrorRecord(null, 0, "req-1"));
    }

    @Test
    public void testCreateErrorRecords_snakeCaseHttpCodeKey() {
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("http_code", 409);
        recordMap.put("error", "duplicate");

        Assert.assertEquals(409, Utils.createInsertErrorRecord(recordMap, 0, null).getHttpCode());
        Assert.assertEquals(409, Utils.createDetokenizeErrorRecord(recordMap, 0, null).getHttpCode());
    }

    @Test
    public void testCreateErrorRecords_statusCodeKey() {
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("statusCode", 422);
        recordMap.put("error", "unprocessable");

        Assert.assertEquals(422, Utils.createInsertErrorRecord(recordMap, 0, null).getHttpCode());
        Assert.assertEquals(422, Utils.createDetokenizeErrorRecord(recordMap, 0, null).getHttpCode());
    }

    @Test
    public void testCreateErrorRecords_noHttpCodeKeyDefaultsTo500() {
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("error", "no code supplied");

        Assert.assertEquals(500, Utils.createInsertErrorRecord(recordMap, 0, null).getHttpCode());
        Assert.assertEquals(500, Utils.createDetokenizeErrorRecord(recordMap, 0, null).getHttpCode());
    }

    // ── error-record building: keys present with explicit null values ────────
    // Regression: the vault sends "skyflowID": null / "tableName": null on failed records, and
    // containsKey() is true for those. Reading them unguarded threw NPE and masked the real error.

    @Test
    public void testCreateInsertErrorRecord_nullSkyflowIdAndTableNameDoNotThrow() {
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("skyflowID", null);
        recordMap.put("tableName", null);
        recordMap.put("error", "Invalid request. Table not found.");
        recordMap.put("httpCode", 400);

        BulkInsertResponseRecord record = Utils.createInsertErrorRecord(recordMap, 0, "req-1");

        Assert.assertNull(record.getSkyflowId());
        Assert.assertNull(record.getTableName());
        Assert.assertEquals(400, record.getHttpCode());
        Assert.assertEquals("Invalid request. Table not found.", record.getError());
    }

    @Test
    public void testCreateDetokenizeErrorRecord_nullTokenFieldsDoNotThrow() {
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("token", null);
        recordMap.put("tokenGroupName", null);
        recordMap.put("error", "Token not found.");
        recordMap.put("httpCode", 404);

        BulkDetokenizeResponseRecord record = Utils.createDetokenizeErrorRecord(recordMap, 0, "req-1");

        Assert.assertNull(record.getToken());
        Assert.assertNull(record.getTokenGroupName());
        Assert.assertEquals(404, record.getHttpCode());
        Assert.assertEquals("Token not found.", record.getError());
    }

    @Test
    public void testCreateErrorRecords_nullHttpCodeValueFallsBackTo500() {
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("httpCode", null);
        recordMap.put("error", "boom");

        Assert.assertEquals(500, Utils.createErrorRecord(recordMap, 0, null).getCode());
        Assert.assertEquals(500, Utils.createInsertErrorRecord(recordMap, 0, null).getHttpCode());
        Assert.assertEquals(500, Utils.createDetokenizeErrorRecord(recordMap, 0, null).getHttpCode());
    }

    @Test
    public void testCreateErrorRecords_nonIntegerHttpCodeIsCoerced() {
        Map<String, Object> asLong = new HashMap<>();
        asLong.put("httpCode", 409L);
        asLong.put("error", "conflict");
        Assert.assertEquals(409, Utils.createInsertErrorRecord(asLong, 0, null).getHttpCode());

        Map<String, Object> asDouble = new HashMap<>();
        asDouble.put("httpCode", 503.0d);
        asDouble.put("error", "unavailable");
        Assert.assertEquals(503, Utils.createInsertErrorRecord(asDouble, 0, null).getHttpCode());

        Map<String, Object> asText = new HashMap<>();
        asText.put("httpCode", "422");
        asText.put("error", "unprocessable");
        Assert.assertEquals(422, Utils.createInsertErrorRecord(asText, 0, null).getHttpCode());
    }

    @Test
    public void testCreateErrorRecords_nonStringErrorDoesNotThrow() {
        Map<String, Object> nested = new HashMap<>();
        nested.put("detail", "inner");
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("error", nested);
        recordMap.put("httpCode", 500);

        Assert.assertNotNull(Utils.createInsertErrorRecord(recordMap, 0, null).getError());
        Assert.assertNotNull(Utils.createErrorRecord(recordMap, 0, null).getError());
    }

    @Test
    public void testCreateErrorRecords_nullErrorValueFallsThroughToMessage() {
        // A null error text would make the record read as a SUCCESS downstream, since failures are
        // counted by getError() != null.
        Map<String, Object> withMessage = new HashMap<>();
        withMessage.put("error", null);
        withMessage.put("message", "vault unreachable");
        Assert.assertEquals("vault unreachable", Utils.createInsertErrorRecord(withMessage, 0, null).getError());

        Map<String, Object> withNeither = new HashMap<>();
        withNeither.put("error", null);
        withNeither.put("message", null);
        Assert.assertEquals("Unknown error", Utils.createInsertErrorRecord(withNeither, 0, null).getError());
    }

    @Test
    public void testCreateErrorRecords_messageKeyIsUsedWhenErrorKeyAbsent() {
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("message", "vault not found");
        recordMap.put("httpCode", 404);

        Assert.assertEquals("vault not found", Utils.createInsertErrorRecord(recordMap, 0, null).getError());
        Assert.assertEquals("vault not found", Utils.createDetokenizeErrorRecord(recordMap, 0, null).getError());
    }

    @Test
    public void testCreateInsertErrorRecord_readsSkyflowIdUsingWireCasing() {
        // The API returns the id as "skyflowID" — matching @JsonProperty("skyflowID") on the
        // generated V1RecordResponseObject — even though the SDK exposes it as getSkyflowId().
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("skyflowID", "id-1");
        recordMap.put("tableName", "cards");
        recordMap.put("error", "duplicate");

        BulkInsertResponseRecord record = Utils.createInsertErrorRecord(recordMap, 0, null);

        Assert.assertEquals("id-1", record.getSkyflowId());
        Assert.assertEquals("cards", record.getTableName());
    }

    @Test
    public void testCreateErrorRecords_requestIdIsCarried() {
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("error", "boom");

        Assert.assertEquals("req-7", Utils.createInsertErrorRecord(recordMap, 3, "req-7").getRequestId());
        Assert.assertEquals("req-7", Utils.createDetokenizeErrorRecord(recordMap, 3, "req-7").getRequestId());
        Assert.assertEquals(3, Utils.createInsertErrorRecord(recordMap, 3, "req-7").getIndex());
    }

    // ── handleBulkInsertBatchException, remaining branches ────────────────────

    @Test
    public void testHandleBulkInsertBatchException_errorFieldAsObjectUsesHelper() {
        // The API's structured error envelope: {"error": {message, httpCode, ...}}
        Map<String, Object> errorObject = new HashMap<>();
        errorObject.put("message", "vault not found");
        errorObject.put("httpCode", 404);
        Map<String, Object> body = new HashMap<>();
        body.put("error", errorObject);
        ApiClientApiException apiEx = new ApiClientApiException("insert failed", 404, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<V1InsertRecordData> batch = Arrays.asList(
                V1InsertRecordData.builder().data(new HashMap<>()).build(),
                V1InsertRecordData.builder().data(new HashMap<>()).build());
        List<BulkInsertResponseRecord> records = Utils.handleBulkInsertBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(2, records.size());
        for (BulkInsertResponseRecord record : records) {
            Assert.assertEquals("vault not found", record.getError());
            Assert.assertEquals(404, record.getHttpCode());
        }
    }

    @Test
    public void testHandleBulkInsertBatchException_errorFieldNeitherMapNorStringUsesApiMessage() {
        Map<String, Object> body = new HashMap<>();
        body.put("error", 500);
        ApiClientApiException apiEx = new ApiClientApiException("insert failed", 500, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<V1InsertRecordData> batch = Collections.singletonList(
                V1InsertRecordData.builder().data(new HashMap<>()).build());
        List<BulkInsertResponseRecord> records = Utils.handleBulkInsertBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, records.size());
        Assert.assertEquals("insert failed", records.get(0).getError());
    }

    @Test
    public void testHandleBulkInsertBatchException_recordsNotAListFallsBackToBatchWideError() {
        Map<String, Object> body = new HashMap<>();
        body.put("records", "not-a-list");
        ApiClientApiException apiEx = new ApiClientApiException("insert failed", 400, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<V1InsertRecordData> batch = Collections.singletonList(
                V1InsertRecordData.builder().data(new HashMap<>()).build());
        List<BulkInsertResponseRecord> records = Utils.handleBulkInsertBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, records.size());
        Assert.assertEquals("insert failed", records.get(0).getError());
        Assert.assertEquals(400, records.get(0).getHttpCode());
    }

    @Test
    public void testHandleBulkInsertBatchException_nonMapEntriesAreSkipped() {
        Map<String, Object> body = new HashMap<>();
        body.put("records", Arrays.asList("not-a-map", null));
        ApiClientApiException apiEx = new ApiClientApiException("insert failed", 400, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<V1InsertRecordData> batch = Collections.singletonList(
                V1InsertRecordData.builder().data(new HashMap<>()).build());
        List<BulkInsertResponseRecord> records = Utils.handleBulkInsertBatchException(wrapper, batch, 0, 50);

        // No entry parsed, so the batch-wide fallback fires instead.
        Assert.assertEquals(1, records.size());
        Assert.assertEquals("insert failed", records.get(0).getError());
    }

    @Test
    public void testHandleBulkInsertBatchException_bodyWithNeitherRecordsNorErrorKey() {
        // A map body that matches neither branch falls through to the batch-wide fallback.
        Map<String, Object> body = new HashMap<>();
        body.put("unexpected", "shape");
        ApiClientApiException apiEx = new ApiClientApiException("insert failed", 503, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<V1InsertRecordData> batch = Collections.singletonList(
                V1InsertRecordData.builder().data(new HashMap<>()).build());
        List<BulkInsertResponseRecord> records = Utils.handleBulkInsertBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, records.size());
        Assert.assertEquals("insert failed", records.get(0).getError());
        Assert.assertEquals(503, records.get(0).getHttpCode());
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
        List<BulkDetokenizeResponseRecord> errors = Utils.handleBulkDetokenizeBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(404, errors.get(0).getHttpCode());
        Assert.assertEquals("token not found", errors.get(0).getError());
        Assert.assertEquals(0, errors.get(0).getIndex());
        // This entry carried no token/group of its own, so those stay null.
        Assert.assertNull(errors.get(0).getToken());
        Assert.assertNull(errors.get(0).getTokenGroupName());
        Assert.assertNull(errors.get(0).getMetadata());
    }

    @Test
    public void testHandleBulkDetokenizeBatchException_responseBodyCarriesTokenAndGroup() {
        // createDetokenizeErrorRecord builds a BulkDetokenizeResponseRecord directly, so the
        // failing token echoed back by the API survives instead of being nulled out.
        Map<String, Object> errorRecordMap = new HashMap<>();
        errorRecordMap.put("token", "tok-bad");
        errorRecordMap.put("tokenGroupName", "email_group");
        errorRecordMap.put("error", "token not found");
        errorRecordMap.put("httpCode", 404);
        Map<String, Object> body = new HashMap<>();
        body.put("response", Collections.singletonList(errorRecordMap));
        ApiClientApiException apiEx = new ApiClientApiException("detokenize failed", 404, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        V1FlowDetokenizeRequest batch = V1FlowDetokenizeRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("tok-bad"))
                .build();
        List<BulkDetokenizeResponseRecord> records = Utils.handleBulkDetokenizeBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, records.size());
        Assert.assertEquals("tok-bad", records.get(0).getToken());
        Assert.assertEquals("email_group", records.get(0).getTokenGroupName());
        Assert.assertEquals("token not found", records.get(0).getError());
        Assert.assertEquals(404, records.get(0).getHttpCode());
    }

    @Test
    public void testHandleBulkDetokenizeBatchException_responseBodyFallsBackToUnknownError() {
        Map<String, Object> errorRecordMap = new HashMap<>();
        errorRecordMap.put("httpCode", 500);
        Map<String, Object> body = new HashMap<>();
        body.put("response", Collections.singletonList(errorRecordMap));
        ApiClientApiException apiEx = new ApiClientApiException("detokenize failed", 500, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        V1FlowDetokenizeRequest batch = V1FlowDetokenizeRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("t1"))
                .build();
        List<BulkDetokenizeResponseRecord> records = Utils.handleBulkDetokenizeBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, records.size());
        Assert.assertEquals("Unknown error", records.get(0).getError());
        Assert.assertEquals(500, records.get(0).getHttpCode());
    }

    @Test
    public void testHandleBulkDetokenizeBatchException_topLevelErrorAppliesToEveryToken() {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "top level auth error");
        ApiClientApiException apiEx = new ApiClientApiException("detokenize failed", 401, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        V1FlowDetokenizeRequest batch = V1FlowDetokenizeRequest.builder()
                .vaultId("vault123")
                .tokens(Arrays.asList("t1", "t2"))
                .build();
        List<BulkDetokenizeResponseRecord> records = Utils.handleBulkDetokenizeBatchException(wrapper, batch, 2, 50);

        Assert.assertEquals(2, records.size());
        Assert.assertEquals("top level auth error", records.get(0).getError());
        Assert.assertEquals(401, records.get(0).getHttpCode());
        // index continues from the batch offset
        Assert.assertEquals(100, records.get(0).getIndex());
        Assert.assertEquals(101, records.get(1).getIndex());
    }

    @Test
    public void testHandleBulkDetokenizeBatchException_errorFieldAsObjectUsesHelper() {
        Map<String, Object> errorObject = new HashMap<>();
        errorObject.put("message", "vault not found");
        errorObject.put("httpCode", 404);
        Map<String, Object> body = new HashMap<>();
        body.put("error", errorObject);
        ApiClientApiException apiEx = new ApiClientApiException("detokenize failed", 404, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        V1FlowDetokenizeRequest batch = V1FlowDetokenizeRequest.builder()
                .vaultId("vault123")
                .tokens(Arrays.asList("t1", "t2"))
                .build();
        List<BulkDetokenizeResponseRecord> records = Utils.handleBulkDetokenizeBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(2, records.size());
        for (BulkDetokenizeResponseRecord record : records) {
            Assert.assertEquals("vault not found", record.getError());
            Assert.assertEquals(404, record.getHttpCode());
        }
    }

    @Test
    public void testHandleBulkDetokenizeBatchException_errorFieldNeitherMapNorStringUsesApiMessage() {
        Map<String, Object> body = new HashMap<>();
        body.put("error", 500);
        ApiClientApiException apiEx = new ApiClientApiException("detokenize failed", 500, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        V1FlowDetokenizeRequest batch = V1FlowDetokenizeRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("t1"))
                .build();
        List<BulkDetokenizeResponseRecord> records = Utils.handleBulkDetokenizeBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, records.size());
        Assert.assertEquals("detokenize failed", records.get(0).getError());
    }

    @Test
    public void testHandleBulkDetokenizeBatchException_responseNotAListFallsBackToBatchWideError() {
        Map<String, Object> body = new HashMap<>();
        body.put("response", "not-a-list");
        ApiClientApiException apiEx = new ApiClientApiException("detokenize failed", 400, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        V1FlowDetokenizeRequest batch = V1FlowDetokenizeRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("t1"))
                .build();
        List<BulkDetokenizeResponseRecord> records = Utils.handleBulkDetokenizeBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, records.size());
        Assert.assertEquals("detokenize failed", records.get(0).getError());
    }

    @Test
    public void testHandleBulkDetokenizeBatchException_nonMapEntriesAreSkipped() {
        Map<String, Object> body = new HashMap<>();
        body.put("response", Arrays.asList("not-a-map", null));
        ApiClientApiException apiEx = new ApiClientApiException("detokenize failed", 400, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        V1FlowDetokenizeRequest batch = V1FlowDetokenizeRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("t1"))
                .build();
        List<BulkDetokenizeResponseRecord> records = Utils.handleBulkDetokenizeBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, records.size());
        Assert.assertEquals("detokenize failed", records.get(0).getError());
    }

    @Test
    public void testHandleBulkDetokenizeBatchException_batchWithNoTokensProducesNoRecords() {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "auth error");
        ApiClientApiException apiEx = new ApiClientApiException("detokenize failed", 401, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        V1FlowDetokenizeRequest batch = V1FlowDetokenizeRequest.builder().vaultId("vault123").build();
        List<BulkDetokenizeResponseRecord> records = Utils.handleBulkDetokenizeBatchException(wrapper, batch, 0, 50);

        Assert.assertTrue(records.isEmpty());
    }

    @Test
    public void testHandleBulkDetokenizeBatchException_bodyWithNeitherResponseNorErrorKey() {
        Map<String, Object> body = new HashMap<>();
        body.put("unexpected", "shape");
        ApiClientApiException apiEx = new ApiClientApiException("detokenize failed", 503, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        V1FlowDetokenizeRequest batch = V1FlowDetokenizeRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("t1"))
                .build();
        List<BulkDetokenizeResponseRecord> records = Utils.handleBulkDetokenizeBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, records.size());
        Assert.assertEquals("detokenize failed", records.get(0).getError());
        Assert.assertEquals(503, records.get(0).getHttpCode());
    }

    // ── formatBulk*Response, empty/absent bodies ──────────────────────────────

    @Test
    public void testFormatBulkResponses_nullResponseReturnsNull() {
        Assert.assertNull(Utils.formatBulkInsertResponse(null, 0, 50, null));
        Assert.assertNull(Utils.formatBulkDetokenizeResponse(null, 0, 50, null));
    }

    @Test
    public void testFormatBulkResponses_absentRecordsReturnsNull() {
        Assert.assertNull(Utils.formatBulkInsertResponse(
                V1InsertResponse.builder().build(), 0, 50, null));
        Assert.assertNull(Utils.formatBulkDetokenizeResponse(
                V1FlowDetokenizeResponse.builder().build(), 0, 50, null));
    }

    @Test
    public void testHandleBulkDetokenizeBatchException_genericExceptionHasNoRequestId() {
        RuntimeException ex = new RuntimeException("boom");
        V1FlowDetokenizeRequest batch = V1FlowDetokenizeRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("t1"))
                .build();

        List<BulkDetokenizeResponseRecord> records = Utils.handleBulkDetokenizeBatchException(ex, batch, 0, 50);

        Assert.assertEquals(1, records.size());
        Assert.assertNull(records.get(0).getRequestId());
        Assert.assertEquals("boom", records.get(0).getError());
    }

    @Test
    public void testHandleBulkDetokenizeBatchException_genericException() {
        RuntimeException ex = new RuntimeException("boom");
        V1FlowDetokenizeRequest batch = V1FlowDetokenizeRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("t1"))
                .build();

        List<BulkDetokenizeResponseRecord> errors = Utils.handleBulkDetokenizeBatchException(ex, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(500, errors.get(0).getHttpCode());
        Assert.assertEquals("boom", errors.get(0).getError());
    }

    @Test
    public void testHandleBulkDetokenizeBatchException_nonApiCauseUsesCauseMessage() {
        // Cause is non-null but not an ApiClientApiException: the message ladder resolves the
        // nested cause's toString() rather than the outer wrapper's message.
        RuntimeException ex = new RuntimeException("wrapper",
                new IllegalStateException("inner boom", new IllegalArgumentException("root cause")));
        V1FlowDetokenizeRequest batch = V1FlowDetokenizeRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("t1"))
                .build();

        List<BulkDetokenizeResponseRecord> records = Utils.handleBulkDetokenizeBatchException(ex, batch, 0, 50);

        Assert.assertEquals(1, records.size());
        Assert.assertEquals(500, records.get(0).getHttpCode());
        Assert.assertEquals("java.lang.IllegalArgumentException: root cause", records.get(0).getError());
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
        List<BulkDeleteTokensResponseRecord> errors = Utils.handleBulkDeleteTokensBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(Integer.valueOf(404), errors.get(0).getHttpCode());
        Assert.assertEquals("not found", errors.get(0).getError());
        // token comes from the batch we sent, so an error record is never missing it
        Assert.assertEquals("t1", errors.get(0).getToken());
        Assert.assertEquals(0, errors.get(0).getIndex());
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
        List<BulkDeleteTokensResponseRecord> errors = Utils.handleBulkDeleteTokensBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(Integer.valueOf(403), errors.get(0).getHttpCode());
        Assert.assertEquals("top level delete error", errors.get(0).getError());
        Assert.assertEquals("t1", errors.get(0).getToken());
    }

    @Test
    public void testHandleBulkDeleteTokensBatchException_genericException() {
        RuntimeException ex = new RuntimeException("boom");
        V1FlowDeleteTokenRequest batch = V1FlowDeleteTokenRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("t1"))
                .build();

        List<BulkDeleteTokensResponseRecord> errors = Utils.handleBulkDeleteTokensBatchException(ex, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(Integer.valueOf(500), errors.get(0).getHttpCode());
        Assert.assertEquals("t1", errors.get(0).getToken());
    }

    @Test
    public void testHandleBulkDeleteTokensBatchException_errorFieldAsObjectUsesHelper() {
        // Structured error envelope {"error": {message, httpCode}} → parsed per token via the helper.
        Map<String, Object> errorObject = new HashMap<>();
        errorObject.put("message", "vault not found");
        errorObject.put("httpCode", 404);
        Map<String, Object> body = new HashMap<>();
        body.put("error", errorObject);
        ApiClientApiException apiEx = new ApiClientApiException("delete failed", 404, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        V1FlowDeleteTokenRequest batch = V1FlowDeleteTokenRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("t1"))
                .build();
        List<BulkDeleteTokensResponseRecord> errors = Utils.handleBulkDeleteTokensBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(Integer.valueOf(404), errors.get(0).getHttpCode());
        Assert.assertEquals("vault not found", errors.get(0).getError());
        Assert.assertEquals("t1", errors.get(0).getToken());
    }

    @Test
    public void testHandleBulkDeleteTokensBatchException_errorFieldNeitherMapNorStringUsesApiMessage() {
        Map<String, Object> body = new HashMap<>();
        body.put("error", 500);
        ApiClientApiException apiEx = new ApiClientApiException("delete failed", 500, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        V1FlowDeleteTokenRequest batch = V1FlowDeleteTokenRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("t1"))
                .build();
        List<BulkDeleteTokensResponseRecord> errors = Utils.handleBulkDeleteTokensBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("delete failed", errors.get(0).getError());
    }

    @Test
    public void testHandleBulkDeleteTokensBatchException_bodyWithNeitherTokensNorErrorKey() {
        // A map body matching neither branch falls through to the batch-wide fallback.
        Map<String, Object> body = new HashMap<>();
        body.put("unexpected", "shape");
        ApiClientApiException apiEx = new ApiClientApiException("delete failed", 503, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        V1FlowDeleteTokenRequest batch = V1FlowDeleteTokenRequest.builder()
                .vaultId("vault123")
                .tokens(Arrays.asList("t1", "t2"))
                .build();
        List<BulkDeleteTokensResponseRecord> errors = Utils.handleBulkDeleteTokensBatchException(wrapper, batch, 1, 50);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals(Integer.valueOf(503), errors.get(0).getHttpCode());
        Assert.assertEquals("delete failed", errors.get(0).getError());
        // startIndex = batchNumber * batchSize = 50
        Assert.assertEquals(50, errors.get(0).getIndex());
        Assert.assertEquals(51, errors.get(1).getIndex());
        Assert.assertEquals("t2", errors.get(1).getToken());
    }

    @Test
    public void testHandleBulkDeleteTokensBatchException_tokensNotAListFallsBackToBatchWideError() {
        Map<String, Object> body = new HashMap<>();
        body.put("tokens", "not-a-list");
        ApiClientApiException apiEx = new ApiClientApiException("delete failed", 400, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        V1FlowDeleteTokenRequest batch = V1FlowDeleteTokenRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("t1"))
                .build();
        List<BulkDeleteTokensResponseRecord> errors = Utils.handleBulkDeleteTokensBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("delete failed", errors.get(0).getError());
        Assert.assertEquals("t1", errors.get(0).getToken());
    }

    @Test
    public void testHandleBulkDeleteTokensBatchException_nonMapEntriesAreSkipped() {
        Map<String, Object> body = new HashMap<>();
        body.put("tokens", Arrays.asList("not-a-map", null));
        ApiClientApiException apiEx = new ApiClientApiException("delete failed", 400, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        V1FlowDeleteTokenRequest batch = V1FlowDeleteTokenRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("t1"))
                .build();
        List<BulkDeleteTokensResponseRecord> errors = Utils.handleBulkDeleteTokensBatchException(wrapper, batch, 0, 50);

        // No entry parsed, so the batch-wide fallback fires instead.
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("delete failed", errors.get(0).getError());
    }

    @Test
    public void testHandleBulkDeleteTokensBatchException_recordEchoesValueAndReadsHttpCodeAndMessage() {
        // createDeleteTokensErrorRecord: http_code key, "message" key, and an echoed "value" token.
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("http_code", 409);
        tokenMap.put("message", "already deleted");
        tokenMap.put("value", "echoed-token");
        Map<String, Object> body = new HashMap<>();
        body.put("tokens", Collections.singletonList(tokenMap));
        ApiClientApiException apiEx = new ApiClientApiException("delete failed", 409, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        V1FlowDeleteTokenRequest batch = V1FlowDeleteTokenRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("requested-token"))
                .build();
        List<BulkDeleteTokensResponseRecord> errors = Utils.handleBulkDeleteTokensBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(Integer.valueOf(409), errors.get(0).getHttpCode());
        Assert.assertEquals("already deleted", errors.get(0).getError());
        // the echoed "value" wins over the token from the request batch
        Assert.assertEquals("echoed-token", errors.get(0).getToken());
    }

    @Test
    public void testHandleBulkDeleteTokensBatchException_recordUsesStatusCodeAndUnknownError() {
        // createDeleteTokensErrorRecord: statusCode key and the no-error/no-message fallback.
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("statusCode", 410);
        Map<String, Object> body = new HashMap<>();
        body.put("tokens", Collections.singletonList(tokenMap));
        ApiClientApiException apiEx = new ApiClientApiException("delete failed", 410, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        V1FlowDeleteTokenRequest batch = V1FlowDeleteTokenRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("t1"))
                .build();
        List<BulkDeleteTokensResponseRecord> errors = Utils.handleBulkDeleteTokensBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(Integer.valueOf(410), errors.get(0).getHttpCode());
        Assert.assertEquals("Unknown error", errors.get(0).getError());
        // no echoed value, so the requested token is reported
        Assert.assertEquals("t1", errors.get(0).getToken());
    }

    // ── handleBulkTokenizeBatchException ───────────────────────────────────────

    private static List<BulkTokenizeRequestRecord> tokenizeBatch(String value, String... groups) {
        return Collections.singletonList(BulkTokenizeRequestRecord.builder()
                .value(value).tokenGroupNames(Arrays.asList(groups)).build());
    }

    @Test
    public void testHandleBulkTokenizeBatchException_apiExceptionFailsEveryTokenGroup() {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "invalid value");
        ApiClientApiException apiEx = new ApiClientApiException("tokenize failed", 400, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<BulkTokenizeResponseRecord> errors = Utils.handleBulkTokenizeBatchException(
                wrapper, tokenizeBatch("v1", "group1", "group2"), 0);

        Assert.assertEquals(1, errors.size());
        BulkTokenizeResponseRecord record = errors.get(0);
        // index is derived from the batch position; the value is echoed from the request
        Assert.assertEquals(0, record.getIndex());
        Assert.assertEquals("v1", record.getValue());
        // one failed token entry per requested group
        Assert.assertEquals(2, record.getTokens().size());
        Assert.assertEquals("group1", record.getTokens().get(0).getTokenGroupName());
        Assert.assertEquals("invalid value", record.getTokens().get(0).getError());
        Assert.assertEquals(Integer.valueOf(400), record.getTokens().get(0).getHttpCode());
        Assert.assertEquals("group2", record.getTokens().get(1).getTokenGroupName());
        Assert.assertEquals("invalid value", record.getTokens().get(1).getError());
    }

    @Test
    public void testHandleBulkTokenizeBatchException_genericException() {
        RuntimeException ex = new RuntimeException("boom");

        // this batch starts at index 3 in the caller's list
        List<BulkTokenizeResponseRecord> errors = Utils.handleBulkTokenizeBatchException(
                ex, tokenizeBatch("v1", "group1"), 3);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(3, errors.get(0).getIndex());
        Assert.assertEquals(Integer.valueOf(500), errors.get(0).getTokens().get(0).getHttpCode());
        Assert.assertEquals("boom", errors.get(0).getTokens().get(0).getError());
    }

    @Test
    public void testHandleBulkTokenizeBatchException_indexesRunConsecutivelyFromBatchStart() {
        RuntimeException ex = new RuntimeException("boom");
        List<BulkTokenizeRequestRecord> batch = Arrays.asList(
                BulkTokenizeRequestRecord.builder().value("v1").build(),
                BulkTokenizeRequestRecord.builder().value("v2").build());

        // this batch starts at index 20, so it covers 20 and 21
        List<BulkTokenizeResponseRecord> errors = Utils.handleBulkTokenizeBatchException(ex, batch, 20);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals(20, errors.get(0).getIndex());
        Assert.assertEquals("v1", errors.get(0).getValue());
        Assert.assertEquals(21, errors.get(1).getIndex());
        Assert.assertEquals("v2", errors.get(1).getValue());
    }

    @Test
    public void testHandleBulkTokenizeBatchException_noTokenGroupsStillReportsOneEntry() {
        RuntimeException ex = new RuntimeException("boom");
        List<BulkTokenizeRequestRecord> batch = Collections.singletonList(
                BulkTokenizeRequestRecord.builder().value("v1").build());

        List<BulkTokenizeResponseRecord> errors = Utils.handleBulkTokenizeBatchException(ex, batch, 0);

        Assert.assertEquals(1, errors.get(0).getTokens().size());
        Assert.assertNull(errors.get(0).getTokens().get(0).getTokenGroupName());
        Assert.assertEquals("boom", errors.get(0).getTokens().get(0).getError());
    }

    @Test
    public void testHandleBulkTokenizeBatchException_errorBodyWithResponseArrayRebuildsRecords() {
        // A 4xx whose body echoes the per-row "response" array is rebuilt via tokenizeRecordsFromErrorBody
        // rather than summarized by the bare status code.
        Map<String, Object> tokenRow = new HashMap<>();
        tokenRow.put("tokenGroupName", "group1");
        tokenRow.put("error", "BYOT token should contain one token group");
        tokenRow.put("httpCode", 400);
        Map<String, Object> responseRow = new HashMap<>();
        responseRow.put("value", "v1");
        responseRow.put("tokens", Collections.singletonList(tokenRow));
        Map<String, Object> body = new HashMap<>();
        body.put("response", Collections.singletonList(responseRow));
        ApiClientApiException apiEx = new ApiClientApiException("tokenize failed", 400, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<BulkTokenizeResponseRecord> errors = Utils.handleBulkTokenizeBatchException(
                wrapper, tokenizeBatch("v1", "group1"), 0);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("v1", errors.get(0).getValue());
        Assert.assertEquals(1, errors.get(0).getTokens().size());
        Assert.assertEquals("group1", errors.get(0).getTokens().get(0).getTokenGroupName());
        Assert.assertEquals("BYOT token should contain one token group",
                errors.get(0).getTokens().get(0).getError());
        Assert.assertEquals(Integer.valueOf(400), errors.get(0).getTokens().get(0).getHttpCode());
    }

    @Test
    public void testHandleBulkTokenizeBatchException_errorFieldAsObjectUsesStructuredMessage() {
        // extractBatchErrorMessage reads {"error": {message}} when the body has no per-row response.
        Map<String, Object> errorObject = new HashMap<>();
        errorObject.put("message", "vault not found");
        Map<String, Object> body = new HashMap<>();
        body.put("error", errorObject);
        ApiClientApiException apiEx = new ApiClientApiException("tokenize failed", 404, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<BulkTokenizeResponseRecord> errors = Utils.handleBulkTokenizeBatchException(
                wrapper, tokenizeBatch("v1", "group1"), 0);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("vault not found", errors.get(0).getTokens().get(0).getError());
        Assert.assertEquals(Integer.valueOf(404), errors.get(0).getTokens().get(0).getHttpCode());
    }

    @Test
    public void testHandleBulkTokenizeBatchException_nonMapBodyUsesApiMessage() {
        // Body is not a map, so extractBatchErrorMessage falls back to the exception's own message.
        ApiClientApiException apiEx = new ApiClientApiException("tokenize failed", 500, "raw string body");
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<BulkTokenizeResponseRecord> errors = Utils.handleBulkTokenizeBatchException(
                wrapper, tokenizeBatch("v1", "group1"), 0);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("tokenize failed", errors.get(0).getTokens().get(0).getError());
        Assert.assertEquals(Integer.valueOf(500), errors.get(0).getTokens().get(0).getHttpCode());
    }

    @Test
    public void testHandleBulkTokenizeBatchException_nullBatchReturnsEmpty() {
        RuntimeException ex = new RuntimeException("boom");

        List<BulkTokenizeResponseRecord> errors = Utils.handleBulkTokenizeBatchException(ex, null, 0);

        Assert.assertTrue(errors.isEmpty());
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

        Assert.assertEquals(1, result.getRecords().size());
        BulkInsertResponseRecord inserted = result.getRecords().get(0);
        Assert.assertEquals("sky-id-1", inserted.getSkyflowId());
        Assert.assertEquals(tokens, inserted.getFields());
        Assert.assertEquals(0, inserted.getIndex());
        Assert.assertEquals(200, inserted.getHttpCode());
        Assert.assertNull(inserted.getError());
    }

    @Test
    public void testFormatBulkInsertResponse_indexOffsetByBatchNumber() {
        V1RecordResponseObject record = V1RecordResponseObject.builder().skyflowId("sky-id-1").build();
        V1InsertResponse response = V1InsertResponse.builder().records(Collections.singletonList(record)).build();

        BulkInsertResponse result = Utils.formatBulkInsertResponse(response, 2, 50, new HashMap<>());

        Assert.assertEquals(100, result.getRecords().get(0).getIndex());
    }

    @Test
    public void testFormatBulkInsertResponse_errorWithMissingHttpCodeDefaultsTo500() {
        V1RecordResponseObject record = V1RecordResponseObject.builder()
                .error("insert failed")
                .build();
        V1InsertResponse response = V1InsertResponse.builder().records(Collections.singletonList(record)).build();

        BulkInsertResponse result = Utils.formatBulkInsertResponse(response, 0, 50, new HashMap<>());

        Assert.assertEquals(1, result.getRecords().size());
        Assert.assertEquals(500, result.getRecords().get(0).getHttpCode());
        Assert.assertEquals("insert failed", result.getRecords().get(0).getError());
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

        Assert.assertEquals(1, result.getRecords().size());
        BulkDetokenizeResponseRecord detokenized = result.getRecords().get(0);
        Assert.assertEquals("token1", detokenized.getToken());
        Assert.assertEquals(0, detokenized.getIndex());
        // Success records default to httpCode 200 and carry no error.
        Assert.assertEquals(200, detokenized.getHttpCode());
        Assert.assertNull(detokenized.getError());
        // Per-batch responses leave the summary unset.
        Assert.assertNull(result.getSummary());
    }

    @Test
    public void testFormatBulkDetokenizeResponse_indexOffsetByBatchNumber() {
        V1FlowDetokenizeResponseObject record = V1FlowDetokenizeResponseObject.builder()
                .token("token1")
                .build();
        V1FlowDetokenizeResponse response = V1FlowDetokenizeResponse.builder()
                .response(Collections.singletonList(record))
                .build();

        BulkDetokenizeResponse result = Utils.formatBulkDetokenizeResponse(response, 2, 50, new HashMap<>());

        Assert.assertEquals(100, result.getRecords().get(0).getIndex());
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

        Assert.assertEquals(1, result.getRecords().size());
        Assert.assertEquals(500, result.getRecords().get(0).getHttpCode());
        Assert.assertEquals("token not found", result.getRecords().get(0).getError());
    }

    @Test
    public void testFormatBulkDetokenizeResponse_emptyResponseReturnsNull() {
        V1FlowDetokenizeResponse response = V1FlowDetokenizeResponse.builder().build();
        Assert.assertNull(Utils.formatBulkDetokenizeResponse(response, 0, 50, new HashMap<>()));
    }

    // ── formatBulkDeleteTokensResponse ─────────────────────────────────────────

    private static V1FlowDeleteTokenRequest deleteBatchOf(String... tokens) {
        return V1FlowDeleteTokenRequest.builder()
                .vaultId("vault123")
                .tokens(Arrays.asList(tokens))
                .build();
    }

    @Test
    public void testFormatBulkDeleteTokensResponse_duplicateTokenRelaysEachRowVerbatim() {
        // the same token sent twice: the API decides each position independently, and has been
        // observed returning both 200,200 and 200,404 for the identical request. Whatever it says
        // must reach the caller unchanged - no deduplication, no normalising one row against the other.
        String token = "e5874be2-940a-4c74-9c08-dc6c1e8c6f9b";
        String message = "DeleteToken failed. Token " + token + " is invalid. Specify a valid token.";
        V1FlowDeleteTokenResponse response = V1FlowDeleteTokenResponse.builder()
                .tokens(Arrays.asList(
                        V1DeleteTokenResponseObject.builder().value(token).httpCode(200).build(),
                        V1DeleteTokenResponseObject.builder()
                                .value(token).error(message).httpCode(404).build()))
                .build();

        BulkDeleteTokensResponse result = Utils.formatBulkDeleteTokensResponse(
                response, deleteBatchOf(token, token), 0, 50, new HashMap<>());
        BulkDeleteTokensResponse withPayload = new BulkDeleteTokensResponse(
                result.getRecords(), Arrays.asList(token, token));

        Assert.assertEquals(2, withPayload.getRecords().size());
        Assert.assertEquals(0, withPayload.getRecords().get(0).getIndex());
        Assert.assertEquals(Integer.valueOf(200), withPayload.getRecords().get(0).getHttpCode());
        Assert.assertNull(withPayload.getRecords().get(0).getError());
        Assert.assertEquals(1, withPayload.getRecords().get(1).getIndex());
        Assert.assertEquals(Integer.valueOf(404), withPayload.getRecords().get(1).getHttpCode());
        Assert.assertEquals(message, withPayload.getRecords().get(1).getError());
        // the summary follows the rows, so a duplicate that the API rejected is not counted deleted
        Assert.assertEquals(2, withPayload.getSummary().getTotalTokens());
        Assert.assertEquals(1, withPayload.getSummary().getTotalDeleted());
        Assert.assertEquals(1, withPayload.getSummary().getTotalFailed());
        // 404 is not retryable, so nothing is offered for resubmission
        Assert.assertTrue(withPayload.getTokensToRetry().isEmpty());
    }

    @Test
    public void testFormatBulkDeleteTokensResponse_success() {
        V1DeleteTokenResponseObject record = V1DeleteTokenResponseObject.builder()
                .value("token1")
                .build();
        V1FlowDeleteTokenResponse response = V1FlowDeleteTokenResponse.builder()
                .tokens(Collections.singletonList(record))
                .build();

        BulkDeleteTokensResponse result = Utils.formatBulkDeleteTokensResponse(
                response, deleteBatchOf("token1"), 0, 50, new HashMap<>());

        Assert.assertEquals(1, result.getRecords().size());
        Assert.assertEquals("token1", result.getRecords().get(0).getToken());
        Assert.assertEquals(Integer.valueOf(200), result.getRecords().get(0).getHttpCode());
        Assert.assertNull(result.getRecords().get(0).getError());
        Assert.assertEquals(0, result.getRecords().get(0).getIndex());
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

        BulkDeleteTokensResponse result = Utils.formatBulkDeleteTokensResponse(
                response, deleteBatchOf("token1"), 0, 50, new HashMap<>());

        Assert.assertEquals(1, result.getRecords().size());
        Assert.assertEquals(Integer.valueOf(404), result.getRecords().get(0).getHttpCode());
        Assert.assertEquals("token not found", result.getRecords().get(0).getError());
        // API omitted the echoed value, so the token falls back to the one we sent
        Assert.assertEquals("token1", result.getRecords().get(0).getToken());
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

        BulkDeleteTokensResponse result = Utils.formatBulkDeleteTokensResponse(
                response, deleteBatchOf("token1"), 0, 50, new HashMap<>());

        Assert.assertEquals(1, result.getRecords().size());
        Assert.assertNull(result.getRecords().get(0).getError());
        Assert.assertEquals("token1", result.getRecords().get(0).getToken());
    }

    @Test
    public void testFormatBulkDeleteTokensResponse_indexesOffsetByBatch() {
        V1FlowDeleteTokenResponse response = V1FlowDeleteTokenResponse.builder()
                .tokens(Arrays.asList(
                        V1DeleteTokenResponseObject.builder().value("token3").build(),
                        V1DeleteTokenResponseObject.builder().value("token4").build()))
                .build();

        // batch 1 with batchSize 2 => indexes continue at 2
        BulkDeleteTokensResponse result = Utils.formatBulkDeleteTokensResponse(
                response, deleteBatchOf("token3", "token4"), 1, 2, new HashMap<>());

        Assert.assertEquals(2, result.getRecords().get(0).getIndex());
        Assert.assertEquals(3, result.getRecords().get(1).getIndex());
    }

    @Test
    public void testFormatBulkDeleteTokensResponse_emptyResponseReturnsNull() {
        V1FlowDeleteTokenResponse response = V1FlowDeleteTokenResponse.builder().build();
        Assert.assertNull(Utils.formatBulkDeleteTokensResponse(
                response, deleteBatchOf("token1"), 0, 50, new HashMap<>()));
    }

    // ── formatBulkTokenizeResponse ─────────────────────────────────────────────

    private static V1FlowTokenizeResponse tokenizeWire(V1FlowTokenizeResponseObject... records) {
        return V1FlowTokenizeResponse.builder().response(java.util.Arrays.asList(records)).build();
    }

    @Test
    public void testFormatBulkTokenizeResponse_success() {
        V1FlowTokenizeResponse response = tokenizeWire(V1FlowTokenizeResponseObject.builder()
                .value("value1")
                .tokens(Collections.singletonList(FlowTokenizeResponseObjectToken.builder()
                        .tokenGroupName("group1").token("tok-abc").build()))
                .build());

        BulkTokenizeResponse result = Utils.formatBulkTokenizeResponse(
                response, tokenizeBatch("value1", "group1"), 0, new HashMap<>());

        Assert.assertEquals(1, result.getRecords().size());
        BulkTokenizeResponseRecord record = result.getRecords().get(0);
        Assert.assertEquals(0, record.getIndex());
        Assert.assertEquals("value1", record.getValue());
        Assert.assertEquals("tok-abc", record.getTokens().get(0).getToken());
        Assert.assertNull(record.getTokens().get(0).getError());
    }

    @Test
    public void testFormatBulkTokenizeResponse_tokenError() {
        V1FlowTokenizeResponse response = tokenizeWire(V1FlowTokenizeResponseObject.builder()
                .value("value1")
                .tokens(Collections.singletonList(FlowTokenizeResponseObjectToken.builder()
                        .tokenGroupName("group1").error("invalid value").httpCode(400).build()))
                .build());

        BulkTokenizeResponse result = Utils.formatBulkTokenizeResponse(
                response, tokenizeBatch("value1", "group1"), 0, new HashMap<>());

        Assert.assertEquals(1, result.getRecords().size());
        TokenizeResponseToken token = result.getRecords().get(0).getTokens().get(0);
        Assert.assertEquals(Integer.valueOf(400), token.getHttpCode());
        Assert.assertEquals("invalid value", token.getError());
    }

    @Test
    public void testFormatBulkTokenizeResponse_derivesIndexFromBatchPosition() {
        V1FlowTokenizeResponse response = tokenizeWire(V1FlowTokenizeResponseObject.builder()
                .value("value1")
                .tokens(Collections.singletonList(FlowTokenizeResponseObjectToken.builder()
                        .tokenGroupName("group1").token("tok-abc").build()))
                .build());

        // this batch starts at index 40 in the caller's list
        BulkTokenizeResponse result = Utils.formatBulkTokenizeResponse(
                response, tokenizeBatch("value1", "group1"), 40, new HashMap<>());

        Assert.assertEquals(40, result.getRecords().get(0).getIndex());
    }

    @Test
    public void testFormatBulkTokenizeResponse_emptyResponseReturnsNull() {
        Assert.assertNull(Utils.formatBulkTokenizeResponse(
                V1FlowTokenizeResponse.builder().build(),
                tokenizeBatch("value1", "group1"), 0, new HashMap<>()));
    }

    // Tests for getQueryRequestBody / buildQueryResponse / getGetRequestBody / buildGetResponse
    // were removed: get and query Utils helpers no longer exist (bulk-only module).

}
