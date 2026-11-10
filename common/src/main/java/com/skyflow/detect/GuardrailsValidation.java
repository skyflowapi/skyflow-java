package com.skyflow.detect;

/**
 * Overall result of a guardrails check.
 */
public enum GuardrailsValidation {
    PASSED,
    FAILED,
    /** The API reported a value this SDK version does not know. */
    UNKNOWN;
}
