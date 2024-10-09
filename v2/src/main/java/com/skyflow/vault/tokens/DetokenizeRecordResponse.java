package com.skyflow.vault.tokens;

import com.skyflow.generated.rest.models.V1DetokenizeRecordResponse;

public class DetokenizeRecordResponse {
    private final String token;
    private final String value;
    private final String type;
    private final String error;

    public DetokenizeRecordResponse(V1DetokenizeRecordResponse record) {
        this.token = record.getToken();
        this.value = record.getValue();
        this.type = record.getValueType().getValue();
        this.error = record.getError();
    }

    public String getError() {
        return error;
    }

    public String getToken() {
        return token;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        StringBuilder response = new StringBuilder("{");
        response.append("\n\t").append("\"token\": \"").append(token).append("\",");
        if (error == null) {
            response.append("\n\t").append("\"value\": \"").append(value).append("\",");
            response.append("\n\t").append("\"type\": \"").append(type).append("\",");
        } else {
            response.append("\n\t").append("\"error\": \"").append(error).append("\",");
        }
        response.append("\n}");
        return response.toString();
    }
}