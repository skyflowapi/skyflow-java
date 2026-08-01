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
    public void testBulkDeleteTokensResponse_twoArgConstructorLeavesSummaryNull() {
        List<DeleteTokensSuccess> success = Collections.emptyList();
        List<ErrorRecord> errors = Collections.emptyList();

        BulkDeleteTokensResponse response = new BulkDeleteTokensResponse(success, errors);

        Assert.assertNull(response.getSummary());
        Assert.assertEquals(success, response.getSuccess());
        Assert.assertEquals(errors, response.getErrors());
    }

    @Test
    public void testBulkDeleteTokensResponse_threeArgConstructorComputesSummary() {
        List<DeleteTokensSuccess> success = Collections.singletonList(new DeleteTokensSuccess(0, "tok-1"));
        List<ErrorRecord> errors = Collections.singletonList(new ErrorRecord(1, "failed", 404));
        List<String> originalPayload = Arrays.asList("tok-1", "tok-2");

        BulkDeleteTokensResponse response = new BulkDeleteTokensResponse(success, errors, originalPayload);

        Assert.assertNotNull(response.getSummary());
        Assert.assertEquals(2, response.getSummary().getTotalTokens());
        Assert.assertEquals(1, response.getSummary().getTotalDeleted());
        Assert.assertEquals(1, response.getSummary().getTotalFailed());
    }

    @Test
    public void testBulkDeleteTokensResponse_toStringNotNull() {
        BulkDeleteTokensResponse response = new BulkDeleteTokensResponse(Collections.emptyList(), Collections.emptyList());
        Assert.assertNotNull(response.toString());
    }

    // ── BulkTokenizeResponse ─────────────────────────────────────────────────

    @Test
    public void testBulkTokenizeResponse_twoArgConstructorLeavesSummaryNull() {
        List<TokenizeSuccess> success = Collections.emptyList();
        List<ErrorRecord> errors = Collections.emptyList();

        BulkTokenizeResponse response = new BulkTokenizeResponse(success, errors);

        Assert.assertNull(response.getSummary());
        Assert.assertEquals(success, response.getSuccess());
        Assert.assertEquals(errors, response.getErrors());
    }

    @Test
    public void testBulkTokenizeResponse_threeArgConstructorClassifiesEachIndex() {
        // index 0: success only              -> totalTokenized
        // index 1: success AND error         -> totalPartial
        // index 2: error only                -> totalFailed
        List<TokenizeSuccess> success = Arrays.asList(
                new TokenizeSuccess(0, "v0"),
                new TokenizeSuccess(1, "v1"));
        List<ErrorRecord> errors = Arrays.asList(
                new ErrorRecord(1, "partial failure", 500),
                new ErrorRecord(2, "full failure", 404));
        List<BulkTokenizeRecord> originalPayload = Arrays.asList(
                BulkTokenizeRecord.builder().value("v0").build(),
                BulkTokenizeRecord.builder().value("v1").build(),
                BulkTokenizeRecord.builder().value("v2").build());

        BulkTokenizeResponse response = new BulkTokenizeResponse(success, errors, originalPayload);

        TokenizeSummary summary = response.getSummary();
        Assert.assertNotNull(summary);
        Assert.assertEquals(3, summary.getTotalTokens());
        Assert.assertEquals(1, summary.getTotalTokenized());
        Assert.assertEquals(1, summary.getTotalPartial());
        Assert.assertEquals(1, summary.getTotalFailed());
    }

    @Test
    public void testBulkTokenizeResponse_toStringNotNull() {
        BulkTokenizeResponse response = new BulkTokenizeResponse(Collections.emptyList(), Collections.emptyList());
        Assert.assertNotNull(response.toString());
    }
}
