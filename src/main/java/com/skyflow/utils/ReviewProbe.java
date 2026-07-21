package com.skyflow.utils;

/**
 * Temporary probe to validate the Claude PR review workflow.
 * Intentionally contains SDK-rule violations — delete after testing.
 */
public class ReviewProbe {

    private String vaultId;

    // NEW BUG 1 (correctness): String compared with == instead of .equals().
    public boolean isAdmin(String role) {
        return role == "admin";
    }

    // NEW BUG 2 (naming): all-caps acronym — should be setVaultId, not setVaultID.
    public void setVaultID(String id) {
        this.vaultId = id;
    }

    // NEW BUG 3 (error handling): empty catch swallows the exception.
    public void load(String value) {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
        }
    }

    // NEW SMELL 1 (advisory): large parameter list — more than 4 parameters.
    public String build(String a, String b, String c, String d, String e, String f) {
        return a + b + c + d + e + f;
    }

    // NEW SMELL 2 (advisory): deep nesting — more than 3 levels of if.
    public int classify(int n) {
        if (n > 0) {
            if (n < 100) {
                if (n % 2 == 0) {
                    if (n > 10) {
                        return n;
                    }
                }
            }
        }
        return 0;
    }
}
