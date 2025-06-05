package com.skyflow.enums;

public enum MaskingMethod {
    BLACKBOX("blackbox"),
    BLUR("blur");

    private final String maskingMethod;

    MaskingMethod(String maskingMethod) {
        this.maskingMethod = maskingMethod;
    }

    public String getMaskingMethod() {
        return maskingMethod;
    }

    @Override
    public String toString() {
        return maskingMethod;
    }
}
