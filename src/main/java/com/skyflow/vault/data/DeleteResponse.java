package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.Optional;

public class DeleteResponse {
    private final Optional<List<String>> deletedIds;

    public DeleteResponse(Optional<List<String>> deletedIds) {
        this.deletedIds = deletedIds;
    }

    public Optional<List<String>> getDeletedIds() {
        return deletedIds;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        JsonObject responseObject = JsonParser.parseString(gson.toJson(this)).getAsJsonObject();
        responseObject.add("errors", new JsonArray());
        return responseObject.toString();
    }
}
