package com.skyflow.vault.tokens;

import com.skyflow.enums.RedactionType;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.models.DetokenizeRecordResponseValueType;
import com.skyflow.generated.rest.models.V1DetokenizeRecordResponse;
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

    @Test
    public void testDetokenizeResponse() {
        try {
            V1DetokenizeRecordResponse record1 = new V1DetokenizeRecordResponse();
            record1.setToken("1234-5678-9012-3456");
            record1.setValue("4111111111111111");
            record1.setValueType(DetokenizeRecordResponseValueType.STRING);
            DetokenizeRecordResponse field = new DetokenizeRecordResponse(record1);

            V1DetokenizeRecordResponse record2 = new V1DetokenizeRecordResponse();
            record2.setToken("3456-7890-1234-5678");
            record2.setValue("");
            record2.setError("Invalid token");
            DetokenizeRecordResponse error = new DetokenizeRecordResponse(record2);

            ArrayList<DetokenizeRecordResponse> fields = new ArrayList<>();
            fields.add(field);
            fields.add(field);

            ArrayList<DetokenizeRecordResponse> errors = new ArrayList<>();
            errors.add(error);
            errors.add(error);

            DetokenizeResponse response = new DetokenizeResponse(fields, errors);
            String responseString = "{\"detokenizedFields\":[{" +
                    "\"token\":\"1234-5678-9012-3456\",\"value\":\"4111111111111111\",\"type\":\"STRING\"}," +
                    "{\"token\":\"1234-5678-9012-3456\",\"value\":\"4111111111111111\",\"type\":\"STRING\"}]," +
                    "\"errors\":[{\"token\":\"3456-7890-1234-5678\",\"error\":\"Invalid token\"}," +
                    "{\"token\":\"3456-7890-1234-5678\",\"error\":\"Invalid token\"}]}";
            Assert.assertEquals(2, response.getDetokenizedFields().size());
            Assert.assertEquals(2, response.getErrors().size());
            Assert.assertEquals("1234-5678-9012-3456", response.getDetokenizedFields().get(0).getToken());
            Assert.assertEquals("4111111111111111", response.getDetokenizedFields().get(0).getValue());
            Assert.assertEquals("STRING", response.getDetokenizedFields().get(0).getType());
            Assert.assertEquals("Invalid token", response.getErrors().get(0).getError());
            Assert.assertEquals(responseString, response.toString());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
