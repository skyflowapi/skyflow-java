package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tests for the response/success/summary building-block classes that carry real
 * constructor logic or toString() serialization: {@link BulkInsertResponseRecord},
 * {@link BulkSummary}, {@link TokenizeSuccess}, {@link TokenizeSummary},
 * {@link DeleteTokensSuccess}, {@link DeleteTokensSummary}, {@link DetokenizeSummary},
 * {@link ErrorRecord} and {@link BulkDetokenizeResponseRecord}.
 */
public class ResponseComponentTests {

    // Tests for Success, Summary and Token were removed: the bulk insert response contract
    // replaced those classes with BulkInsertResponseRecord / BulkSummary, covered below.

    // ── BulkInsertResponseRecord ─────────────────────────────────────────────

    @Test
    public void testBulkInsertResponseRecord_gettersReturnConstructorValues() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", "tok-1");
        Map<String, Object> hashedData = new HashMap<>();
        hashedData.put("name", "hashed-1");

        BulkInsertResponseRecord record = new BulkInsertResponseRecord(
                2, "persons", "skyflow-id-1", fields, hashedData, 200, null);

        Assert.assertEquals(2, record.getIndex());
        Assert.assertEquals("persons", record.getTableName());
        Assert.assertEquals("skyflow-id-1", record.getSkyflowId());
        Assert.assertEquals(fields, record.getFields());
        Assert.assertEquals(hashedData, record.getHashedData());
        Assert.assertEquals(200, record.getHttpCode());
        Assert.assertNull(record.getError());
    }

    @Test
    public void testBulkInsertResponseRecord_errorCase() {
        BulkInsertResponseRecord record = new BulkInsertResponseRecord(
                3, null, null, null, null, 500, "Internal Server Error");

        Assert.assertEquals(3, record.getIndex());
        Assert.assertEquals(500, record.getHttpCode());
        Assert.assertEquals("Internal Server Error", record.getError());
        Assert.assertNull(record.getTableName());
        Assert.assertNull(record.getSkyflowId());
        Assert.assertNull(record.getFields());
        Assert.assertNull(record.getHashedData());
    }

    @Test
    public void testBulkInsertResponseRecord_toStringSerializesNulls() {
        BulkInsertResponseRecord record = new BulkInsertResponseRecord(
                0, "persons", "skyflow-id-2", null, null, 200, null);
        String json = record.toString();
        Assert.assertNotNull(json);
        Assert.assertTrue(json.contains("skyflow-id-2"));
        Assert.assertTrue(json.contains("\"index\":0"));
        Assert.assertTrue(json.contains("\"fields\":null"));
    }

    // ── BulkSummary ──────────────────────────────────────────────────────────

    @Test
    public void testBulkSummary_noArgConstructorDefaultsToZero() {
        BulkSummary summary = new BulkSummary();
        Assert.assertEquals(0, summary.getTotalRecords());
        Assert.assertEquals(0, summary.getTotalInserted());
        Assert.assertEquals(0, summary.getTotalFailed());
    }

    @Test
    public void testBulkSummary_allArgConstructor() {
        BulkSummary summary = new BulkSummary(10, 8, 2);
        Assert.assertEquals(10, summary.getTotalRecords());
        Assert.assertEquals(8, summary.getTotalInserted());
        Assert.assertEquals(2, summary.getTotalFailed());
    }

    @Test
    public void testBulkSummary_toStringNotNull() {
        Assert.assertNotNull(new BulkSummary(1, 1, 0).toString());
    }

    // Tests for TokenizeData were removed: the class no longer exists (bulk-only module).

    // ── TokenizeSuccess ──────────────────────────────────────────────────────

    @Test
    public void testTokenizeSuccess_getIndexAndValueAndEmptyTokensOnConstruction() {
        TokenizeSuccess success = new TokenizeSuccess(3, "value-1");
        Assert.assertEquals(3, success.getIndex());
        Assert.assertEquals("value-1", success.getValue());
        Assert.assertNotNull(success.getTokens());
        Assert.assertTrue(success.getTokens().isEmpty());
    }

    @Test
    public void testTokenizeSuccess_addTokenPopulatesTokensMap() {
        TokenizeSuccess success = new TokenizeSuccess(0, "value-1");
        success.addToken("group1", "tok-1");
        Assert.assertEquals("tok-1", success.getTokens().get("group1"));
    }

    @Test
    public void testTokenizeSuccess_toStringNotNull() {
        Assert.assertNotNull(new TokenizeSuccess(0, "value").toString());
    }

    // ── TokenizeSummary ──────────────────────────────────────────────────────

    @Test
    public void testTokenizeSummary_noArgConstructorDefaultsToZero() {
        TokenizeSummary summary = new TokenizeSummary();
        Assert.assertEquals(0, summary.getTotalTokens());
        Assert.assertEquals(0, summary.getTotalTokenized());
        Assert.assertEquals(0, summary.getTotalPartial());
        Assert.assertEquals(0, summary.getTotalFailed());
    }

    @Test
    public void testTokenizeSummary_allArgConstructor() {
        TokenizeSummary summary = new TokenizeSummary(10, 5, 3, 2);
        Assert.assertEquals(10, summary.getTotalTokens());
        Assert.assertEquals(5, summary.getTotalTokenized());
        Assert.assertEquals(3, summary.getTotalPartial());
        Assert.assertEquals(2, summary.getTotalFailed());
    }

    @Test
    public void testTokenizeSummary_toStringNotNull() {
        Assert.assertNotNull(new TokenizeSummary(1, 1, 0, 0).toString());
    }

    // ── DeleteTokensSuccess ──────────────────────────────────────────────────

    @Test
    public void testDeleteTokensSuccess_gettersReturnConstructorValues() {
        DeleteTokensSuccess success = new DeleteTokensSuccess(1, "tok-1");
        Assert.assertEquals(1, success.getIndex());
        Assert.assertEquals("tok-1", success.getToken());
    }

    @Test
    public void testDeleteTokensSuccess_toStringNotNull() {
        Assert.assertNotNull(new DeleteTokensSuccess(0, "tok-1").toString());
    }

    // ── DeleteTokensSummary ──────────────────────────────────────────────────

    @Test
    public void testDeleteTokensSummary_noArgConstructorDefaultsToZero() {
        DeleteTokensSummary summary = new DeleteTokensSummary();
        Assert.assertEquals(0, summary.getTotalTokens());
        Assert.assertEquals(0, summary.getTotalDeleted());
        Assert.assertEquals(0, summary.getTotalFailed());
    }

    @Test
    public void testDeleteTokensSummary_allArgConstructor() {
        DeleteTokensSummary summary = new DeleteTokensSummary(5, 4, 1);
        Assert.assertEquals(5, summary.getTotalTokens());
        Assert.assertEquals(4, summary.getTotalDeleted());
        Assert.assertEquals(1, summary.getTotalFailed());
    }

    @Test
    public void testDeleteTokensSummary_toStringNotNull() {
        Assert.assertNotNull(new DeleteTokensSummary(1, 1, 0).toString());
    }

    // ── DetokenizeSummary ────────────────────────────────────────────────────

    @Test
    public void testDetokenizeSummary_noArgConstructorDefaultsToZero() {
        DetokenizeSummary summary = new DetokenizeSummary();
        Assert.assertEquals(0, summary.getTotalTokens());
        Assert.assertEquals(0, summary.getTotalDetokenized());
        Assert.assertEquals(0, summary.getTotalFailed());
    }

    @Test
    public void testDetokenizeSummary_allArgConstructor() {
        DetokenizeSummary summary = new DetokenizeSummary(6, 5, 1);
        Assert.assertEquals(6, summary.getTotalTokens());
        Assert.assertEquals(5, summary.getTotalDetokenized());
        Assert.assertEquals(1, summary.getTotalFailed());
    }

    @Test
    public void testDetokenizeSummary_toStringNotNull() {
        Assert.assertNotNull(new DetokenizeSummary(1, 1, 0).toString());
    }

    // ── ErrorRecord ──────────────────────────────────────────────────────────

    @Test
    public void testErrorRecord_threeArgConstructorLeavesRequestIdNull() {
        ErrorRecord error = new ErrorRecord(0, "Not Found", 404);
        Assert.assertEquals(0, error.getIndex());
        Assert.assertEquals("Not Found", error.getError());
        Assert.assertEquals(404, error.getCode());
        Assert.assertNull(error.getRequestId());
    }

    @Test
    public void testErrorRecord_fourArgConstructorSetsRequestId() {
        ErrorRecord error = new ErrorRecord(1, "Server Error", 500, "req-123");
        Assert.assertEquals(1, error.getIndex());
        Assert.assertEquals("Server Error", error.getError());
        Assert.assertEquals(500, error.getCode());
        Assert.assertEquals("req-123", error.getRequestId());
    }

    @Test
    public void testErrorRecord_toStringNotNull() {
        Assert.assertNotNull(new ErrorRecord(0, "err", 400).toString());
    }

    // DetokenizeResponseObject tests removed: the class was deleted; the bulk detokenize response
    // contract replaced it with BulkDetokenizeResponseRecord, covered below.

    // ── BulkDetokenizeResponseRecord ─────────────────────────────────────────

    @Test
    public void testBulkDetokenizeResponseRecord_gettersReturnConstructorValues() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key", "value");

        BulkDetokenizeResponseRecord record = new BulkDetokenizeResponseRecord(
                4, "tok-1", "secret-value", "group1", metadata, 200, null);

        Assert.assertEquals(4, record.getIndex());
        Assert.assertEquals("tok-1", record.getToken());
        Assert.assertEquals("group1", record.getTokenGroupName());
        Assert.assertEquals(metadata, record.getMetadata());
        Assert.assertEquals(200, record.getHttpCode());
        Assert.assertNull(record.getError());
    }

    @Test
    public void testBulkDetokenizeResponseRecord_errorCase() {
        BulkDetokenizeResponseRecord record = new BulkDetokenizeResponseRecord(
                0, "tok-2", null, null, null, 404, "Token not found");

        Assert.assertEquals(0, record.getIndex());
        Assert.assertEquals("Token not found", record.getError());
        Assert.assertEquals(404, record.getHttpCode());
        Assert.assertNull(record.getTokenGroupName());
        Assert.assertNull(record.getMetadata());
    }

    @Test
    public void testBulkDetokenizeResponseRecord_isADetokenizeResponseRecord() {
        BulkDetokenizeResponseRecord record = new BulkDetokenizeResponseRecord(
                1, "tok", "plain", "group", null, 200, null);
        Assert.assertTrue(record instanceof DetokenizeResponseRecord);
    }

    @Test
    public void testBulkDetokenizeResponseRecord_toStringSerializesNulls() {
        BulkDetokenizeResponseRecord record = new BulkDetokenizeResponseRecord(
                2, "tok", null, null, null, 200, null);
        String json = record.toString();

        Assert.assertNotNull(json);
        Assert.assertTrue(json.contains("\"index\":2"));
        Assert.assertTrue(json.contains("\"token\":\"tok\""));
        Assert.assertTrue(json.contains("\"error\":null"));
    }
}
