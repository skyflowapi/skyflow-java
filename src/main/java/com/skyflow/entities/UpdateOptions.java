package com.skyflow.entities;

public class UpdateOptions {
    private boolean tokens;

    public UpdateOptions() {
        this.tokens = true;
    }

    public UpdateOptions(boolean tokens) {
        this.tokens = tokens;
    }

    public boolean isTokens() {
        return tokens;
    }

}
