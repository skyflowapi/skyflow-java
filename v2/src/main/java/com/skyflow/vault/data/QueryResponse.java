package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * <b>Deprecation notice:</b> the {@code skyflow_id} key in each {@link #getFields()} record map is
 * deprecated and will be removed in an upcoming release. Use {@code skyflowId} instead.
 * Both keys are present simultaneously in v2 for backward compatibility.
 */
public class QueryResponse extends BaseQueryResponse {
    public QueryResponse(ArrayList<HashMap<String, Object>> fields) {
        super(fields);
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
