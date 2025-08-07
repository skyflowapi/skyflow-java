package com.skyflow.v2.enums;

public enum TokenType {
    VAULT_TOKEN("vault_token"),
    ENTITY_UNIQUE_COUNTER("entity_unq_counter"),
    ENTITY_ONLY("entity_only");

    private final String tokenType;

    TokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getDefault() {
        return ENTITY_UNIQUE_COUNTER.getTokenType();
    }

    @Override
    public String toString() {
        return tokenType;
    }
}

