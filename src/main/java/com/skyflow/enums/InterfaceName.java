package com.skyflow.enums;

public enum InterfaceName {
    INSERT("insert"),
    DETOKENIZE("detokenize"),
    GET("get"),
    UPDATE("update"),
    DELETE("delete"),
    QUERY("query"),
    TOKENIZE("tokenize"),
    FILE_UPLOAD("file upload"),
    DETECT("detect"),
    INVOKE_CONNECTION("invoke connection");

    private final String interfaceName;

    InterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getName() {
        return interfaceName;
    }
}
