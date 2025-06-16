package com.skyflow.vault.detect;

import com.skyflow.enums.DetectEntities;
import com.skyflow.enums.DetectOutputTranscriptions;
import com.skyflow.enums.MaskingMethod;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

public class DeidentifyFileRequestTest {

    @Test
    public void testBuilderAndGetters() {
        File file = new File("test.txt");
        DetectEntities entity = DetectEntities.DOB;
        MaskingMethod maskingMethod = MaskingMethod.BLACKBOX;
        DetectOutputTranscriptions transcription = DetectOutputTranscriptions.TRANSCRIPTION;
        String outputDir = "/tmp/output";
        int waitTime = 42;

        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(file)
                .entities(Arrays.asList(entity))
                .allowRegexList(Arrays.asList("a.*"))
                .restrictRegexList(Arrays.asList("b.*"))
                .maskingMethod(maskingMethod)
                .outputProcessedImage(true)
                .outputOcrText(true)
                .outputProcessedAudio(true)
                .outputTranscription(transcription)
                .outputDirectory(outputDir)
                .waitTime(waitTime)
                .build();

        Assert.assertEquals(file, request.getFile());
        Assert.assertEquals(entity, request.getEntities().get(0));
        Assert.assertEquals("a.*", request.getAllowRegexList().get(0));
        Assert.assertEquals("b.*", request.getRestrictRegexList().get(0));
        Assert.assertEquals(maskingMethod, request.getMaskingMethod());
        Assert.assertTrue(request.getOutputProcessedImage());
        Assert.assertTrue(request.getOutputOcrText());
        Assert.assertTrue(request.getOutputProcessedAudio());
        Assert.assertEquals(transcription, request.getOutputTranscription());
        Assert.assertEquals(outputDir, request.getOutputDirectory());
        Assert.assertEquals(Integer.valueOf(waitTime), request.getWaitTime());
    }

    @Test
    public void testBuilderDefaults() {
        DeidentifyFileRequest request = DeidentifyFileRequest.builder().build();
        Assert.assertFalse(request.getOutputProcessedImage());
        Assert.assertFalse(request.getOutputOcrText());
        Assert.assertFalse(request.getOutputProcessedAudio());
        Assert.assertNull(request.getOutputDirectory());
        Assert.assertNull(request.getWaitTime());
    }
}