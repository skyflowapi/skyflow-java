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
 * Tests for the Bulk*Response classes: {@link BulkInsertResponse}, {@link BulkDetokenizeResponse},
 * {@link BulkDeleteTokensResponse} and {@link BulkTokenizeResponse}.
 *
 * <p>BulkDeleteTokensResponse/BulkTokenizeResponse each have a 2-arg constructor (success/errors
 * only, summary and originalPayload-derived fields stay null) and a 3-arg constructor (adds
 * originalPayload and computes a summary). BulkInsertResponse and BulkDetokenizeResponse instead
 * carry a single unified {@code records} list, with a 1-arg (per-batch) and a 2-arg (final,
 * summary-computing) constructor. Both additionally derive a retry list from the originalPayload,
 * filtering on a "5xx except 529" retryable-status rule.
 */
public class BulkResponseTests {

    // ── BulkInsertResponse ───────────────────────────────────────────────────

    @Test
    public void testBulkInsertResponse_oneArgConstructorLeavesSummaryAndRetryDependenciesNull() {
        List<BulkInsertResponseRecord> records = Collections.emptyList();

        BulkInsertResponse response = new BulkInsertResponse(records);

        Assert.assertNull(response.getSummary());
        Assert.assertEquals(records, response.getRecords());
        // No failed records, so the retry list is empty and originalPayload (null) is never dereferenced.
        Assert.assertTrue(response.getRecordsToRetry().isEmpty());
    }

    @Test
    public void testBulkInsertResponse_twoArgConstructorComputesSummary() {
        List<BulkInsertResponseRecord> records = Arrays.asList(
                new BulkInsertResponseRecord(0, "table1", "id-1", null, null, null, 200, null, null),
                new BulkInsertResponseRecord(1, null, null, null, null, null, 400, "failed", null));
        List<InsertRequestRecord> originalPayload = new ArrayList<>(Arrays.asList(
                BulkInsertRequestRecord.builder().tableName("table1").build(),
                BulkInsertRequestRecord.builder().tableName("table1").build()));

        BulkInsertResponse response = new BulkInsertResponse(records, originalPayload);

        Assert.assertNotNull(response.getSummary());
        Assert.assertEquals(2, response.getSummary().getTotalRecords());
        Assert.assertEquals(1, response.getSummary().getTotalInserted());
        Assert.assertEquals(1, response.getSummary().getTotalFailed());
    }

    @Test
    public void testBulkInsertResponse_recordsPreserveIndexAndInheritedFields() {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "token-name");
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        Map<String, Object> hashedData = new HashMap<>();
        hashedData.put("name", "hashed-name");

        BulkInsertResponseRecord record = new BulkInsertResponseRecord(
                7, "table1", "id-1", tokens, data, hashedData, 200, null, null);

        BulkInsertResponse response = new BulkInsertResponse(Collections.singletonList(record));

        BulkInsertResponseRecord actual = response.getRecords().get(0);
        Assert.assertEquals(7, actual.getIndex());
        Assert.assertEquals("table1", actual.getTableName());
        Assert.assertEquals("id-1", actual.getSkyflowId());
        Assert.assertEquals(tokens, actual.getTokens());
        // getFields() is deprecated but still delegates to getTokens() for backward compatibility.
        Assert.assertEquals(tokens, actual.getFields());
        Assert.assertEquals(data, actual.getData());
        Assert.assertEquals(hashedData, actual.getHashedData());
        Assert.assertEquals(200, actual.getHttpCode());
        Assert.assertNull(actual.getError());
    }

    @Test
    public void testBulkInsertResponse_getRecordsToRetryFiltersRetryableStatusCodesOnly() {
        BulkInsertRequestRecord record0 = BulkInsertRequestRecord.builder().tableName("t0").build();
        BulkInsertRequestRecord record1 = BulkInsertRequestRecord.builder().tableName("t1").build();
        BulkInsertRequestRecord record2 = BulkInsertRequestRecord.builder().tableName("t2").build();
        BulkInsertRequestRecord record3 = BulkInsertRequestRecord.builder().tableName("t3").build();
        List<InsertRequestRecord> originalPayload = new ArrayList<>(
                Arrays.asList(record0, record1, record2, record3));

        List<BulkInsertResponseRecord> records = Arrays.asList(
                new BulkInsertResponseRecord(0, null, null, null, null, null, 500, "server error", null),  // retryable (lower bound)
                new BulkInsertResponseRecord(1, null, null, null, null, null, 400, "bad request", null),   // not retryable
                new BulkInsertResponseRecord(2, null, null, null, null, null, 599, "server error", null),  // retryable (upper bound)
                new BulkInsertResponseRecord(3, null, null, null, null, null, 529, "special case", null)); // explicitly excluded

        BulkInsertResponse response = new BulkInsertResponse(records, originalPayload);

        List<BulkInsertRequestRecord> recordsToRetry = response.getRecordsToRetry();

        Assert.assertEquals(2, recordsToRetry.size());
        Assert.assertTrue(recordsToRetry.contains(record0));
        Assert.assertTrue(recordsToRetry.contains(record2));
        Assert.assertFalse(recordsToRetry.contains(record1));
        Assert.assertFalse(recordsToRetry.contains(record3));
    }

    @Test
    public void testBulkInsertResponse_toStringNotNull() {
        BulkInsertResponse response = new BulkInsertResponse(Collections.<BulkInsertResponseRecord>emptyList());
        Assert.assertNotNull(response.toString());
    }

    @Test
    public void testBulkInsertResponse_toStringSerializesSummaryAndRecordsButNotInternals() {
        List<BulkInsertResponseRecord> records = Collections.singletonList(
                new BulkInsertResponseRecord(0, "table1", "id-1", null, null, null, 200, null, null));
        List<InsertRequestRecord> originalPayload = new ArrayList<InsertRequestRecord>(
                Collections.singletonList(BulkInsertRequestRecord.builder().tableName("table1").build()));

        BulkInsertResponse response = new BulkInsertResponse(records, originalPayload);
        // Populate the lazily-derived internal so we can prove it is still excluded.
        response.getRecordsToRetry();
        String json = response.toString();

        Assert.assertTrue(json.contains("summary"));
        Assert.assertTrue(json.contains("records"));
        // serializeNulls() spells out the nulls on each record.
        Assert.assertTrue(json.contains("\"error\":null"));
        // transient internals stay out of the JSON.
        Assert.assertFalse(json.contains("originalPayload"));
        Assert.assertFalse(json.contains("recordsToRetry"));
    }

    @Test
    public void testBulkInsertResponse_getRecordsToRetryOnPerBatchResponseDoesNotThrow() {
        // The 1-arg constructor leaves originalPayload null. A 5xx record must not NPE here.
        List<BulkInsertResponseRecord> records = Collections.singletonList(
                new BulkInsertResponseRecord(0, null, null, null, null, null, 500, "server error", null));

        BulkInsertResponse response = new BulkInsertResponse(records);

        Assert.assertTrue(response.getRecordsToRetry().isEmpty());
    }

    // ── BulkDetokenizeResponse ───────────────────────────────────────────────

    @Test
    public void testBulkDetokenizeResponse_oneArgConstructorLeavesSummaryAndRetryDependenciesNull() {
        List<BulkDetokenizeResponseRecord> records = Collections.emptyList();

        BulkDetokenizeResponse response = new BulkDetokenizeResponse(records);

        Assert.assertNull(response.getSummary());
        Assert.assertEquals(records, response.getRecords());
        // No retryable records, so originalPayload (null) is never dereferenced.
        Assert.assertTrue(response.getTokensToRetry().isEmpty());
    }

    @Test
    public void testBulkDetokenizeResponse_twoArgConstructorComputesSummary() {
        List<BulkDetokenizeResponseRecord> records = Arrays.asList(
                new BulkDetokenizeResponseRecord(0, "tok-1", "secret-value", "group1", null, 200, null, null),
                new BulkDetokenizeResponseRecord(1, "tok-2", null, null, null, 404, "failed", null));
        List<String> originalPayload = Arrays.asList("tok-1", "tok-2");

        BulkDetokenizeResponse response = new BulkDetokenizeResponse(records, originalPayload);

        Assert.assertNotNull(response.getSummary());
        Assert.assertEquals(2, response.getSummary().getTotalTokens());
        Assert.assertEquals(1, response.getSummary().getTotalDetokenized());
        Assert.assertEquals(1, response.getSummary().getTotalFailed());
        Assert.assertEquals(records, response.getRecords());
    }

    @Test
    public void testBulkDetokenizeResponse_summaryTotalTokensComesFromOriginalPayloadNotRecords() {
        // Only one of the three submitted tokens came back, so totalTokens tracks the payload size.
        List<BulkDetokenizeResponseRecord> records = Collections.singletonList(
                new BulkDetokenizeResponseRecord(0, "tok-0", "plain-0", "group1", null, 200, null, null));
        List<String> originalPayload = Arrays.asList("tok-0", "tok-1", "tok-2");

        BulkDetokenizeResponse response = new BulkDetokenizeResponse(records, originalPayload);

        Assert.assertEquals(3, response.getSummary().getTotalTokens());
        Assert.assertEquals(1, response.getSummary().getTotalDetokenized());
        Assert.assertEquals(0, response.getSummary().getTotalFailed());
    }

    @Test
    public void testBulkDetokenizeResponse_getTokensToRetryFiltersRetryableStatusCodesOnly() {
        List<String> originalPayload = Arrays.asList("tok-0", "tok-1", "tok-2", "tok-3");
        List<BulkDetokenizeResponseRecord> records = Arrays.asList(
                new BulkDetokenizeResponseRecord(0, null, null, null, null, 500, "server error", null),
                new BulkDetokenizeResponseRecord(1, null, null, null, null, 400, "bad request", null),
                new BulkDetokenizeResponseRecord(2, null, null, null, null, 599, "server error", null),
                new BulkDetokenizeResponseRecord(3, null, null, null, null, 529, "special case", null));

        BulkDetokenizeResponse response = new BulkDetokenizeResponse(records, originalPayload);

        List<String> tokensToRetry = response.getTokensToRetry();

        Assert.assertEquals(2, tokensToRetry.size());
        Assert.assertTrue(tokensToRetry.contains("tok-0"));
        Assert.assertTrue(tokensToRetry.contains("tok-2"));
        Assert.assertFalse(tokensToRetry.contains("tok-1"));
        Assert.assertFalse(tokensToRetry.contains("tok-3"));
    }

    @Test
    public void testBulkDetokenizeResponse_toStringNotNull() {
        BulkDetokenizeResponse response = new BulkDetokenizeResponse(Collections.emptyList());
        Assert.assertNotNull(response.toString());
    }

    @Test
    public void testBulkDetokenizeResponse_toStringSerializesSummaryAndRecordsButNotInternals() {
        List<BulkDetokenizeResponseRecord> records = Collections.singletonList(
                new BulkDetokenizeResponseRecord(0, "tok-0", "plain-0", "group1", null, 200, null, null));
        List<String> originalPayload = Collections.singletonList("tok-0");

        BulkDetokenizeResponse response = new BulkDetokenizeResponse(records, originalPayload);
        // Populate the lazily-derived internal so we can prove it is still excluded.
        response.getTokensToRetry();
        String json = response.toString();

        Assert.assertTrue(json.contains("summary"));
        Assert.assertTrue(json.contains("records"));
        // serializeNulls() spells out the nulls on each record.
        Assert.assertTrue(json.contains("\"error\":null"));
        // transient internals stay out of the JSON.
        Assert.assertFalse(json.contains("originalPayload"));
        Assert.assertFalse(json.contains("tokensToRetry"));
    }

    @Test
    public void testBulkDetokenizeResponse_getTokensToRetryOnPerBatchResponseDoesNotThrow() {
        List<BulkDetokenizeResponseRecord> records = Collections.singletonList(
                new BulkDetokenizeResponseRecord(0, "tok-0", null, null, null, 500, "server error", null));

        BulkDetokenizeResponse response = new BulkDetokenizeResponse(records);

        Assert.assertTrue(response.getTokensToRetry().isEmpty());
    }

    @Test
    public void testBulkDetokenizeResponse_recordsExposeDetokenizedValue() {
        // value is passed through verbatim from the generated response object.
        List<BulkDetokenizeResponseRecord> records = Collections.singletonList(
                new BulkDetokenizeResponseRecord(0, "tok-0", "john@example.com", "group1", null, 200, null, null));

        BulkDetokenizeResponse response = new BulkDetokenizeResponse(records);

        Assert.assertEquals("john@example.com", response.getRecords().get(0).getValue());
    }

    // ── BulkDeleteTokensResponse ─────────────────────────────────────────────

    @Test
    public void testBulkDeleteTokensResponse_singleArgConstructorLeavesSummaryNull() {
        List<BulkDeleteTokensResponseRecord> records = Collections.emptyList();

        BulkDeleteTokensResponse response = new BulkDeleteTokensResponse(records);

        Assert.assertNull(response.getSummary());
        Assert.assertEquals(records, response.getRecords());
    }

    @Test
    public void testBulkDeleteTokensResponse_twoArgConstructorComputesSummary() {
        List<BulkDeleteTokensResponseRecord> records = Arrays.asList(
                new BulkDeleteTokensResponseRecord(0, "tok-1", 200, null),
                new BulkDeleteTokensResponseRecord(1, "tok-2", 404, "Token not found"));
        List<String> originalPayload = Arrays.asList("tok-1", "tok-2");

        BulkDeleteTokensResponse response = new BulkDeleteTokensResponse(records, originalPayload);

        Assert.assertNotNull(response.getSummary());
        Assert.assertEquals(2, response.getSummary().getTotalTokens());
        Assert.assertEquals(1, response.getSummary().getTotalDeleted());
        Assert.assertEquals(1, response.getSummary().getTotalFailed());
        Assert.assertEquals(records, response.getRecords());
    }

    @Test
    public void testBulkDeleteTokensResponse_getTokensToRetryFiltersRetryableStatusCodesOnly() {
        List<BulkDeleteTokensResponseRecord> records = Arrays.asList(
                new BulkDeleteTokensResponseRecord(0, "tok-0", 500, "server error"),   // retryable (lower bound)
                new BulkDeleteTokensResponseRecord(1, "tok-1", 404, "not found"),      // not retryable
                new BulkDeleteTokensResponseRecord(2, "tok-2", 599, "server error"),   // retryable (upper bound)
                new BulkDeleteTokensResponseRecord(3, "tok-3", 529, "special case"),   // explicitly excluded
                new BulkDeleteTokensResponseRecord(4, "tok-4", 200, null));            // succeeded

        BulkDeleteTokensResponse response = new BulkDeleteTokensResponse(
                records, Arrays.asList("tok-0", "tok-1", "tok-2", "tok-3", "tok-4"));

        List<String> tokensToRetry = response.getTokensToRetry();

        Assert.assertEquals(2, tokensToRetry.size());
        Assert.assertTrue(tokensToRetry.contains("tok-0"));
        Assert.assertTrue(tokensToRetry.contains("tok-2"));
        Assert.assertFalse(tokensToRetry.contains("tok-1"));
        Assert.assertFalse(tokensToRetry.contains("tok-3"));
        Assert.assertFalse(tokensToRetry.contains("tok-4"));
    }

    @Test
    public void testBulkDeleteTokensResponse_getTokensToRetryEmptyWhenAllSucceeded() {
        List<BulkDeleteTokensResponseRecord> records = Collections.singletonList(
                new BulkDeleteTokensResponseRecord(0, "tok-0", 200, null));

        BulkDeleteTokensResponse response =
                new BulkDeleteTokensResponse(records, Collections.singletonList("tok-0"));

        Assert.assertTrue(response.getTokensToRetry().isEmpty());
    }

    @Test
    public void testBulkDeleteTokensResponse_tokensToRetryNotSerialized() {
        List<BulkDeleteTokensResponseRecord> records = Collections.singletonList(
                new BulkDeleteTokensResponseRecord(0, "tok-0", 500, "server error"));
        BulkDeleteTokensResponse response =
                new BulkDeleteTokensResponse(records, Collections.singletonList("tok-0"));

        response.getTokensToRetry();   // populate the lazily-derived field

        Assert.assertFalse(response.toString().contains("tokensToRetry"));
    }

    @Test
    public void testBulkDeleteTokensResponse_toStringMatchesContractShape() {
        List<BulkDeleteTokensResponseRecord> records = Arrays.asList(
                new BulkDeleteTokensResponseRecord(0, "a1b2c3d4", 200, null),
                new BulkDeleteTokensResponseRecord(1, "z9y8x7", 404, "Token not found"));
        BulkDeleteTokensResponse response =
                new BulkDeleteTokensResponse(records, Arrays.asList("a1b2c3d4", "z9y8x7"));

        String json = response.toString();

        Assert.assertNotNull(json);
        // summary + records only; originalPayload is internal and must never be serialized
        Assert.assertTrue(json.contains("\"summary\""));
        Assert.assertTrue(json.contains("\"records\""));
        Assert.assertFalse(json.contains("originalPayload"));
        // nulls are serialized so a success record still reports its error field
        Assert.assertTrue(json.contains("\"error\":null"));
        Assert.assertTrue(json.contains("\"index\":0"));
        Assert.assertTrue(json.contains("\"httpCode\":200"));
    }

    // ── BulkTokenizeResponse ─────────────────────────────────────────────────

    private static BulkTokenizeResponseRecord tokenizeRecord(int index, Object value, TokenizeResponseToken... tokens) {
        return new BulkTokenizeResponseRecord(index, value, Arrays.asList(tokens));
    }

    private static TokenizeResponseToken okToken(String group, String token) {
        return new TokenizeResponseToken(group, token, 200, null);
    }

    private static TokenizeResponseToken failedToken(String group, String error) {
        return new TokenizeResponseToken(group, null, 400, error);
    }

    private static List<BulkTokenizeRequestRecord> payloadOf(int size) {
        List<BulkTokenizeRequestRecord> payload = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            payload.add(BulkTokenizeRequestRecord.builder().value("v" + i).build());
        }
        return payload;
    }

    @Test
    public void testBulkTokenizeResponse_singleArgConstructorLeavesSummaryNull() {
        List<BulkTokenizeResponseRecord> records = Collections.emptyList();

        BulkTokenizeResponse response = new BulkTokenizeResponse(records);

        Assert.assertNull(response.getSummary());
        Assert.assertEquals(records, response.getRecords());
    }

    @Test
    public void testBulkTokenizeResponse_twoArgConstructorClassifiesEachValue() {
        // index 0: all groups ok        -> totalTokenized
        // index 1: some ok, some failed -> totalPartial
        // index 2: all groups failed    -> totalFailed
        List<BulkTokenizeResponseRecord> records = Arrays.asList(
                tokenizeRecord(0, "v0", okToken("g1", "tok-0")),
                tokenizeRecord(1, "v1", okToken("g1", "tok-1"), failedToken("g2", "partial failure")),
                tokenizeRecord(2, "v2", failedToken("g1", "full failure")));

        BulkTokenizeResponse response = new BulkTokenizeResponse(records, payloadOf(3));

        TokenizeSummary summary = response.getSummary();
        Assert.assertNotNull(summary);
        // totalTokens counts input values submitted, not output token entries
        Assert.assertEquals(3, summary.getTotalTokens());
        Assert.assertEquals(1, summary.getTotalTokenized());
        Assert.assertEquals(1, summary.getTotalPartial());
        Assert.assertEquals(1, summary.getTotalFailed());
    }

    @Test
    public void testBulkTokenizeResponse_recordWithNoTokensCountsAsFailed() {
        List<BulkTokenizeResponseRecord> records = Collections.singletonList(
                new BulkTokenizeResponseRecord(0, "v0", Collections.emptyList()));

        BulkTokenizeResponse response = new BulkTokenizeResponse(records, payloadOf(1));

        Assert.assertEquals(0, response.getSummary().getTotalTokenized());
        Assert.assertEquals(1, response.getSummary().getTotalFailed());
    }

    @Test
    public void testBulkTokenizeResponse_getRecordsToRetryReturnsCallerRecordUnchanged() {
        // one value, four groups: only the 503 is retryable, but the whole record comes back
        List<BulkTokenizeResponseRecord> records = Collections.singletonList(
                tokenizeRecord(0, "9999999999",
                        okToken("phone_group", "p1q2r3s4"),
                        new TokenizeResponseToken("phone_group_2", null, 503, "unavailable"),
                        new TokenizeResponseToken("phone_group_3", null, 400, "bad group"),
                        new TokenizeResponseToken("phone_group_4", null, 529, "special case")));
        BulkTokenizeRequestRecord requested = BulkTokenizeRequestRecord.builder().value("9999999999")
                .tokenGroupNames(Arrays.asList(
                        "phone_group", "phone_group_2", "phone_group_3", "phone_group_4"))
                .build();

        List<BulkTokenizeRequestRecord> retry =
                new BulkTokenizeResponse(records, Collections.singletonList(requested)).getRecordsToRetry();

        Assert.assertEquals(1, retry.size());
        // the caller's own object is handed straight back, groups and all
        Assert.assertSame(requested, retry.get(0));
        Assert.assertEquals("9999999999", retry.get(0).getValue());
        Assert.assertEquals(Arrays.asList(
                "phone_group", "phone_group_2", "phone_group_3", "phone_group_4"),
                retry.get(0).getTokenGroupNames());
    }

    @Test
    public void testBulkTokenizeResponse_getRecordsToRetryOmitsValuesWithoutRetryableFailures() {
        List<BulkTokenizeResponseRecord> records = Arrays.asList(
                tokenizeRecord(0, "v0", okToken("g1", "tok-0")),
                tokenizeRecord(1, "v1", failedToken("g1", "bad group")),          // 400, not retryable
                tokenizeRecord(2, "v2", new TokenizeResponseToken("g1", null, 500, "server error")));

        List<BulkTokenizeRequestRecord> retry =
                new BulkTokenizeResponse(records, payloadOf(3)).getRecordsToRetry();

        Assert.assertEquals(1, retry.size());
        Assert.assertEquals("v2", retry.get(0).getValue());
    }

    @Test
    public void testBulkTokenizeResponse_getRecordsToRetryLooksUpByIndexNotResponseOrder() {
        // batches finish out of order, so the failing record is not first in the response
        List<BulkTokenizeResponseRecord> records = Arrays.asList(
                tokenizeRecord(2, "v2", new TokenizeResponseToken("g1", null, 500, "server error")),
                tokenizeRecord(0, "v0", okToken("g1", "tok-0")));

        List<BulkTokenizeRequestRecord> retry =
                new BulkTokenizeResponse(records, payloadOf(3)).getRecordsToRetry();

        Assert.assertEquals(1, retry.size());
        Assert.assertEquals("v2", retry.get(0).getValue());
    }

    @Test
    public void testBulkTokenizeResponse_getRecordsToRetrySkipsIndexOutsidePayload() {
        // a malformed index must not blow up with IndexOutOfBoundsException
        List<BulkTokenizeResponseRecord> records = Arrays.asList(
                tokenizeRecord(9, "stray", new TokenizeResponseToken("g1", null, 500, "server error")),
                tokenizeRecord(-1, "stray", new TokenizeResponseToken("g1", null, 500, "server error")),
                tokenizeRecord(0, "v0", new TokenizeResponseToken("g1", null, 500, "server error")));

        List<BulkTokenizeRequestRecord> retry =
                new BulkTokenizeResponse(records, payloadOf(1)).getRecordsToRetry();

        Assert.assertEquals(1, retry.size());
        Assert.assertEquals("v0", retry.get(0).getValue());
    }

    @Test
    public void testBulkTokenizeResponse_getRecordsToRetryCarriesByotToken() {
        List<BulkTokenizeResponseRecord> records = Collections.singletonList(
                tokenizeRecord(0, "v0", new TokenizeResponseToken("g1", null, 500, "server error")));
        List<BulkTokenizeRequestRecord> payload = Collections.singletonList(
                BulkTokenizeRequestRecord.builder().value("v0").token("my-own-token")
                        .tokenGroupNames(Collections.singletonList("g1")).build());

        List<BulkTokenizeRequestRecord> retry =
                new BulkTokenizeResponse(records, payload).getRecordsToRetry();

        Assert.assertEquals("my-own-token", retry.get(0).getToken());
    }

    @Test
    public void testBulkTokenizeResponse_getRecordsToRetryKeepsRequestedGroupsOnBatchFailure() {
        // a batch-level failure reports no group name on the token entry
        List<BulkTokenizeResponseRecord> records = Collections.singletonList(
                tokenizeRecord(0, "v0", new TokenizeResponseToken(null, null, 500, "server error")));
        List<BulkTokenizeRequestRecord> payload = Collections.singletonList(
                BulkTokenizeRequestRecord.builder().value("v0")
                        .tokenGroupNames(Arrays.asList("g1", "g2")).build());

        List<BulkTokenizeRequestRecord> retry =
                new BulkTokenizeResponse(records, payload).getRecordsToRetry();

        Assert.assertEquals(Arrays.asList("g1", "g2"), retry.get(0).getTokenGroupNames());
    }

    @Test
    public void testBulkTokenizeResponse_recordsToRetryNotSerialized() {
        List<BulkTokenizeResponseRecord> records = Collections.singletonList(
                tokenizeRecord(0, "v0", new TokenizeResponseToken("g1", null, 500, "server error")));
        BulkTokenizeResponse response = new BulkTokenizeResponse(records, payloadOf(1));

        response.getRecordsToRetry();   // populate the lazily-derived field

        Assert.assertFalse(response.toString().contains("recordsToRetry"));
    }

    @Test
    public void testBulkTokenizeResponse_toStringMatchesContractShape() {
        List<BulkTokenizeResponseRecord> records = Arrays.asList(
                tokenizeRecord(0, "john@example.com", okToken("email_group", "a1b2c3d4")),
                tokenizeRecord(1, "9999999999",
                        okToken("phone_group", "p1q2r3s4"),
                        failedToken("phone_group_2", "Invalid token group configuration")));

        String json = new BulkTokenizeResponse(records, payloadOf(2)).toString();

        Assert.assertTrue(json.contains("\"summary\""));
        Assert.assertTrue(json.contains("\"records\""));
        Assert.assertFalse(json.contains("originalPayload"));
        Assert.assertTrue(json.contains("\"error\":null"));
        Assert.assertTrue(json.contains("\"index\":0"));
        Assert.assertTrue(json.contains("\"tokenGroupName\":\"email_group\""));
    }
}
