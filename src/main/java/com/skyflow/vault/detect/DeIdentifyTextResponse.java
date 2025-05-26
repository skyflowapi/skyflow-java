package com.skyflow.vault.detect;


import com.google.gson.Gson;

import java.util.List;

public class DeIdentifyTextResponse {
    private final String processedText;
    private final List<EntityInfo> entities;
    private final int wordCount;
    private final int charCount;

    public DeIdentifyTextResponse(String processedText, List<EntityInfo> entities, int wordCount, int charCount) {
        this.processedText = processedText;
        this.entities = entities;
        this.wordCount = wordCount;
        this.charCount = charCount;
    }

    public String getProcessedText() {
        return processedText;
    }

    public List<EntityInfo> getEntities() {
        return entities;
    }

    public int getWordCount() {
        return wordCount;
    }

    public int getCharCount() {
        return charCount;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}
