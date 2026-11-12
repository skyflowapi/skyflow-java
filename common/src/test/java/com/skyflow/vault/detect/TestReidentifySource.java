package com.skyflow.vault.detect;

/** Test stand-in for the per-SDK {@code ReidentifySourceType} enums, which common cannot see. */
public enum TestReidentifySource implements ReidentifyFileSource {
    BASE64,
    PRESIGNED_URL;

    public static TestReidentifySource fromWire(String wireValue) {
        try {
            return wireValue == null ? null : valueOf(wireValue);
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }
}
