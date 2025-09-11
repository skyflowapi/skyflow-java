package com.skyflow.vault.data;

import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.validations.Validations;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DetokenizeRequestTests {
    @Test
    public void testDetokenizeRequestBuilderAndGetters() {
        List<String> tokens = Arrays.asList("token1", "token2");
        TokenGroupRedactions group = TokenGroupRedactions.builder()
                .tokenGroupName("group1")
                .redaction("PLAIN_TEXT")
                .build();
        List<TokenGroupRedactions> groupRedactions = Collections.singletonList(group);

        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(tokens)
                .tokenGroupRedactions(groupRedactions)
                .build();

        Assert.assertEquals(tokens, request.getTokens());
        Assert.assertEquals(groupRedactions, request.getTokenGroupRedactions());
    }

    @Test
    public void testTokenGroupRedactionsBuilderAndGetters() {
        TokenGroupRedactions group = TokenGroupRedactions.builder()
                .tokenGroupName("groupA")
                .redaction("MASKED")
                .build();
        Assert.assertEquals("groupA", group.getTokenGroupName());
        Assert.assertEquals("MASKED", group.getRedaction());
    }

    @Test
    public void testValidateDetokenizeRequestValid() {
        try {
            List<String> tokens = Arrays.asList("token1", "token2");
            TokenGroupRedactions group = TokenGroupRedactions.builder()
                    .tokenGroupName("group1")
                    .redaction("PLAIN_TEXT")
                    .build();
            List<TokenGroupRedactions> groupRedactions = Collections.singletonList(group);
            DetokenizeRequest request = DetokenizeRequest.builder()
                    .tokens(tokens)
                    .tokenGroupRedactions(groupRedactions)
                    .build();
            Validations.validateDetokenizeRequest(request);
        } catch (SkyflowException e) {
            Assert.fail("Should not have thrown exception for valid request");
        }
    }

    @Test
    public void testValidateDetokenizeRequestNull() {
        try {
            Validations.validateDetokenizeRequest(null);
            Assert.fail("Should have thrown exception for null request");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.DetokenizeRequestNull.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateDetokenizeRequestEmptyTokens() {
        try {
            DetokenizeRequest request = DetokenizeRequest.builder()
                    .tokens(new ArrayList<>())
                    .build();
            Validations.validateDetokenizeRequest(request);
            Assert.fail("Should have thrown exception for empty tokens");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyDetokenizeData.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateDetokenizeRequestNullOrEmptyToken() {
        try {
            List<String> tokens = Arrays.asList("token1", null, "");
            DetokenizeRequest request = DetokenizeRequest.builder()
                    .tokens(tokens)
                    .build();
            Validations.validateDetokenizeRequest(request);
            Assert.fail("Should have thrown exception for null/empty token");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyTokenInDetokenizeData.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateDetokenizeRequestTokensSizeExceed() {
        try {
            List<String> tokens = new ArrayList<>();
            for (int i = 0; i < 10001; i++) {
                tokens.add("token" + i);
            }
            DetokenizeRequest request = DetokenizeRequest.builder()
                    .tokens(tokens)
                    .build();
            Validations.validateDetokenizeRequest(request);
            Assert.fail("Should have thrown exception for tokens size exceed");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.TokensSizeExceedError.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateDetokenizeRequestNullGroupRedaction() {
        try {
            List<String> tokens = Arrays.asList("token1");
            List<TokenGroupRedactions> groupRedactions = Arrays.asList((TokenGroupRedactions) null);
            DetokenizeRequest request = DetokenizeRequest.builder()
                    .tokens(tokens)
                    .tokenGroupRedactions(groupRedactions)
                    .build();
            Validations.validateDetokenizeRequest(request);
            Assert.fail("Should have thrown exception for null group redaction");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.NullTokenGroupRedactions.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateDetokenizeRequestEmptyGroupName() {
        try {
            List<String> tokens = Arrays.asList("token1");
            TokenGroupRedactions group = TokenGroupRedactions.builder()
                    .tokenGroupName("")
                    .redaction("PLAIN_TEXT")
                    .build();
            List<TokenGroupRedactions> groupRedactions = Collections.singletonList(group);
            DetokenizeRequest request = DetokenizeRequest.builder()
                    .tokens(tokens)
                    .tokenGroupRedactions(groupRedactions)
                    .build();
            Validations.validateDetokenizeRequest(request);
            Assert.fail("Should have thrown exception for empty group name");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.NullTokenGroupNameInTokenGroup.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateDetokenizeRequestEmptyRedaction() {
        try {
            List<String> tokens = Arrays.asList("token1");
            TokenGroupRedactions group = TokenGroupRedactions.builder()
                    .tokenGroupName("group1")
                    .redaction("")
                    .build();
            List<TokenGroupRedactions> groupRedactions = Collections.singletonList(group);
            DetokenizeRequest request = DetokenizeRequest.builder()
                    .tokens(tokens)
                    .tokenGroupRedactions(groupRedactions)
                    .build();
            Validations.validateDetokenizeRequest(request);
            Assert.fail("Should have thrown exception for empty redaction");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.NullRedactionInTokenGroup.getMessage(), e.getMessage());
        }
    }
}
