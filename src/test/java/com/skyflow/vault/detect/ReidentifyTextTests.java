package com.skyflow.vault.detect;

import com.skyflow.enums.DetectEntities;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.validations.Validations;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.skyflow.errors.ErrorMessage.InvalidEmptyTextInReIdentify;
import static com.skyflow.errors.ErrorMessage.InvalidNullTextInReIdentify;

public class ReidentifyTextTests {

    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";

    private static final String text = "Sensitive data to reidentify";
    private static final List<DetectEntities> redactedEntities = new ArrayList<>();
    private static final List<DetectEntities> maskedEntities = new ArrayList<>();
    private static final List<DetectEntities> plainTextEntities = new ArrayList<>();

    @BeforeClass
    public static void setup() {
        redactedEntities.add(DetectEntities.NAME);
        redactedEntities.add(DetectEntities.DOB);

        maskedEntities.add(DetectEntities.USERNAME);

        plainTextEntities.add(DetectEntities.PHONE_NUMBER);
    }

    @Test
    public void testValidInputInReidentifyTextRequestValidations() {
        try {
            ReidentifyTextRequest request = ReidentifyTextRequest.builder()
                    .text(text)
                    .redactedEntities(redactedEntities)
                    .maskedEntities(maskedEntities)
                    .plainTextEntities(plainTextEntities)
                    .build();

            Validations.validateReidentifyTextRequest(request);
            Assert.assertEquals(text, request.getText());
            Assert.assertEquals(redactedEntities, request.getRedactedEntities());
            Assert.assertEquals(maskedEntities, request.getMaskedEntities());
            Assert.assertEquals(plainTextEntities, request.getPlainTextEntities());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testNoTextInReidentifyTextRequestValidations() {
        try {
            ReidentifyTextRequest request = ReidentifyTextRequest.builder().build();
            Validations.validateReidentifyTextRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        }
    }

    @Test
    public void testNullTextInReidentifyTextRequestValidations() {
        try {
            ReidentifyTextRequest request = ReidentifyTextRequest.builder()
                    .text(null)
                    .build();
            Validations.validateReidentifyTextRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(InvalidNullTextInReIdentify.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyTextInReidentifyTextRequestValidations() {
        try {
            ReidentifyTextRequest request = ReidentifyTextRequest.builder()
                    .text("")
                    .build();
            Validations.validateReidentifyTextRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(InvalidEmptyTextInReIdentify.getMessage(), e.getMessage());
        }
    }


    @Test
    public void testEmptyRedactedEntitiesInReidentifyTextRequestValidations() {
        try {
            ReidentifyTextRequest request = ReidentifyTextRequest.builder()
                    .text(text)
                    .redactedEntities(new ArrayList<>())
                    .build();
            Validations.validateReidentifyTextRequest(request);
            Assert.assertTrue(request.getRedactedEntities().isEmpty());
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        }
    }


    @Test
    public void testEmptyMaskedEntitiesInReidentifyTextRequestValidations() {
        try {
            ReidentifyTextRequest request = ReidentifyTextRequest.builder()
                    .text(text)
                    .maskedEntities(new ArrayList<>())
                    .build();
            Validations.validateReidentifyTextRequest(request);
            Assert.assertTrue(request.getMaskedEntities().isEmpty());
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        }
    }

    @Test
    public void testEmptyPlainTextEntitiesInReidentifyTextRequestValidations() {
        try {
            ReidentifyTextRequest request = ReidentifyTextRequest.builder()
                    .text(text)
                    .plainTextEntities(new ArrayList<>())
                    .build();
            Validations.validateReidentifyTextRequest(request);
            Assert.assertTrue(request.getPlainTextEntities().isEmpty());
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        }
    }


    @Test
    public void testReidentifyResponse() {
        try {
            ReidentifyTextResponse response = new ReidentifyTextResponse(text);
            Assert.assertEquals(text, response.getProcessedText());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}