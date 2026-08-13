package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for the response/success/summary building-block classes that carry real
 * constructor logic or toString() serialization: {@link Token}, {@link TokenizeResponseToken},
 * {@link TokenizeResponseRecord}, {@link BulkTokenizeResponseRecord}, {@link TokenizeSummary},
 * {@link DeleteTokensRecord}, {@link BulkDeleteTokensResponseRecord},
 * {@link DeleteTokensSummary}, {@link DetokenizeSummary},
 * {@link ErrorRecord} and {@link DetokenizeResponseObject}.
 */
public class ResponseComponentTests {

    // Tests for Success and Summary were removed: the bulk insert response contract replaced
    // those classes with BulkInsertResponseRecord / BulkSummary, covered below. Token was removed
    // in the same rework, then reintroduced (with the same shape it had before) as the type
    // InsertResponseRecord.getTokens() now returns - see the InsertResponseRecord section below.

    // ── BulkInsertResponseRecord ─────────────────────────────────────────────

    @Test
    public void testBulkInsertResponseRecord_gettersReturnConstructorValues() {
        Map<String, List<Token>> tokens = new HashMap<>();
        tokens.put("name", Collections.singletonList(new Token("tok-1", "group1")));
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        Map<String, Object> hashedData = new HashMap<>();
        hashedData.put("name", "hashed-1");

        BulkInsertResponseRecord record = new BulkInsertResponseRecord(
                2, "persons", "skyflow-id-1", tokens, data, hashedData, 200, null, null);

        Assert.assertEquals(2, record.getIndex());
        Assert.assertEquals("persons", record.getTableName());
        Assert.assertEquals("skyflow-id-1", record.getSkyflowId());
        Assert.assertEquals(tokens, record.getTokens());
        // getFields() is deprecated but still delegates to getTokens() for backward compatibility.
        Assert.assertEquals(tokens, record.getFields());
        Assert.assertEquals(data, record.getData());
        Assert.assertEquals(hashedData, record.getHashedData());
        Assert.assertEquals(200, record.getHttpCode());
        Assert.assertNull(record.getError());
        // getTokens() is inherited unchanged from InsertResponseRecord - confirm it works on the
        // subclass callers actually receive, not just the base class.
        Assert.assertEquals("tok-1", record.getTokens().get("name").get(0).getToken());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testBulkInsertResponseRecord_deprecatedConstructorAndGetFieldsStillWork() {
        Map<String, List<Token>> tokens = new HashMap<>();
        tokens.put("name", Collections.singletonList(new Token("tok-1", "group1")));
        Map<String, Object> hashedData = new HashMap<>();
        hashedData.put("name", "hashed-1");

        // The pre-existing (data-less) constructor overload and getFields() are both deprecated,
        // but must keep working unchanged for callers who haven't migrated yet.
        BulkInsertResponseRecord record = new BulkInsertResponseRecord(
                2, "persons", "skyflow-id-1", tokens, hashedData, 200, null, null);

        Assert.assertEquals(tokens, record.getTokens());
        Assert.assertEquals(tokens, record.getFields());
        Assert.assertNull(record.getData());
        Assert.assertEquals(hashedData, record.getHashedData());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testInsertResponseRecord_deprecatedConstructorDefaultsDataToNull() {
        // BulkInsertResponseRecord's deprecated constructor delegates straight to the new
        // 7-arg super constructor, so it never exercises InsertResponseRecord's own deprecated
        // 6-arg constructor. Cover that one directly.
        Map<String, List<Token>> tokens = new HashMap<>();
        tokens.put("name", Collections.singletonList(new Token("tok-1", "group1")));
        Map<String, Object> hashedData = new HashMap<>();
        hashedData.put("name", "hashed-1");

        InsertResponseRecord record = new InsertResponseRecord(
                "persons", "skyflow-id-1", tokens, hashedData, 200, null);

        Assert.assertEquals("persons", record.getTableName());
        Assert.assertEquals("skyflow-id-1", record.getSkyflowId());
        Assert.assertEquals(tokens, record.getTokens());
        Assert.assertEquals(tokens, record.getFields());
        Assert.assertNull(record.getData());
        Assert.assertEquals(hashedData, record.getHashedData());
        Assert.assertEquals(200, record.getHttpCode());
        Assert.assertNull(record.getError());
    }

    @Test
    public void testBulkInsertResponseRecord_errorCase() {
        BulkInsertResponseRecord record = new BulkInsertResponseRecord(
                3, null, null, null, null, null, 500, "Internal Server Error", null);

        Assert.assertEquals(3, record.getIndex());
        Assert.assertEquals(500, record.getHttpCode());
        Assert.assertEquals("Internal Server Error", record.getError());
        Assert.assertNull(record.getTableName());
        Assert.assertNull(record.getSkyflowId());
        Assert.assertNull(record.getTokens());
        Assert.assertNull(record.getFields());
        Assert.assertNull(record.getData());
        Assert.assertNull(record.getHashedData());
    }

    @Test
    public void testBulkInsertResponseRecord_toStringSerializesNulls() {
        BulkInsertResponseRecord record = new BulkInsertResponseRecord(
                0, "persons", "skyflow-id-2", null, null, null, 200, null, null);
        String json = record.toString();
        Assert.assertNotNull(json);
        Assert.assertTrue(json.contains("skyflow-id-2"));
        Assert.assertTrue(json.contains("\"index\":0"));
        Assert.assertTrue(json.contains("\"tokens\":null"));
        Assert.assertTrue(json.contains("\"data\":null"));
    }

    // ── Token / Token.parseTokens() ───────────────────────────────────────────

    @Test
    public void testToken_gettersReturnConstructorValues() {
        Token token = new Token("tok-1", "group1");
        Assert.assertEquals("tok-1", token.getToken());
        Assert.assertEquals("group1", token.getTokenGroupName());
    }

    @Test
    public void testToken_toStringSerializesFields() {
        Token token = new Token("tok-1", "group1");
        String json = token.toString();
        Assert.assertTrue(json.contains("tok-1"));
        Assert.assertTrue(json.contains("group1"));
    }

    @Test
    public void testParseTokens_returnsNullWhenRawTokensIsNull() {
        Assert.assertNull(Token.parseTokens(null));
    }

    @Test
    public void testParseTokens_parsesAListOfTokenGroupEntriesPerColumn() {
        // The real, tested API shape for a column tokenized against more than one group -
        // see VaultControllerTests.testBulkInsert_successWithListOfMapsTokenShape.
        Map<String, Object> entry1 = new HashMap<>();
        entry1.put("token", "tok-a");
        entry1.put("tokenGroupName", "tg1");
        Map<String, Object> entry2 = new HashMap<>();
        entry2.put("token", "tok-b");
        entry2.put("tokenGroupName", "tg2");

        Map<String, Object> rawTokens = new HashMap<>();
        rawTokens.put("col1", Arrays.asList(entry1, entry2));

        List<Token> col1 = Token.parseTokens(rawTokens).get("col1");
        Assert.assertEquals(2, col1.size());
        Assert.assertEquals("tok-a", col1.get(0).getToken());
        Assert.assertEquals("tg1", col1.get(0).getTokenGroupName());
        Assert.assertEquals("tok-b", col1.get(1).getToken());
        Assert.assertEquals("tg2", col1.get(1).getTokenGroupName());
    }

    @Test
    public void testParseTokens_parsesASingleTokenGroupEntryNotWrappedInAList() {
        Map<String, Object> entry = new HashMap<>();
        entry.put("token", "tok-a");
        entry.put("tokenGroupName", "tg1");

        Map<String, Object> rawTokens = new HashMap<>();
        rawTokens.put("col1", entry);

        List<Token> col1 = Token.parseTokens(rawTokens).get("col1");
        Assert.assertEquals(1, col1.size());
        Assert.assertEquals("tok-a", col1.get(0).getToken());
        Assert.assertEquals("tg1", col1.get(0).getTokenGroupName());
    }

    @Test
    public void testParseTokens_parsesABareTokenValueWithNoGroupInfo() {
        Map<String, Object> rawTokens = new HashMap<>();
        rawTokens.put("col1", "tok-abc");

        List<Token> col1 = Token.parseTokens(rawTokens).get("col1");
        Assert.assertEquals(1, col1.size());
        Assert.assertEquals("tok-abc", col1.get(0).getToken());
        Assert.assertNull(col1.get(0).getTokenGroupName());
    }

    @Test
    public void testParseTokens_handlesMultipleColumnsIndependently() {
        Map<String, Object> groupedEntry = new HashMap<>();
        groupedEntry.put("token", "tok-a");
        groupedEntry.put("tokenGroupName", "tg1");

        Map<String, Object> rawTokens = new HashMap<>();
        rawTokens.put("col1", Collections.singletonList(groupedEntry));
        rawTokens.put("col2", "tok-bare");

        Map<String, List<Token>> parsed = Token.parseTokens(rawTokens);
        Assert.assertEquals("tg1", parsed.get("col1").get(0).getTokenGroupName());
        Assert.assertEquals("tok-bare", parsed.get("col2").get(0).getToken());
        Assert.assertNull(parsed.get("col2").get(0).getTokenGroupName());
    }

    @Test
    public void testParseTokens_omitsAColumnWithANullValue() {
        Map<String, Object> entry = new HashMap<>();
        entry.put("token", "tok-a");
        entry.put("tokenGroupName", "tg1");

        Map<String, Object> rawTokens = new HashMap<>();
        rawTokens.put("col1", entry);
        rawTokens.put("col2", null);

        Map<String, List<Token>> parsed = Token.parseTokens(rawTokens);
        Assert.assertTrue(parsed.containsKey("col1"));
        Assert.assertFalse(parsed.containsKey("col2"));
    }

    @Test
    public void testParseTokens_mapEntryMissingTokenGroupNameKeyParsesAsNull() {
        Map<String, Object> entry = new HashMap<>();
        entry.put("token", "tok-a");
        // no "tokenGroupName" key at all - distinct from the bare-value case, since here the
        // raw entry is still a Map, just missing one of the two expected keys.

        Map<String, Object> rawTokens = new HashMap<>();
        rawTokens.put("col1", entry);

        List<Token> col1 = Token.parseTokens(rawTokens).get("col1");
        Assert.assertEquals("tok-a", col1.get(0).getToken());
        Assert.assertNull(col1.get(0).getTokenGroupName());
    }

    @Test
    public void testParseTokens_skipsNullEntriesWithinAList() {
        Map<String, Object> entry = new HashMap<>();
        entry.put("token", "tok-a");
        entry.put("tokenGroupName", "tg1");

        Map<String, Object> rawTokens = new HashMap<>();
        rawTokens.put("col1", Arrays.asList(entry, null));

        List<Token> col1 = Token.parseTokens(rawTokens).get("col1");
        Assert.assertEquals(1, col1.size());
        Assert.assertEquals("tok-a", col1.get(0).getToken());
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


    // ── TokenizeResponseToken ────────────────────────────────────────────────

    @Test
    public void testTokenizeResponseToken_successValues() {
        TokenizeResponseToken token = new TokenizeResponseToken("group1", "tok-abc", 200, null);
        Assert.assertEquals("group1", token.getTokenGroupName());
        Assert.assertEquals("tok-abc", token.getToken());
        Assert.assertEquals(Integer.valueOf(200), token.getHttpCode());
        Assert.assertNull(token.getError());
    }

    @Test
    public void testTokenizeResponseToken_errorValues() {
        TokenizeResponseToken token = new TokenizeResponseToken("group2", null, 400, "bad group");
        Assert.assertNull(token.getToken());
        Assert.assertEquals("bad group", token.getError());
        Assert.assertEquals(Integer.valueOf(400), token.getHttpCode());
    }

    @Test
    public void testTokenizeResponseToken_toStringSerializesNulls() {
        Assert.assertTrue(new TokenizeResponseToken("group1", "tok-abc", 200, null)
                .toString().contains("\"error\":null"));
    }

    // ── TokenizeResponseRecord / BulkTokenizeResponseRecord ──────────────────

    @Test
    public void testTokenizeResponseRecord_gettersReturnConstructorValues() {
        List<TokenizeResponseToken> tokens = Collections.singletonList(
                new TokenizeResponseToken("group1", "tok-abc", 200, null));
        TokenizeResponseRecord record = new TokenizeResponseRecord("value1", tokens);
        Assert.assertEquals("value1", record.getValue());
        Assert.assertEquals(tokens, record.getTokens());
    }

    @Test
    public void testBulkTokenizeResponseRecord_carriesIndexAndIsATokenizeResponseRecord() {
        BulkTokenizeResponseRecord record = new BulkTokenizeResponseRecord(7, "value1",
                Collections.singletonList(new TokenizeResponseToken("group1", "tok-abc", 200, null)));
        Assert.assertEquals(7, record.getIndex());
        Assert.assertEquals("value1", record.getValue());
        Assert.assertTrue(record instanceof TokenizeResponseRecord);
        Assert.assertNotNull(record.toString());
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

    // ── DeleteTokensRecord ───────────────────────────────────────────────────

    @Test
    public void testDeleteTokensRecord_gettersReturnConstructorValues() {
        DeleteTokensRecord record = new DeleteTokensRecord("tok-1", 200, null);
        Assert.assertEquals("tok-1", record.getToken());
        Assert.assertEquals(Integer.valueOf(200), record.getHttpCode());
        Assert.assertNull(record.getError());
    }

    @Test
    public void testDeleteTokensRecord_carriesErrorDetails() {
        DeleteTokensRecord record = new DeleteTokensRecord("tok-2", 404, "Token not found");
        Assert.assertEquals("tok-2", record.getToken());
        Assert.assertEquals(Integer.valueOf(404), record.getHttpCode());
        Assert.assertEquals("Token not found", record.getError());
    }

    @Test
    public void testDeleteTokensRecord_toStringSerializesNulls() {
        Assert.assertTrue(new DeleteTokensRecord("tok-1", 200, null).toString().contains("\"error\":null"));
    }

    // ── BulkDeleteTokensResponseRecord ───────────────────────────────────────

    @Test
    public void testBulkDeleteTokensResponseRecord_gettersReturnConstructorValues() {
        BulkDeleteTokensResponseRecord record = new BulkDeleteTokensResponseRecord(1, "tok-1", 200, null);
        Assert.assertEquals(1, record.getIndex());
        Assert.assertEquals("tok-1", record.getToken());
        Assert.assertEquals(Integer.valueOf(200), record.getHttpCode());
        Assert.assertNull(record.getError());
    }

    @Test
    public void testBulkDeleteTokensResponseRecord_isADeleteTokensRecord() {
        Assert.assertTrue(new BulkDeleteTokensResponseRecord(0, "tok-1", 200, null) instanceof DeleteTokensRecord);
    }

    @Test
    public void testBulkDeleteTokensResponseRecord_toStringNotNull() {
        Assert.assertNotNull(new BulkDeleteTokensResponseRecord(0, "tok-1", 200, null).toString());
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
                4, "tok-1", "secret-value", "group1", metadata, 200, null, null);

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
                0, "tok-2", null, null, null, 404, "Token not found", null);

        Assert.assertEquals(0, record.getIndex());
        Assert.assertEquals("Token not found", record.getError());
        Assert.assertEquals(404, record.getHttpCode());
        Assert.assertNull(record.getTokenGroupName());
        Assert.assertNull(record.getMetadata());
    }

    @Test
    public void testBulkDetokenizeResponseRecord_isADetokenizeResponseRecord() {
        BulkDetokenizeResponseRecord record = new BulkDetokenizeResponseRecord(
                1, "tok", "plain", "group", null, 200, null, null);
        Assert.assertTrue(record instanceof DetokenizeResponseRecord);
    }

    @Test
    public void testBulkDetokenizeResponseRecord_toStringSerializesNulls() {
        BulkDetokenizeResponseRecord record = new BulkDetokenizeResponseRecord(
                2, "tok", null, null, null, 200, null, null);
        String json = record.toString();

        Assert.assertNotNull(json);
        Assert.assertTrue(json.contains("\"index\":2"));
        Assert.assertTrue(json.contains("\"token\":\"tok\""));
        Assert.assertTrue(json.contains("\"error\":null"));
    }
}
