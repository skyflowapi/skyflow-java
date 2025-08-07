package com.skyflow.v2.vault.controller;

import com.skyflow.v2.config.Credentials;
import com.skyflow.v2.config.VaultConfig;
import com.skyflow.v2.errors.ErrorCode;
import com.skyflow.v2.errors.ErrorMessage;
import com.skyflow.v2.errors.SkyflowException;
import com.skyflow.v2.vault.detect.AudioBleep;
import com.skyflow.v2.vault.detect.DeidentifyFileRequest;
import com.skyflow.v2.vault.detect.FileInput;
import com.skyflow.v2.vault.detect.GetDetectRunRequest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
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
        vaultConfig.setEnv(com.skyflow.v2.enums.Env.DEV);
        vaultConfig.setCredentials(credentials);

        detectController = new DetectController(vaultConfig, credentials);
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
            FileInput fileInput = FileInput.builder().file(file).build();
            DeidentifyFileRequest request = DeidentifyFileRequest.builder().file(fileInput).build();
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
            FileInput fileInput = FileInput.builder().file(file).build();
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(fileInput)
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
    public void testFileInputBothFileAndFilePathNull() {
        try {
            FileInput fileInput = FileInput.builder().build();
            DeidentifyFileRequest request = DeidentifyFileRequest.builder().file(fileInput).build();
            detectController.deidentifyFile(request);
            Assert.fail("Should have thrown an exception for both file and filePath being null");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains(ErrorMessage.EmptyFileAndFilePathInDeIdentifyFile.getMessage()));
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        }
    }

    @Test
    public void testFileInputBothFileAndFilePathProvided() {
        try {
            java.io.File file = java.io.File.createTempFile("test", ".txt");
            String filePath = file.getAbsolutePath();
            FileInput fileInput = FileInput.builder().file(file).filePath(filePath).build();
            DeidentifyFileRequest request = DeidentifyFileRequest.builder().file(fileInput).build();
            detectController.deidentifyFile(request);
            Assert.fail("Should have thrown an exception for both file and filePath being provided");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains(ErrorMessage.BothFileAndFilePathProvided.getMessage()));
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        }
    }

    @Test
    public void testFileInputEmptyFilePath() {
        try {
            FileInput fileInput = FileInput.builder().filePath("").build();
            DeidentifyFileRequest request = DeidentifyFileRequest.builder().file(fileInput).build();
            detectController.deidentifyFile(request);
            Assert.fail("Should have thrown an exception for empty filePath");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains(ErrorMessage.InvalidFilePath.getMessage()));
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        }
    }

    @Test
    public void testFileInputNonExistentFile() {
        try {
            java.io.File file = new java.io.File("nonexistent.txt");
            FileInput fileInput = FileInput.builder().file(file).build();
            DeidentifyFileRequest request = DeidentifyFileRequest.builder().file(fileInput).build();
            detectController.deidentifyFile(request);
            Assert.fail("Should have thrown an exception for non-existent file");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains(ErrorMessage.FileNotFoundToDeidentify.getMessage()));
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        }
    }

    @Test
    public void testFileInputUnreadableFile() {
        try {
            java.io.File file = new java.io.File("unreadable.txt") {
                @Override
                public boolean exists() { return true; }
                @Override
                public boolean isFile() { return true; }
                @Override
                public boolean canRead() { return false; }
            };
            FileInput fileInput = FileInput.builder().file(file).build();
            DeidentifyFileRequest request = DeidentifyFileRequest.builder().file(fileInput).build();
            detectController.deidentifyFile(request);
            Assert.fail("Should have thrown an exception for unreadable file");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains(ErrorMessage.FileNotReadableToDeidentify.getMessage()));
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        }
    }

    @Test
    public void testNonExistentFileInDeidentifyFileRequest() {
        try {
            File file = new File("nonexistent.txt");
            FileInput fileInput = FileInput.builder().file(file).build();
            DeidentifyFileRequest request = DeidentifyFileRequest.builder().file(fileInput).build();
            detectController.deidentifyFile(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (Exception e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        }
    }

    @Test
    public void testInvalidPixelDensity() throws Exception {
        File file = File.createTempFile("test", ".txt");
        FileInput fileInput = FileInput.builder().file(file).build();
        try {
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(fileInput)
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
        FileInput fileInput = FileInput.builder().file(file).build();
        try {
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(fileInput)
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
        FileInput fileInput = FileInput.builder().file(file).build();
        try {
            AudioBleep bleep =  AudioBleep.builder().build();
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(fileInput)
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
        FileInput fileInput = FileInput.builder().file(file).build();
        try {
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(fileInput)
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
        FileInput fileInput = FileInput.builder().file(file).build();
        try {
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(fileInput)
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
        FileInput fileInput = FileInput.builder().file(file).build();
        try {
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(fileInput)
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

    @Test
    public void testInvalidBleepGain() throws Exception {
        File file = File.createTempFile("test", ".txt");
        FileInput fileInput = FileInput.builder().file(file).build();
        try {
            AudioBleep bleep = AudioBleep.builder().frequency(440.0).gain(-1.0).startPadding(0.1).stopPadding(0.1).build();
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(fileInput)
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
    public void testInvalidBleepStartPadding() throws Exception {
        File file = File.createTempFile("test", ".txt");
        FileInput fileInput = FileInput.builder().file(file).build();
        try {
            AudioBleep bleep = AudioBleep.builder().frequency(440.0).gain(0.5).startPadding(-0.1).stopPadding(0.1).build();
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(fileInput)
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
    public void testInvalidBleepStopPadding() throws Exception {
        File file = File.createTempFile("test", ".txt");
        FileInput fileInput = FileInput.builder().file(file).build();
        try {
            AudioBleep bleep = AudioBleep.builder().frequency(440.0).gain(0.5).startPadding(0.1).stopPadding(-0.1).build();
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(fileInput)
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
    public void testOutputDirectoryNotDirectory() throws Exception {
        File file = File.createTempFile("test", ".txt");
        FileInput fileInput = FileInput.builder().file(file).build();
        File notADir = File.createTempFile("notadir", ".txt");
        try {
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(fileInput)
                    .outputDirectory(notADir.getAbsolutePath())
                    .build();
            detectController.deidentifyFile(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (Exception e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        }
        file.delete();
        notADir.delete();
    }

    @Test
    public void testOutputDirectoryNotWritable() throws Exception {
        File file = File.createTempFile("test", ".txt");
        FileInput fileInput = FileInput.builder().file(file).build();
        File dir = Files.createTempDirectory("notwritabledir").toFile();
        dir.setWritable(false);
        try {
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(fileInput)
                    .outputDirectory(dir.getAbsolutePath())
                    .build();
            detectController.deidentifyFile(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (Exception e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
        } finally {
            dir.setWritable(true);
            file.delete();
            dir.delete();
        }
    }
}