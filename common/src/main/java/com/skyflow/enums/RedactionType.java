package com.skyflow.enums;

/**
 * How a stored value is rendered when it is read back. Shared by the skyvault vault operations
 * ({@code get} and {@code detokenize}) and by Detect re-identification, where a
 * {@code RedactionLevel} applies it to one entity type.
 */
public enum RedactionType {
    PLAIN_TEXT,
    MASKED,
    DEFAULT,
    REDACTED;

    /**
     * @return the wire value, which is the constant name
     */
    @Override
    public String toString() {
        return name();
    }
}
