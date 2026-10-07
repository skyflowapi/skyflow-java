package com.skyflow.vault.data;

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

        Assert.assertEquals(fields, response.getFields());
        Assert.assertNull(response.getErrors());
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

        Assert.assertTrue(json, json.contains("\"fields\":[{\"name\":\"john\"}]"));
        Assert.assertTrue(json, json.contains("\"errors\":null"));
        Assert.assertTrue(json, json.contains("\"metadata\":{\"columns\":[\"name\"]}"));
        Assert.assertTrue(json, json.contains("\"requestId\":\"req-1\""));
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

    // ── GetTokensResponse ────────────────────────────────────────────────────

    @Test
    public void testGetTokensResponse_getterReturnsConstructorValue() {
        HashMap<String, Object> record = new HashMap<>();
        record.put("token", "tok-1");
        ArrayList<HashMap<String, Object>> records = new ArrayList<>(Collections.singletonList(record));

        GetTokensResponse response = new GetTokensResponse(records);

        Assert.assertEquals(records, response.getRecords());
        Assert.assertTrue(response instanceof BaseGetTokensResponse);
    }

    @Test
    public void testGetTokensResponse_toStringSerializesNulls() {
        HashMap<String, Object> record = new HashMap<>();
        record.put("token", "tok-1");
        record.put("error", null);
        record.put("requestId", null);
        GetTokensResponse response = new GetTokensResponse(new ArrayList<>(Collections.singletonList(record)));

        String json = response.toString();

        Assert.assertTrue(json, json.contains("\"token\":\"tok-1\""));
        Assert.assertTrue(json, json.contains("\"error\":null"));
        Assert.assertTrue(json, json.contains("\"requestId\":null"));
    }
}
