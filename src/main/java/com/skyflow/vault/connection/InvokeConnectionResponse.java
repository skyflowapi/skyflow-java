package com.skyflow.vault.connection;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class InvokeConnectionResponse {
    private JsonObject response;

    public InvokeConnectionResponse(JsonObject response) {
        this.response = response;
    }

    public JsonObject getResponse() {
        return response;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        JsonObject responseObject = JsonParser.parseString(gson.toJson(this)).getAsJsonObject();
        responseObject = responseObject.remove("response").getAsJsonObject();
        return responseObject.toString();
    }
}
