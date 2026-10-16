package com.skyflow.vault.controller;

import com.skyflow.generated.rest.types.DeleteResponseObject;
import com.skyflow.generated.rest.types.DeleteTokenResponse;
import com.skyflow.generated.rest.types.DeleteTokenResponseObject;
import com.skyflow.generated.rest.types.DetokenizeResponseObject;
import com.skyflow.generated.rest.types.ExecuteQueryRecordResponse;
import com.skyflow.generated.rest.types.ExecuteQueryResponse;
import com.skyflow.generated.rest.types.ExecuteQueryResponseMetadata;
import com.skyflow.generated.rest.types.GetTokensFromValuesResponse;
import com.skyflow.generated.rest.types.RecordResponseObject;
import com.skyflow.generated.rest.types.TokenizeResponseObject;
import com.skyflow.VaultClient;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.CustomHeaderKey;
import com.skyflow.enums.Env;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.errors.NotFoundError;
import com.skyflow.generated.rest.errors.UnauthorizedError;
import com.skyflow.vault.data.DeleteFilesOptions;
import com.skyflow.vault.data.DeleteFilesRequest;
import com.skyflow.vault.data.DeleteFilesRequestRecord;
import com.skyflow.vault.data.DeleteFilesResponse;
import com.skyflow.vault.data.UploadFilesOptions;
import com.skyflow.vault.data.UploadFilesRequest;
import com.skyflow.vault.data.UploadFilesRequestColumn;
import com.skyflow.vault.data.UploadFilesRequestRecord;
import com.skyflow.vault.data.UploadFilesResponse;
import com.skyflow.errors.ErrorMessage;
import java.io.File;
import com.skyflow.generated.rest.core.ObjectMappers;
import com.skyflow.generated.rest.errors.BadRequestError;
import com.skyflow.generated.rest.errors.ForbiddenError;
import com.skyflow.generated.rest.types.ErrorResponse;
import com.skyflow.generated.rest.resources.files.FilesClient;
import com.skyflow.generated.rest.resources.files.RawFilesClient;
import com.skyflow.generated.rest.resources.query.QueryClient;
import com.skyflow.generated.rest.resources.query.RawQueryClient;
import com.skyflow.generated.rest.resources.records.RawRecordsClient;
import com.skyflow.generated.rest.resources.records.RecordsClient;
import com.skyflow.generated.rest.resources.tokens.RawTokensClient;
import com.skyflow.generated.rest.resources.tokens.TokensClient;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.core.ApiClientHttpResponse;
import com.skyflow.generated.rest.core.RequestOptions;
import com.skyflow.utils.Constants;
import com.skyflow.vault.data.BulkDeleteTokensOptions;
import com.skyflow.vault.data.BulkTokenizeOptions;
import com.skyflow.vault.data.BulkDeleteTokensRequest;
import com.skyflow.vault.data.BulkDeleteTokensResponse;
import com.skyflow.vault.data.BulkDetokenizeRequest;
import com.skyflow.vault.data.BulkDetokenizeResponse;
import com.skyflow.vault.data.BulkInsertRequestRecord;
import com.skyflow.vault.data.BulkInsertRequest;
import com.skyflow.vault.data.BulkInsertResponse;
import com.skyflow.vault.data.BulkTokenizeRequestRecord;
import com.skyflow.vault.data.BulkTokenizeResponseRecord;
import com.skyflow.vault.data.BulkInsertResponseRecord;
import com.skyflow.vault.data.BulkTokenizeRequest;
import com.skyflow.vault.data.BulkTokenizeResponse;
import com.skyflow.vault.data.BulkDetokenizeOptions;
import com.skyflow.vault.data.BulkInsertOptions;
import com.skyflow.vault.data.ColumnRedactions;
import com.skyflow.vault.data.DeleteOptions;
import com.skyflow.vault.data.DeleteRequest;
import com.skyflow.vault.data.DeleteResponse;
import com.skyflow.vault.data.DeleteTokensOptions;
import com.skyflow.vault.data.DetokenizeOptions;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.DetokenizeResponse;
import com.skyflow.vault.data.GetOptions;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;
import com.skyflow.vault.data.GetTokensOptions;
import com.skyflow.vault.data.GetTokensRequest;
import com.skyflow.vault.data.GetTokensRequestRecord;
import com.skyflow.vault.data.GetTokensResponse;
import com.skyflow.vault.data.InsertOptions;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertRequestRecord;
import com.skyflow.vault.data.InsertResponse;
import com.skyflow.vault.data.QueryOptions;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.QueryResponse;
import com.skyflow.vault.data.RequestInterceptor;
import com.skyflow.vault.data.Token;
import com.skyflow.vault.data.TokenGroupRedactions;
import com.skyflow.vault.data.TokenizeOptions;
import com.skyflow.vault.data.TokenizeRequestRecord;
import com.skyflow.vault.data.TokenizeRequest;
import com.skyflow.vault.data.TokenizeResponse;
import com.skyflow.vault.data.UpdateOptions;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateRequestRecord;
import com.skyflow.vault.data.UpdateResponse;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class VaultControllerTests {
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";

    private static VaultController createControllerWithMock(ApiClient mockApiClient) throws Exception {
        Credentials creds = new Credentials();
        creds.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");

        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");
        config.setClusterId("cluster123");
        config.setEnv(Env.DEV);

        VaultController controller = new VaultController(config, creds);
        Field field = VaultClient.class.getDeclaredField("apiClient");
        field.setAccessible(true);
        field.set(controller, mockApiClient);
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

    /** Raw-response mocks for each generated resource client the controller calls. */
    private static final class MockRaw {
        final RawRecordsClient records = Mockito.mock(RawRecordsClient.class);
        final RawTokensClient tokens = Mockito.mock(RawTokensClient.class);
        final RawQueryClient query = Mockito.mock(RawQueryClient.class);
        final RawFilesClient files = Mockito.mock(RawFilesClient.class);
    }

    private static MockRaw mockRawFlowservice(ApiClient mockApi) {
        MockRaw mockRaw = new MockRaw();
        RecordsClient records = Mockito.mock(RecordsClient.class);
        TokensClient tokens = Mockito.mock(TokensClient.class);
        QueryClient query = Mockito.mock(QueryClient.class);
        FilesClient files = Mockito.mock(FilesClient.class);
        when(mockApi.records()).thenReturn(records);
        when(mockApi.files()).thenReturn(files);
        when(files.withRawResponse()).thenReturn(mockRaw.files);
        when(mockApi.tokens()).thenReturn(tokens);
        when(mockApi.query()).thenReturn(query);
        when(records.withRawResponse()).thenReturn(mockRaw.records);
        when(tokens.withRawResponse()).thenReturn(mockRaw.tokens);
        when(query.withRawResponse()).thenReturn(mockRaw.query);
        return mockRaw;
    }

    // ── insert (unary) ────────────────────────────────────────────────────────

    @Test
    public void testInsert_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "tok-abc");
        RecordResponseObject record = RecordResponseObject.builder().httpCode(200)
                .tableName("table1").skyflowId("sky-id-1").tokens(tokens).build();
        com.skyflow.generated.rest.types.InsertResponse body = com.skyflow.generated.rest.types.InsertResponse.builder().records(Collections.singletonList(record)).build();
        ApiClientHttpResponse<com.skyflow.generated.rest.types.InsertResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.records.insertRecords(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        InsertRequestRecord insertRecord = InsertRequestRecord.builder().data(data).build();
        InsertRequest request = InsertRequest.builder()
                .tableName("table1")
                .records(Collections.singletonList(insertRecord))
                .build();

        InsertResponse response = controller.insert(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals("sky-id-1", response.getRecords().get(0).getSkyflowId());
        Assert.assertNull(response.getRecords().get(0).getError());
    }

    @Test
    public void testInsert_invalidRequestThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);
        InsertRequest request = InsertRequest.builder().records(new ArrayList<>()).build();
        try {
            controller.insert(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testInsert_interceptorAddsCustomHeader() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        RecordResponseObject record = RecordResponseObject.builder().httpCode(200).skyflowId("sky-id-1").build();
        com.skyflow.generated.rest.types.InsertResponse body = com.skyflow.generated.rest.types.InsertResponse.builder().records(Collections.singletonList(record)).build();
        ApiClientHttpResponse<com.skyflow.generated.rest.types.InsertResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.records.insertRecords(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        InsertRequestRecord insertRecord = InsertRequestRecord.builder().data(data).build();
        InsertRequest request = InsertRequest.builder()
                .tableName("table1")
                .records(Collections.singletonList(insertRecord))
                .build();

        RequestInterceptor interceptor = ctx -> ctx.addHeader(CustomHeaderKey.SKYFLOW_ACCOUNT_ID, "acct-123");
        InsertOptions options = InsertOptions.builder().interceptor(interceptor).build();

        controller.insert(request, options);

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        Mockito.verify(mockRaw.records).insertRecords(any(), captor.capture());
        Assert.assertEquals("acct-123", captor.getValue().getHeaders().get(CustomHeaderKey.SKYFLOW_ACCOUNT_ID.toString()));
    }

    // ── detokenize (unary) ────────────────────────────────────────────────────

    @Test
    public void testDetokenize_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        DetokenizeResponseObject record = DetokenizeResponseObject.builder()
                .token("tok-1").value(com.skyflow.generated.rest.types.GoogleProtobufValue.of("john@example.com")).build();
        com.skyflow.generated.rest.types.DetokenizeResponse body = com.skyflow.generated.rest.types.DetokenizeResponse.builder()
                .response(Collections.singletonList(record)).build();
        ApiClientHttpResponse<com.skyflow.generated.rest.types.DetokenizeResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.tokens.detokenize(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(Collections.singletonList("tok-1")).build();

        DetokenizeResponse response = controller.detokenize(request);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals("john@example.com", response.getRecords().get(0).getValue());
    }

    @Test
    public void testDetokenize_invalidRequestThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(new ArrayList<>()).build();
        try {
            controller.detokenize(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    // ── delete (unary) ────────────────────────────────────────────────────────

    @Test
    public void testDelete_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        DeleteResponseObject record = DeleteResponseObject.builder().skyflowId("sky-1").httpCode(200).build();
        com.skyflow.generated.rest.types.DeleteResponse body = com.skyflow.generated.rest.types.DeleteResponse.builder().records(Collections.singletonList(record)).build();
        ApiClientHttpResponse<com.skyflow.generated.rest.types.DeleteResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.records.deleteRecords(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);
        DeleteRequest request = DeleteRequest.builder().tableName("table1").skyflowIds(Collections.singletonList("sky-1")).build();

        DeleteResponse response = controller.delete(request);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals("sky-1", response.getRecords().get(0).getSkyflowId());
        Assert.assertNull(response.getRecords().get(0).getError());
    }

    @Test
    public void testDelete_invalidRequestThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);
        DeleteRequest request = DeleteRequest.builder().tableName("table1").build();
        try {
            controller.delete(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    // ── update (unary) ────────────────────────────────────────────────────────

    @Test
    public void testUpdate_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        RecordResponseObject record = RecordResponseObject.builder().httpCode(200)
                .tableName("table1").skyflowId("sky-1").build();
        com.skyflow.generated.rest.types.UpdateResponse body = com.skyflow.generated.rest.types.UpdateResponse.builder().records(Collections.singletonList(record)).build();
        ApiClientHttpResponse<com.skyflow.generated.rest.types.UpdateResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.records.updateRecords(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "jane");
        UpdateRequestRecord updateRecord = UpdateRequestRecord.builder().skyflowId("sky-1").data(data).build();
        UpdateRequest request = UpdateRequest.builder()
                .tableName("table1")
                .records(Collections.singletonList(updateRecord))
                .build();

        UpdateResponse response = controller.update(request);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals("sky-1", response.getRecords().get(0).getSkyflowId());
    }

    // Regression: a single failing record in a unary call can make the vault reflect the failure
    // as the overall HTTP status (here 400), so the generated client throws ApiClientApiException
    // instead of returning a normal body. When that exception body still has the familiar
    // per-record "records" shape, it must land on the UpdateResponse like a 200 partial success
    // would - not surface as a thrown SkyflowException.
    @Test
    public void testUpdate_recordLevelFailureReflectedAsHttpErrorStillReturnsResponse() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        Map<String, Object> failedRecord = new HashMap<>();
        failedRecord.put("skyflowID", null);
        failedRecord.put("tokens", null);
        failedRecord.put("data", null);
        failedRecord.put("hashedData", null);
        failedRecord.put("error", "UPDATE failed. Column card_number is invalid. Specify a valid column.");
        failedRecord.put("httpCode", 400);
        failedRecord.put("tableName", "");
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("records", Collections.singletonList(failedRecord));

        when(mockRaw.records.updateRecords(any(), any()))
                .thenThrow(new ApiClientApiException("Error with status code 400", 400, responseBody));

        VaultController controller = createControllerWithMock(mockApi);

        UpdateRequestRecord updateRecord = UpdateRequestRecord.builder().skyflowId("sky-1").build();
        UpdateRequest request = UpdateRequest.builder()
                .tableName("table1")
                .records(Collections.singletonList(updateRecord))
                .build();

        UpdateResponse response = controller.update(request);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals("UPDATE failed. Column card_number is invalid. Specify a valid column.",
                response.getRecords().get(0).getError());
        Assert.assertEquals(400, response.getRecords().get(0).getHttpCode());
        Assert.assertNull(response.getRecords().get(0).getSkyflowId());
    }

    // Regression: a genuine whole-request API error (e.g. vault not found) has no "records" key
    // at all, so the fallback added above must not swallow it - it still has to throw.
    @Test
    public void testUpdate_wholeRequestApiErrorStillThrows() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("grpc_code", 5);
        errorBody.put("http_code", 404);
        errorBody.put("message", "Invalid request. Vault not found for vaultID: vault123. Specify a valid vaultID.");
        errorBody.put("http_status", "Not Found");
        errorBody.put("details", new ArrayList<>());
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", errorBody);

        when(mockRaw.records.updateRecords(any(), any()))
                .thenThrow(new ApiClientApiException("Error with status code 404", 404, responseBody));

        VaultController controller = createControllerWithMock(mockApi);

        UpdateRequestRecord updateRecord = UpdateRequestRecord.builder().skyflowId("sky-1").build();
        UpdateRequest request = UpdateRequest.builder()
                .tableName("table1")
                .records(Collections.singletonList(updateRecord))
                .build();

        try {
            controller.update(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(
                    "Invalid request. Vault not found for vaultID: vault123. Specify a valid vaultID.",
                    e.getMessage());
            Assert.assertEquals(404, e.getHttpCode());
        }
    }

    @Test
    public void testUpdate_invalidRequestThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);
        UpdateRequest request = UpdateRequest.builder().tableName("table1").records(new ArrayList<>()).build();
        try {
            controller.update(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    // ── get (unary) ───────────────────────────────────────────────────────────

    @Test
    public void testGet_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        RecordResponseObject record = RecordResponseObject.builder().httpCode(200)
                .tableName("table1").skyflowId("sky-1").build();
        com.skyflow.generated.rest.types.GetResponse body = com.skyflow.generated.rest.types.GetResponse.builder().records(Collections.singletonList(record)).build();
        ApiClientHttpResponse<com.skyflow.generated.rest.types.GetResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.records.getRecords(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);
        GetRequest request = GetRequest.builder()
                .tableName("table1")
                .skyflowIds(new ArrayList<>(Collections.singletonList("sky-1")))
                .build();

        GetResponse response = controller.get(request);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals("sky-1", response.getRecords().get(0).getSkyflowId());
    }

    @Test
    public void testGet_invalidRequestThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);
        GetRequest request = GetRequest.builder().tableName("table1").build();
        try {
            controller.get(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    // ── query (unary) ─────────────────────────────────────────────────────────

    @Test
    public void testQuery_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        Map<String, Object> row = new HashMap<>();
        row.put("skyflow_id", "sky-1");
        row.put("name", "john");
        ExecuteQueryResponse body = ExecuteQueryResponse.builder()
                .records(Collections.singletonList(ExecuteQueryRecordResponse.builder().data(row).build()))
                .metadata(ExecuteQueryResponseMetadata.builder().columns(Arrays.asList("skyflow_id", "name")).build())
                .build();
        ApiClientHttpResponse<ExecuteQueryResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.query.executeQuery(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);
        QueryRequest request = QueryRequest.builder().query("SELECT * FROM table1").build();

        QueryResponse response = controller.query(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getFields().size());
        Assert.assertEquals("sky-1", response.getFields().get(0).get("skyflow_id"));
        Assert.assertNull(response.getErrors());
        Assert.assertEquals(Arrays.asList("skyflow_id", "name"), response.getMetadata().getColumns());
        Assert.assertEquals("req-test-123", response.getRequestId());
    }

    @Test
    public void testQuery_invalidRequestThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);
        QueryRequest request = QueryRequest.builder().query("  ").build();
        try {
            controller.query(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
        Mockito.verifyNoInteractions(mockApi);
    }

    @Test
    public void testQuery_apiErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        Map<String, Object> error = new HashMap<>();
        error.put("http_code", 400);
        error.put("message", "Only SELECT queries are supported.");
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", error);
        when(mockRaw.query.executeQuery(any(), any()))
                .thenThrow(new ApiClientApiException("Error with status code 400", 400, responseBody));

        VaultController controller = createControllerWithMock(mockApi);
        QueryRequest request = QueryRequest.builder().query("DELETE FROM table1").build();
        try {
            controller.query(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(400, e.getHttpCode());
            Assert.assertEquals("Only SELECT queries are supported.", e.getMessage());
        }
    }

    @Test
    public void testQuery_typedErrorThrowsSkyflowExceptionWithApiMessage() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        ErrorResponse body = ObjectMappers.JSON_MAPPER.readValue(
                "{\"error\":{\"grpc_code\":3,\"http_code\":400,\"http_status\":\"Bad Request\","
                        + "\"message\":\"Only SELECT queries are supported.\"}}",
                ErrorResponse.class);
        when(mockRaw.query.executeQuery(any(), any())).thenThrow(new BadRequestError(body));

        VaultController controller = createControllerWithMock(mockApi);
        QueryRequest request = QueryRequest.builder().query("DELETE FROM table1").build();
        try {
            controller.query(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(400, e.getHttpCode());
            Assert.assertEquals("Only SELECT queries are supported.", e.getMessage());
        }
    }

    @Test
    public void testQuery_interceptorAddsCustomHeader() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        ApiClientHttpResponse<ExecuteQueryResponse> httpResp =
                new ApiClientHttpResponse<>(ExecuteQueryResponse.builder().build(), buildOkHttpResponse());
        when(mockRaw.query.executeQuery(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);
        List<String> operations = new ArrayList<>();
        RequestInterceptor interceptor = ctx -> {
            operations.add(ctx.getOperation());
            ctx.addHeader(CustomHeaderKey.SKYFLOW_ACCOUNT_ID, "acct-123");
        };
        QueryOptions options = QueryOptions.builder().interceptor(interceptor).build();

        controller.query(QueryRequest.builder().query("SELECT * FROM table1").build(), options);

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        Mockito.verify(mockRaw.query).executeQuery(any(), captor.capture());
        Assert.assertEquals("acct-123", captor.getValue().getHeaders().get(CustomHeaderKey.SKYFLOW_ACCOUNT_ID.toString()));
        Assert.assertEquals(Collections.singletonList("QUERY"), operations);
    }

    // ── getTokens (unary) ─────────────────────────────────────────────────────

    @Test
    public void testGetTokens_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        TokenizeResponseObject record = TokenizeResponseObject.builder().token("tok-1")
                .value(com.skyflow.generated.rest.types.GoogleProtobufValue.of("john@example.com")).tokenGroupName("det_group").httpCode(200).build();
        GetTokensFromValuesResponse body = GetTokensFromValuesResponse.builder().records(Collections.singletonList(record)).build();
        ApiClientHttpResponse<GetTokensFromValuesResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.tokens.getTokens(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);
        GetTokensRequest request = GetTokensRequest.builder()
                .records(Collections.singletonList(GetTokensRequestRecord.builder()
                        .value("john@example.com").tokenGroupName("det_group").build()))
                .build();

        GetTokensResponse response = controller.getTokens(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals("tok-1", response.getRecords().get(0).get("token"));
        Assert.assertNull(response.getRecords().get(0).get("error"));
    }

    @Test
    public void testGetTokens_invalidRequestThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);
        GetTokensRequest request = GetTokensRequest.builder().records(new ArrayList<>()).build();
        try {
            controller.getTokens(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
        Mockito.verifyNoInteractions(mockApi);
    }

    @Test
    public void testGetTokens_recordsShapedErrorBodyReturnedAsResponse() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        Map<String, Object> failedRecord = new HashMap<>();
        failedRecord.put("value", "unknown@example.com");
        failedRecord.put("tokenGroupName", "det_group");
        failedRecord.put("error", "Token not found.");
        failedRecord.put("httpCode", 404);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("records", Collections.singletonList(failedRecord));
        when(mockRaw.tokens.getTokens(any(), any()))
                .thenThrow(new ApiClientApiException("Error with status code 404", 404, responseBody));

        VaultController controller = createControllerWithMock(mockApi);
        GetTokensRequest request = GetTokensRequest.builder()
                .records(Collections.singletonList(GetTokensRequestRecord.builder()
                        .value("unknown@example.com").tokenGroupName("det_group").build()))
                .build();

        GetTokensResponse response = controller.getTokens(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals(404, response.getRecords().get(0).get("httpCode"));
        Assert.assertEquals("Token not found.", response.getRecords().get(0).get("error"));
        Assert.assertEquals("", response.getRecords().get(0).get("token"));
    }

    @Test
    public void testGetTokens_wholeCallErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        Map<String, Object> error = new HashMap<>();
        error.put("http_code", 403);
        error.put("message", "Permission denied.");
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", error);
        when(mockRaw.tokens.getTokens(any(), any()))
                .thenThrow(new ApiClientApiException("Error with status code 403", 403, responseBody));

        VaultController controller = createControllerWithMock(mockApi);
        GetTokensRequest request = GetTokensRequest.builder()
                .records(Collections.singletonList(GetTokensRequestRecord.builder()
                        .value("john@example.com").tokenGroupName("det_group").build()))
                .build();
        try {
            controller.getTokens(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(403, e.getHttpCode());
            Assert.assertEquals("Permission denied.", e.getMessage());
        }
    }

    // The documented statuses arrive as typed errors whose body is an ErrorResponse, not a map;
    // the fields ErrorResponse does not model (here "records") must still be read.
    @Test
    public void testGetTokens_typedErrorWithRecordsBodyReturnedAsResponse() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        ErrorResponse body = ObjectMappers.JSON_MAPPER.readValue(
                "{\"records\":[{\"value\":\"unknown@example.com\",\"tokenGroupName\":\"det_group\","
                        + "\"token\":\"\",\"error\":\"Token not found.\",\"httpCode\":400}]}",
                ErrorResponse.class);
        when(mockRaw.tokens.getTokens(any(), any())).thenThrow(new BadRequestError(body));

        VaultController controller = createControllerWithMock(mockApi);
        GetTokensRequest request = GetTokensRequest.builder()
                .records(Collections.singletonList(GetTokensRequestRecord.builder()
                        .value("unknown@example.com").tokenGroupName("det_group").build()))
                .build();

        GetTokensResponse response = controller.getTokens(request);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals("unknown@example.com", response.getRecords().get(0).get("value"));
        Assert.assertEquals(400, response.getRecords().get(0).get("httpCode"));
        Assert.assertEquals("Token not found.", response.getRecords().get(0).get("error"));
        Assert.assertEquals("", response.getRecords().get(0).get("token"));
    }

    @Test
    public void testGetTokens_typedErrorThrowsSkyflowExceptionWithApiMessage() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        ErrorResponse body = ObjectMappers.JSON_MAPPER.readValue(
                "{\"error\":{\"grpc_code\":7,\"http_code\":403,\"http_status\":\"Forbidden\","
                        + "\"message\":\"Permission denied.\"}}",
                ErrorResponse.class);
        when(mockRaw.tokens.getTokens(any(), any())).thenThrow(new ForbiddenError(body));

        VaultController controller = createControllerWithMock(mockApi);
        GetTokensRequest request = GetTokensRequest.builder()
                .records(Collections.singletonList(GetTokensRequestRecord.builder()
                        .value("john@example.com").tokenGroupName("det_group").build()))
                .build();
        try {
            controller.getTokens(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(403, e.getHttpCode());
            Assert.assertEquals("Permission denied.", e.getMessage());
        }
    }

    @Test
    public void testGetTokens_interceptorAddsCustomHeader() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        ApiClientHttpResponse<GetTokensFromValuesResponse> httpResp =
                new ApiClientHttpResponse<>(GetTokensFromValuesResponse.builder().build(), buildOkHttpResponse());
        when(mockRaw.tokens.getTokens(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);
        List<String> operations = new ArrayList<>();
        RequestInterceptor interceptor = ctx -> {
            operations.add(ctx.getOperation());
            ctx.addHeader(CustomHeaderKey.SKYFLOW_ACCOUNT_ID, "acct-123");
        };
        GetTokensOptions options = GetTokensOptions.builder().interceptor(interceptor).build();
        GetTokensRequest request = GetTokensRequest.builder()
                .records(Collections.singletonList(GetTokensRequestRecord.builder()
                        .value("john@example.com").tokenGroupName("det_group").build()))
                .build();

        controller.getTokens(request, options);

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        Mockito.verify(mockRaw.tokens).getTokens(any(), captor.capture());
        Assert.assertEquals("acct-123", captor.getValue().getHeaders().get(CustomHeaderKey.SKYFLOW_ACCOUNT_ID.toString()));
        Assert.assertEquals(Collections.singletonList("GET_TOKENS"), operations);
    }

    // ── uploadFiles (unary, two-phase) ────────────────────────────────────────

    @org.junit.Rule
    public org.junit.rules.TemporaryFolder filesFolder = new org.junit.rules.TemporaryFolder();

    /** Records each Phase B call and answers with the status mapped to a URL fragment (default 200). */
    // PUTs run concurrently, so uploads are recorded per URL rather than in call order.
    private static final class RecordingUploader implements VaultController.SignedUrlUploader {
        final Map<String, String> bodyByUrl = new java.util.concurrent.ConcurrentHashMap<>();
        final Map<String, String> contentTypeByUrl = new java.util.concurrent.ConcurrentHashMap<>();
        final Map<String, Integer> statusByUrlFragment = new HashMap<>();

        @Override
        public int upload(String signedUrl, okhttp3.RequestBody fileBody) throws java.io.IOException {
            okio.Buffer buffer = new okio.Buffer();
            fileBody.writeTo(buffer);
            bodyByUrl.put(signedUrl, buffer.readUtf8());
            contentTypeByUrl.put(signedUrl, String.valueOf(fileBody.contentType()));
            for (Map.Entry<String, Integer> entry : statusByUrlFragment.entrySet()) {
                if (signedUrl.contains(entry.getKey())) return entry.getValue();
            }
            return 200;
        }
    }

    private static com.skyflow.generated.rest.types.FileUploadResponse uploadWire(String json) throws Exception {
        return ObjectMappers.JSON_MAPPER.readValue(json, com.skyflow.generated.rest.types.FileUploadResponse.class);
    }

    private File textFile(String name, String content) throws Exception {
        File file = filesFolder.newFile(name);
        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            writer.write(content);
        }
        return file;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> columnsOf(HashMap<String, Object> record) {
        return (List<Map<String, Object>>) record.get("columns");
    }

    @Test
    public void testUploadFiles_partialSuccessAcrossColumnsAndRecords() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        File resume = textFile("resume.pdf", "resume bytes");
        when(mockRaw.files.uploadFiles(any(), any())).thenReturn(new ApiClientHttpResponse<>(uploadWire(
                "{\"records\":["
                        + "{\"skyflowID\":\"sky-new\",\"tableName\":\"onboarding\",\"httpCode\":200,"
                        + "\"data\":{\"resumePDF\":\"https://upload.example.com/resumePDF?signed=t1\","
                        + "\"photoID\":\"https://upload.example.com/photoID?signed=t2\"}},"
                        + "{\"skyflowID\":\"sky-bad\",\"tableName\":\"onboarding\",\"httpCode\":400,"
                        + "\"error\":\"Invalid request. skyflowID sky-bad is invalid.\"}]}"),
                buildOkHttpResponse()));

        VaultController controller = createControllerWithMock(mockApi);
        RecordingUploader uploader = new RecordingUploader();
        uploader.statusByUrlFragment.put("photoID", 403);
        controller.signedUrlUploader = uploader;

        UploadFilesRequest request = UploadFilesRequest.builder().records(Arrays.asList(
                UploadFilesRequestRecord.builder().tableName("onboarding").columns(Arrays.asList(
                        UploadFilesRequestColumn.builder().column("resumePDF").filePath(resume.getPath()).build(),
                        UploadFilesRequestColumn.builder().column("photoID").base64("cGhvdG8=").fileName("photo.jpg").build()))
                        .build(),
                UploadFilesRequestRecord.builder().tableName("onboarding").skyflowId("sky-bad").columns(
                        Collections.singletonList(UploadFilesRequestColumn.builder().column("kyc")
                                .base64("a3lj").fileName("kyc.txt").build())).build()))
                .build();

        UploadFilesResponse response = controller.uploadFiles(request);

        Assert.assertEquals(2, response.getRecords().size());
        HashMap<String, Object> created = response.getRecords().get(0);
        Assert.assertEquals("sky-new", created.get("skyflowId"));
        Assert.assertEquals("onboarding", created.get("tableName"));
        Assert.assertEquals(200, created.get("httpCode"));
        Assert.assertNull(created.get("error"));
        Assert.assertNull(created.get("requestId"));
        List<Map<String, Object>> columns = columnsOf(created);
        Assert.assertEquals("resumePDF", columns.get(0).get("column"));
        Assert.assertEquals("resume.pdf", columns.get(0).get("fileName"));
        Assert.assertEquals("UPLOADED", columns.get(0).get("uploadStatus"));
        Assert.assertNull(columns.get(0).get("error"));
        Assert.assertEquals("photo.jpg", columns.get(1).get("fileName"));
        Assert.assertEquals("FAILED", columns.get(1).get("uploadStatus"));
        Assert.assertEquals("PUT failed: 403", columns.get(1).get("error"));

        HashMap<String, Object> rejected = response.getRecords().get(1);
        Assert.assertEquals(400, rejected.get("httpCode"));
        Assert.assertEquals("Invalid request. skyflowID sky-bad is invalid.", rejected.get("error"));
        Assert.assertEquals("req-test-123", rejected.get("requestId"));
        Assert.assertEquals("SKIPPED", columnsOf(rejected).get(0).get("uploadStatus"));
        Assert.assertEquals("kyc.txt", columnsOf(rejected).get(0).get("fileName"));

        // Phase B: one PUT per signed URL with that column's bytes; the rejected record sends nothing
        Assert.assertEquals(2, uploader.bodyByUrl.size());
        Assert.assertEquals("resume bytes", uploader.bodyByUrl.get("https://upload.example.com/resumePDF?signed=t1"));
        Assert.assertEquals("photo", uploader.bodyByUrl.get("https://upload.example.com/photoID?signed=t2"));
        Assert.assertEquals("application/pdf",
                uploader.contentTypeByUrl.get("https://upload.example.com/resumePDF?signed=t1"));
        Assert.assertEquals("image/jpeg",
                uploader.contentTypeByUrl.get("https://upload.example.com/photoID?signed=t2"));
        // the signed URLs are internal credentials and must not reach the caller
        Assert.assertFalse(response.toString().contains("signed="));
    }

    @Test
    public void testUploadFiles_phaseARequestCarriesRecordsAndFileNames() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.files.uploadFiles(any(), any())).thenReturn(new ApiClientHttpResponse<>(
                uploadWire("{\"records\":[]}"), buildOkHttpResponse()));
        VaultController controller = createControllerWithMock(mockApi);
        controller.signedUrlUploader = new RecordingUploader();

        controller.uploadFiles(UploadFilesRequest.builder().records(Collections.singletonList(
                UploadFilesRequestRecord.builder().tableName("identityDocs").skyflowId("sky-1").columns(
                        Collections.singletonList(UploadFilesRequestColumn.builder().column("passport")
                                .base64("cA==").fileName("passport.png").build())).build())).build());

        ArgumentCaptor<com.skyflow.generated.rest.resources.files.requests.FileUploadRequest> captor =
                ArgumentCaptor.forClass(com.skyflow.generated.rest.resources.files.requests.FileUploadRequest.class);
        Mockito.verify(mockRaw.files).uploadFiles(captor.capture(), any());
        com.skyflow.generated.rest.resources.files.requests.FileUploadRequest sent = captor.getValue();
        Assert.assertEquals("vault123", sent.getVaultId());
        Assert.assertEquals("identityDocs", sent.getRecords().get(0).getTableName());
        Assert.assertEquals("sky-1", sent.getRecords().get(0).getSkyflowId().get());
        Assert.assertEquals("passport", sent.getRecords().get(0).getColumns().get(0).getColumn());
        Assert.assertEquals("passport.png", sent.getRecords().get(0).getColumns().get(0).getFileName().get());
    }

    @Test
    public void testUploadFiles_uploadErrorFailsAndMissingSignedUrlSkipsOnlyThatColumn() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.files.uploadFiles(any(), any())).thenReturn(new ApiClientHttpResponse<>(uploadWire(
                "{\"records\":[{\"skyflowID\":\"sky-1\",\"tableName\":\"onboarding\",\"httpCode\":200,"
                        + "\"data\":{\"a\":\"https://upload.example.com/a\",\"b\":\"https://upload.example.com/b\"}}]}"),
                buildOkHttpResponse()));
        VaultController controller = createControllerWithMock(mockApi);
        controller.signedUrlUploader = (url, body) -> {
            if (url.endsWith("/b")) throw new java.io.IOException("connection reset");
            return 201;
        };

        UploadFilesResponse response = controller.uploadFiles(UploadFilesRequest.builder().records(
                Collections.singletonList(UploadFilesRequestRecord.builder().tableName("onboarding").skyflowId("sky-1")
                        .columns(Arrays.asList(
                                UploadFilesRequestColumn.builder().column("a").base64("YQ==").fileName("a.txt").build(),
                                UploadFilesRequestColumn.builder().column("b").base64("Yg==").fileName("b.txt").build(),
                                UploadFilesRequestColumn.builder().column("c").base64("Yw==").fileName("c.txt").build()))
                        .build())).build());

        List<Map<String, Object>> columns = columnsOf(response.getRecords().get(0));
        Assert.assertEquals("UPLOADED", columns.get(0).get("uploadStatus"));
        Assert.assertEquals("FAILED", columns.get(1).get("uploadStatus"));
        Assert.assertEquals("PUT failed: connection reset", columns.get(1).get("error"));
        Assert.assertEquals("SKIPPED", columns.get(2).get("uploadStatus"));
        Assert.assertNull(columns.get(2).get("error"));
        Assert.assertNull(response.getRecords().get(0).get("error"));
    }

    @Test
    public void testUploadFiles_putsRunConcurrentlyCappedAtFiveAndKeepRequestOrder() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        int fileCount = 12;
        StringBuilder data = new StringBuilder();
        List<UploadFilesRequestColumn> requestColumns = new ArrayList<>();
        for (int i = 0; i < fileCount; i++) {
            data.append(i == 0 ? "" : ",").append("\"c").append(i).append("\":\"https://upload.example.com/c")
                    .append(i).append("\"");
            requestColumns.add(UploadFilesRequestColumn.builder().column("c" + i).base64("YQ==")
                    .fileName("f" + i + ".txt").build());
        }
        when(mockRaw.files.uploadFiles(any(), any())).thenReturn(new ApiClientHttpResponse<>(uploadWire(
                "{\"records\":[{\"skyflowID\":\"sky-1\",\"tableName\":\"onboarding\",\"httpCode\":200,"
                        + "\"data\":{" + data + "}}]}"), buildOkHttpResponse()));
        VaultController controller = createControllerWithMock(mockApi);
        java.util.concurrent.atomic.AtomicInteger inFlight = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger maxInFlight = new java.util.concurrent.atomic.AtomicInteger();
        controller.signedUrlUploader = (url, body) -> {
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            inFlight.decrementAndGet();
            return url.endsWith("/c3") ? 500 : 200;
        };

        UploadFilesResponse response = controller.uploadFiles(UploadFilesRequest.builder().records(
                Collections.singletonList(UploadFilesRequestRecord.builder().tableName("onboarding").skyflowId("sky-1")
                        .columns(requestColumns).build())).build());

        Assert.assertTrue("PUTs should overlap", maxInFlight.get() > 1);
        Assert.assertTrue("at most 5 PUTs at once", maxInFlight.get() <= 5);
        List<Map<String, Object>> columns = columnsOf(response.getRecords().get(0));
        Assert.assertEquals(fileCount, columns.size());
        for (int i = 0; i < fileCount; i++) {
            Assert.assertEquals("c" + i, columns.get(i).get("column"));
            Assert.assertEquals(i == 3 ? "FAILED" : "UPLOADED", columns.get(i).get("uploadStatus"));
        }
    }

    @Test
    public void testUploadFiles_wholeCallFailureThrowsAndUploadsNothing() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        ErrorResponse body = ObjectMappers.JSON_MAPPER.readValue(
                "{\"error\":{\"grpc_code\":7,\"http_code\":403,\"http_status\":\"Forbidden\","
                        + "\"message\":\"Permission denied.\"}}", ErrorResponse.class);
        when(mockRaw.files.uploadFiles(any(), any())).thenThrow(new ForbiddenError(body));
        VaultController controller = createControllerWithMock(mockApi);
        RecordingUploader uploader = new RecordingUploader();
        controller.signedUrlUploader = uploader;

        try {
            controller.uploadFiles(UploadFilesRequest.builder().records(Collections.singletonList(
                    UploadFilesRequestRecord.builder().tableName("onboarding").columns(Collections.singletonList(
                            UploadFilesRequestColumn.builder().column("kyc").base64("a3lj").fileName("kyc.txt").build()))
                            .build())).build());
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(403, e.getHttpCode());
            Assert.assertEquals("Permission denied.", e.getMessage());
        }
        Assert.assertTrue(uploader.bodyByUrl.isEmpty());
    }

    @Test
    public void testUploadFiles_rejectedCallThrowsEvenWhenTheBodyListsRecords() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        ErrorResponse body = ObjectMappers.JSON_MAPPER.readValue(
                "{\"records\":[{\"skyflowID\":null,\"tableName\":\"onboarding\",\"httpCode\":400,"
                        + "\"error\":\"Invalid request. skyflowID sky-x is invalid.\"}]}", ErrorResponse.class);
        when(mockRaw.files.uploadFiles(any(), any())).thenThrow(new BadRequestError(body));
        VaultController controller = createControllerWithMock(mockApi);
        RecordingUploader uploader = new RecordingUploader();
        controller.signedUrlUploader = uploader;

        try {
            controller.uploadFiles(UploadFilesRequest.builder().records(
                    Collections.singletonList(UploadFilesRequestRecord.builder().tableName("onboarding").skyflowId("sky-x")
                            .columns(Collections.singletonList(UploadFilesRequestColumn.builder().column("kyc")
                                    .base64("a3lj").fileName("kyc.txt").build())).build())).build());
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(400, e.getHttpCode());
            Assert.assertEquals("Invalid request. skyflowID sky-x is invalid.", e.getMessage());
        }
        Assert.assertTrue(uploader.bodyByUrl.isEmpty());
    }

    @Test
    public void testUploadFiles_invalidRequestThrowsWithoutCallingTheApi() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);
        try {
            controller.uploadFiles(UploadFilesRequest.builder().records(new ArrayList<>()).build());
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyUploadFilesRecords.getMessage(), e.getMessage());
        }
        Mockito.verifyNoInteractions(mockApi);
    }

    @Test
    public void testUploadFiles_invalidBase64ThrowsBeforeCallingTheApi() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);
        try {
            controller.uploadFiles(UploadFilesRequest.builder().records(Collections.singletonList(
                    UploadFilesRequestRecord.builder().tableName("onboarding").columns(Collections.singletonList(
                            UploadFilesRequestColumn.builder().column("kyc").base64("not base64!").fileName("kyc.txt").build()))
                            .build())).build());
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.InvalidBase64InUploadFilesColumn.getMessage(), e.getMessage());
        }
        Mockito.verifyNoInteractions(mockApi);
    }

    @Test
    public void testUploadFiles_interceptorAddsCustomHeaderToPhaseA() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.files.uploadFiles(any(), any())).thenReturn(new ApiClientHttpResponse<>(
                uploadWire("{\"records\":[]}"), buildOkHttpResponse()));
        VaultController controller = createControllerWithMock(mockApi);
        controller.signedUrlUploader = new RecordingUploader();
        List<String> operations = new ArrayList<>();
        RequestInterceptor interceptor = ctx -> {
            operations.add(ctx.getOperation());
            ctx.addHeader(CustomHeaderKey.SKYFLOW_ACCOUNT_ID, "acct-123");
        };

        controller.uploadFiles(UploadFilesRequest.builder().records(Collections.singletonList(
                UploadFilesRequestRecord.builder().tableName("onboarding").columns(Collections.singletonList(
                        UploadFilesRequestColumn.builder().column("kyc").base64("a3lj").fileName("kyc.txt").build()))
                        .build())).build(), UploadFilesOptions.builder().interceptor(interceptor).build());

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        Mockito.verify(mockRaw.files).uploadFiles(any(), captor.capture());
        Assert.assertEquals("acct-123", captor.getValue().getHeaders().get(CustomHeaderKey.SKYFLOW_ACCOUNT_ID.toString()));
        Assert.assertEquals(Collections.singletonList("UPLOAD_FILES"), operations);
    }

    @Test
    public void testUploadFiles_signedUrlPutSendsBytesWithoutAuthorization() throws Exception {
        List<String> seen = Collections.synchronizedList(new ArrayList<>());
        com.sun.net.httpserver.HttpServer server =
                com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/upload", exchange -> {
            java.io.ByteArrayOutputStream received = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            int n;
            while ((n = exchange.getRequestBody().read(chunk)) > 0) received.write(chunk, 0, n);
            seen.add(exchange.getRequestMethod());
            seen.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            seen.add(exchange.getRequestHeaders().getFirst("Content-Type"));
            seen.add(new String(received.toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
            seen.add(exchange.getRequestURI().getQuery());
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        server.start();
        try {
            VaultController controller = createControllerWithMock(Mockito.mock(ApiClient.class));
            // build the vault's own HTTP client, which adds a bearer token to every request
            java.lang.reflect.Method build = VaultClient.class.getDeclaredMethod("updateExecutorInHTTP");
            build.setAccessible(true);
            build.invoke(controller);

            int status = controller.signedUrlUploader.upload(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/upload?signed=token1",
                    okhttp3.RequestBody.create("file bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                            okhttp3.MediaType.parse("application/pdf")));

            Assert.assertEquals(201, status);
            Assert.assertEquals(Arrays.asList("PUT", "null", "application/pdf", "file bytes", "signed=token1"), seen);
        } finally {
            server.stop(0);
        }
    }

    // ── deleteFiles (unary) ───────────────────────────────────────────────────

    private static DeleteFilesRequest deleteFilesRequest() {
        return DeleteFilesRequest.builder().records(Collections.singletonList(
                DeleteFilesRequestRecord.builder().tableName("onboarding").skyflowId("sky-1")
                        .columns(Arrays.asList("resumePDF", "photoID")).build())).build();
    }

    @Test
    public void testDeleteFiles_partialSuccess() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        com.skyflow.generated.rest.types.FileDeleteResponse body = ObjectMappers.JSON_MAPPER.readValue(
                "{\"records\":["
                        + "{\"skyflowID\":\"sky-1\",\"tableName\":\"onboarding\",\"httpCode\":200,"
                        + "\"data\":{\"resumePDF\":\"DELETED\",\"photoID\":\"DELETED\"}},"
                        + "{\"skyflowID\":\"invalid-id-0000\",\"tableName\":\"employees\",\"httpCode\":404,"
                        + "\"error\":\"Invalid request. skyflowID invalid-id-0000 is invalid.\"}]}",
                com.skyflow.generated.rest.types.FileDeleteResponse.class);
        when(mockRaw.files.deleteFiles(any(), any())).thenReturn(new ApiClientHttpResponse<>(body, buildOkHttpResponse()));
        VaultController controller = createControllerWithMock(mockApi);

        DeleteFilesResponse response = controller.deleteFiles(deleteFilesRequest());

        Assert.assertEquals(2, response.getRecords().size());
        HashMap<String, Object> ok = response.getRecords().get(0);
        Assert.assertEquals("sky-1", ok.get("skyflowId"));
        Assert.assertEquals("DELETED", columnsOf(ok).get(1).get("status"));
        Assert.assertNull(ok.get("requestId"));
        HashMap<String, Object> failed = response.getRecords().get(1);
        Assert.assertNull(failed.get("columns"));
        Assert.assertEquals(404, failed.get("httpCode"));
        Assert.assertEquals("req-test-123", failed.get("requestId"));

        ArgumentCaptor<com.skyflow.generated.rest.resources.files.requests.FileDeleteRequest> captor =
                ArgumentCaptor.forClass(com.skyflow.generated.rest.resources.files.requests.FileDeleteRequest.class);
        Mockito.verify(mockRaw.files).deleteFiles(captor.capture(), any());
        Assert.assertEquals("vault123", captor.getValue().getVaultId());
        Assert.assertEquals(Arrays.asList("resumePDF", "photoID"), captor.getValue().getRecords().get(0).getColumns());
    }

    @Test
    public void testDeleteFiles_recordsShapedTypedErrorReturnedAsResponse() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        ErrorResponse body = ObjectMappers.JSON_MAPPER.readValue(
                "{\"records\":[{\"skyflowID\":\"sky-1\",\"tableName\":\"onboarding\",\"httpCode\":404,"
                        + "\"error\":\"No file present in column photoID.\"}]}", ErrorResponse.class);
        when(mockRaw.files.deleteFiles(any(), any())).thenThrow(new NotFoundError(body));
        VaultController controller = createControllerWithMock(mockApi);

        DeleteFilesResponse response = controller.deleteFiles(deleteFilesRequest());

        Assert.assertEquals(404, response.getRecords().get(0).get("httpCode"));
        Assert.assertEquals("No file present in column photoID.", response.getRecords().get(0).get("error"));
    }

    @Test
    public void testDeleteFiles_wholeCallFailureThrows() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        ErrorResponse body = ObjectMappers.JSON_MAPPER.readValue(
                "{\"error\":{\"grpc_code\":16,\"http_code\":401,\"http_status\":\"Unauthorized\","
                        + "\"message\":\"Invalid token.\"}}", ErrorResponse.class);
        when(mockRaw.files.deleteFiles(any(), any())).thenThrow(new UnauthorizedError(body));
        VaultController controller = createControllerWithMock(mockApi);
        try {
            controller.deleteFiles(deleteFilesRequest());
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(401, e.getHttpCode());
            Assert.assertEquals("Invalid token.", e.getMessage());
        }
    }

    @Test
    public void testDeleteFiles_invalidRequestThrowsWithoutCallingTheApi() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);
        try {
            controller.deleteFiles(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.DeleteFilesRequestNull.getMessage(), e.getMessage());
        }
        Mockito.verifyNoInteractions(mockApi);
    }

    @Test
    public void testDeleteFiles_interceptorAddsCustomHeader() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.files.deleteFiles(any(), any())).thenReturn(new ApiClientHttpResponse<>(
                com.skyflow.generated.rest.types.FileDeleteResponse.builder().build(), buildOkHttpResponse()));
        VaultController controller = createControllerWithMock(mockApi);
        List<String> operations = new ArrayList<>();
        RequestInterceptor interceptor = ctx -> {
            operations.add(ctx.getOperation());
            ctx.addHeader(CustomHeaderKey.SKYFLOW_ACCOUNT_ID, "acct-123");
        };

        controller.deleteFiles(deleteFilesRequest(), DeleteFilesOptions.builder().interceptor(interceptor).build());

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        Mockito.verify(mockRaw.files).deleteFiles(any(), captor.capture());
        Assert.assertEquals("acct-123", captor.getValue().getHeaders().get(CustomHeaderKey.SKYFLOW_ACCOUNT_ID.toString()));
        Assert.assertEquals(Collections.singletonList("DELETE_FILES"), operations);
    }

    // ── bulkInsert ────────────────────────────────────────────────────────────

    @Test
    public void testBulkInsert_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "tok-abc");
        RecordResponseObject record = RecordResponseObject.builder().httpCode(200).skyflowId("sky-id-1").tokens(tokens).build();
        com.skyflow.generated.rest.types.InsertResponse body = com.skyflow.generated.rest.types.InsertResponse.builder().records(Collections.singletonList(record)).build();
        ApiClientHttpResponse<com.skyflow.generated.rest.types.InsertResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.records.insertRecords(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder().tableName("table1").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();

        BulkInsertResponse response = controller.bulkInsert(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals("sky-id-1", response.getRecords().get(0).getSkyflowId());
        Assert.assertNull(response.getRecords().get(0).getError());
        Assert.assertEquals(1, response.getSummary().getTotalRecords());
        Assert.assertEquals(1, response.getSummary().getTotalInserted());
        Assert.assertEquals(0, response.getSummary().getTotalFailed());
    }

    @Test
    public void testBulkInsert_invalidRequestThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);
        BulkInsertRequest request = BulkInsertRequest.builder().records(new ArrayList<>()).build();
        try {
            controller.bulkInsert(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testBulkInsertAsync_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "tok-abc");
        RecordResponseObject record = RecordResponseObject.builder().httpCode(200).skyflowId("sky-id-1").tokens(tokens).build();
        com.skyflow.generated.rest.types.InsertResponse body = com.skyflow.generated.rest.types.InsertResponse.builder().records(Collections.singletonList(record)).build();
        ApiClientHttpResponse<com.skyflow.generated.rest.types.InsertResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.records.insertRecords(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder().tableName("table1").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();

        BulkInsertResponse response = controller.bulkInsertAsync(request).get(5, TimeUnit.SECONDS);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals("sky-id-1", response.getRecords().get(0).getSkyflowId());
    }

    @Test
    public void testBulkInsertAsync_unexpectedExceptionWrappedAsSkyflowException() throws Exception {
        // Regression test: bulkInsertAsync's synchronous setup (before the batch futures exist)
        // must wrap any unexpected exception in SkyflowException, same as every other bulk async
        // method - not just ApiClientApiException. A RequestInterceptor is invoked synchronously
        // per batch inside insertBatchFutures, so a caller interceptor that throws is a realistic
        // way to trigger this without reaching into internals.
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder().tableName("table1").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();

        RequestInterceptor interceptor = ctx -> {
            throw new IllegalStateException("interceptor blew up");
        };
        BulkInsertOptions options = BulkInsertOptions.builder().interceptor(interceptor).build();

        try {
            controller.bulkInsertAsync(request, options);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals("interceptor blew up", e.getMessage());
        } catch (IllegalStateException e) {
            Assert.fail("Expected SkyflowException, got IllegalStateException");
        }
    }

    @Test
    public void testBulkInsert_interceptorAddsCustomHeader() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "tok-abc");
        RecordResponseObject record = RecordResponseObject.builder().httpCode(200).skyflowId("sky-id-1").tokens(tokens).build();
        com.skyflow.generated.rest.types.InsertResponse body = com.skyflow.generated.rest.types.InsertResponse.builder().records(Collections.singletonList(record)).build();
        ApiClientHttpResponse<com.skyflow.generated.rest.types.InsertResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.records.insertRecords(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder().tableName("table1").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();

        RequestInterceptor interceptor = ctx -> ctx.addHeader(CustomHeaderKey.SKYFLOW_ACCOUNT_ID, "acct-123");
        BulkInsertOptions options = BulkInsertOptions.builder().interceptor(interceptor).build();

        controller.bulkInsert(request, options);

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        Mockito.verify(mockRaw.records).insertRecords(any(), captor.capture());
        Assert.assertEquals("acct-123", captor.getValue().getHeaders().get(CustomHeaderKey.SKYFLOW_ACCOUNT_ID.toString()));
    }

    // ── bulkDetokenize ────────────────────────────────────────────────────────

    @Test
    public void testBulkDetokenize_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        DetokenizeResponseObject record = DetokenizeResponseObject.builder()
                .token("token1").value(com.skyflow.generated.rest.types.GoogleProtobufValue.of("secret-value")).build();
        com.skyflow.generated.rest.types.DetokenizeResponse body = com.skyflow.generated.rest.types.DetokenizeResponse.builder()
                .response(Collections.singletonList(record)).build();
        ApiClientHttpResponse<com.skyflow.generated.rest.types.DetokenizeResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.tokens.detokenize(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .build();

        BulkDetokenizeResponse response = controller.bulkDetokenize(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals("token1", response.getRecords().get(0).getToken());
        Assert.assertNull(response.getRecords().get(0).getError());
        Assert.assertEquals(1, response.getSummary().getTotalDetokenized());
        Assert.assertEquals(0, response.getSummary().getTotalFailed());
    }

    @Test
    public void testBulkDetokenize_nullRequestThrowsSkyflowExceptionNotNPE() throws Exception {
        // Regression test: configureDetokenizeConcurrencyAndBatchSize() must run AFTER validation,
        // otherwise detokenizeRequest.getTokens().size() NPEs on a null request before validation
        // has a chance to reject it gracefully.
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);
        try {
            controller.bulkDetokenize(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        } catch (NullPointerException e) {
            Assert.fail("Expected SkyflowException, got NullPointerException");
        }
    }

    @Test
    public void testBulkDetokenizeAsync_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        DetokenizeResponseObject record = DetokenizeResponseObject.builder()
                .token("token1").value(com.skyflow.generated.rest.types.GoogleProtobufValue.of("secret-value")).build();
        com.skyflow.generated.rest.types.DetokenizeResponse body = com.skyflow.generated.rest.types.DetokenizeResponse.builder()
                .response(Collections.singletonList(record)).build();
        ApiClientHttpResponse<com.skyflow.generated.rest.types.DetokenizeResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.tokens.detokenize(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .build();

        BulkDetokenizeResponse response = controller.bulkDetokenizeAsync(request).get(5, TimeUnit.SECONDS);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertNull(response.getRecords().get(0).getError());
    }

    @Test
    public void testBulkDetokenizeAsync_nullRequestThrowsSkyflowExceptionNotNPE() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);
        try {
            controller.bulkDetokenizeAsync(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        } catch (NullPointerException e) {
            Assert.fail("Expected SkyflowException, got NullPointerException");
        }
    }

    // ── bulkDeleteTokens ──────────────────────────────────────────────────────

    @Test
    public void testBulkDeleteTokens_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        DeleteTokenResponseObject record = DeleteTokenResponseObject.builder().value("token1").build();
        DeleteTokenResponse body = DeleteTokenResponse.builder()
                .tokens(Collections.singletonList(record)).build();
        ApiClientHttpResponse<DeleteTokenResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.tokens.deleteToken(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        BulkDeleteTokensRequest request = BulkDeleteTokensRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .build();

        BulkDeleteTokensResponse response = controller.bulkDeleteTokens(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals("token1", response.getRecords().get(0).getToken());
        Assert.assertNull(response.getRecords().get(0).getError());
        Assert.assertEquals(Integer.valueOf(200), response.getRecords().get(0).getHttpCode());
    }

    @Test
    public void testBulkDeleteTokens_nullRequestThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);
        try {
            controller.bulkDeleteTokens(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testBulkDeleteTokensAsync_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        DeleteTokenResponseObject record = DeleteTokenResponseObject.builder().value("token1").build();
        DeleteTokenResponse body = DeleteTokenResponse.builder()
                .tokens(Collections.singletonList(record)).build();
        ApiClientHttpResponse<DeleteTokenResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.tokens.deleteToken(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        BulkDeleteTokensRequest request = BulkDeleteTokensRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .build();

        BulkDeleteTokensResponse response = controller.bulkDeleteTokensAsync(request).get(5, TimeUnit.SECONDS);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertNull(response.getRecords().get(0).getError());
    }

    // ── bulkTokenize ──────────────────────────────────────────────────────────

    @Test
    public void testBulkTokenize_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        TokenizeResponseObject responseObject = TokenizeResponseObject.builder().token("tok-abc")
                .value(com.skyflow.generated.rest.types.GoogleProtobufValue.of("value1")).tokenGroupName("group1").build();
        com.skyflow.generated.rest.types.TokenizeResponse body = com.skyflow.generated.rest.types.TokenizeResponse.builder()
                .response(Collections.singletonList(responseObject)).build();
        ApiClientHttpResponse<com.skyflow.generated.rest.types.TokenizeResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.tokens.tokenize(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        List<BulkTokenizeRequestRecord> records = Collections.singletonList(
                BulkTokenizeRequestRecord.builder().value("value1")
                        .tokenGroupNames(Collections.singletonList("group1")).build());
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().records(records).build();

        BulkTokenizeResponse response = controller.bulkTokenize(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals(0, response.getRecords().get(0).getIndex());
        Assert.assertEquals("tok-abc", response.getRecords().get(0).getToken());
        Assert.assertNull(response.getRecords().get(0).getError());
    }

    @Test
    public void testBulkTokenize_partialFailureWithRetryableGroupIsRetriedAsWhole() throws Exception {
        // one value, two groups: group1 succeeds, group2 fails with a retryable 503
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        TokenizeResponseObject succeeded = TokenizeResponseObject.builder().token("tok-abc")
                .value(com.skyflow.generated.rest.types.GoogleProtobufValue.of("value1")).tokenGroupName("group1").httpCode(200).build();
        TokenizeResponseObject failed = TokenizeResponseObject.builder().token("")
                .value(com.skyflow.generated.rest.types.GoogleProtobufValue.of("value1")).tokenGroupName("").error("service unavailable").httpCode(503).build();
        com.skyflow.generated.rest.types.TokenizeResponse body = com.skyflow.generated.rest.types.TokenizeResponse.builder()
                .response(Arrays.asList(succeeded, failed)).build();
        ApiClientHttpResponse<com.skyflow.generated.rest.types.TokenizeResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.tokens.tokenize(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        BulkTokenizeRequestRecord requested = BulkTokenizeRequestRecord.builder().value("value1")
                .tokenGroupNames(Arrays.asList("group1", "group2")).build();
        BulkTokenizeRequest request = BulkTokenizeRequest.builder()
                .records(Collections.singletonList(requested)).build();

        BulkTokenizeResponse response = controller.bulkTokenize(request);

        Assert.assertEquals(2, response.getRecords().size());
        Assert.assertEquals(0, response.getRecords().get(0).getIndex());
        Assert.assertEquals(0, response.getRecords().get(1).getIndex());
        Assert.assertEquals(1, response.getSummary().getTotalPartial());

        List<BulkTokenizeRequestRecord> retry = response.getRecordsToRetry();
        Assert.assertEquals(1, retry.size());
        Assert.assertSame("must return the caller's own record, groups and all", requested, retry.get(0));
    }

    @Test
    public void testBulkTokenize_nullRequestThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);
        try {
            controller.bulkTokenize(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testBulkTokenizeAsync_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        TokenizeResponseObject responseObject = TokenizeResponseObject.builder().token("tok-abc")
                .value(com.skyflow.generated.rest.types.GoogleProtobufValue.of("value1")).tokenGroupName("group1").build();
        com.skyflow.generated.rest.types.TokenizeResponse body = com.skyflow.generated.rest.types.TokenizeResponse.builder()
                .response(Collections.singletonList(responseObject)).build();
        ApiClientHttpResponse<com.skyflow.generated.rest.types.TokenizeResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.tokens.tokenize(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        List<BulkTokenizeRequestRecord> records = Collections.singletonList(
                BulkTokenizeRequestRecord.builder().value("value1")
                        .tokenGroupNames(Collections.singletonList("group1")).build());
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().records(records).build();

        BulkTokenizeResponse response = controller.bulkTokenizeAsync(request).get(5, TimeUnit.SECONDS);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertNull(response.getRecords().get(0).getError());
    }

    // ── additional bulk API-error coverage ───────────────────────────────────
    //
    // Note: unlike the singular insert/detokenize/tokenize/deleteTokens calls, the bulk*
    // entry points recover a per-batch ApiClientApiException into the response's errors list
    // (via Utils.handleBulkXxxBatchException, invoked from the CompletableFuture
    // .exceptionally()/.handle() callbacks inside VaultController's *BatchFutures helpers)
    // instead of letting a SkyflowException escape the call. That is a deliberate resilience
    // design (a single failed batch shouldn't fail an entire bulk request), so these tests
    // assert the errors-list outcome rather than a thrown exception.

    @Test
    public void testBulkInsertAsync_apiErrorCapturedInErrors() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.records.insertRecords(any(), any()))
                .thenThrow(new ApiClientApiException("insert failed", 401, "unauthorized"));

        VaultController controller = createControllerWithMock(mockApi);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder().tableName("table1").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();

        BulkInsertResponse response = controller.bulkInsertAsync(request).get(5, TimeUnit.SECONDS);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals(401, response.getRecords().get(0).getHttpCode());
        Assert.assertNotNull(response.getRecords().get(0).getError());
        Assert.assertNull(response.getRecords().get(0).getSkyflowId());
    }

    @Test
    public void testBulkDeleteTokens_apiErrorCapturedInErrors() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.tokens.deleteToken(any(), any()))
                .thenThrow(new ApiClientApiException("delete failed", 404, "not found"));

        VaultController controller = createControllerWithMock(mockApi);

        BulkDeleteTokensRequest request = BulkDeleteTokensRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .build();

        BulkDeleteTokensResponse response = controller.bulkDeleteTokens(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals(Integer.valueOf(404), response.getRecords().get(0).getHttpCode());
        Assert.assertNotNull(response.getRecords().get(0).getError());
        Assert.assertEquals("token1", response.getRecords().get(0).getToken());
    }

    @Test
    public void testBulkDeleteTokensAsync_apiErrorCapturedInErrors() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.tokens.deleteToken(any(), any()))
                .thenThrow(new ApiClientApiException("delete failed", 404, "not found"));

        VaultController controller = createControllerWithMock(mockApi);

        BulkDeleteTokensRequest request = BulkDeleteTokensRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .build();

        BulkDeleteTokensResponse response = controller.bulkDeleteTokensAsync(request).get(5, TimeUnit.SECONDS);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals(Integer.valueOf(404), response.getRecords().get(0).getHttpCode());
        Assert.assertNotNull(response.getRecords().get(0).getError());
        Assert.assertEquals("token1", response.getRecords().get(0).getToken());
    }

    @Test
    public void testBulkTokenize_apiErrorCapturedInErrors() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.tokens.tokenize(any(), any()))
                .thenThrow(new ApiClientApiException("tokenize failed", 400, "bad request"));

        VaultController controller = createControllerWithMock(mockApi);

        List<BulkTokenizeRequestRecord> records = Collections.singletonList(
                BulkTokenizeRequestRecord.builder().value("value1")
                        .tokenGroupNames(Collections.singletonList("group1")).build());
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().records(records).build();

        BulkTokenizeResponse response = controller.bulkTokenize(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals(0, response.getRecords().get(0).getIndex());
        Assert.assertEquals(Integer.valueOf(400), response.getRecords().get(0).getHttpCode());
        Assert.assertNotNull(response.getRecords().get(0).getError());
    }

    @Test
    public void testBulkTokenizeAsync_apiErrorCapturedInErrors() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.tokens.tokenize(any(), any()))
                .thenThrow(new ApiClientApiException("tokenize failed", 400, "bad request"));

        VaultController controller = createControllerWithMock(mockApi);

        List<BulkTokenizeRequestRecord> records = Collections.singletonList(
                BulkTokenizeRequestRecord.builder().value("value1")
                        .tokenGroupNames(Collections.singletonList("group1")).build());
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().records(records).build();

        BulkTokenizeResponse response = controller.bulkTokenizeAsync(request).get(5, TimeUnit.SECONDS);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals(0, response.getRecords().get(0).getIndex());
        Assert.assertEquals(Integer.valueOf(400), response.getRecords().get(0).getHttpCode());
        Assert.assertNotNull(response.getRecords().get(0).getError());
    }

    @Test
    public void testBulkDetokenizeAsync_apiErrorCapturedInErrors() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.tokens.detokenize(any(), any()))
                .thenThrow(new ApiClientApiException("detokenize failed", 401, "unauthorized"));

        VaultController controller = createControllerWithMock(mockApi);

        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .build();

        BulkDetokenizeResponse response = controller.bulkDetokenizeAsync(request).get(5, TimeUnit.SECONDS);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals(401, response.getRecords().get(0).getHttpCode());
        Assert.assertNotNull(response.getRecords().get(0).getError());
        Assert.assertEquals(1, response.getSummary().getTotalFailed());
        Assert.assertEquals(0, response.getSummary().getTotalDetokenized());
    }

    // ── multi-batch aggregation / partial-batch failure ──────────────────────

    @Test
    public void testBulkInsert_multiBatchAggregatesAcrossBatches() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        Map<String, Object> tokens1 = new HashMap<>();
        tokens1.put("name", "tok-batch1");
        RecordResponseObject record1 = RecordResponseObject.builder().httpCode(200).skyflowId("sky-id-batch1").tokens(tokens1).build();
        com.skyflow.generated.rest.types.InsertResponse body1 = com.skyflow.generated.rest.types.InsertResponse.builder().records(Collections.singletonList(record1)).build();
        ApiClientHttpResponse<com.skyflow.generated.rest.types.InsertResponse> httpResp1 = new ApiClientHttpResponse<>(body1, buildOkHttpResponse());

        Map<String, Object> tokens2 = new HashMap<>();
        tokens2.put("name", "tok-batch2");
        RecordResponseObject record2 = RecordResponseObject.builder().httpCode(200).skyflowId("sky-id-batch2").tokens(tokens2).build();
        com.skyflow.generated.rest.types.InsertResponse body2 = com.skyflow.generated.rest.types.InsertResponse.builder().records(Collections.singletonList(record2)).build();
        ApiClientHttpResponse<com.skyflow.generated.rest.types.InsertResponse> httpResp2 = new ApiClientHttpResponse<>(body2, buildOkHttpResponse());

        when(mockRaw.records.insertRecords(any(), any())).thenReturn(httpResp1).thenReturn(httpResp2);

        VaultController controller = createControllerWithMock(mockApi);

        // Constants.INSERT_BATCH_SIZE defaults to 50, so 75 records forces exactly two batches
        // (50 + 25) — processBulkInsertSync/insertBatchFutures must merge the per-batch record
        // lists collected from more than one CompletableFuture into a single BulkInsertResponse.
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("name", "john" + i);
            records.add(BulkInsertRequestRecord.builder().tableName("table1").data(data).build());
        }
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();

        BulkInsertResponse response = controller.bulkInsert(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Mockito.verify(mockRaw.records, Mockito.times(2)).insertRecords(any(), any());
        Assert.assertEquals(2, response.getRecords().size());
        Assert.assertTrue(response.getRecords().stream().allMatch(r -> r.getError() == null));
        Assert.assertEquals(75, response.getSummary().getTotalRecords());
        Assert.assertEquals(2, response.getSummary().getTotalInserted());
        Assert.assertEquals(0, response.getSummary().getTotalFailed());

        BulkInsertResponseRecord batch1Success = response.getRecords().stream()
                .filter(s -> "sky-id-batch1".equals(s.getSkyflowId())).findFirst().orElse(null);
        BulkInsertResponseRecord batch2Success = response.getRecords().stream()
                .filter(s -> "sky-id-batch2".equals(s.getSkyflowId())).findFirst().orElse(null);
        Assert.assertNotNull(batch1Success);
        Assert.assertNotNull(batch2Success);
        // Index offsets prove the batch-number * batchSize aggregation math in
        // Utils.formatBulkInsertResponse: batch 0 starts at index 0, batch 1 at index 50.
        Assert.assertEquals(0, batch1Success.getIndex());
        Assert.assertEquals(50, batch2Success.getIndex());
    }

    @Test
    public void testBulkInsert_partialBatchFailureKeepsBatch1SuccessAndBatch2Error() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        Map<String, Object> tokens1 = new HashMap<>();
        tokens1.put("name", "tok-batch1");
        RecordResponseObject record1 = RecordResponseObject.builder().httpCode(200).skyflowId("sky-id-batch1").tokens(tokens1).build();
        com.skyflow.generated.rest.types.InsertResponse body1 = com.skyflow.generated.rest.types.InsertResponse.builder().records(Collections.singletonList(record1)).build();
        ApiClientHttpResponse<com.skyflow.generated.rest.types.InsertResponse> httpResp1 = new ApiClientHttpResponse<>(body1, buildOkHttpResponse());

        when(mockRaw.records.insertRecords(any(), any()))
                .thenReturn(httpResp1)
                .thenThrow(new ApiClientApiException("insert failed", 500, "server error"));

        VaultController controller = createControllerWithMock(mockApi);

        // 51 records -> batch 1 has 50 records (succeeds), batch 2 has 1 record (fails).
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("name", "john" + i);
            records.add(BulkInsertRequestRecord.builder().tableName("table1").data(data).build());
        }
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();

        BulkInsertResponse response = controller.bulkInsert(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Mockito.verify(mockRaw.records, Mockito.times(2)).insertRecords(any(), any());

        Assert.assertEquals(2, response.getRecords().size());

        BulkInsertResponseRecord inserted = response.getRecords().stream()
                .filter(r -> r.getError() == null).findFirst().orElse(null);
        Assert.assertNotNull(inserted);
        Assert.assertEquals("sky-id-batch1", inserted.getSkyflowId());
        Assert.assertEquals(0, inserted.getIndex());

        BulkInsertResponseRecord failed = response.getRecords().stream()
                .filter(r -> r.getError() != null).findFirst().orElse(null);
        Assert.assertNotNull(failed);
        Assert.assertEquals(50, failed.getIndex());
        Assert.assertEquals(500, failed.getHttpCode());

        Assert.assertEquals(51, response.getSummary().getTotalRecords());
        Assert.assertEquals(1, response.getSummary().getTotalInserted());
        Assert.assertEquals(1, response.getSummary().getTotalFailed());
    }

    // ── formatBulkInsertResponse: List<Map> token shape ──────────────────────

    @Test
    public void testBulkInsert_successWithListOfMapsTokenShape() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);

        Map<String, Object> tokenEntry = new HashMap<>();
        tokenEntry.put("token", "tok-xyz");
        tokenEntry.put("tokenGroupName", "group1");
        List<Map<String, Object>> tokenList = new ArrayList<>();
        tokenList.add(tokenEntry);

        Map<String, Object> tokens = new HashMap<>();
        tokens.put("field1", tokenList);

        RecordResponseObject record = RecordResponseObject.builder().httpCode(200).skyflowId("sky-id-1").tokens(tokens).build();
        com.skyflow.generated.rest.types.InsertResponse body = com.skyflow.generated.rest.types.InsertResponse.builder().records(Collections.singletonList(record)).build();
        ApiClientHttpResponse<com.skyflow.generated.rest.types.InsertResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.records.insertRecords(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder().tableName("table1").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();

        BulkInsertResponse response = controller.bulkInsert(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getRecords().size());

        BulkInsertResponseRecord inserted = response.getRecords().get(0);
        Assert.assertNotNull(inserted.getTokens());
        // getFields() is deprecated and now renders getTokens()'s typed data back into its
        // original raw shape - a lossless round trip for this input, so it equals the raw map
        // the mock returned in the first place.
        Assert.assertEquals(tokens, inserted.getFields());
        // The wire type's List<Map> token shape is parsed into typed Token objects - no casting.
        List<Token> field1Tokens = inserted.getTokens().get("field1");
        Assert.assertEquals(1, field1Tokens.size());
        Assert.assertEquals("tok-xyz", field1Tokens.get(0).getToken());
        Assert.assertEquals("group1", field1Tokens.get(0).getTokenGroupName());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Request fidelity through batch dispatch
    //
    // Constants.INSERT/DETOKENIZE/TOKENIZE/DELETE_TOKENS_BATCH_SIZE all default to 50, so a
    // 120-item request produces exactly three batches (50 + 50 + 20). These tests assert that
    // (a) non-batched fields are re-applied to EVERY outgoing batch request, (b) item order is
    // preserved end to end, (c) the SDK-assigned response index equals the item's position in
    // the ORIGINAL user list, and (d) a registered interceptor runs once per batch.
    // ─────────────────────────────────────────────────────────────────────────

    private static final int MULTI_BATCH_ITEM_COUNT = 120;
    private static final int EXPECTED_BATCH_COUNT = 3;

    /** Interceptor that records each RequestContext it is handed and stamps a per-batch header. */
    private static final class CountingInterceptor implements RequestInterceptor {
        private final List<com.skyflow.vault.data.RequestContext> contexts =
                java.util.Collections.synchronizedList(new ArrayList<>());

        @Override
        public void intercept(com.skyflow.vault.data.RequestContext context) {
            int callNumber;
            synchronized (contexts) {
                callNumber = contexts.size();
                contexts.add(context);
            }
            context.addHeader(CustomHeaderKey.SKYFLOW_ACCOUNT_ID, "batch-" + callNumber);
        }

        int callCount() {
            return contexts.size();
        }

        List<com.skyflow.vault.data.RequestContext> contexts() {
            return contexts;
        }
    }

    private static void assertInterceptorRanOncePerBatch(CountingInterceptor interceptor,
                                                         List<RequestOptions> capturedOptions) {
        Assert.assertEquals(EXPECTED_BATCH_COUNT, interceptor.callCount());
        // Each batch must get its own RequestContext — never a shared/reused one.
        java.util.Set<Integer> identities = new java.util.HashSet<>();
        for (com.skyflow.vault.data.RequestContext ctx : interceptor.contexts()) {
            identities.add(System.identityHashCode(ctx));
        }
        Assert.assertEquals(EXPECTED_BATCH_COUNT, identities.size());

        // Each context must also report where its batch sits in the request, so an interceptor can
        // tag batches apart (per-batch correlation id, "batch 3 of 12" logging). Every index in
        // 0..n-1 must appear exactly once, and every context must agree on the total.
        java.util.Set<Integer> batchIndexes = new java.util.HashSet<>();
        for (com.skyflow.vault.data.RequestContext ctx : interceptor.contexts()) {
            Assert.assertEquals("totalBatches must be the real batch count",
                    EXPECTED_BATCH_COUNT, ctx.getTotalBatches());
            Assert.assertTrue("batchIndex out of range: " + ctx.getBatchIndex(),
                    ctx.getBatchIndex() >= 0 && ctx.getBatchIndex() < EXPECTED_BATCH_COUNT);
            batchIndexes.add(ctx.getBatchIndex());
        }
        Assert.assertEquals("every batch position must appear exactly once",
                EXPECTED_BATCH_COUNT, batchIndexes.size());

        // The header the interceptor set on each context must reach that batch's RequestOptions.
        Assert.assertEquals(EXPECTED_BATCH_COUNT, capturedOptions.size());
        java.util.Set<String> headerValues = new java.util.HashSet<>();
        for (RequestOptions options : capturedOptions) {
            String value = options.getHeaders().get(CustomHeaderKey.SKYFLOW_ACCOUNT_ID.toString());
            Assert.assertNotNull("Interceptor header missing on a batch", value);
            headerValues.add(value);
        }
        Assert.assertEquals(
                new java.util.HashSet<>(java.util.Arrays.asList("batch-0", "batch-1", "batch-2")),
                headerValues);
    }

    private static ArrayList<InsertRequestRecord> multiBatchInsertRecords() {
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        for (int i = 0; i < MULTI_BATCH_ITEM_COUNT; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("pos", String.valueOf(i));
            records.add(BulkInsertRequestRecord.builder().data(data).build());
        }
        return records;
    }

    private static List<String> multiBatchTokens() {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < MULTI_BATCH_ITEM_COUNT; i++) {
            tokens.add("token-" + i);
        }
        return tokens;
    }

    /** Echoes each request record back as a response record whose skyflowId encodes its "pos". */
    private static void stubInsertEcho(MockRaw mockRaw) {
        when(mockRaw.records.insertRecords(any(), any())).thenAnswer(invocation -> {
            com.skyflow.generated.rest.resources.records.requests.InsertRequest req =
                    invocation.getArgument(0);
            List<RecordResponseObject> responseRecords = new ArrayList<>();
            for (com.skyflow.generated.rest.types.InsertRecordData record : req.getRecords()) {
                responseRecords.add(RecordResponseObject.builder().httpCode(200)
                        .skyflowId("sky-" + record.getData().get("pos"))
                        .tableName(record.getTableName().orElse(null))
                        .build());
            }
            com.skyflow.generated.rest.types.InsertResponse body = com.skyflow.generated.rest.types.InsertResponse.builder().records(responseRecords).build();
            return new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        });
    }

    /** Echoes each requested token back as a detokenize response record. */
    private static void stubDetokenizeEcho(MockRaw mockRaw) {
        when(mockRaw.tokens.detokenize(any(), any())).thenAnswer(invocation -> {
            com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest req =
                    invocation.getArgument(0);
            List<DetokenizeResponseObject> responseRecords = new ArrayList<>();
            for (String token : req.getTokens()) {
                responseRecords.add(DetokenizeResponseObject.builder().token(token).build());
            }
            com.skyflow.generated.rest.types.DetokenizeResponse body = com.skyflow.generated.rest.types.DetokenizeResponse.builder()
                    .response(responseRecords).build();
            return new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        });
    }

    /** Echoes each requested token back as a delete-token response record. */
    private static void stubDeleteTokensEcho(MockRaw mockRaw) {
        when(mockRaw.tokens.deleteToken(any(), any())).thenAnswer(invocation -> {
            com.skyflow.generated.rest.resources.tokens.requests.DeleteTokenRequest req =
                    invocation.getArgument(0);
            List<DeleteTokenResponseObject> responseRecords = new ArrayList<>();
            for (String token : req.getTokens()) {
                responseRecords.add(DeleteTokenResponseObject.builder().value(token).build());
            }
            DeleteTokenResponse body = DeleteTokenResponse.builder()
                    .tokens(responseRecords).build();
            return new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        });
    }

    /** Echoes each requested tokenize value back with one token per requested group name. */
    private static void stubTokenizeEcho(MockRaw mockRaw) {
        when(mockRaw.tokens.tokenize(any(), any())).thenAnswer(invocation -> {
            com.skyflow.generated.rest.resources.tokens.requests.TokenizeRequest req =
                    invocation.getArgument(0);
            List<TokenizeResponseObject> responseRecords = new ArrayList<>();
            for (com.skyflow.generated.rest.types.TokenizeRequestObject obj : req.getData()) {
                responseRecords.add(TokenizeResponseObject.builder()
                        .token("tok-" + obj.getValue().get())
                        .value(com.skyflow.generated.rest.types.GoogleProtobufValue.of(obj.getValue().get()))
                        .tokenGroupName("group1")
                        .build());
            }
            com.skyflow.generated.rest.types.TokenizeResponse body = com.skyflow.generated.rest.types.TokenizeResponse.builder().response(responseRecords).build();
            return new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        });
    }

    // ── bulk insert: batch dispatch fidelity ─────────────────────────────────

    @Test
    public void testBulkInsert_tableNameAndVaultIdReAppliedOnEveryBatch() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        stubInsertEcho(mockRaw);

        VaultController controller = createControllerWithMock(mockApi);
        BulkInsertRequest request = BulkInsertRequest.builder()
                .tableName("cards")
                .records(multiBatchInsertRecords())
                .build();

        controller.bulkInsert(request);

        ArgumentCaptor<com.skyflow.generated.rest.resources.records.requests.InsertRequest> captor =
                ArgumentCaptor.forClass(com.skyflow.generated.rest.resources.records.requests.InsertRequest.class);
        Mockito.verify(mockRaw.records, Mockito.times(EXPECTED_BATCH_COUNT)).insertRecords(captor.capture(), any());

        int expectedPos = 0;
        for (com.skyflow.generated.rest.resources.records.requests.InsertRequest sent : captor.getAllValues()) {
            // insertBatch rebuilds the request per batch, so tableName/vaultId must be re-applied.
            Assert.assertEquals("cards", sent.getTableName());
            Assert.assertEquals("vault123", sent.getVaultId());
            for (com.skyflow.generated.rest.types.InsertRecordData record : sent.getRecords()) {
                // The name rides the envelope only — duplicating it per record is rejected by the vault.
                Assert.assertFalse(record.getTableName().isPresent());
                Assert.assertEquals(String.valueOf(expectedPos), record.getData().get("pos"));
                expectedPos++;
            }
        }
        Assert.assertEquals(MULTI_BATCH_ITEM_COUNT, expectedPos);
    }

    @Test
    public void testBulkInsert_responseIndexMapsToOriginalInputPosition_acrossBatches() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        stubInsertEcho(mockRaw);

        VaultController controller = createControllerWithMock(mockApi);
        BulkInsertRequest request = BulkInsertRequest.builder()
                .tableName("cards")
                .records(multiBatchInsertRecords())
                .build();

        BulkInsertResponse response = controller.bulkInsert(request);

        Assert.assertEquals(MULTI_BATCH_ITEM_COUNT, response.getRecords().size());
        for (int i = 0; i < MULTI_BATCH_ITEM_COUNT; i++) {
            BulkInsertResponseRecord record = response.getRecords().get(i);
            Assert.assertEquals(i, record.getIndex());
            // skyflowId encodes the input record's position, so this proves index -> input position.
            Assert.assertEquals("sky-" + i, record.getSkyflowId());
        }
    }

    @Test
    public void testBulkInsert_interceptorInvokedOncePerBatchWithDistinctContext() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        stubInsertEcho(mockRaw);

        VaultController controller = createControllerWithMock(mockApi);
        BulkInsertRequest request = BulkInsertRequest.builder()
                .tableName("cards")
                .records(multiBatchInsertRecords())
                .build();

        CountingInterceptor interceptor = new CountingInterceptor();
        controller.bulkInsert(request, BulkInsertOptions.builder().interceptor(interceptor).build());

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        Mockito.verify(mockRaw.records, Mockito.times(EXPECTED_BATCH_COUNT)).insertRecords(any(), captor.capture());
        assertInterceptorRanOncePerBatch(interceptor, captor.getAllValues());
    }

    // ── bulk detokenize: batch dispatch fidelity ─────────────────────────────

    @Test
    public void testBulkDetokenize_vaultIdAndRedactionsReachEveryBatchAndTokenOrderPreserved() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        stubDetokenizeEcho(mockRaw);

        VaultController controller = createControllerWithMock(mockApi);
        List<String> tokens = multiBatchTokens();
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder()
                .tokens(tokens)
                .tokenGroupRedactions(Collections.singletonList(
                        TokenGroupRedactions.builder().tokenGroupName("group one").redaction("MASKED").build()))
                .build();

        controller.bulkDetokenize(request);

        ArgumentCaptor<com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest> captor =
                ArgumentCaptor.forClass(com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest.class);
        Mockito.verify(mockRaw.tokens, Mockito.times(EXPECTED_BATCH_COUNT)).detokenize(captor.capture(), any());

        List<String> flattened = new ArrayList<>();
        for (com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest sent : captor.getAllValues()) {
            Assert.assertEquals("vault123", sent.getVaultId());
            Assert.assertTrue(sent.getTokenGroupRedactions().isPresent());
            Assert.assertEquals("group one", sent.getTokenGroupRedactions().get().get(0).getTokenGroupName().get());
            Assert.assertEquals("MASKED", sent.getTokenGroupRedactions().get().get(0).getRedaction().get());
            flattened.addAll(sent.getTokens());
        }
        Assert.assertEquals(tokens, flattened);
    }

    @Test
    public void testBulkDetokenize_responseIndexMapsToOriginalInputPosition_acrossBatches() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        stubDetokenizeEcho(mockRaw);

        VaultController controller = createControllerWithMock(mockApi);
        List<String> tokens = multiBatchTokens();
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder().tokens(tokens).build();

        BulkDetokenizeResponse response = controller.bulkDetokenize(request);

        Assert.assertEquals(MULTI_BATCH_ITEM_COUNT, response.getRecords().size());
        for (int i = 0; i < MULTI_BATCH_ITEM_COUNT; i++) {
            Assert.assertEquals(i, response.getRecords().get(i).getIndex());
            Assert.assertEquals(tokens.get(i), response.getRecords().get(i).getToken());
        }
    }

    @Test
    public void testBulkDetokenize_interceptorInvokedOncePerBatchWithDistinctContext() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        stubDetokenizeEcho(mockRaw);

        VaultController controller = createControllerWithMock(mockApi);
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder().tokens(multiBatchTokens()).build();

        CountingInterceptor interceptor = new CountingInterceptor();
        controller.bulkDetokenize(request, BulkDetokenizeOptions.builder().interceptor(interceptor).build());

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        Mockito.verify(mockRaw.tokens, Mockito.times(EXPECTED_BATCH_COUNT)).detokenize(any(), captor.capture());
        assertInterceptorRanOncePerBatch(interceptor, captor.getAllValues());
    }

    // ── bulk delete tokens: batch dispatch fidelity ──────────────────────────

    @Test
    public void testBulkDeleteTokens_vaultIdOnEveryBatchAndTokenOrderPreserved() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        stubDeleteTokensEcho(mockRaw);

        VaultController controller = createControllerWithMock(mockApi);
        List<String> tokens = multiBatchTokens();
        BulkDeleteTokensRequest request = BulkDeleteTokensRequest.builder().tokens(tokens).build();

        BulkDeleteTokensResponse response = controller.bulkDeleteTokens(request);

        ArgumentCaptor<com.skyflow.generated.rest.resources.tokens.requests.DeleteTokenRequest> captor =
                ArgumentCaptor.forClass(com.skyflow.generated.rest.resources.tokens.requests.DeleteTokenRequest.class);
        Mockito.verify(mockRaw.tokens, Mockito.times(EXPECTED_BATCH_COUNT)).deleteToken(captor.capture(), any());

        List<String> flattened = new ArrayList<>();
        for (com.skyflow.generated.rest.resources.tokens.requests.DeleteTokenRequest sent : captor.getAllValues()) {
            Assert.assertEquals("vault123", sent.getVaultId());
            flattened.addAll(sent.getTokens());
        }
        Assert.assertEquals(tokens, flattened);

        Assert.assertEquals(MULTI_BATCH_ITEM_COUNT, response.getRecords().size());
        for (int i = 0; i < MULTI_BATCH_ITEM_COUNT; i++) {
            Assert.assertEquals(i, response.getRecords().get(i).getIndex());
            Assert.assertEquals(tokens.get(i), response.getRecords().get(i).getToken());
        }
    }

    @Test
    public void testBulkDeleteTokens_interceptorInvokedOncePerBatchWithDistinctContext() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        stubDeleteTokensEcho(mockRaw);

        VaultController controller = createControllerWithMock(mockApi);
        BulkDeleteTokensRequest request = BulkDeleteTokensRequest.builder().tokens(multiBatchTokens()).build();

        CountingInterceptor interceptor = new CountingInterceptor();
        controller.bulkDeleteTokens(request, BulkDeleteTokensOptions.builder().interceptor(interceptor).build());

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        Mockito.verify(mockRaw.tokens, Mockito.times(EXPECTED_BATCH_COUNT)).deleteToken(any(), captor.capture());
        assertInterceptorRanOncePerBatch(interceptor, captor.getAllValues());
    }

    // ── bulk tokenize: batch dispatch fidelity ───────────────────────────────

    @Test
    public void testBulkTokenize_vaultIdOnEveryBatchAndValueOrderPreserved() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        stubTokenizeEcho(mockRaw);

        VaultController controller = createControllerWithMock(mockApi);
        ArrayList<BulkTokenizeRequestRecord> records = new ArrayList<>();
        for (int i = 0; i < MULTI_BATCH_ITEM_COUNT; i++) {
            records.add(BulkTokenizeRequestRecord.builder()
                    .value("value-" + i)
                    .tokenGroupNames(Collections.singletonList("group1"))
                    .build());
        }
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().records(records).build();

        controller.bulkTokenize(request);

        ArgumentCaptor<com.skyflow.generated.rest.resources.tokens.requests.TokenizeRequest> captor =
                ArgumentCaptor.forClass(com.skyflow.generated.rest.resources.tokens.requests.TokenizeRequest.class);
        Mockito.verify(mockRaw.tokens, Mockito.times(EXPECTED_BATCH_COUNT)).tokenize(captor.capture(), any());

        List<Object> flattened = new ArrayList<>();
        for (com.skyflow.generated.rest.resources.tokens.requests.TokenizeRequest sent : captor.getAllValues()) {
            Assert.assertEquals("vault123", sent.getVaultId());
            for (com.skyflow.generated.rest.types.TokenizeRequestObject obj : sent.getData()) {
                Assert.assertEquals(Collections.singletonList("group1"), obj.getTokenGroupNames());
                flattened.add(obj.getValue().get());
            }
        }
        Assert.assertEquals(MULTI_BATCH_ITEM_COUNT, flattened.size());
        for (int i = 0; i < MULTI_BATCH_ITEM_COUNT; i++) {
            Assert.assertEquals("value-" + i, flattened.get(i));
        }
    }

    @Test
    public void testBulkTokenize_interceptorInvokedOncePerBatchWithDistinctContext() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        stubTokenizeEcho(mockRaw);

        VaultController controller = createControllerWithMock(mockApi);
        ArrayList<BulkTokenizeRequestRecord> records = new ArrayList<>();
        for (int i = 0; i < MULTI_BATCH_ITEM_COUNT; i++) {
            records.add(BulkTokenizeRequestRecord.builder().value("value-" + i).build());
        }
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().records(records).build();

        CountingInterceptor interceptor = new CountingInterceptor();
        controller.bulkTokenize(request, BulkTokenizeOptions.builder().interceptor(interceptor).build());

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        Mockito.verify(mockRaw.tokens, Mockito.times(EXPECTED_BATCH_COUNT)).tokenize(any(), captor.capture());
        assertInterceptorRanOncePerBatch(interceptor, captor.getAllValues());
    }

    @Test
    public void testBulkInsert_throwingInterceptorWrappedAsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        MockRaw mockRaw = mockRawFlowservice(mockApi);
        stubInsertEcho(mockRaw);
        VaultController controller = createControllerWithMock(mockApi);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder().tableName("table1").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();

        RequestInterceptor interceptor = ctx -> {
            throw new IllegalStateException("sync insert interceptor blew up");
        };
        BulkInsertOptions options = BulkInsertOptions.builder().interceptor(interceptor).build();

        try {
            controller.bulkInsert(request, options);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals("sync insert interceptor blew up", e.getMessage());
        }
    }
}
