package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.HashMap;

public class InsertResponse {
    private final ArrayList<HashMap<String, Object>> insertedFields;
    private final ArrayList<HashMap<String, Object>> errorFields;

    public InsertResponse(ArrayList<HashMap<String, Object>> insertedFields, ArrayList<HashMap<String, Object>> errorFields) {
        this.insertedFields = insertedFields;
        this.errorFields = errorFields;
    }

    public ArrayList<HashMap<String, Object>> getInsertedFields() {
        return insertedFields;
    }

    public ArrayList<HashMap<String, Object>> getErrorFields() {
        return errorFields;
    }

    @Override
    public String toString() {
        StringBuilder response = new StringBuilder("{");
        response.append("\n\t\"insertedFields\": ").append(formatRecords(insertedFields));
        response.append("\n\t\"errors\": ").append(formatRecords(errorFields));
        response.append("\n}");
        return response.toString();
    }

    private String formatRecords(ArrayList<HashMap<String, Object>> records) {
        StringBuilder sb = new StringBuilder("[");
        for (int index = 0; index < records.size(); index++) {
            HashMap<String, Object> map = records.get(index);
            sb.append("{");
            for (String key : map.keySet()) {
                sb.append("\n\t\"").append(key).append("\": \"").append(map.get(key)).append("\",");
            }
            sb.append("\n}");
            if (index != records.size() - 1) {
                sb.append(", ");
            }
        }
        return toIndentedString(sb.append("]"));
    }

    private String toIndentedString(Object o) {
        return o.toString().replace("\n", "\n\t");
    }
}
