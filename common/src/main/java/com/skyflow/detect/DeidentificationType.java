package com.skyflow.detect;

/**
 * How a detected entity is replaced in the processed output.
 */
public enum DeidentificationType {
    UNKNOWN,
    ENTITY_UNIQUE_COUNTER,
    ENTITY_ONLY,
    VAULT_TOKEN,
    MASK;
}
