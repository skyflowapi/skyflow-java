/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

import com.skyflow.errors.SkyflowException;

/**
 * Defines the behavior of a class that provides a bearer token.
 */
public interface TokenProvider {
    /**
     * Gets a bearer token.
     * @return Returns the stringified bearer token.
     * @throws Exception Throws an exception when we encounter any error scenario.
     */
    String getBearerToken() throws Exception;
}
