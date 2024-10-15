package com.skyflow.vault.connection;

import com.google.gson.JsonObject;

public class InvokeConnectionResponse {
    private JsonObject response;

    public InvokeConnectionResponse(JsonObject response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "InvokeConnectionResponse{" +
                "response=" + response +
                '}';
    }
}
