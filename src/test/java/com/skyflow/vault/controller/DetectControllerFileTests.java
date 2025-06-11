package com.skyflow.vault.controller;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.detect.AudioBleep;
import com.skyflow.vault.detect.DeidentifyFileRequest;
import com.skyflow.vault.detect.GetDetectRunRequest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;

public class DetectControllerFileTests {
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String vaultID = null;
    private static String clusterID = null;
    private static VaultConfig vaultConfig = null;
    private static DetectController detectController = null;

    @BeforeClass
    public static void setup() {
        vaultID = "vault123";
        clusterID = "cluster123";

        Credentials credentials = new Credentials();
        credentials.setToken("valid-token");

        vaultConfig = new VaultConfig();
        vaultConfig.setVaultId(vaultID);
        vaultConfig.setClusterId(clusterID);
        vaultConfig.setEnv(com.skyflow.enums.Env.DEV);
        vaultConfig.setCredentials(credentials);

        detectController = new DetectController(vaultConfig, credentials);
    }

    @Test
    public void testNullFileInDeidentifyFileRequest() {
        try {
            DeidentifyFileRequest request = DeidentifyFileRequest.builder().build();
            detectController.deidentifyFile(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        }
    }

    @Test
    public void testUnreadableFileInDeidentifyFileRequest() {
        try {
            File file = new File("unreadable.txt") {
                @Override
                public boolean exists() { return true; }
                @Override
                public boolean isFile() { return true; }
                @Override
                public boolean canRead() { return false; }
            };
            DeidentifyFileRequest request = DeidentifyFileRequest.builder().file(file).build();
            detectController.deidentifyFile(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException | RuntimeException e) {
            // Depending on implementation, could be SkyflowException or RuntimeException
            Assert.assertTrue(e.getMessage().contains("not readable") || e.getMessage().contains("unreadable.txt"));
        }
    }

    @Test
    public void testNullEntitiesInDeidentifyFileRequest() {
        try {
            File file = File.createTempFile("test", ".txt");
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(file)
                    .entities(new ArrayList<>())
                    .build();
            detectController.deidentifyFile(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (Exception e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        }
    }

    @Test
    public void testNullRequest() {
        try {
            detectController.deidentifyFile(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (Exception e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        }
    }

    @Test
    public void testNonExistentFileInDeidentifyFileRequest() {
        try {
            File file = new File("nonexistent.txt");
            DeidentifyFileRequest request = DeidentifyFileRequest.builder().file(file).build();
            detectController.deidentifyFile(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (Exception e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        }
    }

    @Test
    public void testInvalidPixelDensity() throws Exception {
        File file = File.createTempFile("test", ".txt");
        try {
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(file)
                    .pixelDensity(-1)
                    .build();
            detectController.deidentifyFile(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (Exception e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        }
        file.delete();
    }

    @Test
    public void testInvalidMaxResolution() throws Exception {
        File file = File.createTempFile("test", ".txt");
        try {
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(file)
                    .maxResolution(-1)
                    .build();
            detectController.deidentifyFile(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (Exception e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        }
        file.delete();
    }

    @Test
    public void testInvalidBleepFrequency() throws Exception {
        File file = File.createTempFile("test", ".txt");
        try {
            AudioBleep bleep =  AudioBleep.builder().build();
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(file)
                    .bleep(bleep)
                    .build();
            detectController.deidentifyFile(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (Exception e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        }
        file.delete();
    }

    @Test
    public void testInvalidOutputDirectory() throws Exception {
        File file = File.createTempFile("test", ".txt");
        try {
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(file)
                    .outputDirectory("not/a/real/dir")
                    .build();
            detectController.deidentifyFile(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (Exception e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        }
        file.delete();
    }

    @Test
    public void testInvalidWaitTime() throws Exception {
        File file = File.createTempFile("test", ".txt");
        try {
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(file)
                    .waitTime(-1)
                    .build();
            detectController.deidentifyFile(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (Exception e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        }
        file.delete();
    }

    @Test
    public void testWaitTimeExceedsLimit() throws Exception {
        File file = File.createTempFile("test", ".txt");
        try {
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(file)
                    .waitTime(100)
                    .build();
            detectController.deidentifyFile(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (Exception e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        }
        file.delete();
    }

    @Test
    public void testNullGetDetectRunRequest() {
        try {
            detectController.getDetectRun(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (Exception e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        }
    }

    @Test
    public void testEmptyRunIdInGetDetectRunRequest() {
        try {
            GetDetectRunRequest request =  GetDetectRunRequest.builder().build();
            detectController.getDetectRun(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (Exception e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        }
    }

    @Test
    public void testNullRunIdInGetDetectRunRequest() {
        try {
            GetDetectRunRequest request = GetDetectRunRequest.builder().runId(null).build();
            detectController.getDetectRun(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (Exception e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        }
    }
}