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
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.TokenizeRequest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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

//    @Test
//    public void testInvalidRequestInGetMethod() {
//        try {
//            GetRequest request = GetRequest.builder().build();
//            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
//            skyflowClient.vault().get(request);
//            Assert.fail(EXCEPTION_NOT_THROWN);
//        } catch (SkyflowException e) {
//            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
//            Assert.assertEquals(
//                    Utils.parameterizedString(ErrorMessage.TableKeyError.getMessage(), Constants.SDK_PREFIX),
//                    e.getMessage()
//            );
//        }
//    }

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

}
