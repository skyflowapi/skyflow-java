package com.skyflow.utils;

import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.core.ObjectMappers;
import com.skyflow.generated.rest.types.V1FlowTokenizeResponse;
import com.skyflow.vault.data.BulkTokenizeRequestRecord;
import com.skyflow.vault.data.BulkTokenizeResponse;
import com.skyflow.vault.data.BulkTokenizeResponseRecord;
import com.skyflow.vault.data.TokenizeResponseToken;
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
 * The API returns one flat row per (value, token group) instead of the nested {@code tokens} array
 * the generated wire type models, and a record rejected outright yields a single row regardless of
 * how many groups it asked for. These cover folding those rows back onto the records that produced
 * them.
 */
public class FlatTokenizeResponseTests {

    private static V1FlowTokenizeResponse parse(String json) {
        try {
            return ObjectMappers.JSON_MAPPER.readValue(json, V1FlowTokenizeResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static BulkTokenizeRequestRecord record(Object value, String... groups) {
        return BulkTokenizeRequestRecord.builder()
                .value(value).tokenGroupNames(Arrays.asList(groups)).build();
    }

    private static BulkTokenizeRequestRecord byotRecord(Object value, String token, String... groups) {
        return BulkTokenizeRequestRecord.builder()
                .value(value).token(token).tokenGroupNames(Arrays.asList(groups)).build();
    }

    // ── the exact payload observed against the live API ────────────────────────

    private static final String LIVE_RESPONSE = "{\n"
            + "  \"response\": [\n"
            + "    { \"token\": \"\", \"value\": \"byot-input-value\", \"tokenGroupName\": null,\n"
            + "      \"error\": \"Invalid request. BYOT token should contain one token group.\", \"httpCode\": 400 },\n"
            + "    { \"token\": \"cc1179a3-e2be-404e-9a31-4f97f27bf406\", \"value\": {\"age\": 28, \"email\": \"ka@yahoo.com\"},\n"
            + "      \"tokenGroupName\": \"deterministic_string_tg\", \"error\": null, \"httpCode\": 200 },\n"
            + "    { \"token\": \"\", \"value\": {\"age\": 28, \"email\": \"ka@yahoo.com\"}, \"tokenGroupName\": null,\n"
            + "      \"error\": \"Tokenize failed. Token group emailTokenGroup is invalid. Specify a valid token group.\",\n"
            + "      \"httpCode\": 400 }\n"
            + "  ]\n"
            + "}";

    @Test
    public void testLiveResponse_threeRowsFoldOntoTwoRecords() {
        Map<String, Object> objectValue = new LinkedHashMap<>();
        objectValue.put("email", "ka@yahoo.com");
        objectValue.put("age", 28);
        List<BulkTokenizeRequestRecord> sent = Arrays.asList(
                byotRecord("byot-input-value", "550e8400-e29b-41d4-a716-446655440000",
                        "deterministic_string_tg", "non_deterministic"),
                record(objectValue, "deterministic_string_tg", "emailTokenGroup"));

        BulkTokenizeResponse result =
                Utils.formatBulkTokenizeResponse(parse(LIVE_RESPONSE), sent, 0, new HashMap<>());

        // two inputs in, two records out - not three
        Assert.assertEquals(2, result.getRecords().size());

        BulkTokenizeResponseRecord byot = result.getRecords().get(0);
        Assert.assertEquals(0, byot.getIndex());
        Assert.assertEquals("byot-input-value", byot.getValue());
        Assert.assertEquals(1, byot.getTokens().size());
        Assert.assertEquals("Invalid request. BYOT token should contain one token group.",
                byot.getTokens().get(0).getError());
        Assert.assertEquals(Integer.valueOf(400), byot.getTokens().get(0).getHttpCode());
        // the API sends "" for a token that does not apply
        Assert.assertNull(byot.getTokens().get(0).getToken());

        BulkTokenizeResponseRecord object = result.getRecords().get(1);
        Assert.assertEquals(1, object.getIndex());
        Assert.assertEquals(2, object.getTokens().size());
        Assert.assertEquals("deterministic_string_tg", object.getTokens().get(0).getTokenGroupName());
        Assert.assertEquals("cc1179a3-e2be-404e-9a31-4f97f27bf406", object.getTokens().get(0).getToken());
        Assert.assertNull(object.getTokens().get(0).getError());
        Assert.assertEquals("Tokenize failed. Token group emailTokenGroup is invalid. Specify a valid token group.",
                object.getTokens().get(1).getError());
    }

    @Test
    public void testLiveResponse_summaryClassifiesByRecordNotByRow() {
        Map<String, Object> objectValue = new LinkedHashMap<>();
        objectValue.put("email", "ka@yahoo.com");
        objectValue.put("age", 28);
        List<BulkTokenizeRequestRecord> sent = Arrays.asList(
                byotRecord("byot-input-value", "550e8400", "deterministic_string_tg", "non_deterministic"),
                record(objectValue, "deterministic_string_tg", "emailTokenGroup"));

        BulkTokenizeResponse formatted =
                Utils.formatBulkTokenizeResponse(parse(LIVE_RESPONSE), sent, 0, new HashMap<>());
        BulkTokenizeResponse withPayload =
                new BulkTokenizeResponse(formatted.getRecords(), sent);

        // 2 values in: one wholly failed, one partially tokenized - and the counts sum to 2
        Assert.assertEquals(2, withPayload.getSummary().getTotalTokens());
        Assert.assertEquals(0, withPayload.getSummary().getTotalTokenized());
        Assert.assertEquals(1, withPayload.getSummary().getTotalPartial());
        Assert.assertEquals(1, withPayload.getSummary().getTotalFailed());
    }

    // ── duplicate token groups within one record ───────────────────────────────

    @Test
    public void testDuplicateTokenGroupsInOneRecord_bothRowsKeptUnderOneRecord() {
        String json = "{\"response\": ["
                + "{\"value\": \"v1\", \"tokenGroupName\": \"g1\", \"token\": \"tok-a\", \"httpCode\": 200},"
                + "{\"value\": \"v1\", \"tokenGroupName\": \"g1\", \"token\": \"tok-b\", \"httpCode\": 200}"
                + "]}";
        List<BulkTokenizeRequestRecord> sent = Collections.singletonList(record("v1", "g1", "g1"));

        BulkTokenizeResponse result = Utils.formatBulkTokenizeResponse(parse(json), sent, 0, new HashMap<>());

        // one record asked for the same group twice; both results stay on it rather than
        // collapsing or spilling into a phantom second record
        Assert.assertEquals(1, result.getRecords().size());
        List<TokenizeResponseToken> tokens = result.getRecords().get(0).getTokens();
        Assert.assertEquals(2, tokens.size());
        Assert.assertEquals("tok-a", tokens.get(0).getToken());
        Assert.assertEquals("tok-b", tokens.get(1).getToken());
    }

    // ── boundaries ─────────────────────────────────────────────────────────────

    @Test
    public void testRecordRejectedOutright_nextRecordStillGetsItsOwnRows() {
        // record 0 asked for 2 groups but was rejected wholesale, yielding 1 row
        String json = "{\"response\": ["
                + "{\"value\": \"v0\", \"tokenGroupName\": null, \"token\": \"\", \"error\": \"rejected\", \"httpCode\": 400},"
                + "{\"value\": \"v1\", \"tokenGroupName\": \"g1\", \"token\": \"tok-1\", \"httpCode\": 200},"
                + "{\"value\": \"v1\", \"tokenGroupName\": \"g2\", \"token\": \"tok-2\", \"httpCode\": 200}"
                + "]}";
        List<BulkTokenizeRequestRecord> sent = Arrays.asList(
                record("v0", "g1", "g2"), record("v1", "g1", "g2"));

        BulkTokenizeResponse result = Utils.formatBulkTokenizeResponse(parse(json), sent, 0, new HashMap<>());

        Assert.assertEquals(2, result.getRecords().size());
        Assert.assertEquals(1, result.getRecords().get(0).getTokens().size());
        Assert.assertEquals("rejected", result.getRecords().get(0).getTokens().get(0).getError());
        Assert.assertEquals(2, result.getRecords().get(1).getTokens().size());
        Assert.assertEquals("tok-1", result.getRecords().get(1).getTokens().get(0).getToken());
    }

    @Test
    public void testIndexIsOffsetByBatchStart() {
        String json = "{\"response\": ["
                + "{\"value\": \"v0\", \"tokenGroupName\": \"g1\", \"token\": \"tok-0\", \"httpCode\": 200},"
                + "{\"value\": \"v1\", \"tokenGroupName\": \"g1\", \"token\": \"tok-1\", \"httpCode\": 200}"
                + "]}";
        List<BulkTokenizeRequestRecord> sent = Arrays.asList(record("v0", "g1"), record("v1", "g1"));

        BulkTokenizeResponse result = Utils.formatBulkTokenizeResponse(parse(json), sent, 25, new HashMap<>());

        Assert.assertEquals(25, result.getRecords().get(0).getIndex());
        Assert.assertEquals(26, result.getRecords().get(1).getIndex());
    }

    @Test
    public void testRecordTheResponseNeverMentions_stillReportedWithNoTokens() {
        String json = "{\"response\": ["
                + "{\"value\": \"v0\", \"tokenGroupName\": \"g1\", \"token\": \"tok-0\", \"httpCode\": 200}"
                + "]}";
        List<BulkTokenizeRequestRecord> sent = Arrays.asList(record("v0", "g1"), record("v1", "g1"));

        BulkTokenizeResponse result = Utils.formatBulkTokenizeResponse(parse(json), sent, 0, new HashMap<>());

        // the caller must still see a record per input, so indexes stay aligned with their list
        Assert.assertEquals(2, result.getRecords().size());
        Assert.assertEquals(1, result.getRecords().get(1).getIndex());
        Assert.assertTrue(result.getRecords().get(1).getTokens().isEmpty());
    }

    @Test
    public void testMoreRowsThanTheRequestExplains_rowsAreKeptNotDropped() {
        String json = "{\"response\": ["
                + "{\"value\": \"v0\", \"tokenGroupName\": \"g1\", \"token\": \"tok-0\", \"httpCode\": 200},"
                + "{\"value\": \"stray\", \"tokenGroupName\": \"g9\", \"token\": \"tok-9\", \"httpCode\": 200}"
                + "]}";
        List<BulkTokenizeRequestRecord> sent = Collections.singletonList(record("v0", "g1"));

        BulkTokenizeResponse result = Utils.formatBulkTokenizeResponse(parse(json), sent, 0, new HashMap<>());

        Assert.assertEquals(2, result.getRecords().size());
        Assert.assertEquals("tok-9", result.getRecords().get(1).getTokens().get(0).getToken());
    }

    @Test
    public void testMultipleGroupsForOneValue_foldOntoOneRecord() {
        String json = "{\"response\": ["
                + "{\"value\": \"v0\", \"tokenGroupName\": \"g1\", \"token\": \"tok-a\", \"httpCode\": 200},"
                + "{\"value\": \"v0\", \"tokenGroupName\": \"g2\", \"token\": \"tok-b\", \"httpCode\": 200},"
                + "{\"value\": \"v1\", \"tokenGroupName\": \"g1\", \"token\": \"tok-c\", \"httpCode\": 200}"
                + "]}";
        List<BulkTokenizeRequestRecord> sent = Arrays.asList(
                record("v0", "g1", "g2"), record("v1", "g1"));

        BulkTokenizeResponse result = Utils.formatBulkTokenizeResponse(parse(json), sent, 0, new HashMap<>());

        Assert.assertEquals(2, result.getRecords().size());
        Assert.assertEquals(2, result.getRecords().get(0).getTokens().size());
        Assert.assertEquals("tok-c", result.getRecords().get(1).getTokens().get(0).getToken());
    }

    // ── rejected requests still describe each record in the body ───────────────

    /** Builds the wrapper the SDK sees: CompletableFuture wraps the cause. */
    private static Throwable rejected(int status, Object body) {
        return new RuntimeException(new ApiClientApiException("Error with status code " + status, status, body));
    }

    private static Map<String, Object> row(String value, String group, String token, String error, int httpCode) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("token", token);
        r.put("value", value);
        r.put("tokenGroupName", group);
        r.put("error", error);
        r.put("httpCode", httpCode);
        return r;
    }

    private static Map<String, Object> body(Map<String, Object>... rows) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("response", Arrays.asList(rows));
        return b;
    }

    @Test
    public void testRejectedRequest_reportsTheApiMessageNotJustTheStatus() {
        // BYOT naming two groups: the API rejects the whole record and returns ONE row for it
        List<BulkTokenizeRequestRecord> sent = Collections.singletonList(
                byotRecord("grace@example.com", "tok-1", "deterministic_string_tg", "non_deterministic"));
        Throwable ex = rejected(400, body(row("grace@example.com", null, "", 
                "Invalid request. BYOT token should contain one token group.", 400)));

        List<BulkTokenizeResponseRecord> records =
                Utils.handleBulkTokenizeBatchException(ex, sent, 0);

        Assert.assertEquals(1, records.size());
        // one entry, matching the API - not one fabricated per requested token group
        Assert.assertEquals(1, records.get(0).getTokens().size());
        Assert.assertEquals("Invalid request. BYOT token should contain one token group.",
                records.get(0).getTokens().get(0).getError());
        Assert.assertNull(records.get(0).getTokens().get(0).getTokenGroupName());
        Assert.assertEquals(Integer.valueOf(400), records.get(0).getTokens().get(0).getHttpCode());
    }

    @Test
    public void testRejectedRequest_keepsPerRecordMessagesAndIndexes() {
        List<BulkTokenizeRequestRecord> sent = Arrays.asList(
                record("dave@example.com", "bad_group"), record("erin@example.com", "bad_group"));
        String message = "Tokenize failed. Token group bad_group is invalid. Specify a valid token group.";
        Throwable ex = rejected(400, body(
                row("dave@example.com", null, "", message, 400),
                row("erin@example.com", null, "", message, 400)));

        List<BulkTokenizeResponseRecord> records =
                Utils.handleBulkTokenizeBatchException(ex, sent, 20);

        Assert.assertEquals(2, records.size());
        Assert.assertEquals(20, records.get(0).getIndex());
        Assert.assertEquals(21, records.get(1).getIndex());
        Assert.assertEquals(message, records.get(0).getTokens().get(0).getError());
        Assert.assertEquals(message, records.get(1).getTokens().get(0).getError());
    }

    @Test
    public void testRejectedRequest_withNoUsableBodyFallsBackToTheStatusCode() {
        // a transport-level failure carries no response array to read
        List<BulkTokenizeRequestRecord> sent = Collections.singletonList(record("v1", "g1", "g2"));
        Throwable ex = new RuntimeException("connection reset");

        List<BulkTokenizeResponseRecord> records =
                Utils.handleBulkTokenizeBatchException(ex, sent, 0);

        Assert.assertEquals(1, records.size());
        // no rows to go on, so every requested group is reported as failed
        Assert.assertEquals(2, records.get(0).getTokens().size());
        Assert.assertEquals("connection reset", records.get(0).getTokens().get(0).getError());
        Assert.assertEquals(Integer.valueOf(500), records.get(0).getTokens().get(0).getHttpCode());
    }

    @Test
    public void testRejectedRequest_withUnfamiliarBodyFallsBackToTheStatusCode() {
        List<BulkTokenizeRequestRecord> sent = Collections.singletonList(record("v1", "g1"));
        Map<String, Object> opaque = new LinkedHashMap<>();
        opaque.put("error", "gateway timeout");

        List<BulkTokenizeResponseRecord> records =
                Utils.handleBulkTokenizeBatchException(rejected(504, opaque), sent, 0);

        Assert.assertEquals(1, records.size());
        Assert.assertEquals("gateway timeout", records.get(0).getTokens().get(0).getError());
        Assert.assertEquals(Integer.valueOf(504), records.get(0).getTokens().get(0).getHttpCode());
    }

    @Test
    public void testRejectedRequest_retryableStatusStillSurfacesForRetry() {
        List<BulkTokenizeRequestRecord> sent = Collections.singletonList(record("v1", "g1"));
        Throwable ex = rejected(503, body(row("v1", "g1", "", "service unavailable", 503)));

        List<BulkTokenizeResponseRecord> records =
                Utils.handleBulkTokenizeBatchException(ex, sent, 0);
        BulkTokenizeResponse response = new BulkTokenizeResponse(records, sent);

        Assert.assertEquals(1, response.getRecordsToRetry().size());
        Assert.assertSame(sent.get(0), response.getRecordsToRetry().get(0));
    }

    // ── duplicate-value batching ───────────────────────────────────────────────

    @Test
    public void testBatching_cutsBatchShortWhenAValueRepeats() {
        List<BulkTokenizeRequestRecord> records = Arrays.asList(
                record("alice", "g1"), record("bob", "g1"), record("alice", "g1"), record("carol", "g1"));

        List<List<BulkTokenizeRequestRecord>> batches = Utils.createBulkTokenizeBatches(records, 10);

        // the repeat of "alice" must not share a request with the first one
        Assert.assertEquals(2, batches.size());
        Assert.assertEquals(2, batches.get(0).size());
        Assert.assertEquals("alice", batches.get(0).get(0).getValue());
        Assert.assertEquals("bob", batches.get(0).get(1).getValue());
        Assert.assertEquals("alice", batches.get(1).get(0).getValue());
        Assert.assertEquals("carol", batches.get(1).get(1).getValue());
    }

    @Test
    public void testBatching_stillHonoursBatchSize() {
        List<BulkTokenizeRequestRecord> records = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            records.add(record("v" + i, "g1"));
        }

        List<List<BulkTokenizeRequestRecord>> batches = Utils.createBulkTokenizeBatches(records, 2);

        Assert.assertEquals(3, batches.size());
        Assert.assertEquals(2, batches.get(0).size());
        Assert.assertEquals(2, batches.get(1).size());
        Assert.assertEquals(1, batches.get(2).size());
    }

    @Test
    public void testBatching_batchesStayContiguousSoIndexesFollowFromTheStart() {
        List<BulkTokenizeRequestRecord> records = Arrays.asList(
                record("dup", "g1"), record("dup", "g1"), record("dup", "g1"));

        List<List<BulkTokenizeRequestRecord>> batches = Utils.createBulkTokenizeBatches(records, 10);

        // three copies of one value cannot share a request, so each gets its own
        Assert.assertEquals(3, batches.size());
        int seen = 0;
        for (List<BulkTokenizeRequestRecord> batch : batches) {
            seen += batch.size();
        }
        Assert.assertEquals(records.size(), seen);
    }

    @Test
    public void testBatching_emptyAndNullInputs() {
        Assert.assertTrue(Utils.createBulkTokenizeBatches(null, 10).isEmpty());
        Assert.assertTrue(Utils.createBulkTokenizeBatches(Collections.emptyList(), 10).isEmpty());
    }
}
