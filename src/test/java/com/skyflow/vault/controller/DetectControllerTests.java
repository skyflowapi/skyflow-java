package com.skyflow.vault.controller;

import com.skyflow.Skyflow;
import com.skyflow.VaultClient;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.HttpStatus;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.resources.files.FilesClient;
import com.skyflow.generated.rest.resources.files.requests.GetRunRequest;
import com.skyflow.generated.rest.resources.strings.StringsClient;
import com.skyflow.generated.rest.types.DeidentifiedFileOutput;
import com.skyflow.generated.rest.types.DeidentifiedFileOutputProcessedFileExtension;
import com.skyflow.generated.rest.types.DeidentifyStringResponse;
import com.skyflow.generated.rest.types.DetectRunsResponse;
import com.skyflow.generated.rest.types.DetectRunsResponseOutputType;
import com.skyflow.generated.rest.types.DetectRunsResponseStatus;
import com.skyflow.generated.rest.types.IdentifyResponse;
import com.skyflow.generated.rest.types.WordCharacterCount;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.vault.detect.DeidentifyTextRequest;
import com.skyflow.vault.detect.DeidentifyTextResponse;
import com.skyflow.vault.detect.DeidentifyFileResponse;
import com.skyflow.vault.detect.GetDetectRunRequest;
import com.skyflow.vault.detect.ReidentifyTextRequest;
import com.skyflow.vault.detect.ReidentifyTextResponse;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Collections;

import com.skyflow.generated.rest.core.RequestOptions;
import com.skyflow.vault.detect.DeidentifyFileRequest;
import com.skyflow.vault.detect.FileInput;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class DetectControllerTests {
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static String vaultID = null;
    private static String clusterID = null;
    private static VaultConfig vaultConfig = null;
    private static Skyflow skyflowClient = null;

    @BeforeClass
    public static void setup() throws SkyflowException {
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

    // ─── helper: build a DetectController with a mocked ApiClient ─────────────

    private static DetectController createDetectControllerWithMock(ApiClient mockApiClient) throws Exception {
        Credentials creds = new Credentials();
        creds.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");

        VaultConfig config = new VaultConfig();
        config.setVaultId(vaultID);
        config.setClusterId(clusterID);
        config.setEnv(Env.DEV);

        DetectController controller = new DetectController(config, creds);
        Field f = VaultClient.class.getDeclaredField("apiClient");
        f.setAccessible(true);
        f.set(controller, mockApiClient);
        return controller;
    }

    // ─── deidentifyText — validation ──────────────────────────────────────────

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

    // ─── deidentifyText — happy path ──────────────────────────────────────────

    @Test
    public void testDeidentifyTextHappyPath() throws Exception {
        StringsClient mockStringsClient = Mockito.mock(StringsClient.class);
        ApiClient mockApiClient = Mockito.mock(ApiClient.class);
        when(mockApiClient.strings()).thenReturn(mockStringsClient);

        DeidentifyStringResponse fakeResponse = DeidentifyStringResponse.builder()
                .processedText("hello [REDACTED]")
                .wordCount(2)
                .characterCount(16)
                .build();

        when(mockStringsClient.deidentifyString(any(), any())).thenReturn(fakeResponse);

        DetectController controller = createDetectControllerWithMock(mockApiClient);
        DeidentifyTextRequest request = DeidentifyTextRequest.builder().text("hello world").build();

        try {
            DeidentifyTextResponse response = controller.deidentifyText(request);
            Assert.assertNotNull(response);
            Assert.assertEquals("hello [REDACTED]", response.getProcessedText());
            Assert.assertEquals(2, response.getWordCount());
            Assert.assertEquals(16, response.getCharCount());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN + ": " + e.getMessage());
        }
    }

    // ─── deidentifyText — API error path ──────────────────────────────────────

    @Test
    public void testDeidentifyTextApiError() throws Exception {
        StringsClient mockStringsClient = Mockito.mock(StringsClient.class);
        ApiClient mockApiClient = Mockito.mock(ApiClient.class);
        when(mockApiClient.strings()).thenReturn(mockStringsClient);

        int expectedStatusCode = 403;
        when(mockStringsClient.deidentifyString(any(), any()))
                .thenThrow(new ApiClientApiException("Forbidden", expectedStatusCode, "access denied"));

        DetectController controller = createDetectControllerWithMock(mockApiClient);
        DeidentifyTextRequest request = DeidentifyTextRequest.builder().text("hello world").build();

        try {
            controller.deidentifyText(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(expectedStatusCode, e.getHttpCode());
        }
    }

    @Test
    public void testDeidentifyTextApiError500() throws Exception {
        StringsClient mockStringsClient = Mockito.mock(StringsClient.class);
        ApiClient mockApiClient = Mockito.mock(ApiClient.class);
        when(mockApiClient.strings()).thenReturn(mockStringsClient);

        int expectedStatusCode = 500;
        when(mockStringsClient.deidentifyString(any(), any()))
                .thenThrow(new ApiClientApiException("Internal Server Error", expectedStatusCode, "server error body"));

        DetectController controller = createDetectControllerWithMock(mockApiClient);
        DeidentifyTextRequest request = DeidentifyTextRequest.builder().text("some text").build();

        try {
            controller.deidentifyText(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(expectedStatusCode, e.getHttpCode());
        }
    }

    // ─── reidentifyText — validation ──────────────────────────────────────────

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

    // ─── reidentifyText — happy path ──────────────────────────────────────────

    @Test
    public void testReidentifyTextHappyPath() throws Exception {
        StringsClient mockStringsClient = Mockito.mock(StringsClient.class);
        ApiClient mockApiClient = Mockito.mock(ApiClient.class);
        when(mockApiClient.strings()).thenReturn(mockStringsClient);

        IdentifyResponse fakeResponse = IdentifyResponse.builder().text("original text").build();
        when(mockStringsClient.reidentifyString(any(), any())).thenReturn(fakeResponse);

        DetectController controller = createDetectControllerWithMock(mockApiClient);
        ReidentifyTextRequest request = ReidentifyTextRequest.builder().text("tokenized text").build();

        try {
            ReidentifyTextResponse response = controller.reidentifyText(request);
            Assert.assertNotNull(response);
            Assert.assertEquals("original text", response.getProcessedText());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN + ": " + e.getMessage());
        }
    }

    // ─── reidentifyText — API error path ──────────────────────────────────────

    @Test
    public void testReidentifyTextApiError() throws Exception {
        StringsClient mockStringsClient = Mockito.mock(StringsClient.class);
        ApiClient mockApiClient = Mockito.mock(ApiClient.class);
        when(mockApiClient.strings()).thenReturn(mockStringsClient);

        int expectedStatusCode = 401;
        when(mockStringsClient.reidentifyString(any(), any()))
                .thenThrow(new ApiClientApiException("Unauthorized", expectedStatusCode, "unauthorized body"));

        DetectController controller = createDetectControllerWithMock(mockApiClient);
        ReidentifyTextRequest request = ReidentifyTextRequest.builder().text("some text").build();

        try {
            controller.reidentifyText(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(expectedStatusCode, e.getHttpCode());
        }
    }

    @Test
    public void testReidentifyTextApiError500() throws Exception {
        StringsClient mockStringsClient = Mockito.mock(StringsClient.class);
        ApiClient mockApiClient = Mockito.mock(ApiClient.class);
        when(mockApiClient.strings()).thenReturn(mockStringsClient);

        int expectedStatusCode = 500;
        when(mockStringsClient.reidentifyString(any(), any()))
                .thenThrow(new ApiClientApiException("Internal Server Error", expectedStatusCode, "server error body"));

        DetectController controller = createDetectControllerWithMock(mockApiClient);
        ReidentifyTextRequest request = ReidentifyTextRequest.builder().text("some text").build();

        try {
            controller.reidentifyText(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(expectedStatusCode, e.getHttpCode());
        }
    }

    // ─── getDetectRun — validation ────────────────────────────────────────────

    @Test
    public void testNullRunIdInGetDetectRunRequest() {
        try {
            GetDetectRunRequest request = GetDetectRunRequest.builder().runId(null).build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.detect(vaultID).getDetectRun(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(HttpStatus.BAD_REQUEST.getHttpStatus(), e.getHttpStatus());
        }
    }

    @Test
    public void testEmptyRunIdInGetDetectRunRequest() {
        try {
            GetDetectRunRequest request = GetDetectRunRequest.builder().runId("").build();
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.detect(vaultID).getDetectRun(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(HttpStatus.BAD_REQUEST.getHttpStatus(), e.getHttpStatus());
        }
    }

    // ─── getDetectRun — happy path (no output list) ───────────────────────────

    @Test
    public void testGetDetectRunHappyPathNoOutput() throws Exception {
        FilesClient mockFilesClient = Mockito.mock(FilesClient.class);
        ApiClient mockApiClient = Mockito.mock(ApiClient.class);
        when(mockApiClient.files()).thenReturn(mockFilesClient);

        DetectRunsResponse fakeRunsResponse = DetectRunsResponse.builder()
                .status(DetectRunsResponseStatus.SUCCESS)
                .outputType(DetectRunsResponseOutputType.BASE_64)
                .size(10.5f)
                .duration(1.2f)
                .build();

        when(mockFilesClient.getRun(anyString(), any(GetRunRequest.class)))
                .thenReturn(fakeRunsResponse);

        DetectController controller = createDetectControllerWithMock(mockApiClient);
        GetDetectRunRequest request = GetDetectRunRequest.builder().runId("run-123").build();

        try {
            DeidentifyFileResponse response = controller.getDetectRun(request);
            Assert.assertNotNull(response);
            Assert.assertEquals("run-123", response.getRunId());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN + ": " + e.getMessage());
        }
    }

    // ─── getDetectRun — API error path ────────────────────────────────────────

    @Test
    public void testGetDetectRunApiError() throws Exception {
        FilesClient mockFilesClient = Mockito.mock(FilesClient.class);
        ApiClient mockApiClient = Mockito.mock(ApiClient.class);
        when(mockApiClient.files()).thenReturn(mockFilesClient);

        int expectedStatusCode = 404;
        when(mockFilesClient.getRun(anyString(), any(GetRunRequest.class)))
                .thenThrow(new ApiClientApiException("Not Found", expectedStatusCode, "run not found"));

        DetectController controller = createDetectControllerWithMock(mockApiClient);
        GetDetectRunRequest request = GetDetectRunRequest.builder().runId("run-999").build();

        try {
            controller.getDetectRun(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(expectedStatusCode, e.getHttpCode());
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private static DetectRunsResponse buildSuccessDetectRunsResponse() {
        return DetectRunsResponse.builder()
                .status(DetectRunsResponseStatus.SUCCESS)
                .outputType(DetectRunsResponseOutputType.BASE_64)
                .size(1.0f)
                .duration(0.5f)
                .build();
    }

    private DeidentifyFileResponse runDeidentifyFileForExtension(
            String extension, FilesClient mockFilesClient) throws Exception {
        File tmpFile = File.createTempFile("test-detect", "." + extension);
        tmpFile.deleteOnExit();
        Files.write(tmpFile.toPath(), ("content for " + extension).getBytes());

        ApiClient mockApiClient = Mockito.mock(ApiClient.class);
        when(mockApiClient.files()).thenReturn(mockFilesClient);
        when(mockFilesClient.getRun(anyString(), any(GetRunRequest.class), any(RequestOptions.class)))
                .thenReturn(buildSuccessDetectRunsResponse());

        DetectController controller = createDetectControllerWithMock(mockApiClient);
        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(FileInput.builder().file(tmpFile).build())
                .build();
        return controller.deidentifyFile(request);
    }

    @Test
    public void testGetDetectRunApiError500() throws Exception {
        FilesClient mockFilesClient = Mockito.mock(FilesClient.class);
        ApiClient mockApiClient = Mockito.mock(ApiClient.class);
        when(mockApiClient.files()).thenReturn(mockFilesClient);

        int expectedStatusCode = 500;
        when(mockFilesClient.getRun(anyString(), any(GetRunRequest.class)))
                .thenThrow(new ApiClientApiException("Internal Server Error", expectedStatusCode, "internal error body"));

        DetectController controller = createDetectControllerWithMock(mockApiClient);
        GetDetectRunRequest request = GetDetectRunRequest.builder().runId("run-abc").build();

        try {
            controller.getDetectRun(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(expectedStatusCode, e.getHttpCode());
        }
    }

    // ─── deidentifyFile — validation ──────────────────────────────────────────

    @Test
    public void testDeidentifyFile_nullRequest() {
        try {
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            skyflowClient.detect(vaultID).deidentifyFile(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyRequestBody.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testDeidentifyFile_noFileOrPathProvided() {
        try {
            skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(FileInput.builder().build())
                    .build();
            skyflowClient.detect(vaultID).deidentifyFile(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyFileAndFilePathInDeIdentifyFile.getMessage(), e.getMessage());
        }
    }

    // ─── deidentifyFile — happy path ──────────────────────────────────────────

    @Test
    public void testDeidentifyFile_successWithTxtFileObject() throws Exception {
        File tmpFile = File.createTempFile("test-detect", ".txt");
        tmpFile.deleteOnExit();
        Files.write(tmpFile.toPath(), "hello world".getBytes());

        FilesClient mockFilesClient = Mockito.mock(FilesClient.class);
        ApiClient mockApiClient = Mockito.mock(ApiClient.class);
        when(mockApiClient.files()).thenReturn(mockFilesClient);
        when(mockFilesClient.deidentifyText(any())).thenReturn(
                com.skyflow.generated.rest.types.DeidentifyFileResponse.builder().runId("run-txt-001").build());
        when(mockFilesClient.getRun(anyString(), any(GetRunRequest.class), any(RequestOptions.class)))
                .thenReturn(buildSuccessDetectRunsResponse());

        DetectController controller = createDetectControllerWithMock(mockApiClient);
        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(FileInput.builder().file(tmpFile).build()).build();

        DeidentifyFileResponse response = controller.deidentifyFile(request);
        Assert.assertNotNull(response);
    }

    @Test
    public void testDeidentifyFile_successWithFilePath() throws Exception {
        File tmpFile = File.createTempFile("test-detect-path", ".txt");
        tmpFile.deleteOnExit();
        Files.write(tmpFile.toPath(), "content".getBytes());

        FilesClient mockFilesClient = Mockito.mock(FilesClient.class);
        ApiClient mockApiClient = Mockito.mock(ApiClient.class);
        when(mockApiClient.files()).thenReturn(mockFilesClient);
        when(mockFilesClient.deidentifyText(any())).thenReturn(
                com.skyflow.generated.rest.types.DeidentifyFileResponse.builder().runId("run-path-001").build());
        when(mockFilesClient.getRun(anyString(), any(GetRunRequest.class), any(RequestOptions.class)))
                .thenReturn(buildSuccessDetectRunsResponse());

        DetectController controller = createDetectControllerWithMock(mockApiClient);
        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(FileInput.builder().filePath(tmpFile.getAbsolutePath()).build()).build();

        DeidentifyFileResponse response = controller.deidentifyFile(request);
        Assert.assertNotNull(response);
    }

    @Test
    public void testDeidentifyFile_successWithOutputFile() throws Exception {
        File tmpFile = File.createTempFile("test-detect-out", ".txt");
        tmpFile.deleteOnExit();
        Files.write(tmpFile.toPath(), "content".getBytes());

        String b64 = Base64.getEncoder().encodeToString("processed content".getBytes());
        DeidentifiedFileOutput outputItem = DeidentifiedFileOutput.builder()
                .processedFile(b64)
                .processedFileExtension(DeidentifiedFileOutputProcessedFileExtension.TXT)
                .build();
        DetectRunsResponse successWithOutput = DetectRunsResponse.builder()
                .status(DetectRunsResponseStatus.SUCCESS)
                .outputType(DetectRunsResponseOutputType.BASE_64)
                .size(1.0f).duration(0.5f)
                .output(Collections.singletonList(outputItem))
                .build();

        FilesClient mockFilesClient = Mockito.mock(FilesClient.class);
        ApiClient mockApiClient = Mockito.mock(ApiClient.class);
        when(mockApiClient.files()).thenReturn(mockFilesClient);
        when(mockFilesClient.deidentifyText(any())).thenReturn(
                com.skyflow.generated.rest.types.DeidentifyFileResponse.builder().runId("run-out-001").build());
        when(mockFilesClient.getRun(anyString(), any(GetRunRequest.class), any(RequestOptions.class)))
                .thenReturn(successWithOutput);

        DetectController controller = createDetectControllerWithMock(mockApiClient);
        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(FileInput.builder().file(tmpFile).build())
                .outputDirectory(System.getProperty("java.io.tmpdir"))
                .build();

        DeidentifyFileResponse response = controller.deidentifyFile(request);
        Assert.assertNotNull(response);
    }

    // ─── deidentifyFile — IN_PROGRESS timeout ─────────────────────────────────

    @Test
    public void testDeidentifyFile_inProgressTimeout() throws Exception {
        File tmpFile = File.createTempFile("test-detect-prog", ".txt");
        tmpFile.deleteOnExit();
        Files.write(tmpFile.toPath(), "content".getBytes());

        FilesClient mockFilesClient = Mockito.mock(FilesClient.class);
        ApiClient mockApiClient = Mockito.mock(ApiClient.class);
        when(mockApiClient.files()).thenReturn(mockFilesClient);
        when(mockFilesClient.deidentifyText(any())).thenReturn(
                com.skyflow.generated.rest.types.DeidentifyFileResponse.builder().runId("run-prog-001").build());
        when(mockFilesClient.getRun(anyString(), any(GetRunRequest.class), any(RequestOptions.class)))
                .thenReturn(DetectRunsResponse.builder().status(DetectRunsResponseStatus.IN_PROGRESS).build());

        DetectController controller = createDetectControllerWithMock(mockApiClient);
        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(FileInput.builder().file(tmpFile).build())
                .waitTime(1)
                .build();

        DeidentifyFileResponse response = controller.deidentifyFile(request);
        Assert.assertNotNull(response);
        Assert.assertEquals("IN_PROGRESS", response.getStatus());
    }

    // ─── deidentifyFile — error paths ─────────────────────────────────────────

    @Test
    public void testDeidentifyFile_nonExistentFilePath() {
        try {
            DetectController controller = createDetectControllerWithMock(Mockito.mock(ApiClient.class));
            DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                    .file(FileInput.builder().filePath("/nonexistent/path/file.txt").build())
                    .build();
            controller.deidentifyFile(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (Exception e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testDeidentifyFile_processFileApiError() throws Exception {
        File tmpFile = File.createTempFile("test-detect-err", ".txt");
        tmpFile.deleteOnExit();
        Files.write(tmpFile.toPath(), "content".getBytes());

        FilesClient mockFilesClient = Mockito.mock(FilesClient.class);
        ApiClient mockApiClient = Mockito.mock(ApiClient.class);
        when(mockApiClient.files()).thenReturn(mockFilesClient);
        when(mockFilesClient.deidentifyText(any()))
                .thenThrow(new ApiClientApiException("forbidden", 403, "access denied"));

        DetectController controller = createDetectControllerWithMock(mockApiClient);
        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(FileInput.builder().file(tmpFile).build()).build();

        try {
            controller.deidentifyFile(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(403, e.getHttpCode());
        }
    }

    @Test
    public void testDeidentifyFile_pollForResultsApiError() throws Exception {
        File tmpFile = File.createTempFile("test-detect-poll", ".txt");
        tmpFile.deleteOnExit();
        Files.write(tmpFile.toPath(), "content".getBytes());

        FilesClient mockFilesClient = Mockito.mock(FilesClient.class);
        ApiClient mockApiClient = Mockito.mock(ApiClient.class);
        when(mockApiClient.files()).thenReturn(mockFilesClient);
        when(mockFilesClient.deidentifyText(any())).thenReturn(
                com.skyflow.generated.rest.types.DeidentifyFileResponse.builder().runId("run-poll-err").build());
        when(mockFilesClient.getRun(anyString(), any(GetRunRequest.class), any(RequestOptions.class)))
                .thenThrow(new ApiClientApiException("unavailable", 503, "service unavailable"));

        DetectController controller = createDetectControllerWithMock(mockApiClient);
        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(FileInput.builder().file(tmpFile).build()).build();

        try {
            controller.deidentifyFile(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.PollingForResultsFailed.getMessage(), e.getMessage());
        }
    }

    // ─── processFileByType — all extensions ───────────────────────────────────

    @Test
    public void testDeidentifyFile_pdfExtension() throws Exception {
        FilesClient mockFilesClient = Mockito.mock(FilesClient.class);
        when(mockFilesClient.deidentifyPdf(any())).thenReturn(
                com.skyflow.generated.rest.types.DeidentifyFileResponse.builder().runId("run-pdf").build());
        Assert.assertNotNull(runDeidentifyFileForExtension("pdf", mockFilesClient));
    }

    @Test
    public void testDeidentifyFile_mp3Extension() throws Exception {
        FilesClient mockFilesClient = Mockito.mock(FilesClient.class);
        when(mockFilesClient.deidentifyAudio(any())).thenReturn(
                com.skyflow.generated.rest.types.DeidentifyFileResponse.builder().runId("run-mp3").build());
        Assert.assertNotNull(runDeidentifyFileForExtension("mp3", mockFilesClient));
    }

    @Test
    public void testDeidentifyFile_jpgExtension() throws Exception {
        FilesClient mockFilesClient = Mockito.mock(FilesClient.class);
        when(mockFilesClient.deidentifyImage(any())).thenReturn(
                com.skyflow.generated.rest.types.DeidentifyFileResponse.builder().runId("run-jpg").build());
        Assert.assertNotNull(runDeidentifyFileForExtension("jpg", mockFilesClient));
    }

    @Test
    public void testDeidentifyFile_pptExtension() throws Exception {
        FilesClient mockFilesClient = Mockito.mock(FilesClient.class);
        when(mockFilesClient.deidentifyPresentation(any())).thenReturn(
                com.skyflow.generated.rest.types.DeidentifyFileResponse.builder().runId("run-ppt").build());
        Assert.assertNotNull(runDeidentifyFileForExtension("ppt", mockFilesClient));
    }

    @Test
    public void testDeidentifyFile_csvExtension() throws Exception {
        FilesClient mockFilesClient = Mockito.mock(FilesClient.class);
        when(mockFilesClient.deidentifySpreadsheet(any())).thenReturn(
                com.skyflow.generated.rest.types.DeidentifyFileResponse.builder().runId("run-csv").build());
        Assert.assertNotNull(runDeidentifyFileForExtension("csv", mockFilesClient));
    }

    @Test
    public void testDeidentifyFile_docExtension() throws Exception {
        FilesClient mockFilesClient = Mockito.mock(FilesClient.class);
        when(mockFilesClient.deidentifyDocument(any())).thenReturn(
                com.skyflow.generated.rest.types.DeidentifyFileResponse.builder().runId("run-doc").build());
        Assert.assertNotNull(runDeidentifyFileForExtension("doc", mockFilesClient));
    }

    @Test
    public void testDeidentifyFile_jsonExtension() throws Exception {
        FilesClient mockFilesClient = Mockito.mock(FilesClient.class);
        when(mockFilesClient.deidentifyStructuredText(any())).thenReturn(
                com.skyflow.generated.rest.types.DeidentifyFileResponse.builder().runId("run-json").build());
        Assert.assertNotNull(runDeidentifyFileForExtension("json", mockFilesClient));
    }

    @Test
    public void testDeidentifyFile_defaultExtension() throws Exception {
        FilesClient mockFilesClient = Mockito.mock(FilesClient.class);
        when(mockFilesClient.deidentifyFile(any())).thenReturn(
                com.skyflow.generated.rest.types.DeidentifyFileResponse.builder().runId("run-dcm").build());
        Assert.assertNotNull(runDeidentifyFileForExtension("dcm", mockFilesClient));
    }

    // ─── parseDeidentifyFileResponse — wordCharacterCount branch L272-273 ─────

    @Test
    public void testGetDetectRun_withWordCharacterCount() throws Exception {
        FilesClient mockFilesClient = Mockito.mock(FilesClient.class);
        ApiClient mockApiClient = Mockito.mock(ApiClient.class);
        when(mockApiClient.files()).thenReturn(mockFilesClient);

        DeidentifiedFileOutput outputItem = DeidentifiedFileOutput.builder().build();
        WordCharacterCount wordCharCount = WordCharacterCount.builder()
                .wordCount(10)
                .characterCount(55)
                .build();

        DetectRunsResponse fakeRunsResponse = DetectRunsResponse.builder()
                .status(DetectRunsResponseStatus.SUCCESS)
                .outputType(DetectRunsResponseOutputType.BASE_64)
                .size(5.0f)
                .duration(0.5f)
                .output(Collections.singletonList(outputItem))
                .wordCharacterCount(wordCharCount)
                .build();

        when(mockFilesClient.getRun(anyString(), any(GetRunRequest.class)))
                .thenReturn(fakeRunsResponse);

        DetectController controller = createDetectControllerWithMock(mockApiClient);
        GetDetectRunRequest request = GetDetectRunRequest.builder().runId("run-wc-001").build();

        try {
            DeidentifyFileResponse response = controller.getDetectRun(request);
            Assert.assertNotNull(response);
            Assert.assertEquals(10, (int) response.getWordCount());
            Assert.assertEquals(55, (int) response.getCharCount());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN + ": " + e.getMessage());
        }
    }

    // ─── parseDeidentifyFileResponse — processedFile present branch L283-291 ──

    @Test
    public void testGetDetectRun_withProcessedFile() throws Exception {
        FilesClient mockFilesClient = Mockito.mock(FilesClient.class);
        ApiClient mockApiClient = Mockito.mock(ApiClient.class);
        when(mockApiClient.files()).thenReturn(mockFilesClient);

        String base64Content = Base64.getEncoder().encodeToString("test file content".getBytes());
        DeidentifiedFileOutput outputItem = DeidentifiedFileOutput.builder()
                .processedFile(base64Content)
                .processedFileExtension(DeidentifiedFileOutputProcessedFileExtension.TXT)
                .build();

        DetectRunsResponse fakeRunsResponse = DetectRunsResponse.builder()
                .status(DetectRunsResponseStatus.SUCCESS)
                .outputType(DetectRunsResponseOutputType.BASE_64)
                .size(1.0f)
                .duration(0.1f)
                .output(Collections.singletonList(outputItem))
                .build();

        when(mockFilesClient.getRun(anyString(), any(GetRunRequest.class)))
                .thenReturn(fakeRunsResponse);

        DetectController controller = createDetectControllerWithMock(mockApiClient);
        GetDetectRunRequest request = GetDetectRunRequest.builder().runId("run-file-001").build();

        try {
            DeidentifyFileResponse response = controller.getDetectRun(request);
            Assert.assertNotNull(response);
            Assert.assertEquals("run-file-001", response.getRunId());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN + ": " + e.getMessage());
        }
    }
}
