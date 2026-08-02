package com.skyflow.vault.data;

import com.skyflow.enums.UpsertType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for the plain record/redaction data holders: {@link TokenGroupRedactions},
 * {@link BulkTokenGroupRedactions}, {@link InsertRecord}, {@link BulkInsertRecord},
 * {@link TokenizeRequestRecord} and {@link BulkTokenizeRequestRecord}. None of these classes perform
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

    // ── BulkTokenGroupRedactions ─────────────────────────────────────────────

    @Test
    public void testBulkTokenGroupRedactions_gettersReturnBuilderValues() {
        BulkTokenGroupRedactions redaction = BulkTokenGroupRedactions.builder()
                .tokenGroupName("group2")
                .redaction("REDACT")
                .build();

        Assert.assertEquals("group2", redaction.getTokenGroupName());
        Assert.assertEquals("REDACT", redaction.getRedaction());
    }

    @Test
    public void testBulkTokenGroupRedactions_defaultsAreNull() {
        BulkTokenGroupRedactions redaction = BulkTokenGroupRedactions.builder().build();
        Assert.assertNull(redaction.getTokenGroupName());
        Assert.assertNull(redaction.getRedaction());
    }

    // ── InsertRecord ─────────────────────────────────────────────────────────

    @Test
    public void testInsertRecord_gettersReturnBuilderValues() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "John");
        List<String> upsert = Arrays.asList("id");

        InsertRecord record = InsertRecord.builder()
                .table("persons")
                .data(data)
                .upsert(upsert)
                .upsertType(UpsertType.UPDATE)
                .build();

        Assert.assertEquals("persons", record.getTable());
        Assert.assertEquals(data, record.getData());
        Assert.assertEquals(upsert, record.getUpsert());
        Assert.assertEquals(UpsertType.UPDATE, record.getUpsertType());
    }

    @Test
    public void testInsertRecord_defaultsAreNull() {
        InsertRecord record = InsertRecord.builder().build();
        Assert.assertNull(record.getTable());
        Assert.assertNull(record.getData());
        Assert.assertNull(record.getUpsert());
        Assert.assertNull(record.getUpsertType());
    }

    // ── BulkInsertRecord ─────────────────────────────────────────────────────

    @Test
    public void testBulkInsertRecord_gettersReturnBuilderValues() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Jane");
        List<String> upsert = Arrays.asList("id");

        BulkInsertRecord record = BulkInsertRecord.builder()
                .table("persons")
                .data(data)
                .upsert(upsert)
                .upsertType(UpsertType.REPLACE)
                .build();

        Assert.assertEquals("persons", record.getTable());
        Assert.assertEquals(data, record.getData());
        Assert.assertEquals(upsert, record.getUpsert());
        Assert.assertEquals(UpsertType.REPLACE, record.getUpsertType());
    }

    @Test
    public void testBulkInsertRecord_defaultsAreNull() {
        BulkInsertRecord record = BulkInsertRecord.builder().build();
        Assert.assertNull(record.getTable());
        Assert.assertNull(record.getData());
        Assert.assertNull(record.getUpsert());
        Assert.assertNull(record.getUpsertType());
    }

    // ── TokenizeRequestRecord ───────────────────────────────────────────────────────

    @Test
    public void testTokenizeRecord_gettersReturnBuilderValues() {
        List<String> groups = Arrays.asList("group1", "group2");
        TokenizeRequestRecord record = TokenizeRequestRecord.builder()
                .value("secret-value")
                .tokenGroupNames(groups)
                .build();

        Assert.assertEquals("secret-value", record.getValue());
        Assert.assertEquals(groups, record.getTokenGroupNames());
    }

    @Test
    public void testTokenizeRecord_defaultsAreNull() {
        TokenizeRequestRecord record = TokenizeRequestRecord.builder().build();
        Assert.assertNull(record.getValue());
        Assert.assertNull(record.getTokenGroupNames());
    }

    // ── BulkTokenizeRequestRecord ───────────────────────────────────────────────────

    @Test
    public void testBulkTokenizeRecord_gettersReturnBuilderValues() {
        List<String> groups = Arrays.asList("group3");
        BulkTokenizeRequestRecord record = BulkTokenizeRequestRecord.builder()
                .value(12345)
                .token("byot-token")
                .tokenGroupNames(groups)
                .build();

        Assert.assertEquals(12345, record.getValue());
        Assert.assertEquals("byot-token", record.getToken());
        Assert.assertEquals(groups, record.getTokenGroupNames());
    }

    @Test
    public void testBulkTokenizeRecord_defaultsAreNull() {
        BulkTokenizeRequestRecord record = BulkTokenizeRequestRecord.builder().build();
        Assert.assertNull(record.getValue());
        Assert.assertNull(record.getToken());
        Assert.assertNull(record.getTokenGroupNames());
    }

    @Test
    public void testBulkTokenizeRecord_isATokenizeRequestRecord() {
        // the bulk record adds nothing today; it must still satisfy the parent contract
        BulkTokenizeRequestRecord record = BulkTokenizeRequestRecord.builder().value("v1").build();
        Assert.assertTrue(record instanceof TokenizeRequestRecord);
    }
}
