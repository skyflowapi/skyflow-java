package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;

public class DeleteResponse {
    private final ArrayList<String> deletedIds;

    public DeleteResponse(ArrayList<String> deletedIds) {
        this.deletedIds = deletedIds;
    }

    public ArrayList<String> getDeletedIds() {
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
