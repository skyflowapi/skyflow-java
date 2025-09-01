package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class DetokenizeResponse {
    @Expose(serialize = true)
    private List<DetokenizeResponseObject> success;
    @Expose(serialize = true)
    private DetokenizeSummary summary;

    @Expose(serialize = true)
    private List<ErrorRecord> errors;

    private List<String> originalPayload;
    private List<String> recordsToRetry;

    public DetokenizeResponse(List<DetokenizeResponseObject> success, List<ErrorRecord> errors) {
        this.success = success;
        this.summary = summary;
        this.errors = errors;
    }

    public DetokenizeResponse(List<DetokenizeResponseObject> success, List<ErrorRecord> errors, List<String> originalPayload) {
        this.success = success;
        this.errors = errors;
        this.originalPayload = originalPayload;
        this.summary = new DetokenizeSummary(this.originalPayload.size(), this.success.size(), this.errors.size());
    }

    public List<String> getTokensToRetry() {
        if (recordsToRetry == null) {
            recordsToRetry = new ArrayList<>();
            recordsToRetry = errors.stream()
                    .filter(error -> (error.getCode() >= 500 && error.getCode() <= 599) && error.getCode() != 529)
                    .map(errorRecord -> originalPayload.get(errorRecord.getIndex()))
                    .collect(Collectors.toList());
        }
        return recordsToRetry;
    }

    public List<DetokenizeResponseObject> getSuccess() {
        return success;
    }
    public DetokenizeSummary getSummary() {
        return this.summary;
    }

    public List<ErrorRecord> getErrors() {
        return this.errors;
    }
    @Override
    public String toString() {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        return gson.toJson(this);
    }

}
