package com.skyflow.vault.detect;

import com.skyflow.generated.rest.types.DeidentifyFileOutputProcessedFileType;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

public class DeidentifyFileResponseTest {

    @Test
    public void testAllGettersAndToString() {
        File fileObject = new File("test-path.txt");
        String file = "test-path.txt";
        String type = "pdf";
        String extension = ".pdf";
        Integer wordCount = 100;
        Integer charCount = 500;
        Double sizeInKb = 123.45;
        Double durationInSeconds = 12.3;
        Integer pageCount = 5;
        Integer slideCount = 0;
        FileEntityInfo entityInfo = new FileEntityInfo("PERSON", DeidentifyFileOutputProcessedFileType.ENTITIES, "John Doe");
        java.util.List<FileEntityInfo> entities = Collections.singletonList(entityInfo);
        String runId = "run-123";
        String status = "SUCCESS";
        FileInfo fileInfo = new FileInfo(fileObject);

        DeidentifyFileResponse response = new DeidentifyFileResponse(
                fileInfo, file, type, extension, wordCount, charCount, sizeInKb,
                durationInSeconds, pageCount, slideCount, entities, runId, status
        );

        Assert.assertEquals(type, response.getType());
        Assert.assertEquals(extension, response.getExtension());
        Assert.assertEquals(wordCount, response.getWordCount());
        Assert.assertEquals(charCount, response.getCharCount());
        Assert.assertEquals(sizeInKb, response.getSizeInKb());
        Assert.assertEquals(durationInSeconds, response.getDurationInSeconds());
        Assert.assertEquals(pageCount, response.getPageCount());
        Assert.assertEquals(slideCount, response.getSlideCount());
        Assert.assertEquals(entities, response.getEntities());
        Assert.assertEquals(runId, response.getRunId());
        Assert.assertEquals(status, response.getStatus());

        // toString should return a JSON string containing all fields
        String json = response.toString();
        Assert.assertTrue(json.contains(file));
        Assert.assertTrue(json.contains(type));
        Assert.assertTrue(json.contains(extension));
        Assert.assertTrue(json.contains(runId));
        Assert.assertTrue(json.contains(status));
        Assert.assertTrue(json.contains("PERSON"));
    }
}