package com.skyflow.v2.enums;

public enum DetectOutputTranscriptions {
    DIARIZED_TRANSCRIPTION("diarized_transcription"),
    MEDICAL_DIARIZED_TRANSCRIPTION("medical_diarized_transcription"),
    MEDICAL_TRANSCRIPTION("medical_transcription"),
    PLAINTEXT_TRANSCRIPTION("plaintext_transcription"),
    TRANSCRIPTION("transcription");

    private final String detectOutputTranscriptions;

    DetectOutputTranscriptions(String detectOutputTranscriptions) {
        this.detectOutputTranscriptions = detectOutputTranscriptions;
    }

    public String getDetectOutputTranscriptions() {
        return detectOutputTranscriptions;
    }

    @Override
    public String toString() {
        return detectOutputTranscriptions;
    }
}
