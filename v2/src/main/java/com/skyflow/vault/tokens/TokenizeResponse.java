package com.skyflow.vault.tokens;

import com.google.gson.*;

import java.util.List;

public class TokenizeResponse {
    private final List<String> tokens;

    public TokenizeResponse(List<String> tokens) {
        this.tokens = tokens;
    }

    public List<String> getTokens() {
        return tokens;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        JsonObject responseObject = JsonParser.parseString(gson.toJson(this)).getAsJsonObject();
        JsonArray tokensArray = responseObject.remove("tokens").getAsJsonArray();
        JsonArray newTokensArray = new JsonArray();
        for (JsonElement token : tokensArray) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("token", token.getAsString());
            newTokensArray.add(jsonObject);
        }
        responseObject.add("tokens", newTokensArray);
        responseObject.add("errors", null);
        return responseObject.toString();
    }
}
