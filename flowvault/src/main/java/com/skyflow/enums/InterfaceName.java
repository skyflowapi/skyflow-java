package com.skyflow.enums;

public enum InterfaceName {
    INSERT("insert"),
    DETOKENIZE("detokenize"),
    DELETE("delete tokens"),
    TOKENIZE("tokenize"),
    QUERY("query"),
    GET("get");


    private final String interfaceName;

    InterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getName() {
        return interfaceName;
    }
}
