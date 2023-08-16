/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

/**
 * Stores the generated bearer token.
 */
public class ResponseToken {
    private String accessToken;
    private String tokenType;

    /**
     * Retrieves the access token.
     * @return Returns the access token.
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Sets the access token.
     * @param accessToken Value of the access token.
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Gets the token type.
     * @return Returns the token type.
     */
    public String getTokenType() {
        return tokenType;
    }

    /**
     * Sets the token type.
     * @param tokenType Type of the token.
     */
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
