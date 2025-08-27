package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class InsertResponse {
    // These members will be included in the toString() output
    @Expose(serialize = true)
    private Summary summary;
    @Expose(serialize = true)
    private List<Success> success;
    @Expose(serialize = true)
    private List<ErrorRecord> errors;

    // Internal fields. Should not be included in toString() output
    private ArrayList<HashMap<String, Object>> originalPayload;
    private ArrayList<HashMap<String, Object>> recordsToRetry;

    public InsertResponse(List<Success> successRecords, List<ErrorRecord> errorRecords) {
        this.success = successRecords;
        this.errors = errorRecords;
    }

    public InsertResponse(
            List<Success> successRecords,
            List<ErrorRecord> errorRecords,
            ArrayList<HashMap<String, Object>> originalPayload
    ) {
        this.success = successRecords;
        this.errors = errorRecords;
        this.originalPayload = originalPayload;
        this.summary = new Summary(this.originalPayload.size(), this.success.size(), this.errors.size());
    }

    public Summary getSummary() {
        return this.summary;
    }

    public List<Success> getSuccess() {
        return this.success;
    }

    public List<ErrorRecord> getErrors() {
        return this.errors;
    }

    public ArrayList<HashMap<String, Object>> getRecordsToRetry() {
        if (recordsToRetry == null) {
            recordsToRetry = new ArrayList<>();
            recordsToRetry = errors.stream()
                    .filter(error -> (error.getCode() >= 500 && error.getCode() <= 599) && error.getCode() != 529)
                    .map(errorRecord -> originalPayload.get(errorRecord.getIndex()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        return recordsToRetry;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        return gson.toJson(this);
    }
}