package com.skyflow.vault.controller;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.HttpStatus;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Constants;
import com.skyflow.utils.SdkVersion;
import com.skyflow.utils.Utils;
import com.skyflow.vault.detect.DeidentifyTextRequest;
import com.skyflow.vault.detect.ReidentifyTextRequest;
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
        SdkVersion.setSdkPrefix(Constants.SDK_PREFIX);
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

