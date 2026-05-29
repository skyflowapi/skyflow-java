package com.skyflow.vault.detect;

import com.skyflow.enums.DetectEntities;
import com.skyflow.enums.DetectOutputTranscriptions;
import com.skyflow.enums.MaskingMethod;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

public class DeidentifyFileRequestTest {

    @Test
    public void testBuilderAndGetters() {
        File file = new File("test.txt");
        FileInput fileInput = FileInput.builder().file(file).build();
        DetectEntities entity = DetectEntities.DOB;
        MaskingMethod maskingMethod = MaskingMethod.BLACKBOX;
        DetectOutputTranscriptions transcription = DetectOutputTranscriptions.TRANSCRIPTION;
        String outputDir = "/tmp/output";
        int waitTime = 42;

        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(fileInput)
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

    @Test
    public void testBuilderWithFilePath() {
        String filePath = "/tmp/test.txt";
        FileInput fileInput = FileInput.builder().filePath(filePath).build();
        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(fileInput)
                .entities(Collections.singletonList(DetectEntities.DOB))
                .build();

        Assert.assertEquals(filePath, request.getFileInput().getFilePath());
        Assert.assertNull(request.getFileInput().getFile());
    }

    @Test
    public void testBuilderWithFileAndFilePath() {
        File file = new File("test.txt");
        String filePath = "/tmp/test.txt";
        FileInput fileInput = FileInput.builder().file(file).filePath(filePath).build();
        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(fileInput)
                .entities(Collections.singletonList(DetectEntities.DOB))
                .build();

        Assert.assertEquals(file, request.getFileInput().getFile());
        Assert.assertEquals(filePath, request.getFileInput().getFilePath());
    }

    @Test
    public void testBuilderWithNullFileAndFilePath() {
        FileInput fileInput = FileInput.builder().build();
        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(fileInput)
                .entities(Collections.singletonList(DetectEntities.DOB))
                .build();

        Assert.assertNull(request.getFileInput().getFile());
        Assert.assertNull(request.getFileInput().getFilePath());
    }

    @Test
    public void testBuilderNullBooleansFallToFalse() {
        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .outputProcessedImage(null)
                .outputOcrText(null)
                .outputProcessedAudio(null)
                .build();
        Assert.assertFalse(request.getOutputProcessedImage());
        Assert.assertFalse(request.getOutputOcrText());
        Assert.assertFalse(request.getOutputProcessedAudio());
    }

    @Test
    public void testBuilderWithTokenFormatTransformationsPixelDensityMaxResolutionBleep() {
        TokenFormat tf = TokenFormat.builder().build();
        Transformations tr = new Transformations(null);
        AudioBleep bleep = AudioBleep.builder().frequency(800.0).build();

        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .tokenFormat(tf)
                .transformations(tr)
                .pixelDensity(150)
                .maxResolution(1920)
                .bleep(bleep)
                .build();

        Assert.assertEquals(tf, request.getTokenFormat());
        Assert.assertEquals(tr, request.getTransformations());
        Assert.assertEquals(150, request.getPixelDensity().intValue());
        Assert.assertEquals(1920, request.getMaxResolution().intValue());
        Assert.assertEquals(bleep, request.getBleep());
    }
}