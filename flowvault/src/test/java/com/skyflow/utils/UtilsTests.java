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
import com.skyflow.vault.data.BulkDetokenizeRequest;
import com.skyflow.vault.data.BulkDetokenizeResponse;
import com.skyflow.vault.data.BulkDetokenizeResponseRecord;
import com.skyflow.vault.data.BulkInsertRequestRecord;
import com.skyflow.vault.data.BulkInsertRequest;
import com.skyflow.vault.data.BulkInsertResponse;
import com.skyflow.vault.data.BulkInsertResponseRecord;
import com.skyflow.vault.data.BulkTokenizeRecord;
import com.skyflow.vault.data.BulkTokenizeRequest;
import com.skyflow.vault.data.BulkTokenizeResponse;
import com.skyflow.vault.data.ErrorRecord;
import com.skyflow.vault.data.InsertRequestRecord;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.TokenGroupRedactions;
import com.skyflow.vault.data.UpsertOptions;
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
    public void testGetInsertRequestBody_withTokens() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "tok-abc");
        InsertRequestRecord record = InsertRequestRecord.builder().tableName("table1").data(data).tokens(tokens).build();
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(record);
        InsertRequest request = InsertRequest.builder().records(records).build();
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");

        V1InsertRequest body = Utils.getInsertRequestBody(request, config);

        Assert.assertEquals(tokens, body.getRecords().get().get(0).getTokens().get());
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

    // Tests for getQueryRequestBody / buildQueryResponse / getGetRequestBody / buildGetResponse
    // were removed: get and query Utils helpers no longer exist (bulk-only module).

}
