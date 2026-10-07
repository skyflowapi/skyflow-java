package com.skyflow.utils;

import com.skyflow.generated.rest.resources.query.requests.ExecuteQueryRequest;
import com.skyflow.generated.rest.resources.tokens.requests.DeleteTokenRequest;
import com.skyflow.generated.rest.resources.tokens.requests.GetTokensFromValuesRequest;
import com.skyflow.generated.rest.types.DeleteResponseObject;
import com.skyflow.generated.rest.types.DeleteTokenResponse;
import com.skyflow.generated.rest.types.DeleteTokenResponseObject;
import com.skyflow.generated.rest.types.DetokenizeResponseObject;
import com.skyflow.generated.rest.types.ExecuteQueryRecordResponse;
import com.skyflow.generated.rest.types.ExecuteQueryResponse;
import com.skyflow.generated.rest.types.ExecuteQueryResponseMetadata;
import com.skyflow.generated.rest.types.GetTokensFromValuesRequestObject;
import com.skyflow.generated.rest.types.GetTokensFromValuesResponse;
import com.skyflow.generated.rest.types.InsertRecordData;
import com.skyflow.generated.rest.types.RecordResponseObject;
import com.skyflow.generated.rest.types.TokenizeResponseObject;
import com.google.gson.JsonObject;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.UpdateType;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.core.ApiClientApiException;
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
import com.skyflow.vault.data.ColumnRedactions;
import com.skyflow.vault.data.DeleteRequest;
import com.skyflow.vault.data.DeleteResponse;
import com.skyflow.vault.data.DeleteResponseRecord;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.DetokenizeResponse;
import com.skyflow.vault.data.DetokenizeResponseRecord;
import com.skyflow.vault.data.ErrorRecord;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetRequestRecord;
import com.skyflow.vault.data.GetResponse;
import com.skyflow.vault.data.GetResponseRecord;
import com.skyflow.vault.data.GetTokensRequest;
import com.skyflow.vault.data.GetTokensRequestRecord;
import com.skyflow.vault.data.GetTokensResponse;
import com.skyflow.vault.data.InsertRequestRecord;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;
import com.skyflow.vault.data.InsertResponseRecord;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.QueryResponse;
import com.skyflow.vault.data.TokenGroupRedactions;
import com.skyflow.vault.data.BulkTokenizeResponseRecord;
import com.skyflow.vault.data.TokenizeRequestRecord;
import com.skyflow.vault.data.TokenizeRequest;
import com.skyflow.vault.data.TokenizeResponse;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateRequestRecord;
import com.skyflow.vault.data.UpdateResponse;
import com.skyflow.vault.data.UpsertOptions;
import org.junit.After;
import com.skyflow.generated.rest.types.UpsertUpdateType;
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
import java.util.LinkedHashMap;
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

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getInsertRequestBody(request, config);

        Assert.assertEquals("vault123", body.getVaultId());
        Assert.assertEquals(1, body.getRecords().size());
        Assert.assertEquals("table1", body.getRecords().get(0).getTableName().get());
        Assert.assertEquals(data, body.getRecords().get(0).getData());
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

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getInsertRequestBody(request, config);

        Assert.assertEquals("table1", body.getTableName());
        Assert.assertFalse(body.getRecords().get(0).getTableName().isPresent());
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

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getInsertRequestBody(request, config);

        Assert.assertEquals(tokens, body.getRecords().get(0).getAdditionalProperties().get("tokens"));
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

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getInsertRequestBody(request, config);

        // Request-level upsert stays on the envelope; it is not copied onto the records.
        Assert.assertFalse(body.getRecords().get(0).getUpsert().isPresent());
        Assert.assertTrue(body.getUpsert().isPresent());
        Assert.assertEquals(Collections.singletonList("email"), body.getUpsert().get().getUniqueColumns());
        Assert.assertEquals(UpsertUpdateType.UPDATE, body.getUpsert().get().getUpdateType().get());
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

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getInsertRequestBody(request, config);

        Assert.assertEquals("table1", body.getRecords().get(0).getTableName().get());
        Assert.assertTrue(body.getRecords().get(0).getUpsert().isPresent());
        Assert.assertEquals(UpsertUpdateType.REPLACE, body.getRecords().get(0).getUpsert().get().getUpdateType().get());
    }

    // ── formatInsertResponse (unary) ──────────────────────────────────────────

    @Test
    public void testFormatInsertResponse_successRecord() {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "tok-abc");
        RecordResponseObject record = RecordResponseObject.builder().httpCode(200)
                .tableName("table1").skyflowId("sky-id-1").tokens(tokens).build();
        com.skyflow.generated.rest.types.InsertResponse response = com.skyflow.generated.rest.types.InsertResponse.builder().records(Collections.singletonList(record)).build();

        InsertResponse formatted = Utils.formatInsertResponse(response, new HashMap<>());

        Assert.assertEquals(1, formatted.getRecords().size());
        Assert.assertEquals("table1", formatted.getRecords().get(0).getTableName());
        Assert.assertEquals("sky-id-1", formatted.getRecords().get(0).getSkyflowId());
        Assert.assertEquals(200, formatted.getRecords().get(0).getHttpCode());
        Assert.assertNull(formatted.getRecords().get(0).getError());
    }

    @Test
    public void testFormatInsertResponse_errorRecordDefaultsHttpCode500() {
        RecordResponseObject record = RecordResponseObject.builder().httpCode(500).error("failed").build();
        com.skyflow.generated.rest.types.InsertResponse response = com.skyflow.generated.rest.types.InsertResponse.builder().records(Collections.singletonList(record)).build();

        InsertResponse formatted = Utils.formatInsertResponse(response, new HashMap<>());

        Assert.assertEquals("failed", formatted.getRecords().get(0).getError());
        Assert.assertEquals(500, formatted.getRecords().get(0).getHttpCode());
    }

    @Test
    public void testFormatInsertResponse_nullResponseReturnsEmptyRecords() {
        InsertResponse formatted = Utils.formatInsertResponse(null, new HashMap<>());
        Assert.assertTrue(formatted.getRecords().isEmpty());
    }

    @Test
    public void testFormatInsertResponse_requestIdOnlyPopulatedOnError() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(Constants.REQUEST_ID_HEADER_KEY, Collections.singletonList("req-insert-1"));
        RecordResponseObject success = RecordResponseObject.builder().httpCode(200).skyflowId("sky-id-1").build();
        RecordResponseObject failure = RecordResponseObject.builder().httpCode(500).error("failed").build();
        com.skyflow.generated.rest.types.InsertResponse response = com.skyflow.generated.rest.types.InsertResponse.builder().records(Arrays.asList(success, failure)).build();

        InsertResponse formatted = Utils.formatInsertResponse(response, headers);

        Assert.assertNull(formatted.getRecords().get(0).getRequestId());
        Assert.assertEquals("req-insert-1", formatted.getRecords().get(1).getRequestId());
    }

    // ── getDetokenizeRequestBody / formatDetokenizeResponse (unary) ───────────

    @Test
    public void testGetDetokenizeRequestBody_buildsCorrectRequest() {
        List<String> tokens = Collections.singletonList("tok-1");
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();

        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest body = Utils.getDetokenizeRequestBody(request, "vault123");

        Assert.assertEquals("vault123", body.getVaultId());
        Assert.assertEquals(tokens, body.getTokens());
        Assert.assertFalse(body.getTokenGroupRedactions().isPresent());
    }

    @Test
    public void testGetDetokenizeRequestBody_withTokenGroupRedactions() {
        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(Collections.singletonList("tok-1"))
                .tokenGroupRedactions(Collections.singletonList(
                        TokenGroupRedactions.builder().tokenGroupName("group1").redaction("MASKED").build()))
                .build();

        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest body = Utils.getDetokenizeRequestBody(request, "vault123");

        Assert.assertEquals(1, body.getTokenGroupRedactions().get().size());
        Assert.assertEquals("group1", body.getTokenGroupRedactions().get().get(0).getTokenGroupName().get());
        Assert.assertEquals("MASKED", body.getTokenGroupRedactions().get().get(0).getRedaction().get());
    }

    @Test
    public void testFormatDetokenizeResponse_successRecord() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("skyflowId", "sky-1");
        metadata.put("tableName", "table1");
        DetokenizeResponseObject record = DetokenizeResponseObject.builder()
                .token("tok-1").value(com.skyflow.generated.rest.types.GoogleProtobufValue.of("john@example.com")).tokenGroupName("group1").metadata(metadata).build();
        com.skyflow.generated.rest.types.DetokenizeResponse response = com.skyflow.generated.rest.types.DetokenizeResponse.builder()
                .response(Collections.singletonList(record)).build();

        DetokenizeResponse formatted = Utils.formatDetokenizeResponse(response, new HashMap<>());

        Assert.assertEquals(1, formatted.getRecords().size());
        Assert.assertEquals("tok-1", formatted.getRecords().get(0).getToken());
        Assert.assertEquals("john@example.com", formatted.getRecords().get(0).getValue());
        Assert.assertEquals("sky-1", formatted.getRecords().get(0).getMetadata().getSkyflowId());
        Assert.assertEquals("table1", formatted.getRecords().get(0).getMetadata().getTableName());
        Assert.assertEquals(200, formatted.getRecords().get(0).getHttpCode());
    }

    @Test
    public void testFormatDetokenizeResponse_nullResponseReturnsEmptyRecords() {
        DetokenizeResponse formatted = Utils.formatDetokenizeResponse(null, new HashMap<>());
        Assert.assertTrue(formatted.getRecords().isEmpty());
    }

    @Test
    public void testFormatDetokenizeResponse_requestIdOnlyPopulatedOnError() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(Constants.REQUEST_ID_HEADER_KEY, Collections.singletonList("req-detok-1"));
        DetokenizeResponseObject success = DetokenizeResponseObject.builder().token("tok-1").build();
        DetokenizeResponseObject failure = DetokenizeResponseObject.builder()
                .token("tok-2").error("failed").build();
        com.skyflow.generated.rest.types.DetokenizeResponse response = com.skyflow.generated.rest.types.DetokenizeResponse.builder()
                .response(Arrays.asList(success, failure)).build();

        DetokenizeResponse formatted = Utils.formatDetokenizeResponse(response, headers);

        Assert.assertNull(formatted.getRecords().get(0).getRequestId());
        Assert.assertEquals("req-detok-1", formatted.getRecords().get(1).getRequestId());
    }

    // ── getUpdateRequestBody / formatUpdateResponse ───────────────────────────

    @Test
    public void testGetUpdateRequestBody_buildsCorrectRequest() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "jane");
        UpdateRequestRecord record = UpdateRequestRecord.builder().skyflowId("sky-1").data(data).build();
        UpdateRequest request = UpdateRequest.builder().tableName("table1").records(Collections.singletonList(record)).build();
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");

        com.skyflow.generated.rest.resources.records.requests.UpdateRequest body = Utils.getUpdateRequestBody(request, config);

        Assert.assertEquals("vault123", body.getVaultId());
        Assert.assertEquals("table1", body.getTableName());
        Assert.assertEquals("sky-1", body.getRecords().get(0).getSkyflowId());
        Assert.assertEquals(data, body.getRecords().get(0).getData());
        Assert.assertFalse(body.getUpdateType().isPresent());
    }

    @Test
    public void testGetUpdateRequestBody_withRequestLevelUpdateTypeAndRecordTableName() {
        UpdateRequestRecord record = UpdateRequestRecord.builder()
                .skyflowId("sky-1").data(new HashMap<>()).tableName("table2").build();
        UpdateRequest request = UpdateRequest.builder()
                .tableName("table1").records(Collections.singletonList(record)).updateType(UpdateType.REPLACE).build();
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");

        com.skyflow.generated.rest.resources.records.requests.UpdateRequest body = Utils.getUpdateRequestBody(request, config);

        Assert.assertEquals(com.skyflow.generated.rest.resources.records.types.UpdateRequestUpdateType.REPLACE, body.getUpdateType().get());
        Assert.assertEquals("table2", body.getRecords().get(0).getTableName().get());
    }

    @Test
    public void testGetUpdateRequestBody_withTokens() {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "tok-abc");
        UpdateRequestRecord record = UpdateRequestRecord.builder()
                .skyflowId("sky-1").data(new HashMap<>()).tokens(tokens).build();
        UpdateRequest request = UpdateRequest.builder().tableName("table1").records(Collections.singletonList(record)).build();
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");

        com.skyflow.generated.rest.resources.records.requests.UpdateRequest body = Utils.getUpdateRequestBody(request, config);

        Assert.assertEquals(tokens, body.getRecords().get(0).getAdditionalProperties().get("tokens"));
    }

    @Test
    public void testFormatUpdateResponse_successRecord() {
        RecordResponseObject record = RecordResponseObject.builder().httpCode(200)
                .tableName("table1").skyflowId("sky-1").build();
        com.skyflow.generated.rest.types.UpdateResponse response = com.skyflow.generated.rest.types.UpdateResponse.builder().records(Collections.singletonList(record)).build();

        UpdateResponse formatted = Utils.formatUpdateResponse(response, new HashMap<>());

        Assert.assertEquals(1, formatted.getRecords().size());
        Assert.assertEquals("table1", formatted.getRecords().get(0).getTableName());
        Assert.assertEquals("sky-1", formatted.getRecords().get(0).getSkyflowId());
        Assert.assertEquals(200, formatted.getRecords().get(0).getHttpCode());
    }

    @Test
    public void testFormatUpdateResponse_nullResponseReturnsEmptyRecords() {
        UpdateResponse formatted = Utils.formatUpdateResponse(null, new HashMap<>());
        Assert.assertTrue(formatted.getRecords().isEmpty());
    }

    @Test
    public void testFormatUpdateResponse_requestIdOnlyPopulatedOnError() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(Constants.REQUEST_ID_HEADER_KEY, Collections.singletonList("req-update-1"));
        RecordResponseObject success = RecordResponseObject.builder().httpCode(200).skyflowId("sky-id-1").build();
        RecordResponseObject failure = RecordResponseObject.builder().httpCode(500).error("failed").build();
        com.skyflow.generated.rest.types.UpdateResponse response = com.skyflow.generated.rest.types.UpdateResponse.builder().records(Arrays.asList(success, failure)).build();

        UpdateResponse formatted = Utils.formatUpdateResponse(response, headers);

        Assert.assertNull(formatted.getRecords().get(0).getRequestId());
        Assert.assertEquals("req-update-1", formatted.getRecords().get(1).getRequestId());
    }

    // ── getGetRequestBody / formatGetResponse ─────────────────────────────────

    @Test
    public void testGetGetRequestBody_singleTableMode() {
        Map<String, Object> uniqueValue = new HashMap<>();
        uniqueValue.put("email", "john@example.com");
        GetRequest request = GetRequest.builder()
                .tableName("table1")
                .skyflowIds(new ArrayList<>(Collections.singletonList("id1")))
                .columns(new ArrayList<>(Collections.singletonList("name")))
                .columnRedactions(Collections.singletonList(
                        ColumnRedactions.builder().columnName("email").redaction("MASKED").build()))
                .limit(10)
                .offset(5)
                .build();
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");

        com.skyflow.generated.rest.resources.records.requests.GetRequest body = Utils.getGetRequestBody(request, config);

        Assert.assertEquals("vault123", body.getVaultId());
        Assert.assertEquals("table1", body.getTableName().get());
        Assert.assertEquals(Collections.singletonList("id1"), body.getSkyflowIDs().get());
        Assert.assertEquals(Collections.singletonList("name"), body.getColumns().get());
        Assert.assertEquals("email", body.getColumnRedactions().get().get(0).getColumnName());
        Assert.assertEquals(Integer.valueOf(10), body.getLimit().get());
        Assert.assertEquals(Integer.valueOf(5), body.getOffset().get());
        Assert.assertFalse(body.getRecords().isPresent());
    }

    @Test
    public void testGetGetRequestBody_multiTableModeIgnoresSingleTableFields() {
        GetRequestRecord nested = GetRequestRecord.builder()
                .tableName("table2").skyflowIds(Collections.singletonList("id2")).build();
        GetRequest request = GetRequest.builder().records(Collections.singletonList(nested)).build();
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");

        com.skyflow.generated.rest.resources.records.requests.GetRequest body = Utils.getGetRequestBody(request, config);

        Assert.assertTrue(body.getRecords().isPresent());
        Assert.assertEquals(1, body.getRecords().get().size());
        Assert.assertEquals("table2", body.getRecords().get().get(0).getTableName());
        Assert.assertEquals(Collections.singletonList("id2"), body.getRecords().get().get(0).getSkyflowIDs());
        Assert.assertFalse(body.getTableName().isPresent());
    }

    @Test
    public void testFormatGetResponse_successRecord() {
        RecordResponseObject record = RecordResponseObject.builder().httpCode(200)
                .tableName("table1").skyflowId("sky-1").build();
        com.skyflow.generated.rest.types.GetResponse response = com.skyflow.generated.rest.types.GetResponse.builder().records(Collections.singletonList(record)).build();

        GetResponse formatted = Utils.formatGetResponse(response, new HashMap<>());

        Assert.assertEquals(1, formatted.getRecords().size());
        Assert.assertEquals("table1", formatted.getRecords().get(0).getTableName());
        Assert.assertEquals("sky-1", formatted.getRecords().get(0).getSkyflowId());
    }

    @Test
    public void testFormatGetResponse_nullResponseReturnsEmptyRecords() {
        GetResponse formatted = Utils.formatGetResponse(null, new HashMap<>());
        Assert.assertTrue(formatted.getRecords().isEmpty());
    }

    @Test
    public void testFormatGetResponse_requestIdOnlyPopulatedOnError() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(Constants.REQUEST_ID_HEADER_KEY, Collections.singletonList("req-get-1"));
        RecordResponseObject success = RecordResponseObject.builder().httpCode(200).skyflowId("sky-id-1").build();
        RecordResponseObject failure = RecordResponseObject.builder().httpCode(500).error("failed").build();
        com.skyflow.generated.rest.types.GetResponse response = com.skyflow.generated.rest.types.GetResponse.builder().records(Arrays.asList(success, failure)).build();

        GetResponse formatted = Utils.formatGetResponse(response, headers);

        Assert.assertNull(formatted.getRecords().get(0).getRequestId());
        Assert.assertEquals("req-get-1", formatted.getRecords().get(1).getRequestId());
    }

    // ── getDeleteRequestBody / formatDeleteResponse ───────────────────────────

    @Test
    public void testGetDeleteRequestBody_withIds() {
        DeleteRequest request = DeleteRequest.builder().tableName("table1").skyflowIds(Collections.singletonList("id1")).build();
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");

        com.skyflow.generated.rest.resources.records.requests.DeleteRequest body = Utils.getDeleteRequestBody(request, config);

        Assert.assertEquals("vault123", body.getVaultId());
        Assert.assertEquals("table1", body.getTableName().get());
        Assert.assertEquals(Collections.singletonList("id1"), body.getSkyflowIDs().get());
        Assert.assertFalse(body.getUniqueValues().isPresent());
    }

    @Test
    public void testGetDeleteRequestBody_withUniqueValues() {
        Map<String, Object> uniqueValue = new HashMap<>();
        uniqueValue.put("email", "john@example.com");
        DeleteRequest request = DeleteRequest.builder()
                .tableName("table1").uniqueValues(Collections.singletonList(uniqueValue)).build();
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");

        com.skyflow.generated.rest.resources.records.requests.DeleteRequest body = Utils.getDeleteRequestBody(request, config);

        Assert.assertEquals(uniqueValue, body.getUniqueValues().get().get(0).getData());
        Assert.assertFalse(body.getSkyflowIDs().isPresent());
    }

    @Test
    public void testFormatDeleteResponse_successAndErrorRecords() {
        DeleteResponseObject success = DeleteResponseObject.builder().skyflowId("sky-1").httpCode(200).build();
        DeleteResponseObject failure = DeleteResponseObject.builder().skyflowId("").httpCode(404).error("not found").build();
        com.skyflow.generated.rest.types.DeleteResponse response = com.skyflow.generated.rest.types.DeleteResponse.builder().records(Arrays.asList(success, failure)).build();

        DeleteResponse formatted = Utils.formatDeleteResponse(response, new HashMap<>());

        Assert.assertEquals(2, formatted.getRecords().size());
        Assert.assertEquals("sky-1", formatted.getRecords().get(0).getSkyflowId());
        Assert.assertEquals(Integer.valueOf(200), formatted.getRecords().get(0).getHttpCode());
        Assert.assertNull(formatted.getRecords().get(0).getError());
        Assert.assertEquals("not found", formatted.getRecords().get(1).getError());
        Assert.assertEquals(Integer.valueOf(404), formatted.getRecords().get(1).getHttpCode());
    }

    @Test
    public void testFormatDeleteResponse_nullResponseReturnsEmptyRecords() {
        DeleteResponse formatted = Utils.formatDeleteResponse(null, new HashMap<>());
        Assert.assertTrue(formatted.getRecords().isEmpty());
    }

    @Test
    public void testFormatDeleteResponse_requestIdOnlyPopulatedOnError() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(Constants.REQUEST_ID_HEADER_KEY, Collections.singletonList("req-delete-1"));
        DeleteResponseObject success = DeleteResponseObject.builder().skyflowId("sky-1").httpCode(200).build();
        DeleteResponseObject failure = DeleteResponseObject.builder().skyflowId("").httpCode(404).error("not found").build();
        com.skyflow.generated.rest.types.DeleteResponse response = com.skyflow.generated.rest.types.DeleteResponse.builder().records(Arrays.asList(success, failure)).build();

        DeleteResponse formatted = Utils.formatDeleteResponse(response, headers);

        Assert.assertNull(formatted.getRecords().get(0).getRequestId());
        Assert.assertEquals("req-delete-1", formatted.getRecords().get(1).getRequestId());
    }

    // ── getQueryRequestBody / formatQueryResponse ─────────────────────────────

    @Test
    public void testGetQueryRequestBody_mapsVaultIdAndQuery() {
        QueryRequest request = QueryRequest.builder().query("SELECT * FROM table1 LIMIT 25 OFFSET 25").build();

        ExecuteQueryRequest body = Utils.getQueryRequestBody(request, "vault123");

        Assert.assertEquals("vault123", body.getVaultId());
        Assert.assertEquals("SELECT * FROM table1 LIMIT 25 OFFSET 25", body.getQuery());
    }

    @Test
    public void testFormatQueryResponse_rowsMetadataAndRequestId() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(Constants.REQUEST_ID_HEADER_KEY, Collections.singletonList("req-query-1"));
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("skyflow_id", "sky-1");
        row1.put("name", "john");
        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("skyflow_id", "sky-2");
        row2.put("name", "jane");
        ExecuteQueryResponse response = ExecuteQueryResponse.builder()
                .records(Arrays.asList(
                        ExecuteQueryRecordResponse.builder().data(row1).build(),
                        ExecuteQueryRecordResponse.builder().data(row2).build()))
                .metadata(ExecuteQueryResponseMetadata.builder().columns(Arrays.asList("skyflow_id", "name")).build())
                .build();

        QueryResponse formatted = Utils.formatQueryResponse(response, headers);

        Assert.assertEquals(2, formatted.getFields().size());
        Assert.assertEquals(row1, formatted.getFields().get(0));
        Assert.assertEquals(row2, formatted.getFields().get(1));
        Assert.assertNull(formatted.getErrors());
        Assert.assertEquals(Arrays.asList("skyflow_id", "name"), formatted.getMetadata().getColumns());
        // call-level: set even though nothing failed
        Assert.assertEquals("req-query-1", formatted.getRequestId());
    }

    @Test
    public void testFormatQueryResponse_preservesColumnOrderOfEachRow() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("zeta", 1);
        row.put("alpha", 2);
        row.put("mid", 3);
        ExecuteQueryResponse response = ExecuteQueryResponse.builder()
                .records(Collections.singletonList(ExecuteQueryRecordResponse.builder().data(row).build()))
                .build();

        QueryResponse formatted = Utils.formatQueryResponse(response, new HashMap<>());

        Assert.assertEquals(Arrays.asList("zeta", "alpha", "mid"),
                new ArrayList<>(formatted.getFields().get(0).keySet()));
    }

    @Test
    public void testFormatQueryResponse_recordWithoutDataBecomesEmptyRow() {
        ExecuteQueryResponse response = ExecuteQueryResponse.builder()
                .records(Collections.singletonList(ExecuteQueryRecordResponse.builder().build()))
                .build();

        QueryResponse formatted = Utils.formatQueryResponse(response, new HashMap<>());

        Assert.assertEquals(1, formatted.getFields().size());
        Assert.assertTrue(formatted.getFields().get(0).isEmpty());
    }

    @Test
    public void testFormatQueryResponse_nullResponseReturnsEmptyFieldsAndNonNullMetadata() {
        QueryResponse formatted = Utils.formatQueryResponse(null, null);

        Assert.assertTrue(formatted.getFields().isEmpty());
        Assert.assertNull(formatted.getErrors());
        Assert.assertNotNull(formatted.getMetadata());
        Assert.assertNull(formatted.getMetadata().getColumns());
        Assert.assertNull(formatted.getRequestId());
    }

    @Test
    public void testFormatQueryResponse_metadataWithoutColumns() {
        ExecuteQueryResponse response = ExecuteQueryResponse.builder()
                .metadata(ExecuteQueryResponseMetadata.builder().build())
                .build();

        QueryResponse formatted = Utils.formatQueryResponse(response, new HashMap<>());

        Assert.assertTrue(formatted.getFields().isEmpty());
        Assert.assertNull(formatted.getMetadata().getColumns());
    }

    // ── getGetTokensRequestBody / formatGetTokensResponse ─────────────────────

    @Test
    public void testGetGetTokensRequestBody_mapsEveryRecordInOrder() {
        GetTokensRequest request = GetTokensRequest.builder()
                .records(Arrays.asList(
                        GetTokensRequestRecord.builder().value("john@example.com").tokenGroupName("det_email").build(),
                        GetTokensRequestRecord.builder().value(42).tokenGroupName("det_number").build()))
                .build();

        GetTokensFromValuesRequest body = Utils.getGetTokensRequestBody(request, "vault123");

        Assert.assertEquals("vault123", body.getVaultId());
        List<GetTokensFromValuesRequestObject> records = body.getRecords();
        Assert.assertEquals(2, records.size());
        Assert.assertEquals("john@example.com", records.get(0).getValue().get());
        Assert.assertEquals("det_email", records.get(0).getTokenGroupName());
        Assert.assertEquals(42, records.get(1).getValue().get());
        Assert.assertEquals("det_number", records.get(1).getTokenGroupName());
    }

    @Test
    public void testFormatGetTokensResponse_successAndErrorRecords() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(Constants.REQUEST_ID_HEADER_KEY, Collections.singletonList("req-gettokens-1"));
        TokenizeResponseObject success = TokenizeResponseObject.builder().token("tok-1")
                .value(com.skyflow.generated.rest.types.GoogleProtobufValue.of("john@example.com")).tokenGroupName("det_group").httpCode(200).build();
        TokenizeResponseObject failure = TokenizeResponseObject.builder().token("")
                .value(com.skyflow.generated.rest.types.GoogleProtobufValue.of("unknown@example.com")).tokenGroupName("det_group").error("Token not found.").httpCode(404).build();
        GetTokensFromValuesResponse response = GetTokensFromValuesResponse.builder()
                .records(Arrays.asList(success, failure)).build();

        GetTokensResponse formatted = Utils.formatGetTokensResponse(response, headers);

        Assert.assertEquals(2, formatted.getRecords().size());
        Map<String, Object> first = formatted.getRecords().get(0);
        Assert.assertEquals("john@example.com", first.get("value"));
        Assert.assertEquals("det_group", first.get("tokenGroupName"));
        Assert.assertEquals("tok-1", first.get("token"));
        Assert.assertEquals(200, first.get("httpCode"));
        Assert.assertNull(first.get("error"));
        Assert.assertNull(first.get("requestId"));
        Map<String, Object> second = formatted.getRecords().get(1);
        Assert.assertEquals("unknown@example.com", second.get("value"));
        Assert.assertNull(second.get("token"));
        Assert.assertEquals(404, second.get("httpCode"));
        Assert.assertEquals("Token not found.", second.get("error"));
        Assert.assertEquals("req-gettokens-1", second.get("requestId"));
    }

    @Test
    public void testFormatGetTokensResponse_recordMapKeysInDocumentedOrder() {
        TokenizeResponseObject record = TokenizeResponseObject.builder().token("t")
                .value(com.skyflow.generated.rest.types.GoogleProtobufValue.of("v")).tokenGroupName("g").build();
        GetTokensFromValuesResponse response = GetTokensFromValuesResponse.builder()
                .records(Collections.singletonList(record)).build();

        GetTokensResponse formatted = Utils.formatGetTokensResponse(response, new HashMap<>());

        Assert.assertEquals(Arrays.asList("value", "tokenGroupName", "token", "httpCode", "error", "requestId"),
                new ArrayList<>(formatted.getRecords().get(0).keySet()));
    }

    @Test
    public void testFormatGetTokensResponse_emptyStringsNormalisedToNull() {
        // the API sends "" for a token or error that does not apply
        TokenizeResponseObject record = TokenizeResponseObject.builder().token("")
                .value(com.skyflow.generated.rest.types.GoogleProtobufValue.of("v")).tokenGroupName("g").error("").httpCode(200).build();
        GetTokensFromValuesResponse response = GetTokensFromValuesResponse.builder()
                .records(Collections.singletonList(record)).build();

        GetTokensResponse formatted = Utils.formatGetTokensResponse(response, new HashMap<>());

        Assert.assertNull(formatted.getRecords().get(0).get("token"));
        Assert.assertNull(formatted.getRecords().get(0).get("error"));
        Assert.assertNull(formatted.getRecords().get(0).get("requestId"));
    }

    @Test
    public void testFormatGetTokensResponse_missingHttpCodeDefaultsByOutcome() {
        TokenizeResponseObject success = TokenizeResponseObject.builder().token("tok-a").value(com.skyflow.generated.rest.types.GoogleProtobufValue.of("a")).tokenGroupName("").build();
        TokenizeResponseObject failure = TokenizeResponseObject.builder().token("").value(com.skyflow.generated.rest.types.GoogleProtobufValue.of("b")).tokenGroupName("").error("boom").build();
        GetTokensFromValuesResponse response = GetTokensFromValuesResponse.builder()
                .records(Arrays.asList(success, failure)).build();

        GetTokensResponse formatted = Utils.formatGetTokensResponse(response, new HashMap<>());

        Assert.assertEquals(200, formatted.getRecords().get(0).get("httpCode"));
        Assert.assertEquals(500, formatted.getRecords().get(1).get("httpCode"));
    }

    @Test
    public void testFormatGetTokensResponse_nonStringValueEchoedAsIs() {
        TokenizeResponseObject record = TokenizeResponseObject.builder().token("tok-42")
                .value(com.skyflow.generated.rest.types.GoogleProtobufValue.of(42)).tokenGroupName("det_number").build();
        GetTokensFromValuesResponse response = GetTokensFromValuesResponse.builder()
                .records(Collections.singletonList(record)).build();

        GetTokensResponse formatted = Utils.formatGetTokensResponse(response, new HashMap<>());

        Assert.assertEquals(42, formatted.getRecords().get(0).get("value"));
    }

    @Test
    public void testFormatGetTokensResponse_nullResponseReturnsEmptyRecords() {
        GetTokensResponse formatted = Utils.formatGetTokensResponse(null, new HashMap<>());
        Assert.assertTrue(formatted.getRecords().isEmpty());
    }

    @Test
    public void testFormatGetTokensResponse_absentRecordsReturnsEmptyRecords() {
        GetTokensResponse formatted = Utils.formatGetTokensResponse(
                GetTokensFromValuesResponse.builder().build(), new HashMap<>());
        Assert.assertTrue(formatted.getRecords().isEmpty());
    }

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

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, config);

        Assert.assertEquals("vault123", body.getVaultId());
        Assert.assertEquals("table1", body.getTableName());
        Assert.assertEquals(1, body.getRecords().size());
        Assert.assertFalse(body.getRecords().get(0).getTableName().isPresent());
        Assert.assertEquals(data, body.getRecords().get(0).getData());
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

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, config);

        // Request-level upsert stays on the envelope; it is not copied onto the records.
        Assert.assertFalse(body.getRecords().get(0).getUpsert().isPresent());
        Assert.assertTrue(body.getUpsert().isPresent());
        Assert.assertEquals(Collections.singletonList("email"), body.getUpsert().get().getUniqueColumns());
        Assert.assertEquals(UpsertUpdateType.UPDATE, body.getUpsert().get().getUpdateType().get());
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

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, config);

        Assert.assertEquals("table1", body.getRecords().get(0).getTableName().get());
        Assert.assertTrue(body.getRecords().get(0).getUpsert().isPresent());
        Assert.assertEquals(UpsertUpdateType.REPLACE, body.getRecords().get(0).getUpsert().get().getUpdateType().get());
    }

    // ── getBulkDetokenizeRequestBody ──────────────────────────────────────────

    @Test
    public void testGetBulkDetokenizeRequestBody_buildsCorrectRequest() {
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder()
                .tokens(Arrays.asList("token1", "token2"))
                .build();

        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest body = Utils.getBulkDetokenizeRequestBody(request, "vault123");

        Assert.assertEquals("vault123", body.getVaultId());
        Assert.assertEquals(Arrays.asList("token1", "token2"), body.getTokens());
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

        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest body = Utils.getBulkDetokenizeRequestBody(request, "vault123");

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

        DeleteTokenRequest body = Utils.getBulkDeleteTokensRequestBody(request, "vault123");

        Assert.assertEquals("vault123", body.getVaultId());
        Assert.assertEquals(Arrays.asList("token1", "token2"), body.getTokens());
    }

    // ── getBulkTokenizeRequestBody ─────────────────────────────────────────────

    @Test
    public void testGetBulkTokenizeRequestBody_buildsCorrectRequest() {
        List<BulkTokenizeRequestRecord> records = Collections.singletonList(
                BulkTokenizeRequestRecord.builder()
                        .value("value1")
                        .tokenGroupNames(Collections.singletonList("group1"))
                        .build());

        com.skyflow.generated.rest.resources.tokens.requests.TokenizeRequest body = Utils.getBulkTokenizeRequestBody(records, "vault123");

        Assert.assertEquals("vault123", body.getVaultId());
        Assert.assertEquals(1, body.getData().size());
        Assert.assertEquals("value1", body.getData().get(0).getValue().get());
        Assert.assertEquals(Collections.singletonList("group1"), body.getData().get(0).getTokenGroupNames());
        Assert.assertFalse(body.getData().get(0).getToken().isPresent());
    }

    @Test
    public void testGetBulkTokenizeRequestBody_carriesByotToken() {
        List<BulkTokenizeRequestRecord> records = Collections.singletonList(
                BulkTokenizeRequestRecord.builder()
                        .value("value1")
                        .token("my-own-token")
                        .tokenGroupNames(Collections.singletonList("group1"))
                        .build());

        com.skyflow.generated.rest.resources.tokens.requests.TokenizeRequest body = Utils.getBulkTokenizeRequestBody(records, "vault123");

        Assert.assertEquals("my-own-token", body.getData().get(0).getToken().get().get());
    }

    // ── createBulkInsertBatches ────────────────────────────────────────────────

    @Test
    public void testCreateBulkInsertBatches_splitsEvenly() {
        List<InsertRecordData> records = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            records.add(InsertRecordData.builder().data(new HashMap<>()).build());
        }

        List<List<InsertRecordData>> batches = Utils.createBulkInsertBatches(records, 2);

        Assert.assertEquals(2, batches.size());
        Assert.assertEquals(2, batches.get(0).size());
        Assert.assertEquals(2, batches.get(1).size());
    }

    @Test
    public void testCreateBulkInsertBatches_splitsWithRemainder() {
        List<InsertRecordData> records = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            records.add(InsertRecordData.builder().data(new HashMap<>()).build());
        }

        List<List<InsertRecordData>> batches = Utils.createBulkInsertBatches(records, 2);

        Assert.assertEquals(3, batches.size());
        Assert.assertEquals(1, batches.get(2).size());
    }

    // ── createBulkDetokenizeBatches ────────────────────────────────────────────

    @Test
    public void testCreateBulkDetokenizeBatches_splitsTokens() {
        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest request = com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest.builder()
                .vaultId("vault123")
                .tokens(Arrays.asList("t1", "t2", "t3"))
                .build();

        List<com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest> batches = Utils.createBulkDetokenizeBatches(request, 2);

        Assert.assertEquals(2, batches.size());
        Assert.assertEquals(Arrays.asList("t1", "t2"), batches.get(0).getTokens());
        Assert.assertEquals(Collections.singletonList("t3"), batches.get(1).getTokens());
        Assert.assertEquals("vault123", batches.get(0).getVaultId());
    }

    @Test
    public void testCreateBulkDetokenizeBatches_carriesTokenGroupRedactions() {
        com.skyflow.generated.rest.types.TokenGroupRedactions redaction =
                com.skyflow.generated.rest.types.TokenGroupRedactions.builder()
                        .tokenGroupName("group1")
                        .redaction("MASKED")
                        .build();
        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest request = com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest.builder()
                .vaultId("vault123")
                .tokens(Arrays.asList("t1", "t2"))
                .tokenGroupRedactions(Collections.singletonList(redaction))
                .build();

        List<com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest> batches = Utils.createBulkDetokenizeBatches(request, 5);

        Assert.assertEquals(1, batches.size());
        Assert.assertTrue(batches.get(0).getTokenGroupRedactions().isPresent());
        Assert.assertEquals("group1", batches.get(0).getTokenGroupRedactions().get().get(0).getTokenGroupName().get());
    }

    // ── createBulkDeleteTokensBatches ──────────────────────────────────────────

    @Test
    public void testCreateBulkDeleteTokensBatches_splitsTokens() {
        DeleteTokenRequest request = DeleteTokenRequest.builder()
                .vaultId("vault123")
                .tokens(Arrays.asList("t1", "t2", "t3"))
                .build();

        List<DeleteTokenRequest> batches = Utils.createBulkDeleteTokensBatches(request, 2);

        Assert.assertEquals(2, batches.size());
        Assert.assertEquals(Arrays.asList("t1", "t2"), batches.get(0).getTokens());
        Assert.assertEquals(Collections.singletonList("t3"), batches.get(1).getTokens());
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

        List<InsertRecordData> batch = Collections.singletonList(InsertRecordData.builder().data(new HashMap<>()).build());
        List<BulkInsertResponseRecord> errors = Utils.handleBulkInsertBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(409, errors.get(0).getHttpCode());
        Assert.assertEquals("duplicate", errors.get(0).getError());
    }

    @Test
    public void testHandleBulkInsertBatchException_apiExceptionWithNoParsableBody() {
        ApiClientApiException apiEx = new ApiClientApiException("insert failed", 401, "unauthorized");
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<InsertRecordData> batch = Arrays.asList(
                InsertRecordData.builder().data(new HashMap<>()).build(),
                InsertRecordData.builder().data(new HashMap<>()).build());
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

        List<InsertRecordData> batch = Collections.singletonList(InsertRecordData.builder().data(new HashMap<>()).build());
        List<BulkInsertResponseRecord> errors = Utils.handleBulkInsertBatchException(wrapper, batch, 0, 50);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(401, errors.get(0).getHttpCode());
        Assert.assertEquals("top level auth error", errors.get(0).getError());
    }

    @Test
    public void testHandleBulkInsertBatchException_genericException() {
        RuntimeException ex = new RuntimeException("boom");
        List<InsertRecordData> batch = Collections.singletonList(InsertRecordData.builder().data(new HashMap<>()).build());

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

        List<InsertRecordData> batch = Collections.singletonList(
                InsertRecordData.builder().data(new HashMap<>()).build());
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

        List<InsertRecordData> batch = Collections.singletonList(
                InsertRecordData.builder().data(new HashMap<>()).build());
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

        List<InsertRecordData> batch = Arrays.asList(
                InsertRecordData.builder().data(new HashMap<>()).build(),
                InsertRecordData.builder().data(new HashMap<>()).build());
        List<BulkInsertResponseRecord> records = Utils.handleBulkInsertBatchException(wrapper, batch, 2, 50);

        Assert.assertEquals(100, records.get(0).getIndex());
        Assert.assertEquals(101, records.get(1).getIndex());
    }

    @Test
    public void testHandleBulkInsertBatchException_genericExceptionHasNoRequestId() {
        // A non-API failure has no response headers to read a request id from.
        RuntimeException ex = new RuntimeException("boom");
        List<InsertRecordData> batch = Collections.singletonList(
                InsertRecordData.builder().data(new HashMap<>()).build());

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
        List<InsertRecordData> batch = Collections.singletonList(
                InsertRecordData.builder().data(new HashMap<>()).build());

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
        List<InsertRecordData> batch = Collections.singletonList(
                InsertRecordData.builder().data(new HashMap<>()).build());

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
        // generated RecordResponseObject — even though the SDK exposes it as getSkyflowId().
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

        List<InsertRecordData> batch = Arrays.asList(
                InsertRecordData.builder().data(new HashMap<>()).build(),
                InsertRecordData.builder().data(new HashMap<>()).build());
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

        List<InsertRecordData> batch = Collections.singletonList(
                InsertRecordData.builder().data(new HashMap<>()).build());
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

        List<InsertRecordData> batch = Collections.singletonList(
                InsertRecordData.builder().data(new HashMap<>()).build());
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

        List<InsertRecordData> batch = Collections.singletonList(
                InsertRecordData.builder().data(new HashMap<>()).build());
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

        List<InsertRecordData> batch = Collections.singletonList(
                InsertRecordData.builder().data(new HashMap<>()).build());
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

        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest batch = com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest.builder()
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

        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest batch = com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest.builder()
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

        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest batch = com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest.builder()
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

        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest batch = com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest.builder()
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

        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest batch = com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest.builder()
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

        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest batch = com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest.builder()
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

        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest batch = com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest.builder()
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

        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest batch = com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest.builder()
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

        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest batch = com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest.builder().vaultId("vault123").build();
        List<BulkDetokenizeResponseRecord> records = Utils.handleBulkDetokenizeBatchException(wrapper, batch, 0, 50);

        Assert.assertTrue(records.isEmpty());
    }

    @Test
    public void testHandleBulkDetokenizeBatchException_bodyWithNeitherResponseNorErrorKey() {
        Map<String, Object> body = new HashMap<>();
        body.put("unexpected", "shape");
        ApiClientApiException apiEx = new ApiClientApiException("detokenize failed", 503, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest batch = com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest.builder()
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

    // The wire record lists are required now, so a body without them reads as an empty list.
    @Test
    public void testFormatBulkResponses_absentRecordsYieldsNoRecords() {
        Assert.assertTrue(Utils.formatBulkInsertResponse(
                com.skyflow.generated.rest.types.InsertResponse.builder().build(), 0, 50, null).getRecords().isEmpty());
        Assert.assertTrue(Utils.formatBulkDetokenizeResponse(
                com.skyflow.generated.rest.types.DetokenizeResponse.builder().build(), 0, 50, null).getRecords().isEmpty());
    }

    @Test
    public void testHandleBulkDetokenizeBatchException_genericExceptionHasNoRequestId() {
        RuntimeException ex = new RuntimeException("boom");
        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest batch = com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest.builder()
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
        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest batch = com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest.builder()
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
        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest batch = com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest.builder()
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

        DeleteTokenRequest batch = DeleteTokenRequest.builder()
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

        DeleteTokenRequest batch = DeleteTokenRequest.builder()
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
        DeleteTokenRequest batch = DeleteTokenRequest.builder()
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

        DeleteTokenRequest batch = DeleteTokenRequest.builder()
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

        DeleteTokenRequest batch = DeleteTokenRequest.builder()
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

        DeleteTokenRequest batch = DeleteTokenRequest.builder()
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

        DeleteTokenRequest batch = DeleteTokenRequest.builder()
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

        DeleteTokenRequest batch = DeleteTokenRequest.builder()
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

        DeleteTokenRequest batch = DeleteTokenRequest.builder()
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

        DeleteTokenRequest batch = DeleteTokenRequest.builder()
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

        // index is derived from the batch position; the value is echoed from the request
        // one failed record per requested group
        Assert.assertEquals(2, errors.size());
        Assert.assertEquals(0, errors.get(0).getIndex());
        Assert.assertEquals(0, errors.get(1).getIndex());
        Assert.assertEquals("v1", errors.get(0).getValue());
        Assert.assertEquals("group1", errors.get(0).getTokenGroupName());
        Assert.assertEquals("invalid value", errors.get(0).getError());
        Assert.assertEquals(Integer.valueOf(400), errors.get(0).getHttpCode());
        Assert.assertEquals("group2", errors.get(1).getTokenGroupName());
        Assert.assertEquals("invalid value", errors.get(1).getError());
    }

    @Test
    public void testHandleBulkTokenizeBatchException_genericException() {
        RuntimeException ex = new RuntimeException("boom");

        // this batch starts at index 3 in the caller's list
        List<BulkTokenizeResponseRecord> errors = Utils.handleBulkTokenizeBatchException(
                ex, tokenizeBatch("v1", "group1"), 3);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(3, errors.get(0).getIndex());
        Assert.assertEquals(Integer.valueOf(500), errors.get(0).getHttpCode());
        Assert.assertEquals("boom", errors.get(0).getError());
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

        Assert.assertEquals(1, errors.size());
        Assert.assertNull(errors.get(0).getTokenGroupName());
        Assert.assertEquals("boom", errors.get(0).getError());
    }

    @Test
    public void testHandleBulkTokenizeBatchException_errorBodyWithResponseArrayRebuildsRecords() {
        // A 4xx whose body echoes the per-row "response" array is rebuilt via tokenizeRecordsFromErrorBody
        // rather than summarized by the bare status code.
        Map<String, Object> responseRow = new HashMap<>();
        responseRow.put("value", "v1");
        responseRow.put("tokenGroupName", "group1");
        responseRow.put("error", "BYOT token should contain one token group");
        responseRow.put("httpCode", 400);
        Map<String, Object> body = new HashMap<>();
        body.put("response", Collections.singletonList(responseRow));
        ApiClientApiException apiEx = new ApiClientApiException("tokenize failed", 400, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<BulkTokenizeResponseRecord> errors = Utils.handleBulkTokenizeBatchException(
                wrapper, tokenizeBatch("v1", "group1"), 0);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("v1", errors.get(0).getValue());
        Assert.assertEquals("group1", errors.get(0).getTokenGroupName());
        Assert.assertEquals("BYOT token should contain one token group", errors.get(0).getError());
        Assert.assertEquals(Integer.valueOf(400), errors.get(0).getHttpCode());
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
        Assert.assertEquals("vault not found", errors.get(0).getError());
        Assert.assertEquals(Integer.valueOf(404), errors.get(0).getHttpCode());
    }

    @Test
    public void testHandleBulkTokenizeBatchException_errorFieldAsObjectPrefersNestedErrorOverMessage() {
        // extractBatchErrorMessage prefers a nested "error" key over "message" when both are present
        Map<String, Object> errorObject = new HashMap<>();
        errorObject.put("error", "nested error message");
        errorObject.put("message", "vault not found");
        Map<String, Object> body = new HashMap<>();
        body.put("error", errorObject);
        ApiClientApiException apiEx = new ApiClientApiException("tokenize failed", 404, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<BulkTokenizeResponseRecord> errors = Utils.handleBulkTokenizeBatchException(
                wrapper, tokenizeBatch("v1", "group1"), 0);

        Assert.assertEquals("nested error message", errors.get(0).getError());
    }

    @Test
    public void testHandleBulkTokenizeBatchException_errorFieldAsObjectWithoutAStringFallsBackToApiMessage() {
        // neither "error" nor "message" is a String, so there is nothing usable to read out of it
        Map<String, Object> errorObject = new HashMap<>();
        errorObject.put("message", Collections.singletonList("not a string"));
        Map<String, Object> body = new HashMap<>();
        body.put("error", errorObject);
        ApiClientApiException apiEx = new ApiClientApiException("tokenize failed", 404, body);
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<BulkTokenizeResponseRecord> errors = Utils.handleBulkTokenizeBatchException(
                wrapper, tokenizeBatch("v1", "group1"), 0);

        Assert.assertEquals("tokenize failed", errors.get(0).getError());
    }

    @Test
    public void testHandleBulkTokenizeBatchException_nonMapBodyUsesApiMessage() {
        // Body is not a map, so extractBatchErrorMessage falls back to the exception's own message.
        ApiClientApiException apiEx = new ApiClientApiException("tokenize failed", 500, "raw string body");
        RuntimeException wrapper = new RuntimeException(apiEx);

        List<BulkTokenizeResponseRecord> errors = Utils.handleBulkTokenizeBatchException(
                wrapper, tokenizeBatch("v1", "group1"), 0);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("tokenize failed", errors.get(0).getError());
        Assert.assertEquals(Integer.valueOf(500), errors.get(0).getHttpCode());
    }

    @Test
    public void testHandleBulkTokenizeBatchException_nullBatchReturnsEmpty() {
        RuntimeException ex = new RuntimeException("boom");

        List<BulkTokenizeResponseRecord> errors = Utils.handleBulkTokenizeBatchException(ex, null, 0);

        Assert.assertTrue(errors.isEmpty());
    }

    @Test
    public void testHandleBulkTokenizeBatchException_emptyGroupListStillReportsOneEntry() {
        // an explicitly empty token group list, not a null one, must be treated the same way
        RuntimeException ex = new RuntimeException("boom");
        List<BulkTokenizeRequestRecord> batch = Collections.singletonList(
                BulkTokenizeRequestRecord.builder().value("v1").tokenGroupNames(new ArrayList<>()).build());

        List<BulkTokenizeResponseRecord> errors = Utils.handleBulkTokenizeBatchException(ex, batch, 0);

        Assert.assertEquals(1, errors.size());
        Assert.assertNull(errors.get(0).getTokenGroupName());
    }

    // ── formatBulkInsertResponse ───────────────────────────────────────────────

    @Test
    public void testFormatBulkInsertResponse_success() {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "tok-abc");
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        RecordResponseObject record = RecordResponseObject.builder().httpCode(200)
                .skyflowId("sky-id-1")
                .tokens(tokens)
                .data(data)
                .build();
        com.skyflow.generated.rest.types.InsertResponse response = com.skyflow.generated.rest.types.InsertResponse.builder().records(Collections.singletonList(record)).build();

        BulkInsertResponse result = Utils.formatBulkInsertResponse(response, 0, 50, new HashMap<>());

        Assert.assertEquals(1, result.getRecords().size());
        BulkInsertResponseRecord inserted = result.getRecords().get(0);
        Assert.assertEquals("sky-id-1", inserted.getSkyflowId());
        // The wire type's raw tokens map is parsed into typed Token objects before reaching the
        // caller - see ResponseComponentTests's Token.parseTokens() tests for the parsing logic.
        Assert.assertEquals("tok-abc", inserted.getTokens().get("name").get(0).getToken());
        // getFields() is deprecated, and now renders that typed data back into its original
        // Map<String, Object> shape rather than returning getTokens()'s value directly - a bare
        // string column comes back as a one-element List<Map> instead of the original bare value,
        // since that distinction is lost once the raw data is parsed into Token objects.
        Object nameField = inserted.getFields().get("name");
        Map<?, ?> nameToken = (Map<?, ?>) ((List<?>) nameField).get(0);
        Assert.assertEquals("tok-abc", nameToken.get("token"));
        Assert.assertNull(nameToken.get("tokenGroupName"));
        Assert.assertEquals(data, inserted.getData());
        Assert.assertEquals(0, inserted.getIndex());
        Assert.assertEquals(200, inserted.getHttpCode());
        Assert.assertNull(inserted.getError());
    }

    @Test
    public void testFormatBulkInsertResponse_indexOffsetByBatchNumber() {
        RecordResponseObject record = RecordResponseObject.builder().httpCode(200).skyflowId("sky-id-1").build();
        com.skyflow.generated.rest.types.InsertResponse response = com.skyflow.generated.rest.types.InsertResponse.builder().records(Collections.singletonList(record)).build();

        BulkInsertResponse result = Utils.formatBulkInsertResponse(response, 2, 50, new HashMap<>());

        Assert.assertEquals(100, result.getRecords().get(0).getIndex());
    }

    @Test
    public void testFormatBulkInsertResponse_errorWithMissingHttpCodeDefaultsTo500() {
        RecordResponseObject record = RecordResponseObject.builder().httpCode(500)
                .error("insert failed")
                .build();
        com.skyflow.generated.rest.types.InsertResponse response = com.skyflow.generated.rest.types.InsertResponse.builder().records(Collections.singletonList(record)).build();

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
        DetokenizeResponseObject record = DetokenizeResponseObject.builder()
                .token("token1")
                .value(com.skyflow.generated.rest.types.GoogleProtobufValue.of("secret-value"))
                .build();
        com.skyflow.generated.rest.types.DetokenizeResponse response = com.skyflow.generated.rest.types.DetokenizeResponse.builder()
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
        DetokenizeResponseObject record = DetokenizeResponseObject.builder()
                .token("token1")
                .build();
        com.skyflow.generated.rest.types.DetokenizeResponse response = com.skyflow.generated.rest.types.DetokenizeResponse.builder()
                .response(Collections.singletonList(record))
                .build();

        BulkDetokenizeResponse result = Utils.formatBulkDetokenizeResponse(response, 2, 50, new HashMap<>());

        Assert.assertEquals(100, result.getRecords().get(0).getIndex());
    }

    @Test
    public void testFormatBulkDetokenizeResponse_errorWithMissingHttpCodeDefaultsTo500() {
        DetokenizeResponseObject record = DetokenizeResponseObject.builder()
                .token("token1")
                .error("token not found")
                .build();
        com.skyflow.generated.rest.types.DetokenizeResponse response = com.skyflow.generated.rest.types.DetokenizeResponse.builder()
                .response(Collections.singletonList(record))
                .build();

        BulkDetokenizeResponse result = Utils.formatBulkDetokenizeResponse(response, 0, 50, new HashMap<>());

        Assert.assertEquals(1, result.getRecords().size());
        Assert.assertEquals(500, result.getRecords().get(0).getHttpCode());
        Assert.assertEquals("token not found", result.getRecords().get(0).getError());
    }

    @Test
    public void testFormatBulkDetokenizeResponse_emptyResponseYieldsNoRecords() {
        com.skyflow.generated.rest.types.DetokenizeResponse response = com.skyflow.generated.rest.types.DetokenizeResponse.builder().build();
        Assert.assertTrue(Utils.formatBulkDetokenizeResponse(response, 0, 50, new HashMap<>()).getRecords().isEmpty());
    }

    // ── formatBulkDeleteTokensResponse ─────────────────────────────────────────

    private static DeleteTokenRequest deleteBatchOf(String... tokens) {
        return DeleteTokenRequest.builder()
                .vaultId("vault123")
                .tokens(Arrays.asList(tokens))
                .build();
    }

    @Test
    public void testFormatBulkDeleteTokensResponse_duplicateTokenRelaysEachRowVerbatim() {
        // the same token sent twice: the API decides each position independently, and has been
        // observed returning both 200,200 and 200,404 for the identical request. Whatever it says
        // must reach the caller unchanged - no deduplication, no normalising one row against the other.
        String token = "<TOKEN_1>";
        String message = "DeleteToken failed. Token " + token + " is invalid. Specify a valid token.";
        DeleteTokenResponse response = DeleteTokenResponse.builder()
                .tokens(Arrays.asList(
                        DeleteTokenResponseObject.builder().value(token).httpCode(200).build(),
                        DeleteTokenResponseObject.builder()
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
        DeleteTokenResponseObject record = DeleteTokenResponseObject.builder()
                .value("token1")
                .build();
        DeleteTokenResponse response = DeleteTokenResponse.builder()
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
        DeleteTokenResponseObject record = DeleteTokenResponseObject.builder()
                .error("token not found")
                .httpCode(404)
                .build();
        DeleteTokenResponse response = DeleteTokenResponse.builder()
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
        DeleteTokenResponseObject record = DeleteTokenResponseObject.builder()
                .value("token1")
                .error("transient warning")
                .build();
        DeleteTokenResponse response = DeleteTokenResponse.builder()
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
        DeleteTokenResponse response = DeleteTokenResponse.builder()
                .tokens(Arrays.asList(
                        DeleteTokenResponseObject.builder().value("token3").build(),
                        DeleteTokenResponseObject.builder().value("token4").build()))
                .build();

        // batch 1 with batchSize 2 => indexes continue at 2
        BulkDeleteTokensResponse result = Utils.formatBulkDeleteTokensResponse(
                response, deleteBatchOf("token3", "token4"), 1, 2, new HashMap<>());

        Assert.assertEquals(2, result.getRecords().get(0).getIndex());
        Assert.assertEquals(3, result.getRecords().get(1).getIndex());
    }

    @Test
    public void testFormatBulkDeleteTokensResponse_emptyResponseYieldsNoRecords() {
        DeleteTokenResponse response = DeleteTokenResponse.builder().build();
        Assert.assertTrue(Utils.formatBulkDeleteTokensResponse(
                response, deleteBatchOf("token1"), 0, 50, new HashMap<>()).getRecords().isEmpty());
    }

    // ── formatBulkTokenizeResponse ─────────────────────────────────────────────

    private static com.skyflow.generated.rest.types.TokenizeResponse tokenizeWire(TokenizeResponseObject... records) {
        return com.skyflow.generated.rest.types.TokenizeResponse.builder().response(java.util.Arrays.asList(records)).build();
    }

    @Test
    public void testFormatBulkTokenizeResponse_success() {
        com.skyflow.generated.rest.types.TokenizeResponse response = tokenizeWire(TokenizeResponseObject.builder().token("tok-abc")
                .value(com.skyflow.generated.rest.types.GoogleProtobufValue.of("value1")).tokenGroupName("group1").build());

        BulkTokenizeResponse result = Utils.formatBulkTokenizeResponse(
                response, tokenizeBatch("value1", "group1"), 0, new HashMap<>());

        Assert.assertEquals(1, result.getRecords().size());
        BulkTokenizeResponseRecord record = result.getRecords().get(0);
        Assert.assertEquals(0, record.getIndex());
        Assert.assertEquals("value1", record.getValue());
        Assert.assertEquals("tok-abc", record.getToken());
        Assert.assertNull(record.getError());
    }

    @Test
    public void testFormatBulkTokenizeResponse_tokenError() {
        com.skyflow.generated.rest.types.TokenizeResponse response = tokenizeWire(TokenizeResponseObject.builder().token("")
                .value(com.skyflow.generated.rest.types.GoogleProtobufValue.of("value1")).tokenGroupName("group1").error("invalid value").httpCode(400).build());

        BulkTokenizeResponse result = Utils.formatBulkTokenizeResponse(
                response, tokenizeBatch("value1", "group1"), 0, new HashMap<>());

        Assert.assertEquals(1, result.getRecords().size());
        BulkTokenizeResponseRecord record = result.getRecords().get(0);
        Assert.assertEquals(Integer.valueOf(400), record.getHttpCode());
        Assert.assertEquals("invalid value", record.getError());
    }

    @Test
    public void testFormatBulkTokenizeResponse_derivesIndexFromBatchPosition() {
        com.skyflow.generated.rest.types.TokenizeResponse response = tokenizeWire(TokenizeResponseObject.builder().token("tok-abc")
                .value(com.skyflow.generated.rest.types.GoogleProtobufValue.of("value1")).tokenGroupName("group1").build());

        // this batch starts at index 40 in the caller's list
        BulkTokenizeResponse result = Utils.formatBulkTokenizeResponse(
                response, tokenizeBatch("value1", "group1"), 40, new HashMap<>());

        Assert.assertEquals(40, result.getRecords().get(0).getIndex());
    }

    @Test
    public void testFormatBulkTokenizeResponse_emptyResponseYieldsNoRecords() {
        Assert.assertTrue(Utils.formatBulkTokenizeResponse(
                com.skyflow.generated.rest.types.TokenizeResponse.builder().build(),
                tokenizeBatch("value1", "group1"), 0, new HashMap<>()).getRecords().isEmpty());
    }

    @Test
    public void testFormatBulkTokenizeResponse_nullResponseReturnsNull() {
        Assert.assertNull(Utils.formatBulkTokenizeResponse(
                null, tokenizeBatch("value1", "group1"), 0, new HashMap<>()));
    }

    // ── deleteTokens error records must survive any JSON number type ──────────
    // recordMap holds deserialised JSON: Gson gives Double for numbers bound to Object, Jackson
    // gives Integer or Long by magnitude. A blind (Integer) cast turned a real API error into a
    // ClassCastException, so each representation is covered here.

    private static BulkDeleteTokensResponseRecord deleteError(Object httpCode) {
        Map<String, Object> recordMap = new HashMap<>();
        if (httpCode != null) {
            recordMap.put("http_code", httpCode);
        }
        recordMap.put("error", "Token not found");
        recordMap.put("value", "tok-1");
        Map<String, Object> body = new HashMap<>();
        body.put("tokens", Collections.singletonList(recordMap));
        DeleteTokenRequest batch = DeleteTokenRequest.builder()
                .vaultId("vault123")
                .tokens(Collections.singletonList("tok-1"))
                .build();
        List<BulkDeleteTokensResponseRecord> records = Utils.handleBulkDeleteTokensBatchException(
                new RuntimeException(new ApiClientApiException("delete failed", 500, body)),
                batch, 0, 50);
        return records.get(0);
    }

    @Test
    public void testDeleteTokensErrorRecord_acceptsIntegerHttpCode() {
        Assert.assertEquals(Integer.valueOf(404), deleteError(404).getHttpCode());
    }

    @Test
    public void testDeleteTokensErrorRecord_acceptsDoubleHttpCode() {
        // Gson maps a JSON number to Double when the target type is Object.
        Assert.assertEquals(Integer.valueOf(404), deleteError(404.0d).getHttpCode());
    }

    @Test
    public void testDeleteTokensErrorRecord_acceptsLongHttpCode() {
        Assert.assertEquals(Integer.valueOf(404), deleteError(404L).getHttpCode());
    }

    @Test
    public void testDeleteTokensErrorRecord_acceptsStringHttpCode() {
        Assert.assertEquals(Integer.valueOf(404), deleteError("404").getHttpCode());
    }

    @Test
    public void testDeleteTokensErrorRecord_fallsBackTo500WhenTheCodeIsUnusable() {
        Assert.assertEquals(Integer.valueOf(500), deleteError("not-a-number").getHttpCode());
        Assert.assertEquals(Integer.valueOf(500), deleteError(null).getHttpCode());
    }

    @Test
    public void testDeleteTokensErrorRecord_keepsTheErrorAndEchoedToken() {
        BulkDeleteTokensResponseRecord record = deleteError(404);
        Assert.assertEquals("Token not found", record.getError());
        Assert.assertEquals("tok-1", record.getToken());
    }

    // ── handleInsertRequestException / handleUpdateRequestException / handleGetRequestException /
    // handleDeleteRequestException / handleDetokenizeRequestException / handleGetTokensRequestException
    //
    // These convert a unary call's ApiClientApiException back into a normal response when the
    // body still has the familiar per-record shape. extractExceptionRecords is shared by all six,
    // so its null/malformed-input robustness is exercised thoroughly once here (via insert) and
    // the remaining handlers each get a focused shape-guard + happy-path check.

    @Test
    public void testHandleInsertRequestException_nullBodyReturnsNull() {
        ApiClientApiException ex = new ApiClientApiException("boom", 400, null);
        Assert.assertNull(Utils.handleInsertRequestException(ex));
    }

    @Test
    public void testHandleInsertRequestException_nonMapBodyReturnsNull() {
        ApiClientApiException ex = new ApiClientApiException("boom", 400, "plain text body");
        Assert.assertNull(Utils.handleInsertRequestException(ex));
    }

    @Test
    public void testHandleInsertRequestException_missingRecordsKeyReturnsNull() {
        Map<String, Object> body = new HashMap<>();
        body.put("message", "no records key here");
        ApiClientApiException ex = new ApiClientApiException("boom", 400, body);
        Assert.assertNull(Utils.handleInsertRequestException(ex));
    }

    @Test
    public void testHandleInsertRequestException_recordsValueNotAListReturnsNull() {
        Map<String, Object> body = new HashMap<>();
        body.put("records", "not a list");
        ApiClientApiException ex = new ApiClientApiException("boom", 400, body);
        Assert.assertNull(Utils.handleInsertRequestException(ex));
    }

    @Test
    public void testHandleInsertRequestException_emptyRecordsListReturnsNull() {
        Map<String, Object> body = new HashMap<>();
        body.put("records", new ArrayList<>());
        ApiClientApiException ex = new ApiClientApiException("boom", 400, body);
        Assert.assertNull(Utils.handleInsertRequestException(ex));
    }

    @Test
    public void testHandleInsertRequestException_recordsWithOnlyNonMapElementsReturnsNull() {
        Map<String, Object> body = new HashMap<>();
        body.put("records", Arrays.asList("not-a-map", 123, null));
        ApiClientApiException ex = new ApiClientApiException("boom", 400, body);
        Assert.assertNull(Utils.handleInsertRequestException(ex));
    }

    @Test
    public void testHandleInsertRequestException_mixedValidAndInvalidElementsKeepsOnlyValidOnes() {
        Map<String, Object> validRecord = new HashMap<>();
        validRecord.put("error", "bad column");
        validRecord.put("httpCode", 400);
        Map<String, Object> body = new HashMap<>();
        body.put("records", Arrays.asList("not-a-map", validRecord));
        ApiClientApiException ex = new ApiClientApiException("boom", 400, body);

        InsertResponse response = Utils.handleInsertRequestException(ex);
        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals("bad column", response.getRecords().get(0).getError());
    }

    @Test
    public void testHandleInsertRequestException_missingHttpCodeFallsBackToExceptionStatusCode() {
        Map<String, Object> record = new HashMap<>();
        record.put("error", "bad column");
        Map<String, Object> body = new HashMap<>();
        body.put("records", Collections.singletonList(record));
        ApiClientApiException ex = new ApiClientApiException("boom", 422, body);

        InsertResponse response = Utils.handleInsertRequestException(ex);
        Assert.assertNotNull(response);
        Assert.assertEquals(422, response.getRecords().get(0).getHttpCode());
    }

    @Test
    public void testHandleInsertRequestException_missingErrorFallsBackToUnknownError() {
        Map<String, Object> record = new HashMap<>();
        record.put("httpCode", 400);
        Map<String, Object> body = new HashMap<>();
        body.put("records", Collections.singletonList(record));
        ApiClientApiException ex = new ApiClientApiException("boom", 400, body);

        InsertResponse response = Utils.handleInsertRequestException(ex);
        Assert.assertNotNull(response);
        Assert.assertEquals("Unknown error", response.getRecords().get(0).getError());
    }

    @Test
    public void testHandleInsertRequestException_validRecordsShapePopulatesResponseAndRequestId() {
        Map<String, Object> record = new HashMap<>();
        record.put("skyflowID", null);
        record.put("tableName", "table5");
        record.put("error", "INSERT failed. Column card_number is invalid. Specify a valid column.");
        record.put("httpCode", 400);
        Map<String, Object> body = new HashMap<>();
        body.put("records", Collections.singletonList(record));
        ApiClientApiException ex = new ApiClientApiException("Error with status code 400", 400, body);

        InsertResponse response = Utils.handleInsertRequestException(ex);
        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getRecords().size());
        InsertResponseRecord result = response.getRecords().get(0);
        Assert.assertEquals("table5", result.getTableName());
        Assert.assertNull(result.getSkyflowId());
        Assert.assertEquals("INSERT failed. Column card_number is invalid. Specify a valid column.", result.getError());
        Assert.assertEquals(400, result.getHttpCode());
    }

    @Test
    public void testHandleUpdateRequestException_nonRecordsShapeReturnsNull() {
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("message", "whole request failed");
        Map<String, Object> body = new HashMap<>();
        body.put("error", errorBody);
        ApiClientApiException ex = new ApiClientApiException("boom", 404, body);
        Assert.assertNull(Utils.handleUpdateRequestException(ex));
    }

    @Test
    public void testHandleUpdateRequestException_validRecordsShapePopulatesResponse() {
        Map<String, Object> record = new HashMap<>();
        record.put("error", "UPDATE failed. Column card_number is invalid. Specify a valid column.");
        record.put("httpCode", 400);
        record.put("tableName", "");
        Map<String, Object> body = new HashMap<>();
        body.put("records", Collections.singletonList(record));
        ApiClientApiException ex = new ApiClientApiException("boom", 400, body);

        UpdateResponse response = Utils.handleUpdateRequestException(ex);
        Assert.assertNotNull(response);
        Assert.assertEquals("UPDATE failed. Column card_number is invalid. Specify a valid column.",
                response.getRecords().get(0).getError());
        Assert.assertEquals(400, response.getRecords().get(0).getHttpCode());
    }

    @Test
    public void testHandleGetRequestException_nonRecordsShapeReturnsNull() {
        ApiClientApiException ex = new ApiClientApiException("boom", 500, "server error");
        Assert.assertNull(Utils.handleGetRequestException(ex));
    }

    @Test
    public void testHandleGetRequestException_validRecordsShapePopulatesResponse() {
        Map<String, Object> record = new HashMap<>();
        record.put("skyflowID", "sky-1");
        record.put("tableName", "table1");
        record.put("error", "GET failed. Record not found.");
        record.put("httpCode", 404);
        Map<String, Object> body = new HashMap<>();
        body.put("records", Collections.singletonList(record));
        ApiClientApiException ex = new ApiClientApiException("boom", 404, body);

        GetResponse response = Utils.handleGetRequestException(ex);
        Assert.assertNotNull(response);
        GetResponseRecord result = response.getRecords().get(0);
        Assert.assertEquals("sky-1", result.getSkyflowId());
        Assert.assertEquals("table1", result.getTableName());
        Assert.assertEquals(404, result.getHttpCode());
    }

    @Test
    public void testHandleDeleteRequestException_nonRecordsShapeReturnsNull() {
        Map<String, Object> body = new HashMap<>();
        body.put("records", "not a list");
        ApiClientApiException ex = new ApiClientApiException("boom", 400, body);
        Assert.assertNull(Utils.handleDeleteRequestException(ex));
    }

    @Test
    public void testHandleDeleteRequestException_validRecordsShapePopulatesResponse() {
        Map<String, Object> record = new HashMap<>();
        record.put("skyflowID", "sky-1");
        record.put("error", "DELETE failed. Record not found.");
        record.put("httpCode", 404);
        Map<String, Object> body = new HashMap<>();
        body.put("records", Collections.singletonList(record));
        ApiClientApiException ex = new ApiClientApiException("boom", 404, body);

        DeleteResponse response = Utils.handleDeleteRequestException(ex);
        Assert.assertNotNull(response);
        DeleteResponseRecord result = response.getRecords().get(0);
        Assert.assertEquals("sky-1", result.getSkyflowId());
        Assert.assertEquals(Integer.valueOf(404), result.getHttpCode());
    }

    @Test
    public void testHandleDetokenizeRequestException_recordsKeyIsWrongShapeForDetokenizeReturnsNull() {
        // Detokenize's own wire key is "response", not "records" - a body shaped for the other
        // unary ops must not be mistaken for a detokenize failure.
        Map<String, Object> record = new HashMap<>();
        record.put("error", "some error");
        Map<String, Object> body = new HashMap<>();
        body.put("records", Collections.singletonList(record));
        ApiClientApiException ex = new ApiClientApiException("boom", 400, body);
        Assert.assertNull(Utils.handleDetokenizeRequestException(ex));
    }

    @Test
    public void testHandleDetokenizeRequestException_validResponseShapePopulatesResponse() {
        Map<String, Object> record = new HashMap<>();
        record.put("token", "tok-1");
        record.put("tokenGroupName", "group1");
        record.put("error", "DETOKENIZE failed. Token not found.");
        record.put("httpCode", 404);
        Map<String, Object> body = new HashMap<>();
        body.put("response", Collections.singletonList(record));
        ApiClientApiException ex = new ApiClientApiException("boom", 404, body);

        DetokenizeResponse response = Utils.handleDetokenizeRequestException(ex);
        Assert.assertNotNull(response);
        DetokenizeResponseRecord result = response.getRecords().get(0);
        Assert.assertEquals("tok-1", result.getToken());
        Assert.assertEquals("group1", result.getTokenGroupName());
        Assert.assertEquals("DETOKENIZE failed. Token not found.", result.getError());
        Assert.assertEquals(404, result.getHttpCode());
    }

    @Test
    public void testHandleGetTokensRequestException_nonRecordsShapeReturnsNull() {
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("message", "Permission denied.");
        Map<String, Object> body = new HashMap<>();
        body.put("error", errorBody);
        ApiClientApiException ex = new ApiClientApiException("boom", 403, body);
        Assert.assertNull(Utils.handleGetTokensRequestException(ex));
    }

    @Test
    public void testHandleGetTokensRequestException_responseKeyIsWrongShapeReturnsNull() {
        // getTokens's wire key is "records"; a detokenize-shaped body must not be mistaken for it
        Map<String, Object> record = new HashMap<>();
        record.put("error", "some error");
        Map<String, Object> body = new HashMap<>();
        body.put("response", Collections.singletonList(record));
        ApiClientApiException ex = new ApiClientApiException("boom", 400, body);
        Assert.assertNull(Utils.handleGetTokensRequestException(ex));
    }

    @Test
    public void testHandleGetTokensRequestException_validRecordsShapePopulatesResponse() {
        Map<String, Object> record = new HashMap<>();
        record.put("value", "unknown@example.com");
        record.put("tokenGroupName", "det_group");
        record.put("token", "");
        record.put("error", "Token not found.");
        record.put("httpCode", 404);
        Map<String, Object> body = new HashMap<>();
        body.put("records", Collections.singletonList(record));
        ApiClientApiException ex = new ApiClientApiException("boom", 404, body);

        GetTokensResponse response = Utils.handleGetTokensRequestException(ex);
        Assert.assertNotNull(response);
        Map<String, Object> result = response.getRecords().get(0);
        Assert.assertEquals("unknown@example.com", result.get("value"));
        Assert.assertEquals("det_group", result.get("tokenGroupName"));
        Assert.assertNull(result.get("token"));
        Assert.assertEquals("Token not found.", result.get("error"));
        Assert.assertEquals(404, result.get("httpCode"));
    }

    @Test
    public void testHandleGetTokensRequestException_missingHttpCodeAndErrorFallBack() {
        Map<String, Object> record = new HashMap<>();
        record.put("value", "v");
        Map<String, Object> body = new HashMap<>();
        body.put("records", Collections.singletonList(record));
        ApiClientApiException ex = new ApiClientApiException("boom", 422, body);

        GetTokensResponse response = Utils.handleGetTokensRequestException(ex);
        Assert.assertNotNull(response);
        Assert.assertEquals(422, response.getRecords().get(0).get("httpCode"));
        Assert.assertEquals("Unknown error", response.getRecords().get(0).get("error"));
    }

    // ── Upload files ──────────────────────────────────────────────────────────

    @org.junit.Rule
    public org.junit.rules.TemporaryFolder filesFolder = new org.junit.rules.TemporaryFolder();

    private static com.skyflow.vault.data.UploadFilesRequestRecord uploadRecord(
            String skyflowId, com.skyflow.vault.data.UploadFilesRequestColumn... columns) {
        return com.skyflow.vault.data.UploadFilesRequestRecord.builder()
                .tableName("onboarding").skyflowId(skyflowId).columns(Arrays.asList(columns)).build();
    }

    private static com.skyflow.vault.data.UploadFilesRequest uploadRequest(
            com.skyflow.vault.data.UploadFilesRequestRecord... records) {
        return com.skyflow.vault.data.UploadFilesRequest.builder().records(Arrays.asList(records)).build();
    }

    @Test
    public void testGetUploadFilesRequestBody_mapsRecordsAndDerivesFileNames() throws Exception {
        File resume = filesFolder.newFile("resume.pdf");
        File photo = filesFolder.newFile("photo.jpg");
        com.skyflow.vault.data.UploadFilesRequest request = uploadRequest(
                uploadRecord(null,
                        com.skyflow.vault.data.UploadFilesRequestColumn.builder().column("resumePDF").filePath(resume.getPath()).build(),
                        com.skyflow.vault.data.UploadFilesRequestColumn.builder().column("photoID").fileObject(photo).build()),
                uploadRecord("sky-1",
                        com.skyflow.vault.data.UploadFilesRequestColumn.builder().column("kyc").base64("aGVsbG8=").fileName("kyc.txt").build(),
                        com.skyflow.vault.data.UploadFilesRequestColumn.builder().column("cv").filePath(resume.getPath()).fileName("renamed.pdf").build()));

        com.skyflow.generated.rest.resources.files.requests.FileUploadRequest body =
                Utils.getUploadFilesRequestBody(request, "vault123");

        Assert.assertEquals("vault123", body.getVaultId());
        Assert.assertEquals(2, body.getRecords().size());
        com.skyflow.generated.rest.types.FileUploadRecord created = body.getRecords().get(0);
        Assert.assertEquals("onboarding", created.getTableName());
        Assert.assertFalse(created.getSkyflowId().isPresent());
        Assert.assertEquals("resumePDF", created.getColumns().get(0).getColumn());
        Assert.assertEquals("resume.pdf", created.getColumns().get(0).getFileName().get());
        Assert.assertEquals("photo.jpg", created.getColumns().get(1).getFileName().get());
        com.skyflow.generated.rest.types.FileUploadRecord updated = body.getRecords().get(1);
        Assert.assertEquals("sky-1", updated.getSkyflowId().get());
        Assert.assertEquals("kyc.txt", updated.getColumns().get(0).getFileName().get());
        Assert.assertEquals("renamed.pdf", updated.getColumns().get(1).getFileName().get());
    }

    @Test
    public void testResolveUploadFileName_derivedFromTheSourceOrLeftToTheServer() {
        Assert.assertEquals("a.pdf", Utils.resolveUploadFileName(com.skyflow.vault.data.UploadFilesRequestColumn.builder()
                .filePath("/tmp/dir/a.pdf").build()));
        Assert.assertEquals("b.png", Utils.resolveUploadFileName(com.skyflow.vault.data.UploadFilesRequestColumn.builder()
                .fileObject(new File("/tmp/b.png")).build()));
        Assert.assertEquals("kyc.txt", Utils.resolveUploadFileName(com.skyflow.vault.data.UploadFilesRequestColumn.builder()
                .base64("aGVsbG8=").fileName("kyc.txt").build()));
        // with no name to derive, the server generates one
        Assert.assertNull(Utils.resolveUploadFileName(com.skyflow.vault.data.UploadFilesRequestColumn.builder()
                .base64("aGVsbG8=").build()));
    }

    @Test
    public void testResolveUploadContentType() {
        com.skyflow.vault.data.UploadFilesRequestColumn noType = com.skyflow.vault.data.UploadFilesRequestColumn.builder().build();
        Assert.assertEquals("image/png", Utils.resolveUploadContentType(noType, "photo.png"));
        Assert.assertEquals("application/octet-stream", Utils.resolveUploadContentType(noType, "data.unknownext"));
        Assert.assertEquals("application/octet-stream", Utils.resolveUploadContentType(noType, null));
        Assert.assertEquals("application/pdf", Utils.resolveUploadContentType(
                com.skyflow.vault.data.UploadFilesRequestColumn.builder().contentType("application/pdf").build(), "photo.png"));
    }

    @Test
    public void testBuildUploadFileBody_sendsTheBytesOfEachSource() throws Exception {
        File file = filesFolder.newFile("note.txt");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("from disk");
        }
        Assert.assertEquals("from disk", bodyText(Utils.buildUploadFileBody(
                com.skyflow.vault.data.UploadFilesRequestColumn.builder().filePath(file.getPath()).build(),
                null, "text/plain")));
        // base64 content is sent from the bytes decoded up front, not decoded again here
        Assert.assertEquals("hello", bodyText(Utils.buildUploadFileBody(
                com.skyflow.vault.data.UploadFilesRequestColumn.builder().base64("aGVsbG8=").fileName("h.txt").build(),
                "hello".getBytes(java.nio.charset.StandardCharsets.UTF_8), "text/plain")));
        okhttp3.RequestBody fromObject = Utils.buildUploadFileBody(
                com.skyflow.vault.data.UploadFilesRequestColumn.builder().fileObject(file).build(), null, "text/plain");
        Assert.assertEquals("from disk", bodyText(fromObject));
        Assert.assertEquals("text/plain", fromObject.contentType().toString());
    }

    @Test
    public void testDecodeBase64Columns_decodesEachBase64ColumnOnce() throws Exception {
        com.skyflow.vault.data.UploadFilesRequestColumn first =
                com.skyflow.vault.data.UploadFilesRequestColumn.builder().column("a").base64("aGVsbG8=").fileName("a.txt").build();
        com.skyflow.vault.data.UploadFilesRequestColumn second =
                com.skyflow.vault.data.UploadFilesRequestColumn.builder().column("b").base64("aGVsbG8=").fileName("b.txt").build();
        com.skyflow.vault.data.UploadFilesRequestColumn fromDisk =
                com.skyflow.vault.data.UploadFilesRequestColumn.builder().column("c").filePath("/tmp/c.pdf").build();

        Map<com.skyflow.vault.data.UploadFilesRequestColumn, byte[]> decoded =
                Utils.decodeBase64Columns(uploadRequest(uploadRecord(null, first, fromDisk), uploadRecord("sky-1", second)));

        Assert.assertEquals(2, decoded.size());
        Assert.assertArrayEquals("hello".getBytes(java.nio.charset.StandardCharsets.UTF_8), decoded.get(first));
        // keyed by the column itself, so two columns with equal content keep separate entries
        Assert.assertNotSame(decoded.get(first), decoded.get(second));
        Assert.assertFalse(decoded.containsKey(fromDisk));
    }

    @Test
    public void testDecodeBase64Columns_invalidContentThrows() {
        try {
            Utils.decodeBase64Columns(uploadRequest(uploadRecord(null,
                    com.skyflow.vault.data.UploadFilesRequestColumn.builder().column("kyc").base64("not base64!")
                            .fileName("kyc.txt").build())));
            Assert.fail("Should have thrown an exception");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.InvalidBase64InUploadFilesColumn.getMessage(), e.getMessage());
        }
    }

    private static String bodyText(okhttp3.RequestBody body) throws IOException {
        okio.Buffer buffer = new okio.Buffer();
        body.writeTo(buffer);
        return buffer.readUtf8();
    }

    // ── Delete files ──────────────────────────────────────────────────────────

    @Test
    public void testGetDeleteFilesRequestBody_sendsTheIdentifierEachRecordSets() {
        Map<String, Object> unique = new HashMap<>();
        unique.put("email", "a@b.com");
        com.skyflow.vault.data.DeleteFilesRequest request = com.skyflow.vault.data.DeleteFilesRequest.builder()
                .records(Arrays.asList(
                        com.skyflow.vault.data.DeleteFilesRequestRecord.builder().tableName("onboarding")
                                .skyflowId("sky-1").columns(Arrays.asList("resumePDF", "photoID")).build(),
                        com.skyflow.vault.data.DeleteFilesRequestRecord.builder().tableName("employees")
                                .uniqueValues(Collections.singletonList(unique)).columns(Collections.singletonList("photo")).build()))
                .build();

        com.skyflow.generated.rest.resources.files.requests.FileDeleteRequest body =
                Utils.getDeleteFilesRequestBody(request, "vault123");

        Assert.assertEquals("vault123", body.getVaultId());
        com.skyflow.generated.rest.types.FileDeleteRecord byId = body.getRecords().get(0);
        Assert.assertEquals("onboarding", byId.getTableName());
        Assert.assertEquals("sky-1", byId.getSkyflowId().get());
        Assert.assertEquals(Arrays.asList("resumePDF", "photoID"), byId.getColumns());
        Assert.assertFalse(byId.getUniqueValues().isPresent());
        com.skyflow.generated.rest.types.FileDeleteRecord byUnique = body.getRecords().get(1);
        Assert.assertFalse(byUnique.getSkyflowId().isPresent());
        Assert.assertEquals(unique, byUnique.getUniqueValues().get().get(0).getData());
    }

    @Test
    public void testFormatDeleteFilesResponse_partialSuccess() throws Exception {
        com.skyflow.generated.rest.types.FileDeleteResponse response = com.skyflow.generated.rest.core.ObjectMappers.JSON_MAPPER.readValue(
                "{\"records\":["
                        + "{\"skyflowID\":\"sky-1\",\"tableName\":\"onboarding\",\"httpCode\":200,"
                        + "\"data\":{\"resumePDF\":\"DELETED\",\"photoID\":{\"status\":\"DELETED\"}},\"error\":null},"
                        + "{\"skyflowID\":\"invalid-id-0000\",\"tableName\":\"employees\",\"httpCode\":404,"
                        + "\"error\":\"Invalid request. skyflowID invalid-id-0000 is invalid.\"}]}",
                com.skyflow.generated.rest.types.FileDeleteResponse.class);
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(Constants.REQUEST_ID_HEADER_KEY, Collections.singletonList("req-del-1"));

        com.skyflow.vault.data.DeleteFilesResponse result = Utils.formatDeleteFilesResponse(response, headers);

        Assert.assertEquals(2, result.getRecords().size());
        HashMap<String, Object> ok = result.getRecords().get(0);
        Assert.assertEquals("sky-1", ok.get("skyflowId"));
        Assert.assertEquals("onboarding", ok.get("tableName"));
        Assert.assertEquals(200, ok.get("httpCode"));
        Assert.assertNull(ok.get("error"));
        Assert.assertNull(ok.get("requestId"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columns = (List<Map<String, Object>>) ok.get("columns");
        Assert.assertEquals("resumePDF", columns.get(0).get("column"));
        Assert.assertEquals("DELETED", columns.get(0).get("status"));
        Assert.assertEquals("photoID", columns.get(1).get("column"));
        Assert.assertEquals("DELETED", columns.get(1).get("status"));
        HashMap<String, Object> failed = result.getRecords().get(1);
        Assert.assertNull(failed.get("columns"));
        Assert.assertEquals(404, failed.get("httpCode"));
        Assert.assertEquals("Invalid request. skyflowID invalid-id-0000 is invalid.", failed.get("error"));
        Assert.assertEquals("req-del-1", failed.get("requestId"));
    }

    @Test
    public void testFormatDeleteFilesResponse_nullResponseYieldsNoRecords() {
        Assert.assertTrue(Utils.formatDeleteFilesResponse(null, new HashMap<>()).getRecords().isEmpty());
    }

    @Test
    public void testHandleDeleteFilesRequestException() {
        Map<String, Object> failed = new HashMap<>();
        failed.put("skyflowID", "invalid-id-0000");
        failed.put("tableName", "employees");
        failed.put("error", "Invalid request. skyflowID invalid-id-0000 is invalid.");
        failed.put("httpCode", 404);
        Map<String, Object> body = new HashMap<>();
        body.put("records", Collections.singletonList(failed));

        com.skyflow.vault.data.DeleteFilesResponse response = Utils.handleDeleteFilesRequestException(
                new ApiClientApiException("Error with status code 404", 404, body));

        HashMap<String, Object> record = response.getRecords().get(0);
        Assert.assertEquals("invalid-id-0000", record.get("skyflowId"));
        Assert.assertEquals("employees", record.get("tableName"));
        Assert.assertNull(record.get("columns"));
        Assert.assertEquals(404, record.get("httpCode"));
        Assert.assertNull(Utils.handleDeleteFilesRequestException(
                new ApiClientApiException("Error with status code 500", 500, "boom")));
    }
}
