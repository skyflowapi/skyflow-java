package com.skyflow.vault.tokens;

import com.skyflow.enums.RedactionType;
import com.skyflow.vault.data.BaseDetokenizeData;

public class DetokenizeData extends BaseDetokenizeData {
    private final RedactionType redactionType;

    public DetokenizeData(String token) {
       super(token);
        this.redactionType = RedactionType.DEFAULT;
    }

    public DetokenizeData(String token, RedactionType redactionType) {
        super(token);
        this.redactionType = redactionType == null ? RedactionType.DEFAULT : redactionType;
    }

    public String getToken() {
        return super.getToken();
    }

    public RedactionType getRedactionType() {
        return this.redactionType;
    }
}
