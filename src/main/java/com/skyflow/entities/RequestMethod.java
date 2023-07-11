/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

/**
 * This is the description for RequestMethod Enum.
 */
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

    /**
     * This is the description for toString method.
     * @return This is the description of what the method returns.
     */
    @Override
    public String toString() {
        return requestMethod;
    }
}
