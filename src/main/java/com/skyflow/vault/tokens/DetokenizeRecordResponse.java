package com.skyflow.vault.tokens;

import com.skyflow.generated.rest.models.V1DetokenizeRecordResponse;

public class DetokenizeRecordResponse {
    private final String token;
    private final String value;
    private final String type;
    private final String error;

    public DetokenizeRecordResponse(V1DetokenizeRecordResponse record) {
        this.token = record.getToken();
        this.value = record.getValue().isEmpty() ? null : record.getValue();
        this.type = record.getValueType().getValue().equals("NONE") ? null : record.getValueType().getValue();
        this.error = record.getError();
    }

    public String getError() {
        return error;
    }

    public String getToken() {
        return token;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }
}