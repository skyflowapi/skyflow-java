package com.skyflow.utils;

import java.util.Map;

/**
 * Temporary probe to validate the Claude PR review workflow.
 * Intentionally contains SDK-rule violations — delete after testing.
 */
public class ReviewProbe {

    // NEW BUG 1 (security): hardcoded credential embedded in source.
    private static final String API_TOKEN = "hardcoded-admin-password-123";

    public String fetchRecord(String id) {
        // Original bugs removed: swallowed exception, System.out.println, magic string.
        return id;
    }

    // NEW BUG 2 (code quality): @SuppressWarnings with no explanatory comment.
    @SuppressWarnings("unchecked")
    public Map<String, Object> castPayload(Object raw) {
        return (Map<String, Object>) raw;
    }

    public void process(String value) {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // NEW BUG 3 (error handling): printStackTrace instead of LogUtil.
            e.printStackTrace();
            // NEW BUG 4 (error handling): re-thrown as RuntimeException, not SkyflowException.
            throw new RuntimeException("bad value: " + API_TOKEN);
        }
    }
}
