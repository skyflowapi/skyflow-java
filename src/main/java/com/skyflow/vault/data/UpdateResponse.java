package com.skyflow.vault.data;

import java.util.HashMap;

public class UpdateResponse {
    private final String skyflowId;
    private final HashMap<String, Object> tokens;

    public UpdateResponse(String skyflowId, HashMap<String, Object> tokens) {
        this.skyflowId = skyflowId;
        this.tokens = tokens;
    }

    public String getSkyflowId() {
        return skyflowId;
    }

    public HashMap<String, Object> getTokens() {
        return tokens;
    }

    @Override
    public String toString() {
        StringBuilder response = new StringBuilder("{");
        response.append("\n\t\"skyflowId\": \"").append(skyflowId).append("\"");
        if (!tokens.isEmpty()) {
            response.append(formatTokens(tokens));
        }
        response.append("\n}");
        return response.toString();
    }

    private String formatTokens(HashMap<String, Object> tokens) {
        StringBuilder sb = new StringBuilder();
        for (String key : tokens.keySet()) {
            sb.append("\n\t\"").append(key).append("\": \"").append(tokens.get(key)).append("\",");
        }
        return sb.toString();
    }
}
