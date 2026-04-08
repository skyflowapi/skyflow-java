package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.List;

public class DeleteTokensResponse {
    @Expose(serialize = true)
    private DeleteTokensSummary summary;

    @Expose(serialize = true)
    private List<DeleteTokensSuccess> success;

    @Expose(serialize = true)
    private List<ErrorRecord> errors;

    private List<String> originalPayload;

    public DeleteTokensResponse(List<DeleteTokensSuccess> success, List<ErrorRecord> errors) {
        this.success = success;
        this.errors = errors;
    }

    public DeleteTokensResponse(List<DeleteTokensSuccess> success, List<ErrorRecord> errors, List<String> originalPayload) {
        this.success = success;
        this.errors = errors;
        this.originalPayload = originalPayload;
        this.summary = new DeleteTokensSummary(this.originalPayload.size(), this.success.size(), this.errors.size());
    }

    public DeleteTokensSummary getSummary() {
        return summary;
    }

    public List<DeleteTokensSuccess> getSuccess() {
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
