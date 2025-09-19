package com.skyflow.utils;

import com.google.gson.JsonObject;
import com.skyflow.config.Credentials;
import com.skyflow.enums.Env;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.auth.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.types.InsertRecordData;
import com.skyflow.generated.rest.types.InsertResponse;
import com.skyflow.generated.rest.types.RecordResponseObject;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.*;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UtilsTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTIONNOTTHROWN = "Should have thrown an exception";
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
        context = "testcontext";
        roles = new ArrayList<>();
        String role = "testrole";
        roles.add(role);
        SdkVersion.setSdkPrefix(Constants.SDK_PREFIX);
    }

    @Test
    public void testGetVaultURL() {
        // Test with production environment
        String prodUrl = Utils.getVaultURL("abc123", Env.PROD);
        Assert.assertEquals(
                "https://abc123.skyvault.skyflowapis.com",
                prodUrl
        );

        // Test with development environment
        String devUrl = Utils.getVaultURL("xyz789", Env.DEV);
        Assert.assertEquals(
                "https://xyz789.skyvault.skyflowapis.dev",
                devUrl
        );
    }
    @Test(expected = NullPointerException.class)
    public void testGetVaultURLWithNullEnv() {
        Utils.getVaultURL("abc123", null);
    }

    @Test
    public void testGetVaultURLWithEmptyClusterId() {
        String url = Utils.getVaultURL("", Env.PROD);
        Assert.assertEquals(
                "https://.skyvault.skyflowapis.com",
                url
        );
    }

    @Test
    public void testGenerateBearerTokenWithCredentialsFile() {
        try {
            Credentials credentials = new Credentials();
            credentials.setPath(filePath);
            credentials.setContext(context);
            credentials.setRoles(roles);
            Utils.generateBearerToken(credentials);
            Assert.fail(EXCEPTIONNOTTHROWN);
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
            Assert.fail(EXCEPTIONNOTTHROWN);
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
            Assert.assertEquals(e.getMessage(), ErrorMessage.BearerTokenExpired.getMessage());
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
    public void testCreateBatchesMultipleBatches() {
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
    public void testCreateBatchesWithEmptyList() {
        List<InsertRecordData> records = new ArrayList<>();
        List<List<InsertRecordData>> batches = Utils.createBatches(records, 50);
        Assert.assertTrue("Batches should be empty for empty input", batches.isEmpty());
    }

    @Test
    public void testCreateBatchesWithSmallerSizeThanBatch() {
        List<InsertRecordData> records = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            records.add(InsertRecordData.builder().build());
        }
        List<List<InsertRecordData>> batches = Utils.createBatches(records, 50);

        Assert.assertEquals("Should create single batch", 1, batches.size());
        Assert.assertEquals("Batch should contain all records", 25, batches.get(0).size());
    }

    @Test
    public void testCreateBatchesWithExactBatchSize() {
        List<InsertRecordData> records = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            records.add(InsertRecordData.builder().build());
        }
        List<List<InsertRecordData>> batches = Utils.createBatches(records, 50);

        Assert.assertEquals("Should create single batch", 1, batches.size());
        Assert.assertEquals("Batch should have exact size", 50, batches.get(0).size());
    }

    @Test
    public void testCreateBatchesPreservesOrder() {
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
    public void testCreateBatchesWithNullList() {
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
    public void testHandleBatchExceptionApiClientExceptionWithSingleError() {
        List<InsertRecordData> batch = Arrays.asList(InsertRecordData.builder().build(), InsertRecordData.builder().build());
        List<List<InsertRecordData>> batches = Collections.singletonList(batch);

        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("error", "Common error");
        errorMap.put("http_code", 403);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", errorMap);

        ApiClientApiException apiException = new ApiClientApiException("Forbidden", 403, responseBody);
        Exception exception = new Exception("Test exception", apiException);

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0);

        Assert.assertEquals("Should have errors for all records", 2, errors.size());
        Assert.assertEquals("Error message should be same", "Test exception", errors.get(0).getError());
        Assert.assertEquals("Error code should be same", 500, errors.get(0).getCode());
        Assert.assertEquals("First error index", 0, errors.get(0).getIndex());
        Assert.assertEquals("Second error index", 1, errors.get(1).getIndex());
    }

    @Test
    public void testHandleBatchExceptionWithNonApiClientException() {
        List<InsertRecordData> batch = Arrays.asList(InsertRecordData.builder().build(), InsertRecordData.builder().build());
        List<List<InsertRecordData>> batches = Collections.singletonList(batch);

        RuntimeException exception = new RuntimeException("Unexpected error");

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0);

        Assert.assertEquals("Should have errors for all records", 2, errors.size());
        Assert.assertEquals("Error message should match", "Unexpected error", errors.get(0).getError());
        Assert.assertEquals("Error code should be 500", 500, errors.get(0).getCode());
        Assert.assertEquals("First error index", 0, errors.get(0).getIndex());
        Assert.assertEquals("Second error index", 1, errors.get(1).getIndex());
    }

    @Test
    public void testHandleBatchExceptionWithNonZeroBatchNumber() {
        List<InsertRecordData> batch = Arrays.asList(InsertRecordData.builder().build(), InsertRecordData.builder().build());
        List<List<InsertRecordData>> batches = Arrays.asList(new ArrayList<>(), batch);

        RuntimeException exception = new RuntimeException("Batch error");

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 1);

        Assert.assertEquals("Should have errors for all records", 2, errors.size());
        Assert.assertEquals("First error index should be offset", 2, errors.get(0).getIndex());
        Assert.assertEquals("Second error index should be offset", 3, errors.get(1).getIndex());
    }

    @Test
    public void testHandleBatchExceptionWithNullResponseBody1() {
        List<InsertRecordData> batch = Arrays.asList(InsertRecordData.builder().build(), InsertRecordData.builder().build());
        List<List<InsertRecordData>> batches = Collections.singletonList(batch);

        ApiClientApiException apiException = new ApiClientApiException("Bad Request", 400, null);
        Exception exception = new Exception("Test exception", apiException);

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0);
        Assert.assertEquals("Should return empty list for null response body", 2, errors.size());
    }

    @Test
    public void testFormatResponseWithSuccessAndErrorRecords() {
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
    public void testFormatResponseWithNullResponse() {
        com.skyflow.vault.data.InsertResponse result = Utils.formatResponse(null, 0, 50);
        Assert.assertNull(result);
    }

    @Test
    public void testFormatResponseWithSuccessRecordsOnly() {
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
    public void testFormatResponseWithErrorRecordsOnly() {
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
    public void testFormatResponseWithBatchOffset() {
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
    public void testFormatResponseWithEmptyRecords() {
        InsertResponse response = InsertResponse.builder()
                .records(Optional.of(new ArrayList<>()))
                .build();

        com.skyflow.vault.data.InsertResponse result = Utils.formatResponse(response, 0, 50);

        Assert.assertNotNull("Response should not be null", result);
        Assert.assertTrue("Success list should be empty", result.getSuccess().isEmpty());
        Assert.assertTrue("Error list should be empty", result.getErrors().isEmpty());
    }

    @Test
    public void testFormatResponseWithTokens() {
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
    @Test
    public void testFormatResponseWithTokenListMapping() {
        // Prepare test data
        Map<String, Object> tokenData = new HashMap<>();
        List<Map<String, String>> tokenList = new ArrayList<>();
        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("token", "token123");
        tokenMap.put("tokenGroupName", "group1");
        tokenList.add(tokenMap);
        tokenData.put("field1", tokenList);

        // Create success record with tokens
        RecordResponseObject successRecord = RecordResponseObject.builder()
                .skyflowId(Optional.of("id1"))
                .tokens(Optional.of(tokenData))
                .error(Optional.empty())
                .build();

        // Create response object
        InsertResponse response = InsertResponse.builder()
                .records(Optional.of(Collections.singletonList(successRecord)))
                .build();

        // Format response
        com.skyflow.vault.data.InsertResponse result = Utils.formatResponse(response, 0, 50);

        // Assertions
        Assert.assertNotNull("Response should not be null", result);
        Assert.assertEquals("Should have one success record", 1, result.getSuccess().size());

        Success successResult = result.getSuccess().get(0);
        Assert.assertEquals("Skyflow ID should match", "id1", successResult.getSkyflowId());

        Map<String, List<Token>> tokens = successResult.getTokens();
        Assert.assertNotNull("Tokens map should not be null", tokens);
        Assert.assertTrue("Should contain field1", tokens.containsKey("field1"));

        List<Token> tokensList = tokens.get("field1");
        Assert.assertEquals("Should have one token", 1, tokensList.size());
        Assert.assertEquals("Token value should match", "token123", tokensList.get(0).getToken());
        Assert.assertEquals("Token group name should match", "group1", tokensList.get(0).getTokenGroupName());
    }
    @Test
    public void testHandleBatchExceptionWithRecordsInResponseBody() {
        // Prepare test data
        List<InsertRecordData> batch = Arrays.asList(
                InsertRecordData.builder().build(),
                InsertRecordData.builder().build()
        );
        List<List<InsertRecordData>> batches = Collections.singletonList(batch);

        // Create nested records with errors
        List<Map<String, Object>> recordsList = new ArrayList<>();
        Map<String, Object> record1 = new HashMap<>();
        record1.put("error", "Error 1");
        record1.put("http_code", 400);
        Map<String, Object> record2 = new HashMap<>();
        record2.put("error", "Error 2");
        record2.put("http_code", 401);
        recordsList.add(record1);
        recordsList.add(record2);

        // Create response body
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("records", recordsList);

        // Create API exception
        ApiClientApiException apiException = new ApiClientApiException("Bad Request", 400, responseBody);
        Exception exception = new Exception("Test exception", apiException);

        // Test the method
        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0);

        // Assertions
        Assert.assertNotNull("Errors list should not be null", errors);
        Assert.assertEquals("Should have two error records", 2, errors.size());

        // Verify first error
        Assert.assertEquals("First error message should match", "Test exception", errors.get(0).getError());
        Assert.assertEquals("First error code should match", 500, errors.get(0).getCode());
        Assert.assertEquals("First error index should be 0", 0, errors.get(0).getIndex());

        // Verify second error
        Assert.assertEquals("Second error message should match", "Test exception", errors.get(1).getError());
        Assert.assertEquals("Second error code should match", 500, errors.get(1).getCode());
        Assert.assertEquals("Second error index should be 1", 1, errors.get(1).getIndex());
    }

    @Test
    public void testValidateDetokenizeRequestValidInput() throws SkyflowException {
        ArrayList<String> tokens = new ArrayList<>();
        tokens.add("token1");
        tokens.add("token2");

        ArrayList<TokenGroupRedactions> groupRedactions = new ArrayList<>();
        groupRedactions.add(TokenGroupRedactions.builder()
                .tokenGroupName("group1")
                .redaction("MASK")
                .build());

        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(tokens)
                .tokenGroupRedactions(groupRedactions)
                .build();

        Validations.validateDetokenizeRequest(request); // Should not throw an exception
    }

    @Test
    public void testValidateDetokenizeRequestNullRequest() {
        try{
            Validations.validateDetokenizeRequest(null);
            Assert.fail(EXCEPTIONNOTTHROWN);
        } catch (SkyflowException e){
            assertEquals(e.getMessage(), ErrorMessage.DetokenizeRequestNull.getMessage());
        }

    }

    @Test
    public void testValidateDetokenizeRequestEmptyTokens() {
        try {
            DetokenizeRequest request = DetokenizeRequest.builder()
                    .tokens(new ArrayList<>())
                    .tokenGroupRedactions(new ArrayList<>())
                    .build();

            Validations.validateDetokenizeRequest(request);

        } catch (SkyflowException e){
            assertEquals(e.getMessage(), ErrorMessage.EmptyDetokenizeData.getMessage());
        }
    }

    @Test
    public void testValidateDetokenizeRequestNullTokenInList() {
        ArrayList<String> tokens = new ArrayList<>();
        tokens.add(null);

        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(tokens)
                .tokenGroupRedactions(new ArrayList<>())
                .build();
    }

    @Test
    public void testValidateDetokenizeRequestNullGroupRedactions() {
        ArrayList<String> tokens = new ArrayList<>();
        tokens.add("token1");

        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(tokens)
                .tokenGroupRedactions(null)
                .build();
        try{
            Validations.validateDetokenizeRequest(request);
        } catch (SkyflowException e){
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateDetokenizeRequestNullTokenGroupRedaction() {
        ArrayList<String> tokens = new ArrayList<>();
        tokens.add("token1");

        ArrayList<TokenGroupRedactions> groupRedactions = new ArrayList<>();
        groupRedactions.add(null);

        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(tokens)
                .tokenGroupRedactions(groupRedactions)
                .build();
        try{
            Validations.validateDetokenizeRequest(request);
        } catch (SkyflowException e){
            Assert.assertEquals(ErrorMessage.NullTokenGroupRedactions.getMessage(), e.getMessage());//
        }
    }

    @Test
    public void testValidateDetokenizeRequestEmptyTokenGroupName() {
        ArrayList<String> tokens = new ArrayList<>();
        tokens.add("token1");

        ArrayList<TokenGroupRedactions> groupRedactions = new ArrayList<>();
        groupRedactions.add(TokenGroupRedactions.builder()
                .tokenGroupName("")
                .redaction("MASK")
                .build());

        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(tokens)
                .tokenGroupRedactions(groupRedactions)
                .build();

        try{
            Validations.validateDetokenizeRequest(request);
        } catch (SkyflowException e){
            assertEquals(ErrorMessage.NullTokenGroupNameInTokenGroup.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateDetokenizeRequestEmptyRedaction() {
        ArrayList<String> tokens = new ArrayList<>();
        tokens.add("token1");

        ArrayList<TokenGroupRedactions> groupRedactions = new ArrayList<>();
        groupRedactions.add(TokenGroupRedactions.builder()
                .tokenGroupName("group1")
                .redaction("")
                .build());

        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(tokens)
                .tokenGroupRedactions(groupRedactions)
                .build();

        try {
            Validations.validateDetokenizeRequest(request);
        } catch (SkyflowException e){
            assertEquals(ErrorMessage.NullRedactionInTokenGroup.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequestNullTable() {
        ArrayList<InsertRecord> values = new ArrayList<>();

        HashMap<String, Object> valueMap = new HashMap<>();
        valueMap.put("key1", "value1");
        values.add(InsertRecord.builder().data(valueMap).build());

        InsertRequest request = InsertRequest.builder()
                .table(null)
                .records(values)
                .build();

        try {
            Validations.validateInsertRequest(request);
        } catch (SkyflowException e) {
            assertEquals(ErrorMessage.TableNotSpecifiedInRequestAndRecordObject.getMessage(), e.getMessage()); // Replace with the actual error message
        }
    }

    @Test
    public void testValidateInsertRequestEmptyTable() {
        ArrayList<InsertRecord> values = new ArrayList<>();

        HashMap<String, Object> valueMap = new HashMap<>();
        valueMap.put("key1", "value1");
        values.add(InsertRecord.builder().data(valueMap).build());


        InsertRequest request = InsertRequest.builder()
                .table("")
                .records(values)
                .build();

        try {
            Validations.validateInsertRequest(request);
        } catch (SkyflowException e) {
            assertEquals(ErrorMessage.TableNotSpecifiedInRequestAndRecordObject.getMessage(), e.getMessage()); // Replace with the actual error message
        }
    }

    @Test
    public void testValidateInsertRequestNullValues() {
        InsertRequest request = InsertRequest.builder()
                .table("testTable")
                .records(null)
                .build();

        try {
            Validations.validateInsertRequest(request);
        } catch (SkyflowException e) {
            assertEquals(ErrorMessage.ValuesKeyError.getMessage(), e.getMessage()); // Replace with the actual error message
        }
    }

    @Test
    public void testValidateInsertRequestEmptyValues() {
        InsertRequest request = InsertRequest.builder()
                .table("testTable")
                .records(new ArrayList<>())
                .build();

        try {
            Validations.validateInsertRequest(request);
        } catch (SkyflowException e) {
            assertEquals(ErrorMessage.EmptyValues.getMessage(), e.getMessage()); // Replace with the actual error message
        }
    }


    @Test
    public void testFormatDetokenizeResponseValidResponse() {
        // Arrange
        List<com.skyflow.generated.rest.types.DetokenizeResponseObject> responseObjectsGen = new ArrayList<>();
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("token1", "value1");
        tokens.put("token2", "value2");
        com.skyflow.generated.rest.types.DetokenizeResponseObject object = com.skyflow.generated.rest.types.DetokenizeResponseObject.builder().token("token1").value("value1").tokenGroupName("demo").build();
        responseObjectsGen.add(object);
        responseObjectsGen.add(object);

        com.skyflow.generated.rest.types.DetokenizeResponse response = com.skyflow.generated.rest.types.DetokenizeResponse.builder().response(Optional.of(responseObjectsGen)).build();

        int batch = 0;
        int batchSize = 2;

        // Act
        DetokenizeResponse result = Utils.formatDetokenizeResponse(response, batch, batchSize);

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.getSuccess().size());
        Assert.assertEquals(0, result.getErrors().size());
    }

    @Test
    public void testFormatDetokenizeResponseResponseWithErrors() {


        List<com.skyflow.generated.rest.types.DetokenizeResponseObject> responseObjectsGen = new ArrayList<>();
        com.skyflow.generated.rest.types.DetokenizeResponseObject object = com.skyflow.generated.rest.types.DetokenizeResponseObject.builder().error("Error occurred").httpCode(400).build();
        com.skyflow.generated.rest.types.DetokenizeResponseObject object2 = com.skyflow.generated.rest.types.DetokenizeResponseObject.builder().token("token1").tokenGroupName("demo").value("hello").build();

        responseObjectsGen.add(object);
        responseObjectsGen.add(object2);

        com.skyflow.generated.rest.types.DetokenizeResponse response = com.skyflow.generated.rest.types.DetokenizeResponse.builder().response(Optional.of(responseObjectsGen)).build();

        int batch = 1;
        int batchSize = 2;

        // Act
        DetokenizeResponse result = Utils.formatDetokenizeResponse(response, batch, batchSize);

        // Assert
        assertEquals(1, result.getSuccess().size());
        assertEquals(1, result.getErrors().size());
        assertEquals("Error occurred", result.getErrors().get(0).getError());
    }

    @Test
    public void testFormatDetokenizeResponse_NullResponse() {
        // Act
        DetokenizeResponse result = Utils.formatDetokenizeResponse(null, 0, 2);

        // Assert
        Assert.assertNull(result);
    }

    @Test
    public void testCreateDetokenizeBatchesWithTokenGroupRedactions() {
        List<String> tokens = Arrays.asList("token1", "token2", "token3");
        List<com.skyflow.generated.rest.types.TokenGroupRedactions> groupRedactions = Arrays.asList(
                com.skyflow.generated.rest.types.TokenGroupRedactions.builder().tokenGroupName("group1").redaction("MASK").build(),
                com.skyflow.generated.rest.types.TokenGroupRedactions.builder().tokenGroupName("group2").redaction("PLAIN_TEXT").build(),
                com.skyflow.generated.rest.types.TokenGroupRedactions.builder().tokenGroupName("group3").redaction("REDACTED").build()
        );

        com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest request = com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest.builder()
                .vaultId("vaultId")
                .tokens(tokens)
                .tokenGroupRedactions(groupRedactions)
                .build();

        List<com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest> batches = Utils.createDetokenizeBatches(request, 2);

        Assert.assertEquals(2, batches.size());
        Assert.assertEquals(Arrays.asList("token1", "token2"), batches.get(0).getTokens().get());
        Assert.assertEquals(Arrays.asList("token3"), batches.get(1).getTokens().get());
        Assert.assertTrue(batches.get(0).getTokenGroupRedactions().isPresent());
    }

    @Test
    public void testCreateDetokenizeBatchesWithEmptyTokenGroupRedactions() {
        List<String> tokens = Arrays.asList("token1", "token2");
        com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest request = com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest.builder()
                .vaultId("vaultId")
                .tokens(tokens)
                .tokenGroupRedactions(new ArrayList<>())
                .build();

        List<com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest> batches = Utils.createDetokenizeBatches(request, 1);

        Assert.assertEquals(2, batches.size());
        Assert.assertTrue(batches.get(0).getTokenGroupRedactions().isEmpty());
    }

    @Test
    public void testCreateDetokenizeBatchesWithNullTokenGroupRedactions() {
        List<String> tokens = Arrays.asList("token1", "token2");
        List<com.skyflow.generated.rest.types.TokenGroupRedactions> groupRedactions = new ArrayList<>();
        com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest request = com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest.builder()
                .vaultId("vaultId")
                .tokens(tokens)
                .tokenGroupRedactions(groupRedactions)
                .build();

        List<com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest> batches = Utils.createDetokenizeBatches(request, 1);

        Assert.assertEquals(2, batches.size());
        Assert.assertFalse(batches.get(0).getTokenGroupRedactions().isPresent());
    }

    @Test
    public void testCreateDetokenizeBatchesWithBatchSizeGreaterThanTokens() {
        List<String> tokens = Arrays.asList("token1");
        com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest request = com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest.builder()
                .vaultId("vaultId")
                .tokens(tokens)
                .build();

        List<com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest> batches = Utils.createDetokenizeBatches(request, 5);

        Assert.assertEquals(1, batches.size());
        Assert.assertEquals(Arrays.asList("token1"), batches.get(0).getTokens().get());
    }

    public static List<com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest> createDetokenizeBatches(com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest request, int batchSize) {
        List<com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest> detokenizeRequests = new ArrayList<>();
        List<String> tokens = request.getTokens().get();

        for (int i = 0; i < tokens.size(); i += batchSize) {
            // Create a sublist for the current batch
            List<String> batchTokens = tokens.subList(i, Math.min(i + batchSize, tokens.size()));
            List<com.skyflow.generated.rest.types.TokenGroupRedactions> tokenGroupRedactions = null;
            if (request.getTokenGroupRedactions().isPresent() && !request.getTokenGroupRedactions().get().isEmpty() && i < request.getTokenGroupRedactions().get().size()) {
                tokenGroupRedactions = request.getTokenGroupRedactions().get().subList(i, Math.min(i + batchSize, request.getTokenGroupRedactions().get().size()));            }
            // Build a new DetokenizeRequest for the current batch
            com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest batchRequest = com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest.builder()
                    .vaultId(request.getVaultId())
                    .tokens(new ArrayList<>(batchTokens))
                    .tokenGroupRedactions(tokenGroupRedactions)
                    .build();

            detokenizeRequests.add(batchRequest);
        }

        return detokenizeRequests;
    }


    private DetokenizeResponseObject createResponseObject(String token, String value, String groupName, String error, Integer httpCode) {
        DetokenizeResponseObject responseObject = new DetokenizeResponseObject(
                0,
                String.valueOf(Optional.ofNullable(token)),
                Optional.ofNullable(value),
                String.valueOf(Optional.ofNullable(groupName)),
                String.valueOf(Optional.ofNullable(error)),
                null
        );return responseObject;
    }

    @Test
    public void testCreateErrorRecordWithHttpCodeKey() {
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("message", "Test message");
        recordMap.put("httpCode", 401);

        ErrorRecord error = Utils.createErrorRecord(recordMap, 2);

        Assert.assertEquals(2, error.getIndex());
        Assert.assertEquals("Test message", error.getError());
        Assert.assertEquals(401, error.getCode());
    }

    @Test
    public void testCreateErrorRecordWithStatusCodeKey() {
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("statusCode", 402);

        ErrorRecord error = Utils.createErrorRecord(recordMap, 3);

        Assert.assertEquals(3, error.getIndex());
        Assert.assertEquals("Unknown error", error.getError());
        Assert.assertEquals(402, error.getCode());
    }

    @Test
    public void testCreateErrorRecordWithUnknownErrorMessage() {
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("http_code", 403);

        ErrorRecord error = Utils.createErrorRecord(recordMap, 4);

        Assert.assertEquals(4, error.getIndex());
        Assert.assertEquals("Unknown error", error.getError());
        Assert.assertEquals(403, error.getCode());
    }
    @Test
    public void testHandleBatchExceptionWithNullResponseBody() {
        List<InsertRecordData> batch = Arrays.asList(InsertRecordData.builder().build());
        ApiClientApiException apiException = new ApiClientApiException("Error", 500, null);
        Exception exception = new Exception("Test", apiException);

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0);
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("Test", errors.get(0).getError());
        Assert.assertEquals(500, errors.get(0).getCode());
    }

    @Test
    public void testHandleBatchExceptionWithNonListRecords() {
        List<InsertRecordData> batch = Arrays.asList(InsertRecordData.builder().build());
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("records", "not_a_list");
        ApiClientApiException apiException = new ApiClientApiException("Error", 500, responseBody);
        Exception exception = new Exception("Test", apiException);

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0);
        Assert.assertEquals(1, errors.size());
    }

    @Test
    public void testHandleBatchExceptionWithErrorNotMap() {
        List<InsertRecordData> batch = Arrays.asList(InsertRecordData.builder().build());
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", "not_a_map");
        ApiClientApiException apiException = new ApiClientApiException("Error", 500, responseBody);
        Exception exception = new Exception("Test", apiException);

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0);
        Assert.assertEquals(1, errors.size());
    }

    @Test
    public void testHandleBatchExceptionWithApiClientApiExceptionCause() {
        List<InsertRecordData> batch = Arrays.asList(InsertRecordData.builder().build());
        Map<String, Object> responseBody = new HashMap<>();
        Map<String, Object> error = new HashMap<>();
        error.put("message", "Test error");
        error.put("http_code", 400);
        responseBody.put("error", error);
        responseBody.put("http_code", 400);

        com.skyflow.generated.rest.core.ApiClientApiException apiException = new com.skyflow.generated.rest.core.ApiClientApiException("Error", 400, responseBody);
        Exception exception = new Exception("Outer exception", apiException);

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0);
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("Test error", errors.get(0).getError());
        Assert.assertEquals(400, errors.get(0).getCode());
    }

}