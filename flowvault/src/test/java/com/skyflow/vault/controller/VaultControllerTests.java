package com.skyflow.vault.controller;

import com.skyflow.VaultClient;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.CustomHeaderKey;
import com.skyflow.enums.Env;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.core.ApiClientException;
import com.skyflow.generated.rest.core.ApiClientHttpResponse;
import com.skyflow.generated.rest.core.RequestOptions;
import com.skyflow.generated.rest.resources.flowservice.FlowserviceClient;
import com.skyflow.generated.rest.resources.flowservice.RawFlowserviceClient;
import com.skyflow.generated.rest.resources.records.RawRecordsClient;
import com.skyflow.generated.rest.resources.records.RecordsClient;
import com.skyflow.generated.rest.types.FlowTokenizeResponseObjectToken;
import com.skyflow.generated.rest.types.V1DeleteTokenResponseObject;
import com.skyflow.generated.rest.types.V1ExecuteQueryRecordResponse;
import com.skyflow.generated.rest.types.V1ExecuteQueryResponse;
import com.skyflow.generated.rest.types.V1FlowDeleteTokenResponse;
import com.skyflow.generated.rest.types.V1FlowDetokenizeResponse;
import com.skyflow.generated.rest.types.V1FlowDetokenizeResponseObject;
import com.skyflow.generated.rest.types.V1FlowTokenizeResponse;
import com.skyflow.generated.rest.types.V1FlowTokenizeResponseObject;
import com.skyflow.generated.rest.types.V1GetResponse;
import com.skyflow.generated.rest.types.V1InsertResponse;
import com.skyflow.generated.rest.types.V1RecordResponseObject;
import com.skyflow.utils.Constants;
import com.skyflow.vault.data.BulkDeleteTokensRequest;
import com.skyflow.vault.data.BulkDeleteTokensResponse;
import com.skyflow.vault.data.BulkDetokenizeRequest;
import com.skyflow.vault.data.BulkDetokenizeResponse;
import com.skyflow.vault.data.BulkInsertRecord;
import com.skyflow.vault.data.BulkInsertRequest;
import com.skyflow.vault.data.BulkInsertResponse;
import com.skyflow.vault.data.BulkTokenizeRequestRecord;
import com.skyflow.vault.data.BulkTokenizeResponseRecord;
import com.skyflow.vault.data.BulkTokenizeRequest;
import com.skyflow.vault.data.BulkTokenizeResponse;
import com.skyflow.vault.data.DeleteTokensRequest;
import com.skyflow.vault.data.DeleteTokensResponse;
import com.skyflow.vault.data.DetokenizeData;
import com.skyflow.vault.data.DetokenizeOptions;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.DetokenizeResponse;
import com.skyflow.vault.data.GetOptions;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;
import com.skyflow.vault.data.InsertOptions;
import com.skyflow.vault.data.InsertRecord;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;
import com.skyflow.vault.data.QueryOptions;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.QueryResponse;
import com.skyflow.vault.data.RequestInterceptor;
import com.skyflow.vault.data.Success;
import com.skyflow.vault.data.Token;
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

    private static RawRecordsClient mockRawRecords(ApiClient mockApi) {
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        RawRecordsClient mockRaw = Mockito.mock(RawRecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);
        when(mockRecords.withRawResponse()).thenReturn(mockRaw);
        return mockRaw;
    }

    // ── insert ────────────────────────────────────────────────────────────────

    @Test
    public void testInsert_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "tok-abc");
        V1RecordResponseObject record = V1RecordResponseObject.builder()
                .skyflowId("sky-id-1")
                .tokens(tokens)
                .build();
        V1InsertResponse body = V1InsertResponse.builder().records(Collections.singletonList(record)).build();
        ApiClientHttpResponse<V1InsertResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.insert(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(InsertRecord.builder().table("table1").data(data).build());
        InsertRequest request = InsertRequest.builder().records(records).build();

        InsertResponse response = controller.insert(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getInsertedFields().size());
        Assert.assertEquals("sky-id-1", response.getInsertedFields().get(0).get("skyflowId"));
        Assert.assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void testInsert_apiErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.insert(any(), any()))
                .thenThrow(new ApiClientApiException("insert failed", 401, "unauthorized"));

        VaultController controller = createControllerWithMock(mockApi);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(InsertRecord.builder().table("table1").data(data).build());
        InsertRequest request = InsertRequest.builder().records(records).build();

        try {
            controller.insert(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(401, e.getHttpCode());
        }
    }

    @Test
    public void testInsert_networkErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.insert(any(), any()))
                .thenThrow(new ApiClientException("Network error executing HTTP request"));

        VaultController controller = createControllerWithMock(mockApi);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(InsertRecord.builder().table("table1").data(data).build());
        InsertRequest request = InsertRequest.builder().records(records).build();

        try {
            controller.insert(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
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

    // ── detokenize ────────────────────────────────────────────────────────────

    @Test
    public void testDetokenize_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        V1FlowDetokenizeResponseObject record = V1FlowDetokenizeResponseObject.builder()
                .token("token1")
                .value("secret-value")
                .build();
        V1FlowDetokenizeResponse body = V1FlowDetokenizeResponse.builder()
                .response(Collections.singletonList(record))
                .build();
        ApiClientHttpResponse<V1FlowDetokenizeResponse> httpResp =
                new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.detokenize(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<DetokenizeData> data = new ArrayList<>();
        data.add(new DetokenizeData("token1"));
        DetokenizeRequest request = DetokenizeRequest.builder().detokenizeData(data).build();

        DetokenizeResponse response = controller.detokenize(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getDetokenizedFields().size());
        Assert.assertEquals("secret-value", response.getDetokenizedFields().get(0).getValue());
        Assert.assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void testDetokenize_apiErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.detokenize(any(), any()))
                .thenThrow(new ApiClientApiException("detokenize failed", 401, "unauthorized"));

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<DetokenizeData> data = new ArrayList<>();
        data.add(new DetokenizeData("token1"));
        DetokenizeRequest request = DetokenizeRequest.builder().detokenizeData(data).build();

        try {
            controller.detokenize(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(401, e.getHttpCode());
        }
    }

    @Test
    public void testDetokenize_networkErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.detokenize(any(), any()))
                .thenThrow(new ApiClientException("Network error executing HTTP request"));

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<DetokenizeData> data = new ArrayList<>();
        data.add(new DetokenizeData("token1"));
        DetokenizeRequest request = DetokenizeRequest.builder().detokenizeData(data).build();

        try {
            controller.detokenize(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testDetokenize_nullRequestThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);
        try {
            controller.detokenize(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    // ── insert/detokenize interceptor header wiring ──────────────────────────

    @Test
    public void testInsert_interceptorAddsCustomHeader() throws Exception {
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
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(InsertRecord.builder().table("table1").data(data).build());
        InsertRequest request = InsertRequest.builder().records(records).build();

        RequestInterceptor interceptor = ctx -> ctx.addHeader(CustomHeaderKey.SkyflowAccountName, "acct-name");
        InsertOptions options = InsertOptions.builder().interceptor(interceptor).build();

        controller.insert(request, options);

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        Mockito.verify(mockRaw).insert(any(), captor.capture());
        Assert.assertEquals("acct-name", captor.getValue().getHeaders().get(CustomHeaderKey.SkyflowAccountName.toString()));
    }

    @Test
    public void testDetokenize_noOptionsDoesNotAddCustomHeader() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        V1FlowDetokenizeResponseObject record = V1FlowDetokenizeResponseObject.builder()
                .token("token1").value("secret-value").build();
        V1FlowDetokenizeResponse body = V1FlowDetokenizeResponse.builder()
                .response(Collections.singletonList(record)).build();
        ApiClientHttpResponse<V1FlowDetokenizeResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.detokenize(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<DetokenizeData> data = new ArrayList<>();
        data.add(new DetokenizeData("token1"));
        DetokenizeRequest request = DetokenizeRequest.builder().detokenizeData(data).build();

        controller.detokenize(request, null);

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        Mockito.verify(mockRaw).detokenize(any(), captor.capture());
        Assert.assertNull(captor.getValue().getHeaders().get(CustomHeaderKey.SkyflowAccountID.toString()));
    }

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
        ArrayList<BulkInsertRecord> records = new ArrayList<>();
        records.add(BulkInsertRecord.builder().table("table1").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();

        BulkInsertResponse response = controller.bulkInsert(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getSuccess().size());
        Assert.assertEquals("sky-id-1", response.getSuccess().get(0).getSkyflowId());
        Assert.assertTrue(response.getErrors().isEmpty());
        Assert.assertEquals(1, response.getSummary().getTotalRecords());
        Assert.assertEquals(1, response.getSummary().getTotalInserted());
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
        ArrayList<BulkInsertRecord> records = new ArrayList<>();
        records.add(BulkInsertRecord.builder().table("table1").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();

        BulkInsertResponse response = controller.bulkInsertAsync(request).get(5, TimeUnit.SECONDS);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getSuccess().size());
        Assert.assertEquals("sky-id-1", response.getSuccess().get(0).getSkyflowId());
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
        ArrayList<BulkInsertRecord> records = new ArrayList<>();
        records.add(BulkInsertRecord.builder().table("table1").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();

        RequestInterceptor interceptor = ctx -> ctx.addHeader(CustomHeaderKey.SkyflowAccountID, "acct-123");
        InsertOptions options = InsertOptions.builder().interceptor(interceptor).build();

        controller.bulkInsert(request, options);

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        Mockito.verify(mockRaw).insert(any(), captor.capture());
        Assert.assertEquals("acct-123", captor.getValue().getHeaders().get(CustomHeaderKey.SkyflowAccountID.toString()));
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
        Assert.assertEquals(1, response.getSuccess().size());
        Assert.assertEquals("secret-value", response.getSuccess().get(0).getValue());
        Assert.assertTrue(response.getErrors().isEmpty());
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
        Assert.assertEquals(1, response.getSuccess().size());
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

        FlowTokenizeResponseObjectToken token = FlowTokenizeResponseObjectToken.builder()
                .tokenGroupName("group1").token("tok-abc").build();
        V1FlowTokenizeResponseObject responseObject = V1FlowTokenizeResponseObject.builder()
                .value("value1").tokens(Collections.singletonList(token)).build();
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
        Assert.assertEquals("tok-abc", response.getRecords().get(0).getTokens().get(0).getToken());
        Assert.assertNull(response.getRecords().get(0).getTokens().get(0).getError());
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

        FlowTokenizeResponseObjectToken token = FlowTokenizeResponseObjectToken.builder()
                .tokenGroupName("group1").token("tok-abc").build();
        V1FlowTokenizeResponseObject responseObject = V1FlowTokenizeResponseObject.builder()
                .value("value1").tokens(Collections.singletonList(token)).build();
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
        Assert.assertNull(response.getRecords().get(0).getTokens().get(0).getError());
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
        ArrayList<BulkInsertRecord> records = new ArrayList<>();
        records.add(BulkInsertRecord.builder().table("table1").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();

        BulkInsertResponse response = controller.bulkInsertAsync(request).get(5, TimeUnit.SECONDS);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertTrue(response.getSuccess().isEmpty());
        Assert.assertEquals(1, response.getErrors().size());
        Assert.assertEquals(401, response.getErrors().get(0).getCode());
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
        Assert.assertEquals(Integer.valueOf(400),
                response.getRecords().get(0).getTokens().get(0).getHttpCode());
        Assert.assertNotNull(response.getRecords().get(0).getTokens().get(0).getError());
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
        Assert.assertEquals(Integer.valueOf(400),
                response.getRecords().get(0).getTokens().get(0).getHttpCode());
        Assert.assertNotNull(response.getRecords().get(0).getTokens().get(0).getError());
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
        Assert.assertTrue(response.getSuccess().isEmpty());
        Assert.assertEquals(1, response.getErrors().size());
        Assert.assertEquals(401, response.getErrors().get(0).getCode());
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
        // (50 + 25) — processBulkInsertSync/insertBatchFutures must merge success/error lists
        // collected from more than one CompletableFuture into a single BulkInsertResponse.
        ArrayList<BulkInsertRecord> records = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("name", "john" + i);
            records.add(BulkInsertRecord.builder().table("table1").data(data).build());
        }
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();

        BulkInsertResponse response = controller.bulkInsert(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Mockito.verify(mockRaw, Mockito.times(2)).insert(any(), any());
        Assert.assertTrue(response.getErrors().isEmpty());
        Assert.assertEquals(2, response.getSuccess().size());
        Assert.assertEquals(75, response.getSummary().getTotalRecords());
        Assert.assertEquals(2, response.getSummary().getTotalInserted());

        Success batch1Success = response.getSuccess().stream()
                .filter(s -> "sky-id-batch1".equals(s.getSkyflowId())).findFirst().orElse(null);
        Success batch2Success = response.getSuccess().stream()
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
        ArrayList<BulkInsertRecord> records = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("name", "john" + i);
            records.add(BulkInsertRecord.builder().table("table1").data(data).build());
        }
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();

        BulkInsertResponse response = controller.bulkInsert(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Mockito.verify(mockRaw, Mockito.times(2)).insert(any(), any());

        Assert.assertEquals(1, response.getSuccess().size());
        Assert.assertEquals("sky-id-batch1", response.getSuccess().get(0).getSkyflowId());
        Assert.assertEquals(0, response.getSuccess().get(0).getIndex());

        Assert.assertEquals(1, response.getErrors().size());
        Assert.assertEquals(50, response.getErrors().get(0).getIndex());
        Assert.assertEquals(500, response.getErrors().get(0).getCode());

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
        ArrayList<BulkInsertRecord> records = new ArrayList<>();
        records.add(BulkInsertRecord.builder().table("table1").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();

        BulkInsertResponse response = controller.bulkInsert(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getSuccess().size());

        Success success = response.getSuccess().get(0);
        Assert.assertNotNull(success.getTokens());
        List<Token> field1Tokens = success.getTokens().get("field1");
        Assert.assertNotNull(field1Tokens);
        Assert.assertEquals(1, field1Tokens.size());
        Assert.assertEquals("tok-xyz", field1Tokens.get(0).getToken());
        Assert.assertEquals("group1", field1Tokens.get(0).getTokenGroupName());
    }

    // ── detokenize/tokenize interceptor header wiring ─────────────────────────

    @Test
    public void testDetokenize_interceptorAddsCustomHeader() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        V1FlowDetokenizeResponseObject record = V1FlowDetokenizeResponseObject.builder()
                .token("token1").value("secret-value").build();
        V1FlowDetokenizeResponse body = V1FlowDetokenizeResponse.builder()
                .response(Collections.singletonList(record)).build();
        ApiClientHttpResponse<V1FlowDetokenizeResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.detokenize(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<DetokenizeData> data = new ArrayList<>();
        data.add(new DetokenizeData("token1"));
        DetokenizeRequest request = DetokenizeRequest.builder().detokenizeData(data).build();

        RequestInterceptor interceptor = ctx -> ctx.addHeader(CustomHeaderKey.SkyflowAccountName, "acct-name");
        DetokenizeOptions options = DetokenizeOptions.builder().interceptor(interceptor).build();

        controller.detokenize(request, options);

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        Mockito.verify(mockRaw).detokenize(any(), captor.capture());
        Assert.assertEquals("acct-name", captor.getValue().getHeaders().get(CustomHeaderKey.SkyflowAccountName.toString()));
    }

    @Test
    public void testBulkTokenize_interceptorAddsCustomHeader() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        FlowTokenizeResponseObjectToken token = FlowTokenizeResponseObjectToken.builder()
                .tokenGroupName("group1").token("tok-abc").build();
        V1FlowTokenizeResponseObject responseObject = V1FlowTokenizeResponseObject.builder()
                .value("value1").tokens(Collections.singletonList(token)).build();
        V1FlowTokenizeResponse body = V1FlowTokenizeResponse.builder()
                .response(Collections.singletonList(responseObject)).build();
        ApiClientHttpResponse<V1FlowTokenizeResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.tokenize(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        BulkTokenizeRequest request = BulkTokenizeRequest.builder()
                .records(Collections.singletonList(BulkTokenizeRequestRecord.builder()
                        .value("value1")
                        .tokenGroupNames(Collections.singletonList("group1")).build()))
                .build();

        RequestInterceptor interceptor = ctx -> ctx.addHeader(CustomHeaderKey.SkyflowAccountID, "acct-123");
        TokenizeOptions options = TokenizeOptions.builder().interceptor(interceptor).build();

        controller.bulkTokenize(request, options);

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        Mockito.verify(mockRaw).tokenize(any(), captor.capture());
        Assert.assertEquals("acct-123", captor.getValue().getHeaders().get(CustomHeaderKey.SkyflowAccountID.toString()));
    }

    // ── query ─────────────────────────────────────────────────────────────────

    @Test
    public void testQuery_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawRecordsClient mockRaw = mockRawRecords(mockApi);

        Map<String, Object> data = new HashMap<>();
        data.put("skyflowId", "sky-id-1");
        data.put("name", "john");
        V1ExecuteQueryRecordResponse record = V1ExecuteQueryRecordResponse.builder().data(data).build();
        V1ExecuteQueryResponse body = V1ExecuteQueryResponse.builder().records(Collections.singletonList(record)).build();
        ApiClientHttpResponse<V1ExecuteQueryResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.flowServiceExecuteQuery(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        QueryRequest request = QueryRequest.builder().query("SELECT * FROM table1").build();

        QueryResponse response = controller.query(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getFields().size());
        Assert.assertEquals("sky-id-1", response.getFields().get(0).get("skyflowId"));
        Assert.assertNull(response.getErrors());
    }

    @Test
    public void testQuery_apiErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawRecordsClient mockRaw = mockRawRecords(mockApi);
        when(mockRaw.flowServiceExecuteQuery(any(), any()))
                .thenThrow(new ApiClientApiException("query failed", 401, "unauthorized"));

        VaultController controller = createControllerWithMock(mockApi);
        QueryRequest request = QueryRequest.builder().query("SELECT * FROM table1").build();

        try {
            controller.query(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(401, e.getHttpCode());
        }
    }

    @Test
    public void testQuery_networkErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawRecordsClient mockRaw = mockRawRecords(mockApi);
        when(mockRaw.flowServiceExecuteQuery(any(), any()))
                .thenThrow(new ApiClientException("Network error executing HTTP request"));

        VaultController controller = createControllerWithMock(mockApi);
        QueryRequest request = QueryRequest.builder().query("SELECT * FROM table1").build();

        try {
            controller.query(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testQuery_emptyQueryThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);
        QueryRequest request = QueryRequest.builder().build();
        try {
            controller.query(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testQuery_interceptorAddsCustomHeader() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawRecordsClient mockRaw = mockRawRecords(mockApi);

        Map<String, Object> data = new HashMap<>();
        data.put("skyflowId", "sky-id-1");
        V1ExecuteQueryRecordResponse record = V1ExecuteQueryRecordResponse.builder().data(data).build();
        V1ExecuteQueryResponse body = V1ExecuteQueryResponse.builder().records(Collections.singletonList(record)).build();
        ApiClientHttpResponse<V1ExecuteQueryResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.flowServiceExecuteQuery(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);
        QueryRequest request = QueryRequest.builder().query("SELECT * FROM table1").build();

        RequestInterceptor interceptor = ctx -> ctx.addHeader(CustomHeaderKey.SkyflowAccountID, "acct-123");
        QueryOptions options = QueryOptions.builder().interceptor(interceptor).build();

        controller.query(request, options);

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        Mockito.verify(mockRaw).flowServiceExecuteQuery(any(), captor.capture());
        Assert.assertEquals("acct-123", captor.getValue().getHeaders().get(CustomHeaderKey.SkyflowAccountID.toString()));
    }

    // ── get ───────────────────────────────────────────────────────────────────

    @Test
    public void testGet_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        V1RecordResponseObject record = V1RecordResponseObject.builder().skyflowId("sky-id-1").data(data).build();
        V1GetResponse body = V1GetResponse.builder().records(Collections.singletonList(record)).build();
        ApiClientHttpResponse<V1GetResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.get(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        GetRequest request = GetRequest.builder()
                .table("table1")
                .ids(new ArrayList<>(Collections.singletonList("sky-id-1")))
                .build();

        GetResponse response = controller.get(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getData().size());
        Assert.assertEquals("sky-id-1", response.getData().get(0).get("skyflowId"));
        Assert.assertEquals("john", response.getData().get(0).get("name"));
        Assert.assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void testGet_apiErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.get(any(), any()))
                .thenThrow(new ApiClientApiException("get failed", 401, "unauthorized"));

        VaultController controller = createControllerWithMock(mockApi);
        GetRequest request = GetRequest.builder()
                .table("table1")
                .ids(new ArrayList<>(Collections.singletonList("sky-id-1")))
                .build();

        try {
            controller.get(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(401, e.getHttpCode());
        }
    }

    @Test
    public void testGet_networkErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.get(any(), any()))
                .thenThrow(new ApiClientException("Network error executing HTTP request"));

        VaultController controller = createControllerWithMock(mockApi);
        GetRequest request = GetRequest.builder()
                .table("table1")
                .ids(new ArrayList<>(Collections.singletonList("sky-id-1")))
                .build();

        try {
            controller.get(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testGet_invalidRequestThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);
        GetRequest request = GetRequest.builder().table("table1").build();
        try {
            controller.get(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testGet_interceptorAddsCustomHeader() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        V1RecordResponseObject record = V1RecordResponseObject.builder().skyflowId("sky-id-1").data(data).build();
        V1GetResponse body = V1GetResponse.builder().records(Collections.singletonList(record)).build();
        ApiClientHttpResponse<V1GetResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.get(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);
        GetRequest request = GetRequest.builder()
                .table("table1")
                .ids(new ArrayList<>(Collections.singletonList("sky-id-1")))
                .build();

        RequestInterceptor interceptor = ctx -> ctx.addHeader(CustomHeaderKey.SkyflowAccountID, "acct-123");
        GetOptions options = GetOptions.builder().interceptor(interceptor).build();

        controller.get(request, options);

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        Mockito.verify(mockRaw).get(any(), captor.capture());
        Assert.assertEquals("acct-123", captor.getValue().getHeaders().get(CustomHeaderKey.SkyflowAccountID.toString()));
    }

    @Test
    public void testGet_multiTableRecordsSuccess() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        Map<String, Object> data1 = new HashMap<>();
        data1.put("name", "john");
        V1RecordResponseObject record1 = V1RecordResponseObject.builder()
                .skyflowId("sky-id-1").tableName("table1").data(data1).build();
        Map<String, Object> data2 = new HashMap<>();
        data2.put("email", "jane@example.com");
        V1RecordResponseObject record2 = V1RecordResponseObject.builder()
                .skyflowId("sky-id-2").tableName("table2").data(data2).build();
        V1GetResponse body = V1GetResponse.builder().records(java.util.Arrays.asList(record1, record2)).build();
        ApiClientHttpResponse<V1GetResponse> httpResp = new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.get(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        com.skyflow.vault.data.GetRecordRequest recordRequest1 = com.skyflow.vault.data.GetRecordRequest.builder()
                .table("table1")
                .ids(new ArrayList<>(Collections.singletonList("sky-id-1")))
                .build();
        com.skyflow.vault.data.GetRecordRequest recordRequest2 = com.skyflow.vault.data.GetRecordRequest.builder()
                .table("table2")
                .ids(new ArrayList<>(Collections.singletonList("sky-id-2")))
                .build();
        GetRequest request = GetRequest.builder()
                .records(java.util.Arrays.asList(recordRequest1, recordRequest2))
                .build();

        GetResponse response = controller.get(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(2, response.getData().size());
        Assert.assertEquals("table1", response.getData().get(0).get("tableName"));
        Assert.assertEquals("table2", response.getData().get(1).get("tableName"));
    }

    @Test
    public void testGet_bothTableAndRecordsThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);

        com.skyflow.vault.data.GetRecordRequest recordRequest = com.skyflow.vault.data.GetRecordRequest.builder()
                .table("table2")
                .ids(new ArrayList<>(Collections.singletonList("id2")))
                .build();
        GetRequest request = GetRequest.builder()
                .table("table1")
                .ids(new ArrayList<>(Collections.singletonList("id1")))
                .records(Collections.singletonList(recordRequest))
                .build();

        try {
            controller.get(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }
}
