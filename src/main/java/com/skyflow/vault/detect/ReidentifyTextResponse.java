package com.skyflow.vault.detect;


public class ReidentifyTextResponse {
    private final String processedText;

    public ReidentifyTextResponse(String processedText) {
        this.processedText = processedText;
    }

    public String getProcessedText() {
        return processedText;
    }

    @Override
    public String toString() {
        return "ReidentifyTextResponse{" +
                "processedText='" + processedText + '\'' +
                '}';
    }
}
