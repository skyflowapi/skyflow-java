package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private List<Map<String, Object>> recordsToRetry;

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

//    public void setSummary(Summary summary) {
//        this.summary = summary;
//    }

    public List<Success> getSuccess() {
        return this.success;
    }

//    public void setSuccess(List<Success> success) {
//        this.success = success;
//    }

    public List<ErrorRecord> getErrors() {
        return this.errors;
    }

//    public void setErrors(List<ErrorRecord> errors) {
//        this.errors = errors;
//    }

    public List<Map<String, Object>> getRecordsToRetry() {
        if (recordsToRetry == null) {
            recordsToRetry = new ArrayList<>();
            for (ErrorRecord errorRecord : errors) {
                int index = errorRecord.getIndex();
                recordsToRetry.add(originalPayload.get(index));
            }
        }
        return this.recordsToRetry;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        return gson.toJson(this);
    }
}