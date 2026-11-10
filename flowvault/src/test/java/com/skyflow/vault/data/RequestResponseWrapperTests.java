package com.skyflow.vault.data;

import com.skyflow.enums.UploadStatus;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.validations.Validations;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for the simple request/response wrapper classes: {@link InsertRequest},
 * {@link InsertResponseRecord}, {@link DetokenizeRequest}, {@link DetokenizeResponseRecord},
 * {@link BulkDeleteTokensRequest}, {@link BulkDetokenizeRequest}, {@link BulkTokenizeRequest},
 * {@link BulkInsertRequest}, {@link QueryRequest}, {@link QueryResponse}, {@link GetTokensRequest}
 * and {@link GetTokensResponse}.
 */
public class RequestResponseWrapperTests {

    // Tests for DeleteTokensRequest were removed: the class no longer exists (bulk-only module).

    // ── InsertRequest ────────────────────────────────────────────────────────

    @Test
    public void testInsertRequest_gettersReturnBuilderValues() {
        ArrayList<InsertRequestRecord> records = new ArrayList<>(Collections.singletonList(
                InsertRequestRecord.builder().tableName("persons").build()));
        UpsertOptions upsert = UpsertOptions.builder()
                .uniqueColumns(Arrays.asList("id"))
                .updateType("UPDATE")
                .build();

        InsertRequest request = InsertRequest.builder()
                .upsert(upsert)
                .records(records)
                .build();

        Assert.assertEquals(upsert, request.getUpsert());
        Assert.assertEquals(records, request.getRecords());
    }

    @Test
    public void testInsertRequest_tableNameGetterReturnsBuilderValue() {
        InsertRequest request = InsertRequest.builder().tableName("persons").build();
        Assert.assertEquals("persons", request.getTableName());
    }

    @Test
    public void testInsertRequest_defaultsAreNull() {
        InsertRequest request = InsertRequest.builder().build();
        Assert.assertNull(request.getUpsert());
        Assert.assertNull(request.getRecords());
        Assert.assertNull(request.getTableName());
    }

    // ── InsertRequestRecord ─────────────────────────────────────────────────────────

    @Test
    public void testInsertRequestRecord_gettersReturnBuilderValues() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "tok-abc");
        UpsertOptions upsert = UpsertOptions.builder().uniqueColumns(Arrays.asList("id")).build();

        InsertRequestRecord record = InsertRequestRecord.builder()
                .tableName("persons")
                .data(data)
                .tokens(tokens)
                .upsert(upsert)
                .build();

        Assert.assertEquals("persons", record.getTableName());
        Assert.assertEquals(data, record.getData());
        Assert.assertEquals(tokens, record.getTokens());
        Assert.assertEquals(upsert, record.getUpsert());
    }

    // ── UpsertOptions ────────────────────────────────────────────────────────

    @Test
    public void testTokenizeRequest_getterReturnsBuilderValue() {
        List<TokenizeRequestRecord> records = Collections.singletonList(
                TokenizeRequestRecord.builder().value("v1").build());
        TokenizeRequest request = TokenizeRequest.builder().records(records).build();
        Assert.assertEquals(records, request.getRecords());
    }

    @Test
    public void testBulkTokenizeRequest_isATokenizeRequest() {
        BulkTokenizeRequest request = BulkTokenizeRequest.builder()
                .records(Collections.singletonList(
                        BulkTokenizeRequestRecord.builder().value("v1").build()))
                .build();
        Assert.assertTrue(request instanceof TokenizeRequest);
        // the inherited accessor sees the same records, widened
        Assert.assertEquals(1, ((TokenizeRequest) request).getRecords().size());
    }

    @Test
    public void testBulkTokenizeRequest_plainTokenizeRequestRecordBuildsWithoutThrowing() {
        // The inherited setter's generic signature accepts a plain List<TokenizeRequestRecord>.
        // No builder in this SDK throws from build() — the type mismatch is deferred to
        // Validations (see below), consistent with every other request validation.
        List<TokenizeRequestRecord> plainRecords = Collections.singletonList(
                TokenizeRequestRecord.builder().value("v1").build());
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().records(plainRecords).build();
        Assert.assertNotNull(request);
    }

    @Test
    public void testBulkTokenizeRequest_plainTokenizeRequestRecordThrowsSkyflowExceptionOnValidate() {
        // A caller-side type mistake (plain TokenizeRequestRecord instead of
        // BulkTokenizeRequestRecord) must surface as a clean SkyflowException at validation
        // time — never a raw ClassCastException or IllegalArgumentException.
        List<TokenizeRequestRecord> plainRecords = Collections.singletonList(
                TokenizeRequestRecord.builder().value("v1").build());
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().records(plainRecords).build();
        try {
            Validations.validateBulkTokenizeRequest(request);
            Assert.fail("Should have thrown an exception");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.InvalidBulkTokenizeRecordType.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testTokenizeRequest_defaultIsNull() {
        TokenizeRequest request = TokenizeRequest.builder().build();
        Assert.assertNull(request.getRecords());
    }

    // ── TokenizeResponse ─────────────────────────────────────────────────────

    @Test
    public void testTokenizeResponse_gettersReturnConstructorValues() {
        List<TokenizeResponseRecord> records = Collections.singletonList(
                new TokenizeResponseRecord("value1", "group1", "tok-abc", 200, null));

        TokenizeResponse response = new TokenizeResponse(records);

        Assert.assertEquals(records, response.getResponse());
        Assert.assertEquals("value1", response.getResponse().get(0).getValue());
        Assert.assertEquals("tok-abc", response.getResponse().get(0).getToken());
        Assert.assertNull(response.getResponse().get(0).getError());
    }

    @Test
    public void testTokenizeResponse_toStringSerializesNulls() {
        TokenizeResponse response = new TokenizeResponse(Collections.singletonList(
                new TokenizeResponseRecord("value1", "group1", "tok-abc", 200, null)));
        Assert.assertTrue(response.toString().contains("\"error\":null"));
    }

    // ── DetokenizeRequest ────────────────────────────────────────────────────

    @Test
    public void testDetokenizeRequest_gettersReturnBuilderValues() {
        List<String> tokens = Collections.singletonList("tok-1");
        List<TokenGroupRedactions> redactions = Collections.singletonList(
                TokenGroupRedactions.builder().tokenGroupName("group1").redaction("MASK").build());

        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(tokens)
                .tokenGroupRedactions(redactions)
                .build();

        Assert.assertEquals(tokens, request.getTokens());
        Assert.assertEquals(redactions, request.getTokenGroupRedactions());
    }

    @Test
    public void testDetokenizeRequest_defaultsAreNull() {
        DetokenizeRequest request = DetokenizeRequest.builder().build();
        Assert.assertNull(request.getTokens());
        Assert.assertNull(request.getTokenGroupRedactions());
    }

    // Tests for DetokenizeResponse were removed: the class no longer exists (bulk-only module).

    // ── DetokenizeResponseRecord ─────────────────────────────────────────────

    @Test
    public void testDetokenizeResponseRecord_gettersReturnConstructorValues() {
        DetokenizeResponseRecordMetadata metadata = new DetokenizeResponseRecordMetadata("skyflow-id-1", "table1");

        DetokenizeResponseRecord response = new DetokenizeResponseRecord(
                "tok-1", "secret-value", "group1", metadata, 200, null);

        Assert.assertEquals("tok-1", response.getToken());
        Assert.assertNull(response.getError());
        Assert.assertEquals("group1", response.getTokenGroupName());
        Assert.assertEquals(metadata, response.getMetadata());
        Assert.assertEquals(200, response.getHttpCode());
    }

    @Test
    public void testDetokenizeResponseRecord_errorCase() {
        DetokenizeResponseRecord response = new DetokenizeResponseRecord(
                "tok-2", null, null, null, 404, "Token not found");

        Assert.assertEquals("tok-2", response.getToken());
        Assert.assertEquals("Token not found", response.getError());
        Assert.assertNull(response.getTokenGroupName());
        Assert.assertNull(response.getMetadata());
        Assert.assertEquals(404, response.getHttpCode());
    }

    // ── BulkDeleteTokensRequest ──────────────────────────────────────────────

    @Test
    public void testBulkDeleteTokensRequest_getterReturnsBuilderValue() {
        List<String> tokens = Arrays.asList("tok-1", "tok-2");
        BulkDeleteTokensRequest request = BulkDeleteTokensRequest.builder().tokens(tokens).build();
        Assert.assertEquals(tokens, request.getTokens());
    }

    @Test
    public void testBulkDeleteTokensRequest_defaultIsNull() {
        BulkDeleteTokensRequest request = BulkDeleteTokensRequest.builder().build();
        Assert.assertNull(request.getTokens());
    }

    // ── BulkDetokenizeRequest ────────────────────────────────────────────────

    @Test
    public void testBulkDetokenizeRequest_gettersReturnBuilderValues() {
        List<String> tokens = Arrays.asList("tok-1", "tok-2");
        List<TokenGroupRedactions> redactions = Collections.singletonList(
                TokenGroupRedactions.builder().tokenGroupName("group1").redaction("MASK").build());

        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder()
                .tokens(tokens)
                .tokenGroupRedactions(redactions)
                .build();

        Assert.assertEquals(tokens, request.getTokens());
        Assert.assertEquals(redactions, request.getTokenGroupRedactions());
    }

    @Test
    public void testBulkDetokenizeRequest_defaultsAreNull() {
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder().build();
        Assert.assertNull(request.getTokens());
        Assert.assertNull(request.getTokenGroupRedactions());
    }

    @Test
    public void testBulkDetokenizeRequest_isADetokenizeRequest() {
        // Bulk detokenize now shares the unary request contract; all state is inherited.
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder()
                .tokens(Collections.singletonList("tok-1"))
                .build();
        DetokenizeRequest asUnary = request;
        Assert.assertEquals(Collections.singletonList("tok-1"), asUnary.getTokens());
    }

    // ── BulkTokenizeRequest ──────────────────────────────────────────────────

    @Test
    public void testBulkTokenizeRequest_getterReturnsBuilderValue() {
        List<BulkTokenizeRequestRecord> records = Collections.singletonList(
                BulkTokenizeRequestRecord.builder().value("v1").build());
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().records(records).build();
        Assert.assertEquals(records, request.getRecords());
    }

    @Test
    public void testBulkTokenizeRequest_defaultIsNull() {
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().build();
        Assert.assertNull(request.getRecords());
    }

    // ── BulkInsertRequest ────────────────────────────────────────────────────

    @Test
    public void testBulkInsertRequest_gettersReturnBuilderValues() {
        List<InsertRequestRecord> records = new ArrayList<>(Collections.singletonList(
                BulkInsertRequestRecord.builder().tableName("persons").build()));
        UpsertOptions upsert = UpsertOptions.builder()
                .updateType("REPLACE")
                .uniqueColumns(Arrays.asList("id"))
                .build();

        BulkInsertRequest request = BulkInsertRequest.builder()
                .tableName("persons")
                .upsert(upsert)
                .records(records)
                .build();

        Assert.assertEquals("persons", request.getTableName());
        Assert.assertEquals(upsert, request.getUpsert());
        Assert.assertEquals("REPLACE", request.getUpsert().getUpdateType());
        Assert.assertEquals(Arrays.asList("id"), request.getUpsert().getUniqueColumns());
        Assert.assertEquals(records, request.getRecords());
    }

    @Test
    public void testBulkInsertRequest_defaultsAreNull() {
        BulkInsertRequest request = BulkInsertRequest.builder().build();
        Assert.assertNull(request.getTableName());
        Assert.assertNull(request.getUpsert());
        Assert.assertNull(request.getRecords());
    }

    @Test
    public void testDeleteTokensResponse_gettersReturnConstructorValues() {
        List<DeleteTokensRecord> records = Arrays.asList(
                new DeleteTokensRecord("tok-1", 200, null),
                new DeleteTokensRecord("tok-2", 404, "Token not found"));

        DeleteTokensResponse response = new DeleteTokensResponse(records);

        Assert.assertEquals(records, response.getRecords());
        Assert.assertEquals("tok-1", response.getRecords().get(0).getToken());
        Assert.assertNull(response.getRecords().get(0).getError());
        Assert.assertEquals("Token not found", response.getRecords().get(1).getError());
        Assert.assertEquals(Integer.valueOf(404), response.getRecords().get(1).getHttpCode());
    }

    // Tests for DeleteTokensResponse were removed: the class no longer exists (bulk-only module).

    // ── QueryRequest ─────────────────────────────────────────────────────────

    @Test
    public void testQueryRequest_getterReturnsBuilderValue() {
        QueryRequest request = QueryRequest.builder().query("SELECT * FROM persons").build();
        Assert.assertEquals("SELECT * FROM persons", request.getQuery());
    }

    @Test
    public void testQueryRequest_defaultsAreNull() {
        QueryRequest request = QueryRequest.builder().build();
        Assert.assertNull(request.getQuery());
    }

    @Test
    public void testQueryRequest_isABaseQueryRequest() {
        QueryRequest request = QueryRequest.builder().query("SELECT 1").build();
        Assert.assertTrue(request instanceof BaseQueryRequest);
    }

    // ── QueryResponse ────────────────────────────────────────────────────────

    @Test
    public void testQueryResponse_gettersReturnConstructorValues() {
        HashMap<String, Object> row = new HashMap<>();
        row.put("name", "john");
        ArrayList<HashMap<String, Object>> fields = new ArrayList<>(Collections.singletonList(row));
        QueryResponseMetadata metadata = new QueryResponseMetadata(Collections.singletonList("name"));

        QueryResponse response = new QueryResponse(fields, metadata, "req-1");

        Assert.assertEquals(fields, response.getRecords());
        Assert.assertSame(metadata, response.getMetadata());
        Assert.assertEquals("req-1", response.getRequestId());
        Assert.assertTrue(response instanceof BaseQueryResponse);
    }

    @Test
    public void testQueryResponse_toStringSerializesAllFieldsIncludingNulls() {
        HashMap<String, Object> row = new HashMap<>();
        row.put("name", "john");
        QueryResponse response = new QueryResponse(new ArrayList<>(Collections.singletonList(row)),
                new QueryResponseMetadata(Collections.singletonList("name")), "req-1");

        String json = response.toString();

        Assert.assertEquals("{\"records\":[{\"name\":\"john\"}],\"metadata\":{\"columns\":[\"name\"]},"
                + "\"requestId\":\"req-1\"}", json);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testQueryResponse_recordsAreReadOnlyAndDeprecatedGettersStillWork() {
        HashMap<String, Object> row = new HashMap<>();
        row.put("name", "john");
        ArrayList<HashMap<String, Object>> fields = new ArrayList<>(Collections.singletonList(row));
        QueryResponse response = new QueryResponse(fields, new QueryResponseMetadata(null), null);

        try {
            response.getRecords().clear();
            Assert.fail("getRecords() should be read-only");
        } catch (UnsupportedOperationException expected) {
            Assert.assertEquals(1, response.getRecords().size());
        }
        // kept for compatibility until removal
        Assert.assertSame(fields, response.getFields());
        Assert.assertNull(response.getErrors());
        // a null row list reads as empty, never null
        Assert.assertTrue(new QueryResponse(null, null, null).getRecords().isEmpty());
    }

    // ── GetTokensRequest ─────────────────────────────────────────────────────

    @Test
    public void testGetTokensRequest_getterReturnsBuilderValue() {
        List<GetTokensRequestRecord> records = Collections.singletonList(
                GetTokensRequestRecord.builder().value("john@example.com").tokenGroupName("det_group").build());

        GetTokensRequest request = GetTokensRequest.builder().records(records).build();

        Assert.assertEquals(records, request.getRecords());
        Assert.assertTrue(request instanceof BaseGetTokensRequest);
    }

    @Test
    public void testGetTokensRequest_defaultsAreNull() {
        GetTokensRequest request = GetTokensRequest.builder().build();
        Assert.assertNull(request.getRecords());
    }

    // ── GetTokensResponse / UploadFilesResponse / DeleteFilesResponse ──────────

    @Test
    public void testGetTokensResponse_getterReturnsConstructorValue() {
        GetTokensResponseRecord record = new GetTokensResponseRecord("v", "g", "tok-1", 200, null, null);
        List<GetTokensResponseRecord> records = Collections.singletonList(record);

        GetTokensResponse response = new GetTokensResponse(records);

        Assert.assertSame(records, response.getRecords());
        Assert.assertTrue(response instanceof BaseGetTokensResponse);
        Assert.assertEquals("v", record.getValue());
        Assert.assertEquals("g", record.getTokenGroupName());
        Assert.assertEquals("tok-1", record.getToken());
        Assert.assertEquals(200, record.getHttpCode());
        Assert.assertNull(record.getError());
        Assert.assertNull(record.getRequestId());
    }

    @Test
    public void testGetTokensResponse_toStringSerializesNulls() {
        GetTokensResponse response = new GetTokensResponse(Collections.singletonList(
                new GetTokensResponseRecord("v", "g", "tok-1", 200, null, null)));

        String json = response.toString();

        Assert.assertTrue(json, json.contains("\"token\":\"tok-1\""));
        Assert.assertTrue(json, json.contains("\"error\":null"));
        Assert.assertTrue(json, json.contains("\"requestId\":null"));
        Assert.assertTrue(json, new GetTokensResponseRecord(null, null, "", 404, "boom", "req-1").toString()
                .contains("\"requestId\":\"req-1\""));
    }

    @Test
    public void testUploadFilesResponse_typedRecordsAndStatusEnum() {
        UploadFilesColumnResult column = new UploadFilesColumnResult("photo", "a.png", UploadStatus.FAILED, "PUT failed: 403");
        UploadFilesResponseRecord record = new UploadFilesResponseRecord(
                "sky-1", "documents", Collections.singletonList(column), 200, null, null);

        UploadFilesResponse response = new UploadFilesResponse(Collections.singletonList(record));

        Assert.assertTrue(response instanceof BaseUploadFilesResponse);
        Assert.assertSame(record, response.getRecords().get(0));
        Assert.assertEquals("sky-1", record.getSkyflowId());
        Assert.assertEquals("documents", record.getTableName());
        Assert.assertEquals(200, record.getHttpCode());
        Assert.assertNull(record.getError());
        Assert.assertNull(record.getRequestId());
        Assert.assertSame(column, record.getColumns().get(0));
        Assert.assertEquals("photo", column.getColumn());
        Assert.assertEquals("a.png", column.getFileName());
        Assert.assertEquals(UploadStatus.FAILED, column.getUploadStatus());
        Assert.assertEquals("PUT failed: 403", column.getError());
        String json = response.toString();
        Assert.assertTrue(json, json.contains("\"uploadStatus\":\"FAILED\""));
        Assert.assertTrue(json, record.toString().contains("\"requestId\":null"));
        Assert.assertTrue(json, column.toString().contains("\"fileName\":\"a.png\""));
    }

    @Test
    public void testDeleteFilesResponse_typedRecords() {
        DeleteFilesColumnResult column = new DeleteFilesColumnResult("photo", "DELETED");
        DeleteFilesResponseRecord record = new DeleteFilesResponseRecord(
                "sky-1", "documents", Collections.singletonList(column), 200, null, null);
        DeleteFilesResponseRecord failed = new DeleteFilesResponseRecord(null, "documents", null, 404, "Not found", "req-1");

        DeleteFilesResponse response = new DeleteFilesResponse(Arrays.asList(record, failed));

        Assert.assertTrue(response instanceof BaseDeleteFilesResponse);
        Assert.assertEquals("sky-1", record.getSkyflowId());
        Assert.assertEquals("documents", record.getTableName());
        Assert.assertEquals(200, record.getHttpCode());
        Assert.assertEquals("photo", column.getColumn());
        Assert.assertEquals(com.skyflow.enums.DeleteFilesStatus.DELETED, column.getStatus());
        Assert.assertNull(failed.getColumns());
        Assert.assertEquals("Not found", failed.getError());
        Assert.assertEquals("req-1", failed.getRequestId());
        String json = response.toString();
        Assert.assertTrue(json, json.contains("\"columns\":null"));
        Assert.assertTrue(json, json.contains("\"status\":\"DELETED\""));
        Assert.assertTrue(json, failed.toString().contains("\"error\":\"Not found\""));
        Assert.assertTrue(json, column.toString().contains("\"column\":\"photo\""));
    }

    @Test
    public void testDeleteFilesColumnResult_unrecognizedStatusIsUnknownButKeepsTheRawValue() {
        com.skyflow.vault.data.DeleteFilesColumnResult lower =
                new com.skyflow.vault.data.DeleteFilesColumnResult("kyc", "deleted");
        Assert.assertEquals(com.skyflow.enums.DeleteFilesStatus.DELETED, lower.getStatus());

        com.skyflow.vault.data.DeleteFilesColumnResult other =
                new com.skyflow.vault.data.DeleteFilesColumnResult("kyc", "NOT_FOUND");
        Assert.assertEquals(com.skyflow.enums.DeleteFilesStatus.UNKNOWN, other.getStatus());
        Assert.assertEquals("NOT_FOUND", other.getRawStatus());

        com.skyflow.vault.data.DeleteFilesColumnResult missing =
                new com.skyflow.vault.data.DeleteFilesColumnResult("kyc", null);
        Assert.assertEquals(com.skyflow.enums.DeleteFilesStatus.UNKNOWN, missing.getStatus());
        Assert.assertNull(missing.getRawStatus());
    }
}
