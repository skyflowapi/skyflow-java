package com.skyflow.detect.internal;

import com.skyflow.detect.CheckGuardrailsResponse;
import com.skyflow.detect.DataSourceType;
import com.skyflow.detect.DeidentifyStringResponse;
import com.skyflow.detect.DetectRunStatus;
import com.skyflow.detect.FileDataFormat;
import com.skyflow.detect.GetRunResponse;
import com.skyflow.detect.GuardrailsValidation;
import com.skyflow.detect.DetectedEntity;
import com.skyflow.detect.ReidentifyStringResponse;
import com.skyflow.generated.detect.rest.core.ObjectMappers;
import com.skyflow.generated.detect.rest.types.DeidentifyStringResponseV2;
import com.skyflow.generated.detect.rest.types.DetectGuardrailsResponseV2;
import com.skyflow.generated.detect.rest.types.DetectRunsResponseV2;
import com.skyflow.generated.detect.rest.types.ReidentifyStringResponseV2;
import org.junit.Assert;
import org.junit.Test;

public class DetectResponseMapperTests {

    @Test
    public void mapsFullDeidentifyStringResponse() throws Exception {
        String json = "{\"processedText\":\"My name is [NAME_1].\",\"entities\":[{\"token\":\"NAME_1\",\"value\":\"\","
                + "\"location\":{\"startIndex\":11,\"endIndex\":19,\"startIndexProcessed\":11,\"endIndexProcessed\":19},"
                + "\"entityType\":\"NAME\",\"entityScores\":{\"NAME\":0.9152}}],"
                + "\"metrics\":{\"size\":0.05,\"wordCount\":4,\"characterCount\":20,\"duration\":0.2}}";
        DeidentifyStringResponseV2 api = ObjectMappers.JSON_MAPPER.readValue(json, DeidentifyStringResponseV2.class);

        DeidentifyStringResponse response = DetectResponseMapper.toDeidentifyStringResponse(api);
        Assert.assertEquals("My name is [NAME_1].", response.getProcessedText());
        Assert.assertEquals(1, response.getEntities().size());
        DetectedEntity entity = response.getEntities().get(0);
        Assert.assertEquals("NAME_1", entity.getToken());
        Assert.assertEquals("", entity.getValue());
        Assert.assertEquals("NAME", entity.getEntityType());
        Assert.assertEquals(Integer.valueOf(11), entity.getLocation().getStartIndex());
        Assert.assertEquals(Integer.valueOf(19), entity.getLocation().getEndIndexProcessed());
        Assert.assertEquals(0.9152, entity.getEntityScores().get("NAME"), 1e-9);
        Assert.assertEquals(Integer.valueOf(4), response.getMetrics().getWordCount());
        Assert.assertEquals(Integer.valueOf(20), response.getMetrics().getCharacterCount());
        Assert.assertEquals(0.05, response.getMetrics().getSize(), 1e-9);
        Assert.assertNull(response.getMetrics().getPages());
    }

    @Test
    public void emptyResponseYieldsEmptyEntitiesNotNull() throws Exception {
        DeidentifyStringResponseV2 api = ObjectMappers.JSON_MAPPER.readValue("{}", DeidentifyStringResponseV2.class);
        DeidentifyStringResponse response = DetectResponseMapper.toDeidentifyStringResponse(api);
        Assert.assertNull(response.getProcessedText());
        Assert.assertNotNull(response.getEntities());
        Assert.assertTrue(response.getEntities().isEmpty());
        Assert.assertNull(response.getMetrics());
    }

    @Test
    public void nullListElementsFromTheApiAreSkipped() throws Exception {
        DeidentifyStringResponseV2 strings = ObjectMappers.JSON_MAPPER.readValue(
                "{\"entities\":[null,{\"token\":\"NAME_1\"},null]}", DeidentifyStringResponseV2.class);
        Assert.assertEquals(1, DetectResponseMapper.toDeidentifyStringResponse(strings).getEntities().size());

        DetectRunsResponseV2 run = ObjectMappers.JSON_MAPPER.readValue(
                "{\"status\":\"SUCCESS\",\"output\":[null,{\"processedFile\":\"x\"}]}", DetectRunsResponseV2.class);
        Assert.assertEquals(1, DetectResponseMapper.toGetRunResponse("r", run).getOutput().size());
    }

    @Test
    public void guardrailsResponseWithoutValidationMapsToUnknown() throws Exception {
        DetectGuardrailsResponseV2 api = ObjectMappers.JSON_MAPPER.readValue("{}", DetectGuardrailsResponseV2.class);
        CheckGuardrailsResponse response = DetectResponseMapper.toCheckGuardrailsResponse(api);
        Assert.assertNull(response.getText());
        Assert.assertEquals(GuardrailsValidation.UNKNOWN, response.getValidation());
        Assert.assertNull(response.getToxic());
        Assert.assertNull(response.getDeniedTopic());
    }

    @Test
    public void entityWithoutScoresGetsEmptyMap() throws Exception {
        DeidentifyStringResponseV2 api = ObjectMappers.JSON_MAPPER.readValue(
                "{\"entities\":[{\"token\":\"NAME_1\"}]}", DeidentifyStringResponseV2.class);
        DetectedEntity entity = DetectResponseMapper.toDeidentifyStringResponse(api).getEntities().get(0);
        Assert.assertNotNull(entity.getEntityScores());
        Assert.assertTrue(entity.getEntityScores().isEmpty());
        Assert.assertNull(entity.getLocation());
    }

    @Test
    public void mapsReidentifyStringResponse() throws Exception {
        ReidentifyStringResponseV2 api = ObjectMappers.JSON_MAPPER.readValue(
                "{\"processedText\":\"My name is John Doe.\",\"metrics\":{\"wordCount\":5}}", ReidentifyStringResponseV2.class);
        ReidentifyStringResponse response = DetectResponseMapper.toReidentifyStringResponse(api);
        Assert.assertEquals("My name is John Doe.", response.getProcessedText());
        Assert.assertEquals(Integer.valueOf(5), response.getMetrics().getWordCount());
    }

    @Test
    public void mapsCompletedRun() throws Exception {
        DetectRunsResponseV2 api = ObjectMappers.JSON_MAPPER.readValue(
                "{\"status\":\"SUCCESS\",\"outputType\":\"PRESIGNED_URL\",\"output\":[{\"processedFile\":\"https://x/y.pdf\","
                        + "\"processedFileType\":\"REDACTED_FILE\",\"processedFileExtension\":\"pdf\"}],\"metrics\":{\"pages\":2}}",
                DetectRunsResponseV2.class);
        GetRunResponse run = DetectResponseMapper.toGetRunResponse("run-1", api);
        Assert.assertEquals("run-1", run.getRunId());
        Assert.assertEquals(DetectRunStatus.SUCCESS, run.getStatus());
        Assert.assertEquals(DataSourceType.PRESIGNED_URL, run.getOutputType());
        Assert.assertEquals("https://x/y.pdf", run.getOutput().get(0).getProcessedFile());
        Assert.assertEquals(FileDataFormat.PDF, run.getOutput().get(0).getProcessedFileExtension());
        Assert.assertEquals(Integer.valueOf(2), run.getMetrics().getPages());
    }

    @Test
    public void runWithoutStatusIsUnknownAndUnknownWireValuesAreTolerated() throws Exception {
        GetRunResponse empty = DetectResponseMapper.toGetRunResponse("r",
                ObjectMappers.JSON_MAPPER.readValue("{}", DetectRunsResponseV2.class));
        Assert.assertEquals(DetectRunStatus.UNKNOWN, empty.getStatus());
        Assert.assertTrue(empty.getOutput().isEmpty());
        Assert.assertNull(empty.getOutputType());

        GetRunResponse odd = DetectResponseMapper.toGetRunResponse("r", ObjectMappers.JSON_MAPPER.readValue(
                "{\"status\":\"NEW_STATE\",\"outputType\":\"FTP\",\"output\":[{\"processedFileExtension\":\"heic\"}]}",
                DetectRunsResponseV2.class));
        Assert.assertEquals(DetectRunStatus.UNKNOWN, odd.getStatus());
        Assert.assertNull(odd.getOutputType());
        Assert.assertNull(odd.getOutput().get(0).getProcessedFileExtension());
    }

    @Test
    public void mapsGuardrailsResponseAndToleratesUnknownValidation() throws Exception {
        CheckGuardrailsResponse failed = DetectResponseMapper.toCheckGuardrailsResponse(ObjectMappers.JSON_MAPPER.readValue(
                "{\"text\":\"t\",\"validation\":\"FAILED\",\"toxic\":true}", DetectGuardrailsResponseV2.class));
        Assert.assertEquals("t", failed.getText());
        Assert.assertEquals(GuardrailsValidation.FAILED, failed.getValidation());
        Assert.assertEquals(Boolean.TRUE, failed.getToxic());
        Assert.assertNull(failed.getDeniedTopic());

        CheckGuardrailsResponse odd = DetectResponseMapper.toCheckGuardrailsResponse(ObjectMappers.JSON_MAPPER.readValue(
                "{\"text\":\"t\",\"validation\":\"MAYBE\"}", DetectGuardrailsResponseV2.class));
        Assert.assertEquals(GuardrailsValidation.UNKNOWN, odd.getValidation());
    }
}
