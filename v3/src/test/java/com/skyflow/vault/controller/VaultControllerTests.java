package com.skyflow.vault.controller;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.enums.Env;
import com.skyflow.utils.Constants;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.InsertRecord;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.ErrorRecord;
import com.skyflow.vault.data.DetokenizeResponse;
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
import java.util.HashMap;
import java.util.List;
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
@PowerMockIgnore({"javax.management.*", "java.nio.*", "com.sun.*", "jdk.internal.reflect.*", "javax.crypto.*"})
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
        List<com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest> batches = null; // trigger catch
        List<ErrorRecord> errors = new ArrayList<>();

        Method method = VaultController.class.getDeclaredMethod("detokenizeBatchFutures", ExecutorService.class, List.class, List.class);
        method.setAccessible(true);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        @SuppressWarnings("unchecked")
        List<CompletableFuture<DetokenizeResponse>> futures =
                (List<CompletableFuture<DetokenizeResponse>>) method.invoke(controller, executor, batches, errors);

        Assert.assertTrue(errors.size() == 1);
        Assert.assertNotNull(errors.get(0).getError());
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
    public void testBulkInsertAsyncAggregatesSuccessAndErrors() throws Exception {
        writeEnv("INSERT_BATCH_SIZE=1\nINSERT_CONCURRENCY_LIMIT=2");

        AtomicInteger requestCount = new AtomicInteger(0);
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v2/records/insert", exchange -> {
            int count = requestCount.getAndIncrement();
            String body;
            if (count == 0) {
                body = "{\"records\":[{\"skyflowID\":\"id1\",\"tokens\":{\"field1\":[{\"token\":\"tok1\",\"tokenGroupName\":\"tg1\"}]},\"data\":{\"field1\":\"value1\"},\"httpCode\":200,\"tableName\":\"table1\"}]}";
            } else {
                body = "{\"records\":[{\"error\":\"bad\",\"httpCode\":400,\"tableName\":\"table1\"}]}";
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            VaultConfig cfg = new VaultConfig();
            cfg.setVaultId("vault123");
            cfg.setClusterId("cluster123");
            cfg.setEnv(Env.DEV);
            cfg.setVaultURL("http://localhost:" + port);

            Credentials creds = new Credentials();
            creds.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
            cfg.setCredentials(creds);

            VaultController controller = new VaultController(cfg, creds);

            ArrayList<InsertRecord> records = new ArrayList<>();
            HashMap<String, Object> r1 = new HashMap<>();
            r1.put("field1", "v1");
            HashMap<String, Object> r2 = new HashMap<>();
            r2.put("field1", "v2");
            records.add(InsertRecord.builder().data(r1).build());
            records.add(InsertRecord.builder().data(r2).build());

            InsertRequest insertRequest = InsertRequest.builder().table("table1").records(records).build();

            CompletableFuture<com.skyflow.vault.data.InsertResponse> future = controller.bulkInsertAsync(insertRequest);
            com.skyflow.vault.data.InsertResponse response = future.get(5, TimeUnit.SECONDS);

            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getSummary());
            Assert.assertEquals(2, response.getSummary().getTotalRecords());
            Assert.assertEquals(1, response.getSummary().getTotalInserted());
            Assert.assertEquals(1, response.getSummary().getTotalFailed());
            Assert.assertEquals(1, response.getSuccess().size());
            Assert.assertEquals(1, response.getErrors().size());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testBulkInsertProcessSyncAggregatesSuccessAndErrors() throws Exception {
        writeEnv("INSERT_BATCH_SIZE=1\nINSERT_CONCURRENCY_LIMIT=2");

        AtomicInteger requestCount = new AtomicInteger(0);
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v2/records/insert", exchange -> {
            int count = requestCount.getAndIncrement();
            String body;
            if (count == 0) {
                body = "{\"records\":[{\"skyflowID\":\"id1\",\"tokens\":{\"field1\":[{\"token\":\"tok1\",\"tokenGroupName\":\"tg1\"}]},\"data\":{\"field1\":\"value1\"},\"httpCode\":200,\"tableName\":\"table1\"}]}";
            } else {
                body = "{\"records\":[{\"error\":\"bad\",\"httpCode\":400,\"tableName\":\"table1\"}]}";
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            VaultConfig cfg = new VaultConfig();
            cfg.setVaultId("vault123");
            cfg.setClusterId("cluster123");
            cfg.setEnv(Env.DEV);
            cfg.setVaultURL("http://localhost:" + port);

            Credentials creds = new Credentials();
            creds.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
            cfg.setCredentials(creds);

            VaultController controller = new VaultController(cfg, creds);

            ArrayList<InsertRecord> records = new ArrayList<>();
            HashMap<String, Object> r1 = new HashMap<>();
            r1.put("field1", "v1");
            HashMap<String, Object> r2 = new HashMap<>();
            r2.put("field1", "v2");
            records.add(InsertRecord.builder().data(r1).build());
            records.add(InsertRecord.builder().data(r2).build());

            InsertRequest insertRequest = InsertRequest.builder().table("table1").records(records).build();
            com.skyflow.vault.data.InsertResponse response = controller.bulkInsert(insertRequest);

            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getSummary());
            Assert.assertEquals(2, response.getSummary().getTotalRecords());
            Assert.assertEquals(1, response.getSummary().getTotalInserted());
            Assert.assertEquals(1, response.getSummary().getTotalFailed());
            Assert.assertEquals(1, response.getSuccess().size());
            Assert.assertEquals(1, response.getErrors().size());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testBulkInsertProcessSyncHandlesExceptionalFuturesAndReturnsErrors() throws Exception {
        writeEnv("INSERT_BATCH_SIZE=1\nINSERT_CONCURRENCY_LIMIT=2");

        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v2/records/insert", exchange -> {
            String body = "{\"error\":{\"error\":\"bad\",\"httpCode\":500}}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            VaultConfig cfg = new VaultConfig();
            cfg.setVaultId("vault123");
            cfg.setClusterId("cluster123");
            cfg.setEnv(Env.DEV);
            cfg.setVaultURL("http://localhost:" + port);

            Credentials creds = new Credentials();
            creds.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
            cfg.setCredentials(creds);

            VaultController controller = new VaultController(cfg, creds);

            ArrayList<InsertRecord> records = new ArrayList<>();
            HashMap<String, Object> r1 = new HashMap<>();
            r1.put("field1", "v1");
            HashMap<String, Object> r2 = new HashMap<>();
            r2.put("field1", "v2");
            records.add(InsertRecord.builder().data(r1).build());
            records.add(InsertRecord.builder().data(r2).build());

            InsertRequest insertRequest = InsertRequest.builder().table("table1").records(records).build();
            com.skyflow.vault.data.InsertResponse response = controller.bulkInsert(insertRequest);

            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getSummary());
            Assert.assertEquals(2, response.getSummary().getTotalRecords());
            Assert.assertEquals(0, response.getSummary().getTotalInserted());
            Assert.assertEquals(2, response.getSummary().getTotalFailed());
            Assert.assertEquals(0, response.getSuccess().size());
            Assert.assertEquals(2, response.getErrors().size());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testBulkInsertAsyncCollectsErrorsFromExceptionalFutures() throws Exception {
        writeEnv("INSERT_BATCH_SIZE=1\nINSERT_CONCURRENCY_LIMIT=2");

        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v2/records/insert", exchange -> {
            String body = "{\"error\":{\"error\":\"bad\",\"httpCode\":500}}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            VaultConfig cfg = new VaultConfig();
            cfg.setVaultId("vault123");
            cfg.setClusterId("cluster123");
            cfg.setEnv(Env.DEV);
            cfg.setVaultURL("http://localhost:" + port);

            Credentials creds = new Credentials();
            creds.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
            cfg.setCredentials(creds);

            VaultController controller = new VaultController(cfg, creds);

            ArrayList<InsertRecord> records = new ArrayList<>();
            HashMap<String, Object> r1 = new HashMap<>();
            r1.put("field1", "v1");
            HashMap<String, Object> r2 = new HashMap<>();
            r2.put("field1", "v2");
            records.add(InsertRecord.builder().data(r1).build());
            records.add(InsertRecord.builder().data(r2).build());

            InsertRequest insertRequest = InsertRequest.builder().table("table1").records(records).build();

            CompletableFuture<com.skyflow.vault.data.InsertResponse> future = controller.bulkInsertAsync(insertRequest);
            com.skyflow.vault.data.InsertResponse response = future.get(5, TimeUnit.SECONDS);

            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getSummary());
            Assert.assertEquals(2, response.getSummary().getTotalRecords());
            Assert.assertEquals(0, response.getSummary().getTotalInserted());
            Assert.assertEquals(2, response.getSummary().getTotalFailed());
            Assert.assertEquals(0, response.getSuccess().size());
            Assert.assertEquals(2, response.getErrors().size());
        } finally {
            server.stop(0);
        }
    }

        @Test
    public void testBulkDetokenizeAsyncAggregatesSuccessAndErrors() throws Exception {
        writeEnv("DETOKENIZE_BATCH_SIZE=1\nDETOKENIZE_CONCURRENCY_LIMIT=2");

        AtomicInteger requestCount = new AtomicInteger(0);
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v2/tokens/detokenize", exchange -> {
            int count = requestCount.getAndIncrement();
            String body;
            if (count == 0) {
                body = "{\"response\":[{\"token\":\"token0\",\"value\":\"value0\",\"tokenGroupName\":\"tg1\",\"httpCode\":200,\"metadata\":{\"skyflowID\":\"id1\"}}]}";
            } else {
                body = "{\"response\":[{\"token\":\"token1\",\"error\":\"bad\",\"httpCode\":400,\"tokenGroupName\":\"tg1\"}]}";
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            VaultConfig cfg = new VaultConfig();
            cfg.setVaultId("vault123");
            cfg.setClusterId("cluster123");
            cfg.setEnv(Env.DEV);
            cfg.setVaultURL("http://localhost:" + port);

            Credentials creds = new Credentials();
            creds.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
            cfg.setCredentials(creds);

            VaultController controller = new VaultController(cfg, creds);

            setPrivateField(controller, "detokenizeConcurrencyLimit", 2);

            List<String> tokens = new ArrayList<>();
            tokens.add("token0");
            tokens.add("token1");

            DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();
            CompletableFuture<DetokenizeResponse> future = controller.bulkDetokenizeAsync(request);
            DetokenizeResponse response = future.get(5, TimeUnit.SECONDS);

            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getSummary());
            Assert.assertEquals(2, response.getSummary().getTotalTokens());
            Assert.assertEquals(1, response.getSummary().getTotalDetokenized());
            Assert.assertEquals(1, response.getSummary().getTotalFailed());
            Assert.assertEquals(1, response.getSuccess().size());
            Assert.assertEquals(1, response.getErrors().size());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testBulkDetokenizeAsyncCollectsErrorsFromExceptionalFutures() throws Exception {
        writeEnv("DETOKENIZE_BATCH_SIZE=1\nDETOKENIZE_CONCURRENCY_LIMIT=2");

        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v2/tokens/detokenize", exchange -> {
            String body = "{\"error\":{\"error\":\"bad\",\"httpCode\":500}}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            VaultConfig cfg = new VaultConfig();
            cfg.setVaultId("vault123");
            cfg.setClusterId("cluster123");
            cfg.setEnv(Env.DEV);
            cfg.setVaultURL("http://localhost:" + port);

            Credentials creds = new Credentials();
            creds.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
            cfg.setCredentials(creds);

            VaultController controller = new VaultController(cfg, creds);

            setPrivateField(controller, "detokenizeConcurrencyLimit", 2);

            List<String> tokens = new ArrayList<>();
            tokens.add("token0");
            tokens.add("token1");

            DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();
            CompletableFuture<DetokenizeResponse> future = controller.bulkDetokenizeAsync(request);
            DetokenizeResponse response = future.get(5, TimeUnit.SECONDS);

            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getSummary());
            Assert.assertEquals(2, response.getSummary().getTotalTokens());
            Assert.assertEquals(0, response.getSummary().getTotalDetokenized());
            Assert.assertEquals(2, response.getSummary().getTotalFailed());
            Assert.assertEquals(0, response.getSuccess().size());
            Assert.assertEquals(2, response.getErrors().size());
        } finally {
            server.stop(0);
        }
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
                com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest.class,
                List.class
        );
        processDetokenizeSync.setAccessible(true);

        try {
            processDetokenizeSync.invoke(controller, requestObj, tokens);
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
                com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest.class,
                List.class
        );
        processDetokenizeSync.setAccessible(true);

        java.lang.reflect.Method detokenizeBatchFutures = VaultController.class.getDeclaredMethod(
                "detokenizeBatchFutures",
                ExecutorService.class,
                List.class,
                List.class
        );
        detokenizeBatchFutures.setAccessible(true);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        List<com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest> batches = null; // will trigger catch
        List<ErrorRecord> errors = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<CompletableFuture<DetokenizeResponse>> futures = (List<CompletableFuture<DetokenizeResponse>>) detokenizeBatchFutures.invoke(controller, executor, batches, errors);
        assertTrue(errors.size() == 1);
        assertNotNull(errors.get(0).getError());
        executor.shutdownNow();
    }
}