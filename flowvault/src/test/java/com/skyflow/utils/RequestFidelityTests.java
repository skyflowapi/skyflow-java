package com.skyflow.utils;

import com.skyflow.generated.rest.resources.query.requests.ExecuteQueryRequest;
import com.skyflow.generated.rest.resources.tokens.requests.DeleteTokenRequest;
import com.skyflow.generated.rest.resources.tokens.requests.GetTokensFromValuesRequest;
import com.skyflow.generated.rest.types.DeleteTokenResponse;
import com.skyflow.generated.rest.types.DeleteTokenResponseObject;
import com.skyflow.generated.rest.types.DetokenizeResponseObject;
import com.skyflow.generated.rest.types.ExecuteQueryResponse;
import com.skyflow.generated.rest.types.GetTokensFromValuesResponse;
import com.skyflow.generated.rest.types.InsertRecordData;
import com.skyflow.generated.rest.types.RecordResponseObject;
import com.skyflow.generated.rest.types.TokenizeRequestObject;
import com.skyflow.config.VaultConfig;
import com.skyflow.generated.rest.core.ObjectMappers;
import com.skyflow.vault.data.BulkDeleteTokensRequest;
import com.skyflow.vault.data.BulkDeleteTokensResponse;
import com.skyflow.vault.data.BulkDetokenizeRequest;
import com.skyflow.vault.data.BulkDetokenizeResponse;
import com.skyflow.vault.data.BulkInsertRequest;
import com.skyflow.vault.data.BulkInsertRequestRecord;
import com.skyflow.vault.data.BulkInsertResponse;
import com.skyflow.vault.data.BulkTokenizeRequestRecord;
import com.skyflow.vault.data.BulkTokenizeRequest;
import com.skyflow.vault.data.GetTokensRequest;
import com.skyflow.vault.data.GetTokensRequestRecord;
import com.skyflow.vault.data.GetTokensResponse;
import com.skyflow.vault.data.InsertRequestRecord;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.QueryResponse;
import com.skyflow.vault.data.TokenGroupRedactions;
import com.skyflow.vault.data.UpsertOptions;
import com.skyflow.generated.rest.types.UpsertUpdateType;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request-fidelity tests: every value an SDK user sets on a flowvault request object must reach
 * the outgoing generated REST request object faithfully — same value, same field, same order —
 * including across batching.
 *
 * These tests deliberately assert on the real mapping code in {@link Utils} rather than on any
 * hand-rolled copy of it, and use {@code assertSame} where the SDK should be passing the user's
 * own object through untouched (arbitrary tokenize values). The generated builders, and the
 * Jackson binding of the insert/update bodies, copy the maps and lists they are given, so those
 * are compared by value.
 */
public class RequestFidelityTests {

    private static final String VAULT_ID = "vault123";

    // Values that are easy to mangle: non-ASCII, embedded spaces, punctuation.
    private static final String NON_ASCII_NAME = "日本語 テスト Ω";
    private static final String SPACED_TABLE = "my table name";
    private static final String NON_ASCII_TABLE = "表_日本語";

    private static VaultConfig vaultConfig() {
        VaultConfig config = new VaultConfig();
        config.setVaultId(VAULT_ID);
        return config;
    }

    private static ArrayList<InsertRequestRecord> recordList(InsertRequestRecord... records) {
        return new ArrayList<>(Arrays.asList(records));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bulk insert — field fidelity
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void testBulkInsert_everyRecordFieldReachesWire() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "john");
        Map<String, Object> tokens = new LinkedHashMap<>();
        tokens.put("name", "tok-abc");
        UpsertOptions upsert = UpsertOptions.builder()
                .updateType("UPDATE")
                .uniqueColumns(Arrays.asList("email", "phone"))
                .build();

        BulkInsertRequestRecord record = BulkInsertRequestRecord.builder()
                .tableName("cards")
                .data(data)
                .tokens(tokens)
                .upsert(upsert)
                .build();
        BulkInsertRequest request = BulkInsertRequest.builder()
                .tableName("cards")
                .records(recordList(record))
                .build();

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, vaultConfig());

        Assert.assertEquals(VAULT_ID, body.getVaultId());
        Assert.assertEquals("cards", body.getTableName());
        InsertRecordData wire = body.getRecords().get(0);
        Assert.assertEquals("cards", wire.getTableName().get());
        // The user's own map instances must be handed to the wire object untouched.
        Assert.assertEquals(data, wire.getData());
        Assert.assertEquals(tokens, wire.getAdditionalProperties().get("tokens"));
        Assert.assertEquals(UpsertUpdateType.UPDATE, wire.getUpsert().get().getUpdateType().get());
        Assert.assertEquals(Arrays.asList("email", "phone"), wire.getUpsert().get().getUniqueColumns());
    }

    @Test
    public void testBulkInsert_nonAsciiAndSpacedValues_userValueReachesWire() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", NON_ASCII_NAME);
        data.put("street address", "12 東京都 千代田区");
        Map<String, Object> tokens = new LinkedHashMap<>();
        tokens.put("name", "tök-ábc 123");

        BulkInsertRequestRecord record = BulkInsertRequestRecord.builder()
                .tableName(NON_ASCII_TABLE)
                .data(data)
                .tokens(tokens)
                .build();
        BulkInsertRequest request = BulkInsertRequest.builder()
                .tableName(SPACED_TABLE)
                .records(recordList(record))
                .build();

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, vaultConfig());

        Assert.assertEquals(SPACED_TABLE, body.getTableName());
        InsertRecordData wire = body.getRecords().get(0);
        Assert.assertEquals(NON_ASCII_TABLE, wire.getTableName().get());
        Assert.assertEquals(NON_ASCII_NAME, wire.getData().get("name"));
        Assert.assertEquals("12 東京都 千代田区", wire.getData().get("street address"));
        Assert.assertEquals("tök-ábc 123", ((Map<?, ?>) wire.getAdditionalProperties().get("tokens")).get("name"));
    }

    @Test
    public void testBulkInsert_nonStringDataValues_userValueReachesWire() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("city", "Paris");
        nested.put("zip", 75001);
        List<Object> list = Arrays.asList(1, "two", true);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("age", 42);
        data.put("balance", 1234.56d);
        data.put("longValue", 9007199254740993L);
        data.put("active", Boolean.TRUE);
        data.put("address", nested);
        data.put("tags", list);

        BulkInsertRequestRecord record = BulkInsertRequestRecord.builder()
                .tableName("cards")
                .data(data)
                .build();
        BulkInsertRequest request = BulkInsertRequest.builder().records(recordList(record)).build();

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, vaultConfig());
        Map<String, Object> wireData = body.getRecords().get(0).getData();

        Assert.assertEquals(data, wireData);
        // Object identity/type of every value survives — no toString()-ing, no boxing changes.
        Assert.assertEquals(Integer.valueOf(42), wireData.get("age"));
        Assert.assertEquals(Double.valueOf(1234.56d), wireData.get("balance"));
        Assert.assertEquals(Long.valueOf(9007199254740993L), wireData.get("longValue"));
        Assert.assertSame(Boolean.TRUE, wireData.get("active"));
        Assert.assertEquals(nested, wireData.get("address"));
        Assert.assertEquals(list, wireData.get("tags"));
        Assert.assertEquals(Integer.valueOf(75001), ((Map<?, ?>) wireData.get("address")).get("zip"));
    }

    @Test
    public void testBulkInsert_recordOrderPreserved() {
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("pos", i);
            records.add(BulkInsertRequestRecord.builder().tableName("cards").data(data).build());
        }
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, vaultConfig());

        List<InsertRecordData> wireRecords = body.getRecords();
        Assert.assertEquals(7, wireRecords.size());
        for (int i = 0; i < 7; i++) {
            Assert.assertEquals(Integer.valueOf(i), wireRecords.get(i).getData().get("pos"));
        }
    }

    @Test
    public void testBulkInsert_recordOrderPreservedAcrossBatches() {
        int total = 7;
        int batchSize = 3;
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("pos", i);
            records.add(BulkInsertRequestRecord.builder().tableName("cards").data(data).build());
        }
        BulkInsertRequest request = BulkInsertRequest.builder().tableName("cards").records(records).build();

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, vaultConfig());
        List<List<InsertRecordData>> batches = Utils.createBulkInsertBatches(body.getRecords(), batchSize);

        Assert.assertEquals(3, batches.size());
        int expected = 0;
        for (List<InsertRecordData> batch : batches) {
            for (InsertRecordData wire : batch) {
                Assert.assertEquals(Integer.valueOf(expected), wire.getData().get("pos"));
                // Non-batched per-record fields survive batching on every batch.
                Assert.assertEquals("cards", wire.getTableName().get());
                expected++;
            }
        }
        Assert.assertEquals(total, expected);
    }

    @Test
    public void testBulkInsert_responseIndexMapsToOriginalInputPosition_acrossBatches() {
        int total = 7;
        int batchSize = 3;
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("pos", i);
            records.add(BulkInsertRequestRecord.builder().tableName("cards").data(data).build());
        }
        BulkInsertRequest request = BulkInsertRequest.builder().tableName("cards").records(records).build();

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, vaultConfig());
        List<List<InsertRecordData>> batches = Utils.createBulkInsertBatches(body.getRecords(), batchSize);

        // Simulate the server echoing one response record per request record in each batch, then
        // assert the SDK-assigned index equals the record's position in the ORIGINAL user list.
        List<Integer> indices = new ArrayList<>();
        List<String> skyflowIds = new ArrayList<>();
        for (int batchNumber = 0; batchNumber < batches.size(); batchNumber++) {
            List<RecordResponseObject> responseRecords = new ArrayList<>();
            for (InsertRecordData wire : batches.get(batchNumber)) {
                responseRecords.add(RecordResponseObject.builder().httpCode(200)
                        .skyflowId("sky-" + wire.getData().get("pos"))
                        .build());
            }
            com.skyflow.generated.rest.types.InsertResponse response = com.skyflow.generated.rest.types.InsertResponse.builder().records(responseRecords).build();
            BulkInsertResponse formatted = Utils.formatBulkInsertResponse(response, batchNumber, batchSize, new HashMap<>());
            formatted.getRecords().forEach(r -> {
                indices.add(r.getIndex());
                skyflowIds.add(r.getSkyflowId());
            });
        }

        Assert.assertEquals(total, indices.size());
        for (int i = 0; i < total; i++) {
            Assert.assertEquals(Integer.valueOf(i), indices.get(i));
            Assert.assertEquals("sky-" + i, skyflowIds.get(i));
        }
    }

    // ── insert: table-name precedence / fallback regressions ─────────────────

    @Test
    public void testBulkInsert_blankRecordTableNameStaysAtRequestLevel() {
        // Regression: a blank record-level table name counts as ABSENT (Utils.hasText). The name
        // must go out on the envelope ONLY — the vault rejects a body carrying it at both levels.
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        BulkInsertRequestRecord record = BulkInsertRequestRecord.builder()
                .tableName("   ")
                .data(data)
                .build();
        BulkInsertRequest request = BulkInsertRequest.builder()
                .tableName("cards")
                .records(recordList(record))
                .build();

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, vaultConfig());

        Assert.assertFalse(body.getRecords().get(0).getTableName().isPresent());
        Assert.assertEquals("cards", body.getTableName());
    }

    @Test
    public void testBulkInsert_nullRecordTableNameStaysAtRequestLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        BulkInsertRequestRecord record = BulkInsertRequestRecord.builder().data(data).build();
        BulkInsertRequest request = BulkInsertRequest.builder()
                .tableName("cards")
                .records(recordList(record))
                .build();

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, vaultConfig());

        Assert.assertFalse(body.getRecords().get(0).getTableName().isPresent());
        Assert.assertEquals("cards", body.getTableName());
    }

    @Test
    public void testBulkInsert_emptyStringRecordTableNameStaysAtRequestLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        BulkInsertRequestRecord record = BulkInsertRequestRecord.builder().tableName("").data(data).build();
        BulkInsertRequest request = BulkInsertRequest.builder()
                .tableName("cards")
                .records(recordList(record))
                .build();

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, vaultConfig());

        Assert.assertFalse(body.getRecords().get(0).getTableName().isPresent());
        Assert.assertEquals("cards", body.getTableName());
    }

    @Test
    public void testBulkInsert_recordTableNameOverridesRequestLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        BulkInsertRequestRecord record = BulkInsertRequestRecord.builder()
                .tableName("record_table")
                .data(data)
                .build();
        BulkInsertRequest request = BulkInsertRequest.builder()
                .tableName("request_table")
                .records(recordList(record))
                .build();

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, vaultConfig());

        Assert.assertEquals("record_table", body.getRecords().get(0).getTableName().get());
        // The request-level name still goes out on the envelope, untouched.
        Assert.assertEquals("request_table", body.getTableName());
    }

    @Test
    public void testBulkInsert_perRecordTableNamesResolveIndependently() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        BulkInsertRequestRecord withOwn = BulkInsertRequestRecord.builder().tableName("own").data(data).build();
        BulkInsertRequestRecord blank = BulkInsertRequestRecord.builder().tableName("  ").data(data).build();
        BulkInsertRequestRecord missing = BulkInsertRequestRecord.builder().data(data).build();
        BulkInsertRequest request = BulkInsertRequest.builder()
                .tableName("fallback")
                .records(recordList(withOwn, blank, missing))
                .build();

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, vaultConfig());

        // Only a record that names its own table carries one on the wire; the others rely on the
        // envelope. Nothing is copied down, so the name is never duplicated across both levels.
        Assert.assertEquals("own", body.getRecords().get(0).getTableName().get());
        Assert.assertFalse(body.getRecords().get(1).getTableName().isPresent());
        Assert.assertFalse(body.getRecords().get(2).getTableName().isPresent());
        Assert.assertEquals("fallback", body.getTableName());
    }

    @Test
    public void testBulkInsert_blankRequestLevelTableNameIsOmittedFromEnvelope() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        BulkInsertRequestRecord record = BulkInsertRequestRecord.builder().tableName("cards").data(data).build();
        BulkInsertRequest request = BulkInsertRequest.builder()
                .tableName("   ")
                .records(recordList(record))
                .build();

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, vaultConfig());

        Assert.assertNull(body.getTableName());
        Assert.assertEquals("cards", body.getRecords().get(0).getTableName().get());
    }

    // ── insert: upsert mapping ───────────────────────────────────────────────

    @Test
    public void testUpsert_updateTypeUpdateAndReplace_userValueReachesWire() {
        Assert.assertEquals(UpsertUpdateType.UPDATE, upsertWire("UPDATE").getUpdateType().get());
        Assert.assertEquals(UpsertUpdateType.REPLACE, upsertWire("REPLACE").getUpdateType().get());
    }

    @Test
    public void testUpsert_updateTypeIsMatchedCaseInsensitively() {
        Assert.assertEquals(UpsertUpdateType.UPDATE, upsertWire("update").getUpdateType().get());
        Assert.assertEquals(UpsertUpdateType.UPDATE, upsertWire("UpDaTe").getUpdateType().get());
        Assert.assertEquals(UpsertUpdateType.REPLACE, upsertWire("replace").getUpdateType().get());
        Assert.assertEquals(UpsertUpdateType.REPLACE, upsertWire("Replace").getUpdateType().get());
    }

    @Test
    public void testUpsert_unrecognizedUpdateTypeIsRejectedBeforeMapping() {
        // Validations.validateUpsertOptions now rejects anything that is not UPDATE/REPLACE, so
        // the mapper can no longer be reached with a value it would silently drop. A null
        // updateType stays legal and simply omits the field.
        com.skyflow.generated.rest.types.Upsert nullType = upsertWire(null);
        Assert.assertFalse(nullType.getUpdateType().isPresent());
        Assert.assertEquals(Collections.singletonList("email"), nullType.getUniqueColumns());
    }

    @Test
    public void testUpsert_uniqueColumnsValuesAndOrderReachWire() {
        List<String> uniqueColumns = Arrays.asList("email", "phone number", NON_ASCII_NAME);
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        BulkInsertRequestRecord record = BulkInsertRequestRecord.builder()
                .tableName("cards")
                .data(data)
                .upsert(UpsertOptions.builder().updateType("UPDATE").uniqueColumns(uniqueColumns).build())
                .build();
        BulkInsertRequest request = BulkInsertRequest.builder().records(recordList(record)).build();

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, vaultConfig());

        List<String> wireColumns = body.getRecords().get(0).getUpsert().get().getUniqueColumns();
        Assert.assertEquals(uniqueColumns, wireColumns);
        Assert.assertEquals(Arrays.asList("email", "phone number", NON_ASCII_NAME), wireColumns);
    }

    @Test
    public void testUpsert_recordLevelOverridesRequestLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        BulkInsertRequestRecord record = BulkInsertRequestRecord.builder()
                .tableName("cards")
                .data(data)
                .upsert(UpsertOptions.builder()
                        .updateType("REPLACE")
                        .uniqueColumns(Collections.singletonList("record_col"))
                        .build())
                .build();
        BulkInsertRequest request = BulkInsertRequest.builder()
                .records(recordList(record))
                .upsert(UpsertOptions.builder()
                        .updateType("UPDATE")
                        .uniqueColumns(Collections.singletonList("request_col"))
                        .build())
                .build();

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, vaultConfig());

        com.skyflow.generated.rest.types.Upsert wire = body.getRecords().get(0).getUpsert().get();
        Assert.assertEquals(UpsertUpdateType.REPLACE, wire.getUpdateType().get());
        Assert.assertEquals(Collections.singletonList("record_col"), wire.getUniqueColumns());
    }

    @Test
    public void testUpsert_requestLevelStaysOnEnvelopeAndIsNotCopiedOntoRecords() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        BulkInsertRequest request = BulkInsertRequest.builder()
                .tableName("cards")
                .records(recordList(
                        BulkInsertRequestRecord.builder().data(data).build(),
                        BulkInsertRequestRecord.builder().data(data).build(),
                        BulkInsertRequestRecord.builder().data(data).build()))
                .upsert(UpsertOptions.builder()
                        .updateType("UPDATE")
                        .uniqueColumns(Collections.singletonList("email"))
                        .build())
                .build();

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, vaultConfig());

        // upsert must travel at the same single level as the table name — here, the envelope.
        for (InsertRecordData wire : body.getRecords()) {
            Assert.assertFalse(wire.getUpsert().isPresent());
        }
        Assert.assertTrue(body.getUpsert().isPresent());
        Assert.assertEquals(UpsertUpdateType.UPDATE, body.getUpsert().get().getUpdateType().get());
        Assert.assertEquals(Collections.singletonList("email"), body.getUpsert().get().getUniqueColumns());
    }

    @Test
    public void testUpsert_requestLevelUpsertReachesEnvelope() {
        // Regression: the request-level upsert used to be projected onto every record and never set
        // on the com.skyflow.generated.rest.resources.records.requests.InsertRequest envelope, so VaultController#insertBatchFutures — which reads
        // insertRequest.getUpsert() to re-apply it per batch — always read empty.
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        BulkInsertRequest request = BulkInsertRequest.builder()
                .tableName("cards")
                .records(recordList(BulkInsertRequestRecord.builder().data(data).build()))
                .upsert(UpsertOptions.builder()
                        .updateType("UPDATE")
                        .uniqueColumns(Collections.singletonList("email"))
                        .build())
                .build();

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, vaultConfig());

        Assert.assertTrue(body.getUpsert().isPresent());
        Assert.assertFalse(body.getRecords().get(0).getUpsert().isPresent());
    }

    @Test
    public void testUpsert_emptyUniqueColumnsMeansNoUpsertOnWire() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        BulkInsertRequestRecord record = BulkInsertRequestRecord.builder()
                .tableName("cards")
                .data(data)
                .upsert(UpsertOptions.builder()
                        .updateType("UPDATE")
                        .uniqueColumns(new ArrayList<>())
                        .build())
                .build();
        BulkInsertRequest request = BulkInsertRequest.builder().records(recordList(record)).build();

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, vaultConfig());

        Assert.assertFalse(body.getRecords().get(0).getUpsert().isPresent());
    }

    private static com.skyflow.generated.rest.types.Upsert upsertWire(String updateType) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        BulkInsertRequestRecord record = BulkInsertRequestRecord.builder()
                .tableName("cards")
                .data(data)
                .upsert(UpsertOptions.builder()
                        .updateType(updateType)
                        .uniqueColumns(Collections.singletonList("email"))
                        .build())
                .build();
        BulkInsertRequest request = BulkInsertRequest.builder().records(recordList(record)).build();
        return Utils.getBulkInsertRequestBody(request, vaultConfig())
                .getRecords().get(0).getUpsert().get();
    }

    // ── insert: tokens map ───────────────────────────────────────────────────

    @Test
    public void testBulkInsert_emptyTokensMapIsOmittedFromWire() {
        // Validations.validateInsertRequest rejects an explicitly-set-but-empty tokens map before
        // the body builder runs; this pins the builder's own behavior when called directly.
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        BulkInsertRequestRecord record = BulkInsertRequestRecord.builder()
                .tableName("cards")
                .data(data)
                .tokens(new HashMap<>())
                .build();
        BulkInsertRequest request = BulkInsertRequest.builder().records(recordList(record)).build();

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, vaultConfig());

        Assert.assertFalse(body.getRecords().get(0).getAdditionalProperties().containsKey("tokens"));
    }

    @Test
    public void testBulkInsert_nullTokensMapIsOmittedFromWire() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        BulkInsertRequestRecord record = BulkInsertRequestRecord.builder()
                .tableName("cards")
                .data(data)
                .build();
        BulkInsertRequest request = BulkInsertRequest.builder().records(recordList(record)).build();

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, vaultConfig());

        Assert.assertFalse(body.getRecords().get(0).getAdditionalProperties().containsKey("tokens"));
    }

    @Test
    public void testBulkInsert_multiValueTokensMapReachesWireVerbatim() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        Map<String, Object> tokens = new LinkedHashMap<>();
        tokens.put("name", "tok-1");
        tokens.put("ssn", "tok-2");
        tokens.put("nested", Collections.singletonMap("group", "tok-3"));
        BulkInsertRequestRecord record = BulkInsertRequestRecord.builder()
                .tableName("cards")
                .data(data)
                .tokens(tokens)
                .build();
        BulkInsertRequest request = BulkInsertRequest.builder().records(recordList(record)).build();

        com.skyflow.generated.rest.resources.records.requests.InsertRequest body = Utils.getBulkInsertRequestBody(request, vaultConfig());

        @SuppressWarnings("unchecked")
        Map<String, Object> wireTokens = (Map<String, Object>) body.getRecords().get(0).getAdditionalProperties().get("tokens");
        Assert.assertEquals(tokens, wireTokens);
        Assert.assertEquals("tok-1", wireTokens.get("name"));
        Assert.assertEquals("tok-2", wireTokens.get("ssn"));
        Assert.assertEquals(Collections.singletonMap("group", "tok-3"), wireTokens.get("nested"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bulk detokenize
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void testBulkDetokenize_everyFieldReachesWire() {
        List<String> tokens = Arrays.asList("token-1", "token 2", "トークン-3");
        TokenGroupRedactions groupA = TokenGroupRedactions.builder()
                .tokenGroupName("group one")
                .redaction("MASKED")
                .build();
        TokenGroupRedactions groupB = TokenGroupRedactions.builder()
                .tokenGroupName(NON_ASCII_NAME)
                .redaction("PLAIN_TEXT")
                .build();
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder()
                .tokens(tokens)
                .tokenGroupRedactions(Arrays.asList(groupA, groupB))
                .build();

        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest body = Utils.getBulkDetokenizeRequestBody(request, VAULT_ID);

        Assert.assertEquals(VAULT_ID, body.getVaultId());
        Assert.assertEquals(tokens, body.getTokens());
        Assert.assertEquals(Arrays.asList("token-1", "token 2", "トークン-3"), body.getTokens());

        List<com.skyflow.generated.rest.types.TokenGroupRedactions> wireGroups = body.getTokenGroupRedactions().get();
        Assert.assertEquals(2, wireGroups.size());
        Assert.assertEquals("group one", wireGroups.get(0).getTokenGroupName().get());
        Assert.assertEquals("MASKED", wireGroups.get(0).getRedaction().get());
        Assert.assertEquals(NON_ASCII_NAME, wireGroups.get(1).getTokenGroupName().get());
        Assert.assertEquals("PLAIN_TEXT", wireGroups.get(1).getRedaction().get());
    }

    @Test
    public void testBulkDetokenize_emptyTokenGroupRedactionsListIsOmitted() {
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder()
                .tokens(Collections.singletonList("token-1"))
                .tokenGroupRedactions(new ArrayList<>())
                .build();

        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest body = Utils.getBulkDetokenizeRequestBody(request, VAULT_ID);

        Assert.assertFalse(body.getTokenGroupRedactions().isPresent());
    }

    @Test
    public void testBulkDetokenize_tokenOrderPreservedAcrossBatches() {
        int total = 7;
        int batchSize = 3;
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            tokens.add("token-" + i);
        }
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder().tokens(tokens).build();

        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest body = Utils.getBulkDetokenizeRequestBody(request, VAULT_ID);
        List<com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest> batches = Utils.createBulkDetokenizeBatches(body, batchSize);

        Assert.assertEquals(3, batches.size());
        List<String> flattened = new ArrayList<>();
        for (com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest batch : batches) {
            flattened.addAll(batch.getTokens());
        }
        Assert.assertEquals(tokens, flattened);
    }

    @Test
    public void testBulkDetokenize_vaultIdAndRedactionsPresentOnEveryBatch() {
        int total = 7;
        int batchSize = 3;
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            tokens.add("token-" + i);
        }
        TokenGroupRedactions group = TokenGroupRedactions.builder()
                .tokenGroupName("group one")
                .redaction("MASKED")
                .build();
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder()
                .tokens(tokens)
                .tokenGroupRedactions(Collections.singletonList(group))
                .build();

        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest body = Utils.getBulkDetokenizeRequestBody(request, VAULT_ID);
        List<com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest> batches = Utils.createBulkDetokenizeBatches(body, batchSize);

        Assert.assertEquals(3, batches.size());
        for (com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest batch : batches) {
            Assert.assertEquals(VAULT_ID, batch.getVaultId());
            Assert.assertTrue(batch.getTokenGroupRedactions().isPresent());
            Assert.assertEquals(1, batch.getTokenGroupRedactions().get().size());
            Assert.assertEquals("group one", batch.getTokenGroupRedactions().get().get(0).getTokenGroupName().get());
            Assert.assertEquals("MASKED", batch.getTokenGroupRedactions().get().get(0).getRedaction().get());
        }
    }

    @Test
    public void testBulkDetokenize_responseIndexMapsToOriginalInputPosition_acrossBatches() {
        int total = 7;
        int batchSize = 3;
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            tokens.add("token-" + i);
        }
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder().tokens(tokens).build();
        com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest body = Utils.getBulkDetokenizeRequestBody(request, VAULT_ID);
        List<com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest> batches = Utils.createBulkDetokenizeBatches(body, batchSize);

        List<Integer> indices = new ArrayList<>();
        List<String> echoedTokens = new ArrayList<>();
        for (int batchNumber = 0; batchNumber < batches.size(); batchNumber++) {
            List<DetokenizeResponseObject> responseRecords = new ArrayList<>();
            for (String token : batches.get(batchNumber).getTokens()) {
                responseRecords.add(DetokenizeResponseObject.builder().token(token).build());
            }
            com.skyflow.generated.rest.types.DetokenizeResponse response = com.skyflow.generated.rest.types.DetokenizeResponse.builder().response(responseRecords).build();
            BulkDetokenizeResponse formatted =
                    Utils.formatBulkDetokenizeResponse(response, batchNumber, batchSize, new HashMap<>());
            formatted.getRecords().forEach(r -> {
                indices.add(r.getIndex());
                echoedTokens.add(r.getToken());
            });
        }

        Assert.assertEquals(total, indices.size());
        for (int i = 0; i < total; i++) {
            Assert.assertEquals(Integer.valueOf(i), indices.get(i));
            Assert.assertEquals(tokens.get(i), echoedTokens.get(i));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bulk tokenize
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void testBulkTokenize_everyFieldReachesWire() {
        List<String> groupNames = Arrays.asList("group one", NON_ASCII_NAME);
        BulkTokenizeRequestRecord record = BulkTokenizeRequestRecord.builder()
                .value(NON_ASCII_NAME)
                .tokenGroupNames(groupNames)
                .build();
        List<BulkTokenizeRequestRecord> records = Collections.singletonList(record);

        com.skyflow.generated.rest.resources.tokens.requests.TokenizeRequest body = Utils.getBulkTokenizeRequestBody(records, VAULT_ID);

        Assert.assertEquals(VAULT_ID, body.getVaultId());
        Assert.assertEquals(1, body.getData().size());
        TokenizeRequestObject wire = body.getData().get(0);
        Assert.assertEquals(NON_ASCII_NAME, wire.getValue().get());
        Assert.assertEquals(groupNames, wire.getTokenGroupNames());
        Assert.assertEquals(Arrays.asList("group one", NON_ASCII_NAME), wire.getTokenGroupNames());
    }

    @Test
    public void testBulkTokenize_byotTokenReachesWire() {
        BulkTokenizeRequestRecord record = BulkTokenizeRequestRecord.builder()
                .value("v1")
                .token("my-own-token")
                .tokenGroupNames(Collections.singletonList("g1"))
                .build();

        com.skyflow.generated.rest.resources.tokens.requests.TokenizeRequest body = Utils.getBulkTokenizeRequestBody(
                Collections.singletonList(record), VAULT_ID);

        Assert.assertEquals("my-own-token", body.getData().get(0).getToken().get().get());
    }

    @Test
    public void testBulkTokenize_absentByotTokenIsOmittedFromWire() {
        BulkTokenizeRequestRecord record = BulkTokenizeRequestRecord.builder()
                .value("v1")
                .tokenGroupNames(Collections.singletonList("g1"))
                .build();

        com.skyflow.generated.rest.resources.tokens.requests.TokenizeRequest body = Utils.getBulkTokenizeRequestBody(
                Collections.singletonList(record), VAULT_ID);

        // omitted rather than sent as null, so a non-BYOT request is byte-identical to before
        Assert.assertFalse(body.getData().get(0).getToken().isPresent());
    }

    @Test
    public void testBulkTokenize_nonStringValues_objectIdentitySurvives() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("city", "Paris");
        nested.put("zip", 75001);
        List<Object> listValue = Arrays.asList(1, 2, 3);

        List<BulkTokenizeRequestRecord> records = Arrays.asList(
                BulkTokenizeRequestRecord.builder().value(42).build(),
                BulkTokenizeRequestRecord.builder().value(3.14d).build(),
                BulkTokenizeRequestRecord.builder().value(Boolean.FALSE).build(),
                BulkTokenizeRequestRecord.builder().value(nested).build(),
                BulkTokenizeRequestRecord.builder().value(listValue).build());

        com.skyflow.generated.rest.resources.tokens.requests.TokenizeRequest body = Utils.getBulkTokenizeRequestBody(records, VAULT_ID);
        List<TokenizeRequestObject> wire = body.getData();

        Assert.assertEquals(Integer.valueOf(42), wire.get(0).getValue().get());
        Assert.assertEquals(Double.valueOf(3.14d), wire.get(1).getValue().get());
        Assert.assertSame(Boolean.FALSE, wire.get(2).getValue().get());
        Assert.assertSame(nested, wire.get(3).getValue().get());
        Assert.assertSame(listValue, wire.get(4).getValue().get());
    }

    @Test
    public void testBulkTokenize_nullTokenGroupNamesIsOmitted() {
        List<BulkTokenizeRequestRecord> records =
                Collections.singletonList(BulkTokenizeRequestRecord.builder().value("v1").build());

        com.skyflow.generated.rest.resources.tokens.requests.TokenizeRequest body = Utils.getBulkTokenizeRequestBody(records, VAULT_ID);

        Assert.assertTrue(body.getData().get(0).getTokenGroupNames().isEmpty());
    }

    @Test
    public void testBulkTokenize_recordOrderPreservedAcrossBatches() {
        int total = 7;
        int batchSize = 3;
        List<BulkTokenizeRequestRecord> records = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            records.add(BulkTokenizeRequestRecord.builder().value("value-" + i).build());
        }

        // batching now happens on the SDK records, before the wire object is built, so the
        // response formatter can recover each value's index from its batch position
        List<List<BulkTokenizeRequestRecord>> batches =
                Utils.createBulkTokenizeBatches(records, batchSize);

        Assert.assertEquals(3, batches.size());
        List<Object> flattened = new ArrayList<>();
        for (List<BulkTokenizeRequestRecord> batch : batches) {
            com.skyflow.generated.rest.resources.tokens.requests.TokenizeRequest body = Utils.getBulkTokenizeRequestBody(batch, VAULT_ID);
            // vaultId is a non-batched field and must be re-applied on every batch.
            Assert.assertEquals(VAULT_ID, body.getVaultId());
            for (TokenizeRequestObject obj : body.getData()) {
                flattened.add(obj.getValue().get());
            }
        }
        Assert.assertEquals(total, flattened.size());
        for (int i = 0; i < total; i++) {
            Assert.assertEquals("value-" + i, flattened.get(i));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bulk delete tokens
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void testBulkDeleteTokens_everyFieldReachesWire() {
        List<String> tokens = Arrays.asList("token-1", "token 2", "トークン-3");
        BulkDeleteTokensRequest request = BulkDeleteTokensRequest.builder().tokens(tokens).build();

        DeleteTokenRequest body = Utils.getBulkDeleteTokensRequestBody(request, VAULT_ID);

        Assert.assertEquals(VAULT_ID, body.getVaultId());
        Assert.assertEquals(tokens, body.getTokens());
        Assert.assertEquals(Arrays.asList("token-1", "token 2", "トークン-3"), body.getTokens());
    }

    @Test
    public void testBulkDeleteTokens_tokenOrderPreservedAndVaultIdOnEveryBatch() {
        int total = 7;
        int batchSize = 3;
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            tokens.add("token-" + i);
        }
        BulkDeleteTokensRequest request = BulkDeleteTokensRequest.builder().tokens(tokens).build();

        DeleteTokenRequest body = Utils.getBulkDeleteTokensRequestBody(request, VAULT_ID);
        List<DeleteTokenRequest> batches = Utils.createBulkDeleteTokensBatches(body, batchSize);

        Assert.assertEquals(3, batches.size());
        List<String> flattened = new ArrayList<>();
        for (DeleteTokenRequest batch : batches) {
            Assert.assertEquals(VAULT_ID, batch.getVaultId());
            flattened.addAll(batch.getTokens());
        }
        Assert.assertEquals(tokens, flattened);
    }

    @Test
    public void testBulkDeleteTokens_responseIndexMapsToOriginalInputPosition_acrossBatches() {
        int total = 7;
        int batchSize = 3;
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            tokens.add("token-" + i);
        }
        BulkDeleteTokensRequest request = BulkDeleteTokensRequest.builder().tokens(tokens).build();
        DeleteTokenRequest body = Utils.getBulkDeleteTokensRequestBody(request, VAULT_ID);
        List<DeleteTokenRequest> batches = Utils.createBulkDeleteTokensBatches(body, batchSize);

        List<Integer> indices = new ArrayList<>();
        List<String> echoed = new ArrayList<>();
        for (int batchNumber = 0; batchNumber < batches.size(); batchNumber++) {
            List<DeleteTokenResponseObject> responseRecords = new ArrayList<>();
            for (String token : batches.get(batchNumber).getTokens()) {
                responseRecords.add(DeleteTokenResponseObject.builder().value(token).build());
            }
            DeleteTokenResponse response =
                    DeleteTokenResponse.builder().tokens(responseRecords).build();
            // successes and errors now share one records list, keyed by index
            BulkDeleteTokensResponse formatted = Utils.formatBulkDeleteTokensResponse(
                    response, batches.get(batchNumber), batchNumber, batchSize, new HashMap<>());
            formatted.getRecords().forEach(r -> {
                indices.add(r.getIndex());
                echoed.add(r.getToken());
            });
        }

        Assert.assertEquals(total, indices.size());
        for (int i = 0; i < total; i++) {
            Assert.assertEquals(Integer.valueOf(i), indices.get(i));
            Assert.assertEquals(tokens.get(i), echoed.get(i));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Query
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void testQuery_queryStringReachesWireUntouched() {
        String query = "SELECT name, \"street address\" FROM " + NON_ASCII_TABLE
                + " WHERE name = '" + NON_ASCII_NAME + "' LIMIT 25 OFFSET 50";
        QueryRequest request = QueryRequest.builder().query(query).build();

        ExecuteQueryRequest body = Utils.getQueryRequestBody(request, VAULT_ID);

        Assert.assertEquals(VAULT_ID, body.getVaultId());
        Assert.assertEquals(query, body.getQuery());
    }

    @Test
    public void testQuery_serializesToDocumentedWireShape() throws Exception {
        QueryRequest request = QueryRequest.builder().query("SELECT * FROM persons").build();

        String json = ObjectMappers.JSON_MAPPER.writeValueAsString(Utils.getQueryRequestBody(request, VAULT_ID));

        Assert.assertEquals("{\"vaultID\":\"" + VAULT_ID + "\",\"query\":\"SELECT * FROM persons\"}", json);
    }

    @Test
    public void testQuery_wireResponseRowsReachCallerInOrder() throws Exception {
        ExecuteQueryResponse wire = ObjectMappers.JSON_MAPPER.readValue(
                "{\"records\":["
                        + "{\"data\":{\"skyflow_id\":\"sky-1\",\"name\":\"" + NON_ASCII_NAME + "\",\"age\":30}},"
                        + "{\"data\":{\"skyflow_id\":\"sky-2\",\"name\":\"jane\",\"age\":null}}],"
                        + "\"metadata\":{\"columns\":[\"skyflow_id\",\"name\",\"age\"]}}",
                ExecuteQueryResponse.class);

        QueryResponse response = Utils.formatQueryResponse(wire, new HashMap<>());

        Assert.assertEquals(2, response.getFields().size());
        Assert.assertEquals(Arrays.asList("skyflow_id", "name", "age"),
                new ArrayList<>(response.getFields().get(0).keySet()));
        Assert.assertEquals(NON_ASCII_NAME, response.getFields().get(0).get("name"));
        Assert.assertEquals(30, response.getFields().get(0).get("age"));
        Assert.assertEquals("sky-2", response.getFields().get(1).get("skyflow_id"));
        Assert.assertTrue(response.getFields().get(1).containsKey("age"));
        Assert.assertNull(response.getFields().get(1).get("age"));
        Assert.assertEquals(Arrays.asList("skyflow_id", "name", "age"), response.getMetadata().getColumns());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get tokens
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void testGetTokens_everyRecordReachesWireInOrderWithoutCollapsingDuplicates() {
        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("k", "v");
        List<GetTokensRequestRecord> records = Arrays.asList(
                GetTokensRequestRecord.builder().value(NON_ASCII_NAME).tokenGroupName("group one").build(),
                GetTokensRequestRecord.builder().value(42).tokenGroupName("det_number").build(),
                GetTokensRequestRecord.builder().value(structured).tokenGroupName("det_json").build(),
                GetTokensRequestRecord.builder().value(NON_ASCII_NAME).tokenGroupName("group one").build());
        GetTokensRequest request = GetTokensRequest.builder().records(records).build();

        GetTokensFromValuesRequest body = Utils.getGetTokensRequestBody(request, VAULT_ID);

        Assert.assertEquals(VAULT_ID, body.getVaultId());
        Assert.assertEquals(4, body.getRecords().size());
        for (int i = 0; i < records.size(); i++) {
            // arbitrary values must be handed to the wire object untouched
            Assert.assertSame(records.get(i).getValue(), body.getRecords().get(i).getValue().get());
            Assert.assertEquals(records.get(i).getTokenGroupName(),
                    body.getRecords().get(i).getTokenGroupName());
        }
    }

    @Test
    public void testGetTokens_serializesToDocumentedWireShape() throws Exception {
        GetTokensRequest request = GetTokensRequest.builder().records(Arrays.asList(
                GetTokensRequestRecord.builder().value("john@example.com").tokenGroupName("det_email").build(),
                GetTokensRequestRecord.builder().value(42).tokenGroupName("det_number").build())).build();

        String json = ObjectMappers.JSON_MAPPER.writeValueAsString(Utils.getGetTokensRequestBody(request, VAULT_ID));

        Assert.assertEquals("{\"vaultID\":\"" + VAULT_ID + "\",\"records\":["
                + "{\"value\":\"john@example.com\",\"tokenGroupName\":\"det_email\"},"
                + "{\"value\":42,\"tokenGroupName\":\"det_number\"}]}", json);
    }

    @Test
    public void testGetTokens_wireResponseRecordsReachCallerInOrder() throws Exception {
        GetTokensFromValuesResponse wire = ObjectMappers.JSON_MAPPER.readValue(
                "{\"records\":["
                        + "{\"token\":\"1R9kNnLOPM\",\"value\":\"john@example.com\",\"tokenGroupName\":\"det_reg_rtf\",\"error\":\"\",\"httpCode\":200},"
                        + "{\"token\":\"\",\"value\":\"unknown@example.com\",\"tokenGroupName\":\"det_reg_rtf\",\"error\":\"Token not found.\",\"httpCode\":404}]}",
                GetTokensFromValuesResponse.class);

        GetTokensResponse response = Utils.formatGetTokensResponse(wire, new HashMap<>());

        Assert.assertEquals(2, response.getRecords().size());
        Assert.assertEquals("john@example.com", response.getRecords().get(0).get("value"));
        Assert.assertEquals("1R9kNnLOPM", response.getRecords().get(0).get("token"));
        Assert.assertNull(response.getRecords().get(0).get("error"));
        Assert.assertEquals("unknown@example.com", response.getRecords().get(1).get("value"));
        Assert.assertNull(response.getRecords().get(1).get("token"));
        Assert.assertEquals("Token not found.", response.getRecords().get(1).get("error"));
        Assert.assertEquals(404, response.getRecords().get(1).get("httpCode"));
    }
}
