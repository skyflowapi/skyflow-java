package com.skyflow.vault.tokens;


import com.skyflow.generated.rest.types.V1DetokenizeRecordResponse;

public class DetokenizeRecordResponse {
    private final String token;
    private final String value;
    private final String type;
    private final String error;
    private final String requestId;

    public DetokenizeRecordResponse(V1DetokenizeRecordResponse record) {
        this(record, null);
    }

    public DetokenizeRecordResponse(V1DetokenizeRecordResponse record, String requestId) {
        this.token = record.getToken().orElse(null);

        this.value = record.getValue()
                .filter(val -> val != null && !val.toString().isEmpty())
                .orElse(null);

        this.type = record.getValueType()
                .map(Enum::toString)
                .filter(val -> !"NONE".equals(val))
                .orElse(null);

        this.error = record.getError().orElse(null);

        this.requestId = requestId;
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

    public String getRequestId() {
        return requestId;
    }
}