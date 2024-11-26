package com.skyflow.vault.tokens;

import java.util.ArrayList;

public class DetokenizeResponse {
    private final ArrayList<DetokenizeRecordResponse> detokenizedFields;
    private final ArrayList<DetokenizeRecordResponse> errorRecords;

    public DetokenizeResponse(ArrayList<DetokenizeRecordResponse> detokenizedFields, ArrayList<DetokenizeRecordResponse> errorRecords) {
        this.detokenizedFields = detokenizedFields;
        this.errorRecords = errorRecords;
    }

    public ArrayList<DetokenizeRecordResponse> getDetokenizedFields() {
        return detokenizedFields;
    }

    public ArrayList<DetokenizeRecordResponse> getErrors() {
        return errorRecords;
    }

    @Override
    public String toString() {
        StringBuilder response = new StringBuilder("{");
        response.append("\n\t").append("\"detokenizedFields\": ").append(toIndentedString(detokenizedFields));
        response.append("\n\t").append("\"errors\": ").append(toIndentedString(errorRecords));
        response.append("\n}");
        return response.toString();
    }

    private String toIndentedString(Object o) {
        return o.toString().replace("\n", "\n\t");
    }
}
