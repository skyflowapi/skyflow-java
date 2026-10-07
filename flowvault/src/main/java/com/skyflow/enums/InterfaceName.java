package com.skyflow.enums;

public enum InterfaceName {
    INSERT("insert"),
    UPDATE("update"),
    DETOKENIZE("detokenize"),
    DELETE("delete tokens"),
    DELETE_RECORDS("delete"),
    TOKENIZE("tokenize"),
    GET("get"),
    QUERY("query"),
    GET_TOKENS("get tokens"),
    UPLOAD_FILES("upload files"),
    DELETE_FILES("delete files");


    private final String interfaceName;

    InterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getName() {
        return interfaceName;
    }
}
