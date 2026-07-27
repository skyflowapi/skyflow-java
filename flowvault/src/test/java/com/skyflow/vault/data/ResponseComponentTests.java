package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for the response/success/summary building-block classes that carry real
 * constructor logic or toString() serialization: {@link Success}, {@link Summary},
 * {@link Token}, {@link TokenizeData}, {@link TokenizeSuccess}, {@link TokenizeSummary},
 * {@link DeleteTokensSuccess}, {@link DeleteTokensSummary}, {@link DetokenizeSummary},
 * {@link ErrorRecord} and {@link DetokenizeResponseObject}.
 */
public class ResponseComponentTests {

    // ── Success ──────────────────────────────────────────────────────────────

    @Test
    public void testSuccess_gettersReturnConstructorValues() {
        Map<String, List<Token>> tokens = new HashMap<>();
        tokens.put("group1", Collections.singletonList(new Token("tok-1", "group1")));
        Map<String, Object> data = new HashMap<>();
        data.put("name", "John");

        Success success = new Success(2, "skyflow-id-1", tokens, data, "persons");

        Assert.assertEquals(2, success.getIndex());
        Assert.assertEquals("skyflow-id-1", success.getSkyflowId());
        Assert.assertEquals(tokens, success.getTokens());
        Assert.assertEquals(data, success.getData());
        Assert.assertEquals("persons", success.getTable());
    }

    @Test
    public void testSuccess_toStringContainsSkyflowId() {
        Success success = new Success(0, "skyflow-id-2", new HashMap<>(), new HashMap<>(), "persons");
        String json = success.toString();
        Assert.assertNotNull(json);
        Assert.assertTrue(json.contains("skyflow-id-2"));
    }

    // ── Summary ──────────────────────────────────────────────────────────────

    @Test
    public void testSummary_noArgConstructorDefaultsToZero() {
        Summary summary = new Summary();
        Assert.assertEquals(0, summary.getTotalRecords());
        Assert.assertEquals(0, summary.getTotalInserted());
        Assert.assertEquals(0, summary.getTotalFailed());
    }

    @Test
    public void testSummary_allArgConstructor() {
        Summary summary = new Summary(10, 8, 2);
        Assert.assertEquals(10, summary.getTotalRecords());
        Assert.assertEquals(8, summary.getTotalInserted());
        Assert.assertEquals(2, summary.getTotalFailed());
    }

    @Test
    public void testSummary_toStringNotNull() {
        Assert.assertNotNull(new Summary(1, 1, 0).toString());
    }

    // ── Token ────────────────────────────────────────────────────────────────

    @Test
    public void testToken_gettersReturnConstructorValues() {
        Token token = new Token("tok-value", "group-name");
        Assert.assertEquals("tok-value", token.getToken());
        Assert.assertEquals("group-name", token.getTokenGroupName());
    }

    // ── TokenizeData ─────────────────────────────────────────────────────────
    // Note: TokenizeData has an `index` field set in the constructor but exposes
    // no getter for it - only getValue()/getTokens() are accessible.

    @Test
    public void testTokenizeData_getValueAndEmptyTokensOnConstruction() {
        TokenizeData data = new TokenizeData("plain-value", 0);
        Assert.assertEquals("plain-value", data.getValue());
        Assert.assertNotNull(data.getTokens());
        Assert.assertTrue(data.getTokens().isEmpty());
    }

    @Test
    public void testTokenizeData_addTokenPopulatesTokensMap() {
        TokenizeData data = new TokenizeData("plain-value", 1);
        data.addToken("group1", "tok-1");
        data.addToken("group2", "tok-2");

        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("group1", "tok-1");
        expected.put("group2", "tok-2");
        Assert.assertEquals(expected, data.getTokens());
    }

    @Test
    public void testTokenizeData_toStringNotNull() {
        TokenizeData data = new TokenizeData("value", 0);
        Assert.assertNotNull(data.toString());
    }

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

    // ── DetokenizeResponseObject ─────────────────────────────────────────────

    @Test
    public void testDetokenizeResponseObject_gettersReturnConstructorValues() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key", "value");

        DetokenizeResponseObject obj = new DetokenizeResponseObject(
                4, "tok-1", "plain-value", "group1", null, metadata);

        Assert.assertEquals(4, obj.getIndex());
        Assert.assertEquals("tok-1", obj.getToken());
        Assert.assertEquals("plain-value", obj.getValue());
        Assert.assertEquals("group1", obj.getTokenGroupName());
        Assert.assertNull(obj.getError());
        Assert.assertEquals(metadata, obj.getMetadata());
    }

    @Test
    public void testDetokenizeResponseObject_errorCase() {
        DetokenizeResponseObject obj = new DetokenizeResponseObject(
                0, "tok-2", null, null, "Token not found", null);

        Assert.assertEquals("Token not found", obj.getError());
        Assert.assertNull(obj.getValue());
        Assert.assertNull(obj.getTokenGroupName());
        Assert.assertNull(obj.getMetadata());
    }

    @Test
    public void testDetokenizeResponseObject_toStringNotNull() {
        DetokenizeResponseObject obj = new DetokenizeResponseObject(0, "tok", "v", "g", null, null);
        Assert.assertNotNull(obj.toString());
    }
}
