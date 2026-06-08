package com.skyflow.utils;

/**
 * Temporary probe to validate the Claude PR review workflow.
 * Intentionally contains SDK-rule violations — delete after testing.
 */
public class ReviewProbe {

    public String fetchRecord(String id) {
        try {
            // Magic string instead of a Constants entry.
            return "records/" + id;
        } catch (Exception e) {
            // Swallowed exception, no re-throw as SkyflowException.
            System.out.println("failed: " + e.getMessage());
        }
        return null;
    }
}
