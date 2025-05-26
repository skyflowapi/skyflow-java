package com.skyflow.vault.detect;


import java.util.Map;

public class EntityInfo {
    private final String token;
    private final String value;
    private final TextIndex textIndex;
    private final TextIndex processedIndex;
    private final String entity;
    private final Map<String, Float> scores;

    public EntityInfo(String token, String value, TextIndex textIndex, TextIndex processedIndex, String entity, java.util.Map<String, Float> scores) {
        this.token = token;
        this.value = value;
        this.textIndex = textIndex;
        this.processedIndex = processedIndex;
        this.entity = entity;
        this.scores = scores;
    }

    public String getToken() {
        return token;
    }

    public String getValue() {
        return value;
    }

    public TextIndex getTextIndex() {
        return textIndex;
    }

    public TextIndex getProcessedIndex() {
        return processedIndex;
    }

    public String getEntity() {
        return entity;
    }

    public Map<String, Float> getScores() {
        return scores;
    }
}
