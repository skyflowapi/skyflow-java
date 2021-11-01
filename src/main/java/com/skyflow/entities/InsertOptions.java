package com.skyflow.entities;

public class InsertOptions {

    private boolean tokens;

    public InsertOptions(boolean tokens) {
        this.tokens = tokens;
    }

    public boolean isTokens() {
        return tokens;
    }

    public void setTokens(boolean tokens) {
        this.tokens = tokens;
    }
}
