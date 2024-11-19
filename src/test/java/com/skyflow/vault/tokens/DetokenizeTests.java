package com.skyflow.vault.tokens;

import com.skyflow.enums.RedactionType;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.validations.Validations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

public class DetokenizeTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String token = null;
    private static ArrayList<String> tokens = null;

    @BeforeClass
    public static void setup() throws SkyflowException, NoSuchMethodException {
        tokens = new ArrayList<>();
        token = "test_token_1";
    }

    @Before
    public void setupTest() {
        tokens.clear();
    }

    @Test
    public void testValidInputInDetokenizeRequestValidations() {
        try {
            tokens.add(token);
            DetokenizeRequest request = DetokenizeRequest.builder().
                    tokens(tokens).redactionType(RedactionType.MASKED).continueOnError(false).build();
            Validations.validateDetokenizeRequest(request);
            Assert.assertEquals(1, tokens.size());
            Assert.assertEquals(RedactionType.MASKED, request.getRedactionType());
            Assert.assertFalse(request.getContinueOnError());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testNoTokensInDetokenizeRequestValidations() {
        DetokenizeRequest request = DetokenizeRequest.builder().build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.InvalidDataTokens.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
            Assert.assertEquals(RedactionType.PLAIN_TEXT, request.getRedactionType());
            Assert.assertTrue(request.getContinueOnError());
        }
    }

    @Test
    public void testEmptyTokensInDetokenizeRequestValidations() {
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyDataTokens.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
            Assert.assertEquals(RedactionType.PLAIN_TEXT, request.getRedactionType());
            Assert.assertTrue(request.getContinueOnError());
        }
    }

    @Test
    public void testEmptyTokenInTokensInDetokenizeRequestValidations() {
        tokens.add(token);
        tokens.add("");
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyTokenInDataTokens.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
            Assert.assertEquals(RedactionType.PLAIN_TEXT, request.getRedactionType());
            Assert.assertTrue(request.getContinueOnError());
        }
    }

    @Test
    public void testRedactionAndContinueOnErrorInDetokenizeRequestValidations() {
        tokens.add(token);
        DetokenizeRequest request = DetokenizeRequest.builder().
                tokens(tokens).redactionType(null).continueOnError(null).build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.assertEquals(RedactionType.PLAIN_TEXT, request.getRedactionType());
            Assert.assertTrue(request.getContinueOnError());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
