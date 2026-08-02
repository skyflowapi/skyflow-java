package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for the simple request/response wrapper classes: {@link InsertRequest},
 * {@link InsertResponseRecord}, {@link DetokenizeRequest}, {@link DetokenizeResponseRecord},
 * {@link BulkDeleteTokensRequest}, {@link BulkDetokenizeRequest}, {@link BulkTokenizeRequest}
 * and {@link BulkInsertRequest}.
 */
public class RequestResponseWrapperTests {

    // Tests for DeleteTokensRequest were removed: the class no longer exists (bulk-only module).

    // ── InsertRequest ────────────────────────────────────────────────────────

    @Test
    public void testInsertRequest_gettersReturnBuilderValues() {
        ArrayList<InsertRequestRecord> records = new ArrayList<>(Collections.singletonList(
                InsertRequestRecord.builder().tableName("persons").build()));
        UpsertOptions upsert = UpsertOptions.builder()
                .uniqueColumns(Arrays.asList("id"))
                .updateType("UPDATE")
                .build();

        InsertRequest request = InsertRequest.builder()
                .upsert(upsert)
                .records(records)
                .build();

        Assert.assertEquals(upsert, request.getUpsert());
        Assert.assertEquals(records, request.getRecords());
    }

    @Test
    public void testInsertRequest_tableNameGetterReturnsBuilderValue() {
        InsertRequest request = InsertRequest.builder().tableName("persons").build();
        Assert.assertEquals("persons", request.getTableName());
    }

    @Test
    public void testInsertRequest_defaultsAreNull() {
        InsertRequest request = InsertRequest.builder().build();
        Assert.assertNull(request.getUpsert());
        Assert.assertNull(request.getRecords());
        Assert.assertNull(request.getTableName());
    }

    // ── InsertRequestRecord ─────────────────────────────────────────────────────────

    @Test
    public void testInsertRequestRecord_gettersReturnBuilderValues() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "tok-abc");
        UpsertOptions upsert = UpsertOptions.builder().uniqueColumns(Arrays.asList("id")).build();

        InsertRequestRecord record = InsertRequestRecord.builder()
                .tableName("persons")
                .data(data)
                .tokens(tokens)
                .upsert(upsert)
                .build();

        Assert.assertEquals("persons", record.getTableName());
        Assert.assertEquals(data, record.getData());
        Assert.assertEquals(tokens, record.getTokens());
        Assert.assertEquals(upsert, record.getUpsert());
    }

    // ── UpsertOptions ────────────────────────────────────────────────────────

    @Test
    public void testUpsertOptions_gettersReturnBuilderValues() {
        List<String> uniqueColumns = Arrays.asList("id", "email");
        UpsertOptions upsert = UpsertOptions.builder()
                .updateType("REPLACE")
                .uniqueColumns(uniqueColumns)
                .build();

        Assert.assertEquals("REPLACE", upsert.getUpdateType());
        Assert.assertEquals(uniqueColumns, upsert.getUniqueColumns());
    }

    // Tests for InsertResponse were removed: the class no longer exists (bulk-only module).

    // ── InsertResponseRecord ─────────────────────────────────────────────────

    @Test
    public void testInsertResponseRecord_gettersReturnConstructorValues() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", "tok-abc");
        Map<String, Object> hashedData = new HashMap<>();
        hashedData.put("name", "hashed-value");

        InsertResponseRecord record = new InsertResponseRecord(
                "persons", "id-1", fields, hashedData, 200, null);

        Assert.assertEquals("persons", record.getTableName());
        Assert.assertEquals("id-1", record.getSkyflowId());
        Assert.assertEquals(fields, record.getFields());
        Assert.assertEquals(hashedData, record.getHashedData());
        Assert.assertEquals(200, record.getHttpCode());
        Assert.assertNull(record.getError());
    }

    @Test
    public void testInsertResponseRecord_errorCase() {
        InsertResponseRecord record = new InsertResponseRecord(
                "persons", null, null, null, 400, "insert failed");

        Assert.assertEquals("persons", record.getTableName());
        Assert.assertNull(record.getSkyflowId());
        Assert.assertEquals("insert failed", record.getError());
        Assert.assertEquals(400, record.getHttpCode());
    }

    // Tests for TokenizeRequest / TokenizeResponse were removed: those classes no longer exist (bulk-only module).

    // ── DetokenizeRequest ────────────────────────────────────────────────────

    @Test
    public void testDetokenizeRequest_gettersReturnBuilderValues() {
        List<String> tokens = Collections.singletonList("tok-1");
        List<TokenGroupRedactions> redactions = Collections.singletonList(
                TokenGroupRedactions.builder().tokenGroupName("group1").redaction("MASK").build());

        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(tokens)
                .tokenGroupRedactions(redactions)
                .build();

        Assert.assertEquals(tokens, request.getTokens());
        Assert.assertEquals(redactions, request.getTokenGroupRedactions());
    }

    @Test
    public void testDetokenizeRequest_defaultsAreNull() {
        DetokenizeRequest request = DetokenizeRequest.builder().build();
        Assert.assertNull(request.getTokens());
        Assert.assertNull(request.getTokenGroupRedactions());
    }

    // Tests for DetokenizeResponse were removed: the class no longer exists (bulk-only module).

    // ── DetokenizeResponseRecord ─────────────────────────────────────────────

    @Test
    public void testDetokenizeResponseRecord_gettersReturnConstructorValues() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key", "value");

        DetokenizeResponseRecord response = new DetokenizeResponseRecord(
                "tok-1", "secret-value", "group1", metadata, 200, null);

        Assert.assertEquals("tok-1", response.getToken());
        Assert.assertNull(response.getError());
        Assert.assertEquals("group1", response.getTokenGroupName());
        Assert.assertEquals(metadata, response.getMetadata());
        Assert.assertEquals(200, response.getHttpCode());
    }

    @Test
    public void testDetokenizeResponseRecord_errorCase() {
        DetokenizeResponseRecord response = new DetokenizeResponseRecord(
                "tok-2", null, null, null, 404, "Token not found");

        Assert.assertEquals("tok-2", response.getToken());
        Assert.assertEquals("Token not found", response.getError());
        Assert.assertNull(response.getTokenGroupName());
        Assert.assertNull(response.getMetadata());
        Assert.assertEquals(404, response.getHttpCode());
    }

    // ── BulkDeleteTokensRequest ──────────────────────────────────────────────

    @Test
    public void testBulkDeleteTokensRequest_getterReturnsBuilderValue() {
        List<String> tokens = Arrays.asList("tok-1", "tok-2");
        BulkDeleteTokensRequest request = BulkDeleteTokensRequest.builder().tokens(tokens).build();
        Assert.assertEquals(tokens, request.getTokens());
    }

    @Test
    public void testBulkDeleteTokensRequest_defaultIsNull() {
        BulkDeleteTokensRequest request = BulkDeleteTokensRequest.builder().build();
        Assert.assertNull(request.getTokens());
    }

    // ── BulkDetokenizeRequest ────────────────────────────────────────────────

    @Test
    public void testBulkDetokenizeRequest_gettersReturnBuilderValues() {
        List<String> tokens = Arrays.asList("tok-1", "tok-2");
        List<TokenGroupRedactions> redactions = Collections.singletonList(
                TokenGroupRedactions.builder().tokenGroupName("group1").redaction("MASK").build());

        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder()
                .tokens(tokens)
                .tokenGroupRedactions(redactions)
                .build();

        Assert.assertEquals(tokens, request.getTokens());
        Assert.assertEquals(redactions, request.getTokenGroupRedactions());
    }

    @Test
    public void testBulkDetokenizeRequest_defaultsAreNull() {
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder().build();
        Assert.assertNull(request.getTokens());
        Assert.assertNull(request.getTokenGroupRedactions());
    }

    @Test
    public void testBulkDetokenizeRequest_isADetokenizeRequest() {
        // Bulk detokenize now shares the unary request contract; all state is inherited.
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder()
                .tokens(Collections.singletonList("tok-1"))
                .build();
        DetokenizeRequest asUnary = request;
        Assert.assertEquals(Collections.singletonList("tok-1"), asUnary.getTokens());
    }

    // ── BulkTokenizeRequest ──────────────────────────────────────────────────

    @Test
    public void testBulkTokenizeRequest_getterReturnsBuilderValue() {
        ArrayList<BulkTokenizeRecord> data = new ArrayList<>(Collections.singletonList(
                BulkTokenizeRecord.builder().value("v1").build()));
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().data(data).build();
        Assert.assertEquals(data, request.getData());
    }

    @Test
    public void testBulkTokenizeRequest_defaultIsNull() {
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().build();
        Assert.assertNull(request.getData());
    }

    // ── BulkInsertRequest ────────────────────────────────────────────────────

    @Test
    public void testBulkInsertRequest_gettersReturnBuilderValues() {
        List<InsertRequestRecord> records = new ArrayList<>(Collections.singletonList(
                BulkInsertRequestRecord.builder().tableName("persons").build()));
        UpsertOptions upsert = UpsertOptions.builder()
                .updateType("REPLACE")
                .uniqueColumns(Arrays.asList("id"))
                .build();

        BulkInsertRequest request = BulkInsertRequest.builder()
                .tableName("persons")
                .upsert(upsert)
                .records(records)
                .build();

        Assert.assertEquals("persons", request.getTableName());
        Assert.assertEquals(upsert, request.getUpsert());
        Assert.assertEquals("REPLACE", request.getUpsert().getUpdateType());
        Assert.assertEquals(Arrays.asList("id"), request.getUpsert().getUniqueColumns());
        Assert.assertEquals(records, request.getRecords());
    }

    @Test
    public void testBulkInsertRequest_defaultsAreNull() {
        BulkInsertRequest request = BulkInsertRequest.builder().build();
        Assert.assertNull(request.getTableName());
        Assert.assertNull(request.getUpsert());
        Assert.assertNull(request.getRecords());
    }

    @Test
    public void testBulkInsertRequest_isAnInsertRequest() {
        InsertRequest request = BulkInsertRequest.builder().tableName("persons").build();
        Assert.assertTrue(request instanceof BulkInsertRequest);
        Assert.assertEquals("persons", request.getTableName());
    }

    // Tests for DeleteTokensResponse were removed: the class no longer exists (bulk-only module).

}
