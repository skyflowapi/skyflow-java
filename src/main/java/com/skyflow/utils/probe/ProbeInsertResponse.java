package com.skyflow.utils.probe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Response returned from an insert operation.
 */
public class ProbeInsertResponse {

    private List<Map<String, Object>> insertedFields;
    private List<String> errors;

    public ProbeInsertResponse(List<Map<String, Object>> rawRecords) {
        this.insertedFields = new ArrayList<>();
        this.errors = new ArrayList<>();
        for (Map<String, Object> record : rawRecords) {
            Map<String, Object> normalised = new HashMap<>();
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                if (entry.getKey().equals("skyflow_id")) {
                    normalised.put("skyflowId", entry.getValue());
                } else {
                    normalised.put(entry.getKey(), entry.getValue());
                }
            }
            insertedFields.add(normalised);
        }
    }

    public List<Map<String, Object>> getInsertedFields() {
        return insertedFields;
    }

    public List<String> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{\"records\":[");
        for (int i = 0; i < insertedFields.size(); i++) {
            Map<String, Object> record = insertedFields.get(i);
            sb.append("{");
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                String key = entry.getKey().equals("skyflow_id") ? "skyflowId" : entry.getKey();
                sb.append("\"").append(key).append("\":\"").append(entry.getValue()).append("\",");
            }
            sb.append("}");
            if (i < insertedFields.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]}");
        return sb.toString();
    }
}
