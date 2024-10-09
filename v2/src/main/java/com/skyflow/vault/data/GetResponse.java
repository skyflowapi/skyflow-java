package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.HashMap;

public class GetResponse {
    private final ArrayList<HashMap<String, String>> data;
    private final ArrayList<HashMap<String, String>> errors;

    public GetResponse(ArrayList<HashMap<String, String>> data, ArrayList<HashMap<String, String>> errors) {
        this.data = data;
        this.errors = errors;
    }

    public ArrayList<HashMap<String, String>> getData() {
        return data;
    }

    public ArrayList<HashMap<String, String>> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        StringBuilder response = new StringBuilder("{");
        response.append("\n\t\"data\": ").append(formatRecords(data));
        response.append("\n\t\"errors\": ").append(formatRecords(errors));
        response.append("\n}");
        return response.toString();
    }

    private String formatRecords(ArrayList<HashMap<String, String>> records) {
        StringBuilder sb = new StringBuilder("[");
        for (int index = 0; index < records.size(); index++) {
            HashMap<String, String> map = records.get(index);
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
