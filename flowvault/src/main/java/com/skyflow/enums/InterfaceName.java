package com.skyflow.enums;

public enum InterfaceName {
    INSERT("insert"),
    UPDATE("update"),
    DETOKENIZE("detokenize"),
    DELETE("delete tokens"),
    DELETE_RECORDS("delete"),
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
