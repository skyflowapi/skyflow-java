package com.skyflow.vault.controller;

import com.skyflow.VaultClient;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.CustomHeaderKey;
import com.skyflow.enums.Env;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.core.ApiClientHttpResponse;
import com.skyflow.generated.rest.core.RequestOptions;
import com.skyflow.generated.rest.resources.flowservice.FlowserviceClient;
import com.skyflow.generated.rest.resources.flowservice.RawFlowserviceClient;
import com.skyflow.generated.rest.types.V1DeleteTokenResponseObject;
import com.skyflow.generated.rest.types.V1FlowDeleteTokenResponse;
import com.skyflow.generated.rest.types.V1FlowDetokenizeResponse;
import com.skyflow.generated.rest.types.V1FlowDetokenizeResponseObject;
import com.skyflow.generated.rest.types.V1FlowTokenizeResponse;
import com.skyflow.generated.rest.types.V1FlowTokenizeResponseObject;
import com.skyflow.generated.rest.types.V1InsertResponse;
import com.skyflow.generated.rest.types.V1RecordResponseObject;
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
import com.skyflow.vault.data.DeleteTokensOptions;
import com.skyflow.vault.data.InsertRequestRecord;
import com.skyflow.vault.data.RequestInterceptor;
import com.skyflow.vault.data.Token;
import com.skyflow.vault.data.TokenGroupRedactions;
import com.skyflow.vault.data.TokenizeOptions;
import com.skyflow.vault.data.TokenizeRequestRecord;
import com.skyflow.vault.data.TokenizeRequest;
import com.skyflow.vault.data.TokenizeResponse;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
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

    private static RawFlowserviceClient mockRawFlowservice(ApiClient mockApi) {
        FlowserviceClient mockFlowservice = Mockito.mock(FlowserviceClient.class);
        RawFlowserviceClient mockRaw = Mockito.mock(RawFlowserviceClient.class);
        when(mockApi.flowservice()).thenReturn(mockFlowservice);
        when(mockFlowservice.withRawResponse()).thenReturn(mockRaw);
        return mockRaw;
    }

    // Tests for the unary insert / detokenize / tokenize / deleteTokens controller methods
    // (and their interceptor-header wiring) were removed: VaultController is bulk-only now.

    // ── bulkInsert ────────────────────────────────────────────────────────────

    @Test
    public void testBulkInsert_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "tok-abc");
        V1RecordResponseObject record = V1RecordResponseObject.builder().skyflowId("sky-id-1").tokens(tokens).build();
        V1InsertResponse body = V1InsertResponse.builder().records(Collections.singletonList(record)).build();
        ApiClientHttpResponse<V1InsertResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.insert(any(), any())).thenReturn(httpResp);

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
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "tok-abc");
        V1RecordResponseObject record = V1RecordResponseObject.builder().skyflowId("sky-id-1").tokens(tokens).build();
        V1InsertResponse body = V1InsertResponse.builder().records(Collections.singletonList(record)).build();
        ApiClientHttpResponse<V1InsertResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.insert(any(), any())).thenReturn(httpResp);

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
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "tok-abc");
        V1RecordResponseObject record = V1RecordResponseObject.builder().skyflowId("sky-id-1").tokens(tokens).build();
        V1InsertResponse body = V1InsertResponse.builder().records(Collections.singletonList(record)).build();
        ApiClientHttpResponse<V1InsertResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.insert(any(), any())).thenReturn(httpResp);

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
        Mockito.verify(mockRaw).insert(any(), captor.capture());
        Assert.assertEquals("acct-123", captor.getValue().getHeaders().get(CustomHeaderKey.SKYFLOW_ACCOUNT_ID.toString()));
    }

    // ── bulkDetokenize ────────────────────────────────────────────────────────

    @Test
    public void testBulkDetokenize_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        V1FlowDetokenizeResponseObject record = V1FlowDetokenizeResponseObject.builder()
                .token("token1").value("secret-value").build();
        V1FlowDetokenizeResponse body = V1FlowDetokenizeResponse.builder()
                .response(Collections.singletonList(record)).build();
        ApiClientHttpResponse<V1FlowDetokenizeResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.detokenize(any(), any())).thenReturn(httpResp);

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
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        V1FlowDetokenizeResponseObject record = V1FlowDetokenizeResponseObject.builder()
                .token("token1").value("secret-value").build();
        V1FlowDetokenizeResponse body = V1FlowDetokenizeResponse.builder()
                .response(Collections.singletonList(record)).build();
        ApiClientHttpResponse<V1FlowDetokenizeResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.detokenize(any(), any())).thenReturn(httpResp);

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
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        V1DeleteTokenResponseObject record = V1DeleteTokenResponseObject.builder().value("token1").build();
        V1FlowDeleteTokenResponse body = V1FlowDeleteTokenResponse.builder()
                .tokens(Collections.singletonList(record)).build();
        ApiClientHttpResponse<V1FlowDeleteTokenResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.deletetoken(any(), any())).thenReturn(httpResp);

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
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        V1DeleteTokenResponseObject record = V1DeleteTokenResponseObject.builder().value("token1").build();
        V1FlowDeleteTokenResponse body = V1FlowDeleteTokenResponse.builder()
                .tokens(Collections.singletonList(record)).build();
        ApiClientHttpResponse<V1FlowDeleteTokenResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.deletetoken(any(), any())).thenReturn(httpResp);

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
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        V1FlowTokenizeResponseObject responseObject = V1FlowTokenizeResponseObject.builder()
                .value("value1").tokenGroupName("group1").token("tok-abc").build();
        V1FlowTokenizeResponse body = V1FlowTokenizeResponse.builder()
                .response(Collections.singletonList(responseObject)).build();
        ApiClientHttpResponse<V1FlowTokenizeResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.tokenize(any(), any())).thenReturn(httpResp);

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
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        V1FlowTokenizeResponseObject responseObject = V1FlowTokenizeResponseObject.builder()
                .value("value1").tokenGroupName("group1").token("tok-abc").build();
        V1FlowTokenizeResponse body = V1FlowTokenizeResponse.builder()
                .response(Collections.singletonList(responseObject)).build();
        ApiClientHttpResponse<V1FlowTokenizeResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.tokenize(any(), any())).thenReturn(httpResp);

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
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.insert(any(), any()))
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
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.deletetoken(any(), any()))
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
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.deletetoken(any(), any()))
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
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.tokenize(any(), any()))
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
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.tokenize(any(), any()))
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
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.detokenize(any(), any()))
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
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        Map<String, Object> tokens1 = new HashMap<>();
        tokens1.put("name", "tok-batch1");
        V1RecordResponseObject record1 = V1RecordResponseObject.builder().skyflowId("sky-id-batch1").tokens(tokens1).build();
        V1InsertResponse body1 = V1InsertResponse.builder().records(Collections.singletonList(record1)).build();
        ApiClientHttpResponse<V1InsertResponse> httpResp1 = new ApiClientHttpResponse<>(body1, buildOkHttpResponse());

        Map<String, Object> tokens2 = new HashMap<>();
        tokens2.put("name", "tok-batch2");
        V1RecordResponseObject record2 = V1RecordResponseObject.builder().skyflowId("sky-id-batch2").tokens(tokens2).build();
        V1InsertResponse body2 = V1InsertResponse.builder().records(Collections.singletonList(record2)).build();
        ApiClientHttpResponse<V1InsertResponse> httpResp2 = new ApiClientHttpResponse<>(body2, buildOkHttpResponse());

        when(mockRaw.insert(any(), any())).thenReturn(httpResp1).thenReturn(httpResp2);

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
        Mockito.verify(mockRaw, Mockito.times(2)).insert(any(), any());
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
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        Map<String, Object> tokens1 = new HashMap<>();
        tokens1.put("name", "tok-batch1");
        V1RecordResponseObject record1 = V1RecordResponseObject.builder().skyflowId("sky-id-batch1").tokens(tokens1).build();
        V1InsertResponse body1 = V1InsertResponse.builder().records(Collections.singletonList(record1)).build();
        ApiClientHttpResponse<V1InsertResponse> httpResp1 = new ApiClientHttpResponse<>(body1, buildOkHttpResponse());

        when(mockRaw.insert(any(), any()))
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
        Mockito.verify(mockRaw, Mockito.times(2)).insert(any(), any());

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
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        Map<String, Object> tokenEntry = new HashMap<>();
        tokenEntry.put("token", "tok-xyz");
        tokenEntry.put("tokenGroupName", "group1");
        List<Map<String, Object>> tokenList = new ArrayList<>();
        tokenList.add(tokenEntry);

        Map<String, Object> tokens = new HashMap<>();
        tokens.put("field1", tokenList);

        V1RecordResponseObject record = V1RecordResponseObject.builder().skyflowId("sky-id-1").tokens(tokens).build();
        V1InsertResponse body = V1InsertResponse.builder().records(Collections.singletonList(record)).build();
        ApiClientHttpResponse<V1InsertResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.insert(any(), any())).thenReturn(httpResp);

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

    // Tests for the unary query / get controller methods were removed: VaultController is bulk-only now.

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
    private static void stubInsertEcho(RawFlowserviceClient mockRaw) {
        when(mockRaw.insert(any(), any())).thenAnswer(invocation -> {
            com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest req =
                    invocation.getArgument(0);
            List<V1RecordResponseObject> responseRecords = new ArrayList<>();
            for (com.skyflow.generated.rest.types.V1InsertRecordData record : req.getRecords().get()) {
                responseRecords.add(V1RecordResponseObject.builder()
                        .skyflowId("sky-" + record.getData().get().get("pos"))
                        .tableName(record.getTableName().orElse(null))
                        .build());
            }
            V1InsertResponse body = V1InsertResponse.builder().records(responseRecords).build();
            return new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        });
    }

    /** Echoes each requested token back as a detokenize response record. */
    private static void stubDetokenizeEcho(RawFlowserviceClient mockRaw) {
        when(mockRaw.detokenize(any(), any())).thenAnswer(invocation -> {
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest req =
                    invocation.getArgument(0);
            List<V1FlowDetokenizeResponseObject> responseRecords = new ArrayList<>();
            for (String token : req.getTokens().get()) {
                responseRecords.add(V1FlowDetokenizeResponseObject.builder().token(token).build());
            }
            V1FlowDetokenizeResponse body = V1FlowDetokenizeResponse.builder()
                    .response(responseRecords).build();
            return new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        });
    }

    /** Echoes each requested token back as a delete-token response record. */
    private static void stubDeleteTokensEcho(RawFlowserviceClient mockRaw) {
        when(mockRaw.deletetoken(any(), any())).thenAnswer(invocation -> {
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest req =
                    invocation.getArgument(0);
            List<V1DeleteTokenResponseObject> responseRecords = new ArrayList<>();
            for (String token : req.getTokens().get()) {
                responseRecords.add(V1DeleteTokenResponseObject.builder().value(token).build());
            }
            V1FlowDeleteTokenResponse body = V1FlowDeleteTokenResponse.builder()
                    .tokens(responseRecords).build();
            return new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        });
    }

    /** Echoes each requested tokenize value back with one token per requested group name. */
    private static void stubTokenizeEcho(RawFlowserviceClient mockRaw) {
        when(mockRaw.tokenize(any(), any())).thenAnswer(invocation -> {
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest req =
                    invocation.getArgument(0);
            List<V1FlowTokenizeResponseObject> responseRecords = new ArrayList<>();
            for (com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject obj : req.getData().get()) {
                responseRecords.add(V1FlowTokenizeResponseObject.builder()
                        .value(obj.getValue().get())
                        .tokenGroupName("group1")
                        .token("tok-" + obj.getValue().get())
                        .build());
            }
            V1FlowTokenizeResponse body = V1FlowTokenizeResponse.builder().response(responseRecords).build();
            return new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        });
    }

    // ── bulk insert: batch dispatch fidelity ─────────────────────────────────

    @Test
    public void testBulkInsert_tableNameAndVaultIdReAppliedOnEveryBatch() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        stubInsertEcho(mockRaw);

        VaultController controller = createControllerWithMock(mockApi);
        BulkInsertRequest request = BulkInsertRequest.builder()
                .tableName("cards")
                .records(multiBatchInsertRecords())
                .build();

        controller.bulkInsert(request);

        ArgumentCaptor<com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest> captor =
                ArgumentCaptor.forClass(com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest.class);
        Mockito.verify(mockRaw, Mockito.times(EXPECTED_BATCH_COUNT)).insert(captor.capture(), any());

        int expectedPos = 0;
        for (com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest sent : captor.getAllValues()) {
            // insertBatch rebuilds the request per batch, so tableName/vaultId must be re-applied.
            Assert.assertEquals("cards", sent.getTableName().get());
            Assert.assertEquals("vault123", sent.getVaultId().get());
            for (com.skyflow.generated.rest.types.V1InsertRecordData record : sent.getRecords().get()) {
                // The name rides the envelope only — duplicating it per record is rejected by the vault.
                Assert.assertFalse(record.getTableName().isPresent());
                Assert.assertEquals(String.valueOf(expectedPos), record.getData().get().get("pos"));
                expectedPos++;
            }
        }
        Assert.assertEquals(MULTI_BATCH_ITEM_COUNT, expectedPos);
    }

    @Test
    public void testBulkInsert_responseIndexMapsToOriginalInputPosition_acrossBatches() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
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
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        stubInsertEcho(mockRaw);

        VaultController controller = createControllerWithMock(mockApi);
        BulkInsertRequest request = BulkInsertRequest.builder()
                .tableName("cards")
                .records(multiBatchInsertRecords())
                .build();

        CountingInterceptor interceptor = new CountingInterceptor();
        controller.bulkInsert(request, BulkInsertOptions.builder().interceptor(interceptor).build());

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        Mockito.verify(mockRaw, Mockito.times(EXPECTED_BATCH_COUNT)).insert(any(), captor.capture());
        assertInterceptorRanOncePerBatch(interceptor, captor.getAllValues());
    }

    // ── bulk detokenize: batch dispatch fidelity ─────────────────────────────

    @Test
    public void testBulkDetokenize_vaultIdAndRedactionsReachEveryBatchAndTokenOrderPreserved() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        stubDetokenizeEcho(mockRaw);

        VaultController controller = createControllerWithMock(mockApi);
        List<String> tokens = multiBatchTokens();
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder()
                .tokens(tokens)
                .tokenGroupRedactions(Collections.singletonList(
                        TokenGroupRedactions.builder().tokenGroupName("group one").redaction("MASKED").build()))
                .build();

        controller.bulkDetokenize(request);

        ArgumentCaptor<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest> captor =
                ArgumentCaptor.forClass(com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.class);
        Mockito.verify(mockRaw, Mockito.times(EXPECTED_BATCH_COUNT)).detokenize(captor.capture(), any());

        List<String> flattened = new ArrayList<>();
        for (com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest sent : captor.getAllValues()) {
            Assert.assertEquals("vault123", sent.getVaultId().get());
            Assert.assertTrue(sent.getTokenGroupRedactions().isPresent());
            Assert.assertEquals("group one", sent.getTokenGroupRedactions().get().get(0).getTokenGroupName().get());
            Assert.assertEquals("MASKED", sent.getTokenGroupRedactions().get().get(0).getRedaction().get());
            flattened.addAll(sent.getTokens().get());
        }
        Assert.assertEquals(tokens, flattened);
    }

    @Test
    public void testBulkDetokenize_responseIndexMapsToOriginalInputPosition_acrossBatches() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
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
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        stubDetokenizeEcho(mockRaw);

        VaultController controller = createControllerWithMock(mockApi);
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder().tokens(multiBatchTokens()).build();

        CountingInterceptor interceptor = new CountingInterceptor();
        controller.bulkDetokenize(request, BulkDetokenizeOptions.builder().interceptor(interceptor).build());

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        Mockito.verify(mockRaw, Mockito.times(EXPECTED_BATCH_COUNT)).detokenize(any(), captor.capture());
        assertInterceptorRanOncePerBatch(interceptor, captor.getAllValues());
    }

    // ── bulk delete tokens: batch dispatch fidelity ──────────────────────────

    @Test
    public void testBulkDeleteTokens_vaultIdOnEveryBatchAndTokenOrderPreserved() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        stubDeleteTokensEcho(mockRaw);

        VaultController controller = createControllerWithMock(mockApi);
        List<String> tokens = multiBatchTokens();
        BulkDeleteTokensRequest request = BulkDeleteTokensRequest.builder().tokens(tokens).build();

        BulkDeleteTokensResponse response = controller.bulkDeleteTokens(request);

        ArgumentCaptor<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest> captor =
                ArgumentCaptor.forClass(com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest.class);
        Mockito.verify(mockRaw, Mockito.times(EXPECTED_BATCH_COUNT)).deletetoken(captor.capture(), any());

        List<String> flattened = new ArrayList<>();
        for (com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest sent : captor.getAllValues()) {
            Assert.assertEquals("vault123", sent.getVaultId().get());
            flattened.addAll(sent.getTokens().get());
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
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        stubDeleteTokensEcho(mockRaw);

        VaultController controller = createControllerWithMock(mockApi);
        BulkDeleteTokensRequest request = BulkDeleteTokensRequest.builder().tokens(multiBatchTokens()).build();

        CountingInterceptor interceptor = new CountingInterceptor();
        controller.bulkDeleteTokens(request, BulkDeleteTokensOptions.builder().interceptor(interceptor).build());

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        Mockito.verify(mockRaw, Mockito.times(EXPECTED_BATCH_COUNT)).deletetoken(any(), captor.capture());
        assertInterceptorRanOncePerBatch(interceptor, captor.getAllValues());
    }

    // ── bulk tokenize: batch dispatch fidelity ───────────────────────────────

    @Test
    public void testBulkTokenize_vaultIdOnEveryBatchAndValueOrderPreserved() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
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

        ArgumentCaptor<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest> captor =
                ArgumentCaptor.forClass(com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.class);
        Mockito.verify(mockRaw, Mockito.times(EXPECTED_BATCH_COUNT)).tokenize(captor.capture(), any());

        List<Object> flattened = new ArrayList<>();
        for (com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest sent : captor.getAllValues()) {
            Assert.assertEquals("vault123", sent.getVaultId().get());
            for (com.skyflow.generated.rest.types.V1FlowTokenizeRequestObject obj : sent.getData().get()) {
                Assert.assertEquals(Collections.singletonList("group1"), obj.getTokenGroupNames().get());
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
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
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
        Mockito.verify(mockRaw, Mockito.times(EXPECTED_BATCH_COUNT)).tokenize(any(), captor.capture());
        assertInterceptorRanOncePerBatch(interceptor, captor.getAllValues());
    }
}
