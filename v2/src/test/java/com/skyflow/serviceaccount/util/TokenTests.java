package com.skyflow.serviceaccount.util;

import com.skyflow.Skyflow;
import com.skyflow.enums.LogLevel;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TokenTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";

    @BeforeClass
    public static void setup() {
        Skyflow skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).build();
    }

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
            Dotenv dotenv = Dotenv.load();
            String token = dotenv.get("TEST_EXPIRED_TOKEN");
            Assert.assertTrue(Token.isExpired(token));
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testExpiredJwtTokenForIsExpiredToken() {
        // 3-part fake JWT: middle is base64({"exp":1}) = eyJleHAiOjF9, exp=1970 → always expired
        Assert.assertTrue(Token.isExpired("x.eyJleHAiOjF9.y"));
    }

    @Test
    public void testValidJwtTokenForIsExpiredToken() {
        // 3-part fake JWT: middle is base64({"exp":9999999999}) = eyJleHAiOjk5OTk5OTk5OTl9, far-future
        Assert.assertFalse(Token.isExpired("x.eyJleHAiOjk5OTk5OTk5OTl9.y"));
    }
}
