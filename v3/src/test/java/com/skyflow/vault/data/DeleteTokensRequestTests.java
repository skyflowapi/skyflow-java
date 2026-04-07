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
import java.util.List;

public class DeleteTokensRequestTests {

    @Test
    public void testDeleteTokensRequestBuilderAndGetters() {
        List<String> tokens = Arrays.asList("token1", "token2");
        DeleteTokensRequest request = DeleteTokensRequest.builder()
                .tokens(tokens)
                .build();
        Assert.assertEquals(tokens, request.getTokens());
    }

    @Test
    public void testDeleteTokensRequestNullTokens() {
        DeleteTokensRequest request = DeleteTokensRequest.builder()
                .tokens(null)
                .build();
        Assert.assertNull(request.getTokens());
    }

    @Test
    public void testValidateDeleteTokensRequestValid() {
        try {
            List<String> tokens = Arrays.asList("token1", "token2");
            DeleteTokensRequest request = DeleteTokensRequest.builder()
                    .tokens(tokens)
                    .build();
            Validations.validateDeleteTokensRequest(request);
        } catch (SkyflowException e) {
            Assert.fail("Should not have thrown exception for valid request");
        }
    }

    @Test
    public void testValidateDeleteTokensRequestNull() {
        try {
            Validations.validateDeleteTokensRequest(null);
            Assert.fail("Should have thrown exception for null request");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.DeleteTokensRequestNull.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateDeleteTokensRequestNullTokensList() {
        try {
            DeleteTokensRequest request = DeleteTokensRequest.builder()
                    .tokens(null)
                    .build();
            Validations.validateDeleteTokensRequest(request);
            Assert.fail("Should have thrown exception for null tokens");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyDeleteTokensData.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateDeleteTokensRequestEmptyTokensList() {
        try {
            DeleteTokensRequest request = DeleteTokensRequest.builder()
                    .tokens(new ArrayList<>())
                    .build();
            Validations.validateDeleteTokensRequest(request);
            Assert.fail("Should have thrown exception for empty tokens list");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyDeleteTokensData.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateDeleteTokensRequestNullTokenInList() {
        try {
            List<String> tokens = Arrays.asList("token1", null, "token3");
            DeleteTokensRequest request = DeleteTokensRequest.builder()
                    .tokens(tokens)
                    .build();
            Validations.validateDeleteTokensRequest(request);
            Assert.fail("Should have thrown exception for null token in list");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyTokenInDeleteTokensData.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateDeleteTokensRequestEmptyTokenInList() {
        try {
            List<String> tokens = Arrays.asList("token1", "  ", "token3");
            DeleteTokensRequest request = DeleteTokensRequest.builder()
                    .tokens(tokens)
                    .build();
            Validations.validateDeleteTokensRequest(request);
            Assert.fail("Should have thrown exception for empty token in list");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyTokenInDeleteTokensData.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateDeleteTokensRequestTokensSizeExceed() {
        try {
            List<String> tokens = new ArrayList<>();
            for (int i = 0; i < 10001; i++) {
                tokens.add("token" + i);
            }
            DeleteTokensRequest request = DeleteTokensRequest.builder()
                    .tokens(tokens)
                    .build();
            Validations.validateDeleteTokensRequest(request);
            Assert.fail("Should have thrown exception for tokens size exceed");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.DeleteTokensSizeExceedError.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateDeleteTokensRequestMaxAllowedTokens() {
        try {
            List<String> tokens = new ArrayList<>();
            for (int i = 0; i < 10000; i++) {
                tokens.add("token" + i);
            }
            DeleteTokensRequest request = DeleteTokensRequest.builder()
                    .tokens(tokens)
                    .build();
            Validations.validateDeleteTokensRequest(request);
        } catch (SkyflowException e) {
            Assert.fail("Should not have thrown exception for exactly 10000 tokens: " + e.getMessage());
        }
    }

    @Test
    public void testValidateDeleteTokensRequestSingleToken() {
        try {
            DeleteTokensRequest request = DeleteTokensRequest.builder()
                    .tokens(Collections.singletonList("single-token"))
                    .build();
            Validations.validateDeleteTokensRequest(request);
        } catch (SkyflowException e) {
            Assert.fail("Should not have thrown exception for single valid token: " + e.getMessage());
        }
    }
}
