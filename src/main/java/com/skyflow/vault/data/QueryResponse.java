package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;

public class QueryResponse {
    private final ArrayList<HashMap<String, Object>> fields;
    private final ArrayList<HashMap<String, Object>> errors;

    public QueryResponse(ArrayList<HashMap<String, Object>> fields) {
        this.fields = fields;
        this.errors = null;
    }

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
        Gson gson = new GsonBuilder().serializeNulls().create();
        JsonObject responseObject = gson.toJsonTree(this).getAsJsonObject();
        JsonArray fieldsArray = responseObject.get("fields").getAsJsonArray();
        // tokenizedData is intentionally injected per-record — Query API cannot return tokens;
        // this ensures the field is always present in serialised output for cross-SDK consistency
        for (JsonElement fieldElement : fieldsArray) {
            fieldElement.getAsJsonObject().add("tokenizedData", new JsonObject());
        }
        return responseObject.toString();
    }
}
