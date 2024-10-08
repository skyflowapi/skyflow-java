package com.skyflow.vault.tokens;

public class DetokenizeRecordResponse {
    private String token;
    private String value;
    private String type;
    private String error;

    public String getError() {
        return error;
    }

    void setError(String error) {
        this.error = error;
    }

    public String getToken() {
        return token;
    }

    void setToken(String token) {
        this.token = token;
    }

    public String getValue() {
        return value;
    }

    void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    void setType(String type) {
        this.type = type;
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