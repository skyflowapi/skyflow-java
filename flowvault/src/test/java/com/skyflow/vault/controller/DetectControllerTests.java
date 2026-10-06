package com.skyflow.vault.controller;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.detect.CheckGuardrailsRequest;
import com.skyflow.detect.CheckGuardrailsResponse;
import com.skyflow.detect.DataSourceType;
import com.skyflow.detect.DeidentificationType;
import com.skyflow.detect.DeidentifyFileRequest;
import com.skyflow.detect.DeidentifyFileResponse;
import com.skyflow.detect.DeidentifyStringRequest;
import com.skyflow.detect.DeidentifyStringResponse;
import com.skyflow.detect.Detect;
import com.skyflow.detect.DetectConfiguration;
import com.skyflow.detect.DetectRunStatus;
import com.skyflow.detect.Entity;
import com.skyflow.detect.EntityType;
import com.skyflow.detect.FileDataFormat;
import com.skyflow.detect.GetRunRequest;
import com.skyflow.detect.GetRunResponse;
import com.skyflow.detect.GuardrailsValidation;
import com.skyflow.detect.PollOptions;
import com.skyflow.detect.RedactionLevel;
import com.skyflow.detect.ReidentifyFileRequest;
import com.skyflow.detect.ReidentifyFileResponse;
import com.skyflow.detect.ReidentifyStringRequest;
import com.skyflow.detect.ReidentifyStringResponse;
import com.skyflow.enums.Env;
import com.skyflow.errors.BaseErrorMessage;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

import java.util.Arrays;
import java.util.Collections;

public class DetectControllerTests {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static VaultConfig vaultConfig(String vaultId) {
        VaultConfig config = new VaultConfig();
        config.setVaultId(vaultId);
        config.setClusterId("cluster123");
        config.setEnv(Env.PROD);
        return config;
    }

    @Test
    public void detectAccessorResolvesConfiguredVaults() throws SkyflowException {
        Skyflow client = Skyflow.builder()
                .addVaultConfig(vaultConfig("vault-a"))
                .addVaultConfig(vaultConfig("vault-b"))
                .build();
        Assert.assertNotNull(client.detect());
        Assert.assertSame(client.detect(), client.detect("vault-a"));
        Assert.assertNotSame(client.detect("vault-a"), client.detect("vault-b"));
        try {
            client.detect("vault-c");
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertEquals(BaseErrorMessage.VaultIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void detectAccessorFollowsVaultConfigRemoval() throws SkyflowException {
        Skyflow client = Skyflow.builder().addVaultConfig(vaultConfig("vault-a")).build();
        client.removeVaultConfig("vault-a");
        try {
            client.detect();
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertEquals(BaseErrorMessage.VaultIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void deidentifyStringValidatesBeforeResolvingCredentials() throws SkyflowException {
        DetectController controller = new DetectController(vaultConfig("vault-a"), (Credentials) null);
        try {
            controller.deidentifyString(DeidentifyStringRequest.builder().text(" ").configurationId("cfg").build());
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(BaseErrorMessage.InvalidTextInDeidentifyString.getMessage(), e.getMessage());
        }
    }

    @Test
    public void reidentifyStringValidatesBeforeResolvingCredentials() throws SkyflowException {
        DetectController controller = new DetectController(vaultConfig("vault-a"), (Credentials) null);
        try {
            controller.reidentifyString(ReidentifyStringRequest.builder().text(" ").build());
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(BaseErrorMessage.InvalidTextInReidentifyString.getMessage(), e.getMessage());
        }
    }

    @Test
    public void missingCredentialsSurfaceAsSkyflowExceptionWithoutNetworkCall() throws SkyflowException {
        DetectController controller = new DetectController(vaultConfig("vault-a"), (Credentials) null);
        try {
            controller.reidentifyString(ReidentifyStringRequest.builder().text("[NAME_1]").build());
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertEquals(BaseErrorMessage.EmptyCredentials.getMessage(), e.getMessage());
        }
    }

    // ─── reidentifyFile (flowvault only) ──────────────────────────────────────

    /** API-key credentials resolve without a token exchange, so the only network call is the Detect request. */
    private static Credentials apiKeyCredentials() {
        Credentials credentials = new Credentials();
        credentials.setApiKey("sky-abcde-0123456789abcdef0123456789abcdef");
        return credentials;
    }

    private static DetectController controllerFor(MockWebServer server) {
        VaultConfig config = vaultConfig("vault-1");
        config.setVaultUrl(server.url("").toString());
        DetectController controller = new DetectController(config, apiKeyCredentials());
        controller.setCommonHttpConfig(null, 0, null, null);
        return controller;
    }

    @Test
    public void reidentifyFileValidatesBeforeResolvingCredentials() throws SkyflowException {
        DetectController controller = new DetectController(vaultConfig("vault-a"), (Credentials) null);
        ReidentifyFileRequest[] bad = {
                null,
                ReidentifyFileRequest.builder().value("Zm9v").build(),
                ReidentifyFileRequest.builder().dataSource(DataSourceType.BASE64).value(" ").build(),
        };
        ErrorMessage[] expected = {
                ErrorMessage.ReidentifyFileRequestNull,
                ErrorMessage.InvalidDataSourceInReidentifyFile,
                ErrorMessage.InvalidValueInReidentifyFile,
        };
        for (int i = 0; i < bad.length; i++) {
            try {
                controller.reidentifyFile(bad[i]);
                Assert.fail("expected SkyflowException for case " + i);
            } catch (SkyflowException e) {
                Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
                Assert.assertEquals(expected[i].getMessage(), e.getMessage());
            }
        }
    }

    @Test
    public void reidentifyFileMissingCredentialsSurfaceWithoutNetworkCall() throws SkyflowException {
        DetectController controller = new DetectController(vaultConfig("vault-a"), (Credentials) null);
        try {
            controller.reidentifyFile(ReidentifyFileRequest.builder()
                    .dataSource(DataSourceType.BASE64).value("Zm9v").build());
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertEquals(BaseErrorMessage.EmptyCredentials.getMessage(), e.getMessage());
        }
    }

    @Test
    public void reidentifyFileSendsScopedRequestAndMapsResponse() throws Exception {
        MockWebServer server = new MockWebServer();
        server.start();
        try {
            server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json")
                    .setBody("{\"status\":\"SUCCESS\",\"outputType\":\"BASE64\","
                            + "\"output\":[{\"processedFile\":\"SGVsbG8=\",\"processedFileType\":\"REIDENTIFIED_FILE\","
                            + "\"processedFileExtension\":\"txt\"}],"
                            + "\"metrics\":{\"size\":0.5,\"wordCount\":3,\"characterCount\":12}}"));

            ReidentifyFileResponse response = controllerFor(server).reidentifyFile(ReidentifyFileRequest.builder()
                    .dataSource(DataSourceType.BASE64)
                    .value("W05BTUVfMV0=")
                    .dataFormat(FileDataFormat.TXT)
                    .redactionLevel(Arrays.asList(
                            RedactionLevel.builder().tokenGroupName("names").redactionPattern("mask").build()))
                    .build());

            Assert.assertEquals(DetectRunStatus.SUCCESS, response.getStatus());
            Assert.assertEquals(DataSourceType.BASE64, response.getOutputType());
            Assert.assertEquals(1, response.getOutput().size());
            Assert.assertEquals("SGVsbG8=", response.getOutput().get(0).getProcessedFile());
            Assert.assertEquals("REIDENTIFIED_FILE", response.getOutput().get(0).getProcessedFileType());
            Assert.assertEquals(FileDataFormat.TXT, response.getOutput().get(0).getProcessedFileExtension());
            Assert.assertEquals(Integer.valueOf(3), response.getMetrics().getWordCount());

            RecordedRequest recorded = server.takeRequest();
            Assert.assertEquals("POST", recorded.getMethod());
            Assert.assertEquals("/v2/detect/reidentify/file", recorded.getPath());
            Assert.assertEquals("Bearer sky-abcde-0123456789abcdef0123456789abcdef", recorded.getHeader("Authorization"));
            Assert.assertNotNull(recorded.getHeader("sky-metadata"));
            String body = recorded.getBody().readUtf8();
            Assert.assertTrue(body, body.contains("\"dataSource\":\"BASE64\""));
            Assert.assertTrue(body, body.contains("\"value\":\"W05BTUVfMV0=\""));
            Assert.assertTrue(body, body.contains("\"vaultId\":\"vault-1\""));
            Assert.assertTrue(body, body.contains("\"dataFormat\":\"txt\""));
            Assert.assertTrue(body, body.contains("\"tokenGroupName\":\"names\""));
            Assert.assertTrue(body, body.contains("\"redactionPattern\":\"mask\""));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void reidentifyFileToleratesSparseAndUnknownResponseValues() throws Exception {
        MockWebServer server = new MockWebServer();
        server.start();
        try {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"status\":\"SOMETHING_NEW\"}"));
            ReidentifyFileResponse response = controllerFor(server).reidentifyFile(ReidentifyFileRequest.builder()
                    .dataSource(DataSourceType.SKYFLOW_ID).value("rec-1").build());
            Assert.assertEquals(DetectRunStatus.UNKNOWN, response.getStatus());
            Assert.assertNull(response.getOutputType());
            Assert.assertTrue(response.getOutput().isEmpty());
            Assert.assertNull(response.getMetrics());
            String body = server.takeRequest().getBody().readUtf8();
            Assert.assertFalse(body, body.contains("dataFormat"));
            Assert.assertFalse(body, body.contains("redactionLevel"));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void reidentifyFileMapsApiErrors() throws Exception {
        MockWebServer server = new MockWebServer();
        server.start();
        try {
            server.enqueue(new MockResponse().setResponseCode(400).setHeader("x-request-id", "req-9")
                    .setBody("{\"error\":{\"grpc_code\":3,\"http_code\":400,\"message\":\"unsupported format\","
                            + "\"http_status\":\"Bad Request\",\"details\":[]}}"));
            try {
                controllerFor(server).reidentifyFile(ReidentifyFileRequest.builder()
                        .dataSource(DataSourceType.PRESIGNED_URL).value("https://example.com/f").build());
                Assert.fail("expected SkyflowException");
            } catch (SkyflowException e) {
                Assert.assertEquals(400, e.getHttpCode());
                Assert.assertEquals("unsupported format", e.getMessage());
                Assert.assertEquals("req-9", e.getRequestId());
                Assert.assertEquals(Integer.valueOf(3), e.getGrpcCode());
            }
        } finally {
            server.shutdown();
        }
    }

    // ─── inherited Detect V2 methods, exercised through the flowvault controller ──
    // These prove the flowvault wiring (vault URL from VaultConfig, API-key credentials,
    // flowvault SDK metrics header) end to end, not just the shared base logic.

    private static final String API_KEY = "sky-abcde-0123456789abcdef0123456789abcdef";

    private static void assertCommonHeaders(RecordedRequest recorded, String method, String path) {
        Assert.assertEquals(method, recorded.getMethod());
        Assert.assertEquals(path, recorded.getPath());
        Assert.assertEquals("Bearer " + API_KEY, recorded.getHeader("Authorization"));
        String metrics = recorded.getHeader("sky-metadata");
        Assert.assertNotNull(metrics);
        Assert.assertTrue(metrics, metrics.contains("skyflow-flowvault-java@"));
    }

    @Test
    public void deidentifyStringRoundTripWithStoredConfiguration() throws Exception {
        MockWebServer server = new MockWebServer();
        server.start();
        try {
            server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json")
                    .setBody("{\"processedText\":\"My name is [NAME_1].\","
                            + "\"entities\":[{\"token\":\"NAME_1\",\"value\":\"John Doe\",\"entityType\":\"NAME\","
                            + "\"location\":{\"startIndex\":11,\"endIndex\":19,\"startIndexProcessed\":11,\"endIndexProcessed\":19},"
                            + "\"entityScores\":{\"NAME\":0.98}}],"
                            + "\"metrics\":{\"wordCount\":4,\"characterCount\":20}}"));

            DeidentifyStringResponse response = controllerFor(server).deidentifyString(DeidentifyStringRequest.builder()
                    .text("My name is John Doe.").configurationId("cfg-123").build());

            Assert.assertEquals("My name is [NAME_1].", response.getProcessedText());
            Assert.assertEquals(1, response.getEntities().size());
            Assert.assertEquals("NAME_1", response.getEntities().get(0).getToken());
            Assert.assertEquals("John Doe", response.getEntities().get(0).getValue());
            Assert.assertEquals("NAME", response.getEntities().get(0).getEntityType());
            Assert.assertEquals(Integer.valueOf(11), response.getEntities().get(0).getLocation().getStartIndex());
            Assert.assertEquals(Integer.valueOf(19), response.getEntities().get(0).getLocation().getEndIndexProcessed());
            Assert.assertEquals(0.98, response.getEntities().get(0).getEntityScores().get("NAME"), 1e-9);
            Assert.assertEquals(Integer.valueOf(4), response.getMetrics().getWordCount());

            RecordedRequest recorded = server.takeRequest();
            assertCommonHeaders(recorded, "POST", "/v2/detect/deidentify/string");
            String body = recorded.getBody().readUtf8();
            Assert.assertTrue(body, body.contains("\"text\":\"My name is John Doe.\""));
            Assert.assertTrue(body, body.contains("\"configurationId\":\"cfg-123\""));
            Assert.assertFalse(body, body.contains("\"configuration\":"));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void deidentifyStringRoundTripWithInlineConfigurationScopedToVault() throws Exception {
        MockWebServer server = new MockWebServer();
        server.start();
        try {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"processedText\":\"[NAME_1]\"}"));

            DeidentifyStringResponse response = controllerFor(server).deidentifyString(DeidentifyStringRequest.builder()
                    .text("John")
                    .configuration(DetectConfiguration.builder()
                            .detect(Detect.builder().entities(Collections.singletonList(
                                    Entity.builder().entityType(EntityType.NAME)
                                            .deidentificationType(DeidentificationType.VAULT_TOKEN)
                                            .destination("names").build())).build())
                            .build())
                    .build());

            Assert.assertEquals("[NAME_1]", response.getProcessedText());
            Assert.assertTrue(response.getEntities().isEmpty());
            Assert.assertNull(response.getMetrics());

            String body = server.takeRequest().getBody().readUtf8();
            Assert.assertTrue(body, body.contains("\"configuration\":{"));
            Assert.assertTrue(body, body.contains("\"vaultId\":\"vault-1\""));   // injected by the controller
            Assert.assertTrue(body, body.contains("\"entityType\":\"NAME\""));
            Assert.assertTrue(body, body.contains("\"deidentificationType\":\"VAULT_TOKEN\""));
            Assert.assertTrue(body, body.contains("\"destination\":\"names\""));
            Assert.assertFalse(body, body.contains("configurationId"));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void reidentifyStringRoundTrip() throws Exception {
        MockWebServer server = new MockWebServer();
        server.start();
        try {
            server.enqueue(new MockResponse().setResponseCode(200)
                    .setBody("{\"processedText\":\"My name is J**n D*e.\",\"metrics\":{\"wordCount\":5}}"));

            ReidentifyStringResponse response = controllerFor(server).reidentifyString(ReidentifyStringRequest.builder()
                    .text("My name is [NAME_1].")
                    .redactionLevel(Collections.singletonList(
                            RedactionLevel.builder().tokenGroupName("names").redactionPattern("mask").build()))
                    .build());

            Assert.assertEquals("My name is J**n D*e.", response.getProcessedText());
            Assert.assertEquals(Integer.valueOf(5), response.getMetrics().getWordCount());

            RecordedRequest recorded = server.takeRequest();
            assertCommonHeaders(recorded, "POST", "/v2/detect/reidentify/string");
            String body = recorded.getBody().readUtf8();
            Assert.assertTrue(body, body.contains("\"text\":\"My name is [NAME_1].\""));
            Assert.assertTrue(body, body.contains("\"vaultId\":\"vault-1\""));
            Assert.assertTrue(body, body.contains("\"tokenGroupName\":\"names\""));
            Assert.assertTrue(body, body.contains("\"redactionPattern\":\"mask\""));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void getRunRoundTripQueriesByIdScopedToVault() throws Exception {
        MockWebServer server = new MockWebServer();
        server.start();
        try {
            server.enqueue(new MockResponse().setResponseCode(200)
                    .setBody("{\"status\":\"SUCCESS\",\"outputType\":\"BASE64\","
                            + "\"output\":[{\"processedFile\":\"UERG\",\"processedFileType\":\"REDACTED_FILE\",\"processedFileExtension\":\"pdf\"}],"
                            + "\"metrics\":{\"pages\":3}}"));
            server.enqueue(new MockResponse().setResponseCode(200)
                    .setBody("{\"status\":\"FAILED\",\"message\":\"unsupported file\"}"));

            DetectController controller = controllerFor(server);
            GetRunResponse done = controller.getRun(GetRunRequest.builder().runId(" run-42 ").build());
            Assert.assertEquals("run-42", done.getRunId());
            Assert.assertEquals(DetectRunStatus.SUCCESS, done.getStatus());
            Assert.assertEquals(DataSourceType.BASE64, done.getOutputType());
            Assert.assertEquals("UERG", done.getOutput().get(0).getProcessedFile());
            Assert.assertEquals("REDACTED_FILE", done.getOutput().get(0).getProcessedFileType());
            Assert.assertEquals(FileDataFormat.PDF, done.getOutput().get(0).getProcessedFileExtension());
            Assert.assertEquals(Integer.valueOf(3), done.getMetrics().getPages());
            Assert.assertNull(done.getMessage());

            RecordedRequest recorded = server.takeRequest();
            assertCommonHeaders(recorded, "GET", "/v2/detect/runs/run-42?vaultId=vault-1");
            Assert.assertEquals(0L, recorded.getBodySize());

            GetRunResponse failed = controller.getRun(GetRunRequest.builder().runId("run-43").build());
            Assert.assertEquals(DetectRunStatus.FAILED, failed.getStatus());
            Assert.assertEquals("unsupported file", failed.getMessage());
            Assert.assertTrue(failed.getOutput().isEmpty());
            Assert.assertNull(failed.getOutputType());
            Assert.assertEquals("/v2/detect/runs/run-43?vaultId=vault-1", server.takeRequest().getPath());
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void checkGuardrailsRoundTrip() throws Exception {
        MockWebServer server = new MockWebServer();
        server.start();
        try {
            server.enqueue(new MockResponse().setResponseCode(200)
                    .setBody("{\"text\":\"tell me about politics\",\"validation\":\"FAILED\",\"toxic\":false,\"deniedTopic\":true}"));
            server.enqueue(new MockResponse().setResponseCode(200)
                    .setBody("{\"text\":\"hello\",\"validation\":\"PASSED\"}"));

            DetectController controller = controllerFor(server);
            CheckGuardrailsResponse blocked = controller.checkGuardrails(CheckGuardrailsRequest.builder()
                    .text("tell me about politics").checkToxicity(true).denyTopics(Arrays.asList("politics", "religion")).build());
            Assert.assertEquals("tell me about politics", blocked.getText());
            Assert.assertEquals(GuardrailsValidation.FAILED, blocked.getValidation());
            Assert.assertEquals(Boolean.FALSE, blocked.getToxic());
            Assert.assertEquals(Boolean.TRUE, blocked.getDeniedTopic());

            RecordedRequest recorded = server.takeRequest();
            assertCommonHeaders(recorded, "POST", "/v2/detect/guardrails");
            String body = recorded.getBody().readUtf8();
            Assert.assertTrue(body, body.contains("\"text\":\"tell me about politics\""));
            Assert.assertTrue(body, body.contains("\"vaultId\":\"vault-1\""));
            Assert.assertTrue(body, body.contains("\"checkToxicity\":true"));
            Assert.assertTrue(body, body.contains("\"denyTopics\":[\"politics\",\"religion\"]"));

            CheckGuardrailsResponse passed = controller.checkGuardrails(CheckGuardrailsRequest.builder().text("hello").build());
            Assert.assertEquals(GuardrailsValidation.PASSED, passed.getValidation());
            Assert.assertNull(passed.getToxic());
            Assert.assertNull(passed.getDeniedTopic());
            String minimal = server.takeRequest().getBody().readUtf8();
            Assert.assertFalse(minimal, minimal.contains("checkToxicity"));
            Assert.assertFalse(minimal, minimal.contains("denyTopics"));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void apiErrorsOnInheritedMethodsMapToSkyflowException() throws Exception {
        MockWebServer server = new MockWebServer();
        server.start();
        try {
            server.enqueue(new MockResponse().setResponseCode(404).setHeader("x-request-id", "req-404")
                    .setBody("{\"error\":{\"grpc_code\":5,\"http_code\":404,\"message\":\"run not found\",\"http_status\":\"Not Found\",\"details\":[]}}"));
            try {
                controllerFor(server).getRun(GetRunRequest.builder().runId("missing").build());
                Assert.fail("expected SkyflowException");
            } catch (SkyflowException e) {
                Assert.assertEquals(404, e.getHttpCode());
                Assert.assertEquals("run not found", e.getMessage());
                Assert.assertEquals("req-404", e.getRequestId());
                Assert.assertEquals(Integer.valueOf(5), e.getGrpcCode());
            }
        } finally {
            server.shutdown();
        }
    }

    // ─── deidentifyFile (flowvault only) ──────────────────────────────────────

    @Test
    public void deidentifyFileValidatesBeforeResolvingCredentials() throws SkyflowException {
        DetectController controller = new DetectController(vaultConfig("vault-a"), (Credentials) null);
        try {
            controller.deidentifyFile(DeidentifyFileRequest.builder().value("Zm9v").build());
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(BaseErrorMessage.InvalidDataSourceInDeidentifyFile.getMessage(), e.getMessage());
        }
    }

    @Test
    public void deidentifyFileSubmitOnlyReturnsRunIdWithoutPolling() throws Exception {
        MockWebServer server = new MockWebServer();
        server.start();
        try {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"runId\":\"run-42\"}"));

            DeidentifyFileResponse response = controllerFor(server).deidentifyFile(DeidentifyFileRequest.builder()
                    .dataSource(DataSourceType.SKYFLOW_ID).value("rec-1").dataFormat(FileDataFormat.PDF)
                    .configurationId("cfg-1").build());

            Assert.assertEquals("run-42", response.getRunId());
            Assert.assertNull(response.getStatus());
            Assert.assertTrue(response.getOutput().isEmpty());

            RecordedRequest recorded = server.takeRequest();
            assertCommonHeaders(recorded, "POST", "/v2/detect/deidentify/file");
            String body = recorded.getBody().readUtf8();
            Assert.assertTrue(body, body.contains("\"dataSource\":\"SKYFLOW_ID\""));
            Assert.assertTrue(body, body.contains("\"value\":\"rec-1\""));
            Assert.assertTrue(body, body.contains("\"dataFormat\":\"pdf\""));
            Assert.assertTrue(body, body.contains("\"configurationId\":\"cfg-1\""));
            Assert.assertEquals("only the submit call was made", 1, server.getRequestCount());
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void deidentifyFilePollsToSuccessAndWritesOutputsForLocalFile() throws Exception {
        File input = folder.newFile("invoice.txt");
        Files.write(input.toPath(), "Pay John Doe".getBytes("UTF-8"));
        File outDir = folder.newFolder("out");
        String redacted = Base64.getEncoder().encodeToString("Pay [NAME_1]".getBytes("UTF-8"));

        MockWebServer server = new MockWebServer();
        server.start();
        try {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"runId\":\"run-7\"}"));
            server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"status\":\"QUEUED\"}"));
            server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"status\":\"SUCCESS\",\"outputType\":\"BASE64\","
                    + "\"output\":[{\"processedFile\":\"" + redacted + "\",\"processedFileType\":\"REDACTED_FILE\",\"processedFileExtension\":\"txt\"},"
                    + "{\"processedFile\":\"W10=\",\"processedFileType\":\"ENTITIES\",\"processedFileExtension\":\"json\"}],"
                    + "\"metrics\":{\"wordCount\":3}}"));

            DeidentifyFileResponse response = controllerFor(server).deidentifyFile(DeidentifyFileRequest.builder()
                    .file(input).configurationId("cfg-1")
                    .pollOptions(PollOptions.builder().waitTime(1).build())   // one real 1 s sleep at most
                    .outputDirectory(outDir.getPath())
                    .build());

            Assert.assertEquals("run-7", response.getRunId());
            Assert.assertEquals(DetectRunStatus.SUCCESS, response.getStatus());
            Assert.assertEquals(DataSourceType.BASE64, response.getOutputType());
            Assert.assertEquals(2, response.getOutput().size());
            Assert.assertEquals(Integer.valueOf(3), response.getMetrics().getWordCount());
            Assert.assertEquals("Pay [NAME_1]", new String(Files.readAllBytes(new File(outDir, "processed-invoice.txt").toPath()), "UTF-8"));
            Assert.assertEquals("[]", new String(Files.readAllBytes(new File(outDir, "processed-entities-invoice.json").toPath()), "UTF-8"));

            RecordedRequest submit = server.takeRequest();
            assertCommonHeaders(submit, "POST", "/v2/detect/deidentify/file");
            String body = submit.getBody().readUtf8();
            Assert.assertTrue(body, body.contains("\"dataSource\":\"BASE64\""));
            Assert.assertTrue(body, body.contains("\"value\":\"" + Base64.getEncoder().encodeToString("Pay John Doe".getBytes("UTF-8")) + "\""));
            Assert.assertTrue(body, body.contains("\"dataFormat\":\"txt\""));
            Assert.assertEquals("/v2/detect/runs/run-7?vaultId=vault-1", server.takeRequest().getPath());
            Assert.assertEquals("/v2/detect/runs/run-7?vaultId=vault-1", server.takeRequest().getPath());
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void deidentifyFileReturnsRunIdWithStatusWhenBudgetRunsOut() throws Exception {
        MockWebServer server = new MockWebServer();
        server.start();
        try {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"runId\":\"run-8\"}"));
            server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"status\":\"IN_PROGRESS\"}"));

            DeidentifyFileResponse response = controllerFor(server).deidentifyFile(DeidentifyFileRequest.builder()
                    .dataSource(DataSourceType.BASE64).value("Zm9v")
                    .pollOptions(PollOptions.builder().maxAttempts(1).build()).build());

            Assert.assertEquals("run-8", response.getRunId());
            Assert.assertEquals(DetectRunStatus.IN_PROGRESS, response.getStatus());
            Assert.assertTrue(response.getOutput().isEmpty());
            Assert.assertEquals(2, server.getRequestCount());
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void deidentifyFileSurfacesFailedRunMessage() throws Exception {
        MockWebServer server = new MockWebServer();
        server.start();
        try {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"runId\":\"run-9\"}"));
            server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"status\":\"FAILED\",\"message\":\"unsupported file\"}"));

            DeidentifyFileResponse response = controllerFor(server).deidentifyFile(DeidentifyFileRequest.builder()
                    .dataSource(DataSourceType.BASE64).value("Zm9v").pollOptions(PollOptions.builder().build()).build());

            Assert.assertEquals(DetectRunStatus.FAILED, response.getStatus());
            Assert.assertEquals("unsupported file", response.getMessage());
            Assert.assertEquals(2, server.getRequestCount());
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void deidentifyFileSubmitErrorMapsToSkyflowException() throws Exception {
        MockWebServer server = new MockWebServer();
        server.start();
        try {
            server.enqueue(new MockResponse().setResponseCode(400).setHeader("x-request-id", "req-1")
                    .setBody("{\"error\":{\"grpc_code\":3,\"http_code\":400,\"message\":\"configuration not found\",\"http_status\":\"Bad Request\",\"details\":[]}}"));
            try {
                controllerFor(server).deidentifyFile(DeidentifyFileRequest.builder()
                        .dataSource(DataSourceType.BASE64).value("Zm9v").configurationId("missing").build());
                Assert.fail("expected SkyflowException");
            } catch (SkyflowException e) {
                Assert.assertEquals(400, e.getHttpCode());
                Assert.assertEquals("configuration not found", e.getMessage());
                Assert.assertEquals("req-1", e.getRequestId());
            }
        } finally {
            server.shutdown();
        }
    }
}
