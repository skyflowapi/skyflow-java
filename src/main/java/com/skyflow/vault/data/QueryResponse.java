package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.HashMap;

public class QueryResponse {
    private ArrayList<HashMap<String, Object>> fields;
    private ArrayList<HashMap<String, Object>> tokenizedData;

    public QueryResponse(ArrayList<HashMap<String, Object>> fields) {
        this.fields = fields;
    }

    public ArrayList<HashMap<String, Object>> getFields() {
        return fields;
    }

    @Override
    public String toString() {
        StringBuilder response = new StringBuilder("{");
        response.append("\n\t\"fields\": ").append(formatFields());
        return response.append("\n}").toString();
    }

    private String formatFields() {
        StringBuilder sb = new StringBuilder("[");
        for (int index = 0; index < fields.size(); index++) {
            HashMap<String, Object> map = fields.get(index);
            sb.append("{");
            for (String key : map.keySet()) {
                sb.append("\n\t\"").append(key).append("\": \"").append(map.get(key)).append("\",");
            }
            sb.append("\n\t\"tokenizedData\": ").append(tokenizedData);
            sb.append("\n}");
            if (index != fields.size() - 1) {
                sb.append(", ");
            }
        }
        return toIndentedString(sb.append("]"));
    }

    private String toIndentedString(Object o) {
        return o.toString().replace("\n", "\n\t");
    }

}
