package com.skyflow.enums;

import com.skyflow.generated.rest.models.V1BYOT;

public enum TokenMode {
    DISABLE(V1BYOT.DISABLE),
    ENABLE(V1BYOT.ENABLE),
    ENABLE_STRICT(V1BYOT.ENABLE_STRICT);

    private final V1BYOT byot;

    TokenMode(V1BYOT byot) {
        this.byot = byot;
    }

    public V1BYOT getBYOT() {
        return byot;
    }

    @Override
    public String toString() {
        return String.valueOf(byot);
    }
}
