package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for the plain record/redaction data holders: {@link TokenGroupRedactions},
 * {@link InsertRequestRecord}, {@link BulkInsertRequestRecord}
 * and {@link BulkTokenizeRecord}. None of these classes perform
 * validation in their builders, so coverage here is builder-construction plus getters.
 */
public class RecordAndRedactionTests {

    // ── TokenGroupRedactions ─────────────────────────────────────────────────

    @Test
    public void testTokenGroupRedactions_gettersReturnBuilderValues() {
        TokenGroupRedactions redaction = TokenGroupRedactions.builder()
                .tokenGroupName("group1")
                .redaction("MASK")
                .build();

        Assert.assertEquals("group1", redaction.getTokenGroupName());
        Assert.assertEquals("MASK", redaction.getRedaction());
    }

    @Test
    public void testTokenGroupRedactions_defaultsAreNull() {
        TokenGroupRedactions redaction = TokenGroupRedactions.builder().build();
        Assert.assertNull(redaction.getTokenGroupName());
        Assert.assertNull(redaction.getRedaction());
    }

    // BulkTokenGroupRedactions tests removed: the class was deleted; bulk detokenize now reuses
    // TokenGroupRedactions (covered above).

    // ── InsertRequestRecord ─────────────────────────────────────────────────────────

    @Test
    public void testInsertRequestRecord_gettersReturnBuilderValues() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "John");
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "token-value");
        UpsertOptions upsert = UpsertOptions.builder()
                .uniqueColumns(Arrays.asList("id"))
                .updateType("UPDATE")
                .build();

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

    @Test
    public void testInsertRequestRecord_defaultsAreNull() {
        InsertRequestRecord record = InsertRequestRecord.builder().build();
        Assert.assertNull(record.getTableName());
        Assert.assertNull(record.getData());
        Assert.assertNull(record.getTokens());
        Assert.assertNull(record.getUpsert());
    }

    // ── BulkInsertRequestRecord ──────────────────────────────────────────────

    @Test
    public void testBulkInsertRequestRecord_gettersReturnBuilderValues() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Jane");
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "token-1");
        UpsertOptions upsert = UpsertOptions.builder()
                .updateType("REPLACE")
                .uniqueColumns(Arrays.asList("id"))
                .build();

        BulkInsertRequestRecord record = BulkInsertRequestRecord.builder()
                .tableName("persons")
                .data(data)
                .tokens(tokens)
                .upsert(upsert)
                .build();

        Assert.assertEquals("persons", record.getTableName());
        Assert.assertEquals(data, record.getData());
        Assert.assertEquals(tokens, record.getTokens());
        Assert.assertEquals(upsert, record.getUpsert());
        Assert.assertEquals("REPLACE", record.getUpsert().getUpdateType());
        Assert.assertEquals(Arrays.asList("id"), record.getUpsert().getUniqueColumns());
    }

    @Test
    public void testBulkInsertRequestRecord_defaultsAreNull() {
        BulkInsertRequestRecord record = BulkInsertRequestRecord.builder().build();
        Assert.assertNull(record.getTableName());
        Assert.assertNull(record.getData());
        Assert.assertNull(record.getTokens());
        Assert.assertNull(record.getUpsert());
    }

    @Test
    public void testBulkInsertRequestRecord_isAnInsertRequestRecord() {
        InsertRequestRecord record = BulkInsertRequestRecord.builder().tableName("persons").build();
        Assert.assertTrue(record instanceof BulkInsertRequestRecord);
        Assert.assertEquals("persons", record.getTableName());
    }

    // Tests for TokenizeRecord were removed: the class no longer exists (bulk-only module).

    // ── BulkTokenizeRecord ───────────────────────────────────────────────────

    @Test
    public void testBulkTokenizeRecord_gettersReturnBuilderValues() {
        List<String> groups = Arrays.asList("group3");
        BulkTokenizeRecord record = BulkTokenizeRecord.builder()
                .value(12345)
                .tokenGroupNames(groups)
                .build();

        Assert.assertEquals(12345, record.getValue());
        Assert.assertEquals(groups, record.getTokenGroupNames());
    }

    @Test
    public void testBulkTokenizeRecord_defaultsAreNull() {
        BulkTokenizeRecord record = BulkTokenizeRecord.builder().build();
        Assert.assertNull(record.getValue());
        Assert.assertNull(record.getTokenGroupNames());
    }
}
