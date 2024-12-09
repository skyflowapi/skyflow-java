package com.skyflow.vault.tokens;

import com.google.gson.Gson;

import java.util.ArrayList;

public class DetokenizeResponse {
    private final ArrayList<DetokenizeRecordResponse> detokenizedFields;
    private final ArrayList<DetokenizeRecordResponse> errors;

    public DetokenizeResponse(ArrayList<DetokenizeRecordResponse> detokenizedFields, ArrayList<DetokenizeRecordResponse> errors) {
        this.detokenizedFields = detokenizedFields;
        this.errors = errors;
    }

    public ArrayList<DetokenizeRecordResponse> getDetokenizedFields() {
        return detokenizedFields;
    }

    public ArrayList<DetokenizeRecordResponse> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
