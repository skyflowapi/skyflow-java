package com.skyflow.utils;

import com.google.gson.JsonObject;
import com.skyflow.config.Credentials;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.types.InsertRecordData;
import com.skyflow.generated.rest.types.InsertResponse;
import com.skyflow.generated.rest.types.RecordResponseObject;
import com.skyflow.vault.data.ErrorRecord;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

public class UtilsTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String filePath = null;
    private static String credentialsString = null;
    private static String token = null;
    private static String context = null;
    private static ArrayList<String> roles = null;

    @BeforeClass
    public static void setup() {
        filePath = "invalid/file/path/credentials.json";
        credentialsString = "invalid credentials string";
        token = "invalid-token";
        context = "test_context";
        roles = new ArrayList<>();
        String role = "test_role";
        roles.add(role);
        SdkVersion.setSdkPrefix(Constants.SDK_PREFIX);
    }

    @Test
    public void testGenerateBearerTokenWithCredentialsFile() {
        try {
            Credentials credentials = new Credentials();
            credentials.setPath(filePath);
            credentials.setContext(context);
            credentials.setRoles(roles);
            Utils.generateBearerToken(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.FileNotFound.getMessage(), filePath),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testGenerateBearerTokenWithCredentialsString() {
        try {
            Credentials credentials = new Credentials();
            credentials.setCredentialsString(credentialsString);
            credentials.setContext(context);
            credentials.setRoles(roles);
            Utils.generateBearerToken(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.CredentialsStringInvalidJson.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testGenerateBearerTokenWithToken() {
        try {
            Credentials credentials = new Credentials();
            credentials.setToken(token);
            credentials.setContext(context);
            credentials.setRoles(roles);
            String bearerToken = Utils.generateBearerToken(credentials);
            Assert.assertEquals(token, bearerToken);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetMetrics() {
        try {
            JsonObject metrics = Utils.getMetrics();
            Assert.assertNotNull(metrics.get(Constants.SDK_METRIC_NAME_VERSION));
            Assert.assertNotNull(metrics.get(Constants.SDK_METRIC_CLIENT_DEVICE_MODEL));
            Assert.assertNotNull(metrics.get(Constants.SDK_METRIC_CLIENT_OS_DETAILS));
            Assert.assertNotNull(metrics.get(Constants.SDK_METRIC_RUNTIME_DETAILS));
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetMetricsWithException() {
        try {
            System.clearProperty("os.name");
            System.clearProperty("os.version");
            System.clearProperty("java.version");

            String sdkVersion = Constants.SDK_VERSION;
            JsonObject metrics = Utils.getMetrics();
            Assert.assertEquals("skyflow-java@" + sdkVersion, metrics.get(Constants.SDK_METRIC_NAME_VERSION).getAsString());
            Assert.assertEquals("Java@", metrics.get(Constants.SDK_METRIC_RUNTIME_DETAILS).getAsString());
            Assert.assertTrue(metrics.get(Constants.SDK_METRIC_CLIENT_DEVICE_MODEL).getAsString().isEmpty());
            Assert.assertTrue(metrics.get(Constants.SDK_METRIC_CLIENT_OS_DETAILS).getAsString().isEmpty());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testCreateBatches_MultipleBatches() {
        List<InsertRecordData> records = new ArrayList<>();
        for (int i = 0; i < 125; i++) {
            records.add(InsertRecordData.builder().build());
        }
        List<List<InsertRecordData>> batches = Utils.createBatches(records, 50);

        Assert.assertEquals(3, batches.size());
        Assert.assertEquals(50, batches.get(0).size());
        Assert.assertEquals(50, batches.get(1).size());
        Assert.assertEquals(25, batches.get(2).size());
    }

    @Test
    public void testCreateBatches_WithEmptyList() {
        List<InsertRecordData> records = new ArrayList<>();
        List<List<InsertRecordData>> batches = Utils.createBatches(records, 50);
        Assert.assertTrue("Batches should be empty for empty input", batches.isEmpty());
    }

    @Test
    public void testCreateBatches_WithSmallerSizeThanBatch() {
        List<InsertRecordData> records = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            records.add(InsertRecordData.builder().build());
        }
        List<List<InsertRecordData>> batches = Utils.createBatches(records, 50);

        Assert.assertEquals("Should create single batch", 1, batches.size());
        Assert.assertEquals("Batch should contain all records", 25, batches.get(0).size());
    }

    @Test
    public void testCreateBatches_WithExactBatchSize() {
        List<InsertRecordData> records = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            records.add(InsertRecordData.builder().build());
        }
        List<List<InsertRecordData>> batches = Utils.createBatches(records, 50);

        Assert.assertEquals("Should create single batch", 1, batches.size());
        Assert.assertEquals("Batch should have exact size", 50, batches.get(0).size());
    }

    @Test
    public void testCreateBatches_PreservesOrder() {
        List<InsertRecordData> records = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            InsertRecordData record = InsertRecordData.builder()
                    .data(Optional.of(Collections.singletonMap("id", String.valueOf(i))))
                    .build();
            records.add(record);
        }

        List<List<InsertRecordData>> batches = Utils.createBatches(records, 50);

        Assert.assertEquals("Should create two batches", 2, batches.size());
        Assert.assertEquals("First record in first batch should be 0",
                "0", batches.get(0).get(0).getData().get().get("id"));
        Assert.assertEquals("First record in second batch should be 50",
                "50", batches.get(1).get(0).getData().get().get("id"));
    }

    @Test(expected = NullPointerException.class)
    public void testCreateBatches_WithNullList() {
        Utils.createBatches(null, 50);
    }

    @Test
    public void testCreateErrorRecord() {
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("error", "Test error");
        recordMap.put("message", "Test message");
        recordMap.put("http_code", 400);

        ErrorRecord error = Utils.createErrorRecord(recordMap, 1);

        Assert.assertEquals(1, error.getIndex());
        Assert.assertEquals("Test error", error.getError());
        Assert.assertEquals(400, error.getCode());
    }

    @Test
    public void testHandleBatchException_ApiClientExceptionWithSingleError() {
        List<InsertRecordData> batch = Arrays.asList(InsertRecordData.builder().build(), InsertRecordData.builder().build());
        List<List<InsertRecordData>> batches = Collections.singletonList(batch);

        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("error", "Common error");
        errorMap.put("http_code", 403);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", errorMap);

        ApiClientApiException apiException = new ApiClientApiException("Forbidden", 403, responseBody);
        Exception exception = new Exception("Test exception", apiException);

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0, batches);

        Assert.assertEquals("Should have errors for all records", 2, errors.size());
        Assert.assertEquals("Error message should be same", "Common error", errors.get(0).getError());
        Assert.assertEquals("Error code should be same", 403, errors.get(0).getCode());
        Assert.assertEquals("First error index", 0, errors.get(0).getIndex());
        Assert.assertEquals("Second error index", 1, errors.get(1).getIndex());
    }

    @Test
    public void testHandleBatchException_WithNonApiClientException() {
        List<InsertRecordData> batch = Arrays.asList(InsertRecordData.builder().build(), InsertRecordData.builder().build());
        List<List<InsertRecordData>> batches = Collections.singletonList(batch);

        RuntimeException exception = new RuntimeException("Unexpected error");

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0, batches);

        Assert.assertEquals("Should have errors for all records", 2, errors.size());
        Assert.assertEquals("Error message should match", "Unexpected error", errors.get(0).getError());
        Assert.assertEquals("Error code should be 500", 500, errors.get(0).getCode());
        Assert.assertEquals("First error index", 0, errors.get(0).getIndex());
        Assert.assertEquals("Second error index", 1, errors.get(1).getIndex());
    }

    @Test
    public void testHandleBatchException_WithNonZeroBatchNumber() {
        List<InsertRecordData> batch = Arrays.asList(InsertRecordData.builder().build(), InsertRecordData.builder().build());
        List<List<InsertRecordData>> batches = Arrays.asList(new ArrayList<>(), batch);

        RuntimeException exception = new RuntimeException("Batch error");

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 1, batches);

        Assert.assertEquals("Should have errors for all records", 2, errors.size());
        Assert.assertEquals("First error index should be offset", 2, errors.get(0).getIndex());
        Assert.assertEquals("Second error index should be offset", 3, errors.get(1).getIndex());
    }

    @Test
    public void testHandleBatchException_WithNullResponseBody() {
        List<InsertRecordData> batch = Arrays.asList(InsertRecordData.builder().build(), InsertRecordData.builder().build());
        List<List<InsertRecordData>> batches = Collections.singletonList(batch);

        ApiClientApiException apiException = new ApiClientApiException("Bad Request", 400, null);
        Exception exception = new Exception("Test exception", apiException);

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0, batches);

        Assert.assertEquals("Should return empty list for null response body", 0, errors.size());
    }

    @Test
    public void testFormatResponse_WithSuccessAndErrorRecords() {
        RecordResponseObject successRecord = RecordResponseObject.builder()
                .skyflowId(Optional.of("testId1"))
                .error(Optional.empty())
                .build();
        RecordResponseObject errorRecord = RecordResponseObject.builder()
                .error(Optional.of("Test error"))
                .httpCode(Optional.of(400))
                .build();

        InsertResponse response = InsertResponse.builder()
                .records(Optional.of(Arrays.asList(successRecord, errorRecord)))
                .build();

        com.skyflow.vault.data.InsertResponse result = Utils.formatResponse(response, 0, 50);

        Assert.assertNotNull(result.getSuccess());
        Assert.assertEquals(1, result.getSuccess().size());
        Assert.assertEquals("testId1", result.getSuccess().get(0).getSkyflowId());

        Assert.assertNotNull(result.getErrors());
        Assert.assertEquals(1, result.getErrors().size());
        Assert.assertEquals("Test error", result.getErrors().get(0).getError());
        Assert.assertEquals(400, result.getErrors().get(0).getCode());
    }

    @Test
    public void testFormatResponse_WithNullResponse() {
        com.skyflow.vault.data.InsertResponse result = Utils.formatResponse(null, 0, 50);
        Assert.assertNull(result);
    }

    @Test
    public void testFormatResponse_WithSuccessRecordsOnly() {
        RecordResponseObject successRecord1 = RecordResponseObject.builder()
                .skyflowId(Optional.of("id1"))
                .error(Optional.empty())
                .build();
        RecordResponseObject successRecord2 = RecordResponseObject.builder()
                .skyflowId(Optional.of("id2"))
                .error(Optional.empty())
                .build();

        InsertResponse response = InsertResponse.builder()
                .records(Optional.of(Arrays.asList(successRecord1, successRecord2)))
                .build();

        com.skyflow.vault.data.InsertResponse result = Utils.formatResponse(response, 0, 50);

        Assert.assertNotNull("Response should not be null", result);
        Assert.assertEquals("Should have two success records", 2, result.getSuccess().size());
        Assert.assertEquals("First skyflow ID should match", "id1", result.getSuccess().get(0).getSkyflowId());
        Assert.assertEquals("Second skyflow ID should match", "id2", result.getSuccess().get(1).getSkyflowId());
        Assert.assertTrue("Error list should be empty", result.getErrors().isEmpty());
    }

    @Test
    public void testFormatResponse_WithErrorRecordsOnly() {
        RecordResponseObject errorRecord1 = RecordResponseObject.builder()
                .error(Optional.of("Error 1"))
                .httpCode(Optional.of(400))
                .build();
        RecordResponseObject errorRecord2 = RecordResponseObject.builder()
                .error(Optional.of("Error 2"))
                .httpCode(Optional.of(500))
                .build();

        InsertResponse response = InsertResponse.builder()
                .records(Optional.of(Arrays.asList(errorRecord1, errorRecord2)))
                .build();

        com.skyflow.vault.data.InsertResponse result = Utils.formatResponse(response, 0, 50);

        Assert.assertNotNull("Response should not be null", result);
        Assert.assertEquals("Should have two error records", 2, result.getErrors().size());
        Assert.assertEquals("First error message should match", "Error 1", result.getErrors().get(0).getError());
        Assert.assertEquals("First error code should match", 400, result.getErrors().get(0).getCode());
        Assert.assertEquals("Second error message should match", "Error 2", result.getErrors().get(1).getError());
        Assert.assertEquals("Second error code should match", 500, result.getErrors().get(1).getCode());
        Assert.assertTrue("Success list should be empty", result.getSuccess().isEmpty());
    }

    @Test
    public void testFormatResponse_WithBatchOffset() {
        RecordResponseObject successRecord = RecordResponseObject.builder()
                .skyflowId(Optional.of("id1"))
                .error(Optional.empty())
                .build();
        RecordResponseObject errorRecord = RecordResponseObject.builder()
                .error(Optional.of("Error"))
                .httpCode(Optional.of(400))
                .build();

        InsertResponse response = InsertResponse.builder()
                .records(Optional.of(Arrays.asList(successRecord, errorRecord)))
                .build();

        com.skyflow.vault.data.InsertResponse result = Utils.formatResponse(response, 1, 50);

        Assert.assertNotNull("Response should not be null", result);
        Assert.assertEquals("Should have correct index for error record", 51, result.getErrors().get(0).getIndex());
    }

    @Test
    public void testFormatResponse_WithEmptyRecords() {
        InsertResponse response = InsertResponse.builder()
                .records(Optional.of(new ArrayList<>()))
                .build();

        com.skyflow.vault.data.InsertResponse result = Utils.formatResponse(response, 0, 50);

        Assert.assertNotNull("Response should not be null", result);
        Assert.assertTrue("Success list should be empty", result.getSuccess().isEmpty());
        Assert.assertTrue("Error list should be empty", result.getErrors().isEmpty());
    }

    @Test
    public void testFormatResponse_WithTokens() {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("field1", "token1");
        tokens.put("field2", "token2");

        RecordResponseObject successRecord = RecordResponseObject.builder()
                .skyflowId(Optional.of("id1"))
                .tokens(Optional.of(tokens))
                .error(Optional.empty())
                .build();

        InsertResponse response = InsertResponse.builder()
                .records(Optional.of(Collections.singletonList(successRecord)))
                .build();

        com.skyflow.vault.data.InsertResponse result = Utils.formatResponse(response, 0, 50);

        Assert.assertNotNull("Response should not be null", result);
        Assert.assertEquals("Should have one success record", 1, result.getSuccess().size());
        Assert.assertEquals("Skyflow ID should match", "id1", result.getSuccess().get(0).getSkyflowId());
    }
}