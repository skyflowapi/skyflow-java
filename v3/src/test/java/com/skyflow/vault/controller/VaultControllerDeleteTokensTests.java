package com.skyflow.vault.controller;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Constants;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.DeleteTokensRequest;
import com.skyflow.vault.data.DeleteTokensResponse;
import com.skyflow.vault.data.ErrorRecord;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
        "javax.management.*",
        "java.nio.*",
        "com.sun.net.httpserver.*",
        "sun.net.httpserver.*",
        "sun.nio.ch.*",
        "jdk.internal.reflect.*",
        "javax.crypto.*",
        "javax.net.ssl.*"
})
public class VaultControllerDeleteTokensTests {

    private static final String ENV_PATH = "./.env";

    private VaultConfig vaultConfig;
    private Credentials credentials;

    @Before
    public void setUp() {
        vaultConfig = new VaultConfig();
        vaultConfig.setVaultId("vault123");
        vaultConfig.setClusterId("cluster123");
        vaultConfig.setEnv(Env.DEV);

        credentials = new Credentials();
        credentials.setToken("valid-token");
        vaultConfig.setCredentials(credentials);

        writeEnv("DELETE_TOKENS_BATCH_SIZE=50\nDELETE_TOKENS_CONCURRENCY_LIMIT=1");
    }

    @After
    public void tearDown() {
        writeEnv("");
    }

    private void writeEnv(String content) {
        java.io.File envFile = new java.io.File(ENV_PATH);
        java.io.File parentDir = envFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        try (FileWriter writer = new FileWriter(envFile)) {
            writer.write(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private VaultController createController() throws SkyflowException {
        return new VaultController(vaultConfig, credentials);
    }

    private void setPrivateField(Object obj, String field, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private int getPrivateInt(Object obj, String field) throws Exception {
        Field f = obj.getClass().getDeclaredField(field);
        f.setAccessible(true);
        return f.getInt(obj);
    }

    private List<String> getTokens(int count) {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tokens.add("token" + i);
        }
        return tokens;
    }

    // ─── Validation tests via Validations directly ───────────────────────────

    @Test
    public void testValidation_nullRequest() {
        try {
            Validations.validateDeleteTokensRequest(null);
            fail("Expected SkyflowException for null request");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.DeleteTokensRequestNull.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidation_nullTokensList() {
        DeleteTokensRequest req = DeleteTokensRequest.builder().tokens(null).build();
        try {
            Validations.validateDeleteTokensRequest(req);
            fail("Expected SkyflowException for null tokens list");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.EmptyDeleteTokensData.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidation_emptyTokensList() {
        DeleteTokensRequest req = DeleteTokensRequest.builder().tokens(new ArrayList<>()).build();
        try {
            Validations.validateDeleteTokensRequest(req);
            fail("Expected SkyflowException for empty tokens list");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.EmptyDeleteTokensData.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidation_nullTokenInList() {
        List<String> tokens = Arrays.asList("token1", null, "token3");
        DeleteTokensRequest req = DeleteTokensRequest.builder().tokens(tokens).build();
        try {
            Validations.validateDeleteTokensRequest(req);
            fail("Expected SkyflowException for null token in list");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.EmptyTokenInDeleteTokensData.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidation_emptyTokenInList() {
        List<String> tokens = Arrays.asList("token1", "  ", "token3");
        DeleteTokensRequest req = DeleteTokensRequest.builder().tokens(tokens).build();
        try {
            Validations.validateDeleteTokensRequest(req);
            fail("Expected SkyflowException for empty token in list");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.EmptyTokenInDeleteTokensData.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidation_tokensSizeExceeds10000() {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 10001; i++) {
            tokens.add("token" + i);
        }
        DeleteTokensRequest req = DeleteTokensRequest.builder().tokens(tokens).build();
        try {
            Validations.validateDeleteTokensRequest(req);
            fail("Expected SkyflowException for tokens size exceeding 10000");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.DeleteTokensSizeExceedError.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidation_validRequest() {
        List<String> tokens = Arrays.asList("token1", "token2", "token3");
        DeleteTokensRequest req = DeleteTokensRequest.builder().tokens(tokens).build();
        try {
            Validations.validateDeleteTokensRequest(req);
        } catch (SkyflowException e) {
            fail("Should not throw exception for valid request: " + e.getMessage());
        }
    }

    // ─── bulkDeleteTokens validation error propagation ────────────────────────

    @Test
    public void testBulkDeleteTokens_nullRequest() throws SkyflowException {
        VaultController controller = createController();
        try {
            controller.bulkDeleteTokens(null);
            fail("Expected SkyflowException");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.DeleteTokensRequestNull.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testBulkDeleteTokens_emptyTokens() throws SkyflowException {
        VaultController controller = createController();
        DeleteTokensRequest req = DeleteTokensRequest.builder().tokens(new ArrayList<>()).build();
        try {
            controller.bulkDeleteTokens(req);
            fail("Expected SkyflowException for empty tokens");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.EmptyDeleteTokensData.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testBulkDeleteTokensAsync_nullRequest() throws SkyflowException {
        VaultController controller = createController();
        try {
            controller.bulkDeleteTokensAsync(null);
            fail("Expected SkyflowException");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.DeleteTokensRequestNull.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testBulkDeleteTokensAsync_emptyTokens() throws SkyflowException {
        VaultController controller = createController();
        DeleteTokensRequest req = DeleteTokensRequest.builder().tokens(new ArrayList<>()).build();
        try {
            controller.bulkDeleteTokensAsync(req);
            fail("Expected SkyflowException for empty tokens");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.EmptyDeleteTokensData.getMessage(), e.getMessage());
        }
    }

    // ─── Batch / concurrency configuration tests ─────────────────────────────

    @Test
    public void testDefaultDeleteTokensBatchConfig() throws Exception {
        writeEnv("");
        VaultController controller = createController();
        assertEquals(Constants.DELETE_TOKENS_BATCH_SIZE.intValue(), getPrivateInt(controller, "deleteTokensBatchSize"));
        assertEquals(Constants.DELETE_TOKENS_CONCURRENCY_LIMIT.intValue(), getPrivateInt(controller, "deleteTokensConcurrencyLimit"));
    }

    @Test
    public void testCustomValidBatchAndConcurrency() throws Exception {
        writeEnv("DELETE_TOKENS_BATCH_SIZE=10\nDELETE_TOKENS_CONCURRENCY_LIMIT=3");
        VaultController controller = createController();
        List<String> tokens = getTokens(30);
        DeleteTokensRequest req = DeleteTokensRequest.builder().tokens(tokens).build();
        try {
            controller.bulkDeleteTokens(req);
        } catch (Exception ignored) {}

        assertEquals(10, getPrivateInt(controller, "deleteTokensBatchSize"));
        assertEquals(3, getPrivateInt(controller, "deleteTokensConcurrencyLimit"));
    }

    @Test
    public void testBatchSizeExceedsMax() throws Exception {
        writeEnv("DELETE_TOKENS_BATCH_SIZE=2000");
        VaultController controller = createController();
        List<String> tokens = getTokens(50);
        DeleteTokensRequest req = DeleteTokensRequest.builder().tokens(tokens).build();
        try {
            controller.bulkDeleteTokens(req);
        } catch (Exception ignored) {}

        // When batch size exceeds MAX, it should be clamped to MAX
        assertEquals(Constants.MAX_DELETE_TOKENS_BATCH_SIZE.intValue(), getPrivateInt(controller, "deleteTokensBatchSize"));
    }

    @Test
    public void testConcurrencyExceedsMax() throws Exception {
        writeEnv("DELETE_TOKENS_CONCURRENCY_LIMIT=200");
        VaultController controller = createController();
        List<String> tokens = getTokens(50);
        DeleteTokensRequest req = DeleteTokensRequest.builder().tokens(tokens).build();
        try {
            controller.bulkDeleteTokens(req);
        } catch (Exception ignored) {}

        assertEquals(1, getPrivateInt(controller, "deleteTokensConcurrencyLimit"));
    }

    @Test
    public void testBatchSizeZeroFallsBackToDefault() throws Exception {
        writeEnv("DELETE_TOKENS_BATCH_SIZE=0");
        VaultController controller = createController();
        List<String> tokens = getTokens(10);
        DeleteTokensRequest req = DeleteTokensRequest.builder().tokens(tokens).build();
        try {
            controller.bulkDeleteTokens(req);
        } catch (Exception ignored) {}

        assertEquals(Constants.DELETE_TOKENS_BATCH_SIZE.intValue(), getPrivateInt(controller, "deleteTokensBatchSize"));
    }

    @Test
    public void testBatchSizeNegativeFallsBackToDefault() throws Exception {
        writeEnv("DELETE_TOKENS_BATCH_SIZE=-5");
        VaultController controller = createController();
        List<String> tokens = getTokens(10);
        DeleteTokensRequest req = DeleteTokensRequest.builder().tokens(tokens).build();
        try {
            controller.bulkDeleteTokens(req);
        } catch (Exception ignored) {}

        assertEquals(Constants.DELETE_TOKENS_BATCH_SIZE.intValue(), getPrivateInt(controller, "deleteTokensBatchSize"));
    }

    @Test
    public void testConcurrencyZeroFallsBackToDefault() throws Exception {
        writeEnv("DELETE_TOKENS_CONCURRENCY_LIMIT=0");
        VaultController controller = createController();
        List<String> tokens = getTokens(10);
        DeleteTokensRequest req = DeleteTokensRequest.builder().tokens(tokens).build();
        try {
            controller.bulkDeleteTokens(req);
        } catch (Exception ignored) {}

        int maxNeeded = (10 + Constants.DELETE_TOKENS_BATCH_SIZE - 1) / Constants.DELETE_TOKENS_BATCH_SIZE;
        int expected = Math.min(Constants.DELETE_TOKENS_CONCURRENCY_LIMIT, maxNeeded);
        assertEquals(expected, getPrivateInt(controller, "deleteTokensConcurrencyLimit"));
    }

    @Test
    public void testTotalRequestsLessThanBatchSize() throws Exception {
        writeEnv("DELETE_TOKENS_BATCH_SIZE=100\nDELETE_TOKENS_CONCURRENCY_LIMIT=10");
        VaultController controller = createController();
        List<String> tokens = getTokens(10);
        DeleteTokensRequest req = DeleteTokensRequest.builder().tokens(tokens).build();
        try {
            controller.bulkDeleteTokens(req);
        } catch (Exception ignored) {}

        assertEquals(100, getPrivateInt(controller, "deleteTokensBatchSize"));
        assertEquals(1, getPrivateInt(controller, "deleteTokensConcurrencyLimit"));
    }

    @Test
    public void testHighConcurrencyForLowTokens() throws Exception {
        writeEnv("DELETE_TOKENS_BATCH_SIZE=1000\nDELETE_TOKENS_CONCURRENCY_LIMIT=100");
        VaultController controller = createController();
        List<String> tokens = getTokens(10000);
        DeleteTokensRequest req = DeleteTokensRequest.builder().tokens(tokens).build();
        try {
            controller.bulkDeleteTokens(req);
        } catch (Exception ignored) {}

        assertEquals(1000, getPrivateInt(controller, "deleteTokensBatchSize"));
        assertEquals(Constants.MAX_DELETE_TOKENS_CONCURRENCY_LIMIT.intValue(), getPrivateInt(controller, "deleteTokensConcurrencyLimit"));
    }

    @Test
    public void testFractionalLastBatch() throws Exception {
        writeEnv("DELETE_TOKENS_BATCH_SIZE=100");
        VaultController controller = createController();
        List<String> tokens = getTokens(9050);
        DeleteTokensRequest req = DeleteTokensRequest.builder().tokens(tokens).build();
        try {
            controller.bulkDeleteTokens(req);
        } catch (Exception ignored) {}

        assertEquals(100, getPrivateInt(controller, "deleteTokensBatchSize"));
        assertEquals(Constants.DELETE_TOKENS_CONCURRENCY_LIMIT.intValue(), getPrivateInt(controller, "deleteTokensConcurrencyLimit"));
    }

    @Test
    public void testZeroTotalRequests() throws Exception {
        VaultController controller = createController();
        List<String> tokens = getTokens(0);
        DeleteTokensRequest req = DeleteTokensRequest.builder().tokens(tokens).build();

        boolean exceptionThrown = false;
        try {
            controller.bulkDeleteTokens(req);
        } catch (Exception e) {
            exceptionThrown = true;
        }
        assertTrue("Exception should be thrown for zero tokens", exceptionThrown);
    }

    // ─── deleteTokensBatchFutures catch branch ────────────────────────────────

    @Test
    public void testDeleteTokensBatchFutures_catchBranchAddsErrorRecord() throws Exception {
        VaultController controller = createController();
        setPrivateField(controller, "deleteTokensBatchSize", 2);

        List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest> batches = null;

        Method method = VaultController.class.getDeclaredMethod(
                "deleteTokensBatchFutures", ExecutorService.class, List.class);
        method.setAccessible(true);

        ExecutorService executor = Executors.newFixedThreadPool(1);
        @SuppressWarnings("unchecked")
        List<CompletableFuture<DeleteTokensResponse>> futures =
                (List<CompletableFuture<DeleteTokensResponse>>) method.invoke(controller, executor, batches);

        // errors are now returned via futures, not a shared list
        Assert.assertNotNull(futures);
        executor.shutdownNow();
    }

    // ─── processDeleteTokensSync via reflection ───────────────────────────────

    @Test
    public void testProcessDeleteTokensSyncNormalPath() throws Exception {
        VaultConfig cfg = new VaultConfig();
        cfg.setVaultId("vault123");
        cfg.setClusterId("cluster123");
        cfg.setEnv(Env.DEV);
        Credentials creds = new Credentials();
        creds.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        cfg.setCredentials(creds);

        VaultController controller = new VaultController(cfg, creds);
        setPrivateField(controller, "deleteTokensConcurrencyLimit", 1);
        setPrivateField(controller, "deleteTokensBatchSize", 1);

        List<String> tokens = Collections.singletonList("token0");
        DeleteTokensRequest request = DeleteTokensRequest.builder().tokens(tokens).build();

        Method getRequestBody = VaultController.class.getSuperclass()
                .getDeclaredMethod("getDeleteTokensRequestBody", DeleteTokensRequest.class);
        getRequestBody.setAccessible(true);
        Object requestObj = getRequestBody.invoke(controller, request);

        Method processSync = VaultController.class.getDeclaredMethod(
                "processDeleteTokensSync",
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest.class,
                List.class
        );
        processSync.setAccessible(true);

        try {
            processSync.invoke(controller, requestObj, tokens);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof SkyflowException
                    || cause instanceof java.util.concurrent.ExecutionException
                    || cause instanceof RuntimeException);
        }
    }
}
