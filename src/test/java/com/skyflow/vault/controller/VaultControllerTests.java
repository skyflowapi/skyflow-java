package com.skyflow.vault.controller;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.HttpStatus;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.vault.data.*;
import com.skyflow.vault.tokens.DetokenizeData;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.TokenizeRequest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VaultControllerTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String vaultID = null;
    private static String clusterID = null;
    private static VaultConfig vaultConfig = null;
    private static Skyflow skyflowClient = null;
    private ApiClient mockApiClient;

    @BeforeClass
    public static void setup() throws SkyflowException, NoSuchMethodException {
        vaultID = "vault123";
        clusterID = "cluster123";

        Credentials credentials = new Credentials();
        credentials.setToken("valid-token");

        vaultConfig = new VaultConfig();
        vaultConfig.setVaultId(vaultID);
        vaultConfig.setClusterId(clusterID);
        vaultConfig.setEnv(Env.DEV);
        vaultConfig.setCredentials(credentials);


        skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)
                .addVaultConfig(vaultConfig)
                .build();

    }

    @Test
    public void testInvalidRequestInInsertMethod() {
        try {
            InsertRequest request = InsertRequest.builder().build();
            skyflowClient.vault().insert(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TableKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testInvalidRequestInDetokenizeMethod() {
        try {
            DetokenizeRequest request = DetokenizeRequest.builder().build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().detokenize(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.InvalidDetokenizeData.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testInvalidRequestInGetMethod() {
        try {
            GetRequest request = GetRequest.builder().build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().get(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TableKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testInvalidRequestInUpdateMethod() {
        try {
            UpdateRequest request = UpdateRequest.builder().build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().update(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TableKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testInvalidRequestInDeleteMethod() {
        try {
            DeleteRequest request = DeleteRequest.builder().build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().delete(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TableKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testInvalidRequestInQueryMethod() {
        try {
            QueryRequest request = QueryRequest.builder().build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().query(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.QueryKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testInvalidRequestInTokenizeMethod() {
        try {
            TokenizeRequest request = TokenizeRequest.builder().build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().tokenize(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.ColumnValuesKeyErrorTokenize.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
            Assert.assertNull(e.getRequestId());
            Assert.assertNull(e.getGrpcCode());
            Assert.assertTrue(e.getDetails().isEmpty());
            Assert.assertEquals(HttpStatus.BAD_REQUEST.getHttpStatus(), e.getHttpStatus());
        }
    }

    @Test
    public void testInvalidRequestInFileUploadMethod() {
        try {
            FileUploadRequest request = FileUploadRequest.builder().build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().uploadFile(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TableKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
            Assert.assertNull(e.getRequestId());
            Assert.assertNull(e.getGrpcCode());
            Assert.assertTrue(e.getDetails().isEmpty());
            Assert.assertEquals(HttpStatus.BAD_REQUEST.getHttpStatus(), e.getHttpStatus());
        }
    }

    @Test
    public void testApiClientExceptionInInsertMethod() {
        try {
            ArrayList<HashMap<String, Object>> values1 = new ArrayList<>();
            HashMap<String, Object> value1 = new HashMap<>();
            value1.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>");
            values1.add(value1);
            InsertRequest request = InsertRequest.builder().table("table1").values(values1).build();

            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().insert(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals("Network error executing HTTP request", e.getMessage());
            Assert.assertNull(e.getRequestId());
            Assert.assertNull(e.getGrpcCode());
            Assert.assertNull(e.getDetails());
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        }
    }

    @Test
    public void testApiClientExceptionInDetokenizeMethod() {
        try {
            DetokenizeData data = new DetokenizeData("token");
            ArrayList<DetokenizeData> detokenizeData = new ArrayList<>();
            detokenizeData.add(data);
            DetokenizeRequest request = DetokenizeRequest.builder().detokenizeData(detokenizeData).build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().detokenize(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals("Network error executing HTTP request", e.getMessage());
            Assert.assertNull(e.getRequestId());
            Assert.assertNull(e.getGrpcCode());
            Assert.assertNull(e.getDetails());
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        }
    }

    @Test
    public void testApiClientExceptionInGetMethod() {
        try {
            ArrayList<String> skyflowIds = new ArrayList<>();
            skyflowIds.add("<YOUR_SKYFLOW_ID>"); // Replace with the Skyflow
            GetRequest request = GetRequest.builder().ids(skyflowIds).table("table1").build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().get(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals("Network error executing HTTP request", e.getMessage());
            Assert.assertNull(e.getRequestId());
            Assert.assertNull(e.getGrpcCode());
            Assert.assertNull(e.getDetails());
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        }
    }

    @Test
    public void testApiClientExceptionInUpdateMethod() {
        try {
            HashMap<String, Object> data1 = new HashMap<>();
            data1.put("skyflow_id", "<YOUR_SKYFLOW_ID>"); // Replace with the Skyflow ID of the record

            UpdateRequest request = UpdateRequest.builder().data(data1).table("table1").build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().update(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals("Network error executing HTTP request", e.getMessage());
            Assert.assertNull(e.getRequestId());
            Assert.assertNull(e.getGrpcCode());
            Assert.assertNull(e.getDetails());
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        }
    }

    @Test
    public void testApiClientExceptionInDeleteMethod() {
        try {
            ArrayList<String> skyflowIds = new ArrayList<>();
            skyflowIds.add("<YOUR_SKYFLOW_ID>"); // Replace with the Skyflow
            DeleteRequest request = DeleteRequest.builder().ids(skyflowIds).table("table1").build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().delete(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals("Network error executing HTTP request", e.getMessage());
            Assert.assertNull(e.getRequestId());
            Assert.assertNull(e.getGrpcCode());
            Assert.assertNull(e.getDetails());
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        }
    }

    @Test
    public void testApiClientExceptionInQueryMethod() {
        try {
            QueryRequest request = QueryRequest.builder().query("SELECT * FROM table1").build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().query(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals("Network error executing HTTP request", e.getMessage());
            Assert.assertNull(e.getRequestId());
            Assert.assertNull(e.getGrpcCode());
            Assert.assertNull(e.getDetails());
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        }
    }

    @Test
    public void testApiClientExceptionInTokenizeMethod() {
        try {
            List<com.skyflow.vault.tokens.ColumnValue> columnValues = new ArrayList<>();
            columnValues.add(com.skyflow.vault.tokens.ColumnValue.builder().value("val").columnGroup("grp").build());
            TokenizeRequest request = TokenizeRequest.builder().values(columnValues).build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().tokenize(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals("Network error executing HTTP request", e.getMessage());
            Assert.assertNull(e.getRequestId());
            Assert.assertNull(e.getGrpcCode());
            Assert.assertNull(e.getDetails());
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        }
    }

    @Test
    public void testApiClientExceptionInFileUploadMethod() {
        try {
            FileUploadRequest request = FileUploadRequest.builder().columnName("column").table("table1").filePath("./src/test/resources/noPrivateKeyCredentials.json").build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.vault().uploadFile(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals("Network error executing HTTP request", e.getMessage());
            Assert.assertNull(e.getRequestId());
            Assert.assertNull(e.getGrpcCode());
            Assert.assertNull(e.getDetails());
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        }
    }
}