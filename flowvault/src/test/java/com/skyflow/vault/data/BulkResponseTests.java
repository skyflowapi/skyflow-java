package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for the Bulk*Response classes: {@link BulkInsertResponse}, {@link BulkDetokenizeResponse},
 * {@link BulkDeleteTokensResponse} and {@link BulkTokenizeResponse}.
 *
 * <p>Each has a 2-arg constructor (success/errors only, summary and originalPayload-derived
 * fields stay null) and a 3-arg constructor (adds originalPayload and computes a summary).
 * BulkInsertResponse/BulkDetokenizeResponse additionally derive a retry list from the
 * originalPayload, filtering on a "5xx except 529" retryable-status rule.
 */
public class BulkResponseTests {

    // ── BulkInsertResponse ───────────────────────────────────────────────────

    @Test
    public void testBulkInsertResponse_twoArgConstructorLeavesSummaryAndRetryDependenciesNull() {
        List<Success> success = Collections.emptyList();
        List<ErrorRecord> errors = Collections.emptyList();

        BulkInsertResponse response = new BulkInsertResponse(success, errors);

        Assert.assertNull(response.getSummary());
        Assert.assertEquals(success, response.getSuccess());
        Assert.assertEquals(errors, response.getErrors());
        // No errors, so the retry list is empty and originalPayload (null) is never dereferenced.
        Assert.assertTrue(response.getRecordsToRetry().isEmpty());
    }

    @Test
    public void testBulkInsertResponse_threeArgConstructorComputesSummary() {
        List<Success> success = Collections.singletonList(
                new Success(0, "id-1", null, null, "table1"));
        List<ErrorRecord> errors = Collections.singletonList(
                new ErrorRecord(1, "failed", 400));
        ArrayList<BulkInsertRecord> originalPayload = new ArrayList<>(Arrays.asList(
                BulkInsertRecord.builder().table("table1").build(),
                BulkInsertRecord.builder().table("table1").build()));

        BulkInsertResponse response = new BulkInsertResponse(success, errors, originalPayload);

        Assert.assertNotNull(response.getSummary());
        Assert.assertEquals(2, response.getSummary().getTotalRecords());
        Assert.assertEquals(1, response.getSummary().getTotalInserted());
        Assert.assertEquals(1, response.getSummary().getTotalFailed());
    }

    @Test
    public void testBulkInsertResponse_getRecordsToRetryFiltersRetryableStatusCodesOnly() {
        BulkInsertRecord record0 = BulkInsertRecord.builder().table("t0").build();
        BulkInsertRecord record1 = BulkInsertRecord.builder().table("t1").build();
        BulkInsertRecord record2 = BulkInsertRecord.builder().table("t2").build();
        BulkInsertRecord record3 = BulkInsertRecord.builder().table("t3").build();
        ArrayList<BulkInsertRecord> originalPayload = new ArrayList<>(
                Arrays.asList(record0, record1, record2, record3));

        List<ErrorRecord> errors = Arrays.asList(
                new ErrorRecord(0, "server error", 500),   // retryable (lower bound)
                new ErrorRecord(1, "bad request", 400),    // not retryable
                new ErrorRecord(2, "server error", 599),   // retryable (upper bound)
                new ErrorRecord(3, "special case", 529));  // explicitly excluded

        BulkInsertResponse response = new BulkInsertResponse(
                Collections.emptyList(), errors, originalPayload);

        ArrayList<BulkInsertRecord> recordsToRetry = response.getRecordsToRetry();

        Assert.assertEquals(2, recordsToRetry.size());
        Assert.assertTrue(recordsToRetry.contains(record0));
        Assert.assertTrue(recordsToRetry.contains(record2));
        Assert.assertFalse(recordsToRetry.contains(record1));
        Assert.assertFalse(recordsToRetry.contains(record3));
    }

    @Test
    public void testBulkInsertResponse_toStringNotNull() {
        BulkInsertResponse response = new BulkInsertResponse(Collections.emptyList(), Collections.emptyList());
        Assert.assertNotNull(response.toString());
    }

    // ── BulkDetokenizeResponse ───────────────────────────────────────────────

    @Test
    public void testBulkDetokenizeResponse_twoArgConstructorLeavesSummaryNull() {
        List<DetokenizeResponseObject> success = Collections.emptyList();
        List<ErrorRecord> errors = Collections.emptyList();

        BulkDetokenizeResponse response = new BulkDetokenizeResponse(success, errors);

        Assert.assertNull(response.getSummary());
        Assert.assertEquals(success, response.getSuccess());
        Assert.assertEquals(errors, response.getErrors());
        Assert.assertTrue(response.getTokensToRetry().isEmpty());
    }

    @Test
    public void testBulkDetokenizeResponse_threeArgConstructorComputesSummary() {
        List<DetokenizeResponseObject> success = Collections.singletonList(
                new DetokenizeResponseObject(0, "tok-1", "value", "group1", null, null));
        List<ErrorRecord> errors = Collections.singletonList(new ErrorRecord(1, "failed", 404));
        List<String> originalPayload = Arrays.asList("tok-1", "tok-2");

        BulkDetokenizeResponse response = new BulkDetokenizeResponse(success, errors, originalPayload);

        Assert.assertNotNull(response.getSummary());
        Assert.assertEquals(2, response.getSummary().getTotalTokens());
        Assert.assertEquals(1, response.getSummary().getTotalDetokenized());
        Assert.assertEquals(1, response.getSummary().getTotalFailed());
    }

    @Test
    public void testBulkDetokenizeResponse_getTokensToRetryFiltersRetryableStatusCodesOnly() {
        List<String> originalPayload = Arrays.asList("tok-0", "tok-1", "tok-2", "tok-3");
        List<ErrorRecord> errors = Arrays.asList(
                new ErrorRecord(0, "server error", 500),
                new ErrorRecord(1, "bad request", 400),
                new ErrorRecord(2, "server error", 599),
                new ErrorRecord(3, "special case", 529));

        BulkDetokenizeResponse response = new BulkDetokenizeResponse(
                Collections.emptyList(), errors, originalPayload);

        List<String> tokensToRetry = response.getTokensToRetry();

        Assert.assertEquals(2, tokensToRetry.size());
        Assert.assertTrue(tokensToRetry.contains("tok-0"));
        Assert.assertTrue(tokensToRetry.contains("tok-2"));
        Assert.assertFalse(tokensToRetry.contains("tok-1"));
        Assert.assertFalse(tokensToRetry.contains("tok-3"));
    }

    @Test
    public void testBulkDetokenizeResponse_toStringNotNull() {
        BulkDetokenizeResponse response = new BulkDetokenizeResponse(Collections.emptyList(), Collections.emptyList());
        Assert.assertNotNull(response.toString());
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
