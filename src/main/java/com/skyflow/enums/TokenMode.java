package com.skyflow.enums;

import com.skyflow.generated.rest.types.V1Byot;

public enum TokenMode {
    DISABLE(V1Byot.DISABLE),
    ENABLE(V1Byot.ENABLE),
    ENABLE_STRICT(V1Byot.ENABLE_STRICT);

    private final V1Byot byot;

    TokenMode(V1Byot byot) {
        this.byot = byot;
    }

    public V1Byot getBYOT() {
        return byot;
    }

    @Override
    public String toString() {
        return byot.toString();
    }
}
