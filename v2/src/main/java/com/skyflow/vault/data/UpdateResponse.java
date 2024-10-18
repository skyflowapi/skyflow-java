package com.skyflow.vault.data;

import java.util.HashMap;

public class UpdateResponse {
    private String skyflowId;
    private HashMap<String, Object> tokens;

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
        response.append("\n\t\"skyflowId\": ").append(skyflowId);
        response.append("\n\t\"tokens\": ").append(formatRecords(tokens));
        response.append("\n}");
        return response.toString();
    }

    private String formatRecords(HashMap<String, Object> tokens) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (String key : tokens.keySet()) {
            sb.append("\n\t\"").append(key).append("\": \"").append(tokens.get(key)).append("\",");
        }
        sb.append("\n}");
        return toIndentedString(sb);
    }

    private String toIndentedString(Object o) {
        return o.toString().replace("\n", "\n\t");
    }
}
