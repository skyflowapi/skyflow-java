package com.skyflow.utils;

import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.core.ObjectMappers;
import com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest;
import com.skyflow.generated.rest.types.V1DeleteTokenResponseObject;
import com.skyflow.generated.rest.types.V1FlowDeleteTokenResponse;
import com.skyflow.generated.rest.types.V1FlowTokenizeResponse;
import com.skyflow.utils.BaseConstants;
import com.skyflow.vault.data.BulkDeleteTokensResponse;
import com.skyflow.vault.data.BulkDeleteTokensResponseRecord;
import com.skyflow.vault.data.BulkTokenizeRequestRecord;
import com.skyflow.vault.data.BulkTokenizeResponse;
import com.skyflow.vault.data.BulkTokenizeResponseRecord;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The API does not report a request id per record; it comes back once per call in the
 * {@code x-request-id} response header. Bulk operations split the caller's list across several
 * calls, so the SDK stamps each error with the id of the call it came from - and only errors, since
 * that is when it is useful for support.
 *
 * <p>These cover the two properties that matter: every error from one batch shares a single id, and
 * errors from different batches carry different ones.
 */
public class RequestIdTests {

    private static final String REQ_ID_A = "req-aaaa-1111";
    private static final String REQ_ID_B = "req-bbbb-2222";

    private static Map<String, List<String>> headers(String requestId) {
        Map<String, List<String>> headers = new HashMap<>();
        if (requestId != null) {
            headers.put(BaseConstants.REQUEST_ID_HEADER_KEY, Collections.singletonList(requestId));
        }
        return headers;
    }

    /**
     * Builds the exception the SDK actually sees on a non-2xx, carrying a real okhttp response so
     * the header is read the same way it is in production rather than injected.
     */
    private static ApiClientApiException apiException(String message, int status, Object body,
                                                      String requestId) {
        Response.Builder raw = new Response.Builder()
                .request(new Request.Builder().url("https://vault.example.test/v1/tokenize").build())
                .protocol(Protocol.HTTP_1_1)
                .code(status)
                .message(message);
        if (requestId != null) {
            raw.header(BaseConstants.REQUEST_ID_HEADER_KEY, requestId);
        }
        return new ApiClientApiException(message, status, body, raw.build());
    }

    private static V1FlowTokenizeResponse tokenizeWire(String json) {
        try {
            return ObjectMappers.JSON_MAPPER.readValue(json, V1FlowTokenizeResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static BulkTokenizeRequestRecord tokenizeRecord(Object value, String... groups) {
        return BulkTokenizeRequestRecord.builder()
                .value(value).tokenGroupNames(Arrays.asList(groups)).build();
    }

    // ── tokenize: success carries no id, errors carry the batch's ──────────────

    @Test
    public void testTokenize_requestIdOnErrorsOnly() {
        String json = "{\"response\": ["
                + "{\"value\":\"ok\",\"tokenGroupName\":\"g1\",\"token\":\"tok-1\",\"httpCode\":200,\"error\":null},"
                + "{\"value\":\"bad\",\"tokenGroupName\":null,\"token\":\"\",\"httpCode\":400,"
                + " \"error\":\"Token group g9 is invalid.\"}"
                + "]}";
        List<BulkTokenizeRequestRecord> sent = Arrays.asList(
                tokenizeRecord("ok", "g1"), tokenizeRecord("bad", "g9"));

        BulkTokenizeResponse result = Utils.formatBulkTokenizeResponse(
                tokenizeWire(json), sent, 0, headers(REQ_ID_A));

        BulkTokenizeResponseRecord success = result.getRecords().get(0);
        BulkTokenizeResponseRecord failure = result.getRecords().get(1);
        Assert.assertNull("a successful token must not carry a request id", success.getRequestId());
        Assert.assertEquals(REQ_ID_A, failure.getRequestId());
    }

    @Test
    public void testTokenize_everyErrorInOneBatchSharesOneRequestId() {
        String json = "{\"response\": ["
                + "{\"value\":\"v0\",\"tokenGroupName\":null,\"token\":\"\",\"httpCode\":400,\"error\":\"bad group\"},"
                + "{\"value\":\"v1\",\"tokenGroupName\":null,\"token\":\"\",\"httpCode\":400,\"error\":\"bad group\"},"
                + "{\"value\":\"v2\",\"tokenGroupName\":null,\"token\":\"\",\"httpCode\":400,\"error\":\"bad group\"}"
                + "]}";
        List<BulkTokenizeRequestRecord> sent = Arrays.asList(
                tokenizeRecord("v0", "g1"), tokenizeRecord("v1", "g1"), tokenizeRecord("v2", "g1"));

        BulkTokenizeResponse result = Utils.formatBulkTokenizeResponse(
                tokenizeWire(json), sent, 0, headers(REQ_ID_A));

        Set<String> ids = tokenizeRequestIds(result.getRecords());
        Assert.assertEquals(Collections.singleton(REQ_ID_A), ids);
    }

    @Test
    public void testTokenize_twoBatchesReportTwoDistinctRequestIds() {
        String batchJson = "{\"response\": ["
                + "{\"value\":\"%s\",\"tokenGroupName\":null,\"token\":\"\",\"httpCode\":400,\"error\":\"bad group\"},"
                + "{\"value\":\"%s\",\"tokenGroupName\":null,\"token\":\"\",\"httpCode\":400,\"error\":\"bad group\"}"
                + "]}";

        // batch 0 occupies indexes 0-1 and answers with REQ_ID_A
        BulkTokenizeResponse first = Utils.formatBulkTokenizeResponse(
                tokenizeWire(String.format(batchJson, "v0", "v1")),
                Arrays.asList(tokenizeRecord("v0", "g1"), tokenizeRecord("v1", "g1")),
                0, headers(REQ_ID_A));
        // batch 1 occupies indexes 2-3 and answers with REQ_ID_B
        BulkTokenizeResponse second = Utils.formatBulkTokenizeResponse(
                tokenizeWire(String.format(batchJson, "v2", "v3")),
                Arrays.asList(tokenizeRecord("v2", "g1"), tokenizeRecord("v3", "g1")),
                2, headers(REQ_ID_B));

        Assert.assertEquals(Collections.singleton(REQ_ID_A), tokenizeRequestIds(first.getRecords()));
        Assert.assertEquals(Collections.singleton(REQ_ID_B), tokenizeRequestIds(second.getRecords()));

        // merged, as the controller returns them: index tells you the record, requestId the call
        List<BulkTokenizeResponseRecord> merged = new ArrayList<>(first.getRecords());
        merged.addAll(second.getRecords());
        Assert.assertEquals(4, merged.size());
        Assert.assertEquals(REQ_ID_A, merged.get(0).getRequestId());
        Assert.assertEquals(REQ_ID_A, merged.get(1).getRequestId());
        Assert.assertEquals(REQ_ID_B, merged.get(2).getRequestId());
        Assert.assertEquals(REQ_ID_B, merged.get(3).getRequestId());
        Assert.assertEquals(2, merged.get(2).getIndex());
        Assert.assertEquals(3, merged.get(3).getIndex());
    }

    @Test
    public void testTokenize_missingHeaderLeavesRequestIdNull() {
        String json = "{\"response\": [{\"value\":\"v0\",\"tokenGroupName\":null,\"token\":\"\","
                + "\"httpCode\":400,\"error\":\"bad group\"}]}";
        BulkTokenizeResponse result = Utils.formatBulkTokenizeResponse(
                tokenizeWire(json), Collections.singletonList(tokenizeRecord("v0", "g1")),
                0, headers(null));

        Assert.assertNull(result.getRecords().get(0).getRequestId());
    }

    @Test
    public void testTokenize_partialRecordStampsOnlyTheFailedGroup() {
        String json = "{\"response\": ["
                + "{\"value\":\"v0\",\"tokenGroupName\":\"g1\",\"token\":\"tok-1\",\"httpCode\":200,\"error\":null},"
                + "{\"value\":\"v0\",\"tokenGroupName\":null,\"token\":\"\",\"httpCode\":400,\"error\":\"bad group\"}"
                + "]}";
        BulkTokenizeResponse result = Utils.formatBulkTokenizeResponse(
                tokenizeWire(json), Collections.singletonList(tokenizeRecord("v0", "g1", "g2")),
                0, headers(REQ_ID_A));

        List<BulkTokenizeResponseRecord> records = result.getRecords();
        Assert.assertEquals(2, records.size());
        Assert.assertNull(records.get(0).getRequestId());
        Assert.assertEquals(REQ_ID_A, records.get(1).getRequestId());
    }

    // ── tokenize: rejected requests ────────────────────────────────────────────

    @Test
    public void testTokenize_rejectedRequestStampsIdFromTheFailedCall() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("token", "");
        row.put("value", "v0");
        row.put("tokenGroupName", null);
        row.put("error", "Invalid request. BYOT token should contain one token group.");
        row.put("httpCode", 400);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("response", Collections.singletonList(row));

        Throwable ex = new RuntimeException(
                apiException("Error with status code 400", 400, body, REQ_ID_B));

        List<BulkTokenizeResponseRecord> records = Utils.handleBulkTokenizeBatchException(
                ex, Collections.singletonList(tokenizeRecord("v0", "g1")), 0);

        Assert.assertEquals(REQ_ID_B, records.get(0).getRequestId());
    }

    @Test
    public void testTokenize_rejectedRequestWithNoBodyStillStampsEveryGroup() {
        Throwable ex = new RuntimeException(
                apiException("boom", 503, null, REQ_ID_A));

        List<BulkTokenizeResponseRecord> records = Utils.handleBulkTokenizeBatchException(
                ex, Collections.singletonList(tokenizeRecord("v0", "g1", "g2")), 0);

        Assert.assertEquals(2, records.size());
        Assert.assertEquals(REQ_ID_A, records.get(0).getRequestId());
        Assert.assertEquals(REQ_ID_A, records.get(1).getRequestId());
    }

    @Test
    public void testTokenize_transportFailureReportsTheInnermostCause() {
        // a mistyped cluster id surfaces as UnknownHostException three levels down: the future
        // wraps ApiClientException("Network error..."), which wraps the real cause. Reporting the
        // wrapper tells the caller nothing, so the innermost cause must win.
        java.net.UnknownHostException dns = new java.net.UnknownHostException(
                "badcluster.skyvault.skyflowapis.dev: nodename nor servname provided, or not known");
        Throwable ex = new RuntimeException(
                new com.skyflow.generated.rest.core.ApiClientException(
                        "Network error executing HTTP request", dns));

        List<BulkTokenizeResponseRecord> records = Utils.handleBulkTokenizeBatchException(
                ex, Collections.singletonList(tokenizeRecord("v0", "g1")), 0);

        String error = records.get(0).getError();
        Assert.assertTrue("expected the DNS failure, got: " + error,
                error.contains("UnknownHostException"));
        Assert.assertTrue(error.contains("badcluster.skyvault.skyflowapis.dev"));
    }

    @Test
    public void testDelete_transportFailureReportsTheInnermostCause() {
        java.net.UnknownHostException dns = new java.net.UnknownHostException(
                "badcluster.skyvault.skyflowapis.dev: nodename nor servname provided, or not known");
        Throwable ex = new RuntimeException(
                new com.skyflow.generated.rest.core.ApiClientException(
                        "Network error executing HTTP request", dns));

        List<BulkDeleteTokensResponseRecord> records =
                Utils.handleBulkDeleteTokensBatchException(ex, deleteBatch("t0", "t1"), 0, 50);

        Assert.assertEquals(2, records.size());
        for (BulkDeleteTokensResponseRecord record : records) {
            Assert.assertTrue("expected the DNS failure, got: " + record.getError(),
                    record.getError().contains("UnknownHostException"));
            Assert.assertEquals(Integer.valueOf(500), record.getHttpCode());
        }
    }

    @Test
    public void testTokenize_transportFailureHasNoRequestId() {
        // never reached the API, so there is no call to point at
        List<BulkTokenizeResponseRecord> records = Utils.handleBulkTokenizeBatchException(
                new RuntimeException("connection reset"),
                Collections.singletonList(tokenizeRecord("v0", "g1")), 0);

        Assert.assertNull(records.get(0).getRequestId());
        Assert.assertEquals("connection reset", records.get(0).getError());
    }

    // ── delete: success carries no id, errors carry the batch's ────────────────

    private static V1FlowDeleteTokenResponse deleteWire(V1DeleteTokenResponseObject... rows) {
        return V1FlowDeleteTokenResponse.builder().tokens(Arrays.asList(rows)).build();
    }

    private static V1DeleteTokenResponseObject deleted(String token) {
        return V1DeleteTokenResponseObject.builder().value(token).httpCode(200).build();
    }

    private static V1DeleteTokenResponseObject failed(String token, String error) {
        return V1DeleteTokenResponseObject.builder().value(token).error(error).httpCode(404).build();
    }

    private static V1FlowDeleteTokenRequest deleteBatch(String... tokens) {
        return V1FlowDeleteTokenRequest.builder()
                .vaultId("vault123").tokens(Arrays.asList(tokens)).build();
    }

    @Test
    public void testDelete_requestIdOnErrorsOnly() {
        BulkDeleteTokensResponse result = Utils.formatBulkDeleteTokensResponse(
                deleteWire(deleted("t0"), failed("t1", "Token t1 is invalid.")),
                deleteBatch("t0", "t1"), 0, 50, headers(REQ_ID_A));

        Assert.assertNull("a deleted token must not carry a request id",
                result.getRecords().get(0).getRequestId());
        Assert.assertEquals(REQ_ID_A, result.getRecords().get(1).getRequestId());
    }

    @Test
    public void testDelete_everyErrorInOneBatchSharesOneRequestId() {
        BulkDeleteTokensResponse result = Utils.formatBulkDeleteTokensResponse(
                deleteWire(failed("t0", "invalid"), failed("t1", "invalid"), failed("t2", "invalid")),
                deleteBatch("t0", "t1", "t2"), 0, 50, headers(REQ_ID_A));

        Assert.assertEquals(Collections.singleton(REQ_ID_A), deleteRequestIds(result.getRecords()));
    }

    @Test
    public void testDelete_twoBatchesReportTwoDistinctRequestIds() {
        // batch 0 of size 2 covers indexes 0-1
        BulkDeleteTokensResponse first = Utils.formatBulkDeleteTokensResponse(
                deleteWire(failed("t0", "invalid"), failed("t1", "invalid")),
                deleteBatch("t0", "t1"), 0, 2, headers(REQ_ID_A));
        // batch 1 of size 2 covers indexes 2-3
        BulkDeleteTokensResponse second = Utils.formatBulkDeleteTokensResponse(
                deleteWire(failed("t2", "invalid"), failed("t3", "invalid")),
                deleteBatch("t2", "t3"), 1, 2, headers(REQ_ID_B));

        Assert.assertEquals(Collections.singleton(REQ_ID_A), deleteRequestIds(first.getRecords()));
        Assert.assertEquals(Collections.singleton(REQ_ID_B), deleteRequestIds(second.getRecords()));

        List<BulkDeleteTokensResponseRecord> merged = new ArrayList<>(first.getRecords());
        merged.addAll(second.getRecords());
        Assert.assertEquals(4, merged.size());
        Assert.assertEquals(REQ_ID_A, merged.get(0).getRequestId());
        Assert.assertEquals(REQ_ID_A, merged.get(1).getRequestId());
        Assert.assertEquals(REQ_ID_B, merged.get(2).getRequestId());
        Assert.assertEquals(REQ_ID_B, merged.get(3).getRequestId());
        Assert.assertEquals(2, merged.get(2).getIndex());
        Assert.assertEquals(3, merged.get(3).getIndex());
    }

    @Test
    public void testDelete_mixedBatchStampsOnlyTheFailures() {
        BulkDeleteTokensResponse result = Utils.formatBulkDeleteTokensResponse(
                deleteWire(deleted("t0"), failed("t1", "invalid"), deleted("t2"), failed("t3", "invalid")),
                deleteBatch("t0", "t1", "t2", "t3"), 0, 50, headers(REQ_ID_A));

        List<BulkDeleteTokensResponseRecord> records = result.getRecords();
        Assert.assertNull(records.get(0).getRequestId());
        Assert.assertEquals(REQ_ID_A, records.get(1).getRequestId());
        Assert.assertNull(records.get(2).getRequestId());
        Assert.assertEquals(REQ_ID_A, records.get(3).getRequestId());
    }

    @Test
    public void testDelete_missingHeaderLeavesRequestIdNull() {
        BulkDeleteTokensResponse result = Utils.formatBulkDeleteTokensResponse(
                deleteWire(failed("t0", "invalid")), deleteBatch("t0"), 0, 50, headers(null));

        Assert.assertNull(result.getRecords().get(0).getRequestId());
    }

    // ── delete: rejected requests ──────────────────────────────────────────────

    @Test
    public void testDelete_rejectedRequestWithPerTokenBodyStampsEveryRecord() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("value", "t0");
        row.put("error", "Token t0 is invalid.");
        row.put("httpCode", 404);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tokens", Collections.singletonList(row));

        Throwable ex = new RuntimeException(
                apiException("Error with status code 404", 404, body, REQ_ID_B));

        List<BulkDeleteTokensResponseRecord> records =
                Utils.handleBulkDeleteTokensBatchException(ex, deleteBatch("t0"), 0, 50);

        Assert.assertEquals(REQ_ID_B, records.get(0).getRequestId());
    }

    @Test
    public void testDelete_rejectedRequestWithNoBodyStampsEveryToken() {
        Throwable ex = new RuntimeException(
                apiException("boom", 503, null, REQ_ID_A));

        List<BulkDeleteTokensResponseRecord> records =
                Utils.handleBulkDeleteTokensBatchException(ex, deleteBatch("t0", "t1"), 0, 50);

        Assert.assertEquals(2, records.size());
        Assert.assertEquals(Collections.singleton(REQ_ID_A), deleteRequestIds(records));
    }

    @Test
    public void testDelete_rejectedBatchesKeepTheirOwnRequestIds() {
        Throwable exA = new RuntimeException(
                apiException("boom", 503, null, REQ_ID_A));
        Throwable exB = new RuntimeException(
                apiException("boom", 503, null, REQ_ID_B));

        List<BulkDeleteTokensResponseRecord> first =
                Utils.handleBulkDeleteTokensBatchException(exA, deleteBatch("t0", "t1"), 0, 2);
        List<BulkDeleteTokensResponseRecord> second =
                Utils.handleBulkDeleteTokensBatchException(exB, deleteBatch("t2", "t3"), 1, 2);

        Assert.assertEquals(Collections.singleton(REQ_ID_A), deleteRequestIds(first));
        Assert.assertEquals(Collections.singleton(REQ_ID_B), deleteRequestIds(second));
        Assert.assertEquals(2, second.get(0).getIndex());
        Assert.assertEquals(3, second.get(1).getIndex());
    }

    @Test
    public void testDelete_transportFailureHasNoRequestId() {
        List<BulkDeleteTokensResponseRecord> records = Utils.handleBulkDeleteTokensBatchException(
                new RuntimeException("connection reset"), deleteBatch("t0"), 0, 50);

        Assert.assertNull(records.get(0).getRequestId());
        Assert.assertEquals("connection reset", records.get(0).getError());
    }

    // ── JSON output ───────────────────────────────────────────────────────────

    @Test
    public void testRequestIdAppearsInToString() {
        BulkDeleteTokensResponse result = Utils.formatBulkDeleteTokensResponse(
                deleteWire(deleted("t0"), failed("t1", "invalid")),
                deleteBatch("t0", "t1"), 0, 50, headers(REQ_ID_A));

        String json = result.toString();
        Assert.assertTrue("errors must expose the id to callers reading the JSON",
                json.contains("\"requestId\":\"" + REQ_ID_A + "\""));
        Assert.assertTrue("successes must show it as null rather than omit it",
                json.contains("\"requestId\":null"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Every non-null request id across a tokenize result's error entries. */
    private static Set<String> tokenizeRequestIds(List<BulkTokenizeResponseRecord> records) {
        Set<String> ids = new HashSet<>();
        for (BulkTokenizeResponseRecord record : records) {
            if (record.getError() != null) {
                Assert.assertNotNull("every error must carry a request id", record.getRequestId());
                ids.add(record.getRequestId());
            }
        }
        return ids;
    }

    /** Every non-null request id across a delete result's error records. */
    private static Set<String> deleteRequestIds(List<BulkDeleteTokensResponseRecord> records) {
        Set<String> ids = new HashSet<>();
        for (BulkDeleteTokensResponseRecord record : records) {
            if (record.getError() != null) {
                Assert.assertNotNull("every error must carry a request id", record.getRequestId());
                ids.add(record.getRequestId());
            }
        }
        return ids;
    }
}
