package com.skyflow.vault.tokens;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;

public class DetokenizeResponse {
    private final ArrayList<DetokenizeRecordResponse> detokenizedFields;
    private final ArrayList<DetokenizeRecordResponse> errors;

    public DetokenizeResponse(ArrayList<DetokenizeRecordResponse> detokenizedFields, ArrayList<DetokenizeRecordResponse> errors) {
        this.detokenizedFields = detokenizedFields;
        this.errors = errors;
    }

    public ArrayList<DetokenizeRecordResponse> getDetokenizedFields() {
        return detokenizedFields;
    }

    public ArrayList<DetokenizeRecordResponse> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder()
                .serializeNulls()
                .registerTypeAdapter(
                        DetokenizeRecordResponse.class,
                        (com.google.gson.JsonSerializer<DetokenizeRecordResponse>) (src, typeOfSrc, context) -> {
                            com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                            obj.addProperty("token", src.getToken());
                            if (src.getValue() != null) {
                                obj.addProperty("value", src.getValue());
                            }
                            if (src.getType() != null) {
                                obj.addProperty("type", src.getType());
                            }
                            if (src.getError() != null) {
                                obj.add("error", context.serialize(src.getError()));
                            }
                            if (src.getRequestId() != null) {
                                obj.addProperty("requestId", src.getRequestId());
                            }
                            return obj;
                        }
                )
                .create();
        return gson.toJson(this);
    }
}
