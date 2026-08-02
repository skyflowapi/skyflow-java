package com.skyflow.enums;

public enum InterfaceName {
    INSERT("insert"),
    DETOKENIZE("detokenize"),
    DELETE("delete tokens"),
    TOKENIZE("tokenize");


    private final String interfaceName;

    InterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getName() {
        return interfaceName;
    }
}
