/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

import com.skyflow.errors.SkyflowException;

/**
 * This is the description for TokenProvider Interface.
 */
public interface TokenProvider {
    /**
     * This is the description for getBearerToken method.
     * @return This is the description of what the method returns.
     * @throws Exception This is the description for Exception.
     */
    String getBearerToken() throws Exception;
}
