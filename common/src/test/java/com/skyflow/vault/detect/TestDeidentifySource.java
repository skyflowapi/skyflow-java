package com.skyflow.vault.detect;

/** Test stand-in for the per-SDK {@code DeidentifySourceType} enums, which common cannot see. Carries every API value. */
public enum TestDeidentifySource implements DeidentifyFileSource {
    BASE64,
    SKYFLOW_ID,
    PRESIGNED_URL;

    public static TestDeidentifySource fromWire(String wireValue) {
        try {
            return wireValue == null ? null : valueOf(wireValue);
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }
}
