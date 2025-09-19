package com.skyflow.vault.controller;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Constants;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.InsertRecord;
import com.skyflow.vault.data.InsertRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import static org.junit.Assert.*;


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
        // Print the contents of the .env file
        try (Scanner scanner = new Scanner(envFile)) {
            System.out.println("Current .env contents:");
            while (scanner.hasNextLine()) {
                System.out.println(scanner.nextLine());
            }
        } catch (IOException e) {
            System.out.println("Could not read .env file: " + e.getMessage());
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

        assertEquals(1000, getPrivateInt(controller, "insertBatchSize"));
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

        assertEquals(50, getPrivateInt(controller, "insertBatchSize"));

        writeEnv("INSERT_BATCH_SIZE=-5");

        try {
            controller.bulkInsert(insertRequest);
        } catch (Exception ignored) {
            // Ignore, Testing concurrency/batch config
        }

        assertEquals(50, getPrivateInt(controller, "insertBatchSize"));
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
        assertEquals(10, getPrivateInt(controller, "insertConcurrencyLimit"));
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
        assertEquals(10, getPrivateInt(controller, "insertConcurrencyLimit"));
    }

    private int getPrivateInt(Object obj, String field) throws Exception {
        Field f = obj.getClass().getDeclaredField(field);
        f.setAccessible(true);
        return f.getInt(obj);
    }
}