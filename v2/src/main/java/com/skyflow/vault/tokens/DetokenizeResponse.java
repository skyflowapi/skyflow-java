package com.skyflow.vault.tokens;

import com.skyflow.generated.rest.models.V1DetokenizeRecordResponse;
import com.skyflow.generated.rest.models.V1DetokenizeResponse;

import java.util.ArrayList;
import java.util.List;

public class DetokenizeResponse {
    private final V1DetokenizeResponse generatedResponse;
    private final ArrayList<DetokenizeRecordResponse> detokenizedFields;
    private final ArrayList<DetokenizeRecordResponse> errorRecords;

    public DetokenizeResponse(V1DetokenizeResponse response) {
        this.generatedResponse = response;
        this.detokenizedFields = new ArrayList<>();
        this.errorRecords = new ArrayList<>();
        this.createResponse();
    }

    public ArrayList<DetokenizeRecordResponse> getDetokenizedFields() {
        return detokenizedFields;
    }

    public ArrayList<DetokenizeRecordResponse> getErrors() {
        return errorRecords;
    }

    private void createResponse() {
        List<V1DetokenizeRecordResponse> records = this.generatedResponse.getRecords();
        if (records != null) {
            createSuccessResponse(records);
        }
    }

    private void createSuccessResponse(List<V1DetokenizeRecordResponse> records) {
        for (V1DetokenizeRecordResponse record : records) {
            DetokenizeRecordResponse recordResponse = new DetokenizeRecordResponse();
            recordResponse.setToken(record.getToken());
            if (record.getError() != null) {
                recordResponse.setError(record.getError());
                this.errorRecords.add(recordResponse);
            } else {
                recordResponse.setValue(record.getValue());
                recordResponse.setType(record.getValueType().getValue());
                this.detokenizedFields.add(recordResponse);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder response = new StringBuilder("{");
        response.append("\n\t").append("\"detokenizedFields\": ").append(toIndentedString(detokenizedFields));
        response.append("\n\t").append("\"errors\": \"").append(toIndentedString(errorRecords));
        response.append("\n}");
        return response.toString();
    }

    private String toIndentedString(Object o) {
        return o.toString().replace("\n", "\n\t");
    }
}
