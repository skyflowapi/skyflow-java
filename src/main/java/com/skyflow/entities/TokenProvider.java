package com.skyflow.entities;

import com.skyflow.errors.SkyflowException;

public interface TokenProvider {
    String getBearerToken() throws Exception;
}
