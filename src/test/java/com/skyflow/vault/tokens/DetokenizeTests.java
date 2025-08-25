package com.skyflow.vault.tokens;

import com.skyflow.enums.RedactionType;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.types.DetokenizeRecordResponseValueType;
import com.skyflow.generated.rest.types.V1DetokenizeRecordResponse;
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
    private static final String requestId = "95be08fc-4d13-4335-8b8d-24e85d53ed1d";
    private static ArrayList<DetokenizeData> detokenizeData = null;
    private static DetokenizeData maskedRedactionRecord = null;
    private static DetokenizeData plainRedactionRecord = null;

    @BeforeClass
    public static void setup() throws SkyflowException, NoSuchMethodException {
        detokenizeData = new ArrayList<>();
        maskedRedactionRecord = new DetokenizeData("test_token_1", RedactionType.MASKED);
        plainRedactionRecord = new DetokenizeData("test_token_2");
    }

    @Before
    public void setupTest() {
        detokenizeData.clear();
    }

    @Test
    public void testValidInputInDetokenizeRequestValidations() {
        try {
            detokenizeData.add(maskedRedactionRecord);
            DetokenizeRequest request = DetokenizeRequest.builder()
                    .detokenizeData(detokenizeData).continueOnError(false).downloadURL(true).build();
            Validations.validateDetokenizeRequest(request);
            Assert.assertEquals(1, request.getDetokenizeData().size());
            Assert.assertEquals(RedactionType.MASKED.toString(), request.getDetokenizeData().get(0).getRedactionType().toString());
            Assert.assertFalse(request.getContinueOnError());
            Assert.assertTrue(request.getDownloadURL());
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
                    Utils.parameterizedString(ErrorMessage.InvalidDetokenizeData.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
            Assert.assertFalse(request.getContinueOnError());
            Assert.assertFalse(request.getDownloadURL());
        }
    }

    @Test
    public void testEmptyTokensInDetokenizeRequestValidations() {
        DetokenizeRequest request = DetokenizeRequest.builder().detokenizeData(detokenizeData).build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyDetokenizeData.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
            Assert.assertFalse(request.getContinueOnError());
            Assert.assertFalse(request.getDownloadURL());
        }
    }

    @Test
    public void testEmptyTokenInDetokenizeRequestValidations() {
        DetokenizeData detokenizeDataRecord = new DetokenizeData("");
        detokenizeData.add(maskedRedactionRecord);
        detokenizeData.add(detokenizeDataRecord);

        DetokenizeRequest request = DetokenizeRequest.builder().detokenizeData(detokenizeData).build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyTokenInDetokenizeData.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
            Assert.assertFalse(request.getContinueOnError());
            Assert.assertFalse(request.getDownloadURL());
        }
    }

    @Test
    public void testNullTokenInDetokenizeRequestValidations() {
        DetokenizeData detokenizeDataRecord = new DetokenizeData(null);
        detokenizeData.add(maskedRedactionRecord);
        detokenizeData.add(detokenizeDataRecord);

        DetokenizeRequest request = DetokenizeRequest.builder().detokenizeData(detokenizeData).build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyTokenInDetokenizeData.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
            Assert.assertFalse(request.getContinueOnError());
            Assert.assertFalse(request.getDownloadURL());
        }
    }

    @Test
    public void testRedactionAndContinueOnErrorInDetokenizeRequestValidations() {
        detokenizeData.add(plainRedactionRecord);

        DetokenizeRequest request = DetokenizeRequest.builder().
                detokenizeData(detokenizeData).continueOnError(null).build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.assertEquals(RedactionType.DEFAULT, request.getDetokenizeData().get(0).getRedactionType());
            Assert.assertFalse(request.getContinueOnError());
            Assert.assertFalse(request.getDownloadURL());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testDetokenizeResponse() {
        try {
            V1DetokenizeRecordResponse record1 = V1DetokenizeRecordResponse.builder()
                    .token("1234-5678-9012-3456")
                    .value("4111111111111111")
                    .valueType(DetokenizeRecordResponseValueType.STRING)
                    .build();
            DetokenizeRecordResponse field = new DetokenizeRecordResponse(record1);

            V1DetokenizeRecordResponse record2 = V1DetokenizeRecordResponse.builder()
                    .token("3456-7890-1234-5678")
                    .value("")
                    .error("Invalid token")
                    .build();

            DetokenizeRecordResponse error = new DetokenizeRecordResponse(record2, requestId);

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
                    "\"errors\":[{\"token\":\"3456-7890-1234-5678\",\"error\":\"Invalid token\",\"requestId\":\"" + requestId + "\"}," +
                    "{\"token\":\"3456-7890-1234-5678\",\"error\":\"Invalid token\",\"requestId\":\"" + requestId + "\"}]}";
            Assert.assertEquals(2, response.getDetokenizedFields().size());
            Assert.assertEquals(2, response.getErrors().size());
            Assert.assertEquals("1234-5678-9012-3456", response.getDetokenizedFields().get(0).getToken());
            Assert.assertEquals("4111111111111111", response.getDetokenizedFields().get(0).getValue());
            Assert.assertEquals("STRING", response.getDetokenizedFields().get(0).getType());
            Assert.assertEquals("Invalid token", response.getErrors().get(0).getError());
            Assert.assertEquals(requestId, response.getErrors().get(0).getRequestId());
            Assert.assertEquals(responseString, response.toString());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
