package com.skyflow.detect.internal;

import com.skyflow.detect.DataSourceType;
import com.skyflow.detect.DetectRunStatus;
import com.skyflow.detect.EntityType;
import com.skyflow.detect.FileDataFormat;
import com.skyflow.detect.FileOutput;
import com.skyflow.detect.RedactionLevel;
import com.skyflow.detect.RedactionType;
import com.skyflow.detect.ReidentifyFileRequest;
import com.skyflow.detect.ReidentifyFileResponse;
import com.skyflow.generated.detect.rest.core.ObjectMappers;
import com.skyflow.generated.detect.rest.resources.filesv2.requests.ReidentifyFileRequestV2;
import com.skyflow.generated.detect.rest.types.ReidentifyFileResponseV2;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/** Request mapping is checked on the serialised wire JSON, response mapping from raw API JSON. */
public class ReidentifyFileMapperTests {

    private static String wire(ReidentifyFileRequestV2 request) throws Exception {
        return ObjectMappers.JSON_MAPPER.writeValueAsString(request);
    }

    private static ReidentifyFileResponse fromJson(String json) throws Exception {
        return ReidentifyFileMapper.toResponse(ObjectMappers.JSON_MAPPER.readValue(json, ReidentifyFileResponseV2.class));
    }

    // ─── request ──────────────────────────────────────────────────────────────

    @Test
    public void mapsRequiredFieldsAndInjectsVaultId() throws Exception {
        ReidentifyFileRequestV2 api = ReidentifyFileMapper.toApiRequest(ReidentifyFileRequest.builder()
                .dataSource(DataSourceType.BASE64).value("W05BTUVfMV0=").build(), "vault-1");

        Assert.assertEquals("BASE64", api.getDataSource().toString());
        Assert.assertEquals("W05BTUVfMV0=", api.getValue());
        Assert.assertEquals("vault-1", api.getVaultId());
        Assert.assertFalse(api.getDataFormat().isPresent());
        Assert.assertFalse(api.getRedactionLevel().isPresent());

        String json = wire(api);
        Assert.assertTrue(json, json.contains("\"dataSource\":\"BASE64\""));
        Assert.assertTrue(json, json.contains("\"value\":\"W05BTUVfMV0=\""));
        Assert.assertTrue(json, json.contains("\"vaultId\":\"vault-1\""));
        Assert.assertFalse(json, json.contains("dataFormat"));
        Assert.assertFalse(json, json.contains("redactionLevel"));
    }

    @Test
    public void mapsEveryDataSource() throws Exception {
        for (DataSourceType source : DataSourceType.values()) {
            ReidentifyFileRequestV2 api = ReidentifyFileMapper.toApiRequest(ReidentifyFileRequest.builder()
                    .dataSource(source).value("x").build(), "v");
            Assert.assertEquals(source.name(), api.getDataSource().toString());
            Assert.assertTrue(wire(api).contains("\"dataSource\":\"" + source.name() + "\""));
        }
    }

    @Test
    public void dataFormatIsSentAsLowerCaseWireValue() throws Exception {
        for (FileDataFormat format : new FileDataFormat[]{FileDataFormat.TXT, FileDataFormat.CSV, FileDataFormat.JSON,
                FileDataFormat.JSONL, FileDataFormat.XML, FileDataFormat.DOCX, FileDataFormat.XLSX, FileDataFormat.PPTX}) {
            ReidentifyFileRequestV2 api = ReidentifyFileMapper.toApiRequest(ReidentifyFileRequest.builder()
                    .dataSource(DataSourceType.SKYFLOW_ID).value("rec").dataFormat(format).build(), "v");
            Assert.assertEquals(format.name().toLowerCase(), api.getDataFormat().get().toString());
            Assert.assertTrue(wire(api).contains("\"dataFormat\":\"" + format.name().toLowerCase() + "\""));
        }
    }

    @Test
    public void mapsFlowvaultAndSkyvaultStyleRedactionLevels() throws Exception {
        ReidentifyFileRequestV2 api = ReidentifyFileMapper.toApiRequest(ReidentifyFileRequest.builder()
                .dataSource(DataSourceType.PRESIGNED_URL).value("https://s3/presigned")
                .redactionLevel(Arrays.asList(
                        RedactionLevel.builder().tokenGroupName("names").redactionPattern("mask-middle").build(),
                        RedactionLevel.builder().entityName(EntityType.EMAIL_ADDRESS).redactionType(RedactionType.MASKED).build()))
                .build(), "v");

        Assert.assertEquals(2, api.getRedactionLevel().get().size());
        String json = wire(api);
        Assert.assertTrue(json, json.contains("\"tokenGroupName\":\"names\""));
        Assert.assertTrue(json, json.contains("\"redactionPattern\":\"mask-middle\""));
        Assert.assertTrue(json, json.contains("\"entityName\":\"EMAIL_ADDRESS\""));
        Assert.assertTrue(json, json.contains("\"redactionType\":\"MASKED\""));
        // Unset halves of each level must not be serialised as nulls.
        Assert.assertFalse(json, json.contains("null"));
    }

    @Test
    public void emptyRedactionLevelListIsSentAsEmptyArray() throws Exception {
        ReidentifyFileRequestV2 api = ReidentifyFileMapper.toApiRequest(ReidentifyFileRequest.builder()
                .dataSource(DataSourceType.BASE64).value("x").redactionLevel(Collections.<RedactionLevel>emptyList()).build(), "v");
        Assert.assertTrue(api.getRedactionLevel().get().isEmpty());
        Assert.assertTrue(wire(api).contains("\"redactionLevel\":[]"));
    }

    // ─── response ─────────────────────────────────────────────────────────────

    @Test
    public void mapsFullResponse() throws Exception {
        ReidentifyFileResponse response = fromJson("{\"status\":\"SUCCESS\",\"outputType\":\"PRESIGNED_URL\","
                + "\"output\":[{\"processedFile\":\"https://s3/out-1\",\"processedFileType\":\"REIDENTIFIED_FILE\",\"processedFileExtension\":\"docx\"},"
                + "{\"processedFile\":\"https://s3/out-2\",\"processedFileType\":\"ENTITIES\",\"processedFileExtension\":\"json\"}],"
                + "\"metrics\":{\"size\":12.5,\"wordCount\":120,\"characterCount\":800,\"pages\":2,\"slides\":0,\"duration\":1.25}}");

        Assert.assertEquals(DetectRunStatus.SUCCESS, response.getStatus());
        Assert.assertEquals(DataSourceType.PRESIGNED_URL, response.getOutputType());
        Assert.assertEquals(2, response.getOutput().size());
        FileOutput first = response.getOutput().get(0);
        Assert.assertEquals("https://s3/out-1", first.getProcessedFile());
        Assert.assertEquals("REIDENTIFIED_FILE", first.getProcessedFileType());
        Assert.assertEquals(FileDataFormat.DOCX, first.getProcessedFileExtension());
        Assert.assertEquals("ENTITIES", response.getOutput().get(1).getProcessedFileType());
        Assert.assertEquals(FileDataFormat.JSON, response.getOutput().get(1).getProcessedFileExtension());
        Assert.assertEquals(12.5, response.getMetrics().getSize(), 1e-9);
        Assert.assertEquals(Integer.valueOf(120), response.getMetrics().getWordCount());
        Assert.assertEquals(Integer.valueOf(800), response.getMetrics().getCharacterCount());
        Assert.assertEquals(Integer.valueOf(2), response.getMetrics().getPages());
        Assert.assertEquals(Integer.valueOf(0), response.getMetrics().getSlides());
        Assert.assertEquals(1.25, response.getMetrics().getDuration(), 1e-9);
    }

    @Test
    public void mapsEveryKnownStatusAndOutputType() throws Exception {
        for (DetectRunStatus status : DetectRunStatus.values()) {
            Assert.assertEquals(status, fromJson("{\"status\":\"" + status.name() + "\"}").getStatus());
        }
        for (DataSourceType type : DataSourceType.values()) {
            Assert.assertEquals(type, fromJson("{\"outputType\":\"" + type.name() + "\"}").getOutputType());
        }
    }

    @Test
    public void emptyResponseYieldsDefaultsNotNulls() throws Exception {
        ReidentifyFileResponse response = fromJson("{}");
        Assert.assertEquals(DetectRunStatus.UNKNOWN, response.getStatus());
        Assert.assertNull(response.getOutputType());
        Assert.assertNotNull(response.getOutput());
        Assert.assertTrue(response.getOutput().isEmpty());
        Assert.assertNull(response.getMetrics());
    }

    @Test
    public void unknownWireValuesDegradeGracefully() throws Exception {
        ReidentifyFileResponse response = fromJson("{\"status\":\"BRAND_NEW\",\"outputType\":\"FTP\","
                + "\"output\":[{\"processedFile\":\"x\",\"processedFileType\":\"NEW_KIND\",\"processedFileExtension\":\"heic\"}]}");
        Assert.assertEquals(DetectRunStatus.UNKNOWN, response.getStatus());
        Assert.assertNull(response.getOutputType());
        FileOutput output = response.getOutput().get(0);
        Assert.assertEquals("x", output.getProcessedFile());
        Assert.assertEquals("NEW_KIND", output.getProcessedFileType());
        Assert.assertNull(output.getProcessedFileExtension());
    }

    @Test
    public void nullOutputElementsAreSkipped() throws Exception {
        Assert.assertEquals(1, fromJson("{\"output\":[null,{\"processedFile\":\"x\"},null]}").getOutput().size());
    }

    @Test
    public void outputWithMissingFieldsMapsToNulls() throws Exception {
        FileOutput output = fromJson("{\"output\":[{}]}").getOutput().get(0);
        Assert.assertNull(output.getProcessedFile());
        Assert.assertNull(output.getProcessedFileType());
        Assert.assertNull(output.getProcessedFileExtension());
    }

    @Test
    public void outputListIsUnmodifiable() throws Exception {
        ReidentifyFileResponse response = fromJson("{\"output\":[{\"processedFile\":\"x\"}]}");
        try {
            response.getOutput().clear();
            Assert.fail("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            Assert.assertEquals(1, response.getOutput().size());
        }
    }
}
