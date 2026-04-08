package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TokenizeResponse {
    @Expose(serialize = true)
    private TokenizeSummary summary;

    @Expose(serialize = true)
    private List<TokenizeSuccess> success;

    @Expose(serialize = true)
    private List<ErrorRecord> errors;

    private List<TokenizeRecord> originalPayload;

    public TokenizeResponse(List<TokenizeSuccess> success, List<ErrorRecord> errors) {
        this.success = success;
        this.errors = errors;
    }

    public TokenizeResponse(List<TokenizeSuccess> success, List<ErrorRecord> errors, List<TokenizeRecord> originalPayload) {
        this.success = success;
        this.errors = errors;
        this.originalPayload = originalPayload;

        int totalTokens = originalPayload.size();

        // Collect indices that appear in success and in errors
        Set<Integer> successIndices = new HashSet<>();
        for (TokenizeSuccess s : this.success) {
            successIndices.add(s.getIndex());
        }
        Set<Integer> errorIndices = new HashSet<>();
        for (ErrorRecord e : this.errors) {
            errorIndices.add(e.getIndex());
        }

        // totalTokenized  = ALL token groups succeeded   (in success, NOT in errors)
        // totalPartial    = SOME succeeded, SOME failed  (in both success AND errors)
        // totalFailed     = ALL token groups failed      (in errors, NOT in success)
        int totalTokenized = 0;
        int totalPartial = 0;
        int totalFailed = 0;
        for (int i = 0; i < totalTokens; i++) {
            boolean hasSuccess = successIndices.contains(i);
            boolean hasError   = errorIndices.contains(i);
            if (hasSuccess && !hasError)       totalTokenized++;
            else if (hasSuccess && hasError)   totalPartial++;
            else if (!hasSuccess && hasError)  totalFailed++;
            // else: index not present in either (shouldn't happen in practice)
        }

        this.summary = new TokenizeSummary(totalTokens, totalTokenized, totalPartial, totalFailed);
    }

    public TokenizeSummary getSummary() {
        return summary;
    }

    public List<TokenizeSuccess> getSuccess() {
        return success;
    }

    public List<ErrorRecord> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        return gson.toJson(this);
    }
}
