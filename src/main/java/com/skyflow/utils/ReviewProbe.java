package com.skyflow.utils;

/**
 * Temporary probe to validate the Claude PR review workflow.
 * Intentionally contains SDK-rule violations — delete after testing.
 */
public class ReviewProbe {

    // Dummy comment block to test incremental review (no new issue expected).
    // Line 1 of 10
    // Line 2 of 10
    // Line 3 of 10
    // Line 4 of 10
    // Line 5 of 10
    // Line 6 of 10
    // Line 7 of 10
    // Line 8 of 10
    // Line 9 of 10
    // Line 10 of 10


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
