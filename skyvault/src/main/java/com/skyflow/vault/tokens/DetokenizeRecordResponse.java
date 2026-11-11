package com.skyflow.vault.tokens;


import com.skyflow.generated.rest.types.V1DetokenizeRecordResponse;
import com.skyflow.vault.data.BaseDetokenizeRecordResponse;

import java.util.Optional;

public class DetokenizeRecordResponse extends BaseDetokenizeRecordResponse {
    private final String type;
    private final String requestId;
    private final String value;

    public DetokenizeRecordResponse(com.skyflow.generated.rest.types.DetokenizeRecordResponse record) {
        this(record, null);
    }

    public DetokenizeRecordResponse(com.skyflow.generated.rest.types.DetokenizeRecordResponse record, String requestId) {
        this(record.getToken(), record.getError(), record.getValue(), record.getValueType().map(Object::toString), requestId);
    }

    /** Kept for binary compatibility with callers built against the previous generated model. */
    public DetokenizeRecordResponse(V1DetokenizeRecordResponse record) {
        this(record, null);
    }

    /** Kept for binary compatibility with callers built against the previous generated model. */
    public DetokenizeRecordResponse(V1DetokenizeRecordResponse record, String requestId) {
        this(record.getToken(), record.getError(), record.getValue(), record.getValueType().map(Object::toString), requestId);
    }

    private DetokenizeRecordResponse(Optional<String> token, Optional<String> error, Optional<String> value,
                                     Optional<String> valueType, String requestId) {
        super(token.orElse(null), error.orElse(null));
        this.value = value
                .filter(val -> val != null && !val.isEmpty())
                .orElse(null);
        this.type = valueType
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
