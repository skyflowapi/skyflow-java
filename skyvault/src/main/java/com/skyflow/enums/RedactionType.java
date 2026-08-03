package com.skyflow.enums;


import com.skyflow.generated.rest.types.RedactionEnumRedaction;

public enum RedactionType {
    PLAIN_TEXT(RedactionEnumRedaction.PLAIN_TEXT),
    MASKED(RedactionEnumRedaction.MASKED),
    DEFAULT(RedactionEnumRedaction.DEFAULT),
    REDACTED(RedactionEnumRedaction.REDACTED);

    private final RedactionEnumRedaction redaction;

    RedactionType(RedactionEnumRedaction redaction) {
        this.redaction = redaction;
    }

    public RedactionEnumRedaction getRedaction() {
        return redaction;
    }

    @Override
    public String toString() {
        return String.valueOf(redaction);
    }
}
