package com.skyflow.vault.controller;

import com.skyflow.Skyflow;
import com.skyflow.VaultClient;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.HttpStatus;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.core.ApiClientHttpResponse;
import com.skyflow.generated.rest.resources.query.QueryClient;
import com.skyflow.generated.rest.resources.records.RawRecordsClient;
import com.skyflow.generated.rest.resources.records.RecordsClient;
import com.skyflow.generated.rest.resources.tokens.RawTokensClient;
import com.skyflow.generated.rest.resources.tokens.TokensClient;
import com.skyflow.generated.rest.types.V1BatchOperationResponse;
import com.skyflow.generated.rest.types.V1BulkDeleteRecordResponse;
import com.skyflow.generated.rest.types.V1BulkGetRecordResponse;
import com.skyflow.generated.rest.types.V1DetokenizeRecordResponse;
import com.skyflow.generated.rest.types.V1DetokenizeResponse;
import com.skyflow.generated.rest.types.V1FieldRecords;
import com.skyflow.generated.rest.types.V1GetQueryResponse;
import com.skyflow.generated.rest.types.V1InsertRecordResponse;
import com.skyflow.generated.rest.types.V1RecordMetaProperties;
import com.skyflow.generated.rest.types.V1TokenizeRecordResponse;
import com.skyflow.generated.rest.types.V1TokenizeResponse;
import com.skyflow.generated.rest.types.V1UpdateRecordResponse;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.vault.data.DeleteRequest;
import com.skyflow.vault.data.DeleteResponse;
import com.skyflow.vault.data.FileUploadRequest;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.QueryResponse;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateResponse;
import com.skyflow.vault.tokens.ColumnValue;
import com.skyflow.vault.tokens.DetokenizeData;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.DetokenizeResponse;
import com.skyflow.vault.tokens.TokenizeRequest;
import com.skyflow.vault.tokens.TokenizeResponse;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class VaultControllerTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String vaultID = null;
    private static String clusterID = null;
    private static VaultConfig vaultConfig = null;
    private static Skyflow skyflowClient = null;

    @BeforeClass
    public static void setup() throws SkyflowException, NoSuchMethodException {
        vaultID = "vault123";
        clusterID = "cluster123";

        Credentials credentials = new Credentials();
        credentials.setToken("valid-token");

        vaultConfig = new VaultConfig();
        vaultConfig.setVaultId(vaultID);
        vaultConfig.setClusterId(clusterID);
        vaultConfig.setEnv(Env.DEV);
        vaultConfig.setCredentials(credentials);

        skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)
                .addVaultConfig(vaultConfig)
                .build();
    }

    // --- helpers ---

    private static VaultController createControllerWithMock(ApiClient mockApiClient) throws Exception {
        Credentials creds = new Credentials();
        creds.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");

        VaultConfig config = new VaultConfig();
        config.setVaultId(vaultID);
        config.setClusterId(clusterID);
        config.setEnv(Env.DEV);

        VaultController controller = new VaultController(config, creds);
        Field f = VaultClient.class.getDeclaredField("apiClient");
        f.setAccessible(true);
        f.set(controller, mockApiClient);
        return controller;
    }

    private static Response buildOkHttpResponse() {
        return new Response.Builder()
                .request(new Request.Builder().url("https://dummy.example.com").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header(Constants.REQUEST_ID_HEADER_KEY, "req-test-123")
                .build();
    }

    // --- validation failure tests (existing) ---

    @Test
    public void testInvalidRequestInInsertMethod() {
        try {
            InsertRequest request = InsertRequest.builder().build();
            skyflowClient.vault().insert(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TableKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testInvalidRequestInDetokenizeMethod() {
        try {
            DetokenizeRequest request = DetokenizeRequest.builder().build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().detokenize(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.InvalidDetokenizeData.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testInvalidRequestInGetMethod() {
        try {
            GetRequest request = GetRequest.builder().build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().get(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TableKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testInvalidRequestInUpdateMethod() {
        try {
            UpdateRequest request = UpdateRequest.builder().build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().update(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TableKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testInvalidRequestInDeleteMethod() {
        try {
            DeleteRequest request = DeleteRequest.builder().build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().delete(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TableKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testInvalidRequestInQueryMethod() {
        try {
            QueryRequest request = QueryRequest.builder().build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().query(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.QueryKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testInvalidRequestInTokenizeMethod() {
        try {
            TokenizeRequest request = TokenizeRequest.builder().build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().tokenize(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.ColumnValuesKeyErrorTokenize.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
            Assert.assertNull(e.getRequestId());
            Assert.assertNull(e.getGrpcCode());
            Assert.assertTrue(e.getDetails().isEmpty());
            Assert.assertEquals(HttpStatus.BAD_REQUEST.getHttpStatus(), e.getHttpStatus());
        }
    }

    @Test
    public void testInvalidRequestInFileUploadMethod() {
        try {
            FileUploadRequest request = FileUploadRequest.builder().build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().uploadFile(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TableKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
            Assert.assertNull(e.getRequestId());
            Assert.assertNull(e.getGrpcCode());
            Assert.assertTrue(e.getDetails().isEmpty());
            Assert.assertEquals(HttpStatus.BAD_REQUEST.getHttpStatus(), e.getHttpStatus());
        }
    }

    // --- getFormattedGetRecord / getFormattedQueryRecord tests ---

    @Test
    public void testGetFormattedGetRecordNormalisesSkyflowId() throws Exception {
        Map<String, Object> fields = new HashMap<>();
        fields.put("skyflow_id", "abc-123");
        fields.put("name", "John");
        V1FieldRecords record = V1FieldRecords.builder().fields(fields).build();

        Method method = VaultController.class.getDeclaredMethod("getFormattedGetRecord", V1FieldRecords.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        HashMap<String, Object> result = (HashMap<String, Object>) method.invoke(null, record);

        Assert.assertEquals("skyflowId should be present (new form)", "abc-123", result.get("skyflowId"));
        Assert.assertEquals("skyflow_id should still be present (v2 deprecated form)", "abc-123", result.get("skyflow_id"));
        Assert.assertEquals("other fields should be preserved", "John", result.get("name"));
    }

    @Test
    public void testGetFormattedQueryRecordNormalisesSkyflowId() throws Exception {
        Map<String, Object> fields = new HashMap<>();
        fields.put("skyflow_id", "xyz-456");
        fields.put("email", "test@example.com");
        V1FieldRecords record = V1FieldRecords.builder().fields(fields).build();

        Method method = VaultController.class.getDeclaredMethod("getFormattedQueryRecord", V1FieldRecords.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        HashMap<String, Object> result = (HashMap<String, Object>) method.invoke(null, record);

        Assert.assertEquals("skyflowId should be present (new form)", "xyz-456", result.get("skyflowId"));
        Assert.assertEquals("skyflow_id should still be present (v2 deprecated form)", "xyz-456", result.get("skyflow_id"));
        Assert.assertEquals("other fields should be preserved", "test@example.com", result.get("email"));
    }

    @Test
    public void testGetFormattedGetRecordNormalisesSkyflowIdInTokensBranch() throws Exception {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("skyflow_id", "tok-789");
        tokens.put("card_number", "tok-card-abc");
        V1FieldRecords record = V1FieldRecords.builder().tokens(tokens).build();

        Method method = VaultController.class.getDeclaredMethod("getFormattedGetRecord", V1FieldRecords.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        HashMap<String, Object> result = (HashMap<String, Object>) method.invoke(null, record);

        Assert.assertEquals("skyflowId should be present (new form)", "tok-789", result.get("skyflowId"));
        Assert.assertEquals("skyflow_id should still be present (v2 deprecated form)", "tok-789", result.get("skyflow_id"));
        Assert.assertEquals("other token fields should be preserved", "tok-card-abc", result.get("card_number"));
    }

    // --- downloadUrl tests ---

    @Test
    public void testGetRequestDownloadUrlNewForm() {
        GetRequest request = GetRequest.builder()
                .table("test_table")
                .downloadUrl(true)
                .build();
        Assert.assertTrue("new downloadUrl(true) should be set", request.getDownloadUrl());
    }

    @Test
    public void testGetRequestDownloadURLDeprecatedFormStillWorks() {
        GetRequest request = GetRequest.builder()
                .table("test_table")
                .downloadURL(true)
                .build();
        Assert.assertTrue("deprecated downloadURL() should still work", request.getDownloadURL());
        Assert.assertTrue("new getDownloadUrl() returns same value", request.getDownloadUrl());
    }

    @Test
    public void testGetRequestDownloadUrlDefaultIsTrue() {
        GetRequest request = GetRequest.builder()
                .table("test_table")
                .build();
        Assert.assertTrue("downloadUrl should be true by default (preserved from original)", request.getDownloadUrl());
    }

    @Test
    public void testDetokenizeRequestDownloadUrlNewForm() {
        DetokenizeRequest request = DetokenizeRequest.builder()
                .downloadUrl(true)
                .build();
        Assert.assertTrue("new downloadUrl(true) should be set", request.getDownloadUrl());
    }

    @Test
    public void testDetokenizeRequestDownloadURLDeprecatedFormStillWorks() {
        DetokenizeRequest request = DetokenizeRequest.builder()
                .downloadURL(true)
                .build();
        Assert.assertTrue("deprecated downloadURL() should still work", request.getDownloadURL());
        Assert.assertTrue("new getDownloadUrl() returns same value", request.getDownloadUrl());
    }

    @Test
    public void testDetokenizeRequestDownloadUrlDefaultIsFalse() {
        DetokenizeRequest request = DetokenizeRequest.builder().build();
        Assert.assertFalse("downloadUrl should be false by default", request.getDownloadUrl());
    }

    // --- extractUpdateSkyflowId tests ---

    @Test
    public void testExtractUpdateSkyflowId_onlyCamelCase() throws Exception {
        HashMap<String, Object> data = new HashMap<>();
        data.put("skyflowId", "id-camel-only");
        data.put("card_number", "4111111111111111");

        Method method = VaultController.class.getDeclaredMethod("extractUpdateSkyflowId", HashMap.class);
        method.setAccessible(true);
        String result = (String) method.invoke(null, data);

        Assert.assertEquals("should return the skyflowId value", "id-camel-only", result);
        Assert.assertFalse("skyflowId should be removed from data map", data.containsKey("skyflowId"));
        Assert.assertTrue("other fields should be preserved", data.containsKey("card_number"));
    }

    @Test
    public void testExtractUpdateSkyflowId_onlySnakeCase() throws Exception {
        HashMap<String, Object> data = new HashMap<>();
        data.put("skyflow_id", "id-snake-only");
        data.put("card_number", "4111111111111111");

        Method method = VaultController.class.getDeclaredMethod("extractUpdateSkyflowId", HashMap.class);
        method.setAccessible(true);
        String result = (String) method.invoke(null, data);

        Assert.assertEquals("should return the skyflow_id value", "id-snake-only", result);
        Assert.assertFalse("skyflow_id should be removed from data map", data.containsKey("skyflow_id"));
        Assert.assertTrue("other fields should be preserved", data.containsKey("card_number"));
    }

    @Test
    public void testExtractUpdateSkyflowId_bothKeys_prefersSkyflowId() throws Exception {
        HashMap<String, Object> data = new HashMap<>();
        data.put("skyflowId", "id-camel");
        data.put("skyflow_id", "id-snake");
        data.put("card_number", "4111111111111111");

        Method method = VaultController.class.getDeclaredMethod("extractUpdateSkyflowId", HashMap.class);
        method.setAccessible(true);
        String result = (String) method.invoke(null, data);

        Assert.assertEquals("skyflowId should be preferred when both keys are present", "id-camel", result);
    }

    @Test
    public void testExtractUpdateSkyflowId_bothKeys_removesBothFromMap() throws Exception {
        HashMap<String, Object> data = new HashMap<>();
        data.put("skyflowId", "id-camel");
        data.put("skyflow_id", "id-snake");
        data.put("card_number", "4111111111111111");

        Method method = VaultController.class.getDeclaredMethod("extractUpdateSkyflowId", HashMap.class);
        method.setAccessible(true);
        method.invoke(null, data);

        Assert.assertFalse("skyflowId should be removed from data map", data.containsKey("skyflowId"));
        Assert.assertFalse("skyflow_id should be removed from data map", data.containsKey("skyflow_id"));
        Assert.assertTrue("other fields should be preserved", data.containsKey("card_number"));
    }

    // --- insert (bulk) ---

    @Test
    public void testInsert_bulkSuccess() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);

        V1RecordMetaProperties meta = V1RecordMetaProperties.builder().skyflowId("id-123").build();
        V1InsertRecordResponse insertResp = V1InsertRecordResponse.builder()
                .records(Collections.singletonList(meta))
                .build();
        when(mockRecords.recordServiceInsertRecord(anyString(), anyString(), any())).thenReturn(insertResp);

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<HashMap<String, Object>> values = new ArrayList<>();
        HashMap<String, Object> row = new HashMap<>();
        row.put("card_number", "4111111111111111");
        values.add(row);
        InsertRequest request = InsertRequest.builder().table("test_table").values(values).build();

        InsertResponse response = controller.insert(request);

        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertNotNull("insertedFields should not be null", response.getInsertedFields());
        Assert.assertEquals(1, response.getInsertedFields().size());
        Assert.assertEquals("id-123", response.getInsertedFields().get(0).get("skyflowId"));
    }

    @Test
    public void testInsert_bulkApiErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);
        when(mockRecords.recordServiceInsertRecord(anyString(), anyString(), any()))
                .thenThrow(new ApiClientApiException("insert failed", 400, "bad request body"));

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<HashMap<String, Object>> values = new ArrayList<>();
        HashMap<String, Object> row = new HashMap<>();
        row.put("card_number", "4111111111111111");
        values.add(row);
        InsertRequest request = InsertRequest.builder().table("test_table").values(values).build();

        try {
            controller.insert(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(400, e.getHttpCode());
        }
    }

    // --- insert (batch / continueOnError) ---

    @Test
    public void testInsert_batchSuccess() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        RawRecordsClient mockRawRecords = Mockito.mock(RawRecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);
        when(mockRecords.withRawResponse()).thenReturn(mockRawRecords);

        V1BatchOperationResponse batchBody = V1BatchOperationResponse.builder().build();
        Response rawResp = buildOkHttpResponse();
        ApiClientHttpResponse<V1BatchOperationResponse> httpResp = new ApiClientHttpResponse<>(batchBody, rawResp);
        when(mockRawRecords.recordServiceBatchOperation(anyString(), any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<HashMap<String, Object>> values = new ArrayList<>();
        HashMap<String, Object> row = new HashMap<>();
        row.put("card_number", "4111111111111111");
        values.add(row);
        InsertRequest request = InsertRequest.builder()
                .table("test_table")
                .values(values)
                .continueOnError(true)
                .build();

        InsertResponse response = controller.insert(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
    }

    @Test
    public void testInsert_batchApiErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        RawRecordsClient mockRawRecords = Mockito.mock(RawRecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);
        when(mockRecords.withRawResponse()).thenReturn(mockRawRecords);
        when(mockRawRecords.recordServiceBatchOperation(anyString(), any(), any()))
                .thenThrow(new ApiClientApiException("batch failed", 500, "server error"));

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<HashMap<String, Object>> values = new ArrayList<>();
        HashMap<String, Object> row = new HashMap<>();
        row.put("card_number", "4111111111111111");
        values.add(row);
        InsertRequest request = InsertRequest.builder()
                .table("test_table")
                .values(values)
                .continueOnError(true)
                .build();

        try {
            controller.insert(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(500, e.getHttpCode());
        }
    }

    // --- detokenize ---

    @Test
    public void testDetokenize_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        TokensClient mockTokens = Mockito.mock(TokensClient.class);
        RawTokensClient mockRawTokens = Mockito.mock(RawTokensClient.class);
        when(mockApi.tokens()).thenReturn(mockTokens);
        when(mockTokens.withRawResponse()).thenReturn(mockRawTokens);

        V1DetokenizeRecordResponse detokRecord = V1DetokenizeRecordResponse.builder()
                .token("tok-123")
                .build();
        V1DetokenizeResponse detokBody = V1DetokenizeResponse.builder()
                .records(Collections.singletonList(detokRecord))
                .build();
        Response rawResp = buildOkHttpResponse();
        ApiClientHttpResponse<V1DetokenizeResponse> httpResp = new ApiClientHttpResponse<>(detokBody, rawResp);
        when(mockRawTokens.recordServiceDetokenize(anyString(), any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<DetokenizeData> detokenizeDataList = new ArrayList<>();
        detokenizeDataList.add(new DetokenizeData("tok-123"));
        DetokenizeRequest request = DetokenizeRequest.builder()
                .detokenizeData(detokenizeDataList)
                .build();

        DetokenizeResponse response = controller.detokenize(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertNotNull("detokenizedFields should not be null", response.getDetokenizedFields());
        Assert.assertEquals(1, response.getDetokenizedFields().size());
    }

    @Test
    public void testDetokenize_apiErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        TokensClient mockTokens = Mockito.mock(TokensClient.class);
        RawTokensClient mockRawTokens = Mockito.mock(RawTokensClient.class);
        when(mockApi.tokens()).thenReturn(mockTokens);
        when(mockTokens.withRawResponse()).thenReturn(mockRawTokens);
        when(mockRawTokens.recordServiceDetokenize(anyString(), any(), any()))
                .thenThrow(new ApiClientApiException("detokenize failed", 401, "unauthorized"));

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<DetokenizeData> detokenizeDataList = new ArrayList<>();
        detokenizeDataList.add(new DetokenizeData("tok-bad"));
        DetokenizeRequest request = DetokenizeRequest.builder()
                .detokenizeData(detokenizeDataList)
                .build();

        try {
            controller.detokenize(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(401, e.getHttpCode());
        }
    }

    // --- get ---

    @Test
    public void testGet_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);

        Map<String, Object> fields = new HashMap<>();
        fields.put("skyflow_id", "id-get-001");
        fields.put("card_number", "4111111111111111");
        V1FieldRecords fieldRecords = V1FieldRecords.builder().fields(fields).build();
        V1BulkGetRecordResponse getResp = V1BulkGetRecordResponse.builder()
                .records(Collections.singletonList(fieldRecords))
                .build();
        when(mockRecords.recordServiceBulkGetRecord(anyString(), anyString(), any(), any())).thenReturn(getResp);

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<String> ids = new ArrayList<>();
        ids.add("id-get-001");
        GetRequest request = GetRequest.builder().table("test_table").ids(ids).build();

        GetResponse response = controller.get(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertNotNull("data should not be null", response.getData());
        Assert.assertEquals(1, response.getData().size());
        Assert.assertEquals("id-get-001", response.getData().get(0).get("skyflowId"));
    }

    @Test
    public void testGet_apiErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);
        when(mockRecords.recordServiceBulkGetRecord(anyString(), anyString(), any(), any()))
                .thenThrow(new ApiClientApiException("get failed", 404, "not found"));

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<String> ids = new ArrayList<>();
        ids.add("id-missing");
        GetRequest request = GetRequest.builder().table("test_table").ids(ids).build();

        try {
            controller.get(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(404, e.getHttpCode());
        }
    }

    // --- update ---

    @Test
    public void testUpdate_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);

        V1UpdateRecordResponse updateResp = V1UpdateRecordResponse.builder().skyflowId("id-upd-001").build();
        when(mockRecords.recordServiceUpdateRecord(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(updateResp);

        VaultController controller = createControllerWithMock(mockApi);

        HashMap<String, Object> data = new HashMap<>();
        data.put("skyflowId", "id-upd-001");
        data.put("card_number", "9999999999999999");
        UpdateRequest request = UpdateRequest.builder().table("test_table").data(data).build();

        UpdateResponse response = controller.update(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
    }

    @Test
    public void testUpdate_apiErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);
        when(mockRecords.recordServiceUpdateRecord(anyString(), anyString(), anyString(), any(), any()))
                .thenThrow(new ApiClientApiException("update failed", 403, "forbidden"));

        VaultController controller = createControllerWithMock(mockApi);

        HashMap<String, Object> data = new HashMap<>();
        data.put("skyflowId", "id-upd-bad");
        data.put("card_number", "0000000000000000");
        UpdateRequest request = UpdateRequest.builder().table("test_table").data(data).build();

        try {
            controller.update(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(403, e.getHttpCode());
        }
    }

    // --- delete ---

    @Test
    public void testDelete_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);

        V1BulkDeleteRecordResponse deleteResp = V1BulkDeleteRecordResponse.builder()
                .recordIdResponse(Collections.singletonList("id-del-001"))
                .build();
        when(mockRecords.recordServiceBulkDeleteRecord(anyString(), anyString(), any(), any()))
                .thenReturn(deleteResp);

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<String> ids = new ArrayList<>();
        ids.add("id-del-001");
        DeleteRequest request = DeleteRequest.builder().table("test_table").ids(ids).build();

        DeleteResponse response = controller.delete(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertNotNull("deletedIds should not be null", response.getDeletedIds());
        Assert.assertEquals(1, response.getDeletedIds().size());
        Assert.assertEquals("id-del-001", response.getDeletedIds().get(0));
    }

    @Test
    public void testDelete_apiErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);
        when(mockRecords.recordServiceBulkDeleteRecord(anyString(), anyString(), any(), any()))
                .thenThrow(new ApiClientApiException("delete failed", 400, "bad id"));

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<String> ids = new ArrayList<>();
        ids.add("id-bad");
        DeleteRequest request = DeleteRequest.builder().table("test_table").ids(ids).build();

        try {
            controller.delete(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(400, e.getHttpCode());
        }
    }

    // --- query ---

    @Test
    public void testQuery_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        QueryClient mockQuery = Mockito.mock(QueryClient.class);
        when(mockApi.query()).thenReturn(mockQuery);

        Map<String, Object> fields = new HashMap<>();
        fields.put("skyflow_id", "id-qry-001");
        V1FieldRecords fieldRecords = V1FieldRecords.builder().fields(fields).build();
        V1GetQueryResponse queryResp = V1GetQueryResponse.builder()
                .records(Collections.singletonList(fieldRecords))
                .build();
        when(mockQuery.queryServiceExecuteQuery(anyString(), any(), any())).thenReturn(queryResp);

        VaultController controller = createControllerWithMock(mockApi);

        QueryRequest request = QueryRequest.builder().query("SELECT * FROM test_table LIMIT 1").build();

        QueryResponse response = controller.query(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertNotNull("fields should not be null", response.getFields());
        Assert.assertEquals(1, response.getFields().size());
        Assert.assertEquals("id-qry-001", response.getFields().get(0).get("skyflowId"));
    }

    @Test
    public void testQuery_apiErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        QueryClient mockQuery = Mockito.mock(QueryClient.class);
        when(mockApi.query()).thenReturn(mockQuery);
        when(mockQuery.queryServiceExecuteQuery(anyString(), any(), any()))
                .thenThrow(new ApiClientApiException("query failed", 400, "invalid sql"));

        VaultController controller = createControllerWithMock(mockApi);

        QueryRequest request = QueryRequest.builder().query("SELECT * FROM test_table LIMIT 1").build();

        try {
            controller.query(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(400, e.getHttpCode());
        }
    }

    // --- insert (batch) with actual records — covers getFormattedBatchInsertRecord ---

    @Test
    public void testInsert_batchSuccessWithRecords() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        RawRecordsClient mockRawRecords = Mockito.mock(RawRecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);
        when(mockRecords.withRawResponse()).thenReturn(mockRawRecords);

        // Build a response item whose Body contains a records array with skyflowId and tokens
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("card_number", "tok-card-111");

        Map<String, Object> recordEntry = new HashMap<>();
        recordEntry.put("skyflowId", "id-batch-001");
        recordEntry.put("tokens", tokens);

        List<Map<String, Object>> recordsList = new ArrayList<>();
        recordsList.add(recordEntry);

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("records", recordsList);

        Map<String, Object> responseItem = new HashMap<>();
        responseItem.put("Body", bodyMap);

        List<Map<String, Object>> responses = new ArrayList<>();
        responses.add(responseItem);

        V1BatchOperationResponse batchBody = V1BatchOperationResponse.builder()
                .responses(responses)
                .build();
        Response rawResp = buildOkHttpResponse();
        ApiClientHttpResponse<V1BatchOperationResponse> httpResp = new ApiClientHttpResponse<>(batchBody, rawResp);
        when(mockRawRecords.recordServiceBatchOperation(anyString(), any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<HashMap<String, Object>> values = new ArrayList<>();
        HashMap<String, Object> row = new HashMap<>();
        row.put("card_number", "4111111111111111");
        values.add(row);
        InsertRequest request = InsertRequest.builder()
                .table("test_table")
                .values(values)
                .continueOnError(true)
                .build();

        InsertResponse response = controller.insert(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertNotNull("insertedFields should not be null", response.getInsertedFields());
        Assert.assertEquals(1, response.getInsertedFields().size());
        Assert.assertEquals("id-batch-001", response.getInsertedFields().get(0).get("skyflowId"));
    }

    // --- insert (bulk) with tokens in metadata — covers getFormattedBulkInsertRecord tokens branch ---

    @Test
    public void testInsert_bulkSuccessWithTokens() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);

        Map<String, Object> tokens = new HashMap<>();
        tokens.put("card_number", "tok-card-456");
        V1RecordMetaProperties meta = V1RecordMetaProperties.builder()
                .skyflowId("id-with-tokens")
                .tokens(tokens)
                .build();
        V1InsertRecordResponse insertResp = V1InsertRecordResponse.builder()
                .records(Collections.singletonList(meta))
                .build();
        when(mockRecords.recordServiceInsertRecord(anyString(), anyString(), any())).thenReturn(insertResp);

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<HashMap<String, Object>> values = new ArrayList<>();
        HashMap<String, Object> row = new HashMap<>();
        row.put("card_number", "4111111111111111");
        values.add(row);
        InsertRequest request = InsertRequest.builder().table("test_table").values(values).build();

        InsertResponse response = controller.insert(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertNotNull("insertedFields should not be null", response.getInsertedFields());
        Assert.assertEquals(1, response.getInsertedFields().size());
        Assert.assertEquals("id-with-tokens", response.getInsertedFields().get(0).get("skyflowId"));
        Assert.assertEquals("tok-card-456", response.getInsertedFields().get(0).get("card_number"));
    }

    // --- update with tokens in response — covers lambda$1 (getFormattedUpdateRecord tokens branch) ---

    @Test
    public void testUpdate_withTokensInResponse() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);

        Map<String, Object> tokens = new HashMap<>();
        tokens.put("card_number", "tok-upd-card");
        V1UpdateRecordResponse updateResp = V1UpdateRecordResponse.builder()
                .skyflowId("id-upd-tok")
                .tokens(tokens)
                .build();
        when(mockRecords.recordServiceUpdateRecord(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(updateResp);

        VaultController controller = createControllerWithMock(mockApi);

        HashMap<String, Object> data = new HashMap<>();
        data.put("skyflowId", "id-upd-tok");
        data.put("card_number", "4111111111111111");
        UpdateRequest request = UpdateRequest.builder().table("test_table").data(data).build();

        UpdateResponse response = controller.update(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertNotNull("tokens map should not be null", response.getTokens());
        // skyflowId is put into the tokens map by getFormattedUpdateRecord
        Assert.assertEquals("id-upd-tok", response.getTokens().get("skyflowId"));
        Assert.assertEquals("tok-upd-card", response.getTokens().get("card_number"));
    }

    // --- detokenize with an error record — covers error-record path in detokenize ---

    @Test
    public void testDetokenize_errorRecordPath() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        TokensClient mockTokens = Mockito.mock(TokensClient.class);
        RawTokensClient mockRawTokens = Mockito.mock(RawTokensClient.class);
        when(mockApi.tokens()).thenReturn(mockTokens);
        when(mockTokens.withRawResponse()).thenReturn(mockRawTokens);

        V1DetokenizeRecordResponse errRecord = V1DetokenizeRecordResponse.builder()
                .token("tok-bad")
                .error("token not found")
                .build();
        V1DetokenizeResponse detokBody = V1DetokenizeResponse.builder()
                .records(Collections.singletonList(errRecord))
                .build();
        Response rawResp = buildOkHttpResponse();
        ApiClientHttpResponse<V1DetokenizeResponse> httpResp = new ApiClientHttpResponse<>(detokBody, rawResp);
        when(mockRawTokens.recordServiceDetokenize(anyString(), any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<DetokenizeData> detokenizeDataList = new ArrayList<>();
        detokenizeDataList.add(new DetokenizeData("tok-bad"));
        DetokenizeRequest request = DetokenizeRequest.builder()
                .detokenizeData(detokenizeDataList)
                .build();

        DetokenizeResponse response = controller.detokenize(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertNotNull("errors should not be null", response.getErrors());
        Assert.assertEquals(1, response.getErrors().size());
        Assert.assertEquals("tok-bad", response.getErrors().get(0).getToken());
        Assert.assertEquals("token not found", response.getErrors().get(0).getError());
    }

    // --- tokenize ---

    @Test
    public void testTokenize_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        TokensClient mockTokens = Mockito.mock(TokensClient.class);
        when(mockApi.tokens()).thenReturn(mockTokens);

        V1TokenizeRecordResponse tokenRecord = V1TokenizeRecordResponse.builder().token("tok-abc").build();
        V1TokenizeResponse tokenResp = V1TokenizeResponse.builder()
                .records(Collections.singletonList(tokenRecord))
                .build();
        when(mockTokens.recordServiceTokenize(anyString(), any(), any())).thenReturn(tokenResp);

        VaultController controller = createControllerWithMock(mockApi);

        ColumnValue cv = ColumnValue.builder().value("test-val").columnGroup("test-group").build();
        TokenizeRequest request = TokenizeRequest.builder()
                .values(Collections.singletonList(cv))
                .build();

        TokenizeResponse response = controller.tokenize(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertNotNull("tokens should not be null", response.getTokens());
        Assert.assertEquals(1, response.getTokens().size());
        Assert.assertEquals("tok-abc", response.getTokens().get(0));
    }

    @Test
    public void testTokenize_apiErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        TokensClient mockTokens = Mockito.mock(TokensClient.class);
        when(mockApi.tokens()).thenReturn(mockTokens);
        when(mockTokens.recordServiceTokenize(anyString(), any(), any()))
                .thenThrow(new ApiClientApiException("tokenize failed", 422, "unprocessable"));

        VaultController controller = createControllerWithMock(mockApi);

        ColumnValue cv = ColumnValue.builder().value("test-val").columnGroup("test-group").build();
        TokenizeRequest request = TokenizeRequest.builder()
                .values(Collections.singletonList(cv))
                .build();

        try {
            controller.tokenize(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(422, e.getHttpCode());
        }
    }
}
