package com.skyflow.serviceaccount.util;

import org.junit.Assert;
import org.junit.Test;

public class TokenTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";

    @Test
    public void testNoTokenForIsExpiredToken() {
        try {
            Assert.assertTrue(Token.isExpired(null));
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testEmptyTokenForIsExpiredToken() {
        try {
            Assert.assertTrue(Token.isExpired(""));
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testInvalidTokenForIsExpiredToken() {
        try {
            Assert.assertTrue(Token.isExpired("invalid-token"));
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testExpiredTokenForIsExpiredToken() {
        try {
            String token = System.getProperty("TEST_EXPIRED_TOKEN");
            Assert.assertTrue(Token.isExpired(token));
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
