package com.skyflow.vault.data;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

public class BaseQueryResponse {
    private final ArrayList<HashMap<String, Object>> fields;
    private final ArrayList<HashMap<String, Object>> errors;

    public BaseQueryResponse(ArrayList<HashMap<String, Object>> fields) {
        this.fields = fields;
        this.errors = null;
    }

    /**
     * Returns the list of record maps from the Query response. Each map contains all
     * field name/value pairs for the record.
     */
    public ArrayList<HashMap<String, Object>> getFields() {
        return fields;
    }

    /**
     * Always returns null. The Query API does not support partial-error responses.
     */
    public ArrayList<HashMap<String, Object>> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        Gson gson = new Gson().newBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
