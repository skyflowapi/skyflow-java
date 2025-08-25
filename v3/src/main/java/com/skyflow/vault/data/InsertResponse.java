package com.skyflow.vault.data;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InsertResponse {
    private Summary summary;
    private List<Success> success;
    private List<ErrorRecord> errors;

    private List<Map<String, Object>> recordsToRetry;

    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public List<Success> getSuccess() {
        return success;
    }

    public void setSuccess(List<Success> success) {
        this.success = success;
    }

    public List<ErrorRecord> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorRecord> errors) {
        this.errors = errors;
    }

    public void setRecordsToRetry(List<Map<String, Object>> records) {
        if(recordsToRetry == null){
            recordsToRetry = records;
        } else {
            recordsToRetry.addAll(records);
        }
    }
    public List<Map<String, Object>> getRecordsToRetry() {
        if(recordsToRetry == null){
            return new ArrayList<>();
        }
        return recordsToRetry;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}