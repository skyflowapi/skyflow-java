package com.skyflow.vault.data;

import com.google.gson.internal.LinkedTreeMap;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.models.V1InsertRecordResponse;
import com.skyflow.generated.rest.models.V1RecordMetaProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InsertResponse {
    private final V1InsertRecordResponse generatedResponse;
    private final ArrayList<HashMap<String, String>> insertedFields;
    private final ArrayList<HashMap<String, String>> errorFields;

    public InsertResponse(V1InsertRecordResponse response) throws SkyflowException {
        this.generatedResponse = response;
        this.insertedFields = new ArrayList<>();
        this.errorFields = new ArrayList<>();
        this.createResponse();
    }

    public ArrayList<HashMap<String, String>> getInsertedFields() {
        return insertedFields;
    }

    public ArrayList<HashMap<String, String>> getErrorFields() {
        return errorFields;
    }

    private void createResponse() throws SkyflowException {
        List<V1RecordMetaProperties> records = this.generatedResponse.getRecords();
        if (records != null && !records.isEmpty()) {
            createSuccessResponse(records);
        }
    }

    private void createSuccessResponse(List<V1RecordMetaProperties> records) throws SkyflowException {
        try {
            for (V1RecordMetaProperties record : records) {
                HashMap<String, String> insertRecord = new HashMap<>();

                String skyflowId = record.getSkyflowId();
                insertRecord.put("skyflowId", skyflowId);

                /*
                Getting unchecked cast warning, however, this type is inferred
                from an exception trying to cast into another type. Therefore,
                this type cast will not fail.
                 */
                LinkedTreeMap<String, String> tokensMap = (LinkedTreeMap<String, String>) record.getTokens();
                if (tokensMap != null) {
                    for (String key : tokensMap.keySet()) {
                        insertRecord.put(key, tokensMap.get(key));
                    }
                }
                insertedFields.add(insertRecord);
            }
        } catch (Exception e) {
            throw new SkyflowException(e);
        }
    }

    @Override
    public String toString() {
        StringBuilder response = new StringBuilder("{");
        response.append("\n\t\"insertedFields\": ").append(formatRecords(insertedFields));
        response.append("\n\t\"errors\": ").append(formatRecords(errorFields));
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
