package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.skyflow.utils.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;

public class UpdateResponse {
    private final String skyflowId;
    private final HashMap<String, Object> tokens;

    public UpdateResponse(String skyflowId, HashMap<String, Object> tokens) {
        this.skyflowId = skyflowId;
        this.tokens = tokens;
    }

    public String getSkyflowId() {
        return skyflowId;
    }

    public HashMap<String, Object> getTokens() {
        return tokens;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        JsonObject responseObject = JsonParser.parseString(gson.toJson(this)).getAsJsonObject();
        JsonObject tokensObject = responseObject.remove(Constants.JsonFieldNames.TOKENS).getAsJsonObject();
        for (String key : tokensObject.keySet()) {
            responseObject.add(key, tokensObject.get(key));
        }
        JsonObject finalResponseObject = new JsonObject();
        finalResponseObject.add(Constants.JsonFieldNames.UPDATED_FIELD, responseObject);
        finalResponseObject.add(Constants.JsonFieldNames.ERRORS, null);
        return finalResponseObject.toString();
    }
}
