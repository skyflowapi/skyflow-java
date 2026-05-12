package com.skyflow.vault.controller;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.enums.Env;
import com.skyflow.utils.Constants;
import com.skyflow.utils.validations.Validations;
import com.skyflow.enums.CustomHeaderKey;
import com.skyflow.vault.data.DeleteTokensOptions;
import com.skyflow.vault.data.DeleteTokensRequest;
import com.skyflow.vault.data.DeleteTokensResponse;
import com.skyflow.vault.data.DetokenizeOptions;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.DetokenizeResponse;
import com.skyflow.vault.data.InsertOptions;
import com.skyflow.vault.data.InsertRecord;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.ErrorRecord;
import com.skyflow.vault.data.TokenizeOptions;
import com.skyflow.vault.data.TokenizeRecord;
import com.skyflow.vault.data.TokenizeRequest;
import com.skyflow.vault.data.TokenizeResponse;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore; // Import this
import org.powermock.modules.junit4.PowerMockRunner;

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
public class VaultControllerTests {
    private static final String ENV_PATH = "./.env";

    private VaultConfig vaultConfig;
    private Credentials credentials;

    @Before
    public void setUp() {
        vaultConfig = new VaultConfig();
        vaultConfig.setVaultId("vault123");
        vaultConfig.setClusterId("cluster123");
        vaultConfig.setEnv(com.skyflow.enums.Env.DEV);

        credentials = new Credentials();
        credentials.setToken("valid-token");
        vaultConfig.setCredentials(credentials);

        writeEnv("INSERT_BATCH_SIZE=50\nINSERT_CONCURRENCY_LIMIT=10");
    }

    @After
    public void tearDown() {
        // Optionally clean up .env file
        writeEnv(""); // or restore to default
    }

    private void writeEnv(String content) {
        java.io.File envFile = new java.io.File(ENV_PATH);
        java.io.File parentDir = envFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs(); // Create parent directory if it doesn't exist
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

    // Helper to set private fields via reflection
    private void setPrivateField(Object obj, String field, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private void invokeConfigureInsertConcurrencyAndBatchSize(VaultController controller, int totalRequests) throws Exception {
        Method method = VaultController.class.getDeclaredMethod("configureInsertConcurrencyAndBatchSize", int.class);
        method.setAccessible(true);
        method.invoke(controller, totalRequests);
    }
    private void invokeConfigureDetokenizeConcurrencyAndBatchSize(VaultController controller, int totalRequests) throws Exception {
        Method method = VaultController.class.getDeclaredMethod("configureDetokenizeConcurrencyAndBatchSize", int.class);
        method.setAccessible(true);
        method.invoke(controller, totalRequests);
    }

    private ArrayList<InsertRecord> generateValues(int noOfRecords) {
        ArrayList<InsertRecord> values = new ArrayList<>();
        for (int i = 0; i < noOfRecords; i++) {
            values.add(InsertRecord.builder().data(new HashMap<>()).build());
        }

        return values;
    }

    @Test
    public void testValidation_tableIsNull() {
        InsertRequest req = InsertRequest.builder().table(null).records(generateValues(1)).build();
        try {
            Validations.validateInsertRequest(req);
            fail("Expected SkyflowException for null table");
        } catch (SkyflowException e) {
            assertTrue(!e.getMessage().isEmpty());
        }
    }

    @Test
    public void testValidation_tableIsEmpty() {
        InsertRequest req = InsertRequest.builder().table("   ").records(generateValues(1)).build();
        try {
            Validations.validateInsertRequest(req);
            fail("Expected SkyflowException for empty table");
        } catch (SkyflowException e) {
            assertTrue(!e.getMessage().isEmpty());
        }
    }

    @Test
    public void testValidation_valuesIsNull() {
        InsertRequest req = InsertRequest.builder().table("table1").records(null).build();
        try {
            Validations.validateInsertRequest(req);
            fail("Expected SkyflowException for null values");
        } catch (SkyflowException e) {
            assertTrue(!e.getMessage().isEmpty());
        }
    }

    @Test
    public void testValidation_valuesIsEmpty() {
        InsertRequest req = InsertRequest.builder().table("table1").records(new ArrayList<>()).build();
        try {
            Validations.validateInsertRequest(req);
            fail("Expected SkyflowException for empty values");
        } catch (SkyflowException e) {
            assertFalse(e.getMessage().isEmpty());
        }
    }

    @Test
    public void testValidation_upsertIsEmpty() throws SkyflowException {
        try {
            InsertRequest req = InsertRequest.builder()
                    .table("table1")
                    .records(generateValues(1))
                    .upsert(new ArrayList<>())
                    .build();
            Validations.validateInsertRequest(req);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyUpsertValues.getMessage(), e.getMessage());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }


    @Test
    public void testValidation_keyIsNullOrEmpty() {
        ArrayList<InsertRecord> values = new ArrayList<>();
        HashMap<String, Object> map = new HashMap<>();
        map.put(null, "value");
        values.add(InsertRecord.builder().data(map).build());
        InsertRequest req = InsertRequest.builder().table("table1").records(values).build();
        try {
            Validations.validateInsertRequest(req);
            fail("Expected SkyflowException for null key in values");
        } catch (SkyflowException e) {
            assertFalse(e.getMessage().isEmpty());
        }

        // Test empty key
        values.clear();
        map = new HashMap<>();
        map.put("   ", "value");
        values.add(InsertRecord.builder().data(map).build());
        req = InsertRequest.builder().table("table1").records(values).build();
        try {
            Validations.validateInsertRequest(req);
            fail("Expected SkyflowException for empty key in values");
        } catch (SkyflowException e) {
            assertFalse(e.getMessage().isEmpty());
        }
    }

    @Test
    public void testValidation_valueIsNullOrEmpty() {
        ArrayList<InsertRecord> values = new ArrayList<>();
        HashMap<String, Object> map = new HashMap<>();
        map.put("field1", null);
        values.add(InsertRecord.builder().data(map).build());
        InsertRequest req = InsertRequest.builder().table("table1").records(values).build();
        try {
            Validations.validateInsertRequest(req);
            fail("Expected SkyflowException for null value in values");
        } catch (SkyflowException e) {
            assertFalse(e.getMessage().isEmpty());
        }

        // Test empty value
        values.clear();
        map = new HashMap<>();
        map.put("field1", "   ");
        values.add(InsertRecord.builder().data(map).build());
        req = InsertRequest.builder().table("table1").records(values).build();
        try {
            Validations.validateInsertRequest(req);
            fail("Expected SkyflowException for empty value in values");
        } catch (SkyflowException e) {
            assertFalse(e.getMessage().isEmpty());
        }
    }

    @Test
    public void testDefaultValues() throws Exception {
        VaultController controller = createController();
        setPrivateField(controller, "insertBatchSize", Constants.INSERT_BATCH_SIZE);
        setPrivateField(controller, "insertConcurrencyLimit", Constants.INSERT_CONCURRENCY_LIMIT);

        invokeConfigureInsertConcurrencyAndBatchSize(controller, 10);
        assertEquals(Constants.INSERT_BATCH_SIZE.intValue(), getPrivateInt(controller, "insertBatchSize"));
        assertEquals(Math.min(Constants.INSERT_CONCURRENCY_LIMIT, (10 + Constants.INSERT_BATCH_SIZE - 1) / Constants.INSERT_BATCH_SIZE),
                getPrivateInt(controller, "insertConcurrencyLimit"));
    }
    @Test
    public void testDefaultValuesForDetokenize() throws Exception {
        VaultController controller = createController();
        setPrivateField(controller, "detokenizeBatchSize", Constants.DETOKENIZE_BATCH_SIZE);
        setPrivateField(controller, "detokenizeConcurrencyLimit", Constants.DETOKENIZE_CONCURRENCY_LIMIT);

        invokeConfigureDetokenizeConcurrencyAndBatchSize(controller, 10);
        assertEquals(Constants.DETOKENIZE_BATCH_SIZE.intValue(), getPrivateInt(controller, "detokenizeBatchSize"));
        assertEquals(Math.min(Constants.DETOKENIZE_CONCURRENCY_LIMIT, (10 + Constants.DETOKENIZE_BATCH_SIZE - 1) / Constants.DETOKENIZE_BATCH_SIZE),
                getPrivateInt(controller, "detokenizeConcurrencyLimit"));
    }

    @Test
    public void testCustomValidBatchAndConcurrency() throws Exception {
        writeEnv("INSERT_BATCH_SIZE=5\nINSERT_CONCURRENCY_LIMIT=3");
        VaultController controller = createController();

        InsertRequest insertRequest = InsertRequest.builder().table("table1").records(generateValues(20)).build();

        try {
            controller.bulkInsert(insertRequest);
        } catch (Exception ignored) {
            //  Ignore, Testing concurrency/batch config
        }

        assertEquals(5, getPrivateInt(controller, "insertBatchSize"));
        assertEquals(3, getPrivateInt(controller, "insertConcurrencyLimit"));
    }

    @Test
    public void testBatchSizeExceedsMax() throws Exception {
        writeEnv("INSERT_BATCH_SIZE=1100\nINSERT_CONCURRENCY_LIMIT=3");
        VaultController controller = createController();

        InsertRequest insertRequest = InsertRequest.builder().table("table1").records(generateValues(50)).build();

        try {
            controller.bulkInsert(insertRequest);
        } catch (Exception ignored) {
            //  Ignore, Testing concurrency/batch config
        }

        assertEquals(Constants.MAX_INSERT_BATCH_SIZE.intValue(), getPrivateInt(controller, "insertBatchSize"));
    }

    @Test
    public void testConcurrencyExceedsMax() throws Exception {
        writeEnv("INSERT_CONCURRENCY_LIMIT=110");
        VaultController controller = createController();
        InsertRequest insertRequest = InsertRequest.builder().table("table1").records(generateValues(50)).build();


        try {
            controller.bulkInsert(insertRequest);
        } catch (Exception ignored) {
            // Ignore, Testing concurrency/batch config
        }

        assertEquals(1, getPrivateInt(controller, "insertConcurrencyLimit"));
    }

    @Test
    public void testNonNumericInsertBatchSize_usesDefault() throws Exception {
        writeEnv("INSERT_BATCH_SIZE=abc");
        VaultController controller = createController();
        InsertRequest insertRequest = InsertRequest.builder().table("table1").records(generateValues(10)).build();

        try {
            controller.bulkInsert(insertRequest);
        } catch (Exception ignored) {
        }

        assertEquals(Constants.INSERT_BATCH_SIZE.intValue(), getPrivateInt(controller, "insertBatchSize"));
    }

    @Test
    public void testNonNumericInsertConcurrencyLimit_usesDefault() throws Exception {
        writeEnv("INSERT_CONCURRENCY_LIMIT=xyz");
        VaultController controller = createController();
        InsertRequest insertRequest = InsertRequest.builder().table("table1").records(generateValues(10)).build();

        try {
            controller.bulkInsert(insertRequest);
        } catch (Exception ignored) {
        }

        int expected = Math.min(Constants.INSERT_CONCURRENCY_LIMIT,
                (10 + Constants.INSERT_BATCH_SIZE - 1) / Constants.INSERT_BATCH_SIZE);
        assertEquals(expected, getPrivateInt(controller, "insertConcurrencyLimit"));
    }

    @Test
    public void testNonNumericDetokenizeBatchSize_usesDefault() throws Exception {
        writeEnv("DETOKENIZE_BATCH_SIZE=abc");
        VaultController controller = createController();
        List<String> tokens = getTokens(10);
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();

        try {
            controller.bulkDetokenize(request);
        } catch (Exception ignored) {
        }

        assertEquals(Constants.DETOKENIZE_BATCH_SIZE.intValue(), getPrivateInt(controller, "detokenizeBatchSize"));
    }

    @Test
    public void testNonNumericDetokenizeConcurrencyLimit_usesDefault() throws Exception {
        writeEnv("DETOKENIZE_CONCURRENCY_LIMIT=xyz");
        VaultController controller = createController();
        List<String> tokens = getTokens(10);
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();

        try {
            controller.bulkDetokenize(request);
        } catch (Exception ignored) {
        }

        int expected = Math.min(Constants.DETOKENIZE_CONCURRENCY_LIMIT,
                (10 + Constants.DETOKENIZE_BATCH_SIZE - 1) / Constants.DETOKENIZE_BATCH_SIZE);
        assertEquals(expected, getPrivateInt(controller, "detokenizeConcurrencyLimit"));
    }

    @Test
    public void testBatchSizeZeroOrNegative() throws Exception {
        writeEnv("INSERT_BATCH_SIZE=0");
        VaultController controller = createController();
        InsertRequest insertRequest = InsertRequest.builder().table("table1").records(generateValues(10)).build();

        try {
            controller.bulkInsert(insertRequest);
        } catch (Exception ignored) {
            // Ignore, Testing concurrency/batch config
        }

        assertEquals(Constants.INSERT_BATCH_SIZE.intValue(), getPrivateInt(controller, "insertBatchSize"));

        writeEnv("INSERT_BATCH_SIZE=-5");

        try {
            controller.bulkInsert(insertRequest);
        } catch (Exception ignored) {
            // Ignore, Testing concurrency/batch config
        }

        assertEquals(Constants.INSERT_BATCH_SIZE.intValue(), getPrivateInt(controller, "insertBatchSize"));
    }

    @Test
    public void testConcurrencyZeroOrNegative() throws Exception {
        writeEnv("INSERT_CONCURRENCY_LIMIT=0");
        VaultController controller = createController();
        InsertRequest insertRequest = InsertRequest.builder().table("table1").records(generateValues(10)).build();

        try {
            controller.bulkInsert(insertRequest);
        } catch (Exception ignored) {
            // Ignore, Testing concurrency/batch config
        }

        int min = Math.min(Constants.INSERT_CONCURRENCY_LIMIT, (10 + Constants.INSERT_BATCH_SIZE - 1) / Constants.INSERT_BATCH_SIZE);
        assertEquals(min, getPrivateInt(controller, "insertConcurrencyLimit"));


        writeEnv("INSERT_CONCURRENCY_LIMIT=-5");

        try {
            controller.bulkInsert(insertRequest);
        } catch (Exception ignored) {
            // Ignore, Testing concurrency/batch config
        }

        min = Math.min(Constants.INSERT_CONCURRENCY_LIMIT, (10 + Constants.INSERT_BATCH_SIZE - 1) / Constants.INSERT_BATCH_SIZE);
        assertEquals(min, getPrivateInt(controller, "insertConcurrencyLimit"));
    }


    @Test
    public void testTotalRequestsLessThanBatchSize() throws Exception {
        writeEnv("INSERT_BATCH_SIZE=100\nINSERT_CONCURRENCY_LIMIT=10");
        VaultController controller = createController();
        InsertRequest insertRequest = InsertRequest.builder().table("table1").records(generateValues(10)).build();


        try {
            controller.bulkInsert(insertRequest);
        } catch (Exception ignored) {
            // Ignore, Testing concurrency/batch config
        }

        assertEquals(100, getPrivateInt(controller, "insertBatchSize"));
        assertEquals(1, getPrivateInt(controller, "insertConcurrencyLimit"));
    }

    @Test
    public void testTotalRequestsZero() throws Exception {
        VaultController controller = createController();
        InsertRequest insertRequest = InsertRequest.builder().table("table1").records(generateValues(0)).build();

        boolean exceptionThrown = false;

        try {

            controller.bulkInsert(insertRequest);
        } catch (Exception e) {
            exceptionThrown = true;
        }
        assertTrue("Exception should be thrown for zero records", exceptionThrown);
    }


    @Test
    public void testHighConcurrencyForLowRecords() throws Exception {
        writeEnv("INSERT_BATCH_SIZE=1000\nINSERT_CONCURRENCY_LIMIT=100");
        VaultController controller = createController();
        InsertRequest insertRequest = InsertRequest.builder().table("table1").records(generateValues(10000)).build();

        try {
            controller.bulkInsert(insertRequest);
        } catch (Exception ignored) {
        }

        // Only 10 batches needed, so concurrency should be clamped to 10
        assertEquals(1000, getPrivateInt(controller, "insertBatchSize"));
        assertEquals(Constants.MAX_INSERT_CONCURRENCY_LIMIT.intValue(), getPrivateInt(controller, "insertConcurrencyLimit"));
    }


    @Test
    public void testFractionalLastBatch() throws Exception {
        writeEnv("INSERT_BATCH_SIZE=100");
        VaultController controller = createController();
        InsertRequest insertRequest = InsertRequest.builder().table("table1").records(generateValues(9050)).build();

        try {
            controller.bulkInsert(insertRequest);
        } catch (Exception ignored) {
        }

        // Last batch should have 50 records, concurrency should be 101
        assertEquals(100, getPrivateInt(controller, "insertBatchSize"));
        assertEquals(Constants.INSERT_CONCURRENCY_LIMIT.intValue(), getPrivateInt(controller, "insertConcurrencyLimit"));
    }

    @Test
    public void testCustomValidBatchAndConcurrencyDETOKENIZE() throws Exception {
        writeEnv("DETOKENIZE_BATCH_SIZE=5\nDETOKENIZE_CONCURRENCY_LIMIT=3");
        VaultController controller = createController();
        //20
        List<String> tokens = getTokens(20);
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();
        try {
            controller.bulkDetokenize(request);
        } catch (Exception ignored) {
            //  Ignore, Testing concurrency/batch config
        }

        assertEquals(5, getPrivateInt(controller, "detokenizeBatchSize"));
        assertEquals(3, getPrivateInt(controller, "detokenizeConcurrencyLimit"));
    }

    @Test
    public void testBatchSizeExceedsMaxDETOKENIZE() throws Exception {
        writeEnv("DETOKENIZE_BATCH_SIZE=1100\nDETOKENIZE_CONCURRENCY_LIMIT=3");
        VaultController controller = createController();
        //20
        List<String> tokens = getTokens(50);
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();
        try {
            controller.bulkDetokenize(request);
        } catch (Exception ignored) {
            //  Ignore, Testing concurrency/batch config
        }


        assertEquals(Constants.MAX_DETOKENIZE_BATCH_SIZE.intValue(), getPrivateInt(controller, "detokenizeBatchSize"));
    }

    @Test
    public void testConcurrencyExceedsMaxDETOKENIZE() throws Exception {
        writeEnv("DETOKENIZE_CONCURRENCY_LIMIT=110");
        VaultController controller = createController();
        //20
        List<String> tokens = getTokens(50);
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();
        try {
            controller.bulkDetokenize(request);
        } catch (Exception ignored) {
            //  Ignore, Testing concurrency/batch config
        }


        assertEquals(1, getPrivateInt(controller, "detokenizeConcurrencyLimit"));
    }

    @Test
    public void testBatchSizeZeroOrNegativeDETOKENIZE() throws Exception {
        writeEnv("DETOKENIZE_BATCH_SIZE=0");
        VaultController controller = createController();
        //20
        List<String> tokens = getTokens(10);
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();
        try {
            controller.bulkDetokenize(request);
        } catch (Exception ignored) {
            //  Ignore, Testing concurrency/batch config
        }

        assertEquals(Constants.DETOKENIZE_BATCH_SIZE.intValue(), getPrivateInt(controller, "detokenizeBatchSize"));

        writeEnv("DETOKENIZE_BATCH_SIZE=-5");

        try {
            controller.bulkDetokenize(request);
        } catch (Exception ignored) {
            // Ignore, Testing concurrency/batch config
        }

        assertEquals(Constants.DETOKENIZE_BATCH_SIZE.intValue(), getPrivateInt(controller, "detokenizeBatchSize"));
    }

    @Test
    public void testConcurrencyZeroOrNegativeDETOKENIZE() throws Exception {
        writeEnv("DETOKENIZE_CONCURRENCY_LIMIT=0");
        VaultController controller = createController();
        //20
        List<String> tokens = getTokens(10);
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();
        try {
            controller.bulkDetokenize(request);
        } catch (Exception ignored) {
            //  Ignore, Testing concurrency/batch config
        }

        int min = Math.min(Constants.DETOKENIZE_CONCURRENCY_LIMIT, (10 + Constants.DETOKENIZE_BATCH_SIZE - 1) / Constants.DETOKENIZE_BATCH_SIZE);
        assertEquals(min, getPrivateInt(controller, "detokenizeConcurrencyLimit"));


        writeEnv("DETOKENIZE_CONCURRENCY_LIMIT=-5");

        try {
            controller.bulkDetokenize(request);
        } catch (Exception ignored) {
            // Ignore, Testing concurrency/batch config
        }

        min = Math.min(Constants.DETOKENIZE_CONCURRENCY_LIMIT, (10 + Constants.DETOKENIZE_BATCH_SIZE - 1) / Constants.DETOKENIZE_BATCH_SIZE);
        assertEquals(min, getPrivateInt(controller, "detokenizeConcurrencyLimit"));
    }


    @Test
    public void testTotalRequestsLessThanBatchSizeDETOKENIZE() throws Exception {
        writeEnv("DETOKENIZE_BATCH_SIZE=100\nDETOKENIZE_CONCURRENCY_LIMIT=10");
        VaultController controller = createController();
        //20
        List<String> tokens = getTokens(10);
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();
        try {
            controller.bulkDetokenize(request);
        } catch (Exception ignored) {
            //  Ignore, Testing concurrency/batch config
        }


        assertEquals(100, getPrivateInt(controller, "detokenizeBatchSize"));
        assertEquals(1, getPrivateInt(controller, "detokenizeConcurrencyLimit"));
    }

    @Test
    public void testTotalRequestsZeroDETOKENIZE() throws Exception {
        VaultController controller = createController();
        //20
        List<String> tokens = getTokens(0);
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();

        boolean exceptionThrown = false;

        try {

            controller.bulkDetokenize(request);
        } catch (Exception e) {
            exceptionThrown = true;
        }
        assertTrue("Exception should be thrown for zero records", exceptionThrown);
    }


    @Test
    public void testHighConcurrencyForLowRecordsDETOKENIZE() throws Exception {
        writeEnv("DETOKENIZE_BATCH_SIZE=1000\nDETOKENIZE_CONCURRENCY_LIMIT=100");
        VaultController controller = createController();
        List<String> tokens = getTokens(10000);
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();
        try {
            controller.bulkDetokenize(request);
        } catch (Exception ignored) {
            //  Ignore, Testing concurrency/batch config
        }

        // Only 10 batches needed, so concurrency should be clamped to 10
        assertEquals(1000, getPrivateInt(controller, "detokenizeBatchSize"));
        assertEquals(Constants.MAX_DETOKENIZE_CONCURRENCY_LIMIT.intValue(), getPrivateInt(controller, "detokenizeConcurrencyLimit"));
    }


    @Test
    public void testFractionalLastBatchDETOKENIZE() throws Exception {
        writeEnv("DETOKENIZE_BATCH_SIZE=100");
        VaultController controller = createController();
        List<String> tokens = getTokens(9050);
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();
        try {
            controller.bulkDetokenize(request);
        } catch (Exception ignored) {
            //  Ignore, Testing concurrency/batch config
        }


        // Last batch should have 50 records, concurrency should be 101
        assertEquals(100, getPrivateInt(controller, "detokenizeBatchSize"));
        assertEquals(Constants.DETOKENIZE_CONCURRENCY_LIMIT.intValue(), getPrivateInt(controller, "detokenizeConcurrencyLimit"));
    }

    @Test
    public void testDetokenizeBatchFuturesCatchBranchAddsErrorRecord() throws Exception {
        VaultController controller = createController();
        setPrivateField(controller, "detokenizeBatchSize", 2);
        List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest> batches = null; // trigger catch
        List<ErrorRecord> errors = new ArrayList<>();

        Method method = VaultController.class.getDeclaredMethod("detokenizeBatchFutures", ExecutorService.class, List.class, List.class, Map.class);
        method.setAccessible(true);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        @SuppressWarnings("unchecked")
        List<CompletableFuture<DetokenizeResponse>> futures =
                (List<CompletableFuture<DetokenizeResponse>>) method.invoke(controller, executor, batches, errors, Collections.emptyMap());

        Assert.assertTrue(errors.size() == 1);
        Assert.assertEquals(0, errors.get(0).getIndex());
        Assert.assertEquals(500, errors.get(0).getCode());
        executor.shutdownNow();
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

        @Test
    public void testProcessDetokenizeSyncNormalPath() throws Exception {
        VaultConfig cfg = new VaultConfig();
        cfg.setVaultId("vault123");
        cfg.setClusterId("cluster123");
        cfg.setEnv(Env.DEV);
        Credentials creds = new Credentials();
        creds.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        cfg.setCredentials(creds);

        VaultController controller = new VaultController(cfg, creds);
        setPrivateField(controller, "detokenizeConcurrencyLimit", 1);
        setPrivateField(controller, "detokenizeBatchSize", 1);

        List<String> tokens = new ArrayList<>();
        tokens.add("token0");
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();

        java.lang.reflect.Method getDetokenizeRequestBody = VaultController.class.getSuperclass().getDeclaredMethod("getDetokenizeRequestBody", DetokenizeRequest.class);
        getDetokenizeRequestBody.setAccessible(true);
        Object requestObj = getDetokenizeRequestBody.invoke(controller, request);

        java.lang.reflect.Method processDetokenizeSync = VaultController.class.getDeclaredMethod(
                "processDetokenizeSync",
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.class,
                List.class,
                Map.class
        );
        processDetokenizeSync.setAccessible(true);

        try {
            processDetokenizeSync.invoke(controller, requestObj, tokens, Collections.emptyMap());
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof SkyflowException || cause instanceof ExecutionException || cause instanceof RuntimeException);
        }
    }

    @Test
    public void testProcessDetokenizeSyncErrorPath() throws Exception {
        VaultConfig cfg = new VaultConfig();
        cfg.setVaultId("vault123");
        cfg.setClusterId("cluster123");
        cfg.setEnv(Env.DEV);
        Credentials creds = new Credentials();
        creds.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        cfg.setCredentials(creds);

        VaultController controller = new VaultController(cfg, creds);
        setPrivateField(controller, "detokenizeConcurrencyLimit", 1);
        setPrivateField(controller, "detokenizeBatchSize", 1);

        List<String> tokens = new ArrayList<>();
        tokens.add("token0");
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();

        java.lang.reflect.Method getDetokenizeRequestBody = VaultController.class.getSuperclass().getDeclaredMethod("getDetokenizeRequestBody", DetokenizeRequest.class);
        getDetokenizeRequestBody.setAccessible(true);
        Object requestObj = getDetokenizeRequestBody.invoke(controller, request);

        java.lang.reflect.Method processDetokenizeSync = VaultController.class.getDeclaredMethod(
                "processDetokenizeSync",
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest.class,
                List.class,
                Map.class
        );
        processDetokenizeSync.setAccessible(true);

        java.lang.reflect.Method detokenizeBatchFutures = VaultController.class.getDeclaredMethod(
                "detokenizeBatchFutures",
                ExecutorService.class,
                List.class,
                List.class,
                Map.class
        );
        detokenizeBatchFutures.setAccessible(true);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest> batches = null; // will trigger catch
        List<ErrorRecord> errors = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<CompletableFuture<DetokenizeResponse>> futures = (List<CompletableFuture<DetokenizeResponse>>) detokenizeBatchFutures.invoke(controller, executor, batches, errors, Collections.emptyMap());
        assertTrue(errors.size() == 1);
        assertEquals(0, errors.get(0).getIndex());
        assertEquals(500, errors.get(0).getCode());
        executor.shutdownNow();
    }

    // ── Custom header options validation via VaultController ──────────────────

    @Test
    public void bulkInsert_optionsWithEmptyHeaderValue_throwsSkyflowException() throws SkyflowException {
        VaultController controller = createController();
        InsertRequest request = InsertRequest.builder()
                .table("tbl")
                .records(generateValues(1))
                .build();
        InsertOptions opts = InsertOptions.builder()
                .addCustomHeader(CustomHeaderKey.SkyflowAccountID, "")
                .build();
        try {
            controller.bulkInsert(request, opts);
            fail("Expected SkyflowException for empty header value");
        } catch (SkyflowException e) {
            assertEquals(ErrorMessage.EmptyValueInCustomHeaders.getMessage(), e.getMessage());
        }
    }

    @Test
    public void bulkInsert_optionsWithBlankHeaderValue_throwsSkyflowException() throws SkyflowException {
        VaultController controller = createController();
        InsertRequest request = InsertRequest.builder()
                .table("tbl")
                .records(generateValues(1))
                .build();
        InsertOptions opts = InsertOptions.builder()
                .addCustomHeader(CustomHeaderKey.RequestIDHeader, "   ")
                .build();
        try {
            controller.bulkInsert(request, opts);
            fail("Expected SkyflowException for blank header value");
        } catch (SkyflowException e) {
            assertEquals(ErrorMessage.EmptyValueInCustomHeaders.getMessage(), e.getMessage());
        }
    }

    @Test
    public void bulkInsert_nullOptions_doesNotThrowForHeaderValidation() throws SkyflowException {
        VaultController controller = createController();
        InsertRequest request = InsertRequest.builder()
                .table("tbl")
                .records(generateValues(1))
                .build();
        try {
            controller.bulkInsert(request, (InsertOptions) null);
        } catch (SkyflowException e) {
            assertFalse("Should not throw EmptyValueInCustomHeaders for null options",
                    e.getMessage().equals(ErrorMessage.EmptyValueInCustomHeaders.getMessage()));
            assertFalse("Should not throw NullCustomHeaderKey for null options",
                    e.getMessage().equals(ErrorMessage.NullCustomHeaderKey.getMessage()));
        }
    }

    @Test
    public void bulkDetokenize_optionsWithEmptyHeaderValue_throwsSkyflowException() throws SkyflowException {
        VaultController controller = createController();
        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(getTokens(1))
                .build();
        DetokenizeOptions opts = DetokenizeOptions.builder()
                .addCustomHeader(CustomHeaderKey.SkyflowAccountName, "")
                .build();
        try {
            controller.bulkDetokenize(request, opts);
            fail("Expected SkyflowException for empty header value");
        } catch (SkyflowException e) {
            assertEquals(ErrorMessage.EmptyValueInCustomHeaders.getMessage(), e.getMessage());
        }
    }

    @Test
    public void bulkDetokenize_nullOptions_doesNotThrowForHeaderValidation() throws SkyflowException {
        VaultController controller = createController();
        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(getTokens(1))
                .build();
        try {
            controller.bulkDetokenize(request, (DetokenizeOptions) null);
        } catch (SkyflowException e) {
            assertFalse("Should not throw header error for null options",
                    e.getMessage().equals(ErrorMessage.EmptyValueInCustomHeaders.getMessage()));
        }
    }

    @Test
    public void bulkDeleteTokens_optionsWithEmptyHeaderValue_throwsSkyflowException() throws SkyflowException {
        VaultController controller = createController();
        DeleteTokensRequest request = DeleteTokensRequest.builder()
                .tokens(getTokens(1))
                .build();
        DeleteTokensOptions opts = DeleteTokensOptions.builder()
                .addCustomHeader(CustomHeaderKey.RequestIDHeader, "")
                .build();
        try {
            controller.bulkDeleteTokens(request, opts);
            fail("Expected SkyflowException for empty header value");
        } catch (SkyflowException e) {
            assertEquals(ErrorMessage.EmptyValueInCustomHeaders.getMessage(), e.getMessage());
        }
    }

    @Test
    public void bulkTokenize_optionsWithEmptyHeaderValue_throwsSkyflowException() throws SkyflowException {
        VaultController controller = createController();
        ArrayList<TokenizeRecord> data = new ArrayList<>();
        data.add(TokenizeRecord.builder().value("val").build());
        TokenizeRequest request = TokenizeRequest.builder().data(data).build();
        TokenizeOptions opts = TokenizeOptions.builder()
                .addCustomHeader(CustomHeaderKey.SkyflowAccountID, "")
                .build();
        try {
            controller.bulkTokenize(request, opts);
            fail("Expected SkyflowException for empty header value");
        } catch (SkyflowException e) {
            assertEquals(ErrorMessage.EmptyValueInCustomHeaders.getMessage(), e.getMessage());
        }
    }

    @Test
    public void bulkDeleteTokens_nullOptions_doesNotThrowForHeaderValidation() throws SkyflowException {
        VaultController controller = createController();
        DeleteTokensRequest request = DeleteTokensRequest.builder()
                .tokens(getTokens(1))
                .build();
        try {
            controller.bulkDeleteTokens(request, (DeleteTokensOptions) null);
        } catch (SkyflowException e) {
            assertFalse("Should not throw header error for null options",
                    e.getMessage().equals(ErrorMessage.EmptyValueInCustomHeaders.getMessage()));
            assertFalse("Should not throw NullCustomHeaderKey for null options",
                    e.getMessage().equals(ErrorMessage.NullCustomHeaderKey.getMessage()));
        }
    }

    @Test
    public void bulkTokenize_nullOptions_doesNotThrowForHeaderValidation() throws SkyflowException {
        VaultController controller = createController();
        ArrayList<TokenizeRecord> data = new ArrayList<>();
        data.add(TokenizeRecord.builder().value("val").build());
        TokenizeRequest request = TokenizeRequest.builder().data(data).build();
        try {
            controller.bulkTokenize(request, (TokenizeOptions) null);
        } catch (SkyflowException e) {
            assertFalse("Should not throw header error for null options",
                    e.getMessage().equals(ErrorMessage.EmptyValueInCustomHeaders.getMessage()));
            assertFalse("Should not throw NullCustomHeaderKey for null options",
                    e.getMessage().equals(ErrorMessage.NullCustomHeaderKey.getMessage()));
        }
    }

    @Test
    public void bulkInsertAsync_optionsWithEmptyHeaderValue_throwsSkyflowException() throws SkyflowException {
        VaultController controller = createController();
        InsertRequest request = InsertRequest.builder()
                .table("tbl")
                .records(generateValues(1))
                .build();
        InsertOptions opts = InsertOptions.builder()
                .addCustomHeader(CustomHeaderKey.SkyflowAccountName, "")
                .build();
        try {
            controller.bulkInsertAsync(request, opts);
            fail("Expected SkyflowException for empty header value");
        } catch (SkyflowException e) {
            assertEquals(ErrorMessage.EmptyValueInCustomHeaders.getMessage(), e.getMessage());
        }
    }

    @Test
    public void bulkDetokenizeAsync_optionsWithEmptyHeaderValue_throwsSkyflowException() throws SkyflowException {
        VaultController controller = createController();
        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(getTokens(1))
                .build();
        DetokenizeOptions opts = DetokenizeOptions.builder()
                .addCustomHeader(CustomHeaderKey.RequestIDHeader, "   ")
                .build();
        try {
            controller.bulkDetokenizeAsync(request, opts);
            fail("Expected SkyflowException for blank header value");
        } catch (SkyflowException e) {
            assertEquals(ErrorMessage.EmptyValueInCustomHeaders.getMessage(), e.getMessage());
        }
    }

    @Test
    public void bulkDeleteTokensAsync_optionsWithEmptyHeaderValue_throwsSkyflowException() throws SkyflowException {
        VaultController controller = createController();
        DeleteTokensRequest request = DeleteTokensRequest.builder()
                .tokens(getTokens(1))
                .build();
        DeleteTokensOptions opts = DeleteTokensOptions.builder()
                .addCustomHeader(CustomHeaderKey.SkyflowAccountID, "")
                .build();
        try {
            controller.bulkDeleteTokensAsync(request, opts);
            fail("Expected SkyflowException for empty header value");
        } catch (SkyflowException e) {
            assertEquals(ErrorMessage.EmptyValueInCustomHeaders.getMessage(), e.getMessage());
        }
    }

    @Test
    public void bulkTokenizeAsync_optionsWithEmptyHeaderValue_throwsSkyflowException() throws SkyflowException {
        VaultController controller = createController();
        ArrayList<TokenizeRecord> data = new ArrayList<>();
        data.add(TokenizeRecord.builder().value("val").build());
        TokenizeRequest request = TokenizeRequest.builder().data(data).build();
        TokenizeOptions opts = TokenizeOptions.builder()
                .addCustomHeader(CustomHeaderKey.SkyflowAccountName, "  ")
                .build();
        try {
            controller.bulkTokenizeAsync(request, opts);
            fail("Expected SkyflowException for blank header value");
        } catch (SkyflowException e) {
            assertEquals(ErrorMessage.EmptyValueInCustomHeaders.getMessage(), e.getMessage());
        }
    }

    // ── configureDeleteTokensConcurrencyAndBatchSize ──────────────────────────

    @Test
    public void testCustomValidBatchAndConcurrency_DELETE_TOKENS() throws Exception {
        writeEnv("DELETE_TOKENS_BATCH_SIZE=5\nDELETE_TOKENS_CONCURRENCY_LIMIT=3");
        VaultController controller = createController();
        DeleteTokensRequest request = DeleteTokensRequest.builder().tokens(getTokens(20)).build();
        try { controller.bulkDeleteTokens(request); } catch (Exception ignored) {}
        assertEquals(5, getPrivateInt(controller, "deleteTokensBatchSize"));
        assertEquals(3, getPrivateInt(controller, "deleteTokensConcurrencyLimit"));
    }

    @Test
    public void testBatchSizeExceedsMax_DELETE_TOKENS() throws Exception {
        writeEnv("DELETE_TOKENS_BATCH_SIZE=1100");
        VaultController controller = createController();
        DeleteTokensRequest request = DeleteTokensRequest.builder().tokens(getTokens(50)).build();
        try { controller.bulkDeleteTokens(request); } catch (Exception ignored) {}
        assertEquals(Constants.MAX_DELETE_TOKENS_BATCH_SIZE.intValue(), getPrivateInt(controller, "deleteTokensBatchSize"));
    }

    @Test
    public void testConcurrencyExceedsMax_DELETE_TOKENS() throws Exception {
        writeEnv("DELETE_TOKENS_CONCURRENCY_LIMIT=110");
        VaultController controller = createController();
        DeleteTokensRequest request = DeleteTokensRequest.builder().tokens(getTokens(50)).build();
        try { controller.bulkDeleteTokens(request); } catch (Exception ignored) {}
        assertEquals(1, getPrivateInt(controller, "deleteTokensConcurrencyLimit"));
    }

    @Test
    public void testBatchSizeZeroOrNegative_DELETE_TOKENS() throws Exception {
        writeEnv("DELETE_TOKENS_BATCH_SIZE=0");
        VaultController controller = createController();
        DeleteTokensRequest request = DeleteTokensRequest.builder().tokens(getTokens(10)).build();
        try { controller.bulkDeleteTokens(request); } catch (Exception ignored) {}
        assertEquals(Constants.DELETE_TOKENS_BATCH_SIZE.intValue(), getPrivateInt(controller, "deleteTokensBatchSize"));

        writeEnv("DELETE_TOKENS_BATCH_SIZE=-5");
        try { controller.bulkDeleteTokens(request); } catch (Exception ignored) {}
        assertEquals(Constants.DELETE_TOKENS_BATCH_SIZE.intValue(), getPrivateInt(controller, "deleteTokensBatchSize"));
    }

    @Test
    public void testConcurrencyZeroOrNegative_DELETE_TOKENS() throws Exception {
        writeEnv("DELETE_TOKENS_CONCURRENCY_LIMIT=0");
        VaultController controller = createController();
        DeleteTokensRequest request = DeleteTokensRequest.builder().tokens(getTokens(10)).build();
        try { controller.bulkDeleteTokens(request); } catch (Exception ignored) {}
        int min = Math.min(Constants.DELETE_TOKENS_CONCURRENCY_LIMIT,
                (10 + Constants.DELETE_TOKENS_BATCH_SIZE - 1) / Constants.DELETE_TOKENS_BATCH_SIZE);
        assertEquals(min, getPrivateInt(controller, "deleteTokensConcurrencyLimit"));

        writeEnv("DELETE_TOKENS_CONCURRENCY_LIMIT=-5");
        try { controller.bulkDeleteTokens(request); } catch (Exception ignored) {}
        assertEquals(min, getPrivateInt(controller, "deleteTokensConcurrencyLimit"));
    }

    @Test
    public void testNonNumericDeleteTokensBatchSize_usesDefault() throws Exception {
        writeEnv("DELETE_TOKENS_BATCH_SIZE=abc");
        VaultController controller = createController();
        DeleteTokensRequest request = DeleteTokensRequest.builder().tokens(getTokens(10)).build();
        try { controller.bulkDeleteTokens(request); } catch (Exception ignored) {}
        assertEquals(Constants.DELETE_TOKENS_BATCH_SIZE.intValue(), getPrivateInt(controller, "deleteTokensBatchSize"));
    }

    @Test
    public void testNonNumericDeleteTokensConcurrencyLimit_usesDefault() throws Exception {
        writeEnv("DELETE_TOKENS_CONCURRENCY_LIMIT=xyz");
        VaultController controller = createController();
        DeleteTokensRequest request = DeleteTokensRequest.builder().tokens(getTokens(10)).build();
        try { controller.bulkDeleteTokens(request); } catch (Exception ignored) {}
        int expected = Math.min(Constants.DELETE_TOKENS_CONCURRENCY_LIMIT,
                (10 + Constants.DELETE_TOKENS_BATCH_SIZE - 1) / Constants.DELETE_TOKENS_BATCH_SIZE);
        assertEquals(expected, getPrivateInt(controller, "deleteTokensConcurrencyLimit"));
    }

    // ── configureTokenizeConcurrencyAndBatchSize ──────────────────────────────

    @Test
    public void testCustomValidBatchAndConcurrency_TOKENIZE() throws Exception {
        writeEnv("TOKENIZE_BATCH_SIZE=5\nTOKENIZE_CONCURRENCY_LIMIT=3");
        VaultController controller = createController();
        TokenizeRequest request = TokenizeRequest.builder().data(generateTokenizeData(20)).build();
        try { controller.bulkTokenize(request); } catch (Exception ignored) {}
        assertEquals(5, getPrivateInt(controller, "tokenizeBatchSize"));
        assertEquals(3, getPrivateInt(controller, "tokenizeConcurrencyLimit"));
    }

    @Test
    public void testBatchSizeExceedsMax_TOKENIZE() throws Exception {
        writeEnv("TOKENIZE_BATCH_SIZE=1100");
        VaultController controller = createController();
        TokenizeRequest request = TokenizeRequest.builder().data(generateTokenizeData(50)).build();
        try { controller.bulkTokenize(request); } catch (Exception ignored) {}
        assertEquals(Constants.MAX_TOKENIZE_BATCH_SIZE.intValue(), getPrivateInt(controller, "tokenizeBatchSize"));
    }

    @Test
    public void testConcurrencyExceedsMax_TOKENIZE() throws Exception {
        writeEnv("TOKENIZE_CONCURRENCY_LIMIT=110");
        VaultController controller = createController();
        TokenizeRequest request = TokenizeRequest.builder().data(generateTokenizeData(50)).build();
        try { controller.bulkTokenize(request); } catch (Exception ignored) {}
        assertEquals(1, getPrivateInt(controller, "tokenizeConcurrencyLimit"));
    }

    @Test
    public void testBatchSizeZeroOrNegative_TOKENIZE() throws Exception {
        writeEnv("TOKENIZE_BATCH_SIZE=0");
        VaultController controller = createController();
        TokenizeRequest request = TokenizeRequest.builder().data(generateTokenizeData(10)).build();
        try { controller.bulkTokenize(request); } catch (Exception ignored) {}
        assertEquals(Constants.TOKENIZE_BATCH_SIZE.intValue(), getPrivateInt(controller, "tokenizeBatchSize"));

        writeEnv("TOKENIZE_BATCH_SIZE=-5");
        try { controller.bulkTokenize(request); } catch (Exception ignored) {}
        assertEquals(Constants.TOKENIZE_BATCH_SIZE.intValue(), getPrivateInt(controller, "tokenizeBatchSize"));
    }

    @Test
    public void testConcurrencyZeroOrNegative_TOKENIZE() throws Exception {
        writeEnv("TOKENIZE_CONCURRENCY_LIMIT=0");
        VaultController controller = createController();
        TokenizeRequest request = TokenizeRequest.builder().data(generateTokenizeData(10)).build();
        try { controller.bulkTokenize(request); } catch (Exception ignored) {}
        int min = Math.min(Constants.TOKENIZE_CONCURRENCY_LIMIT,
                (10 + Constants.TOKENIZE_BATCH_SIZE - 1) / Constants.TOKENIZE_BATCH_SIZE);
        assertEquals(min, getPrivateInt(controller, "tokenizeConcurrencyLimit"));

        writeEnv("TOKENIZE_CONCURRENCY_LIMIT=-5");
        try { controller.bulkTokenize(request); } catch (Exception ignored) {}
        assertEquals(min, getPrivateInt(controller, "tokenizeConcurrencyLimit"));
    }

    @Test
    public void testNonNumericTokenizeBatchSize_usesDefault() throws Exception {
        writeEnv("TOKENIZE_BATCH_SIZE=abc");
        VaultController controller = createController();
        TokenizeRequest request = TokenizeRequest.builder().data(generateTokenizeData(10)).build();
        try { controller.bulkTokenize(request); } catch (Exception ignored) {}
        assertEquals(Constants.TOKENIZE_BATCH_SIZE.intValue(), getPrivateInt(controller, "tokenizeBatchSize"));
    }

    @Test
    public void testNonNumericTokenizeConcurrencyLimit_usesDefault() throws Exception {
        writeEnv("TOKENIZE_CONCURRENCY_LIMIT=xyz");
        VaultController controller = createController();
        TokenizeRequest request = TokenizeRequest.builder().data(generateTokenizeData(10)).build();
        try { controller.bulkTokenize(request); } catch (Exception ignored) {}
        int expected = Math.min(Constants.TOKENIZE_CONCURRENCY_LIMIT,
                (10 + Constants.TOKENIZE_BATCH_SIZE - 1) / Constants.TOKENIZE_BATCH_SIZE);
        assertEquals(expected, getPrivateInt(controller, "tokenizeConcurrencyLimit"));
    }

    // ── Null batch list early-return paths ────────────────────────────────────

    @Test
    public void deleteTokensBatchFutures_nullBatches_returnsEmptyList() throws Exception {
        VaultController controller = createController();
        Method method = VaultController.class.getDeclaredMethod(
                "deleteTokensBatchFutures", ExecutorService.class, List.class, Map.class);
        method.setAccessible(true);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        @SuppressWarnings("unchecked")
        List<CompletableFuture<DeleteTokensResponse>> futures =
                (List<CompletableFuture<DeleteTokensResponse>>) method.invoke(
                        controller, executor, null, Collections.emptyMap());
        assertTrue("Expected empty list for null batches", futures.isEmpty());
        executor.shutdownNow();
    }

    @Test
    public void tokenizeBatchFutures_nullBatches_returnsEmptyList() throws Exception {
        VaultController controller = createController();
        Method method = VaultController.class.getDeclaredMethod(
                "tokenizeBatchFutures", ExecutorService.class, List.class, Map.class);
        method.setAccessible(true);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        @SuppressWarnings("unchecked")
        List<CompletableFuture<TokenizeResponse>> futures =
                (List<CompletableFuture<TokenizeResponse>>) method.invoke(
                        controller, executor, null, Collections.emptyMap());
        assertTrue("Expected empty list for null batches", futures.isEmpty());
        executor.shutdownNow();
    }

    // ── Async SkyflowException re-throw paths ─────────────────────────────────

    @Test
    public void bulkDeleteTokensAsync_emptyTokens_throwsSkyflowException() throws Exception {
        VaultController controller = createController();
        DeleteTokensRequest request = DeleteTokensRequest.builder().tokens(new ArrayList<>()).build();
        try {
            controller.bulkDeleteTokensAsync(request);
            fail("Expected SkyflowException");
        } catch (SkyflowException e) {
            assertEquals(ErrorMessage.EmptyDeleteTokensData.getMessage(), e.getMessage());
        }
    }

    @Test
    public void bulkTokenizeAsync_emptyData_throwsSkyflowException() throws Exception {
        VaultController controller = createController();
        TokenizeRequest request = TokenizeRequest.builder().data(new ArrayList<>()).build();
        try {
            controller.bulkTokenizeAsync(request);
            fail("Expected SkyflowException");
        } catch (SkyflowException e) {
            assertEquals(ErrorMessage.EmptyTokenizeData.getMessage(), e.getMessage());
        }
    }

    // ── extractCustomHeaders ──────────────────────────────────────────────────

    @Test
    public void extractCustomHeaders_insertOptions_null_returnsEmptyMap() throws Exception {
        VaultController controller = createController();
        Method method = VaultController.class.getDeclaredMethod("extractCustomHeaders", InsertOptions.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(controller, (InsertOptions) null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void extractCustomHeaders_insertOptions_withHeaders_convertsToStringKeys() throws Exception {
        VaultController controller = createController();
        Method method = VaultController.class.getDeclaredMethod("extractCustomHeaders", InsertOptions.class);
        method.setAccessible(true);
        InsertOptions opts = InsertOptions.builder()
                .addCustomHeader(CustomHeaderKey.SkyflowAccountID, "acct-val")
                .addCustomHeader(CustomHeaderKey.RequestIDHeader, "req-123")
                .build();
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(controller, opts);
        assertEquals(2, result.size());
        assertEquals("acct-val", result.get(CustomHeaderKey.SkyflowAccountID.toString()));
        assertEquals("req-123", result.get(CustomHeaderKey.RequestIDHeader.toString()));
    }

    @Test
    public void extractCustomHeaders_detokenizeOptions_null_returnsEmptyMap() throws Exception {
        VaultController controller = createController();
        Method method = VaultController.class.getDeclaredMethod("extractCustomHeaders", DetokenizeOptions.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(controller, (DetokenizeOptions) null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void extractCustomHeaders_detokenizeOptions_withHeaders_convertsToStringKeys() throws Exception {
        VaultController controller = createController();
        Method method = VaultController.class.getDeclaredMethod("extractCustomHeaders", DetokenizeOptions.class);
        method.setAccessible(true);
        DetokenizeOptions opts = DetokenizeOptions.builder()
                .addCustomHeader(CustomHeaderKey.SkyflowAccountName, "my-account")
                .build();
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(controller, opts);
        assertEquals("my-account", result.get(CustomHeaderKey.SkyflowAccountName.toString()));
    }

    @Test
    public void extractCustomHeaders_deleteTokensOptions_null_returnsEmptyMap() throws Exception {
        VaultController controller = createController();
        Method method = VaultController.class.getDeclaredMethod("extractCustomHeaders", DeleteTokensOptions.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(controller, (DeleteTokensOptions) null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void extractCustomHeaders_deleteTokensOptions_withHeaders_convertsToStringKeys() throws Exception {
        VaultController controller = createController();
        Method method = VaultController.class.getDeclaredMethod("extractCustomHeaders", DeleteTokensOptions.class);
        method.setAccessible(true);
        DeleteTokensOptions opts = DeleteTokensOptions.builder()
                .addCustomHeader(CustomHeaderKey.RequestIDHeader, "del-req")
                .build();
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(controller, opts);
        assertEquals("del-req", result.get(CustomHeaderKey.RequestIDHeader.toString()));
    }

    @Test
    public void extractCustomHeaders_tokenizeOptions_null_returnsEmptyMap() throws Exception {
        VaultController controller = createController();
        Method method = VaultController.class.getDeclaredMethod("extractCustomHeaders", TokenizeOptions.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(controller, (TokenizeOptions) null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void extractCustomHeaders_tokenizeOptions_withHeaders_convertsToStringKeys() throws Exception {
        VaultController controller = createController();
        Method method = VaultController.class.getDeclaredMethod("extractCustomHeaders", TokenizeOptions.class);
        method.setAccessible(true);
        TokenizeOptions opts = TokenizeOptions.builder()
                .addCustomHeader(CustomHeaderKey.SkyflowAccountID, "tok-acct")
                .build();
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(controller, opts);
        assertEquals("tok-acct", result.get(CustomHeaderKey.SkyflowAccountID.toString()));
    }

    // ── buildRequestOptions ───────────────────────────────────────────────────

    @Test
    public void buildRequestOptions_noCustomHeaders_containsMetricsHeader() throws Exception {
        VaultController controller = createController();
        Method method = VaultController.class.getDeclaredMethod("buildRequestOptions", Map.class);
        method.setAccessible(true);
        com.skyflow.generated.rest.core.RequestOptions opts =
                (com.skyflow.generated.rest.core.RequestOptions) method.invoke(controller, Collections.emptyMap());
        Map<String, String> headers = opts.getHeaders();
        assertTrue("Metrics header must always be present", headers.containsKey(com.skyflow.utils.Constants.SDK_METRICS_HEADER_KEY));
    }

    @Test
    public void buildRequestOptions_withCustomHeaders_containsBothMetricsAndCustom() throws Exception {
        VaultController controller = createController();
        Method method = VaultController.class.getDeclaredMethod("buildRequestOptions", Map.class);
        method.setAccessible(true);
        Map<String, String> custom = new HashMap<>();
        custom.put(CustomHeaderKey.SkyflowAccountID.toString(), "acct-999");
        custom.put(CustomHeaderKey.RequestIDHeader.toString(), "req-001");
        com.skyflow.generated.rest.core.RequestOptions opts =
                (com.skyflow.generated.rest.core.RequestOptions) method.invoke(controller, custom);
        Map<String, String> headers = opts.getHeaders();
        assertTrue("Metrics header must be present", headers.containsKey(com.skyflow.utils.Constants.SDK_METRICS_HEADER_KEY));
        assertEquals("acct-999", headers.get(CustomHeaderKey.SkyflowAccountID.toString()));
        assertEquals("req-001", headers.get(CustomHeaderKey.RequestIDHeader.toString()));
    }

    @Test
    public void buildRequestOptions_nullCustomHeaders_containsOnlyMetricsHeader() throws Exception {
        VaultController controller = createController();
        Method method = VaultController.class.getDeclaredMethod("buildRequestOptions", Map.class);
        method.setAccessible(true);
        com.skyflow.generated.rest.core.RequestOptions opts =
                (com.skyflow.generated.rest.core.RequestOptions) method.invoke(controller, (Map<String, String>) null);
        Map<String, String> headers = opts.getHeaders();
        assertTrue("Metrics header must be present even for null custom headers",
                headers.containsKey(com.skyflow.utils.Constants.SDK_METRICS_HEADER_KEY));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void invokeConfigureDeleteTokensConcurrencyAndBatchSize(VaultController controller, int totalRequests) throws Exception {
        Method method = VaultController.class.getDeclaredMethod("configureDeleteTokensConcurrencyAndBatchSize", int.class);
        method.setAccessible(true);
        method.invoke(controller, totalRequests);
    }

    private void invokeConfigureTokenizeConcurrencyAndBatchSize(VaultController controller, int totalRequests) throws Exception {
        Method method = VaultController.class.getDeclaredMethod("configureTokenizeConcurrencyAndBatchSize", int.class);
        method.setAccessible(true);
        method.invoke(controller, totalRequests);
    }

    private ArrayList<TokenizeRecord> generateTokenizeData(int count) {
        ArrayList<TokenizeRecord> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            data.add(TokenizeRecord.builder().value("val" + i).build());
        }
        return data;
    }
}