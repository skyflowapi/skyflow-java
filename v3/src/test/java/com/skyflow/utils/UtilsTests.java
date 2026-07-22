package com.skyflow.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.skyflow.config.Credentials;
import com.skyflow.enums.Env;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.auth.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.types.V1DeleteTokenResponseObject;
import com.skyflow.generated.rest.types.V1FlowDeleteTokenResponse;
import com.skyflow.generated.rest.types.V1FlowDetokenizeResponseObject;
import com.skyflow.generated.rest.types.V1FlowTokenizeResponseObject;
import com.skyflow.generated.rest.types.V1InsertRecordData;
import com.skyflow.generated.rest.types.V1InsertResponse;
import com.skyflow.generated.rest.types.V1RecordResponseObject;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.*;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Assume;
import org.junit.Test;
import org.junit.Assert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.assertEquals;

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
        context = "testcontext";
        roles = new ArrayList<>();
        String role = "testrole";
        roles.add(role);
        SdkVersion.setSdkPrefix(Constants.SDK_PREFIX);
    }

    public static List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest> createDetokenizeBatches(com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest request, int batchSize) {
        List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest> detokenizeRequests = new ArrayList<>();
        List<String> tokens = request.getTokens().get();

        for (int i = 0; i < tokens.size(); i += batchSize) {
            // Create a sublist for the current batch
            List<String> batchTokens = tokens.subList(i, Math.min(i + batchSize, tokens.size()));
            List<com.skyflow.generated.rest.types.V1TokenGroupRedactions> tokenGroupRedactions = null;
            if (request.getTokenGroupRedactions().isPresent() && !request.getTokenGroupRedactions().get().isEmpty() && i < request.getTokenGroupRedactions().get().size()) {
                tokenGroupRedactions = request.getTokenGroupRedactions().get().subList(i, Math.min(i + batchSize, request.getTokenGroupRedactions().get().size()));
            }
            // Build a new DetokenizeRequest for the current batch
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest batchRequest = com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.builder()
                    .vaultId(request.getVaultId())
                    .tokens(new ArrayList<>(batchTokens))
                    .tokenGroupRedactions(tokenGroupRedactions)
                    .build();

            detokenizeRequests.add(batchRequest);
        }

        return detokenizeRequests;
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
        List<V1InsertRecordData> records = new ArrayList<>();
        for (int i = 0; i < 125; i++) {
            records.add(V1InsertRecordData.builder().build());
        }
        List<List<V1InsertRecordData>> batches = Utils.createBatches(records, 50);

        Assert.assertEquals(3, batches.size());
        Assert.assertEquals(50, batches.get(0).size());
        Assert.assertEquals(50, batches.get(1).size());
        Assert.assertEquals(25, batches.get(2).size());
    }

    @Test
    public void testCreateBatchesWithEmptyList() {
        List<V1InsertRecordData> records = new ArrayList<>();
        List<List<V1InsertRecordData>> batches = Utils.createBatches(records, 50);
        Assert.assertTrue("Batches should be empty for empty input", batches.isEmpty());
    }

    @Test
    public void testCreateBatchesWithSmallerSizeThanBatch() {
        List<V1InsertRecordData> records = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            records.add(V1InsertRecordData.builder().build());
        }
        List<List<V1InsertRecordData>> batches = Utils.createBatches(records, 50);

        Assert.assertEquals("Should create single batch", 1, batches.size());
        Assert.assertEquals("Batch should contain all records", 25, batches.get(0).size());
    }

    @Test
    public void testCreateBatchesWithExactBatchSize() {
        List<V1InsertRecordData> records = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            records.add(V1InsertRecordData.builder().build());
        }
        List<List<V1InsertRecordData>> batches = Utils.createBatches(records, 50);

        Assert.assertEquals("Should create single batch", 1, batches.size());
        Assert.assertEquals("Batch should have exact size", 50, batches.get(0).size());
    }

    @Test
    public void testCreateBatchesPreservesOrder() {
        List<V1InsertRecordData> records = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            V1InsertRecordData record = V1InsertRecordData.builder()
                    .data(Optional.of(Collections.singletonMap("id", String.valueOf(i))))
                    .build();
            records.add(record);
        }

        List<List<V1InsertRecordData>> batches = Utils.createBatches(records, 50);

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
        List<V1InsertRecordData> batch = Arrays.asList(V1InsertRecordData.builder().build(), V1InsertRecordData.builder().build());
        List<List<V1InsertRecordData>> batches = Collections.singletonList(batch);

        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("error", "Common error");
        errorMap.put("http_code", 403);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", errorMap);

        ApiClientApiException apiException = new ApiClientApiException("Forbidden", 403, responseBody);
        Exception exception = new Exception("Test exception", apiException);

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0, 1);

        Assert.assertEquals("Should have errors for all records", 2, errors.size());
        Assert.assertEquals("Error message should be same", "Test exception", errors.get(0).getError());
        Assert.assertEquals("Error code should be same", 500, errors.get(0).getCode());
        Assert.assertEquals("First error index", 0, errors.get(0).getIndex());
        Assert.assertEquals("Second error index", 1, errors.get(1).getIndex());
    }

    @Test
    public void testHandleBatchExceptionWithNonApiClientException() {
        List<V1InsertRecordData> batch = Arrays.asList(V1InsertRecordData.builder().build(), V1InsertRecordData.builder().build());
        List<List<V1InsertRecordData>> batches = Collections.singletonList(batch);

        RuntimeException exception = new RuntimeException("Unexpected error");

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0, 1);

        Assert.assertEquals("Should have errors for all records", 2, errors.size());
        Assert.assertEquals("Error message should match", "Unexpected error", errors.get(0).getError());
        Assert.assertEquals("Error code should be 500", 500, errors.get(0).getCode());
        Assert.assertEquals("First error index", 0, errors.get(0).getIndex());
        Assert.assertEquals("Second error index", 1, errors.get(1).getIndex());
    }

    @Test
    public void testHandleBatchExceptionWithNonZeroBatchNumber() {
        List<V1InsertRecordData> batch = Arrays.asList(V1InsertRecordData.builder().build(), V1InsertRecordData.builder().build());
        List<List<V1InsertRecordData>> batches = Arrays.asList(new ArrayList<>(), batch);

        RuntimeException exception = new RuntimeException("Batch error");

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 1, 1);
        Assert.assertEquals("Should have errors for all records", 2, errors.size());
        Assert.assertEquals("First error index should be offset", 1, errors.get(0).getIndex());
        Assert.assertEquals("Second error index should be offset", 2, errors.get(1).getIndex());
    }

    @Test
    public void testHandleBatchExceptionWithNullResponseBody1() {
        List<V1InsertRecordData> batch = Arrays.asList(V1InsertRecordData.builder().build(), V1InsertRecordData.builder().build());
        List<List<V1InsertRecordData>> batches = Collections.singletonList(batch);

        ApiClientApiException apiException = new ApiClientApiException("Bad Request", 400, null);
        Exception exception = new Exception("Test exception", apiException);

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0, 1);
        Assert.assertEquals("Should return empty list for null response body", 2, errors.size());
    }

    @Test
    public void testFormatResponseWithSuccessAndErrorRecords() {
        V1RecordResponseObject successRecord = V1RecordResponseObject.builder()
                .skyflowId(Optional.of("testId1"))
                .error(Optional.empty())
                .build();
        V1RecordResponseObject errorRecord = V1RecordResponseObject.builder()
                .error(Optional.of("Test error"))
                .httpCode(Optional.of(400))
                .build();

        V1InsertResponse response = V1InsertResponse.builder()
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
        V1RecordResponseObject successRecord1 = V1RecordResponseObject.builder()
                .skyflowId(Optional.of("id1"))
                .error(Optional.empty())
                .build();
        V1RecordResponseObject successRecord2 = V1RecordResponseObject.builder()
                .skyflowId(Optional.of("id2"))
                .error(Optional.empty())
                .build();

        V1InsertResponse response = V1InsertResponse.builder()
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
        V1RecordResponseObject errorRecord1 = V1RecordResponseObject.builder()
                .error(Optional.of("Error 1"))
                .httpCode(Optional.of(400))
                .build();
        V1RecordResponseObject errorRecord2 = V1RecordResponseObject.builder()
                .error(Optional.of("Error 2"))
                .httpCode(Optional.of(500))
                .build();

        V1InsertResponse response = V1InsertResponse.builder()
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
        V1RecordResponseObject successRecord = V1RecordResponseObject.builder()
                .skyflowId(Optional.of("id1"))
                .error(Optional.empty())
                .build();
        V1RecordResponseObject errorRecord = V1RecordResponseObject.builder()
                .error(Optional.of("Error"))
                .httpCode(Optional.of(400))
                .build();

        V1InsertResponse response = V1InsertResponse.builder()
                .records(Optional.of(Arrays.asList(successRecord, errorRecord)))
                .build();

        com.skyflow.vault.data.InsertResponse result = Utils.formatResponse(response, 1, 50);

        Assert.assertNotNull("Response should not be null", result);
        Assert.assertEquals("Should have correct index for error record", 51, result.getErrors().get(0).getIndex());
    }

    @Test
    public void testFormatResponseWithEmptyRecords() {
        V1InsertResponse response = V1InsertResponse.builder()
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

        V1RecordResponseObject successRecord = V1RecordResponseObject.builder()
                .skyflowId(Optional.of("id1"))
                .tokens(Optional.of(tokens))
                .error(Optional.empty())
                .build();

        V1InsertResponse response = V1InsertResponse.builder()
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
        V1RecordResponseObject successRecord = V1RecordResponseObject.builder()
                .skyflowId(Optional.of("id1"))
                .tokens(Optional.of(tokenData))
                .error(Optional.empty())
                .build();

        // Create response object
        V1InsertResponse response = V1InsertResponse.builder()
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
        List<V1InsertRecordData> batch = Arrays.asList(
                V1InsertRecordData.builder().build(),
                V1InsertRecordData.builder().build()
        );
        List<List<V1InsertRecordData>> batches = Collections.singletonList(batch);

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
        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0, 1);

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
        try {
            Validations.validateDetokenizeRequest(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
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

        } catch (SkyflowException e) {
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
        try {
            Validations.validateDetokenizeRequest(request);
        } catch (SkyflowException e) {
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
        try {
            Validations.validateDetokenizeRequest(request);
        } catch (SkyflowException e) {
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

        try {
            Validations.validateDetokenizeRequest(request);
        } catch (SkyflowException e) {
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
        } catch (SkyflowException e) {
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
            assertEquals(ErrorMessage.RecordsKeyError.getMessage(), e.getMessage()); // Replace with the actual error message
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
            assertEquals(ErrorMessage.EmptyRecords.getMessage(), e.getMessage()); // Replace with the actual error message
        }
    }

    @Test
    public void testFormatDetokenizeResponseValidResponse() {
        // Arrange
        List<com.skyflow.generated.rest.types.V1FlowDetokenizeResponseObject> responseObjectsGen = new ArrayList<>();
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("token1", "value1");
        tokens.put("token2", "value2");
        com.skyflow.generated.rest.types.V1FlowDetokenizeResponseObject object = com.skyflow.generated.rest.types.V1FlowDetokenizeResponseObject.builder().token("token1").value("value1").tokenGroupName("demo").build();
        responseObjectsGen.add(object);
        responseObjectsGen.add(object);

        com.skyflow.generated.rest.types.V1FlowDetokenizeResponse response = com.skyflow.generated.rest.types.V1FlowDetokenizeResponse.builder().response(Optional.of(responseObjectsGen)).build();

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


        List<com.skyflow.generated.rest.types.V1FlowDetokenizeResponseObject> responseObjectsGen = new ArrayList<>();
        com.skyflow.generated.rest.types.V1FlowDetokenizeResponseObject object = com.skyflow.generated.rest.types.V1FlowDetokenizeResponseObject.builder().error("Error occurred").httpCode(400).build();
        com.skyflow.generated.rest.types.V1FlowDetokenizeResponseObject object2 = com.skyflow.generated.rest.types.V1FlowDetokenizeResponseObject.builder().token("token1").tokenGroupName("demo").value("hello").build();

        responseObjectsGen.add(object);
        responseObjectsGen.add(object2);

        com.skyflow.generated.rest.types.V1FlowDetokenizeResponse response = com.skyflow.generated.rest.types.V1FlowDetokenizeResponse.builder().response(Optional.of(responseObjectsGen)).build();

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
        List<com.skyflow.generated.rest.types.V1TokenGroupRedactions> groupRedactions = Arrays.asList(
                com.skyflow.generated.rest.types.V1TokenGroupRedactions.builder().tokenGroupName("group1").redaction("MASK").build(),
                com.skyflow.generated.rest.types.V1TokenGroupRedactions.builder().tokenGroupName("group2").redaction("PLAIN_TEXT").build(),
                com.skyflow.generated.rest.types.V1TokenGroupRedactions.builder().tokenGroupName("group3").redaction("REDACTED").build()
        );

        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest request = com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.builder()
                .vaultId("vaultId")
                .tokens(tokens)
                .tokenGroupRedactions(groupRedactions)
                .build();

        List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest> batches = Utils.createDetokenizeBatches(request, 2);

        Assert.assertEquals(2, batches.size());
        Assert.assertEquals(Arrays.asList("token1", "token2"), batches.get(0).getTokens().get());
        Assert.assertEquals(Arrays.asList("token3"), batches.get(1).getTokens().get());
        Assert.assertTrue(batches.get(0).getTokenGroupRedactions().isPresent());
    }

    @Test
    public void testCreateDetokenizeBatchesWithEmptyTokenGroupRedactions() {
        List<String> tokens = Arrays.asList("token1", "token2");
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest request = com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.builder()
                .vaultId("vaultId")
                .tokens(tokens)
                .tokenGroupRedactions(new ArrayList<>())
                .build();

        List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest> batches = Utils.createDetokenizeBatches(request, 1);

        Assert.assertEquals(2, batches.size());
//        List<com.skyflow.generated.rest.types.TokenGroupRedactions> redactions = batches.get(0).getTokenGroupRedactions().get();
//        Assert.assertTrue(redactions.isEmpty());
    }

    @Test
    public void testCreateDetokenizeBatchesWithNullTokenGroupRedactions() {
        List<String> tokens = Arrays.asList("token1", "token2");
        List<com.skyflow.generated.rest.types.V1TokenGroupRedactions> groupRedactions = new ArrayList<>();
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest request = com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.builder()
                .vaultId("vaultId")
                .tokens(tokens)
                .tokenGroupRedactions(groupRedactions)
                .build();

        List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest> batches = Utils.createDetokenizeBatches(request, 1);

        Assert.assertEquals(2, batches.size());
        Assert.assertFalse(batches.get(0).getTokenGroupRedactions().isPresent());
    }

    @Test
    public void testCreateDetokenizeBatchesWithBatchSizeGreaterThanTokens() {
        List<String> tokens = Arrays.asList("token1");
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest request = com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.builder()
                .vaultId("vaultId")
                .tokens(tokens)
                .build();

        List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest> batches = Utils.createDetokenizeBatches(request, 5);

        Assert.assertEquals(1, batches.size());
        Assert.assertEquals(Arrays.asList("token1"), batches.get(0).getTokens().get());
    }

    // ── handleBatchException — rest.ApiClientApiException paths ──────────────

    @Test
    public void handleBatchException_restException_nullBody_createsOneErrorPerRecord() {
        List<V1InsertRecordData> batch = Arrays.asList(
                V1InsertRecordData.builder().build(), V1InsertRecordData.builder().build());
        com.skyflow.generated.rest.core.ApiClientApiException apiEx =
                new com.skyflow.generated.rest.core.ApiClientApiException("server error", 503, null);
        Exception wrapper = new Exception("outer", apiEx);

        List<ErrorRecord> errors = Utils.handleBatchException(wrapper, batch, 0, 2);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals(0, errors.get(0).getIndex());
        Assert.assertEquals(1, errors.get(1).getIndex());
        Assert.assertEquals(503, errors.get(0).getCode());
    }

    @Test
    public void handleBatchException_restException_stringBody_doesNotThrowAndCreatesErrors() {
        List<V1InsertRecordData> batch = Collections.singletonList(V1InsertRecordData.builder().build());
        com.skyflow.generated.rest.core.ApiClientApiException apiEx =
                new com.skyflow.generated.rest.core.ApiClientApiException("Unauthorized", 401, "plain string body");
        Exception wrapper = new Exception("outer", apiEx);

        List<ErrorRecord> errors = Utils.handleBatchException(wrapper, batch, 0, 1);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(0, errors.get(0).getIndex());
        Assert.assertEquals(401, errors.get(0).getCode());
    }

    @Test
    public void handleBatchException_restException_errorFieldIsString_usesStringAsMessage() {
        List<V1InsertRecordData> batch = Arrays.asList(
                V1InsertRecordData.builder().build(), V1InsertRecordData.builder().build());
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Access denied");
        com.skyflow.generated.rest.core.ApiClientApiException apiEx =
                new com.skyflow.generated.rest.core.ApiClientApiException("Forbidden", 403, body);
        Exception wrapper = new Exception("outer", apiEx);

        List<ErrorRecord> errors = Utils.handleBatchException(wrapper, batch, 0, 2);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals("Access denied", errors.get(0).getError());
        Assert.assertEquals(403, errors.get(0).getCode());
        Assert.assertEquals(0, errors.get(0).getIndex());
        Assert.assertEquals(1, errors.get(1).getIndex());
    }

    @Test
    public void handleBatchException_recordsListWithNonMapEntry_skipsNonMapItem() {
        List<V1InsertRecordData> batch = Arrays.asList(
                V1InsertRecordData.builder().build(), V1InsertRecordData.builder().build());
        Map<String, Object> rec = new HashMap<>();
        rec.put("error", "Err");
        rec.put("http_code", 400);
        List<Object> mixedList = new ArrayList<>(Arrays.asList(rec, "not-a-map"));
        Map<String, Object> body = new HashMap<>();
        body.put("records", mixedList);
        com.skyflow.generated.rest.core.ApiClientApiException apiEx =
                new com.skyflow.generated.rest.core.ApiClientApiException("Error", 400, body);
        Exception wrapper = new Exception("outer", apiEx);

        List<ErrorRecord> errors = Utils.handleBatchException(wrapper, batch, 0, 2);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("Err", errors.get(0).getError());
        Assert.assertEquals(400, errors.get(0).getCode());
        Assert.assertEquals(0, errors.get(0).getIndex());
    }

    @Test
    public void handleBatchException_restException_nullBody_batchNumber1_indexOffset() {
        List<V1InsertRecordData> batch = Arrays.asList(
                V1InsertRecordData.builder().build(), V1InsertRecordData.builder().build());
        com.skyflow.generated.rest.core.ApiClientApiException apiEx =
                new com.skyflow.generated.rest.core.ApiClientApiException("error", 500, null);
        Exception wrapper = new Exception("outer", apiEx);

        List<ErrorRecord> errors = Utils.handleBatchException(wrapper, batch, 2, 3);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals(6, errors.get(0).getIndex()); // 2 * 3 = 6
        Assert.assertEquals(7, errors.get(1).getIndex());
    }

    // ── handleDetokenizeBatchException — non-Map items in response list ────────

    @Test
    public void handleDetokenizeBatchException_responseListWithNonMapEntry_skipsNonMapItem() {
        List<String> tokens = Arrays.asList("t1", "t2");
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.builder()
                        .tokens(tokens).vaultId("v1").build();

        Map<String, Object> rec = new HashMap<>();
        rec.put("error", "bad token");
        rec.put("http_code", 400);
        List<Object> mixedList = new ArrayList<>(Arrays.asList(rec, "not-a-map"));
        Map<String, Object> body = new HashMap<>();
        body.put("response", mixedList);
        com.skyflow.generated.rest.core.ApiClientApiException apiEx =
                new com.skyflow.generated.rest.core.ApiClientApiException("Error", 400, body);
        Exception wrapper = new Exception("outer", apiEx);

        List<ErrorRecord> errors = Utils.handleDetokenizeBatchException(wrapper, batch, 0, 2);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("bad token", errors.get(0).getError());
        Assert.assertEquals(400, errors.get(0).getCode());
        Assert.assertEquals(0, errors.get(0).getIndex());
    }

    @Test
    public void handleDetokenizeBatchException_restException_nullBody_createsOneErrorPerToken() {
        List<String> tokens = Arrays.asList("t1", "t2", "t3");
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.builder()
                        .tokens(tokens).vaultId("v1").build();
        com.skyflow.generated.rest.core.ApiClientApiException apiEx =
                new com.skyflow.generated.rest.core.ApiClientApiException("gateway timeout", 504, null);
        Exception wrapper = new Exception("outer", apiEx);

        List<ErrorRecord> errors = Utils.handleDetokenizeBatchException(wrapper, batch, 1, 3);

        Assert.assertEquals(3, errors.size());
        Assert.assertEquals(3, errors.get(0).getIndex()); // 1 * 3 = 3
        Assert.assertEquals(4, errors.get(1).getIndex());
        Assert.assertEquals(5, errors.get(2).getIndex());
        Assert.assertEquals(504, errors.get(0).getCode());
    }

    @Test
    public void handleDetokenizeBatchException_restException_errorFieldIsString_usesStringAsMessage() {
        List<String> tokens = Arrays.asList("t1", "t2");
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.builder()
                        .tokens(tokens).vaultId("v1").build();
        Map<String, Object> body = new HashMap<>();
        body.put("error", "token not found");
        com.skyflow.generated.rest.core.ApiClientApiException apiEx =
                new com.skyflow.generated.rest.core.ApiClientApiException("Not found", 404, body);
        Exception wrapper = new Exception("outer", apiEx);

        List<ErrorRecord> errors = Utils.handleDetokenizeBatchException(wrapper, batch, 0, 2);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals("token not found", errors.get(0).getError());
        Assert.assertEquals(404, errors.get(0).getCode());
    }

    // ── handleDeleteTokensBatchException — rest exception + null body ──────────

    @Test
    public void handleDeleteTokensBatchException_restException_nullBody_createsOneErrorPerToken() {
        List<String> tokens = Arrays.asList("tok1", "tok2", "tok3");
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest.builder()
                        .tokens(tokens).vaultId("v1").build();
        com.skyflow.generated.rest.core.ApiClientApiException apiEx =
                new com.skyflow.generated.rest.core.ApiClientApiException("Service unavailable", 503, null);
        Exception wrapper = new Exception("outer", apiEx);

        List<ErrorRecord> errors = Utils.handleDeleteTokensBatchException(wrapper, batch, 0, 3);

        Assert.assertEquals(3, errors.size());
        Assert.assertEquals(0, errors.get(0).getIndex());
        Assert.assertEquals(1, errors.get(1).getIndex());
        Assert.assertEquals(2, errors.get(2).getIndex());
        Assert.assertEquals(503, errors.get(0).getCode());
    }

    @Test
    public void handleDeleteTokensBatchException_restException_stringBody_doesNotThrow() {
        List<String> tokens = Collections.singletonList("tok1");
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest.builder()
                        .tokens(tokens).vaultId("v1").build();
        com.skyflow.generated.rest.core.ApiClientApiException apiEx =
                new com.skyflow.generated.rest.core.ApiClientApiException("Forbidden", 403, "string error body");
        Exception wrapper = new Exception("outer", apiEx);

        List<ErrorRecord> errors = Utils.handleDeleteTokensBatchException(wrapper, batch, 0, 1);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(403, errors.get(0).getCode());
    }

    @Test
    public void handleDeleteTokensBatchException_restException_errorFieldIsString_usesStringAsMessage() {
        List<String> tokens = Arrays.asList("tok1", "tok2");
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest.builder()
                        .tokens(tokens).vaultId("v1").build();
        Map<String, Object> body = new HashMap<>();
        body.put("error", "quota exceeded");
        com.skyflow.generated.rest.core.ApiClientApiException apiEx =
                new com.skyflow.generated.rest.core.ApiClientApiException("TooManyRequests", 429, body);
        Exception wrapper = new Exception("outer", apiEx);

        List<ErrorRecord> errors = Utils.handleDeleteTokensBatchException(wrapper, batch, 0, 2);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals("quota exceeded", errors.get(0).getError());
        Assert.assertEquals(429, errors.get(0).getCode());
    }

    // ── handleTokenizeBatchException — rest exception + null body ─────────────

    @Test
    public void handleTokenizeBatchException_restException_nullBody_createsOneErrorPerDataItem() {
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.builder()
                        .vaultId("v1")
                        .data(Arrays.asList(
                                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("v1").build(),
                                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("v2").build()))
                        .build();
        com.skyflow.generated.rest.core.ApiClientApiException apiEx =
                new com.skyflow.generated.rest.core.ApiClientApiException("Gateway timeout", 504, null);
        Exception wrapper = new Exception("outer", apiEx);

        List<ErrorRecord> errors = Utils.handleTokenizeBatchException(wrapper, batch, 1, 2);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals(2, errors.get(0).getIndex()); // batchNumber(1) * batchSize(2) = 2
        Assert.assertEquals(3, errors.get(1).getIndex());
        Assert.assertEquals(504, errors.get(0).getCode());
    }

    @Test
    public void handleTokenizeBatchException_restException_stringBody_doesNotThrow() {
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.builder()
                        .vaultId("v1")
                        .data(Collections.singletonList(
                                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("val").build()))
                        .build();
        com.skyflow.generated.rest.core.ApiClientApiException apiEx =
                new com.skyflow.generated.rest.core.ApiClientApiException("Unauthorized", 401, "raw string");
        Exception wrapper = new Exception("outer", apiEx);

        List<ErrorRecord> errors = Utils.handleTokenizeBatchException(wrapper, batch, 0, 1);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(401, errors.get(0).getCode());
    }

    @Test
    public void handleTokenizeBatchException_restException_errorFieldIsString_usesStringAsMessage() {
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.builder()
                        .vaultId("v1")
                        .data(Arrays.asList(
                                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("a").build(),
                                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("b").build()))
                        .build();
        Map<String, Object> body = new HashMap<>();
        body.put("error", "vault is locked");
        com.skyflow.generated.rest.core.ApiClientApiException apiEx =
                new com.skyflow.generated.rest.core.ApiClientApiException("Locked", 423, body);
        Exception wrapper = new Exception("outer", apiEx);

        List<ErrorRecord> errors = Utils.handleTokenizeBatchException(wrapper, batch, 0, 2);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals("vault is locked", errors.get(0).getError());
        Assert.assertEquals(423, errors.get(0).getCode());
    }

    // ── formatDeleteTokensResponse — absent tokens Optional ───────────────────

    @Test
    public void formatDeleteTokensResponse_absentTokensOptional_returnsNull() {
        com.skyflow.generated.rest.types.V1FlowDeleteTokenResponse response =
                com.skyflow.generated.rest.types.V1FlowDeleteTokenResponse.builder().build();

        DeleteTokensResponse result = Utils.formatDeleteTokensResponse(response, 0, 10);

        Assert.assertNull(result);
    }

    @Test
    public void formatDeleteTokensResponse_nullResponse_returnsNull() {
        Assert.assertNull(Utils.formatDeleteTokensResponse(null, 0, 10));
    }

    // ── formatDetokenizeResponse — absent response Optional ───────────────────

    @Test
    public void formatDetokenizeResponse_absentResponseOptional_returnsNull() {
        com.skyflow.generated.rest.types.V1FlowDetokenizeResponse response =
                com.skyflow.generated.rest.types.V1FlowDetokenizeResponse.builder().build();

        DetokenizeResponse result = Utils.formatDetokenizeResponse(response, 0, 10);

        Assert.assertNull(result);
    }

    // ── formatTokenizeResponse — null response ────────────────────────────────

    @Test
    public void formatTokenizeResponse_nullResponse_returnsNull() {
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batchRequest =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.builder()
                        .vaultId("v1")
                        .data(Collections.singletonList(
                                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("v").build()))
                        .build();

        TokenizeResponse result = Utils.formatTokenizeResponse(null, batchRequest, 0, 1);

        Assert.assertNull(result);
    }

    @Test
    public void formatTokenizeResponse_responseOptionalAbsent_returnsNull() {
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batchRequest =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.builder()
                        .vaultId("v1")
                        .data(Collections.singletonList(
                                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("v").build()))
                        .build();
        com.skyflow.generated.rest.types.V1FlowTokenizeResponse response =
                com.skyflow.generated.rest.types.V1FlowTokenizeResponse.builder().build();

        TokenizeResponse result = Utils.formatTokenizeResponse(response, batchRequest, 0, 1);

        Assert.assertNull(result);
    }

    private DetokenizeResponseObject createResponseObject(String token, String value, String groupName, String error, Integer httpCode) {
        DetokenizeResponseObject responseObject = new DetokenizeResponseObject(
                0,
                String.valueOf(Optional.ofNullable(token)),
                Optional.ofNullable(value),
                String.valueOf(Optional.ofNullable(groupName)),
                String.valueOf(Optional.ofNullable(error)),
                null
        );
        return responseObject;
    }

    private static Response buildOkHttpResponse(int code, String requestId) {
        Request request = new Request.Builder().url("https://example.com").build();
        Response.Builder builder = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("Test");
        if (requestId != null) {
            builder.header("x-request-id", requestId);
        }
        return builder.build();
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
        List<V1InsertRecordData> batch = Arrays.asList(V1InsertRecordData.builder().build());
        ApiClientApiException apiException = new ApiClientApiException("Error", 500, null);
        Exception exception = new Exception("Test", apiException);

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0, 1);
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("Test", errors.get(0).getError());
        Assert.assertEquals(500, errors.get(0).getCode());
    }

    @Test
    public void testHandleBatchExceptionWithNonListRecords() {
        List<V1InsertRecordData> batch = Arrays.asList(V1InsertRecordData.builder().build());
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("records", "not_a_list");
        ApiClientApiException apiException = new ApiClientApiException("Error", 500, responseBody);
        Exception exception = new Exception("Test", apiException);

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0, 1);
        Assert.assertEquals(1, errors.size());
    }

    @Test
    public void testHandleBatchExceptionWithErrorNotMap() {
        List<V1InsertRecordData> batch = Arrays.asList(V1InsertRecordData.builder().build());
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", "not_a_map");
        ApiClientApiException apiException = new ApiClientApiException("Error", 500, responseBody);
        Exception exception = new Exception("Test", apiException);

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0, 1);
        Assert.assertEquals(1, errors.size());
    }

    @Test
    public void testHandleBatchExceptionWithApiClientApiExceptionCause() {
        List<V1InsertRecordData> batch = Arrays.asList(V1InsertRecordData.builder().build());
        Map<String, Object> responseBody = new HashMap<>();
        Map<String, Object> error = new HashMap<>();
        error.put("message", "Test error");
        error.put("http_code", 400);
        responseBody.put("error", error);
        responseBody.put("http_code", 400);

        com.skyflow.generated.rest.core.ApiClientApiException apiException = new com.skyflow.generated.rest.core.ApiClientApiException("Error", 400, responseBody);
        Exception exception = new Exception("Outer exception", apiException);

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0, 1);
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("Test error", errors.get(0).getError());
        Assert.assertEquals(400, errors.get(0).getCode());
    }

    @Test
    public void testHandleBatchExceptionWithRecordsListUsesCreateErrorRecord() {
        List<V1InsertRecordData> batch = Arrays.asList(
                V1InsertRecordData.builder().build(),
                V1InsertRecordData.builder().build()
        );

        Map<String, Object> record1 = new HashMap<>();
        record1.put("error", "Err1");
        record1.put("http_code", 401);
        Map<String, Object> record2 = new HashMap<>();
        record2.put("message", "Err2");
        record2.put("statusCode", 402);
        List<Map<String, Object>> records = Arrays.asList(record1, record2);

        Map<String, Object> response = new HashMap<>();
        response.put("records", records);

        Throwable cause = new com.skyflow.generated.rest.core.ApiClientApiException("Error", 400, response);
        Exception exception = new Exception("Outer", cause);

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0, 2);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals("Err1", errors.get(0).getError());
        Assert.assertEquals(401, errors.get(0).getCode());
        Assert.assertEquals("Err2", errors.get(1).getError());
        Assert.assertEquals(402, errors.get(1).getCode());
    }

    @Test
    public void testHandleDetokenizeBatchExceptionWithResponseList() {
        List<String> tokens = Arrays.asList("t1", "t2");
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.builder()
                        .tokens(tokens)
                        .vaultId("vault")
                        .build();

        Map<String, Object> record1 = new HashMap<>();
        record1.put("error", "A");
        record1.put("http_code", 400);
        Map<String, Object> record2 = new HashMap<>();
        record2.put("message", "B");
        record2.put("statusCode", 401);
        List<Map<String, Object>> responseList = Arrays.asList(record1, record2);
        Map<String, Object> response = new HashMap<>();
        response.put("response", responseList);

        Throwable cause = new com.skyflow.generated.rest.core.ApiClientApiException("Error", 400, response);
        Exception exception = new Exception("Outer", cause);

        List<ErrorRecord> errors = Utils.handleDetokenizeBatchException(exception, batch, 0, 2);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals("A", errors.get(0).getError());
        Assert.assertEquals(400, errors.get(0).getCode());
        Assert.assertEquals("B", errors.get(1).getError());
        Assert.assertEquals(401, errors.get(1).getCode());
    }

    @Test
    public void testHandleDetokenizeBatchExceptionWithErrorField() {
        List<String> tokens = Arrays.asList("t1", "t2");
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.builder()
                        .tokens(tokens)
                        .vaultId("vault")
                        .build();

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("error", "all bad");
        errorBody.put("http_code", 403);
        Map<String, Object> response = new HashMap<>();
        response.put("error", errorBody);

        Throwable cause = new com.skyflow.generated.rest.core.ApiClientApiException("Error", 403, response);
        Exception exception = new Exception("Outer", cause);

        List<ErrorRecord> errors = Utils.handleDetokenizeBatchException(exception, batch, 1, 2);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals(2, errors.get(0).getIndex());
        Assert.assertEquals("all bad", errors.get(0).getError());
        Assert.assertEquals(403, errors.get(0).getCode());
    }

    @Test
    public void testHandleDetokenizeBatchExceptionWithNonApiCause() {
        List<String> tokens = Arrays.asList("t1", "t2");
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.builder()
                        .tokens(tokens)
                        .vaultId("vault")
                        .build();

        RuntimeException exception = new RuntimeException("plain failure");

        List<ErrorRecord> errors = Utils.handleDetokenizeBatchException(exception, batch, 0, 2);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals("plain failure", errors.get(0).getError());
        Assert.assertEquals(500, errors.get(0).getCode());
    }

    @Test
    public void testIsValidURLVariants() {
        Assert.assertTrue(Utils.isValidURL("https://example.com"));
        Assert.assertFalse(Utils.isValidURL("http://example.com"));
        Assert.assertFalse(Utils.isValidURL("https://"));
    }

    @Test
    public void testGetEnvVaultURLWithValidEnv() {
        Assume.assumeTrue(System.getenv("VAULT_URL") == null);

        String userDir = System.getProperty("user.dir");
        Path envPath = Paths.get(userDir, ".env");
        byte[] original = null;
        boolean existed = Files.exists(envPath);
        try {
            if (existed) {
                original = Files.readAllBytes(envPath);
            }
            Files.write(envPath,
                    Collections.singletonList("VAULT_URL=https://vault.example.com"),
                    StandardCharsets.UTF_8);

            String url = Utils.getEnvVaultURL();
            Assert.assertEquals("https://vault.example.com", url);
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        } finally {
            try {
                if (existed) {
                    Files.write(envPath, original);
                } else {
                    Files.deleteIfExists(envPath);
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    public void testGetEnvVaultURLEmpty() {
        Assume.assumeTrue(System.getenv("VAULT_URL") == null);

        String userDir = System.getProperty("user.dir");
        Path envPath = Paths.get(userDir, ".env");
        byte[] original = null;
        boolean existed = Files.exists(envPath);
        try {
            if (existed) {
                original = Files.readAllBytes(envPath);
            }
            Files.write(envPath,
                    Collections.singletonList("VAULT_URL=   "),
                    StandardCharsets.UTF_8);

            Utils.getEnvVaultURL();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyVaultUrl.getMessage(), e.getMessage());
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        } finally {
            try {
                if (existed) {
                    Files.write(envPath, original);
                } else {
                    Files.deleteIfExists(envPath);
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    public void testGetEnvVaultURLInvalidFormat() {
        Assume.assumeTrue(System.getenv("VAULT_URL") == null);

        String userDir = System.getProperty("user.dir");
        Path envPath = Paths.get(userDir, ".env");
        byte[] original = null;
        boolean existed = Files.exists(envPath);
        try {
            if (existed) {
                original = Files.readAllBytes(envPath);
            }
            Files.write(envPath,
                    Collections.singletonList("VAULT_URL=http://bad.example.com"),
                    StandardCharsets.UTF_8);

            Utils.getEnvVaultURL();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.InvalidVaultUrlFormat.getMessage(), e.getMessage());
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        } finally {
            try {
                if (existed) {
                    Files.write(envPath, original);
                } else {
                    Files.deleteIfExists(envPath);
                }
            } catch (Exception ignored) {
            }
        }
    }

    // ── createErrorRecord with requestId ─────────────────────────────────────

    @Test
    public void testCreateErrorRecordWithRequestId() {
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("error", "Unauthorized");
        recordMap.put("http_code", 401);

        ErrorRecord err = Utils.createErrorRecord(recordMap, 2, "req-id-abc");

        Assert.assertEquals(2, err.getIndex());
        Assert.assertEquals("Unauthorized", err.getError());
        Assert.assertEquals(401, err.getCode());
        Assert.assertEquals("req-id-abc", err.getRequestId());
    }

    @Test
    public void testCreateErrorRecordLegacyOverload_requestIdIsNull() {
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("message", "Server error");
        recordMap.put("http_code", 500);

        ErrorRecord err = Utils.createErrorRecord(recordMap, 0);

        Assert.assertNull(err.getRequestId());
    }

    @Test
    public void testCreateErrorRecordWithNullRequestId() {
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("error", "Not found");
        recordMap.put("http_code", 404);

        ErrorRecord err = Utils.createErrorRecord(recordMap, 1, null);

        Assert.assertNull(err.getRequestId());
        Assert.assertEquals("Not found", err.getError());
    }

    // ── handleBatchException with x-request-id ────────────────────────────────

    @Test
    public void testHandleBatchExceptionRequestIdExtractedFromHeaders() {
        List<V1InsertRecordData> batch = Collections.singletonList(V1InsertRecordData.builder().build());
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("message", "Unauthorized");
        errorMap.put("http_code", 401);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", errorMap);

        Response okResponse = buildOkHttpResponse(401, "insert-req-id-123");
        com.skyflow.generated.rest.core.ApiClientApiException apiException =
                new com.skyflow.generated.rest.core.ApiClientApiException("Unauthorized", 401, responseBody, okResponse);
        Exception exception = new Exception("Outer", apiException);

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0, 1);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("Unauthorized", errors.get(0).getError());
        Assert.assertEquals(401, errors.get(0).getCode());
        Assert.assertEquals("insert-req-id-123", errors.get(0).getRequestId());
    }

    @Test
    public void testHandleBatchExceptionWithRecordsList_requestIdPropagated() {
        List<V1InsertRecordData> batch = Arrays.asList(
                V1InsertRecordData.builder().build(), V1InsertRecordData.builder().build());
        Map<String, Object> rec1 = new HashMap<>();
        rec1.put("error", "Err1");
        rec1.put("http_code", 400);
        Map<String, Object> rec2 = new HashMap<>();
        rec2.put("error", "Err2");
        rec2.put("http_code", 422);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("records", Arrays.asList(rec1, rec2));

        Response okResponse = buildOkHttpResponse(400, "batch-req-id-456");
        com.skyflow.generated.rest.core.ApiClientApiException apiException =
                new com.skyflow.generated.rest.core.ApiClientApiException("Error", 400, responseBody, okResponse);
        Exception exception = new Exception("Outer", apiException);

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0, 2);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals("batch-req-id-456", errors.get(0).getRequestId());
        Assert.assertEquals("batch-req-id-456", errors.get(1).getRequestId());
    }

    @Test
    public void testHandleBatchExceptionNoRequestIdHeader_requestIdIsNull() {
        List<V1InsertRecordData> batch = Collections.singletonList(V1InsertRecordData.builder().build());
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("message", "Forbidden");
        errorMap.put("http_code", 403);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", errorMap);

        Response okResponse = buildOkHttpResponse(403, null);
        com.skyflow.generated.rest.core.ApiClientApiException apiException =
                new com.skyflow.generated.rest.core.ApiClientApiException("Forbidden", 403, responseBody, okResponse);
        Exception exception = new Exception("Outer", apiException);

        List<ErrorRecord> errors = Utils.handleBatchException(exception, batch, 0, 1);

        Assert.assertEquals(1, errors.size());
        Assert.assertNull(errors.get(0).getRequestId());
    }

    // ── handleDetokenizeBatchException with x-request-id ─────────────────────

    @Test
    public void testHandleDetokenizeBatchExceptionRequestIdExtracted() {
        List<String> tokens = Arrays.asList("t1", "t2");
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.builder()
                        .tokens(tokens).vaultId("v1").build();

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("error", "invalid token");
        errorBody.put("http_code", 400);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", errorBody);

        Response okResponse = buildOkHttpResponse(400, "detok-req-id-789");
        com.skyflow.generated.rest.core.ApiClientApiException apiException =
                new com.skyflow.generated.rest.core.ApiClientApiException("Error", 400, responseBody, okResponse);
        Exception exception = new Exception("Outer", apiException);

        List<ErrorRecord> errors = Utils.handleDetokenizeBatchException(exception, batch, 0, 2);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals("detok-req-id-789", errors.get(0).getRequestId());
        Assert.assertEquals("detok-req-id-789", errors.get(1).getRequestId());
    }

    @Test
    public void testHandleDetokenizeBatchExceptionNoRequestIdHeader_isNull() {
        List<String> tokens = Collections.singletonList("t1");
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.builder()
                        .tokens(tokens).vaultId("v1").build();

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("error", "invalid token");
        errorBody.put("http_code", 400);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", errorBody);

        Response okResponse = buildOkHttpResponse(400, null);
        com.skyflow.generated.rest.core.ApiClientApiException apiException =
                new com.skyflow.generated.rest.core.ApiClientApiException("Error", 400, responseBody, okResponse);
        Exception exception = new Exception("Outer", apiException);

        List<ErrorRecord> errors = Utils.handleDetokenizeBatchException(exception, batch, 0, 1);

        Assert.assertEquals(1, errors.size());
        Assert.assertNull(errors.get(0).getRequestId());
    }

    // ── handleDeleteTokensBatchException ─────────────────────────────────────

    @Test
    public void testHandleDeleteTokensBatchExceptionWithTokensList() {
        List<String> tokens = Arrays.asList("tok1", "tok2");
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest.builder()
                        .tokens(tokens).vaultId("v1").build();

        Map<String, Object> rec1 = new HashMap<>();
        rec1.put("error", "Token expired");
        rec1.put("http_code", 400);
        Map<String, Object> rec2 = new HashMap<>();
        rec2.put("message", "Token not found");
        rec2.put("statusCode", 404);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("tokens", Arrays.asList(rec1, rec2));

        com.skyflow.generated.rest.core.ApiClientApiException apiException =
                new com.skyflow.generated.rest.core.ApiClientApiException("Error", 400, responseBody);
        Exception exception = new Exception("Outer", apiException);

        List<ErrorRecord> errors = Utils.handleDeleteTokensBatchException(exception, batch, 0, 2);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals("Token expired", errors.get(0).getError());
        Assert.assertEquals(400, errors.get(0).getCode());
        Assert.assertEquals(0, errors.get(0).getIndex());
        Assert.assertEquals("Token not found", errors.get(1).getError());
        Assert.assertEquals(404, errors.get(1).getCode());
    }

    @Test
    public void testHandleDeleteTokensBatchExceptionWithErrorField() {
        List<String> tokens = Arrays.asList("tok1", "tok2");
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest.builder()
                        .tokens(tokens).vaultId("v1").build();

        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("error", "Unauthorized");
        errorMap.put("http_code", 401);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", errorMap);

        com.skyflow.generated.rest.core.ApiClientApiException apiException =
                new com.skyflow.generated.rest.core.ApiClientApiException("Error", 401, responseBody);
        Exception exception = new Exception("Outer", apiException);

        List<ErrorRecord> errors = Utils.handleDeleteTokensBatchException(exception, batch, 1, 2);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals(2, errors.get(0).getIndex());
        Assert.assertEquals("Unauthorized", errors.get(0).getError());
        Assert.assertEquals(401, errors.get(0).getCode());
    }

    @Test
    public void testHandleDeleteTokensBatchExceptionWithNonApiCause() {
        List<String> tokens = Arrays.asList("tok1", "tok2");
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest.builder()
                        .tokens(tokens).vaultId("v1").build();

        RuntimeException exception = new RuntimeException("network failure");

        List<ErrorRecord> errors = Utils.handleDeleteTokensBatchException(exception, batch, 0, 2);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals("network failure", errors.get(0).getError());
        Assert.assertEquals(500, errors.get(0).getCode());
        Assert.assertEquals(0, errors.get(0).getIndex());
        Assert.assertEquals(1, errors.get(1).getIndex());
    }

    @Test
    public void testHandleDeleteTokensBatchExceptionRequestIdExtracted() {
        List<String> tokens = Collections.singletonList("tok1");
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest.builder()
                        .tokens(tokens).vaultId("v1").build();

        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("error", "Forbidden");
        errorMap.put("http_code", 403);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", errorMap);

        Response okResponse = buildOkHttpResponse(403, "del-req-id-111");
        com.skyflow.generated.rest.core.ApiClientApiException apiException =
                new com.skyflow.generated.rest.core.ApiClientApiException("Error", 403, responseBody, okResponse);
        Exception exception = new Exception("Outer", apiException);

        List<ErrorRecord> errors = Utils.handleDeleteTokensBatchException(exception, batch, 0, 1);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("del-req-id-111", errors.get(0).getRequestId());
    }

    // ── handleTokenizeBatchException ─────────────────────────────────────────

    @Test
    public void testHandleTokenizeBatchExceptionWithResponseList() {
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.builder()
                        .vaultId("v1")
                        .data(Arrays.asList(
                                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("val1").build(),
                                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("val2").build()))
                        .build();

        Map<String, Object> rec1 = new HashMap<>();
        rec1.put("error", "Error A");
        rec1.put("http_code", 400);
        Map<String, Object> rec2 = new HashMap<>();
        rec2.put("message", "Error B");
        rec2.put("statusCode", 422);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("response", Arrays.asList(rec1, rec2));

        com.skyflow.generated.rest.core.ApiClientApiException apiException =
                new com.skyflow.generated.rest.core.ApiClientApiException("Error", 400, responseBody);
        Exception exception = new Exception("Outer", apiException);

        List<ErrorRecord> errors = Utils.handleTokenizeBatchException(exception, batch, 0, 2);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals("Error A", errors.get(0).getError());
        Assert.assertEquals(400, errors.get(0).getCode());
        Assert.assertEquals("Error B", errors.get(1).getError());
        Assert.assertEquals(422, errors.get(1).getCode());
    }

    @Test
    public void testHandleTokenizeBatchExceptionWithErrorField() {
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.builder()
                        .vaultId("v1")
                        .data(Arrays.asList(
                                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("v1").build(),
                                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("v2").build()))
                        .build();

        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("error", "Unauthorized");
        errorMap.put("http_code", 401);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", errorMap);

        com.skyflow.generated.rest.core.ApiClientApiException apiException =
                new com.skyflow.generated.rest.core.ApiClientApiException("Error", 401, responseBody);
        Exception exception = new Exception("Outer", apiException);

        List<ErrorRecord> errors = Utils.handleTokenizeBatchException(exception, batch, 1, 2);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals(2, errors.get(0).getIndex());
        Assert.assertEquals("Unauthorized", errors.get(0).getError());
    }

    @Test
    public void testHandleTokenizeBatchExceptionWithNonApiCause() {
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.builder()
                        .vaultId("v1")
                        .data(Collections.singletonList(
                                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("val").build()))
                        .build();

        RuntimeException exception = new RuntimeException("connection refused");

        List<ErrorRecord> errors = Utils.handleTokenizeBatchException(exception, batch, 0, 1);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("connection refused", errors.get(0).getError());
        Assert.assertEquals(500, errors.get(0).getCode());
        Assert.assertNull(errors.get(0).getRequestId());
    }

    @Test
    public void testHandleTokenizeBatchExceptionRequestIdExtracted() {
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.builder()
                        .vaultId("v1")
                        .data(Collections.singletonList(
                                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("val").build()))
                        .build();

        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("error", "Quota exceeded");
        errorMap.put("http_code", 429);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", errorMap);

        Response okResponse = buildOkHttpResponse(429, "tok-req-id-222");
        com.skyflow.generated.rest.core.ApiClientApiException apiException =
                new com.skyflow.generated.rest.core.ApiClientApiException("Error", 429, responseBody, okResponse);
        Exception exception = new Exception("Outer", apiException);

        List<ErrorRecord> errors = Utils.handleTokenizeBatchException(exception, batch, 0, 1);

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("tok-req-id-222", errors.get(0).getRequestId());
    }

    // ── formatResponse with headers ───────────────────────────────────────────

    @Test
    public void testFormatResponseWithRequestIdFromHeaders() {
        V1RecordResponseObject errorRecord = V1RecordResponseObject.builder()
                .error(Optional.of("Duplicate record"))
                .httpCode(Optional.of(409))
                .build();
        V1InsertResponse response = V1InsertResponse.builder()
                .records(Optional.of(Collections.singletonList(errorRecord)))
                .build();

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("x-request-id", Collections.singletonList("ins-req-id-333"));

        com.skyflow.vault.data.InsertResponse result = Utils.formatResponse(response, 0, 1, headers);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.getErrors().size());
        Assert.assertEquals("Duplicate record", result.getErrors().get(0).getError());
        Assert.assertEquals("ins-req-id-333", result.getErrors().get(0).getRequestId());
    }

    @Test
    public void testFormatResponseNullHeaders_requestIdIsNull() {
        V1RecordResponseObject errorRecord = V1RecordResponseObject.builder()
                .error(Optional.of("Not found"))
                .httpCode(Optional.of(404))
                .build();
        V1InsertResponse response = V1InsertResponse.builder()
                .records(Optional.of(Collections.singletonList(errorRecord)))
                .build();

        com.skyflow.vault.data.InsertResponse result = Utils.formatResponse(response, 0, 1, null);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.getErrors().size());
        Assert.assertNull(result.getErrors().get(0).getRequestId());
    }

    // ── formatDetokenizeResponse with headers ─────────────────────────────────

    @Test
    public void testFormatDetokenizeResponseWithRequestIdFromHeaders() {
        com.skyflow.generated.rest.types.V1FlowDetokenizeResponseObject errorObj =
                com.skyflow.generated.rest.types.V1FlowDetokenizeResponseObject.builder()
                        .error("Token invalid").httpCode(400).build();
        com.skyflow.generated.rest.types.V1FlowDetokenizeResponse response =
                com.skyflow.generated.rest.types.V1FlowDetokenizeResponse.builder()
                        .response(Optional.of(Collections.singletonList(errorObj))).build();

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("x-request-id", Collections.singletonList("det-req-id-444"));

        DetokenizeResponse result = Utils.formatDetokenizeResponse(response, 0, 1, headers);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.getErrors().size());
        Assert.assertEquals("Token invalid", result.getErrors().get(0).getError());
        Assert.assertEquals("det-req-id-444", result.getErrors().get(0).getRequestId());
    }

    @Test
    public void testFormatDetokenizeResponseNullHeaders_requestIdIsNull() {
        com.skyflow.generated.rest.types.V1FlowDetokenizeResponseObject errorObj =
                com.skyflow.generated.rest.types.V1FlowDetokenizeResponseObject.builder()
                        .error("Token invalid").httpCode(400).build();
        com.skyflow.generated.rest.types.V1FlowDetokenizeResponse response =
                com.skyflow.generated.rest.types.V1FlowDetokenizeResponse.builder()
                        .response(Optional.of(Collections.singletonList(errorObj))).build();

        DetokenizeResponse result = Utils.formatDetokenizeResponse(response, 0, 1, null);

        Assert.assertNotNull(result);
        Assert.assertNull(result.getErrors().get(0).getRequestId());
    }

    // ── formatDeleteTokensResponse with headers ───────────────────────────────

    @Test
    public void testFormatDeleteTokensResponseWithRequestIdFromHeaders() {
        V1DeleteTokenResponseObject errorRecord = V1DeleteTokenResponseObject.builder()
                .value("tok-abc")
                .error("Token expired")
                .httpCode(400)
                .build();
        V1FlowDeleteTokenResponse response = V1FlowDeleteTokenResponse.builder()
                .tokens(Collections.singletonList(errorRecord))
                .build();

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("x-request-id", Collections.singletonList("del-req-id-555"));

        DeleteTokensResponse result = Utils.formatDeleteTokensResponse(response, 0, 1, headers);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.getErrors().size());
        Assert.assertEquals("Token expired", result.getErrors().get(0).getError());
        Assert.assertEquals("del-req-id-555", result.getErrors().get(0).getRequestId());
    }

    @Test
    public void testFormatDeleteTokensResponseNullHeaders_requestIdIsNull() {
        V1DeleteTokenResponseObject errorRecord = V1DeleteTokenResponseObject.builder()
                .value("tok-abc")
                .error("Token expired")
                .httpCode(400)
                .build();
        V1FlowDeleteTokenResponse response = V1FlowDeleteTokenResponse.builder()
                .tokens(Collections.singletonList(errorRecord))
                .build();

        DeleteTokensResponse result = Utils.formatDeleteTokensResponse(response, 0, 1, null);

        Assert.assertNotNull(result);
        Assert.assertNull(result.getErrors().get(0).getRequestId());
    }

    @Test
    public void testFormatDeleteTokensResponseSuccessRecord_noRequestId() {
        V1DeleteTokenResponseObject successRecord = V1DeleteTokenResponseObject.builder()
                .value("tok-success")
                .build();
        V1FlowDeleteTokenResponse response = V1FlowDeleteTokenResponse.builder()
                .tokens(Collections.singletonList(successRecord))
                .build();

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("x-request-id", Collections.singletonList("req-id-xyz"));

        DeleteTokensResponse result = Utils.formatDeleteTokensResponse(response, 0, 1, headers);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.getSuccess().size());
        Assert.assertEquals(0, result.getErrors().size());
    }

    // ── formatTokenizeResponse with headers ───────────────────────────────────

    @Test
    public void testFormatTokenizeResponseWithRequestIdFromHeaders() throws Exception {
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batchRequest =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.builder()
                        .vaultId("v1")
                        .data(Collections.singletonList(
                                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder()
                                        .value("sensitive").build()))
                        .build();

        String json = "{\"error\":\"Tokenize failed\",\"httpCode\":403}";
        V1FlowTokenizeResponseObject errorObj = new ObjectMapper()
                .readValue(json, V1FlowTokenizeResponseObject.class);

        com.skyflow.generated.rest.types.V1FlowTokenizeResponse response =
                com.skyflow.generated.rest.types.V1FlowTokenizeResponse.builder()
                        .response(Optional.of(Collections.singletonList(errorObj))).build();

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("x-request-id", Collections.singletonList("tok-req-id-666"));

        TokenizeResponse result = Utils.formatTokenizeResponse(response, batchRequest, 0, 1, headers);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.getErrors().size());
        Assert.assertEquals("Tokenize failed", result.getErrors().get(0).getError());
        Assert.assertEquals("tok-req-id-666", result.getErrors().get(0).getRequestId());
    }

    @Test
    public void testFormatTokenizeResponseNullHeaders_requestIdIsNull() throws Exception {
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batchRequest =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.builder()
                        .vaultId("v1")
                        .data(Collections.singletonList(
                                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder()
                                        .value("data").build()))
                        .build();

        String json = "{\"error\":\"Quota exceeded\",\"httpCode\":429}";
        V1FlowTokenizeResponseObject errorObj = new ObjectMapper()
                .readValue(json, V1FlowTokenizeResponseObject.class);

        com.skyflow.generated.rest.types.V1FlowTokenizeResponse response =
                com.skyflow.generated.rest.types.V1FlowTokenizeResponse.builder()
                        .response(Optional.of(Collections.singletonList(errorObj))).build();

        TokenizeResponse result = Utils.formatTokenizeResponse(response, batchRequest, 0, 1, null);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.getErrors().size());
        Assert.assertNull(result.getErrors().get(0).getRequestId());
    }

    // ── formatTokenizeResponse — success path ────────────────────────────────

    @Test
    public void testFormatTokenizeResponseSuccessPath_returnsSuccessRecord() throws Exception {
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batchRequest =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.builder()
                        .vaultId("v1")
                        .data(Collections.singletonList(
                                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder()
                                        .value("sensitive-value").build()))
                        .build();

        // No "error" key → success path
        String json = "{\"token\":\"tok-abc\"}";
        V1FlowTokenizeResponseObject successObj = new ObjectMapper()
                .readValue(json, V1FlowTokenizeResponseObject.class);

        com.skyflow.generated.rest.types.V1FlowTokenizeResponse response =
                com.skyflow.generated.rest.types.V1FlowTokenizeResponse.builder()
                        .response(Optional.of(Collections.singletonList(successObj))).build();

        TokenizeResponse result = Utils.formatTokenizeResponse(response, batchRequest, 0, 1, null);

        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getErrors().size());
        Assert.assertEquals(1, result.getSuccess().size());
        Assert.assertEquals(0, result.getSuccess().get(0).getIndex());
        Assert.assertEquals("tok-abc", result.getSuccess().get(0).getTokens().get(null));
    }

    @Test
    public void testFormatTokenizeResponseWithTokenGroupNames_multiGroupConsumed() throws Exception {
        // Request with one data item having 2 tokenGroupNames → consumes 2 response entries
        com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject reqObj =
                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder()
                        .value("sensitive")
                        .tokenGroupNames(Arrays.asList("group1", "group2"))
                        .build();
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batchRequest =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.builder()
                        .vaultId("v1")
                        .data(Collections.singletonList(reqObj))
                        .build();

        V1FlowTokenizeResponseObject resp1 = new ObjectMapper()
                .readValue("{\"token\":\"tok1\",\"tokenGroupName\":\"group1\"}", V1FlowTokenizeResponseObject.class);
        V1FlowTokenizeResponseObject resp2 = new ObjectMapper()
                .readValue("{\"token\":\"tok2\",\"tokenGroupName\":\"group2\"}", V1FlowTokenizeResponseObject.class);

        com.skyflow.generated.rest.types.V1FlowTokenizeResponse response =
                com.skyflow.generated.rest.types.V1FlowTokenizeResponse.builder()
                        .response(Optional.of(Arrays.asList(resp1, resp2))).build();

        TokenizeResponse result = Utils.formatTokenizeResponse(response, batchRequest, 0, 1, null);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.getSuccess().size());  // 1 input record → 1 success entry
        Assert.assertEquals(0, result.getErrors().size());
        Map<String, String> tokens = result.getSuccess().get(0).getTokens();
        Assert.assertEquals("tok1", tokens.get("group1"));
        Assert.assertEquals("tok2", tokens.get("group2"));
    }

    @Test
    public void testFormatTokenizeResponseAbsentBatchData_returnsEmptySuccessAndErrors() throws Exception {
        // batchRequest.getData() is absent → requestData defaults to empty list
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batchRequest =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.builder()
                        .vaultId("v1")
                        .build();  // no data set

        String json = "{\"token\":\"tok-abc\"}";
        V1FlowTokenizeResponseObject obj = new ObjectMapper()
                .readValue(json, V1FlowTokenizeResponseObject.class);

        com.skyflow.generated.rest.types.V1FlowTokenizeResponse response =
                com.skyflow.generated.rest.types.V1FlowTokenizeResponse.builder()
                        .response(Optional.of(Collections.singletonList(obj))).build();

        TokenizeResponse result = Utils.formatTokenizeResponse(response, batchRequest, 0, 1, null);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.getSuccess().isEmpty());
        Assert.assertTrue(result.getErrors().isEmpty());
    }

    // ── SK-3002: client-side timeout is classified as 408, not a generic 500 ──

    private static Throwable timeoutFailure() {
        // Mirrors the real chain: OkHttp call-timeout (SocketTimeoutException, a
        // subclass of InterruptedIOException) -> wrapped by the generated client in
        // ApiClientException -> wrapped by CompletableFuture in CompletionException.
        return new java.util.concurrent.CompletionException(
                new com.skyflow.generated.rest.core.ApiClientException(
                        "Network error executing HTTP request",
                        new java.net.SocketTimeoutException("timeout")));
    }

    @Test
    public void handleBatchException_timeout_yields408AndCleanMessage() {
        List<V1InsertRecordData> batch = Arrays.asList(
                V1InsertRecordData.builder().build(),
                V1InsertRecordData.builder().build());

        List<ErrorRecord> errors = Utils.handleBatchException(timeoutFailure(), batch, 0, 1);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals("timeout maps to 408", 408, errors.get(0).getCode());
        Assert.assertEquals("Request timed out.", errors.get(0).getError());
    }

    @Test
    public void handleDetokenizeBatchException_timeout_yields408AndCleanMessage() {
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.builder()
                        .tokens(Arrays.asList("t1", "t2")).vaultId("v1").build();

        List<ErrorRecord> errors = Utils.handleDetokenizeBatchException(timeoutFailure(), batch, 0, 2);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals("timeout maps to 408", 408, errors.get(0).getCode());
        Assert.assertEquals("Request timed out.", errors.get(0).getError());
    }

    @Test
    public void handleDeleteTokensBatchException_timeout_yields408AndCleanMessage() {
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest.builder()
                        .tokens(Arrays.asList("tok1", "tok2")).vaultId("v1").build();

        List<ErrorRecord> errors = Utils.handleDeleteTokensBatchException(timeoutFailure(), batch, 0, 2);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals("timeout maps to 408", 408, errors.get(0).getCode());
        Assert.assertEquals("Request timed out.", errors.get(0).getError());
    }

    @Test
    public void handleTokenizeBatchException_timeout_yields408AndCleanMessage() {
        com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batch =
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.builder()
                        .vaultId("v1")
                        .data(Arrays.asList(
                                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("v1").build(),
                                com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject.builder().value("v2").build()))
                        .build();

        List<ErrorRecord> errors = Utils.handleTokenizeBatchException(timeoutFailure(), batch, 0, 2);

        Assert.assertEquals(2, errors.size());
        Assert.assertEquals("timeout maps to 408", 408, errors.get(0).getCode());
        Assert.assertEquals("Request timed out.", errors.get(0).getError());
    }
}