package com.skyflow.vault.controller;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Constants;
import com.skyflow.utils.SdkVersion;
import com.skyflow.utils.Utils;
import com.skyflow.vault.data.InsertRequest;
import okhttp3.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

public class VaultControllerTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String vaultID;
    private static String clusterID;
    private static VaultConfig vaultConfig;
    private VaultController vaultController;

    private OkHttpClient mockClient;

    @BeforeClass
    public static void setupClass() {
        vaultID = "vault123";
        clusterID = "cluster123";
        SdkVersion.setSdkPrefix(Constants.SDK_PREFIX);
    }

    @Before
    public void setup() {
//         Create mock interceptor
        Interceptor mockInterceptor = chain -> {
            // Create mock response
            String mockResponseBody = "{\"records\":[{\"skyflowId\":\"test-id-123\",\"tokens\":{}}]}";
            return new Response.Builder()
                    .code(200)
                    .message("OK")
                    .protocol(Protocol.HTTP_1_1)
                    .request(chain.request())
                    .body(ResponseBody.create(
                            MediaType.parse("application/json"),
                            mockResponseBody
                    ))
                    .build();
        };

        // Create client with mock interceptor
        mockClient = new OkHttpClient.Builder()
                .addInterceptor(mockInterceptor)
                .build();
        vaultConfig = new VaultConfig();
        vaultConfig.setVaultId(vaultID);
        vaultConfig.setClusterId(clusterID);
        vaultConfig.setEnv(Env.DEV);

        Credentials credentials = new Credentials();
        credentials.setToken("valid-token");
        vaultConfig.setCredentials(credentials);
        this.vaultController = new VaultController(vaultConfig, credentials);
//        spyController = Mockito.spy(vaultController);
        // Create mock response
//        String mockResponseBody = "{\"records\":[{\"skyflowId\":\"test-id-123\",\"tokens\":{}}]}";
//        ResponseBody responseBody = ResponseBody.create(
//                MediaType.parse("application/json"),
//                mockResponseBody
//        );
//
//        Response mockResponse = new Response.Builder()
//                .code(200)
//                .message("OK")
//                .protocol(Protocol.HTTP_1_1)
//                .request(new Request.Builder().url("https://test.com").build())
//                .body(responseBody)
//                .build();
//
//        // Mock Call
//        Call mockCall = PowerMockito.mock(Call.class);
//        try {
//            PowerMockito.when(mockCall.execute()).thenReturn(mockResponse);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        // Mock OkHttpClient
//        mockClient = PowerMockito.mock(OkHttpClient.class);
//        PowerMockito.when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);


    }

//    @Test
//    public void testBulkInsertSuccess() throws SkyflowException {
//        // Prepare test data
//        ArrayList<HashMap<String, Object>> records = new ArrayList<>();
//        HashMap<String, Object> record = new HashMap<>();
//        record.put("field1", "value1");
//        records.add(record);
//
//        InsertRequest request = InsertRequest.builder()
//                .values(records)
//                .table("test_table")
//                .build();
//
//        // Create mock response
//        List<RecordResponseObject> responseObjects = new ArrayList<>();
//        RecordResponseObject responseObject = RecordResponseObject.builder()
//                .skyflowId("test-id-123")
//                .data(record)
//                .build();
//        responseObjects.add(responseObject);
//
//        InsertResponse mockResponse = InsertResponse.builder()
//                .records(responseObjects)
//                .build();
//
//        InsertResponse resp = InsertResponse.builder().records(responseObjects).build();
//        // Mock insertBatch method
//        when(vaultController.bulkInsert(any()));
//
//        // Execute test
//        com.skyflow.vault.data.InsertResponse response = vaultController.bulkInsert(request);
//
//        // Verify response
//        Assert.assertNotNull(response);
//        Assert.assertNotNull(response.getSuccess());
//        Assert.assertEquals(1, response.getSuccess().size());
//        Assert.assertEquals("test-id-123", response.getSuccess().get(0).getSkyflowId());
//
//        // Verify method was called
////        verify(vaultController).insertBatch(any(), eq("test_table"));
//    }
    @Test
    public void testInvalidRequestInInsertMethod() {
        try {
            InsertRequest request = InsertRequest.builder().build();
            vaultController.bulkInsert(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TableKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }
//    @Test
//    public void testBulkInsertWithApiError3() throws SkyflowException {
//        // Prepare test data
//        ArrayList<HashMap<String, Object>> records = new ArrayList<>();
//        HashMap<String, Object> record = new HashMap<>();
//        record.put("field1", "value1");
//        records.add(record);
//
//        InsertRequest request = InsertRequest.builder()
//                .values(records)
//                .table("test_table")
//                .build();
//
//        try {
//            com.skyflow.vault.data.InsertResponse res = vaultController.bulkInsert(request);
//            String resp = "{\"summary\":{\"total_records\":1,\"total_inserted\":0,\"total_failed\":1},\"errors\":[{\"index\":0,\"error\":\"com.skyflow.generated.rest.core.ApiClientException: Network error executing HTTP request\",\"code\":500}]}";
//            Assert.assertEquals(res.toString(), resp);
//        } catch (SkyflowException e) {
//            Assert.assertEquals(400, e.getHttpCode());
//        }
//    }

//    @Test
//    public void testInsert(){
//        // Prepare test data
//        ArrayList<HashMap<String, Object>> records = new ArrayList<>();
//        HashMap<String, Object> record = new HashMap<>();
//        record.put("field1", "value1");
//        records.add(record);
//
//        InsertRequest request = InsertRequest.builder()
//                .values(records)
//                .table("test_table")
//                .build();
//        List<InsertRecordData> recordDataList = new ArrayList<>();
//        InsertRecordData recordData = InsertRecordData.builder().data(record).build();
//        recordDataList.add(recordData);
//
//        com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest request1 = com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest.builder()
//                .records(recordDataList).vaultId("id").tableName("test_table").build();
//        RecordResponseObject recordResponseObject = RecordResponseObject.builder().data(record).build();
//        List<RecordResponseObject> recordResponseObjects = new ArrayList<>();
//        recordResponseObjects.add(recordResponseObject);
//
////        ApiClient apiClient = PowerMockito.mock(ApiClient.class);
//////        ApiClientBuilder apiClientBuilder = PowerMockito.mock(ApiClientBuilder.class);
////        RecordserviceClient recordserviceClient = PowerMockito.mock(RecordserviceClient.class);
////        apiClient = ApiClient.builder().url("https://demo.com").httpClient(new OkHttpClient()).build();
////        when(recordserviceClient.insert(request1)).thenReturn(apiClient.recordservice().insert(request1));
//
////        PowerMockito.when(OkHttpClient.class).thenReturn(this.mockClient);
//        PowerMockito.mock(OkHttpClient.class);
//
//        try {
//            com.skyflow.vault.data.InsertResponse res = vaultController.bulkInsert(request);
//            String resp = "{\"summary\":{\"total_records\":1,\"total_inserted\":0,\"total_failed\":1},\"errors\":[{\"index\":0,\"error\":\"com.skyflow.generated.rest.core.ApiClientException: Network error executing HTTP request\",\"code\":500}]}";
//            Assert.assertEquals(res.toString(), resp);
//            System.out.println("resppp=>"+ res);
//        } catch (SkyflowException e) {
//            Assert.assertEquals(400, e.getHttpCode());
//        }
//    }
}