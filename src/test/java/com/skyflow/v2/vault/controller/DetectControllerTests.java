package com.skyflow.v2.vault.controller;

import com.skyflow.v2.Skyflow;
import com.skyflow.common.config.Credentials;
import com.skyflow.common.config.VaultConfig;
import com.skyflow.common.enums.Env;
import com.skyflow.common.enums.LogLevel;
import com.skyflow.common.errors.ErrorMessage;
import com.skyflow.common.errors.HttpStatus;
import com.skyflow.common.errors.SkyflowException;
import com.skyflow.v2.utils.Constants;
import com.skyflow.v2.utils.Utils;
import com.skyflow.v2.vault.detect.DeidentifyTextRequest;
import com.skyflow.v2.vault.detect.ReidentifyTextRequest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class DetectControllerTests {
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String vaultID = null;
    private static String clusterID = null;
    private static VaultConfig vaultConfig = null;
    private static Skyflow skyflowClient = null;

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
    public void testNullTextInRequestInDeidentifyStringMethod() {
        try {
            DeidentifyTextRequest request = DeidentifyTextRequest.builder().text(null).build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.detect(vaultID).deidentifyText(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.InvalidTextInDeIdentify.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
            Assert.assertNull(e.getRequestId());
            Assert.assertNull(e.getGrpcCode());
            Assert.assertEquals(HttpStatus.BAD_REQUEST.getHttpStatus(), e.getHttpStatus());
        }
    }

    @Test
    public void testEmptyTextInRequestInDeidentifyStringMethod() {
        try {
            DeidentifyTextRequest request = DeidentifyTextRequest.builder().text("").build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.detect(vaultID).deidentifyText(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.InvalidTextInDeIdentify.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
            Assert.assertNull(e.getRequestId());
            Assert.assertNull(e.getGrpcCode());
            Assert.assertEquals(HttpStatus.BAD_REQUEST.getHttpStatus(), e.getHttpStatus());
        }
    }

    @Test
    public void testNullTextInRequestInReidentifyStringMethod() {
        try {
            ReidentifyTextRequest request = ReidentifyTextRequest.builder().text(null).build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.detect(vaultID).reidentifyText(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.InvalidTextInReIdentify.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
            Assert.assertNull(e.getRequestId());
            Assert.assertNull(e.getGrpcCode());
            Assert.assertEquals(HttpStatus.BAD_REQUEST.getHttpStatus(), e.getHttpStatus());
        }
    }

    @Test
    public void testEmptyTextInRequestInReidentifyStringMethod() {
        try {
            ReidentifyTextRequest request = ReidentifyTextRequest.builder().text("").build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.detect(vaultID).reidentifyText(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.InvalidTextInReIdentify.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
            Assert.assertNull(e.getRequestId());
            Assert.assertNull(e.getGrpcCode());
            Assert.assertEquals(HttpStatus.BAD_REQUEST.getHttpStatus(), e.getHttpStatus());
        }
    }

}

