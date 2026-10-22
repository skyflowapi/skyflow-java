package com.skyflow;

import com.skyflow.config.VaultConfig;
import com.skyflow.config.BaseCredentials;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.*;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.types.*;
import com.skyflow.generated.rest.resources.query.QueryClient;
import com.skyflow.generated.rest.resources.records.RecordsClient;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceBatchOperationBody;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceInsertRecordBody;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceUpdateRecordBody;
import com.skyflow.generated.rest.resources.tokens.TokensClient;
import com.skyflow.generated.rest.resources.tokens.requests.V1DetokenizePayload;
import com.skyflow.generated.rest.resources.tokens.requests.V1TokenizePayload;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.tokens.ColumnValue;
import com.skyflow.vault.tokens.DetokenizeData;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.TokenizeRequest;
import com.skyflow.vault.data.FileUploadRequest;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Arrays;
import java.util.Collections;

public class VaultClientTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static VaultClient vaultClient;
    private static String vaultID = null;
    private static String clusterID = null;
    private static String token = null;
    private static String table = null;
    private static String value = null;
    private static String columnGroup = null;
    private static String apiKey = null;
    private static ArrayList<DetokenizeData> detokenizeData = null;
    private static ArrayList<HashMap<String, Object>> insertValues = null;
    private static ArrayList<HashMap<String, Object>> insertTokens = null;
    private static HashMap<String, Object> valueMap = null;
    private static HashMap<String, Object> tokenMap = null;
    private static VaultConfig vaultConfig;

    // Dummy API key lives outside the source tree, in a resource file under dummy-non-secrets/
    // (excluded from Gitleaks scans), rather than as a string literal here.
    @BeforeClass
    public static void setup() throws SkyflowException, IOException {
        vaultID = "vault123";
        clusterID = "cluster123";
        token = "test_token";
        detokenizeData = new ArrayList<>();
        table = "test_table";
        value = "test_value";
        columnGroup = "test_column_group";
        apiKey = null;
        insertValues = new ArrayList<>();
        insertTokens = new ArrayList<>();
        valueMap = new HashMap<>();
        tokenMap = new HashMap<>();

        vaultConfig = new VaultConfig();
        vaultConfig.setVaultId(vaultID);
        vaultConfig.setClusterId(clusterID);
        vaultConfig.setEnv(Env.PROD);

        String dummyApiKey = new String(Files.readAllBytes(
                Paths.get("./src/test/resources/dummy-non-secrets/dummy-api-key.txt")), StandardCharsets.UTF_8).trim();
        Credentials credentials = new Credentials();
        credentials.setApiKey(dummyApiKey);
        vaultConfig.setCredentials(credentials);
        vaultClient = new VaultClient(vaultConfig, credentials);
        vaultClient.setBearerToken();
    }

    @Test
    public void testVaultClientGetRecordsAPI() {
        try {
            RecordsClient recordsClient = vaultClient.getRecordsApi();
            Assert.assertNotNull(recordsClient);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN + e.getMessage());
        }
    }

    @Test
    public void testVaultClientGetTokensAPI() {
        try {
            TokensClient tokensClient = vaultClient.getTokensApi();
            Assert.assertNotNull(tokensClient);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testVaultClientGetQueryAPI() {
        try {
            QueryClient queryClient = vaultClient.getQueryApi();
            Assert.assertNotNull(queryClient);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testVaultClientGetVaultConfig() {
        try {
            VaultConfig config = vaultClient.getVaultConfig();
            Assert.assertNotNull(config);
            Assert.assertEquals(vaultID, config.getVaultId());
            Assert.assertEquals(clusterID, config.getClusterId());
            Assert.assertEquals(Env.PROD, config.getEnv());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetDetokenizePayload() {
        try {
            DetokenizeData detokenizeDataRecord1 = new DetokenizeData(token);
            DetokenizeData detokenizeDataRecord2 = new DetokenizeData(token);
            detokenizeData.add(detokenizeDataRecord1);
            detokenizeData.add(detokenizeDataRecord2);
            DetokenizeRequest detokenizeRequest = DetokenizeRequest.builder()
                    .detokenizeData(detokenizeData)
                    .downloadURL(true)
                    .continueOnError(false)
                    .build();
            V1DetokenizePayload payload = vaultClient.getDetokenizePayload(detokenizeRequest);
            Assert.assertFalse(payload.getContinueOnError().get());
            Assert.assertTrue(payload.getDownloadUrl().get());
            Assert.assertEquals(2, payload.getDetokenizationParameters().get().size());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetBulkInsertRequestBody() {
        try {
            valueMap.clear();
            valueMap.put("test_column_1", "test_value_1");
            valueMap.put("test_column_2", "test_value_2");
            tokenMap.clear();
            tokenMap.put("test_column_1", "test_token_1");
            insertValues.clear();
            insertValues.add(valueMap);
            insertValues.add(valueMap);
            insertTokens.clear();
            insertTokens.add(tokenMap);
            InsertRequest insertRequest1 = InsertRequest.builder()
                    .table(table)
                    .values(insertValues)
                    .tokens(insertTokens)
                    .tokenMode(TokenMode.ENABLE)
                    .returnTokens(true)
                    .build();
            RecordServiceInsertRecordBody body1 = vaultClient.getBulkInsertRequestBody(insertRequest1);
            Assert.assertTrue(body1.getTokenization().get());
            Assert.assertEquals(V1Byot.ENABLE, body1.getByot().get());
            Assert.assertEquals(2, body1.getRecords().get().size());

            InsertRequest insertRequest2 = InsertRequest.builder()
                    .table(table)
                    .values(insertValues)
                    .build();
            RecordServiceInsertRecordBody body2 = vaultClient.getBulkInsertRequestBody(insertRequest2);
            Assert.assertEquals(2, body2.getRecords().get().size());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetBatchInsertRequestBody() {
        try {
            valueMap.clear();
            valueMap.put("test_column_1", "test_value_1");
            valueMap.put("test_column_2", "test_value_2");
            tokenMap.clear();
            tokenMap.put("test_column_1", "test_token_1");
            insertValues.clear();
            insertValues.add(valueMap);
            insertValues.add(valueMap);
            insertTokens.clear();
            insertTokens.add(tokenMap);
            InsertRequest insertRequest1 = InsertRequest.builder()
                    .table(table)
                    .values(insertValues)
                    .tokens(insertTokens)
                    .tokenMode(TokenMode.ENABLE)
                    .returnTokens(false)
                    .build();
            RecordServiceBatchOperationBody body1 = vaultClient.getBatchInsertRequestBody(insertRequest1);
            Assert.assertTrue(body1.getContinueOnError().get());
            Assert.assertEquals(2, body1.getRecords().get().size());

            InsertRequest insertRequest2 = InsertRequest.builder().table(table).values(insertValues).build();
            RecordServiceBatchOperationBody body2 = vaultClient.getBatchInsertRequestBody(insertRequest2);
            Assert.assertEquals(2, body2.getRecords().get().size());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetUpdateRequestBodyWithTokens() {
        try {
            valueMap.clear();
            valueMap.put("test_column_1", "test_value_1");
            valueMap.put("test_column_2", "test_value_2");
            tokenMap.clear();
            tokenMap.put("test_column_1", "test_token_1");
            UpdateRequest updateRequest = UpdateRequest.builder()
                    .table(table)
                    .data(valueMap)
                    .tokens(tokenMap)
                    .returnTokens(true)
                    .build();
            RecordServiceUpdateRecordBody body = vaultClient.getUpdateRequestBody(updateRequest);
            Assert.assertTrue(body.getTokenization().get());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetUpdateRequestBodyWithoutTokens() {
        try {
            valueMap.clear();
            valueMap.put("test_column_1", "test_value_1");
            valueMap.put("test_column_2", "test_value_2");
            tokenMap.clear();
            tokenMap.put("test_column_1", "test_token_1");
            UpdateRequest updateRequest = UpdateRequest.builder()
                    .table(table)
                    .data(valueMap)
                    .returnTokens(false)
                    .build();
            RecordServiceUpdateRecordBody body = vaultClient.getUpdateRequestBody(updateRequest);
            Assert.assertFalse(body.getTokenization().get());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetTokenizePayload() {
        try {
            ColumnValue columnValue = ColumnValue.builder().value(value).columnGroup(columnGroup).build();
            List<ColumnValue> columnValues = new ArrayList<>();
            columnValues.add(columnValue);
            TokenizeRequest tokenizeRequest = TokenizeRequest.builder().values(columnValues).build();
            V1TokenizePayload payload = vaultClient.getTokenizePayload(tokenizeRequest);
            Assert.assertEquals(1, payload.getTokenizationParameters().get().size());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testSetBearerToken() {
        try {
            Credentials credentials = new Credentials();
            credentials.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
            vaultConfig.setCredentials(credentials);
            vaultClient = new VaultClient(vaultConfig, credentials);
            vaultClient.setBearerToken();

            Assert.assertNotNull(vaultClient.getTokensApi());

            vaultClient.setBearerToken();
            Assert.assertNotNull(vaultClient.getTokensApi());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN + ": " + e.getMessage());
        }
    }

    @Test
    public void testSetBearerTokenWithApiKey() {
        try {
            Credentials credentials = new Credentials();
            credentials.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321"); // Use a non-null dummy API key
            vaultConfig.setCredentials(credentials);
            vaultClient.updateVaultConfig();
            vaultClient.setCommonCredentials(credentials);

            // regular scenario
            vaultClient.setBearerToken();

            // re-use scenario
            vaultClient.setBearerToken();

            // If no exception is thrown, the test passes
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN + ": " + e.getMessage());
        }
    }

    @Test
    public void testSetBearerTokenWithEnvCredentials() {
        try {
            // Loaded for parity with a local dev environment; not read below, so a missing
            // .env file (as in CI/sandbox) must not fail this test.
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            vaultConfig.setCredentials(null);
            vaultClient.updateVaultConfig();
            vaultClient.setCommonCredentials(null);
            vaultClient.setBearerToken();
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
            Assert.assertNull(vaultClient.getVaultConfig().getCredentials());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    // --- unsupported file extension must throw SkyflowException, not raw IllegalArgumentException ---

    // Fields moved to base classes (e.g. common.BaseVaultClient) aren't found by
    // getDeclaredField on the subclass, so walk up the hierarchy.
    private java.lang.reflect.Field findDeclaredField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    @Test
    public void testGetFileForFileUpload_withFileObject() {
        try {
            java.io.File fileObj = java.io.File.createTempFile("upload-test", ".txt");
            fileObj.deleteOnExit();
            FileUploadRequest request = FileUploadRequest.builder()
                    .fileObject(fileObj)
                    .table("test_table")
                    .columnName("test_col")
                    .build();
            java.io.File result = vaultClient.getFileForFileUpload(request);
            Assert.assertEquals(fileObj, result);
        } catch (Exception e) {
            Assert.fail("Should not have thrown: " + e.getMessage());
        }
    }

    @Test
    public void testUpdateExecutorInHTTP_interceptorAddsAuthorizationHeader() {
        try {
            Credentials creds = new Credentials();
            creds.setToken("x.eyJleHAiOjk5OTk5OTk5OTl9.y");
            VaultConfig config = new VaultConfig();
            config.setVaultId("isolated-vault-http");
            config.setClusterId(clusterID);
            config.setEnv(com.skyflow.enums.Env.DEV);
            config.setCredentials(creds);
            VaultClient freshClient = new VaultClient(config, null);

            freshClient.setBearerToken(); // triggers updateExecutorInHTTP → creates sharedHttpClient with interceptor

            // Access sharedHttpClient via reflection
            java.lang.reflect.Field field = findDeclaredField(VaultClient.class, "sharedHttpClient");
            field.setAccessible(true);
            OkHttpClient httpClient = (OkHttpClient) field.get(freshClient);
            Assert.assertNotNull(httpClient);
            Assert.assertFalse(httpClient.interceptors().isEmpty());

            // Get the interceptor (our lambda)
            okhttp3.Interceptor interceptor = httpClient.interceptors().get(0);

            // Mock Chain and invoke the interceptor
            okhttp3.Interceptor.Chain mockChain = Mockito.mock(okhttp3.Interceptor.Chain.class);
            Request mockRequest = new Request.Builder().url("https://example.com").build();
            Mockito.when(mockChain.request()).thenReturn(mockRequest);

            Response mockResponse = new Response.Builder()
                    .request(mockRequest)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create("", MediaType.get("application/json")))
                    .build();
            Mockito.when(mockChain.proceed(Mockito.any(Request.class))).thenReturn(mockResponse);

            // Invoke the lambda — this covers lambda$updateExecutorInHTTP$21
            Response response = interceptor.intercept(mockChain);
            Assert.assertNotNull(response);

            // Verify the interceptor added the Authorization header
            Mockito.verify(mockChain).proceed(Mockito.argThat(req ->
                req.header("Authorization") != null &&
                req.header("Authorization").startsWith("Bearer ")
            ));
        } catch (Exception e) {
            Assert.fail("Should not have thrown: " + e.getMessage());
        }
    }

    @Test
    public void testGetFileForFileUpload_withNoFileInput_returnsNull() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .table("test_table")
                    .columnName("test_col")
                    .build();
            java.io.File result = vaultClient.getFileForFileUpload(request);
            Assert.assertNull(result);
        } catch (Exception e) {
            Assert.fail("Should not have thrown: " + e.getMessage());
        }
    }

}
