package com.skyflow.vault.tokens;


import com.skyflow.generated.rest.types.V1DetokenizeRecordResponse;
import com.skyflow.vault.data.BaseDetokenizeRecordResponse;

public class DetokenizeRecordResponse extends BaseDetokenizeRecordResponse {
    private final String type;
    private final String requestId;
    private final String value;

    public DetokenizeRecordResponse(V1DetokenizeRecordResponse record) {
        this(record, null);
    }

    public DetokenizeRecordResponse(V1DetokenizeRecordResponse record, String requestId) {
        super(record.getToken().orElse(null), record.getError().orElse(null));
        this.value = record.getValue()
                .filter(val -> val != null && !val.toString().isEmpty())
                .orElse(null);
        this.type = record.getValueType()
                .map(Enum::toString)
                .filter(val -> !"NONE".equals(val))
                .orElse(null);

        this.requestId = requestId;
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