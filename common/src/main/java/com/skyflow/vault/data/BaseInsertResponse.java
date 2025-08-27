package com.skyflow.vault.data;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

class BaseInsertResponse {
    private final ArrayList<HashMap<String, Object>> insertedFields;
    private final ArrayList<HashMap<String, Object>> errors;

    public BaseInsertResponse(ArrayList<HashMap<String, Object>> insertedFields, ArrayList<HashMap<String, Object>> errors) {
        this.insertedFields = insertedFields;
        this.errors = errors;
    }

    public ArrayList<HashMap<String, Object>> getInsertedFields() {
        return insertedFields;
    }

    public ArrayList<HashMap<String, Object>> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
