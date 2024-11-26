package com.skyflow.vault.tokens;

import java.util.List;

public class TokenizeResponse {
    private final List<String> tokens;

    public TokenizeResponse(List<String> tokens) {
        this.tokens = tokens;
    }

    public List<String> getTokens() {
        return tokens;
    }

    @Override
    public String toString() {
        StringBuilder response = new StringBuilder("{");
        response.append("\n\t\"tokens\": ").append(tokens);
        return response.append("\n}").toString();
    }
}
