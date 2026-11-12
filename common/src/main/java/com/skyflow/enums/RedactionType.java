package com.skyflow.enums;

/**
 * How a stored value is rendered when it is read back. Shared by the skyvault vault operations
 * ({@code get} and {@code detokenize}) and by Detect re-identification, where a
 * {@link com.skyflow.vault.detect.RedactionRule} (the SDK's {@code RedactionLevel}) applies it to one entity type.
 */
public enum RedactionType {
    PLAIN_TEXT,
    MASKED,
    /**
     * Use the redaction configured on the vault column. Valid for {@code get} and {@code detokenize}.
     * The Detect API rejects it with HTTP 400 in a {@link com.skyflow.vault.detect.RedactionRule}
     * (the SDK's {@code RedactionLevel}); use {@link #PLAIN_TEXT}, {@link #MASKED} or {@link #REDACTED}
     * for re-identification.
     */
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
