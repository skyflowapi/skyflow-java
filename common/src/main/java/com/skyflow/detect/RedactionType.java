package com.skyflow.detect;

/**
 * How a token is rendered when re-identifying (skyvault vaults).
 */
public enum RedactionType {
    PLAIN_TEXT,
    MASKED,
    DEFAULT,
    REDACTED;
}
