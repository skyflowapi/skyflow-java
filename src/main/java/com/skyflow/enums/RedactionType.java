package com.skyflow.enums;

import com.skyflow.generated.rest.models.RedactionEnumREDACTION;

public enum RedactionType {
    PLAIN_TEXT(RedactionEnumREDACTION.PLAIN_TEXT),
    MASKED(RedactionEnumREDACTION.MASKED),
    DEFAULT(RedactionEnumREDACTION.DEFAULT),
    REDACTED(RedactionEnumREDACTION.REDACTED);

    private final RedactionEnumREDACTION redaction;

    RedactionType(RedactionEnumREDACTION redaction) {
        this.redaction = redaction;
    }

    public RedactionEnumREDACTION getRedaction() {
        return redaction;
    }

    @Override
    public String toString() {
        return String.valueOf(redaction);
    }
}
