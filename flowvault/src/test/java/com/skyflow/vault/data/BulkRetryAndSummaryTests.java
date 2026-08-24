package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Summary classification and retry-list derivation on the bulk responses.
 *
 * <p>Retryable is defined as "has an error AND a 5xx status other than 529", so each conjunct is
 * exercised separately — a 4xx failure, a 5xx with no error, a null status, and the 529 carve-out
 * must all stay out of the retry list.
 */
public class BulkRetryAndSummaryTests {

    private static BulkTokenizeResponseRecord row(int index, String group, String token, Integer httpCode, String error) {
        return new BulkTokenizeResponseRecord(index, "value" + index, group, token, httpCode, error, null);
    }

    private static BulkTokenizeRequestRecord requestRecord(String value) {
        return (BulkTokenizeRequestRecord) BulkTokenizeRequestRecord.builder().value(value).build();
    }

    // ── BulkTokenizeResponse.buildSummary ─────────────────────────────────────

    @Test
    public void testTokenizeSummary_allGroupsSucceededCountsAsTokenized() {
        List<BulkTokenizeResponseRecord> records = Arrays.asList(
                row(0, "g1", "t1", 200, null),
                row(0, "g2", "t2", 200, null));

        TokenizeSummary summary = new BulkTokenizeResponse(records, Collections.singletonList(requestRecord("a")))
                .getSummary();

        Assert.assertEquals(1, summary.getTotalTokens());
        Assert.assertEquals(1, summary.getTotalTokenized());
        Assert.assertEquals(0, summary.getTotalPartial());
        Assert.assertEquals(0, summary.getTotalFailed());
    }

    @Test
    public void testTokenizeSummary_someGroupsFailedCountsAsPartial() {
        List<BulkTokenizeResponseRecord> records = Arrays.asList(
                row(0, "g1", "t1", 200, null),
                row(0, "g2", null, 400, "bad group"));

        TokenizeSummary summary = new BulkTokenizeResponse(records, Collections.singletonList(requestRecord("a")))
                .getSummary();

        Assert.assertEquals(0, summary.getTotalTokenized());
        Assert.assertEquals(1, summary.getTotalPartial());
        Assert.assertEquals(0, summary.getTotalFailed());
    }

    @Test
    public void testTokenizeSummary_everyGroupFailedCountsAsFailed() {
        List<BulkTokenizeResponseRecord> records = Arrays.asList(
                row(0, "g1", null, 500, "boom"),
                row(0, "g2", null, 500, "boom"));

        TokenizeSummary summary = new BulkTokenizeResponse(records, Collections.singletonList(requestRecord("a")))
                .getSummary();

        Assert.assertEquals(0, summary.getTotalTokenized());
        Assert.assertEquals(0, summary.getTotalPartial());
        Assert.assertEquals(1, summary.getTotalFailed());
    }

    @Test
    public void testTokenizeSummary_noRowsAtAllCountsAsFailed() {
        // A value with no rows in the response at all is a failure, not a success.
        TokenizeSummary summary = new BulkTokenizeResponse(new ArrayList<>(), Collections.singletonList(requestRecord("a")))
                .getSummary();

        Assert.assertEquals(1, summary.getTotalFailed());
    }

    @Test
    public void testTokenizeSummary_totalTokensComesFromTheSubmittedPayload() {
        // Two values submitted, only one came back — totalTokens must reflect what was sent.
        List<BulkTokenizeResponseRecord> records = Collections.singletonList(
                row(0, "g1", "t1", 200, null));

        TokenizeSummary summary = new BulkTokenizeResponse(
                records, Arrays.asList(requestRecord("a"), requestRecord("b"))).getSummary();

        Assert.assertEquals(2, summary.getTotalTokens());
        Assert.assertEquals(1, summary.getTotalTokenized());
    }

    @Test
    public void testTokenizeSummary_nullRecordsYieldsZeroes() {
        TokenizeSummary summary = new BulkTokenizeResponse(null, new ArrayList<>()).getSummary();

        Assert.assertEquals(0, summary.getTotalTokens());
        Assert.assertEquals(0, summary.getTotalTokenized());
        Assert.assertEquals(0, summary.getTotalPartial());
        Assert.assertEquals(0, summary.getTotalFailed());
    }

    @Test
    public void testTokenizeSummary_isNullWhenNoPayloadWasSupplied() {
        // The single-arg constructor is the "no summary" path used before batching completes.
        Assert.assertNull(new BulkTokenizeResponse(new ArrayList<>()).getSummary());
    }

    @Test
    public void testTokenizeSummary_classifiesByRowsWhenNoPayloadIsGiven() {
        // The two-arg constructor can still be called with a null payload; classification then
        // falls back to the indexes actually present in records instead of the submitted list.
        List<BulkTokenizeResponseRecord> records = Arrays.asList(
                row(0, "g1", "t1", 200, null),
                row(1, "g1", null, 400, "bad group"),
                row(2, "g1", "t2", 200, null),
                row(2, "g2", null, 400, "bad group"));

        TokenizeSummary summary = new BulkTokenizeResponse(records, null).getSummary();

        // without a submitted payload to count values from, totalTokens falls back to the row count
        Assert.assertEquals(4, summary.getTotalTokens());
        Assert.assertEquals(1, summary.getTotalTokenized());
        Assert.assertEquals(1, summary.getTotalPartial());
        Assert.assertEquals(1, summary.getTotalFailed());
    }

    @Test
    public void testTokenizeSummary_nullRecordsAndNullPayloadYieldsZeroes() {
        TokenizeSummary summary = new BulkTokenizeResponse(null, null).getSummary();

        Assert.assertEquals(0, summary.getTotalTokens());
        Assert.assertEquals(0, summary.getTotalTokenized());
        Assert.assertEquals(0, summary.getTotalPartial());
        Assert.assertEquals(0, summary.getTotalFailed());
    }

    // ── BulkTokenizeResponse.getRecordsToRetry ────────────────────────────────

    @Test
    public void testTokenizeRetry_withNullRecordsReturnsEmpty() {
        Assert.assertTrue(new BulkTokenizeResponse(null, Collections.singletonList(requestRecord("a")))
                .getRecordsToRetry().isEmpty());
    }

    @Test
    public void testTokenizeRetry_only5xxFailuresAreReturned() {
        List<BulkTokenizeResponseRecord> records = Arrays.asList(
                row(0, "g", null, 500, "server"),
                row(1, "g", null, 400, "client"),
                row(2, "g", "t", 200, null));
        List<BulkTokenizeRequestRecord> payload =
                Arrays.asList(requestRecord("a"), requestRecord("b"), requestRecord("c"));

        List<BulkTokenizeRequestRecord> retry = new BulkTokenizeResponse(records, payload).getRecordsToRetry();

        Assert.assertEquals(1, retry.size());
        Assert.assertSame("must return the caller's own record object", payload.get(0), retry.get(0));
    }

    @Test
    public void testTokenizeRetry_529IsNotRetried() {
        List<BulkTokenizeResponseRecord> records = Collections.singletonList(
                row(0, "g", null, 529, "site frozen"));

        Assert.assertTrue(new BulkTokenizeResponse(records, Collections.singletonList(requestRecord("a")))
                .getRecordsToRetry().isEmpty());
    }

    @Test
    public void testTokenizeRetry_599IsRetriedAnd600IsNot() {
        Assert.assertEquals(1, new BulkTokenizeResponse(
                Collections.singletonList(row(0, "g", null, 599, "edge")),
                Collections.singletonList(requestRecord("a"))).getRecordsToRetry().size());
        Assert.assertEquals(0, new BulkTokenizeResponse(
                Collections.singletonList(row(0, "g", null, 600, "edge")),
                Collections.singletonList(requestRecord("a"))).getRecordsToRetry().size());
    }

    @Test
    public void testTokenizeRetry_5xxWithoutAnErrorIsNotRetried() {
        List<BulkTokenizeResponseRecord> records = Collections.singletonList(
                row(0, "g", "t", 500, null));

        Assert.assertTrue(new BulkTokenizeResponse(records, Collections.singletonList(requestRecord("a")))
                .getRecordsToRetry().isEmpty());
    }

    @Test
    public void testTokenizeRetry_nullHttpCodeIsNotRetried() {
        List<BulkTokenizeResponseRecord> records = Collections.singletonList(
                row(0, "g", null, null, "no status"));

        Assert.assertTrue(new BulkTokenizeResponse(records, Collections.singletonList(requestRecord("a")))
                .getRecordsToRetry().isEmpty());
    }

    @Test
    public void testTokenizeRetry_partialFailureRetriesTheWholeRecord() {
        // One group failed retryably, another succeeded — the value still needs resubmitting.
        List<BulkTokenizeResponseRecord> records = Arrays.asList(
                row(0, "g1", "t1", 200, null),
                row(0, "g2", null, 503, "down"));

        Assert.assertEquals(1, new BulkTokenizeResponse(records, Collections.singletonList(requestRecord("a")))
                .getRecordsToRetry().size());
    }

    @Test
    public void testTokenizeRetry_recordWithNoRowsIsNotRetried() {
        Assert.assertTrue(new BulkTokenizeResponse(new ArrayList<>(), Collections.singletonList(requestRecord("a")))
                .getRecordsToRetry().isEmpty());
    }

    @Test
    public void testTokenizeRetry_outOfRangeIndexIsSkippedRatherThanThrowing() {
        List<BulkTokenizeResponseRecord> records = Arrays.asList(
                row(5, "g", null, 500, "server"),
                row(-1, "g", null, 500, "server"));

        Assert.assertTrue(new BulkTokenizeResponse(records, Collections.singletonList(requestRecord("a")))
                .getRecordsToRetry().isEmpty());
    }

    @Test
    public void testTokenizeRetry_withoutOriginalPayloadReturnsEmpty() {
        List<BulkTokenizeResponseRecord> records = Collections.singletonList(
                row(0, "g", null, 500, "server"));

        Assert.assertTrue(new BulkTokenizeResponse(records).getRecordsToRetry().isEmpty());
    }

    @Test
    public void testTokenizeRetry_isMemoisedAcrossCalls() {
        List<BulkTokenizeResponseRecord> records = Collections.singletonList(
                row(0, "g", null, 500, "server"));
        BulkTokenizeResponse response =
                new BulkTokenizeResponse(records, Collections.singletonList(requestRecord("a")));

        Assert.assertSame(response.getRecordsToRetry(), response.getRecordsToRetry());
    }

    // ── BulkDeleteTokensResponse ──────────────────────────────────────────────

    private static BulkDeleteTokensResponseRecord deleteRecord(int index, String token, Integer code, String error) {
        return new BulkDeleteTokensResponseRecord(index, token, code, error);
    }

    @Test
    public void testDeleteTokensSummary_countsDeletedAndFailed() {
        List<BulkDeleteTokensResponseRecord> records = Arrays.asList(
                deleteRecord(0, "t1", 200, null),
                deleteRecord(1, "t2", 404, "not found"));

        DeleteTokensSummary summary =
                new BulkDeleteTokensResponse(records, Arrays.asList("t1", "t2")).getSummary();

        Assert.assertEquals(2, summary.getTotalTokens());
        Assert.assertEquals(1, summary.getTotalDeleted());
        Assert.assertEquals(1, summary.getTotalFailed());
    }

    @Test
    public void testDeleteTokensSummary_totalFallsBackToRecordCountWithoutPayload() {
        List<BulkDeleteTokensResponseRecord> records = Arrays.asList(
                deleteRecord(0, "t1", 200, null),
                deleteRecord(1, "t2", 500, "boom"));

        DeleteTokensSummary summary = new BulkDeleteTokensResponse(records, null).getSummary();

        Assert.assertEquals(2, summary.getTotalTokens());
    }

    @Test
    public void testDeleteTokensSummary_nullRecordsYieldsZeroes() {
        DeleteTokensSummary summary = new BulkDeleteTokensResponse(null, new ArrayList<>()).getSummary();

        Assert.assertEquals(0, summary.getTotalTokens());
        Assert.assertEquals(0, summary.getTotalDeleted());
        Assert.assertEquals(0, summary.getTotalFailed());
    }

    @Test
    public void testDeleteTokensRetry_only5xxFailuresAreReturned() {
        List<BulkDeleteTokensResponseRecord> records = Arrays.asList(
                deleteRecord(0, "t1", 500, "server"),
                deleteRecord(1, "t2", 400, "client"),
                deleteRecord(2, "t3", 200, null));

        List<String> retry = new BulkDeleteTokensResponse(records, Arrays.asList("t1", "t2", "t3"))
                .getTokensToRetry();

        Assert.assertEquals(Collections.singletonList("t1"), retry);
    }

    @Test
    public void testDeleteTokensRetry_529IsNotRetried() {
        List<BulkDeleteTokensResponseRecord> records =
                Collections.singletonList(deleteRecord(0, "t1", 529, "site frozen"));

        Assert.assertTrue(new BulkDeleteTokensResponse(records, Collections.singletonList("t1"))
                .getTokensToRetry().isEmpty());
    }

    @Test
    public void testDeleteTokensRetry_5xxWithoutErrorOrNullCodeIsNotRetried() {
        Assert.assertTrue(new BulkDeleteTokensResponse(
                Collections.singletonList(deleteRecord(0, "t1", 500, null)),
                Collections.singletonList("t1")).getTokensToRetry().isEmpty());
        Assert.assertTrue(new BulkDeleteTokensResponse(
                Collections.singletonList(deleteRecord(0, "t1", null, "no status")),
                Collections.singletonList("t1")).getTokensToRetry().isEmpty());
    }

    @Test
    public void testDeleteTokensRetry_nullTokenIsSkipped() {
        // A failed record with no token echoed back cannot be resubmitted.
        List<BulkDeleteTokensResponseRecord> records =
                Collections.singletonList(deleteRecord(0, null, 500, "server"));

        Assert.assertTrue(new BulkDeleteTokensResponse(records, Collections.singletonList("t1"))
                .getTokensToRetry().isEmpty());
    }

    @Test
    public void testDeleteTokensRetry_isMemoisedAcrossCalls() {
        List<BulkDeleteTokensResponseRecord> records =
                Collections.singletonList(deleteRecord(0, "t1", 500, "server"));
        BulkDeleteTokensResponse response =
                new BulkDeleteTokensResponse(records, Collections.singletonList("t1"));

        Assert.assertSame(response.getTokensToRetry(), response.getTokensToRetry());
    }

    @Test
    public void testDeleteTokensRetry_withNullRecordsReturnsEmpty() {
        Assert.assertTrue(new BulkDeleteTokensResponse(null, Collections.singletonList("t1"))
                .getTokensToRetry().isEmpty());
    }
}
