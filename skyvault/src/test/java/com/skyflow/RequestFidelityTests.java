package com.skyflow;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.RedactionType;
import com.skyflow.enums.RequestMethod;
import com.skyflow.enums.TokenMode;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.resources.query.QueryClient;
import com.skyflow.generated.rest.resources.query.requests.QueryServiceExecuteQueryBody;
import com.skyflow.generated.rest.resources.records.RawRecordsClient;
import com.skyflow.generated.rest.resources.records.RecordsClient;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceBatchOperationBody;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceBulkDeleteRecordBody;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceBulkGetRecordRequest;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceInsertRecordBody;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceUpdateRecordBody;
import com.skyflow.generated.rest.resources.records.requests.UploadFileV2Request;
import com.skyflow.generated.rest.resources.records.types.RecordServiceBulkGetRecordRequestOrderBy;
import com.skyflow.generated.rest.resources.records.types.RecordServiceBulkGetRecordRequestRedaction;
import com.skyflow.generated.rest.resources.tokens.requests.V1DetokenizePayload;
import com.skyflow.generated.rest.resources.tokens.requests.V1TokenizePayload;
import com.skyflow.generated.rest.types.BatchRecordMethod;
import com.skyflow.generated.rest.types.RedactionEnumRedaction;
import com.skyflow.generated.rest.types.UploadFileV2Response;
import com.skyflow.generated.rest.types.V1BatchOperationResponse;
import com.skyflow.generated.rest.types.V1BatchRecord;
import com.skyflow.generated.rest.types.V1BulkDeleteRecordResponse;
import com.skyflow.generated.rest.types.V1BulkGetRecordResponse;
import com.skyflow.generated.rest.types.V1Byot;
import com.skyflow.generated.rest.types.V1DetokenizeRecordRequest;
import com.skyflow.generated.rest.types.V1FieldRecords;
import com.skyflow.generated.rest.types.V1GetQueryResponse;
import com.skyflow.generated.rest.types.V1InsertRecordResponse;
import com.skyflow.generated.rest.types.V1TokenizeRecordRequest;
import com.skyflow.generated.rest.types.V1TokenizeResponse;
import com.skyflow.generated.rest.types.V1UpdateRecordResponse;
import com.skyflow.utils.HttpUtility;
import com.skyflow.utils.Utils;
import com.skyflow.vault.connection.InvokeConnectionRequest;
import com.skyflow.vault.controller.ConnectionController;
import com.skyflow.vault.controller.VaultController;
import com.skyflow.vault.data.DeleteRequest;
import com.skyflow.vault.data.FileUploadRequest;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.tokens.ColumnValue;
import com.skyflow.vault.tokens.DetokenizeData;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.TokenizeRequest;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ProtocolException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Request-fidelity tests for the v2 SDK.
 *
 * <p>Every test here builds a request the way an SDK user would, runs it through the real
 * production mapping code ({@link VaultClient}'s request-body builders, {@link VaultController}'s
 * inline builders, {@link Utils} / {@link HttpUtility} for connections) and asserts the exact value
 * that lands on the outgoing generated REST request object.
 *
 * <p>Tests suffixed {@code _knownGap} pin behaviour that is a confirmed defect. They assert the
 * CURRENT behaviour on purpose so that any future change to production code is caught here.
 */
public class RequestFidelityTests {

    private static final String VAULT_ID = "vault123";
    private static final String CLUSTER_ID = "cluster123";
    private static final String API_KEY = "sky-ab123-abcd1234cdef1234abcd4321cdef4321"; // gitleaks:allow
    private static final String CONNECTION_URL = "https://conn.example.com/api/{resource}/details";

    // Non-trivial values reused across tests.
    private static final String NON_ASCII_NAME = "日本語 テスト";
    private static final String NON_ASCII_CITY = "北京市 朝阳区";
    private static final String MULTILINE_NOTE = "line one\nline two with  spaces";

    // ------------------------------------------------------------------
    // harness
    // ------------------------------------------------------------------

    private static VaultConfig testVaultConfig() {
        VaultConfig config = new VaultConfig();
        config.setVaultId(VAULT_ID);
        config.setClusterId(CLUSTER_ID);
        config.setEnv(Env.DEV);
        return config;
    }

    private static Credentials apiKeyCredentials() {
        Credentials credentials = new Credentials();
        credentials.setApiKey(API_KEY);
        return credentials;
    }

    /**
     * VaultClient's request-body builders are {@code protected} and this test lives in the same
     * {@code com.skyflow} package, so they can be exercised directly with no mocking at all.
     */
    private static VaultClient newVaultClient() {
        return new VaultClient(testVaultConfig(), apiKeyCredentials());
    }

    private static VaultController newControllerWithMockApi(ApiClient mockApiClient) throws Exception {
        VaultController controller = new VaultController(testVaultConfig(), apiKeyCredentials());
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
                .header(com.skyflow.utils.Constants.REQUEST_ID_HEADER_KEY, "req-fidelity-1")
                .build();
    }

    private static HashMap<String, Object> richRow() {
        HashMap<String, Object> nested = new HashMap<>();
        nested.put("city", NON_ASCII_CITY);
        nested.put("zip", "100000");

        HashMap<String, Object> row = new HashMap<>();
        row.put("name", NON_ASCII_NAME);
        row.put("notes", MULTILINE_NOTE);
        row.put("age", 42);
        row.put("balance", 3.14);
        row.put("active", true);
        row.put("address", nested);
        return row;
    }

    private static ArrayList<HashMap<String, Object>> rows(HashMap<String, Object>... maps) {
        return new ArrayList<>(Arrays.asList(maps));
    }

    private static ArrayList<String> list(String... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    // ==================================================================
    // insert — bulk branch (continueOnError == false)
    // ==================================================================

    @Test
    public void testInsertBulk_everyBuilderValueReachesGeneratedBody() {
        VaultClient client = newVaultClient();

        HashMap<String, Object> row1 = richRow();
        HashMap<String, Object> row2 = new HashMap<>();
        row2.put("name", "second record");

        InsertRequest request = InsertRequest.builder()
                .table("cards")
                .values(rows(row1, row2))
                .returnTokens(true)
                .upsert("email")
                .tokenMode(TokenMode.ENABLE_STRICT)
                .continueOnError(false)
                .build();

        RecordServiceInsertRecordBody body = client.getBulkInsertRequestBody(request);

        Assert.assertTrue("tokenization must carry returnTokens", body.getTokenization().isPresent());
        Assert.assertTrue(body.getTokenization().get());
        Assert.assertEquals("email", body.getUpsert().get());
        Assert.assertEquals(V1Byot.ENABLE_STRICT, body.getByot().get());

        List<V1FieldRecords> records = body.getRecords().get();
        Assert.assertEquals(2, records.size());

        Map<String, Object> fields0 = records.get(0).getFields().get();
        Assert.assertEquals(NON_ASCII_NAME, fields0.get("name"));
        Assert.assertEquals(MULTILINE_NOTE, fields0.get("notes"));
        Assert.assertEquals(42, fields0.get("age"));
        Assert.assertEquals(3.14, (Double) fields0.get("balance"), 0.0);
        Assert.assertEquals(Boolean.TRUE, fields0.get("active"));
        @SuppressWarnings("unchecked")
        Map<String, Object> address = (Map<String, Object>) fields0.get("address");
        Assert.assertEquals(NON_ASCII_CITY, address.get("city"));
        Assert.assertEquals("100000", address.get("zip"));
        Assert.assertEquals("whole values map must be carried verbatim", row1, fields0);

        Assert.assertEquals("second record", records.get(1).getFields().get().get("name"));
        Assert.assertFalse("no tokens supplied -> tokens absent", records.get(0).getTokens().isPresent());
    }

    @Test
    public void testInsertBulk_homogeneousReachesGeneratedBody() {
        VaultClient client = newVaultClient();
        InsertRequest request = InsertRequest.builder()
                .table("cards")
                .values(rows(richRow()))
                .homogeneous(true)
                .build();

        RecordServiceInsertRecordBody body = client.getBulkInsertRequestBody(request);
        Assert.assertTrue(body.getHomogeneous().isPresent());
        Assert.assertTrue(body.getHomogeneous().get());
    }

    @Test
    public void testInsertBulk_defaultsReachGeneratedBody() {
        VaultClient client = newVaultClient();
        InsertRequest request = InsertRequest.builder()
                .table("cards")
                .values(rows(richRow()))
                .build();

        RecordServiceInsertRecordBody body = client.getBulkInsertRequestBody(request);
        Assert.assertFalse("returnTokens defaults to false", body.getTokenization().get());
        Assert.assertEquals("tokenMode defaults to DISABLE -> byot DISABLE", V1Byot.DISABLE, body.getByot().get());
        Assert.assertFalse("upsert not set -> absent", body.getUpsert().isPresent());
        Assert.assertFalse("homogeneous not set -> absent", body.getHomogeneous().isPresent());
    }

    @Test
    public void testInsertBulk_tokensPairPositionallyWithValues() {
        VaultClient client = newVaultClient();

        HashMap<String, Object> value0 = new HashMap<>();
        value0.put("card_number", "4111111111111111");
        HashMap<String, Object> value1 = new HashMap<>();
        value1.put("card_number", "5111111111111111");

        HashMap<String, Object> token0 = new HashMap<>();
        token0.put("card_number", "token-for-row-0");
        HashMap<String, Object> token1 = new HashMap<>();
        token1.put("card_number", "token-for-row-1");

        InsertRequest request = InsertRequest.builder()
                .table("cards")
                .values(rows(value0, value1))
                .tokens(rows(token0, token1))
                .tokenMode(TokenMode.ENABLE_STRICT)
                .build();

        List<V1FieldRecords> records = client.getBulkInsertRequestBody(request).getRecords().get();

        Assert.assertEquals("4111111111111111", records.get(0).getFields().get().get("card_number"));
        Assert.assertEquals("token-for-row-0", records.get(0).getTokens().get().get("card_number"));
        Assert.assertEquals("5111111111111111", records.get(1).getFields().get().get("card_number"));
        Assert.assertEquals("token-for-row-1", records.get(1).getTokens().get().get("card_number"));
    }

    @Test
    public void testInsertBulk_recordOrderIsPreserved() {
        VaultClient client = newVaultClient();
        ArrayList<HashMap<String, Object>> values = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            HashMap<String, Object> row = new HashMap<>();
            row.put("position", i);
            values.add(row);
        }

        InsertRequest request = InsertRequest.builder().table("cards").values(values).build();
        List<V1FieldRecords> records = client.getBulkInsertRequestBody(request).getRecords().get();

        Assert.assertEquals(5, records.size());
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals("record order must be preserved", i, records.get(i).getFields().get().get("position"));
        }
    }

    @Test
    public void testInsertBulk_fewerTokensThanValuesLeavesTrailingRecordsWithoutTokens() {
        VaultClient client = newVaultClient();

        HashMap<String, Object> value0 = new HashMap<>();
        value0.put("card_number", "4111111111111111");
        HashMap<String, Object> value1 = new HashMap<>();
        value1.put("card_number", "5111111111111111");
        HashMap<String, Object> token0 = new HashMap<>();
        token0.put("card_number", "token-0");

        InsertRequest request = InsertRequest.builder()
                .table("cards")
                .values(rows(value0, value1))
                .tokens(rows(token0))
                .tokenMode(TokenMode.ENABLE)
                .build();

        List<V1FieldRecords> records = client.getBulkInsertRequestBody(request).getRecords().get();
        Assert.assertEquals(2, records.size());
        Assert.assertEquals("token-0", records.get(0).getTokens().get().get("card_number"));
        Assert.assertFalse("record without a matching token entry gets no tokens",
                records.get(1).getTokens().isPresent());
    }

    // ==================================================================
    // insert — batch branch (continueOnError == true)
    // ==================================================================

    @Test
    public void testInsertBatch_everyBuilderValueReachesGeneratedBody() {
        VaultClient client = newVaultClient();

        HashMap<String, Object> row1 = richRow();
        HashMap<String, Object> row2 = new HashMap<>();
        row2.put("name", "second record");

        InsertRequest request = InsertRequest.builder()
                .table("cards")
                .values(rows(row1, row2))
                .returnTokens(true)
                .upsert("email")
                .tokenMode(TokenMode.ENABLE)
                .continueOnError(true)
                .build();

        RecordServiceBatchOperationBody body = client.getBatchInsertRequestBody(request);

        Assert.assertTrue("batch body always sends continueOnError=true", body.getContinueOnError().get());
        Assert.assertEquals(V1Byot.ENABLE, body.getByot().get());

        List<V1BatchRecord> records = body.getRecords().get();
        Assert.assertEquals(2, records.size());

        V1BatchRecord first = records.get(0);
        Assert.assertEquals("table maps to per-record tableName", "cards", first.getTableName().get());
        Assert.assertEquals(BatchRecordMethod.POST, first.getMethod().get());
        Assert.assertEquals("email", first.getUpsert().get());
        Assert.assertTrue(first.getTokenization().get());
        Assert.assertEquals("whole values map must be carried verbatim", row1, first.getFields().get());
        Assert.assertEquals(NON_ASCII_NAME, first.getFields().get().get("name"));

        Assert.assertEquals("cards", records.get(1).getTableName().get());
        Assert.assertEquals("second record", records.get(1).getFields().get().get("name"));
    }

    @Test
    public void testInsertBatch_tokensPairPositionallyAndOrderIsPreserved() {
        VaultClient client = newVaultClient();

        ArrayList<HashMap<String, Object>> values = new ArrayList<>();
        ArrayList<HashMap<String, Object>> tokens = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            HashMap<String, Object> row = new HashMap<>();
            row.put("position", i);
            values.add(row);

            HashMap<String, Object> token = new HashMap<>();
            token.put("position", "token-" + i);
            tokens.add(token);
        }

        InsertRequest request = InsertRequest.builder()
                .table("cards")
                .values(values)
                .tokens(tokens)
                .tokenMode(TokenMode.ENABLE_STRICT)
                .continueOnError(true)
                .build();

        List<V1BatchRecord> records = client.getBatchInsertRequestBody(request).getRecords().get();
        Assert.assertEquals(4, records.size());
        for (int i = 0; i < 4; i++) {
            Assert.assertEquals(i, records.get(i).getFields().get().get("position"));
            Assert.assertEquals("token-" + i, records.get(i).getTokens().get().get("position"));
        }
    }

    /**
     * KNOWN GAP: {@code homogeneous} is silently dropped when {@code continueOnError(true)} routes
     * the insert down the batch path — the batch body has no field able to carry it.
     */
    @Test
    public void testInsertBatch_homogeneousIsDropped_knownGap() {
        VaultClient client = newVaultClient();

        InsertRequest bulkRequest = InsertRequest.builder()
                .table("cards").values(rows(richRow())).homogeneous(true).continueOnError(false).build();
        RecordServiceInsertRecordBody bulkBody = client.getBulkInsertRequestBody(bulkRequest);
        Assert.assertTrue("bulk branch DOES send homogeneous", bulkBody.getHomogeneous().get());
        Assert.assertTrue(bulkBody.toString().contains("homogeneous"));

        InsertRequest batchRequest = InsertRequest.builder()
                .table("cards").values(rows(richRow())).homogeneous(true).continueOnError(true).build();
        RecordServiceBatchOperationBody batchBody = client.getBatchInsertRequestBody(batchRequest);

        for (Method method : RecordServiceBatchOperationBody.class.getMethods()) {
            Assert.assertFalse("batch body has no homogeneous accessor",
                    method.getName().toLowerCase(Locale.ROOT).contains("homogeneous"));
        }
        for (Method method : V1BatchRecord.class.getMethods()) {
            Assert.assertFalse("batch record has no homogeneous accessor",
                    method.getName().toLowerCase(Locale.ROOT).contains("homogeneous"));
        }
        Assert.assertTrue(batchBody.getAdditionalProperties().isEmpty());
        Assert.assertFalse("homogeneous never reaches the wire on the batch branch",
                batchBody.toString().contains("homogeneous"));
    }

    @Test
    public void testInsert_continueOnErrorFalseUsesBulkEndpointWithTableAsPathParam() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);
        when(mockRecords.recordServiceInsertRecord(anyString(), anyString(), any()))
                .thenReturn(V1InsertRecordResponse.builder().build());

        VaultController controller = newControllerWithMockApi(mockApi);
        InsertRequest request = InsertRequest.builder()
                .table("cards").values(rows(richRow())).returnTokens(true).continueOnError(false).build();
        controller.insert(request);

        ArgumentCaptor<RecordServiceInsertRecordBody> bodyCaptor =
                ArgumentCaptor.forClass(RecordServiceInsertRecordBody.class);
        Mockito.verify(mockRecords).recordServiceInsertRecord(eq(VAULT_ID), eq("cards"), bodyCaptor.capture());
        Assert.assertTrue(bodyCaptor.getValue().getTokenization().get());
        Mockito.verify(mockRecords, Mockito.never()).withRawResponse();
    }

    @Test
    public void testInsert_continueOnErrorTrueUsesBatchEndpoint() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        RawRecordsClient mockRawRecords = Mockito.mock(RawRecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);
        when(mockRecords.withRawResponse()).thenReturn(mockRawRecords);
        when(mockRawRecords.recordServiceBatchOperation(anyString(), any(), any()))
                .thenReturn(new com.skyflow.generated.rest.core.ApiClientHttpResponse<>(
                        V1BatchOperationResponse.builder().build(), buildOkHttpResponse()));

        VaultController controller = newControllerWithMockApi(mockApi);
        InsertRequest request = InsertRequest.builder()
                .table("cards").values(rows(richRow())).continueOnError(true).build();
        controller.insert(request);

        ArgumentCaptor<RecordServiceBatchOperationBody> bodyCaptor =
                ArgumentCaptor.forClass(RecordServiceBatchOperationBody.class);
        Mockito.verify(mockRawRecords).recordServiceBatchOperation(eq(VAULT_ID), bodyCaptor.capture(), any());
        Assert.assertTrue(bodyCaptor.getValue().getContinueOnError().get());
        Assert.assertEquals("cards", bodyCaptor.getValue().getRecords().get().get(0).getTableName().get());
        Mockito.verify(mockRecords, Mockito.never()).recordServiceInsertRecord(anyString(), anyString(), any());
    }

    // ==================================================================
    // update
    // ==================================================================

    @Test
    public void testUpdate_everyBuilderValueReachesGeneratedRequest() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);
        when(mockRecords.recordServiceUpdateRecord(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(V1UpdateRecordResponse.builder().skyflowId("sky-id-1").build());

        VaultController controller = newControllerWithMockApi(mockApi);

        HashMap<String, Object> data = new HashMap<>();
        data.put("skyflowId", "sky-id-1");
        data.put("name", NON_ASCII_NAME);
        data.put("notes", MULTILINE_NOTE);
        data.put("age", 42);

        HashMap<String, Object> tokens = new HashMap<>();
        tokens.put("name", "token-for-name");

        UpdateRequest request = UpdateRequest.builder()
                .table("cards")
                .data(data)
                .tokens(tokens)
                .returnTokens(true)
                .tokenMode(TokenMode.ENABLE)
                .build();

        controller.update(request);

        ArgumentCaptor<String> tableCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<RecordServiceUpdateRecordBody> bodyCaptor =
                ArgumentCaptor.forClass(RecordServiceUpdateRecordBody.class);
        Mockito.verify(mockRecords).recordServiceUpdateRecord(
                eq(VAULT_ID), tableCaptor.capture(), idCaptor.capture(), bodyCaptor.capture(), any());

        Assert.assertEquals("cards", tableCaptor.getValue());
        Assert.assertEquals("skyflowId becomes the path id", "sky-id-1", idCaptor.getValue());

        RecordServiceUpdateRecordBody body = bodyCaptor.getValue();
        Assert.assertTrue(body.getTokenization().get());
        Assert.assertEquals(V1Byot.ENABLE, body.getByot().get());

        Map<String, Object> fields = body.getRecord().get().getFields().get();
        Assert.assertFalse("skyflowId must be stripped from fields", fields.containsKey("skyflowId"));
        Assert.assertEquals(NON_ASCII_NAME, fields.get("name"));
        Assert.assertEquals(MULTILINE_NOTE, fields.get("notes"));
        Assert.assertEquals(42, fields.get("age"));
        Assert.assertEquals(3, fields.size());

        Assert.assertEquals("token-for-name", body.getRecord().get().getTokens().get().get("name"));
    }

    @Test
    public void testUpdate_deprecatedSkyflowIdKeyBecomesPathIdAndIsStripped() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);
        when(mockRecords.recordServiceUpdateRecord(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(V1UpdateRecordResponse.builder().skyflowId("snake-id").build());

        VaultController controller = newControllerWithMockApi(mockApi);

        HashMap<String, Object> data = new HashMap<>();
        data.put("skyflow_id", "snake-id");
        data.put("name", NON_ASCII_NAME);

        controller.update(UpdateRequest.builder().table("cards").data(data).build());

        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<RecordServiceUpdateRecordBody> bodyCaptor =
                ArgumentCaptor.forClass(RecordServiceUpdateRecordBody.class);
        Mockito.verify(mockRecords).recordServiceUpdateRecord(
                anyString(), anyString(), idCaptor.capture(), bodyCaptor.capture(), any());

        Assert.assertEquals("snake-id", idCaptor.getValue());
        Map<String, Object> fields = bodyCaptor.getValue().getRecord().get().getFields().get();
        Assert.assertFalse(fields.containsKey("skyflow_id"));
        Assert.assertEquals(NON_ASCII_NAME, fields.get("name"));
    }

    @Test
    public void testUpdate_defaultsReachGeneratedRequest() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);
        when(mockRecords.recordServiceUpdateRecord(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(V1UpdateRecordResponse.builder().skyflowId("sky-id-1").build());

        VaultController controller = newControllerWithMockApi(mockApi);

        HashMap<String, Object> data = new HashMap<>();
        data.put("skyflowId", "sky-id-1");
        data.put("name", "plain");

        controller.update(UpdateRequest.builder().table("cards").data(data).build());

        ArgumentCaptor<RecordServiceUpdateRecordBody> bodyCaptor =
                ArgumentCaptor.forClass(RecordServiceUpdateRecordBody.class);
        Mockito.verify(mockRecords).recordServiceUpdateRecord(
                anyString(), anyString(), anyString(), bodyCaptor.capture(), any());

        Assert.assertFalse("returnTokens defaults to false", bodyCaptor.getValue().getTokenization().get());
        Assert.assertEquals(V1Byot.DISABLE, bodyCaptor.getValue().getByot().get());
        Assert.assertFalse("no tokens supplied -> tokens absent",
                bodyCaptor.getValue().getRecord().get().getTokens().isPresent());
    }

    /**
     * KNOWN GAP: {@code update()} removes {@code skyflowId} from the caller's own data map, so the
     * same request object cannot be reused — a second call fails validation.
     */
    @Test
    public void testUpdate_mutatesCallersDataMap_knownGap() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);
        when(mockRecords.recordServiceUpdateRecord(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(V1UpdateRecordResponse.builder().skyflowId("sky-id-1").build());

        VaultController controller = newControllerWithMockApi(mockApi);

        HashMap<String, Object> data = new HashMap<>();
        data.put("skyflowId", "sky-id-1");
        data.put("name", NON_ASCII_NAME);

        UpdateRequest request = UpdateRequest.builder().table("cards").data(data).build();
        controller.update(request);

        Assert.assertFalse("caller's map is mutated: skyflowId removed", data.containsKey("skyflowId"));
        Assert.assertSame("request still hands back the same (now-mutated) map", data, request.getData());

        try {
            controller.update(request);
            Assert.fail("second update with the same request object should fail");
        } catch (SkyflowException e) {
            Assert.assertTrue("fails because skyflowId is gone from the caller's map: " + e.getMessage(),
                    e.getMessage().contains("'skyflow_id' is missing from the data payload"));
        }
    }

    // ==================================================================
    // get
    // ==================================================================

    private static VaultController mockGetController(RecordsClient mockRecords) throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        when(mockApi.records()).thenReturn(mockRecords);
        when(mockRecords.recordServiceBulkGetRecord(anyString(), anyString(), any(), any()))
                .thenReturn(V1BulkGetRecordResponse.builder()
                        .records(Collections.<V1FieldRecords>emptyList())
                        .build());
        return newControllerWithMockApi(mockApi);
    }

    @Test
    public void testGet_idsFieldsRedactionAndPagingReachGeneratedRequest() throws Exception {
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        VaultController controller = mockGetController(mockRecords);

        GetRequest request = GetRequest.builder()
                .table("cards")
                .ids(list("id-1", "id-2", "id-3"))
                .fields(list("name", "card_number"))
                .redactionType(RedactionType.PLAIN_TEXT)
                .offset("10")
                .limit("25")
                .downloadUrl(false)
                .orderBy("DESCENDING")
                .build();

        controller.get(request);

        ArgumentCaptor<String> tableCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<RecordServiceBulkGetRecordRequest> requestCaptor =
                ArgumentCaptor.forClass(RecordServiceBulkGetRecordRequest.class);
        Mockito.verify(mockRecords).recordServiceBulkGetRecord(
                eq(VAULT_ID), tableCaptor.capture(), requestCaptor.capture(), any());

        Assert.assertEquals("cards", tableCaptor.getValue());
        RecordServiceBulkGetRecordRequest generated = requestCaptor.getValue();
        Assert.assertEquals("ids map to skyflowIds, order preserved",
                Arrays.asList("id-1", "id-2", "id-3"), generated.getSkyflowIds().get());
        Assert.assertEquals(Arrays.asList("name", "card_number"), generated.getFields().get());
        Assert.assertEquals(RecordServiceBulkGetRecordRequestRedaction.PLAIN_TEXT, generated.getRedaction().get());
        Assert.assertEquals("10", generated.getOffset().get());
        Assert.assertEquals("25", generated.getLimit().get());
        Assert.assertFalse("downloadUrl(false) must reach downloadURL", generated.getDownloadUrl().get());
        Assert.assertEquals(RecordServiceBulkGetRecordRequestOrderBy.DESCENDING, generated.getOrderBy().get());
        Assert.assertFalse(generated.getColumnName().isPresent());
        Assert.assertFalse(generated.getColumnValues().isPresent());
    }

    @Test
    public void testGet_columnNameAndColumnValuesReachGeneratedRequest() throws Exception {
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        VaultController controller = mockGetController(mockRecords);

        GetRequest request = GetRequest.builder()
                .table("cards")
                .columnName("email")
                .columnValues(list("a@example.com", NON_ASCII_NAME, "value with spaces"))
                .redactionType(RedactionType.MASKED)
                .build();

        controller.get(request);

        ArgumentCaptor<RecordServiceBulkGetRecordRequest> requestCaptor =
                ArgumentCaptor.forClass(RecordServiceBulkGetRecordRequest.class);
        Mockito.verify(mockRecords).recordServiceBulkGetRecord(
                anyString(), anyString(), requestCaptor.capture(), any());

        RecordServiceBulkGetRecordRequest generated = requestCaptor.getValue();
        Assert.assertEquals("email", generated.getColumnName().get());
        Assert.assertEquals(Arrays.asList("a@example.com", NON_ASCII_NAME, "value with spaces"),
                generated.getColumnValues().get());
        Assert.assertEquals(RecordServiceBulkGetRecordRequestRedaction.MASKED, generated.getRedaction().get());
        Assert.assertFalse(generated.getSkyflowIds().isPresent());
    }

    @Test
    public void testGet_returnTokensMapsToTokenization() throws Exception {
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        VaultController controller = mockGetController(mockRecords);

        controller.get(GetRequest.builder().table("cards").ids(list("id-1")).returnTokens(true).build());

        ArgumentCaptor<RecordServiceBulkGetRecordRequest> requestCaptor =
                ArgumentCaptor.forClass(RecordServiceBulkGetRecordRequest.class);
        Mockito.verify(mockRecords).recordServiceBulkGetRecord(
                anyString(), anyString(), requestCaptor.capture(), any());

        Assert.assertTrue(requestCaptor.getValue().getTokenization().get());
        Assert.assertFalse("no redactionType -> redaction absent", requestCaptor.getValue().getRedaction().isPresent());
    }

    @Test
    public void testGet_defaultOrderByAndDownloadUrlReachGeneratedRequest() throws Exception {
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        VaultController controller = mockGetController(mockRecords);

        controller.get(GetRequest.builder().table("cards").ids(list("id-1")).build());

        ArgumentCaptor<RecordServiceBulkGetRecordRequest> requestCaptor =
                ArgumentCaptor.forClass(RecordServiceBulkGetRecordRequest.class);
        Mockito.verify(mockRecords).recordServiceBulkGetRecord(
                anyString(), anyString(), requestCaptor.capture(), any());

        Assert.assertEquals(RecordServiceBulkGetRecordRequestOrderBy.ASCENDING,
                requestCaptor.getValue().getOrderBy().get());
        Assert.assertTrue("downloadUrl defaults to true", requestCaptor.getValue().getDownloadUrl().get());
    }

    @Test
    public void testGet_orderByAcceptsAllThreeGeneratedEnumNames() throws Exception {
        for (String orderBy : new String[]{"ASCENDING", "DESCENDING", "NONE"}) {
            RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
            VaultController controller = mockGetController(mockRecords);

            controller.get(GetRequest.builder().table("cards").ids(list("id-1")).orderBy(orderBy).build());

            ArgumentCaptor<RecordServiceBulkGetRecordRequest> requestCaptor =
                    ArgumentCaptor.forClass(RecordServiceBulkGetRecordRequest.class);
            Mockito.verify(mockRecords).recordServiceBulkGetRecord(
                    anyString(), anyString(), requestCaptor.capture(), any());
            Assert.assertEquals(RecordServiceBulkGetRecordRequestOrderBy.valueOf(orderBy),
                    requestCaptor.getValue().getOrderBy().get());
        }
    }

    /**
     * FIXED: {@code orderBy} is now validated against the allowed set ({@code ASCENDING},
     * {@code DESCENDING}, {@code NONE}) before it ever reaches {@code Enum.valueOf}, so an
     * unrecognised value throws a clean {@link SkyflowException} instead of a raw
     * {@link IllegalArgumentException}.
     */
    @Test
    public void testGet_orderByInvalidValueThrowsSkyflowException() throws Exception {
        for (String orderBy : new String[]{"DESC", "descending", "asc"}) {
            RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
            VaultController controller = mockGetController(mockRecords);
            try {
                controller.get(GetRequest.builder().table("cards").ids(list("id-1")).orderBy(orderBy).build());
                Assert.fail("expected an exception for orderBy=" + orderBy);
            } catch (SkyflowException e) {
                Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
                Assert.assertEquals(ErrorMessage.InvalidOrderBy.getMessage(), e.getMessage());
            }
            Mockito.verify(mockRecords, Mockito.never())
                    .recordServiceBulkGetRecord(anyString(), anyString(), any(), any());
        }
    }

    /**
     * KNOWN GAP: {@code downloadUrl(null)} silently coerces to {@code true}, while
     * {@code returnTokens(null)} keeps the null (and therefore disappears from the request).
     */
    @Test
    public void testGet_downloadUrlNullCoercesToTrueButReturnTokensNullDoesNot_knownGap() throws Exception {
        GetRequest request = GetRequest.builder()
                .table("cards")
                .ids(list("id-1"))
                .downloadUrl(null)
                .returnTokens(null)
                .build();

        Assert.assertTrue("downloadUrl(null) coerces to true", request.getDownloadUrl());
        Assert.assertNull("returnTokens(null) stays null", request.getReturnTokens());

        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        VaultController controller = mockGetController(mockRecords);
        controller.get(request);

        ArgumentCaptor<RecordServiceBulkGetRecordRequest> requestCaptor =
                ArgumentCaptor.forClass(RecordServiceBulkGetRecordRequest.class);
        Mockito.verify(mockRecords).recordServiceBulkGetRecord(
                anyString(), anyString(), requestCaptor.capture(), any());

        Assert.assertTrue("downloadURL=true is sent even though the user passed null",
                requestCaptor.getValue().getDownloadUrl().get());
        Assert.assertFalse("tokenization is omitted for a null returnTokens",
                requestCaptor.getValue().getTokenization().isPresent());
    }

    // ==================================================================
    // delete
    // ==================================================================

    @Test
    public void testDelete_tableAndIdsReachGeneratedRequest() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        when(mockApi.records()).thenReturn(mockRecords);
        when(mockRecords.recordServiceBulkDeleteRecord(anyString(), anyString(), any(), any()))
                .thenReturn(V1BulkDeleteRecordResponse.builder().build());

        VaultController controller = newControllerWithMockApi(mockApi);
        controller.delete(DeleteRequest.builder()
                .table("cards")
                .ids(list("id-3", "id-1", "id-2"))
                .build());

        ArgumentCaptor<String> tableCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<RecordServiceBulkDeleteRecordBody> bodyCaptor =
                ArgumentCaptor.forClass(RecordServiceBulkDeleteRecordBody.class);
        Mockito.verify(mockRecords).recordServiceBulkDeleteRecord(
                eq(VAULT_ID), tableCaptor.capture(), bodyCaptor.capture(), any());

        Assert.assertEquals("cards", tableCaptor.getValue());
        Assert.assertEquals("ids map to skyflowIds, order preserved",
                Arrays.asList("id-3", "id-1", "id-2"), bodyCaptor.getValue().getSkyflowIds().get());
    }

    // ==================================================================
    // query
    // ==================================================================

    @Test
    public void testQuery_queryStringReachesGeneratedBodyVerbatim() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        QueryClient mockQuery = Mockito.mock(QueryClient.class);
        when(mockApi.query()).thenReturn(mockQuery);
        when(mockQuery.queryServiceExecuteQuery(anyString(), any(), any()))
                .thenReturn(V1GetQueryResponse.builder().build());

        VaultController controller = newControllerWithMockApi(mockApi);

        String sql = "SELECT name, \"card number\"\nFROM cards\n"
                + "WHERE name = 'Smith''s' AND city = '" + NON_ASCII_CITY + "'\nLIMIT 10";
        controller.query(QueryRequest.builder().query(sql).build());

        ArgumentCaptor<QueryServiceExecuteQueryBody> bodyCaptor =
                ArgumentCaptor.forClass(QueryServiceExecuteQueryBody.class);
        Mockito.verify(mockQuery).queryServiceExecuteQuery(eq(VAULT_ID), bodyCaptor.capture(), any());

        Assert.assertEquals("query with quotes and newlines must be sent unchanged",
                sql, bodyCaptor.getValue().getQuery().get());
    }

    @Test
    public void testQuery_simpleQueryReachesGeneratedBody() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        QueryClient mockQuery = Mockito.mock(QueryClient.class);
        when(mockApi.query()).thenReturn(mockQuery);
        when(mockQuery.queryServiceExecuteQuery(anyString(), any(), any()))
                .thenReturn(V1GetQueryResponse.builder().build());

        VaultController controller = newControllerWithMockApi(mockApi);
        controller.query(QueryRequest.builder().query("SELECT * FROM cards LIMIT 1").build());

        ArgumentCaptor<QueryServiceExecuteQueryBody> bodyCaptor =
                ArgumentCaptor.forClass(QueryServiceExecuteQueryBody.class);
        Mockito.verify(mockQuery).queryServiceExecuteQuery(anyString(), bodyCaptor.capture(), any());
        Assert.assertEquals("SELECT * FROM cards LIMIT 1", bodyCaptor.getValue().getQuery().get());
    }

    // ==================================================================
    // tokenize
    // ==================================================================

    @Test
    public void testTokenize_columnValuesReachPayloadInOrder() {
        VaultClient client = newVaultClient();

        List<ColumnValue> columnValues = Arrays.asList(
                ColumnValue.builder().value("4111111111111111").columnGroup("cg_cards").build(),
                ColumnValue.builder().value(NON_ASCII_NAME).columnGroup("cg_names").build(),
                ColumnValue.builder().value("value with  spaces").columnGroup("cg_misc").build());

        V1TokenizePayload payload = client.getTokenizePayload(
                TokenizeRequest.builder().values(columnValues).build());

        List<V1TokenizeRecordRequest> parameters = payload.getTokenizationParameters().get();
        Assert.assertEquals(3, parameters.size());
        Assert.assertEquals("4111111111111111", parameters.get(0).getValue().get());
        Assert.assertEquals("cg_cards", parameters.get(0).getColumnGroup().get());
        Assert.assertEquals(NON_ASCII_NAME, parameters.get(1).getValue().get());
        Assert.assertEquals("cg_names", parameters.get(1).getColumnGroup().get());
        Assert.assertEquals("value with  spaces", parameters.get(2).getValue().get());
        Assert.assertEquals("cg_misc", parameters.get(2).getColumnGroup().get());
    }

    @Test
    public void testTokenize_payloadReachesGeneratedApiCall() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        com.skyflow.generated.rest.resources.tokens.TokensClient mockTokens =
                Mockito.mock(com.skyflow.generated.rest.resources.tokens.TokensClient.class);
        when(mockApi.tokens()).thenReturn(mockTokens);
        when(mockTokens.recordServiceTokenize(anyString(), any(), any()))
                .thenReturn(V1TokenizeResponse.builder().build());

        VaultController controller = newControllerWithMockApi(mockApi);
        controller.tokenize(TokenizeRequest.builder().values(Collections.singletonList(
                ColumnValue.builder().value(NON_ASCII_NAME).columnGroup("cg_names").build())).build());

        ArgumentCaptor<V1TokenizePayload> payloadCaptor = ArgumentCaptor.forClass(V1TokenizePayload.class);
        Mockito.verify(mockTokens).recordServiceTokenize(eq(VAULT_ID), payloadCaptor.capture(), any());

        V1TokenizeRecordRequest parameter = payloadCaptor.getValue().getTokenizationParameters().get().get(0);
        Assert.assertEquals(NON_ASCII_NAME, parameter.getValue().get());
        Assert.assertEquals("cg_names", parameter.getColumnGroup().get());
    }

    // ==================================================================
    // detokenize
    // ==================================================================

    @Test
    public void testDetokenize_eachTokenKeepsItsOwnRedactionType() {
        VaultClient client = newVaultClient();

        ArrayList<DetokenizeData> detokenizeData = new ArrayList<>(Arrays.asList(
                new DetokenizeData("tok-default", RedactionType.DEFAULT),
                new DetokenizeData("tok-plain", RedactionType.PLAIN_TEXT),
                new DetokenizeData("tok-masked", RedactionType.MASKED),
                new DetokenizeData("tok-redacted", RedactionType.REDACTED)));

        V1DetokenizePayload payload = client.getDetokenizePayload(
                DetokenizeRequest.builder().detokenizeData(detokenizeData).build());

        List<V1DetokenizeRecordRequest> parameters = payload.getDetokenizationParameters().get();
        Assert.assertEquals(4, parameters.size());

        Assert.assertEquals("tok-default", parameters.get(0).getToken().get());
        Assert.assertEquals(RedactionEnumRedaction.DEFAULT, parameters.get(0).getRedaction().get());
        Assert.assertEquals("tok-plain", parameters.get(1).getToken().get());
        Assert.assertEquals(RedactionEnumRedaction.PLAIN_TEXT, parameters.get(1).getRedaction().get());
        Assert.assertEquals("tok-masked", parameters.get(2).getToken().get());
        Assert.assertEquals(RedactionEnumRedaction.MASKED, parameters.get(2).getRedaction().get());
        Assert.assertEquals("tok-redacted", parameters.get(3).getToken().get());
        Assert.assertEquals(RedactionEnumRedaction.REDACTED, parameters.get(3).getRedaction().get());
    }

    @Test
    public void testDetokenize_tokenWithoutRedactionDefaultsToDefault() {
        VaultClient client = newVaultClient();
        ArrayList<DetokenizeData> detokenizeData = new ArrayList<>(Arrays.asList(
                new DetokenizeData("tok-a"),
                new DetokenizeData("tok-b", null)));

        V1DetokenizePayload payload = client.getDetokenizePayload(
                DetokenizeRequest.builder().detokenizeData(detokenizeData).build());

        List<V1DetokenizeRecordRequest> parameters = payload.getDetokenizationParameters().get();
        Assert.assertEquals(RedactionEnumRedaction.DEFAULT, parameters.get(0).getRedaction().get());
        Assert.assertEquals(RedactionEnumRedaction.DEFAULT, parameters.get(1).getRedaction().get());
    }

    @Test
    public void testDetokenize_continueOnErrorAndDownloadUrlReachPayload() {
        VaultClient client = newVaultClient();
        ArrayList<DetokenizeData> detokenizeData =
                new ArrayList<>(Collections.singletonList(new DetokenizeData("tok-a")));

        V1DetokenizePayload payload = client.getDetokenizePayload(DetokenizeRequest.builder()
                .detokenizeData(detokenizeData)
                .continueOnError(true)
                .downloadUrl(true)
                .build());
        Assert.assertTrue(payload.getContinueOnError().get());
        Assert.assertTrue(payload.getDownloadUrl().get());

        V1DetokenizePayload defaults = client.getDetokenizePayload(DetokenizeRequest.builder()
                .detokenizeData(detokenizeData)
                .build());
        Assert.assertFalse("continueOnError defaults to false", defaults.getContinueOnError().get());
        Assert.assertFalse("downloadUrl defaults to false", defaults.getDownloadUrl().get());
    }

    @Test
    public void testDetokenize_tokenOrderIsPreserved() {
        VaultClient client = newVaultClient();
        ArrayList<DetokenizeData> detokenizeData = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            detokenizeData.add(new DetokenizeData("tok-" + i, RedactionType.MASKED));
        }

        List<V1DetokenizeRecordRequest> parameters = client.getDetokenizePayload(
                        DetokenizeRequest.builder().detokenizeData(detokenizeData).build())
                .getDetokenizationParameters().get();

        for (int i = 0; i < 5; i++) {
            Assert.assertEquals("tok-" + i, parameters.get(i).getToken().get());
        }
    }

    // ==================================================================
    // uploadFile
    // ==================================================================

    private static VaultController mockUploadController(RecordsClient mockRecords) throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        when(mockApi.records()).thenReturn(mockRecords);
        when(mockRecords.uploadFileV2(anyString(), any(File.class), any(), any()))
                .thenReturn(UploadFileV2Response.builder().build());
        return newControllerWithMockApi(mockApi);
    }

    @Test
    public void testUploadFile_metadataFieldsAndFilePathReachGeneratedRequest() throws Exception {
        File tempFile = File.createTempFile("fidelity-upload", ".txt");
        tempFile.deleteOnExit();
        Files.write(tempFile.toPath(), "hello".getBytes("UTF-8"));

        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        VaultController controller = mockUploadController(mockRecords);

        controller.uploadFile(FileUploadRequest.builder()
                .table("cards")
                .columnName("file_column")
                .skyflowId("sky-id-9")
                .filePath(tempFile.getAbsolutePath())
                .build());

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<UploadFileV2Request> requestCaptor = ArgumentCaptor.forClass(UploadFileV2Request.class);
        Mockito.verify(mockRecords).uploadFileV2(
                eq(VAULT_ID), fileCaptor.capture(), requestCaptor.capture(), any());

        UploadFileV2Request generated = requestCaptor.getValue();
        Assert.assertEquals("table maps to tableName", "cards", generated.getTableName());
        Assert.assertEquals("file_column", generated.getColumnName());
        Assert.assertEquals("sky-id-9", generated.getSkyflowId().get());
        Assert.assertFalse("returnFileMetadata is hard-coded to false", generated.getReturnFileMetadata().get());
        Assert.assertEquals(tempFile.getAbsolutePath(), fileCaptor.getValue().getPath());
    }

    @Test
    public void testUploadFile_base64AndFileNameProduceMatchingFile() throws Exception {
        File tempDir = Files.createTempDirectory("fidelity-b64").toFile();
        tempDir.deleteOnExit();
        File target = new File(tempDir, "decoded-upload.txt");
        target.deleteOnExit();

        byte[] payload = "café ☕".getBytes("UTF-8");
        String base64 = Base64.getEncoder().encodeToString(payload);

        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        VaultController controller = mockUploadController(mockRecords);

        controller.uploadFile(FileUploadRequest.builder()
                .table("cards")
                .columnName("file_column")
                .skyflowId("sky-id-b64")
                .base64(base64)
                .fileName(target.getAbsolutePath())
                .build());

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<UploadFileV2Request> requestCaptor = ArgumentCaptor.forClass(UploadFileV2Request.class);
        Mockito.verify(mockRecords).uploadFileV2(
                anyString(), fileCaptor.capture(), requestCaptor.capture(), any());

        Assert.assertEquals(target.getAbsolutePath(), fileCaptor.getValue().getPath());
        Assert.assertArrayEquals("base64 must be decoded into the named file",
                payload, Files.readAllBytes(fileCaptor.getValue().toPath()));
        Assert.assertEquals("sky-id-b64", requestCaptor.getValue().getSkyflowId().get());
    }

    @Test
    public void testUploadFile_fileObjectIsPassedThroughUnchanged() throws Exception {
        File tempFile = File.createTempFile("fidelity-object", ".bin");
        tempFile.deleteOnExit();
        Files.write(tempFile.toPath(), new byte[]{1, 2, 3});

        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        VaultController controller = mockUploadController(mockRecords);

        controller.uploadFile(FileUploadRequest.builder()
                .table("cards")
                .columnName("file_column")
                .fileObject(tempFile)
                .build());

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<UploadFileV2Request> requestCaptor = ArgumentCaptor.forClass(UploadFileV2Request.class);
        Mockito.verify(mockRecords).uploadFileV2(
                anyString(), fileCaptor.capture(), requestCaptor.capture(), any());

        Assert.assertSame("the caller's File instance must be forwarded", tempFile, fileCaptor.getValue());
        Assert.assertFalse("no skyflowId supplied -> absent", requestCaptor.getValue().getSkyflowId().isPresent());
    }

    @Test
    public void testUploadFile_filePathTakesPrecedenceOverOtherSources() throws Exception {
        File pathFile = File.createTempFile("fidelity-precedence-path", ".txt");
        pathFile.deleteOnExit();
        Files.write(pathFile.toPath(), "from-path".getBytes("UTF-8"));

        File objectFile = File.createTempFile("fidelity-precedence-object", ".txt");
        objectFile.deleteOnExit();
        Files.write(objectFile.toPath(), "from-object".getBytes("UTF-8"));

        RecordsClient mockRecords = Mockito.mock(RecordsClient.class);
        VaultController controller = mockUploadController(mockRecords);

        controller.uploadFile(FileUploadRequest.builder()
                .table("cards")
                .columnName("file_column")
                .filePath(pathFile.getAbsolutePath())
                .fileObject(objectFile)
                .build());

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        Mockito.verify(mockRecords).uploadFileV2(anyString(), fileCaptor.capture(), any(), any());
        Assert.assertEquals(pathFile.getAbsolutePath(), fileCaptor.getValue().getPath());
    }

    // ==================================================================
    // invoke — URL construction (com.skyflow.utils.Utils)
    // ==================================================================

    private static ConnectionConfig connectionConfig(String url) {
        ConnectionConfig config = new ConnectionConfig();
        config.setConnectionId("conn123");
        config.setConnectionUrl(url);
        config.setCredentials(apiKeyCredentials());
        return config;
    }

    @Test
    public void testInvoke_pathParamsAreSubstitutedIntoTheUrl() {
        Map<String, String> pathParams = new LinkedHashMap<>();
        pathParams.put("resource", "cards");

        String url = Utils.constructConnectionURL(connectionConfig(CONNECTION_URL),
                InvokeConnectionRequest.builder().pathParams(pathParams).build());

        Assert.assertEquals("https://conn.example.com/api/cards/details", url);
    }

    @Test
    public void testInvoke_queryParamsAreAppendedInOrderWithNoTrailingAmpersand() {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("limit", "10");
        queryParams.put("offset", "20");

        String url = Utils.constructConnectionURL(connectionConfig("https://conn.example.com/api"),
                InvokeConnectionRequest.builder().queryParams(queryParams).build());

        Assert.assertEquals("https://conn.example.com/api?limit=10&offset=20", url);
    }

    /**
     * KNOWN GAP: query-param values are concatenated raw, so a value containing {@code &} or
     * {@code =} re-partitions the query string into extra parameters.
     */
    @Test
    public void testInvoke_queryParamValuesAreNotPercentEncoded_knownGap() {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("q", "a&b=c");

        String url = Utils.constructConnectionURL(connectionConfig("https://conn.example.com/api"),
                InvokeConnectionRequest.builder().queryParams(queryParams).build());

        Assert.assertEquals("value is injected raw", "https://conn.example.com/api?q=a&b=c", url);

        String queryString = url.substring(url.indexOf('?') + 1);
        Assert.assertEquals("one user param has become two wire params", 2, queryString.split("&").length);
        Assert.assertEquals("q=a", queryString.split("&")[0]);
        Assert.assertEquals("b=c", queryString.split("&")[1]);
    }

    /**
     * KNOWN GAP: a query-param value containing a space is injected raw, producing an invalid URI.
     */
    @Test
    public void testInvoke_queryParamValueWithSpaceIsNotEncoded_knownGap() {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("name", "John Doe");

        String url = Utils.constructConnectionURL(connectionConfig("https://conn.example.com/api"),
                InvokeConnectionRequest.builder().queryParams(queryParams).build());

        Assert.assertEquals("https://conn.example.com/api?name=John Doe", url);
        Assert.assertFalse("no %20 encoding is applied", url.contains("%20"));
        try {
            java.net.URI.create(url);
            Assert.fail("raw space should make this an invalid URI");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("Illegal character"));
        }
    }

    /**
     * KNOWN GAP: path-param values are substituted raw, so a value containing {@code /} silently
     * changes the URL's path structure.
     */
    @Test
    public void testInvoke_pathParamValuesAreNotPercentEncoded_knownGap() {
        Map<String, String> pathParams = new LinkedHashMap<>();
        pathParams.put("resource", "cards/123");

        String url = Utils.constructConnectionURL(connectionConfig(CONNECTION_URL),
                InvokeConnectionRequest.builder().pathParams(pathParams).build());

        Assert.assertEquals("https://conn.example.com/api/cards/123/details", url);
        Assert.assertFalse("no %2F encoding is applied", url.toUpperCase(Locale.ROOT).contains("%2F"));
        Assert.assertEquals("the path gained a segment", 5, java.net.URI.create(url).getPath().split("/").length);
    }

    /**
     * KNOWN GAP: a path-param entry with no matching {@code {placeholder}} is discarded silently —
     * no error, no log, and the URL is unchanged.
     */
    @Test
    public void testInvoke_pathParamWithoutMatchingPlaceholderIsSilentlyDiscarded_knownGap() {
        Map<String, String> pathParams = new LinkedHashMap<>();
        pathParams.put("resource", "cards");
        pathParams.put("thisKeyIsNotInTheUrl", "ignored-value");

        String url = Utils.constructConnectionURL(connectionConfig(CONNECTION_URL),
                InvokeConnectionRequest.builder().pathParams(pathParams).build());

        Assert.assertEquals("https://conn.example.com/api/cards/details", url);
        Assert.assertFalse(url.contains("ignored-value"));
        Assert.assertFalse(url.contains("thisKeyIsNotInTheUrl"));
    }

    @Test
    public void testInvoke_unfilledPlaceholderRemainsInTheUrl() {
        String url = Utils.constructConnectionURL(connectionConfig(CONNECTION_URL),
                InvokeConnectionRequest.builder().build());
        Assert.assertEquals("https://conn.example.com/api/{resource}/details", url);
    }

    // ==================================================================
    // invoke — header construction (com.skyflow.utils.Utils)
    // ==================================================================

    @Test
    public void testInvoke_headerKeysAreLowercasedAndValuesPreserved() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", NON_ASCII_NAME);

        Map<String, String> constructed = Utils.constructConnectionHeadersMap(headers);

        Assert.assertEquals(2, constructed.size());
        Assert.assertEquals("application/json", constructed.get("content-type"));
        Assert.assertEquals(NON_ASCII_NAME, constructed.get("x-custom-header"));
    }

    /**
     * KNOWN GAP: header keys are lowercased without a {@link Locale}, and two keys differing only
     * in case collapse into a single entry (last writer wins).
     */
    @Test
    public void testInvoke_headerKeysDifferingOnlyInCaseCollapse_knownGap() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Custom", "first-value");
        headers.put("x-custom", "second-value");
        headers.put("X-CUSTOM", "third-value");

        Map<String, String> constructed = Utils.constructConnectionHeadersMap(headers);

        Assert.assertEquals("three user headers collapse into one", 1, constructed.size());
        Assert.assertEquals("last writer wins", "third-value", constructed.get("x-custom"));
    }

    // ==================================================================
    // invoke — body encoding (com.skyflow.utils.HttpUtility)
    // ==================================================================

    @Test
    public void testInvoke_formEncodedBodyEncodesKeysValuesAndNestedObjects() {
        JsonObject body = new JsonObject();
        body.addProperty("name", "John Doe");
        JsonObject nested = new JsonObject();
        nested.addProperty("city", "New York");
        body.add("address", nested);

        String encoded = HttpUtility.formatJsonToFormEncodedString(body);

        Assert.assertTrue(encoded.contains("name=John+Doe"));
        Assert.assertTrue("nested objects are flattened to key[subkey]",
                encoded.contains("address%5Bcity%5D=New+York"));
    }

    /**
     * KNOWN GAP: with {@code application/x-www-form-urlencoded}, a 1-element JSON array silently
     * loses its array-ness and is sent as a bare scalar.
     */
    @Test
    public void testInvoke_formEncodedSingleElementArrayLosesArrayness_knownGap() {
        JsonObject body = new JsonObject();
        JsonArray items = new JsonArray();
        items.add("only-item");
        body.add("items", items);

        Assert.assertEquals("items=only-item", HttpUtility.formatJsonToFormEncodedString(body));
    }

    /**
     * KNOWN GAP: with {@code application/x-www-form-urlencoded}, an array of 2+ elements blows up
     * with a raw {@link IllegalStateException}.
     */
    @Test
    public void testInvoke_formEncodedMultiElementArrayThrowsIllegalState_knownGap() {
        JsonObject body = new JsonObject();
        JsonArray items = new JsonArray();
        items.add("first");
        items.add("second");
        body.add("items", items);

        try {
            HttpUtility.formatJsonToFormEncodedString(body);
            Assert.fail("expected IllegalStateException for a multi-element array");
        } catch (IllegalStateException expected) {
            Assert.assertTrue(expected.getMessage().contains("2"));
        }
    }

    /**
     * KNOWN GAP: with {@code application/x-www-form-urlencoded}, a JSON null blows up with a raw
     * {@link UnsupportedOperationException}.
     */
    @Test
    public void testInvoke_formEncodedJsonNullThrowsUnsupportedOperation_knownGap() {
        JsonObject body = new JsonObject();
        body.add("maybe", JsonNull.INSTANCE);

        try {
            HttpUtility.formatJsonToFormEncodedString(body);
            Assert.fail("expected UnsupportedOperationException for a JSON null");
        } catch (UnsupportedOperationException expected) {
            Assert.assertEquals("JsonNull", expected.getMessage());
        }
    }

    /**
     * KNOWN GAP: the multipart encoder shares {@code convertJsonToMap} and therefore has exactly
     * the same array/null defects.
     */
    @Test
    public void testInvoke_multipartBodyHasTheSameArrayAndNullDefects_knownGap() {
        JsonObject single = new JsonObject();
        JsonArray oneItem = new JsonArray();
        oneItem.add("only-item");
        single.add("items", oneItem);
        Assert.assertTrue("array-ness is lost",
                HttpUtility.formatJsonToMultiPartFormDataString(single, "bnd").contains("only-item"));

        JsonObject many = new JsonObject();
        JsonArray twoItems = new JsonArray();
        twoItems.add("first");
        twoItems.add("second");
        many.add("items", twoItems);
        try {
            HttpUtility.formatJsonToMultiPartFormDataString(many, "bnd");
            Assert.fail("expected IllegalStateException for a multi-element array");
        } catch (IllegalStateException expected) {
            Assert.assertNotNull(expected.getMessage());
        }

        JsonObject withNull = new JsonObject();
        withNull.add("maybe", JsonNull.INSTANCE);
        try {
            HttpUtility.formatJsonToMultiPartFormDataString(withNull, "bnd");
            Assert.fail("expected UnsupportedOperationException for a JSON null");
        } catch (UnsupportedOperationException expected) {
            Assert.assertEquals("JsonNull", expected.getMessage());
        }
    }

    // ==================================================================
    // invoke — transport
    // ==================================================================

    /**
     * KNOWN GAP: {@link RequestMethod#PATCH} can be built into a request but never reaches the
     * wire — {@code HttpURLConnection.setRequestMethod} rejects it, surfacing as a
     * {@link SkyflowException} wrapping a {@link ProtocolException}.
     */
    @Test
    public void testInvoke_patchMethodCannotBeSent_knownGap() {
        ConnectionController controller =
                new ConnectionController(connectionConfig("https://conn.example.com/api"), apiKeyCredentials());

        try {
            controller.invoke(InvokeConnectionRequest.builder().method(RequestMethod.PATCH).build());
            Assert.fail("PATCH should not be sendable");
        } catch (SkyflowException e) {
            Assert.assertTrue("cause must be a ProtocolException, got: " + e.getCause(),
                    e.getCause() instanceof ProtocolException);
            Assert.assertTrue(e.getCause().getMessage().contains("PATCH"));
        }
    }
}
