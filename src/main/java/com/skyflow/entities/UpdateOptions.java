package com.skyflow.entities;

/**
 * This is the description for UpdateOptions Class.
 */
public class UpdateOptions {
    private boolean tokens;

    /**
     * @ignore
     */
    public UpdateOptions() {
        this.tokens = true;
    }

    /**
     * @param tokens This is the description of the tokens parameter.
     */
    public UpdateOptions(boolean tokens) {
        this.tokens = tokens;
    }

    /**
     * This is the description for isTokens method.
     * @return This is the description of what the method returns.
     */
    public boolean isTokens() {
        return tokens;
    }

}
