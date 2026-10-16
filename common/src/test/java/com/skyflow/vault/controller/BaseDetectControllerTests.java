package com.skyflow.vault.controller;

import com.skyflow.config.BaseVaultConfig;
import com.skyflow.detect.DeidentifyStringRequest;
import com.skyflow.detect.DeidentifyStringResponse;
import com.skyflow.detect.Detect;
import com.skyflow.detect.DetectConfiguration;
import com.skyflow.detect.Entity;
import com.skyflow.detect.CheckGuardrailsRequest;
import com.skyflow.detect.CheckGuardrailsResponse;
import com.skyflow.detect.DataSourceType;
import com.skyflow.detect.DetectRunStatus;
import com.skyflow.detect.EntityType;
import com.skyflow.detect.FileDataFormat;
import com.skyflow.detect.GetRunRequest;
import com.skyflow.detect.GetRunResponse;
import com.skyflow.detect.GuardrailsValidation;
import com.skyflow.detect.RedactionLevel;
import com.skyflow.detect.ReidentifyStringRequest;
import com.skyflow.detect.ReidentifyStringResponse;
import com.skyflow.enums.Env;
import com.skyflow.errors.BaseErrorMessage;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class BaseDetectControllerTests {

    /** Minimal SDK controller: fixed URL, counted tokens, no credential chain. */
    private static final class TestDetectController extends BaseDetectController<BaseVaultConfig> {
        private String vaultUrl;
        private final AtomicInteger tokenCalls = new AtomicInteger();
        private SkyflowException tokenFailure;

        TestDetectController(BaseVaultConfig config, String vaultUrl) {
            super(config, null);
            this.vaultUrl = vaultUrl;
        }

        @Override
        protected String resolveVaultUrl() {
            return vaultUrl;
        }

        @Override
        protected String getSdkMetrics() {
            return "{\"sdk_name_version\":\"test\"}";
        }

        @Override
        protected String resolveToken() throws SkyflowException {
            if (tokenFailure != null) {
                throw tokenFailure;
            }
            return "token-" + tokenCalls.incrementAndGet();
        }
    }

    private MockWebServer server;
    private TestDetectController controller;

    private static BaseVaultConfig config(String vaultId) {
        BaseVaultConfig config = new BaseVaultConfig();
        config.setVaultId(vaultId);
        config.setClusterId("cluster");
        config.setEnv(Env.PROD);
        return config;
    }

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        controller = new TestDetectController(config("vault-1"), server.url("").toString());
        controller.setCommonHttpConfig(null, 0, null, null);
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void deidentifyStringSendsScopedRequestAndMapsResponse() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json")
                .setBody("{\"processedText\":\"My name is [NAME_1].\",\"entities\":[{\"token\":\"NAME_1\",\"entityType\":\"NAME\","
                        + "\"location\":{\"startIndex\":11,\"endIndex\":19}}],\"metrics\":{\"wordCount\":4}}"));

        DeidentifyStringResponse response = controller.deidentifyString(DeidentifyStringRequest.builder()
                .text("My name is John Doe.")
                .configuration(DetectConfiguration.builder()
                        .detect(Detect.builder().entities(Collections.singletonList(
                                Entity.builder().entityType(EntityType.NAME).build())).build())
                        .build())
                .build());

        Assert.assertEquals("My name is [NAME_1].", response.getProcessedText());
        Assert.assertEquals("NAME_1", response.getEntities().get(0).getToken());
        Assert.assertEquals(Integer.valueOf(4), response.getMetrics().getWordCount());

        RecordedRequest recorded = server.takeRequest();
        Assert.assertEquals("POST", recorded.getMethod());
        Assert.assertEquals("/v2/detect/deidentify/string", recorded.getPath());
        Assert.assertEquals("Bearer token-1", recorded.getHeader("Authorization"));
        Assert.assertEquals("{\"sdk_name_version\":\"test\"}", recorded.getHeader("sky-metadata"));
        String body = recorded.getBody().readUtf8();
        Assert.assertTrue(body, body.contains("\"text\":\"My name is John Doe.\""));
        Assert.assertTrue(body, body.contains("\"vaultId\":\"vault-1\""));
        Assert.assertTrue(body, body.contains("\"entityType\":\"NAME\""));
        Assert.assertFalse(body, body.contains("configurationId"));
    }

    @Test
    public void reidentifyStringSendsVaultIdInBodyAndRefreshesTokenPerCall() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"processedText\":\"My name is John Doe.\"}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"processedText\":\"My name is J**n D*e.\"}"));

        ReidentifyStringResponse first = controller.reidentifyString(
                ReidentifyStringRequest.builder().text("My name is [NAME_1].").build());
        ReidentifyStringResponse second = controller.reidentifyString(ReidentifyStringRequest.builder()
                .text("My name is [NAME_1].")
                .redactionLevel(Collections.singletonList(
                        RedactionLevel.builder().tokenGroupName("group").redactionPattern("mask").build()))
                .build());

        Assert.assertEquals("My name is John Doe.", first.getProcessedText());
        Assert.assertEquals("My name is J**n D*e.", second.getProcessedText());

        RecordedRequest r1 = server.takeRequest();
        Assert.assertEquals("/v2/detect/reidentify/string", r1.getPath());
        Assert.assertEquals("Bearer token-1", r1.getHeader("Authorization"));
        String body1 = r1.getBody().readUtf8();
        Assert.assertTrue(body1, body1.contains("\"vaultId\":\"vault-1\""));
        Assert.assertFalse(body1, body1.contains("redactionLevel"));

        RecordedRequest r2 = server.takeRequest();
        Assert.assertEquals("Bearer token-2", r2.getHeader("Authorization"));
        String body2 = r2.getBody().readUtf8();
        Assert.assertTrue(body2, body2.contains("\"tokenGroupName\":\"group\""));
        Assert.assertTrue(body2, body2.contains("\"redactionPattern\":\"mask\""));
    }

    @Test
    public void getRunQueriesRunByIdScopedToVault() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                "{\"status\":\"SUCCESS\",\"outputType\":\"BASE64\",\"output\":[{\"processedFile\":\"QUJD\","
                        + "\"processedFileType\":\"REDACTED_FILE\",\"processedFileExtension\":\"pdf\"},"
                        + "{\"processedFile\":\"e30=\",\"processedFileType\":\"ENTITIES\",\"processedFileExtension\":\"json\"}],"
                        + "\"metrics\":{\"pages\":3,\"size\":12.5}}"));

        GetRunResponse response = controller.getRun(GetRunRequest.builder().runId(" run-42 ").build());

        Assert.assertEquals("run-42", response.getRunId());
        Assert.assertEquals(DetectRunStatus.SUCCESS, response.getStatus());
        Assert.assertEquals(DataSourceType.BASE64, response.getOutputType());
        Assert.assertEquals(2, response.getOutput().size());
        Assert.assertEquals("QUJD", response.getOutput().get(0).getProcessedFile());
        Assert.assertEquals("REDACTED_FILE", response.getOutput().get(0).getProcessedFileType());
        Assert.assertEquals(FileDataFormat.PDF, response.getOutput().get(0).getProcessedFileExtension());
        Assert.assertEquals(FileDataFormat.JSON, response.getOutput().get(1).getProcessedFileExtension());
        Assert.assertEquals(Integer.valueOf(3), response.getMetrics().getPages());
        Assert.assertNull(response.getMessage());

        RecordedRequest recorded = server.takeRequest();
        Assert.assertEquals("GET", recorded.getMethod());
        Assert.assertEquals("/v2/detect/runs/run-42?vaultId=vault-1", recorded.getPath());
        Assert.assertEquals("Bearer token-1", recorded.getHeader("Authorization"));
        Assert.assertEquals("{\"sdk_name_version\":\"test\"}", recorded.getHeader("sky-metadata"));
    }

    @Test
    public void getRunReportsPendingAndFailedRuns() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"status\":\"IN_PROGRESS\"}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                "{\"status\":\"FAILED\",\"message\":\"unsupported file\",\"output\":[]}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"status\":\"SOMETHING_NEW\"}"));

        GetRunResponse pending = controller.getRun(GetRunRequest.builder().runId("r1").build());
        Assert.assertEquals(DetectRunStatus.IN_PROGRESS, pending.getStatus());
        Assert.assertNotNull(pending.getOutput());
        Assert.assertTrue(pending.getOutput().isEmpty());
        Assert.assertNull(pending.getOutputType());
        Assert.assertNull(pending.getMetrics());

        GetRunResponse failed = controller.getRun(GetRunRequest.builder().runId("r2").build());
        Assert.assertEquals(DetectRunStatus.FAILED, failed.getStatus());
        Assert.assertEquals("unsupported file", failed.getMessage());

        GetRunResponse unknown = controller.getRun(GetRunRequest.builder().runId("r3").build());
        Assert.assertEquals("unrecognised statuses degrade to UNKNOWN", DetectRunStatus.UNKNOWN, unknown.getStatus());
    }

    @Test
    public void getRunValidatesRunIdBeforeTokenOrNetwork() throws Exception {
        for (GetRunRequest bad : new GetRunRequest[]{null, GetRunRequest.builder().build(), GetRunRequest.builder().runId("  ").build()}) {
            try {
                controller.getRun(bad);
                Assert.fail("expected SkyflowException for " + bad);
            } catch (SkyflowException e) {
                Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            }
        }
        Assert.assertEquals(0, controller.tokenCalls.get());
        Assert.assertEquals(0, server.getRequestCount());
    }

    @Test
    public void getRunMapsNotFound() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(404)
                .setBody("{\"error\":{\"grpc_code\":5,\"http_code\":404,\"message\":\"run not found\",\"http_status\":\"Not Found\",\"details\":[]}}"));
        try {
            controller.getRun(GetRunRequest.builder().runId("missing").build());
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertEquals(404, e.getHttpCode());
            Assert.assertEquals("run not found", e.getMessage());
        }
    }

    @Test
    public void checkGuardrailsSendsScopedRequestAndMapsResponse() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                "{\"text\":\"tell me about politics\",\"validation\":\"FAILED\",\"toxic\":false,\"deniedTopic\":true}"));

        CheckGuardrailsResponse response = controller.checkGuardrails(CheckGuardrailsRequest.builder()
                .text("tell me about politics")
                .checkToxicity(true)
                .denyTopics(Arrays.asList("politics", "medical advice"))
                .build());

        Assert.assertEquals("tell me about politics", response.getText());
        Assert.assertEquals(GuardrailsValidation.FAILED, response.getValidation());
        Assert.assertEquals(Boolean.FALSE, response.getToxic());
        Assert.assertEquals(Boolean.TRUE, response.getDeniedTopic());

        RecordedRequest recorded = server.takeRequest();
        Assert.assertEquals("POST", recorded.getMethod());
        Assert.assertEquals("/v2/detect/guardrails", recorded.getPath());
        Assert.assertEquals("Bearer token-1", recorded.getHeader("Authorization"));
        Assert.assertEquals("{\"sdk_name_version\":\"test\"}", recorded.getHeader("sky-metadata"));
        String body = recorded.getBody().readUtf8();
        Assert.assertTrue(body, body.contains("\"text\":\"tell me about politics\""));
        Assert.assertTrue(body, body.contains("\"vaultId\":\"vault-1\""));
        Assert.assertTrue(body, body.contains("\"checkToxicity\":true"));
        Assert.assertTrue(body, body.contains("\"denyTopics\":[\"politics\",\"medical advice\"]"));
    }

    @Test
    public void checkGuardrailsOmitsUnsetOptionsAndMapsPassed() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"text\":\"hello\",\"validation\":\"PASSED\"}"));
        CheckGuardrailsResponse response = controller.checkGuardrails(CheckGuardrailsRequest.builder().text("hello").build());
        Assert.assertEquals(GuardrailsValidation.PASSED, response.getValidation());
        Assert.assertNull(response.getToxic());
        Assert.assertNull(response.getDeniedTopic());
        String body = server.takeRequest().getBody().readUtf8();
        Assert.assertFalse(body, body.contains("checkToxicity"));
        Assert.assertFalse(body, body.contains("denyTopics"));
    }

    @Test
    public void checkGuardrailsValidatesBeforeTokenOrNetwork() throws Exception {
        CheckGuardrailsRequest[] bad = {
                null,
                CheckGuardrailsRequest.builder().text(" ").build(),
        };
        for (CheckGuardrailsRequest request : bad) {
            try {
                controller.checkGuardrails(request);
                Assert.fail("expected SkyflowException for " + request);
            } catch (SkyflowException e) {
                Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            }
        }
        Assert.assertEquals(0, controller.tokenCalls.get());
        Assert.assertEquals(0, server.getRequestCount());
    }

    @Test
    public void deidentifyStringWithStoredConfigurationSendsOnlyTextAndId() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"processedText\":\"[NAME_1]\"}"));
        controller.deidentifyString(DeidentifyStringRequest.builder().text("John").configurationId("cfg-1").build());
        String body = server.takeRequest().getBody().readUtf8();
        Assert.assertEquals("{\"text\":\"John\",\"configurationId\":\"cfg-1\"}", body);
    }

    @Test
    public void apiErrorIsMappedToSkyflowExceptionWithServerMessage() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(400).setHeader("x-request-id", "req-123")
                .setBody("{\"error\":{\"grpc_code\":3,\"http_code\":400,\"message\":\"configuration not found\","
                        + "\"http_status\":\"Bad Request\",\"details\":[]}}"));
        try {
            controller.deidentifyString(DeidentifyStringRequest.builder().text("hello").configurationId("missing").build());
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertEquals(400, e.getHttpCode());
            Assert.assertEquals("configuration not found", e.getMessage());
            Assert.assertEquals("req-123", e.getRequestId());
            Assert.assertEquals(Integer.valueOf(3), e.getGrpcCode());
        }
    }

    @Test
    public void unmodelledStatusIsStillMapped() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(404)
                .setBody("{\"error\":{\"grpc_code\":5,\"http_code\":404,\"message\":\"not found\",\"http_status\":\"Not Found\",\"details\":[]}}"));
        try {
            controller.reidentifyString(ReidentifyStringRequest.builder().text("[NAME_1]").build());
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertEquals(404, e.getHttpCode());
            Assert.assertEquals("not found", e.getMessage());
        }
    }

    @Test
    public void validationFailsBeforeTokenOrNetwork() throws Exception {
        try {
            controller.deidentifyString(DeidentifyStringRequest.builder().text("  ").configurationId("cfg").build());
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(BaseErrorMessage.InvalidTextInDeidentifyString.getMessage(), e.getMessage());
        }
        Assert.assertEquals(0, controller.tokenCalls.get());
        Assert.assertEquals(0, server.getRequestCount());
    }

    @Test
    public void tokenFailurePropagatesWithoutNetworkCall() throws Exception {
        controller.tokenFailure = new SkyflowException(401, "no credentials");
        try {
            controller.reidentifyString(ReidentifyStringRequest.builder().text("[NAME_1]").build());
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertEquals("no credentials", e.getMessage());
        }
        Assert.assertEquals(0, server.getRequestCount());
    }

    @Test
    public void clientIsRebuiltWhenUrlOrHttpConfigChanges() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        controller.reidentifyString(ReidentifyStringRequest.builder().text("[NAME_1]").build());
        Object first = controller.stringsClient();
        Assert.assertSame("same settings must reuse the client", first, controller.stringsClient());

        controller.setCommonHttpConfig(30, 0, null, null);
        Object afterConfig = controller.stringsClient();
        Assert.assertNotSame("changed http config must rebuild", first, afterConfig);

        MockWebServer other = new MockWebServer();
        other.start();
        try {
            controller.vaultUrl = other.url("").toString();
            other.enqueue(new MockResponse().setResponseCode(200).setBody("{\"processedText\":\"moved\"}"));
            Assert.assertEquals("moved", controller.reidentifyString(
                    ReidentifyStringRequest.builder().text("[NAME_1]").build()).getProcessedText());
            Assert.assertNotSame("changed url must rebuild", afterConfig, controller.stringsClient());
            Assert.assertEquals(1, other.getRequestCount());
        } finally {
            other.shutdown();
        }
    }

    @Test
    public void setVaultConfigRescopesRequests() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        controller.reidentifyString(ReidentifyStringRequest.builder().text("[NAME_1]").build());
        controller.setVaultConfig(config("vault-2"));
        controller.reidentifyString(ReidentifyStringRequest.builder().text("[NAME_1]").build());
        Assert.assertTrue(server.takeRequest().getBody().readUtf8().contains("\"vaultId\":\"vault-1\""));
        Assert.assertTrue(server.takeRequest().getBody().readUtf8().contains("\"vaultId\":\"vault-2\""));
    }
}
