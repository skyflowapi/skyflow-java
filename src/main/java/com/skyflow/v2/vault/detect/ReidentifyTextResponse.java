package com.skyflow.v2.vault.detect;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
