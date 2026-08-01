package com.skyflow.vault.data;

import com.skyflow.enums.UpsertType;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for the simple request/response wrapper classes: {@link DeleteTokensRequest},
 * {@link InsertRequest}, {@link InsertResponse}, {@link TokenizeRequest}, {@link TokenizeResponse},
 * {@link DetokenizeRequest}, {@link DetokenizeResponse}, {@link DetokenizeRecordResponse},
 * {@link DetokenizeData}, {@link BulkDeleteTokensRequest}, {@link BulkDetokenizeRequest},
 * {@link BulkTokenizeRequest}, {@link BulkInsertRequest} and {@link DeleteTokensResponse}.
 */
public class RequestResponseWrapperTests {

    // ── DeleteTokensRequest ──────────────────────────────────────────────────

    @Test
    public void testDeleteTokensRequest_getterReturnsBuilderValue() {
        List<String> tokens = Arrays.asList("tok-1", "tok-2");
        DeleteTokensRequest request = DeleteTokensRequest.builder().tokens(tokens).build();
        Assert.assertEquals(tokens, request.getTokens());
    }

    @Test
    public void testDeleteTokensRequest_defaultIsNull() {
        DeleteTokensRequest request = DeleteTokensRequest.builder().build();
        Assert.assertNull(request.getTokens());
    }

    // ── InsertRequest ────────────────────────────────────────────────────────

    @Test
    public void testInsertRequest_gettersReturnBuilderValues() {
        ArrayList<InsertRecord> records = new ArrayList<>(Collections.singletonList(
                InsertRecord.builder().table("persons").build()));
        List<String> upsert = Arrays.asList("id");

        InsertRequest request = InsertRequest.builder()
                .table("persons")
                .upsert(upsert)
                .upsertType(UpsertType.UPDATE)
                .records(records)
                .build();

        Assert.assertEquals("persons", request.getTable());
        Assert.assertEquals(upsert, request.getUpsert());
        Assert.assertEquals(UpsertType.UPDATE, request.getUpsertType());
        Assert.assertEquals(records, request.getRecords());
    }

    @Test
    public void testInsertRequest_defaultsAreNull() {
        InsertRequest request = InsertRequest.builder().build();
        Assert.assertNull(request.getTable());
        Assert.assertNull(request.getUpsert());
        Assert.assertNull(request.getUpsertType());
        Assert.assertNull(request.getRecords());
    }

    // ── InsertResponse ───────────────────────────────────────────────────────

    @Test
    public void testInsertResponse_gettersReturnConstructorValues() {
        ArrayList<HashMap<String, Object>> insertedFields = new ArrayList<>();
        HashMap<String, Object> field = new HashMap<>();
        field.put("skyflow_id", "id-1");
        insertedFields.add(field);
        ArrayList<HashMap<String, Object>> errors = new ArrayList<>();

        InsertResponse response = new InsertResponse(insertedFields, errors);

        Assert.assertEquals(insertedFields, response.getInsertedFields());
        Assert.assertEquals(errors, response.getErrors());
    }

    // ── TokenizeRequest ──────────────────────────────────────────────────────

    @Test
    public void testTokenizeRequest_getterReturnsBuilderValue() {
        ArrayList<TokenizeRecord> data = new ArrayList<>(Collections.singletonList(
                TokenizeRecord.builder().value("v1").build()));
        TokenizeRequest request = TokenizeRequest.builder().data(data).build();
        Assert.assertEquals(data, request.getData());
    }

    @Test
    public void testTokenizeRequest_defaultIsNull() {
        TokenizeRequest request = TokenizeRequest.builder().build();
        Assert.assertNull(request.getData());
    }

    // ── TokenizeResponse ─────────────────────────────────────────────────────

    @Test
    public void testTokenizeResponse_defaultConstructorInitializesEmptyErrors() {
        TokenizeResponse response = new TokenizeResponse();
        Assert.assertNotNull(response.getErrors());
        Assert.assertTrue(response.getErrors().isEmpty());
        Assert.assertNull(response.getTokenizedData());
    }

    @Test
    public void testTokenizeResponse_errorsConstructorAndTokenizedDataSetter() {
        ArrayList<HashMap<String, Object>> errors = new ArrayList<>();
        HashMap<String, Object> error = new HashMap<>();
        error.put("error", "failed");
        errors.add(error);

        TokenizeResponse response = new TokenizeResponse(errors);
        Assert.assertEquals(errors, response.getErrors());

        List<TokenizeData> tokenizedData = Collections.singletonList(new TokenizeData("v1", 0));
        response.setTokenizedData(tokenizedData);
        Assert.assertEquals(tokenizedData, response.getTokenizedData());
    }

    // ── DetokenizeRequest ────────────────────────────────────────────────────

    @Test
    public void testDetokenizeRequest_gettersReturnBuilderValues() {
        ArrayList<DetokenizeData> detokenizeData = new ArrayList<>(Collections.singletonList(
                new DetokenizeData("tok-1")));
        List<TokenGroupRedactions> redactions = Collections.singletonList(
                TokenGroupRedactions.builder().tokenGroupName("group1").redaction("MASK").build());

        DetokenizeRequest request = DetokenizeRequest.builder()
                .detokenizeData(detokenizeData)
                .tokenGroupRedactions(redactions)
                .build();

        Assert.assertEquals(detokenizeData, request.getDetokenizeData());
        Assert.assertEquals(redactions, request.getTokenGroupRedactions());
    }

    @Test
    public void testDetokenizeRequest_defaultsAreNull() {
        DetokenizeRequest request = DetokenizeRequest.builder().build();
        Assert.assertNull(request.getDetokenizeData());
        Assert.assertNull(request.getTokenGroupRedactions());
    }

    // ── DetokenizeResponse ───────────────────────────────────────────────────

    @Test
    public void testDetokenizeResponse_gettersReturnConstructorValues() {
        ArrayList<DetokenizeRecordResponse> detokenizedFields = new ArrayList<>(Collections.singletonList(
                new DetokenizeRecordResponse("tok-1", "value", null, "group1", null)));
        ArrayList<DetokenizeRecordResponse> errors = new ArrayList<>();

        DetokenizeResponse response = new DetokenizeResponse(detokenizedFields, errors);

        Assert.assertEquals(detokenizedFields, response.getDetokenizedFields());
        Assert.assertEquals(errors, response.getErrors());
        Assert.assertNotNull(response.toString());
    }

    // ── DetokenizeRecordResponse ─────────────────────────────────────────────

    @Test
    public void testDetokenizeRecordResponse_gettersReturnConstructorValues() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key", "value");

        DetokenizeRecordResponse response = new DetokenizeRecordResponse(
                "tok-1", "plain-value", null, "group1", metadata);

        Assert.assertEquals("tok-1", response.getToken());
        Assert.assertEquals("plain-value", response.getValue());
        Assert.assertNull(response.getError());
        Assert.assertEquals("group1", response.getTokenGroupName());
        Assert.assertEquals(metadata, response.getMetadata());
        Assert.assertNotNull(response.toString());
    }

    @Test
    public void testDetokenizeRecordResponse_errorCase() {
        DetokenizeRecordResponse response = new DetokenizeRecordResponse(
                "tok-2", null, "Token not found", null, null);

        Assert.assertEquals("tok-2", response.getToken());
        Assert.assertEquals("Token not found", response.getError());
        Assert.assertNull(response.getValue());
        Assert.assertNull(response.getTokenGroupName());
        Assert.assertNull(response.getMetadata());
    }

    // ── DetokenizeData ───────────────────────────────────────────────────────

    @Test
    public void testDetokenizeData_getterReturnsConstructorValue() {
        DetokenizeData data = new DetokenizeData("tok-1");
        Assert.assertEquals("tok-1", data.getToken());
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
        List<BulkTokenGroupRedactions> redactions = Collections.singletonList(
                BulkTokenGroupRedactions.builder().tokenGroupName("group1").redaction("MASK").build());

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

    // ── BulkTokenizeRequest ──────────────────────────────────────────────────

    @Test
    public void testBulkTokenizeRequest_getterReturnsBuilderValue() {
        ArrayList<BulkTokenizeRecord> data = new ArrayList<>(Collections.singletonList(
                BulkTokenizeRecord.builder().value("v1").build()));
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().data(data).build();
        Assert.assertEquals(data, request.getData());
    }

    @Test
    public void testBulkTokenizeRequest_defaultIsNull() {
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().build();
        Assert.assertNull(request.getData());
    }

    // ── BulkInsertRequest ────────────────────────────────────────────────────

    @Test
    public void testBulkInsertRequest_gettersReturnBuilderValues() {
        ArrayList<BulkInsertRecord> records = new ArrayList<>(Collections.singletonList(
                BulkInsertRecord.builder().table("persons").build()));
        List<String> upsert = Arrays.asList("id");

        BulkInsertRequest request = BulkInsertRequest.builder()
                .table("persons")
                .upsert(upsert)
                .upsertType(UpsertType.REPLACE)
                .records(records)
                .build();

        Assert.assertEquals("persons", request.getTable());
        Assert.assertEquals(upsert, request.getUpsert());
        Assert.assertEquals(UpsertType.REPLACE, request.getUpsertType());
        Assert.assertEquals(records, request.getRecords());
    }

    @Test
    public void testBulkInsertRequest_defaultsAreNull() {
        BulkInsertRequest request = BulkInsertRequest.builder().build();
        Assert.assertNull(request.getTable());
        Assert.assertNull(request.getUpsert());
        Assert.assertNull(request.getUpsertType());
        Assert.assertNull(request.getRecords());
    }

    // ── DeleteTokensResponse ─────────────────────────────────────────────────

    @Test
    public void testDeleteTokensResponse_gettersReturnConstructorValues() {
        List<String> tokens = Arrays.asList("tok-1", "tok-2");
        ArrayList<HashMap<String, Object>> errors = new ArrayList<>();
        HashMap<String, Object> error = new HashMap<>();
        error.put("error", "failed");
        errors.add(error);

        DeleteTokensResponse response = new DeleteTokensResponse(tokens, errors);

        Assert.assertEquals(tokens, response.getTokens());
        Assert.assertEquals(errors, response.getErrors());
    }
}
