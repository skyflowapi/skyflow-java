package com.skyflow.vault.tokens;

import com.google.gson.*;
import com.skyflow.utils.Constants;

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
        JsonArray tokensArray = responseObject.remove(Constants.JsonFieldNames.TOKENS).getAsJsonArray();
        JsonArray newTokensArray = new JsonArray();
        for (JsonElement token : tokensArray) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty(Constants.ApiToken.TOKEN, token.getAsString());
            newTokensArray.add(jsonObject);
        }
        responseObject.add(Constants.JsonFieldNames.TOKENS, newTokensArray);
        responseObject.add(Constants.JsonFieldNames.ERRORS, null);
        return responseObject.toString();
    }
}
