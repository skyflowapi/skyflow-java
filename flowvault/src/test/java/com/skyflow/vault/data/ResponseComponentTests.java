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
 * constructor logic or toString() serialization: {@link Token},
 * {@link TokenizeResponseRecord}, {@link BulkTokenizeResponseRecord}, {@link TokenizeSummary},
 * {@link DeleteTokensRecord}, {@link BulkDeleteTokensResponseRecord},
 * {@link DeleteTokensSummary}, {@link DetokenizeSummary}, {@link DetokenizeMetadata},
 * {@link ErrorRecord} and {@link DetokenizeResponseObject}.
 */
public class ResponseComponentTests {

    // getFields()'s deprecated, pre-typed shape for a single column with one token group -
    // {"name": [{"token": "tok-1", "tokenGroupName": "group1"}]} - matching
    // tokens.put("name", Collections.singletonList(new Token("tok-1", "group1"))).
    private static Map<String, Object> singleColumnRawFields() {
        Map<String, Object> rawToken = new HashMap<>();
        rawToken.put("token", "tok-1");
        rawToken.put("tokenGroupName", "group1");
        Map<String, Object> rawFields = new HashMap<>();
        rawFields.put("name", Collections.singletonList(rawToken));
        return rawFields;
    }

    // Tests for Success and Summary were removed: the bulk insert response contract replaced
    // those classes with BulkInsertResponseRecord / BulkSummary, covered below. Token was removed
    // in the same rework, then reintroduced (with the same shape it had before) as the type
    // InsertResponseRecord.getTokens()/BulkInsertResponseRecord.getTokens() now return.

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
        // getFields() is deprecated, and now returns its original (pre-typed) shape - a Map<String,
        // Object> rendered back from the typed getTokens() data, not getTokens()'s value itself.
        Assert.assertEquals(singleColumnRawFields(), record.getFields());
        Assert.assertEquals(data, record.getData());
        Assert.assertEquals(hashedData, record.getHashedData());
        Assert.assertEquals(200, record.getHttpCode());
        Assert.assertNull(record.getError());
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
        Assert.assertEquals(singleColumnRawFields(), record.getFields());
        Assert.assertNull(record.getData());
        Assert.assertEquals(hashedData, record.getHashedData());
    }

    @Test
    public void testInsertResponseRecord_gettersReturnConstructorValues() {
        // InsertResponseRecord (unary) carries no deprecated back-compat surface - it is a
        // brand-new type with no pre-1.0.2 callers, unlike BulkInsertResponseRecord above.
        Map<String, List<Token>> tokens = new HashMap<>();
        tokens.put("name", Collections.singletonList(new Token("tok-1", "group1")));
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        Map<String, Object> hashedData = new HashMap<>();
        hashedData.put("name", "hashed-1");

        InsertResponseRecord record = new InsertResponseRecord(
                "persons", "skyflow-id-1", tokens, data, hashedData, 200, null, "req-1");

        Assert.assertEquals("persons", record.getTableName());
        Assert.assertEquals("skyflow-id-1", record.getSkyflowId());
        Assert.assertEquals(tokens, record.getTokens());
        Assert.assertEquals(data, record.getData());
        Assert.assertEquals(hashedData, record.getHashedData());
        Assert.assertEquals(200, record.getHttpCode());
        Assert.assertNull(record.getError());
        Assert.assertEquals("req-1", record.getRequestId());
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
        Assert.assertNull(token.getPath());
    }

    @Test
    public void testToken_toStringSerializesFields() {
        Token token = new Token("tok-1", "group1");
        String json = token.toString();
        Assert.assertTrue(json.contains("tok-1"));
        Assert.assertTrue(json.contains("group1"));
    }

    @Test
    public void testToken_threeArgConstructorSetsPath() {
        Token token = new Token("tok-1", "group1", "street");
        Assert.assertEquals("tok-1", token.getToken());
        Assert.assertEquals("group1", token.getTokenGroupName());
        Assert.assertEquals("street", token.getPath());
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
    public void testParseTokens_parsesPathForANestedColumnValue() {
        // Real shape observed for a structured column (e.g. an "address" object) tokenized
        // per nested field - see flowdb_dp_apis.proto's own example response.
        Map<String, Object> entry = new HashMap<>();
        entry.put("token", "tok-a");
        entry.put("tokenGroupName", "tg1");
        entry.put("path", "phone_numbers[0].type");

        Map<String, Object> rawTokens = new HashMap<>();
        rawTokens.put("address", Collections.singletonList(entry));

        List<Token> address = Token.parseTokens(rawTokens).get("address");
        Assert.assertEquals("phone_numbers[0].type", address.get(0).getPath());
    }

    @Test
    public void testParseTokens_mapEntryMissingPathKeyParsesAsNull() {
        Map<String, Object> entry = new HashMap<>();
        entry.put("token", "tok-a");
        entry.put("tokenGroupName", "tg1");
        // no "path" key - the normal case for a flat (non-structured) column.

        Map<String, Object> rawTokens = new HashMap<>();
        rawTokens.put("col1", entry);

        List<Token> col1 = Token.parseTokens(rawTokens).get("col1");
        Assert.assertNull(col1.get(0).getPath());
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

    @Test
    public void testToRawTokens_returnsNullWhenTokensIsNull() {
        Assert.assertNull(Token.toRawTokens(null));
    }

    @Test
    public void testToRawTokens_rendersEachTokenAsAMapWithBothKeys() {
        Map<String, List<Token>> tokens = new HashMap<>();
        tokens.put("col1", Collections.singletonList(new Token("tok-a", "tg1")));

        Map<String, Object> raw = Token.toRawTokens(tokens);

        List<?> col1 = (List<?>) raw.get("col1");
        Assert.assertEquals(1, col1.size());
        Map<?, ?> entry = (Map<?, ?>) col1.get(0);
        Assert.assertEquals("tok-a", entry.get("token"));
        Assert.assertEquals("tg1", entry.get("tokenGroupName"));
        // No path was set on the Token, so the rendered map has no such key at all - not a
        // "path": null entry - keeping the path-less shape identical to before path existed.
        Assert.assertFalse(entry.containsKey("path"));
    }

    @Test
    public void testToRawTokens_rendersPathWhenPresent() {
        Map<String, List<Token>> tokens = new HashMap<>();
        tokens.put("address", Collections.singletonList(new Token("tok-a", "tg1", "street")));

        Map<?, ?> entry = (Map<?, ?>) ((List<?>) Token.toRawTokens(tokens).get("address")).get(0);

        Assert.assertEquals("street", entry.get("path"));
    }

    @Test
    public void testToRawTokens_rendersMultipleTokenGroupsAsSeparateMapEntries() {
        Map<String, List<Token>> tokens = new HashMap<>();
        tokens.put("col1", Arrays.asList(new Token("tok-a", "tg1"), new Token("tok-b", "tg2")));

        List<?> col1 = (List<?>) Token.toRawTokens(tokens).get("col1");

        Assert.assertEquals(2, col1.size());
        Assert.assertEquals("tok-a", ((Map<?, ?>) col1.get(0)).get("token"));
        Assert.assertEquals("tok-b", ((Map<?, ?>) col1.get(1)).get("token"));
    }

    @Test
    public void testToRawTokens_isTheInverseOfParseTokensForMapShapedInput() {
        // parseTokens() followed by toRawTokens() round-trips losslessly when every column's raw
        // value was already a {token, tokenGroupName} map (or list of them) - the shape toRawTokens
        // always produces. Only the bare-value case (see UtilsTests) loses information on the way.
        Map<String, Object> entry = new HashMap<>();
        entry.put("token", "tok-a");
        entry.put("tokenGroupName", "tg1");
        Map<String, Object> rawTokens = new HashMap<>();
        rawTokens.put("col1", Collections.singletonList(entry));

        Map<String, Object> roundTripped = Token.toRawTokens(Token.parseTokens(rawTokens));

        Assert.assertEquals(rawTokens, roundTripped);
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


    // ── TokenizeResponseRecord / BulkTokenizeResponseRecord ──────────────────

    @Test
    public void testTokenizeResponseRecord_successValues() {
        TokenizeResponseRecord record = new TokenizeResponseRecord("value1", "group1", "tok-abc", 200, null);
        Assert.assertEquals("value1", record.getValue());
        Assert.assertEquals("group1", record.getTokenGroupName());
        Assert.assertEquals("tok-abc", record.getToken());
        Assert.assertEquals(Integer.valueOf(200), record.getHttpCode());
        Assert.assertNull(record.getError());
    }

    @Test
    public void testTokenizeResponseRecord_errorValues() {
        TokenizeResponseRecord record = new TokenizeResponseRecord("value1", "group2", null, 400, "bad group");
        Assert.assertNull(record.getToken());
        Assert.assertEquals("bad group", record.getError());
        Assert.assertEquals(Integer.valueOf(400), record.getHttpCode());
    }

    @Test
    public void testTokenizeResponseRecord_toStringSerializesNulls() {
        Assert.assertTrue(new TokenizeResponseRecord("value1", "group1", "tok-abc", 200, null)
                .toString().contains("\"error\":null"));
    }

    @Test
    public void testBulkTokenizeResponseRecord_carriesIndexAndIsATokenizeResponseRecord() {
        BulkTokenizeResponseRecord record = new BulkTokenizeResponseRecord(
                7, "value1", "group1", "tok-abc", 200, null, null);
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
        DetokenizeMetadata metadata = new DetokenizeMetadata("skyflow-id-1", "table1");

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

    // ── DetokenizeMetadata ────────────────────────────────────────────────────

    @Test
    public void testDetokenizeMetadata_gettersReturnConstructorValues() {
        DetokenizeMetadata metadata = new DetokenizeMetadata("skyflow-id-1", "table1");

        Assert.assertEquals("skyflow-id-1", metadata.getSkyflowId());
        Assert.assertEquals("table1", metadata.getTableName());
    }

    @Test
    public void testDetokenizeMetadata_toStringSerializesFields() {
        String json = new DetokenizeMetadata("skyflow-id-1", "table1").toString();

        Assert.assertTrue(json.contains("\"skyflowId\":\"skyflow-id-1\""));
        Assert.assertTrue(json.contains("\"tableName\":\"table1\""));
    }

    @Test
    public void testParseMetadata_returnsNullWhenRawMetadataIsNull() {
        Assert.assertNull(DetokenizeMetadata.parseMetadata(null));
    }

    @Test
    public void testParseMetadata_parsesTheCamelCaseWireShape() {
        // The shape Utils.formatBulkDetokenizeResponse actually hands this after its own
        // skyflowID -> skyflowId rename; table is left as-is on the wire.
        Map<String, Object> raw = new HashMap<>();
        raw.put("skyflowId", "skyflow-id-1");
        raw.put("table", "table1");

        DetokenizeMetadata metadata = DetokenizeMetadata.parseMetadata(raw);

        Assert.assertEquals("skyflow-id-1", metadata.getSkyflowId());
        Assert.assertEquals("table1", metadata.getTableName());
    }

    @Test
    public void testParseMetadata_prefersAnAlreadyCamelCasedTableNameKeyOverTable() {
        // Exercises the containsKey("tableName") branch directly - every other test only ever
        // supplies the wire's "table" key, never "tableName" itself.
        Map<String, Object> raw = new HashMap<>();
        raw.put("skyflowId", "skyflow-id-1");
        raw.put("tableName", "table1");
        raw.put("table", "should-be-ignored");

        DetokenizeMetadata metadata = DetokenizeMetadata.parseMetadata(raw);

        Assert.assertEquals("table1", metadata.getTableName());
    }

    @Test
    public void testParseMetadata_parsesTheLiteralProtoWireShape() {
        // flowdb_dp_apis.proto's own example value uses this exact casing/naming -
        // {"table": "table1", "skyflowID": "..."} - unrenamed.
        Map<String, Object> raw = new HashMap<>();
        raw.put("skyflowID", "skyflow-id-1");
        raw.put("table", "table1");

        DetokenizeMetadata metadata = DetokenizeMetadata.parseMetadata(raw);

        Assert.assertEquals("skyflow-id-1", metadata.getSkyflowId());
        Assert.assertEquals("table1", metadata.getTableName());
    }

    @Test
    public void testParseMetadata_missingKeysParseAsNull() {
        DetokenizeMetadata metadata = DetokenizeMetadata.parseMetadata(new HashMap<>());

        Assert.assertNull(metadata.getSkyflowId());
        Assert.assertNull(metadata.getTableName());
    }
}
