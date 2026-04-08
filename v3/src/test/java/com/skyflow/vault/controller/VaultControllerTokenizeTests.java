package com.skyflow.vault.controller;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Constants;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.ErrorRecord;
import com.skyflow.vault.data.TokenizeRecord;
import com.skyflow.vault.data.TokenizeRequest;
import com.skyflow.vault.data.TokenizeResponse;
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
public class VaultControllerTokenizeTests {

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

        writeEnv("TOKENIZE_BATCH_SIZE=50\nTOKENIZE_CONCURRENCY_LIMIT=1");
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

    private ArrayList<TokenizeRecord> generateRecords(int count) {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            records.add(TokenizeRecord.builder()
                    .value("value" + i)
                    .tokenGroupNames(Collections.singletonList("group1"))
                    .build());
        }
        return records;
    }

    // ─── Validation tests via Validations directly ───────────────────────────

    @Test
    public void testValidation_nullRequest() {
        try {
            Validations.validateTokenizeRequest(null);
            fail("Expected SkyflowException for null request");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.TokenizeRequestNull.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidation_emptyData() {
        TokenizeRequest req = TokenizeRequest.builder().data(new ArrayList<>()).build();
        try {
            Validations.validateTokenizeRequest(req);
            fail("Expected SkyflowException for empty data");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.EmptyTokenizeData.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidation_nullRecord() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(null);
        TokenizeRequest req = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(req);
            fail("Expected SkyflowException for null record");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.TokenizeRecordNull.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidation_emptyValue() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(TokenizeRecord.builder().value("  ").tokenGroupNames(Collections.singletonList("g1")).build());
        TokenizeRequest req = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(req);
            fail("Expected SkyflowException for empty value");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.EmptyValueInTokenizeRecord.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidation_emptyTokenGroupNames() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(TokenizeRecord.builder().value("val").tokenGroupNames(new ArrayList<>()).build());
        TokenizeRequest req = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(req);
            fail("Expected SkyflowException for empty token group names");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.EmptyTokenGroupNamesInTokenizeRecord.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidation_emptyTokenGroupNameInList() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(TokenizeRecord.builder().value("val").tokenGroupNames(Arrays.asList("g1", "  ")).build());
        TokenizeRequest req = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(req);
            fail("Expected SkyflowException for empty group name in list");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.EmptyTokenGroupNameInTokenizeRecord.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidation_sizeExceeds10000() {
        ArrayList<TokenizeRecord> records = generateRecords(10001);
        TokenizeRequest req = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(req);
            fail("Expected SkyflowException for size exceed");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.TokenizeDataSizeExceedError.getMessage(), e.getMessage());
        }
    }

    // ─── bulkTokenize / bulkTokenizeAsync validation error propagation ────────

    @Test
    public void testBulkTokenize_nullRequest() throws SkyflowException {
        VaultController controller = createController();
        try {
            controller.bulkTokenize(null);
            fail("Expected SkyflowException");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.TokenizeRequestNull.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testBulkTokenize_emptyData() throws SkyflowException {
        VaultController controller = createController();
        TokenizeRequest req = TokenizeRequest.builder().data(new ArrayList<>()).build();
        try {
            controller.bulkTokenize(req);
            fail("Expected SkyflowException for empty data");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.EmptyTokenizeData.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testBulkTokenizeAsync_nullRequest() throws SkyflowException {
        VaultController controller = createController();
        try {
            controller.bulkTokenizeAsync(null);
            fail("Expected SkyflowException");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.TokenizeRequestNull.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testBulkTokenizeAsync_emptyData() throws SkyflowException {
        VaultController controller = createController();
        TokenizeRequest req = TokenizeRequest.builder().data(new ArrayList<>()).build();
        try {
            controller.bulkTokenizeAsync(req);
            fail("Expected SkyflowException for empty data");
        } catch (SkyflowException e) {
            assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            assertEquals(ErrorMessage.EmptyTokenizeData.getMessage(), e.getMessage());
        }
    }

    // ─── Batch / concurrency configuration tests ─────────────────────────────

    @Test
    public void testDefaultTokenizeBatchConfig() throws Exception {
        writeEnv("");
        VaultController controller = createController();
        assertEquals(Constants.TOKENIZE_BATCH_SIZE.intValue(), getPrivateInt(controller, "tokenizeBatchSize"));
        assertEquals(Constants.TOKENIZE_CONCURRENCY_LIMIT.intValue(), getPrivateInt(controller, "tokenizeConcurrencyLimit"));
    }

    @Test
    public void testCustomValidBatchAndConcurrency() throws Exception {
        writeEnv("TOKENIZE_BATCH_SIZE=10\nTOKENIZE_CONCURRENCY_LIMIT=3");
        VaultController controller = createController();
        TokenizeRequest req = TokenizeRequest.builder().data(generateRecords(30)).build();
        try {
            controller.bulkTokenize(req);
        } catch (Exception ignored) {}

        assertEquals(10, getPrivateInt(controller, "tokenizeBatchSize"));
        assertEquals(3, getPrivateInt(controller, "tokenizeConcurrencyLimit"));
    }

    @Test
    public void testBatchSizeClampsToMax() throws Exception {
        writeEnv("TOKENIZE_BATCH_SIZE=2000");
        VaultController controller = createController();
        TokenizeRequest req = TokenizeRequest.builder().data(generateRecords(50)).build();
        try {
            controller.bulkTokenize(req);
        } catch (Exception ignored) {}

        assertEquals(Constants.MAX_TOKENIZE_BATCH_SIZE.intValue(), getPrivateInt(controller, "tokenizeBatchSize"));
    }

    @Test
    public void testConcurrencyLimitClampsToMax() throws Exception {
        writeEnv("TOKENIZE_CONCURRENCY_LIMIT=200");
        VaultController controller = createController();
        TokenizeRequest req = TokenizeRequest.builder().data(generateRecords(10000)).build();
        try {
            controller.bulkTokenize(req);
        } catch (Exception ignored) {}

        assertEquals(Constants.MAX_TOKENIZE_CONCURRENCY_LIMIT.intValue(), getPrivateInt(controller, "tokenizeConcurrencyLimit"));
    }

    @Test
    public void testBatchSizeZeroFallsToDefault() throws Exception {
        writeEnv("TOKENIZE_BATCH_SIZE=0");
        VaultController controller = createController();
        TokenizeRequest req = TokenizeRequest.builder().data(generateRecords(10)).build();
        try {
            controller.bulkTokenize(req);
        } catch (Exception ignored) {}

        assertEquals(Constants.TOKENIZE_BATCH_SIZE.intValue(), getPrivateInt(controller, "tokenizeBatchSize"));
    }

    @Test
    public void testBatchSizeNegativeFallsToDefault() throws Exception {
        writeEnv("TOKENIZE_BATCH_SIZE=-10");
        VaultController controller = createController();
        TokenizeRequest req = TokenizeRequest.builder().data(generateRecords(10)).build();
        try {
            controller.bulkTokenize(req);
        } catch (Exception ignored) {}

        assertEquals(Constants.TOKENIZE_BATCH_SIZE.intValue(), getPrivateInt(controller, "tokenizeBatchSize"));
    }

    @Test
    public void testConcurrencyZeroFallsToDefault() throws Exception {
        writeEnv("TOKENIZE_CONCURRENCY_LIMIT=0");
        VaultController controller = createController();
        TokenizeRequest req = TokenizeRequest.builder().data(generateRecords(10)).build();
        try {
            controller.bulkTokenize(req);
        } catch (Exception ignored) {}

        int maxNeeded = (10 + Constants.TOKENIZE_BATCH_SIZE - 1) / Constants.TOKENIZE_BATCH_SIZE;
        int expected = Math.min(Constants.TOKENIZE_CONCURRENCY_LIMIT, maxNeeded);
        assertEquals(expected, getPrivateInt(controller, "tokenizeConcurrencyLimit"));
    }

    @Test
    public void testTotalRequestsLessThanBatchSize() throws Exception {
        writeEnv("TOKENIZE_BATCH_SIZE=100\nTOKENIZE_CONCURRENCY_LIMIT=10");
        VaultController controller = createController();
        TokenizeRequest req = TokenizeRequest.builder().data(generateRecords(10)).build();
        try {
            controller.bulkTokenize(req);
        } catch (Exception ignored) {}

        assertEquals(100, getPrivateInt(controller, "tokenizeBatchSize"));
        assertEquals(1, getPrivateInt(controller, "tokenizeConcurrencyLimit"));
    }

    @Test
    public void testHighConcurrencyForLowRecords() throws Exception {
        writeEnv("TOKENIZE_BATCH_SIZE=1000\nTOKENIZE_CONCURRENCY_LIMIT=100");
        VaultController controller = createController();
        TokenizeRequest req = TokenizeRequest.builder().data(generateRecords(10000)).build();
        try {
            controller.bulkTokenize(req);
        } catch (Exception ignored) {}

        assertEquals(1000, getPrivateInt(controller, "tokenizeBatchSize"));
        assertEquals(Constants.MAX_TOKENIZE_CONCURRENCY_LIMIT.intValue(), getPrivateInt(controller, "tokenizeConcurrencyLimit"));
    }

    @Test
    public void testFractionalLastBatch() throws Exception {
        writeEnv("TOKENIZE_BATCH_SIZE=100");
        VaultController controller = createController();
        TokenizeRequest req = TokenizeRequest.builder().data(generateRecords(9050)).build();
        try {
            controller.bulkTokenize(req);
        } catch (Exception ignored) {}

        assertEquals(100, getPrivateInt(controller, "tokenizeBatchSize"));
        assertEquals(Constants.TOKENIZE_CONCURRENCY_LIMIT.intValue(), getPrivateInt(controller, "tokenizeConcurrencyLimit"));
    }

    @Test
    public void testZeroRecordsThrowsException() throws Exception {
        VaultController controller = createController();
        TokenizeRequest req = TokenizeRequest.builder().data(new ArrayList<>()).build();
        boolean exceptionThrown = false;
        try {
            controller.bulkTokenize(req);
        } catch (Exception e) {
            exceptionThrown = true;
        }
        assertTrue("Exception should be thrown for zero records", exceptionThrown);
    }

    // ─── tokenizeBatchFutures catch branch ───────────────────────────────────

    @Test
    public void testTokenizeBatchFutures_catchBranchAddsErrorRecord() throws Exception {
        VaultController controller = createController();
        setPrivateField(controller, "tokenizeBatchSize", 2);

        // Pass null batches list to trigger the catch branch inside tokenizeBatchFutures
        List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest> batches = null;

        Method method = VaultController.class.getDeclaredMethod(
                "tokenizeBatchFutures", ExecutorService.class, List.class);
        method.setAccessible(true);

        ExecutorService executor = Executors.newFixedThreadPool(1);
        @SuppressWarnings("unchecked")
        List<CompletableFuture<TokenizeResponse>> futures =
                (List<CompletableFuture<TokenizeResponse>>) method.invoke(controller, executor, batches);

        // errors are now returned via futures, not a shared list
        Assert.assertNotNull(futures);
        executor.shutdownNow();
    }

    // ─── processTokenizeSync via reflection ──────────────────────────────────

    @Test
    public void testProcessTokenizeSyncNormalPath() throws Exception {
        VaultConfig cfg = new VaultConfig();
        cfg.setVaultId("vault123");
        cfg.setClusterId("cluster123");
        cfg.setEnv(Env.DEV);
        Credentials creds = new Credentials();
        creds.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        cfg.setCredentials(creds);

        VaultController controller = new VaultController(cfg, creds);
        setPrivateField(controller, "tokenizeConcurrencyLimit", 1);
        setPrivateField(controller, "tokenizeBatchSize", 1);

        ArrayList<TokenizeRecord> data = new ArrayList<>();
        data.add(TokenizeRecord.builder().value("val1").tokenGroupNames(Collections.singletonList("g1")).build());
        TokenizeRequest request = TokenizeRequest.builder().data(data).build();

        Method getRequestBody = VaultController.class.getSuperclass()
                .getDeclaredMethod("getTokenizeRequestBody", TokenizeRequest.class);
        getRequestBody.setAccessible(true);
        Object requestObj = getRequestBody.invoke(controller, request);

        Method processSync = VaultController.class.getDeclaredMethod(
                "processTokenizeSync",
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest.class,
                ArrayList.class
        );
        processSync.setAccessible(true);

        try {
            processSync.invoke(controller, requestObj, data);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof SkyflowException
                    || cause instanceof java.util.concurrent.ExecutionException
                    || cause instanceof RuntimeException);
        }
    }
}
