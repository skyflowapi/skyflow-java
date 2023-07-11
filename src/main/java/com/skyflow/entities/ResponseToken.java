/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

/**
 * This is the description for ResponseToken Class.
 */
public class ResponseToken {
    private String accessToken;
    private String tokenType;

    /**
     * This is the description for getAccessToken method.
     * @return This is the description of what the method returns.
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * This is the description for setAccessToken method.
     * @param accessToken This is the description of the accessToken parameter.
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * This is the description for getTokenType method.
     * @return This is the description of what the method returns.
     */
    public String getTokenType() {
        return tokenType;
    }

    /**
     * This is the description for setTokenType method.
     * @param tokenType This is the description of the tokenType parameter.
     */
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
