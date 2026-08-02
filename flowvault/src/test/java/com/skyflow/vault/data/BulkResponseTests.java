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
                new BulkInsertResponseRecord(0, "table1", "id-1", null, null, 200, null, null),
                new BulkInsertResponseRecord(1, null, null, null, null, 400, "failed", null));
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
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", "token-name");
        Map<String, Object> hashedData = new HashMap<>();
        hashedData.put("name", "hashed-name");

        BulkInsertResponseRecord record = new BulkInsertResponseRecord(
                7, "table1", "id-1", fields, hashedData, 200, null, null);

        BulkInsertResponse response = new BulkInsertResponse(Collections.singletonList(record));

        BulkInsertResponseRecord actual = response.getRecords().get(0);
        Assert.assertEquals(7, actual.getIndex());
        Assert.assertEquals("table1", actual.getTableName());
        Assert.assertEquals("id-1", actual.getSkyflowId());
        Assert.assertEquals(fields, actual.getFields());
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
                new BulkInsertResponseRecord(0, null, null, null, null, 500, "server error", null),  // retryable (lower bound)
                new BulkInsertResponseRecord(1, null, null, null, null, 400, "bad request", null),   // not retryable
                new BulkInsertResponseRecord(2, null, null, null, null, 599, "server error", null),  // retryable (upper bound)
                new BulkInsertResponseRecord(3, null, null, null, null, 529, "special case", null)); // explicitly excluded

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
                new BulkInsertResponseRecord(0, "table1", "id-1", null, null, 200, null, null));
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
                new BulkInsertResponseRecord(0, null, null, null, null, 500, "server error", null));

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
