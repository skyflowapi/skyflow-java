package com.skyflow.detect.internal;

import com.skyflow.detect.DataSourceType;
import com.skyflow.detect.DeidentifyFileRequest;
import com.skyflow.detect.DeidentifyFileResponse;
import com.skyflow.detect.Detect;
import com.skyflow.detect.DetectConfiguration;
import com.skyflow.detect.DetectRunStatus;
import com.skyflow.detect.Entity;
import com.skyflow.detect.EntityType;
import com.skyflow.detect.FileDataFormat;
import com.skyflow.detect.GetRunResponse;
import com.skyflow.errors.BaseErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.detect.rest.core.ObjectMappers;
import com.skyflow.generated.detect.rest.resources.filesv2.requests.DeidentifyFileRequestV2;
import com.skyflow.generated.detect.rest.types.DetectRunsResponseV2;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Collections;

public class DeidentifyFileMapperTests {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static String wire(DeidentifyFileRequestV2 request) throws Exception {
        return ObjectMappers.JSON_MAPPER.writeValueAsString(request);
    }

    @Test
    public void mapsRawValueRequest() throws Exception {
        DeidentifyFileRequestV2 api = DetectRequestMapper.toDeidentifyFileRequest(DeidentifyFileRequest.builder()
                .dataSource(DataSourceType.SKYFLOW_ID).value("rec-1").configurationId("cfg-1").build(), "vault-1");
        Assert.assertEquals("SKYFLOW_ID", api.getDataSource().toString());
        Assert.assertEquals("rec-1", api.getValue());
        Assert.assertEquals("cfg-1", api.getConfigurationId().get());
        Assert.assertFalse(api.getDataFormat().isPresent());
        Assert.assertFalse(api.getConfiguration().isPresent());
        String json = wire(api);
        Assert.assertTrue(json, json.contains("\"dataSource\":\"SKYFLOW_ID\""));
        Assert.assertTrue(json, json.contains("\"configurationId\":\"cfg-1\""));
        Assert.assertFalse(json, json.contains("dataFormat"));
        Assert.assertFalse(json, json.contains("\"configuration\""));
    }

    @Test
    public void dataFormatIsSentLowerCase() throws Exception {
        DeidentifyFileRequestV2 api = DetectRequestMapper.toDeidentifyFileRequest(DeidentifyFileRequest.builder()
                .dataSource(DataSourceType.PRESIGNED_URL).value("https://s3/in").dataFormat(FileDataFormat.PDF).build(), "v");
        Assert.assertTrue(wire(api).contains("\"dataFormat\":\"pdf\""));
    }

    @Test
    public void inlineConfigurationGetsVaultId() throws Exception {
        DeidentifyFileRequestV2 api = DetectRequestMapper.toDeidentifyFileRequest(DeidentifyFileRequest.builder()
                .dataSource(DataSourceType.BASE64).value("Zm9v")
                .configuration(DetectConfiguration.builder().detect(Detect.builder().entities(Collections.singletonList(
                        Entity.builder().entityType(EntityType.NAME).build())).build()).build())
                .build(), "vault-9");
        String json = wire(api);
        Assert.assertTrue(json, json.contains("\"vaultId\":\"vault-9\""));
        Assert.assertTrue(json, json.contains("\"entityType\":\"NAME\""));
    }

    @Test
    public void fileRequestIsEncodedAndFormatInferred() throws Exception {
        File input = folder.newFile("patient-notes.PDF");
        byte[] content = {1, 2, 3, 4, 5};
        Files.write(input.toPath(), content);

        DeidentifyFileRequestV2 api = DetectRequestMapper.toDeidentifyFileRequest(
                DeidentifyFileRequest.builder().file(input).build(), "v");
        Assert.assertEquals("BASE64", api.getDataSource().toString());
        Assert.assertEquals(Base64.getEncoder().encodeToString(content), api.getValue());
        Assert.assertEquals("pdf", api.getDataFormat().get().toString());

        DeidentifyFileRequestV2 explicit = DetectRequestMapper.toDeidentifyFileRequest(
                DeidentifyFileRequest.builder().filePath(input.getPath()).dataFormat(FileDataFormat.TXT).build(), "v");
        Assert.assertEquals("txt", explicit.getDataFormat().get().toString());
    }

    @Test
    public void localFileIsAlwaysSentAsBase64EvenIfDataSourceWasCleared() throws Exception {
        File input = folder.newFile("a.txt");
        Files.write(input.toPath(), new byte[]{1});
        DeidentifyFileRequestV2 api = DetectRequestMapper.toDeidentifyFileRequest(
                DeidentifyFileRequest.builder().file(input).dataSource(null).build(), "v");
        Assert.assertEquals("BASE64", api.getDataSource().toString());
    }

    @Test
    public void unknownOrMissingExtensionLeavesFormatForTheServer() throws Exception {
        for (String name : new String[]{"archive.heic", "README", "trailing."}) {
            File input = folder.newFile(name);
            Files.write(input.toPath(), new byte[]{9});
            DeidentifyFileRequestV2 api = DetectRequestMapper.toDeidentifyFileRequest(
                    DeidentifyFileRequest.builder().file(input).build(), "v");
            Assert.assertFalse(name, api.getDataFormat().isPresent());
        }
    }

    @Test
    public void unreadableFileIsReportedAsEncodeFailure() {
        try {
            DetectRequestMapper.toDeidentifyFileRequest(
                    DeidentifyFileRequest.builder().file(new File(folder.getRoot(), "nope.txt")).build(), "v");
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertEquals(BaseErrorMessage.FailedToEncodeFile.getMessage(), e.getMessage());
        }
    }

    @Test
    public void submittedResponseCarriesOnlyRunId() {
        DeidentifyFileResponse response = DetectResponseMapper.submittedDeidentifyFileResponse("run-7");
        Assert.assertEquals("run-7", response.getRunId());
        Assert.assertNull(response.getStatus());
        Assert.assertNull(response.getOutputType());
        Assert.assertTrue(response.getOutput().isEmpty());
        Assert.assertNull(response.getMetrics());
        Assert.assertNull(response.getMessage());
    }

    @Test
    public void polledResponseMergesRunIdWithRun() throws Exception {
        DetectRunsResponseV2 api = ObjectMappers.JSON_MAPPER.readValue(
                "{\"status\":\"SUCCESS\",\"outputType\":\"BASE64\",\"output\":[{\"processedFile\":\"UERG\","
                        + "\"processedFileType\":\"REDACTED_FILE\",\"processedFileExtension\":\"pdf\"}],\"metrics\":{\"pages\":2}}",
                DetectRunsResponseV2.class);
        GetRunResponse run = DetectResponseMapper.toGetRunResponse("run-7", api);
        DeidentifyFileResponse response = DetectResponseMapper.toDeidentifyFileResponse("run-7", run);
        Assert.assertEquals("run-7", response.getRunId());
        Assert.assertEquals(DetectRunStatus.SUCCESS, response.getStatus());
        Assert.assertEquals(DataSourceType.BASE64, response.getOutputType());
        Assert.assertEquals("UERG", response.getOutput().get(0).getProcessedFile());
        Assert.assertEquals(FileDataFormat.PDF, response.getOutput().get(0).getProcessedFileExtension());
        Assert.assertEquals(Integer.valueOf(2), response.getMetrics().getPages());
    }
}
