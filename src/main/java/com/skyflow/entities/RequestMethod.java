/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

/**
 * Supported request methods.
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
     * Retrieves the set request method type.
     * @return Returns the request method type.
     */
    @Override
    public String toString() {
        return requestMethod;
    }
}
