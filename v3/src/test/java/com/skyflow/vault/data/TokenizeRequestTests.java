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

public class TokenizeRequestTests {

    private TokenizeRecord validRecord(String value, String... groupNames) {
        return TokenizeRecord.builder()
                .value(value)
                .tokenGroupNames(Arrays.asList(groupNames))
                .build();
    }

    // ─── Builder / getter tests ──────────────────────────────────────────────

    @Test
    public void testTokenizeRecordBuilderAndGetters() {
        TokenizeRecord record = TokenizeRecord.builder()
                .value("sachin")
                .tokenGroupNames(Arrays.asList("group1", "group2"))
                .build();
        Assert.assertEquals("sachin", record.getValue());
        Assert.assertEquals(Arrays.asList("group1", "group2"), record.getTokenGroupNames());
    }

    @Test
    public void testTokenizeRequestBuilderAndGetters() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(validRecord("val1", "group1"));
        TokenizeRequest req = TokenizeRequest.builder().data(records).build();
        Assert.assertEquals(records, req.getData());
    }

    // ─── Validation: null / empty request ────────────────────────────────────

    @Test
    public void testValidateTokenizeRequest_nullRequest() {
        try {
            Validations.validateTokenizeRequest(null);
            Assert.fail("Expected SkyflowException for null request");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.TokenizeRequestNull.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateTokenizeRequest_nullData() {
        TokenizeRequest req = TokenizeRequest.builder().data(null).build();
        try {
            Validations.validateTokenizeRequest(req);
            Assert.fail("Expected SkyflowException for null data");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyTokenizeData.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateTokenizeRequest_emptyData() {
        TokenizeRequest req = TokenizeRequest.builder().data(new ArrayList<>()).build();
        try {
            Validations.validateTokenizeRequest(req);
            Assert.fail("Expected SkyflowException for empty data");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyTokenizeData.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateTokenizeRequest_dataSizeExceed() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        for (int i = 0; i < 10001; i++) {
            records.add(validRecord("v" + i, "g1"));
        }
        TokenizeRequest req = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(req);
            Assert.fail("Expected SkyflowException for size exceed");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.TokenizeDataSizeExceedError.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateTokenizeRequest_maxAllowedSize() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            records.add(validRecord("v" + i, "g1"));
        }
        TokenizeRequest req = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(req);
        } catch (SkyflowException e) {
            Assert.fail("Should not throw for exactly 10000 records: " + e.getMessage());
        }
    }

    // ─── Validation: null record in list ─────────────────────────────────────

    @Test
    public void testValidateTokenizeRequest_nullRecordInList() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(validRecord("v1", "g1"));
        records.add(null);
        TokenizeRequest req = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(req);
            Assert.fail("Expected SkyflowException for null record");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.TokenizeRecordNull.getMessage(), e.getMessage());
        }
    }

    // ─── Validation: empty / null value ──────────────────────────────────────

    @Test
    public void testValidateTokenizeRequest_nullValue() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(TokenizeRecord.builder().value(null).tokenGroupNames(Collections.singletonList("g1")).build());
        TokenizeRequest req = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(req);
            Assert.fail("Expected SkyflowException for null value");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyValueInTokenizeRecord.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateTokenizeRequest_emptyValue() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(TokenizeRecord.builder().value("  ").tokenGroupNames(Collections.singletonList("g1")).build());
        TokenizeRequest req = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(req);
            Assert.fail("Expected SkyflowException for empty value");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyValueInTokenizeRecord.getMessage(), e.getMessage());
        }
    }

    // ─── Validation: empty / null tokenGroupNames ─────────────────────────────

    @Test
    public void testValidateTokenizeRequest_nullTokenGroupNames() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(TokenizeRecord.builder().value("val").tokenGroupNames(null).build());
        TokenizeRequest req = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(req);
            Assert.fail("Expected SkyflowException for null tokenGroupNames");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyTokenGroupNamesInTokenizeRecord.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateTokenizeRequest_emptyTokenGroupNames() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(TokenizeRecord.builder().value("val").tokenGroupNames(new ArrayList<>()).build());
        TokenizeRequest req = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(req);
            Assert.fail("Expected SkyflowException for empty tokenGroupNames");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyTokenGroupNamesInTokenizeRecord.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateTokenizeRequest_nullTokenGroupNameInList() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(TokenizeRecord.builder().value("val").tokenGroupNames(Arrays.asList("g1", null)).build());
        TokenizeRequest req = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(req);
            Assert.fail("Expected SkyflowException for null group name in list");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyTokenGroupNameInTokenizeRecord.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateTokenizeRequest_emptyTokenGroupNameInList() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(TokenizeRecord.builder().value("val").tokenGroupNames(Arrays.asList("g1", "  ")).build());
        TokenizeRequest req = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(req);
            Assert.fail("Expected SkyflowException for empty group name in list");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyTokenGroupNameInTokenizeRecord.getMessage(), e.getMessage());
        }
    }

    // ─── Validation: valid request ────────────────────────────────────────────

    @Test
    public void testValidateTokenizeRequest_valid() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(validRecord("sachin", "non_deterministic"));
        records.add(validRecord("dhoni", "deterministic_string_tg", "group2"));
        TokenizeRequest req = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(req);
        } catch (SkyflowException e) {
            Assert.fail("Should not throw for valid request: " + e.getMessage());
        }
    }

    @Test
    public void testValidateTokenizeRequest_singleRecord() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(validRecord("value1", "group1"));
        TokenizeRequest req = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(req);
        } catch (SkyflowException e) {
            Assert.fail("Should not throw for single valid record: " + e.getMessage());
        }
    }
}
