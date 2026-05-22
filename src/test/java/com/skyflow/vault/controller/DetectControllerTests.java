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

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Collections;

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
