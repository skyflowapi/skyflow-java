package com.skyflow.entities;

/**
 * Additional parameters for the update method.
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
     * @param tokens Value of the tokens update option.
     */
    public UpdateOptions(boolean tokens) {
        this.tokens = tokens;
    }

    /**
     * Checks whether or not to return tokens.
     * @return Returns the value of tokens option.
     */
    public boolean isTokens() {
        return tokens;
    }

}
