/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

public enum RequestMethod {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    PATCH("PATCH"),
    DELETE("DELETE"),
    ;

    private final String requestMethod;


    RequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    @Override
    public String toString() {
        return requestMethod;
    }
}
