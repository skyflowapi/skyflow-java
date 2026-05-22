package com.skyflow.enums;

import org.junit.Assert;
import org.junit.Test;

public class DetectOutputTranscriptionsTest {

    @Test
    public void testDiarizedTranscription() {
        Assert.assertEquals("diarized_transcription", DetectOutputTranscriptions.DIARIZED_TRANSCRIPTION.getDetectOutputTranscriptions());
        Assert.assertEquals("diarized_transcription", DetectOutputTranscriptions.DIARIZED_TRANSCRIPTION.toString());
    }

    @Test
    public void testMedicalDiarizedTranscription() {
        Assert.assertEquals("medical_diarized_transcription", DetectOutputTranscriptions.MEDICAL_DIARIZED_TRANSCRIPTION.getDetectOutputTranscriptions());
        Assert.assertEquals("medical_diarized_transcription", DetectOutputTranscriptions.MEDICAL_DIARIZED_TRANSCRIPTION.toString());
    }

    @Test
    public void testMedicalTranscription() {
        Assert.assertEquals("medical_transcription", DetectOutputTranscriptions.MEDICAL_TRANSCRIPTION.getDetectOutputTranscriptions());
        Assert.assertEquals("medical_transcription", DetectOutputTranscriptions.MEDICAL_TRANSCRIPTION.toString());
    }

    @Test
    public void testTranscription() {
        Assert.assertEquals("transcription", DetectOutputTranscriptions.TRANSCRIPTION.getDetectOutputTranscriptions());
        Assert.assertEquals("transcription", DetectOutputTranscriptions.TRANSCRIPTION.toString());
    }
}
