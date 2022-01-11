package com.skyflow.entities;

public class InsertOptions {

    private boolean tokens;

    public InsertOptions() {
        this.tokens = true;
    }

    public InsertOptions(boolean tokens) {
        this.tokens = tokens;
    }

    public boolean isTokens() {
        return tokens;
    }
}
