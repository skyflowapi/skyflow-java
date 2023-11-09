/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.common.utils;

import com.skyflow.entities.TokenProvider;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;

class InvalidTokenProvider implements TokenProvider {

    private final boolean isException;

    public InvalidTokenProvider() {
        this.isException = false;
    }

    public InvalidTokenProvider(boolean isException) {
        this.isException = isException;
    }

    @Override
    public String getBearerToken() throws Exception {
        if (!isException)
            return "invalid_token";
        else
            throw new Exception("EXCEPTION THROWN!!");
    }
}

class InvalidTokenProvider2 implements TokenProvider {
    @Override
    public String getBearerToken() throws Exception {
        return "a.b.c";
    }
}

@RunWith(PowerMockRunner.class)
@PrepareForTest
public class TokenUtilsTest {

    TokenUtils tokenUtils = new TokenUtils();

    @Test
    public void testInvalidGetBearerToken() {
        try {
            String token = tokenUtils.getBearerToken(new InvalidTokenProvider());
        } catch (SkyflowException e) {
            assertEquals(e.getMessage(), ErrorCode.InvalidBearerToken.getDescription());
        }
    }

    @Test
    @PrepareForTest
    public void testInvalidGetBearerToken2() {
        try {
            String token = tokenUtils.getBearerToken(new InvalidTokenProvider2());
        } catch (SkyflowException e) {
            assertEquals(e.getMessage(), ErrorCode.InvalidBearerToken.getDescription());
        }
    }

    @Test
    public void testIsTokenValid() {
        try {
            String token = System.getProperty("TEST_EXPIRED_TOKEN");
            assertEquals(false, TokenUtils.isTokenValid(token));
        } catch (SkyflowException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetBearerTokenException() {
        try {
            String token = tokenUtils.getBearerToken(new InvalidTokenProvider(true));
        } catch (SkyflowException e) {
            assertEquals(e.getMessage(), "EXCEPTION THROWN!!");
        }
    }
}
