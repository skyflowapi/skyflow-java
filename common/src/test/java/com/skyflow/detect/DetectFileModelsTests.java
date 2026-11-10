package com.skyflow.detect;

import com.skyflow.detect.internal.DetectResponseMapper;
import com.skyflow.detect.internal.ReidentifyFileMapper;
import com.skyflow.enums.RedactionType;
import com.skyflow.generated.common.rest.core.ObjectMappers;
import com.skyflow.generated.common.rest.types.DetectRunsResponseV2;
import com.skyflow.generated.common.rest.types.ReidentifyFileResponseV2;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

/**
 * toString coverage of the file request and response models. A file request's toString must never print the
 * base64 payload itself, only its length.
 */
public class DetectFileModelsTests {

    @Test
    public void deidentifyFileRequestToStringHidesThePayloadButShowsEveryOtherField() {
        PollOptions poll = PollOptions.builder().waitTime(10).build();
        DeidentifyFileRequest withValue = DeidentifyFileRequest.builder()
                .dataSource(TestDeidentifySource.PRESIGNED_URL)
                .value("https://example.test/secret-payload")
                .dataFormat(FileDataFormat.PDF)
                .configurationId("cfg-1")
                .pollOptions(poll)
                .outputDirectory("/tmp/out")
                .build();
        String text = withValue.toString();
        Assert.assertFalse("payload must not be printed: " + text, text.contains("secret-payload"));
        for (String expected : new String[]{"DeidentifyFileRequest{", "dataSource=PRESIGNED_URL", "value=<35 chars>",
                "file=null", "filePath=null", "dataFormat=PDF", "configurationId=cfg-1", "configuration=null",
                "pollOptions=PollOptions{", "outputDirectory=/tmp/out"}) {
            Assert.assertTrue("expected '" + expected + "' in " + text, text.contains(expected));
        }

        DeidentifyFileRequest withFile = DeidentifyFileRequest.builder()
                .file(new File("/data/in/notes.txt"))
                .filePath("/data/in/notes.txt")
                .configuration(DetectConfiguration.builder().name("inline").build())
                .build();
        String fileText = withFile.toString();
        Assert.assertTrue(fileText, fileText.contains("value=null"));
        Assert.assertTrue(fileText, fileText.contains("notes.txt"));
        Assert.assertTrue(fileText, fileText.contains("configuration=DetectConfiguration{"));
        Assert.assertEquals("/data/in/notes.txt", withFile.getFilePath());
        Assert.assertEquals("notes.txt", withFile.getFile().getName());
    }

    @Test
    public void reidentifyFileRequestToStringShowsEveryField() {
        ReidentifyFileRequest request = ReidentifyFileRequest.builder()
                .dataSource(TestReidentifySource.BASE64)
                .value("QUJD")
                .dataFormat(FileDataFormat.TXT)
                .redactionLevel(Collections.singletonList(
                        TestRedactionLevel.builder().entityName(EntityType.NAME).redactionType(RedactionType.PLAIN_TEXT).build()))
                .build();
        String text = request.toString();
        for (String expected : new String[]{"ReidentifyFileRequest{", "dataSource=BASE64", "dataFormat=TXT", "redactionLevel=["}) {
            Assert.assertTrue("expected '" + expected + "' in " + text, text.contains(expected));
        }
        Assert.assertEquals(TestReidentifySource.BASE64, request.getDataSource());
        Assert.assertEquals("QUJD", request.getValue());
        Assert.assertEquals(1, request.getRedactionLevel().size());
    }

    @Test
    public void getRunResponseToStringShowsEveryField() throws Exception {
        DetectRunsResponseV2 api = ObjectMappers.JSON_MAPPER.readValue(
                "{\"status\":\"FAILED\",\"outputType\":\"BASE64\",\"output\":[],\"message\":\"boom\"}", DetectRunsResponseV2.class);
        GetRunResponse run = DetectResponseMapper.toGetRunResponse("run-3", api, TestDeidentifySource::valueOf);
        String text = run.toString();
        for (String expected : new String[]{"GetRunResponse{", "runId=run-3", "status=FAILED", "outputType=BASE64",
                "output=[]", "metrics=null", "message=boom"}) {
            Assert.assertTrue("expected '" + expected + "' in " + text, text.contains(expected));
        }

        DeidentifyFileResponse merged = DetectResponseMapper.toDeidentifyFileResponse("run-3", run);
        Assert.assertEquals(DetectRunStatus.FAILED, merged.getStatus());
        Assert.assertEquals("boom", merged.getMessage());
        Assert.assertTrue(merged.toString().contains("message=boom"));
    }

    @Test
    public void reidentifyFileResponseToStringShowsEveryField() throws Exception {
        ReidentifyFileResponseV2 api = ObjectMappers.JSON_MAPPER.readValue(
                "{\"status\":\"SUCCESS\",\"outputType\":\"PRESIGNED_URL\","
                        + "\"output\":[{\"processedFile\":\"https://x\",\"processedFileType\":\"REIDENTIFIED_FILE\",\"processedFileExtension\":\"pdf\"}],"
                        + "\"metrics\":{\"pages\":2}}", ReidentifyFileResponseV2.class);
        ReidentifyFileResponse response = ReidentifyFileMapper.toResponse(api, TestReidentifySource::valueOf);
        String text = response.toString();
        for (String expected : new String[]{"ReidentifyFileResponse{", "status=SUCCESS", "outputType=PRESIGNED_URL",
                "output=[FileOutput{", "processedFileType=REIDENTIFIED_FILE", "metrics=Metrics{", "pages=2"}) {
            Assert.assertTrue("expected '" + expected + "' in " + text, text.contains(expected));
        }
    }
}
