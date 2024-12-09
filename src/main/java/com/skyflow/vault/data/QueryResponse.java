package com.skyflow.vault.data;

import com.google.gson.*;

import java.util.ArrayList;
import java.util.HashMap;

public class QueryResponse {
    private final ArrayList<HashMap<String, Object>> fields;
    private ArrayList<HashMap<String, Object>> tokenizedData;

    public QueryResponse(ArrayList<HashMap<String, Object>> fields) {
        this.fields = fields;
    }

    public ArrayList<HashMap<String, Object>> getFields() {
        return fields;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        JsonObject responseObject = gson.toJsonTree(this).getAsJsonObject();
        JsonArray fieldsArray = responseObject.get("fields").getAsJsonArray();
        for (JsonElement fieldElement : fieldsArray) {
            fieldElement.getAsJsonObject().add("tokenizedData", new JsonObject());
        }
        responseObject.add("errors", new JsonArray());
        responseObject.remove("tokenizedData");
        return responseObject.toString();
    }
}
