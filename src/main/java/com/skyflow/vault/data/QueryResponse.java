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
        Gson gson = new Gson();
        JsonObject responseObject = JsonParser.parseString(gson.toJson(this)).getAsJsonObject();
        JsonArray fieldsArray = responseObject.get("fields").getAsJsonArray();
        for (JsonElement fieldElement : fieldsArray) {
            fieldElement.getAsJsonObject().add("tokenizedData", null);
        }
        responseObject.add("errors", new JsonArray());
        return responseObject.toString();
    }
}
