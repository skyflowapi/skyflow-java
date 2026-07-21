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
import com.skyflow.generated.rest.types.FlowTokenizeResponseObjectToken;
import com.skyflow.generated.rest.types.V1DeleteTokenResponseObject;
import com.skyflow.generated.rest.types.V1FlowDeleteTokenResponse;
import com.skyflow.generated.rest.types.V1FlowDetokenizeResponse;
import com.skyflow.generated.rest.types.V1FlowDetokenizeResponseObject;
import com.skyflow.generated.rest.types.V1FlowTokenizeResponse;
import com.skyflow.generated.rest.types.V1FlowTokenizeResponseObject;
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
import com.skyflow.vault.data.BulkTokenizeRecord;
import com.skyflow.vault.data.BulkTokenizeRequest;
import com.skyflow.vault.data.BulkTokenizeResponse;
import com.skyflow.vault.data.DeleteTokensRequest;
import com.skyflow.vault.data.DeleteTokensResponse;
import com.skyflow.vault.data.DetokenizeData;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.DetokenizeResponse;
import com.skyflow.vault.data.InsertOptions;
import com.skyflow.vault.data.InsertRecord;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;
import com.skyflow.vault.data.RequestInterceptor;
import com.skyflow.vault.data.TokenizeRecord;
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

    // ── tokenize ──────────────────────────────────────────────────────────────

    @Test
    public void testTokenize_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        FlowTokenizeResponseObjectToken token = FlowTokenizeResponseObjectToken.builder()
                .tokenGroupName("group1")
                .token("tok-abc")
                .build();
        V1FlowTokenizeResponseObject responseObject = V1FlowTokenizeResponseObject.builder()
                .value("value1")
                .tokens(Collections.singletonList(token))
                .build();
        V1FlowTokenizeResponse body = V1FlowTokenizeResponse.builder()
                .response(Collections.singletonList(responseObject))
                .build();
        ApiClientHttpResponse<V1FlowTokenizeResponse> httpResp =
                new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.tokenize(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(TokenizeRecord.builder()
                .value("value1")
                .tokenGroupNames(Collections.singletonList("group1"))
                .build());
        TokenizeRequest request = TokenizeRequest.builder().data(records).build();

        TokenizeResponse response = controller.tokenize(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getTokenizedData().size());
        Assert.assertEquals("tok-abc", response.getTokenizedData().get(0).getTokens().get("group1"));
        Assert.assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void testTokenize_apiErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.tokenize(any(), any()))
                .thenThrow(new ApiClientApiException("tokenize failed", 401, "unauthorized"));

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(TokenizeRecord.builder()
                .value("value1")
                .tokenGroupNames(Collections.singletonList("group1"))
                .build());
        TokenizeRequest request = TokenizeRequest.builder().data(records).build();

        try {
            controller.tokenize(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(401, e.getHttpCode());
        }
    }

    @Test
    public void testTokenize_networkErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.tokenize(any(), any()))
                .thenThrow(new ApiClientException("Network error executing HTTP request"));

        VaultController controller = createControllerWithMock(mockApi);

        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(TokenizeRecord.builder()
                .value("value1")
                .tokenGroupNames(Collections.singletonList("group1"))
                .build());
        TokenizeRequest request = TokenizeRequest.builder().data(records).build();

        try {
            controller.tokenize(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testTokenize_nullRequestThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);
        try {
            controller.tokenize(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    // ── deleteTokens ──────────────────────────────────────────────────────────

    @Test
    public void testDeleteTokens_success() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);

        V1DeleteTokenResponseObject record = V1DeleteTokenResponseObject.builder()
                .value("token1")
                .build();
        V1FlowDeleteTokenResponse body = V1FlowDeleteTokenResponse.builder()
                .tokens(Collections.singletonList(record))
                .build();
        ApiClientHttpResponse<V1FlowDeleteTokenResponse> httpResp =
                new ApiClientHttpResponse<>(body, buildOkHttpResponse());
        when(mockRaw.deletetoken(any(), any())).thenReturn(httpResp);

        VaultController controller = createControllerWithMock(mockApi);

        DeleteTokensRequest request = DeleteTokensRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .build();

        DeleteTokensResponse response = controller.deleteTokens(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(Collections.singletonList("token1"), response.getTokens());
        Assert.assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void testDeleteTokens_apiErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.deletetoken(any(), any()))
                .thenThrow(new ApiClientApiException("delete failed", 404, "not found"));

        VaultController controller = createControllerWithMock(mockApi);

        DeleteTokensRequest request = DeleteTokensRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .build();

        try {
            controller.deleteTokens(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(404, e.getHttpCode());
        }
    }

    @Test
    public void testDeleteTokens_networkErrorThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.deletetoken(any(), any()))
                .thenThrow(new ApiClientException("Network error executing HTTP request"));

        VaultController controller = createControllerWithMock(mockApi);

        DeleteTokensRequest request = DeleteTokensRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .build();

        try {
            controller.deleteTokens(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testDeleteTokens_nullRequestThrowsSkyflowException() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        VaultController controller = createControllerWithMock(mockApi);
        try {
            controller.deleteTokens(null);
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
        Assert.assertEquals(1, response.getSuccess().size());
        Assert.assertEquals("token1", response.getSuccess().get(0).getToken());
        Assert.assertTrue(response.getErrors().isEmpty());
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
        Assert.assertEquals(1, response.getSuccess().size());
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

        ArrayList<BulkTokenizeRecord> records = new ArrayList<>();
        records.add(BulkTokenizeRecord.builder().value("value1").tokenGroupNames(Collections.singletonList("group1")).build());
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().data(records).build();

        BulkTokenizeResponse response = controller.bulkTokenize(request);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getSuccess().size());
        Assert.assertEquals("tok-abc", response.getSuccess().get(0).getTokens().get("group1"));
        Assert.assertTrue(response.getErrors().isEmpty());
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

        ArrayList<BulkTokenizeRecord> records = new ArrayList<>();
        records.add(BulkTokenizeRecord.builder().value("value1").tokenGroupNames(Collections.singletonList("group1")).build());
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().data(records).build();

        BulkTokenizeResponse response = controller.bulkTokenizeAsync(request).get(5, TimeUnit.SECONDS);
        Assert.assertNotNull(INVALID_EXCEPTION_THROWN, response);
        Assert.assertEquals(1, response.getSuccess().size());
    }
}
