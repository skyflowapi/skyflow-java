package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;

public class DeleteResponse {
    private final List<String> deletedIds;

    public DeleteResponse(List<String> deletedIds) {
        this.deletedIds = deletedIds;
    }

    public List<String> getDeletedIds() {
        return deletedIds;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        JsonObject responseObject = JsonParser.parseString(gson.toJson(this)).getAsJsonObject();
        responseObject.add("errors", null);
        return responseObject.toString();
    }
}
