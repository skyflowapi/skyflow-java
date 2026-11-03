package com.skyflow.serviceaccount.util;

import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.logger.LogUtil;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TokenTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";

    @BeforeClass
    public static void setup() {
        LogUtil.setupLogger(LogLevel.DEBUG);
    }

    private static String unsignedJwt(String payloadJson) {
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        String header = enc.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = enc.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".sig";
    }

    private static String jwtExpiringIn(long seconds) {
        return unsignedJwt("{\"exp\":" + (System.currentTimeMillis() / 1000 + seconds) + "}");
    }

    @Test
    public void testIsExpiringWithin_expiredTokenIsDue() {
        Assert.assertTrue(Token.isExpiringWithin(jwtExpiringIn(-5), 60));
    }

    @Test
    public void testIsExpiringWithin_tokenInsideWindowIsDue() {
        Assert.assertTrue(Token.isExpiringWithin(jwtExpiringIn(30), 60));
    }

    @Test
    public void testIsExpiringWithin_tokenBeyondWindowIsNotDue() {
        Assert.assertFalse(Token.isExpiringWithin(jwtExpiringIn(600), 60));
    }

    @Test
    public void testIsExpiringWithin_zeroWindowMatchesExpiryOnly() {
        Assert.assertTrue(Token.isExpiringWithin(jwtExpiringIn(-1), 0));
        Assert.assertFalse(Token.isExpiringWithin(jwtExpiringIn(5), 0));
    }

    @Test
    public void testIsExpiringWithin_nullEmptyMalformedAndNoExpAreNotDue() {
        Assert.assertFalse(Token.isExpiringWithin(null, 60));
        Assert.assertFalse(Token.isExpiringWithin("", 60));
        Assert.assertFalse(Token.isExpiringWithin("not-a-jwt", 60));
        Assert.assertFalse(Token.isExpiringWithin("a.b", 60));
        Assert.assertFalse(Token.isExpiringWithin(unsignedJwt("{\"sub\":\"x\"}"), 60));
        Assert.assertFalse(Token.isExpiringWithin(unsignedJwt("{\"exp\":\"soon\"}"), 60));
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
        // Skip rather than fail when no local .env fixture provides a real expired token —
        // testExpiredJwtTokenForIsExpiredToken below already covers this behavior without one.
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String token = dotenv.get("TEST_EXPIRED_TOKEN");
        Assume.assumeNotNull(token);
        Assert.assertTrue(Token.isExpired(token));
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

    @Test
    public void testDecodedMalformedBase64ThrowsSkyflowException() {
        try {
            Token.decoded("x.not-valid-base64!!!.y");
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            // expected: malformed base64 now surfaces as SkyflowException, not IllegalArgumentException
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN + ": " + e);
        }
    }

    @Test
    public void testDecodedNonObjectJsonThrowsSkyflowException() {
        // middle segment is base64({[1,2,3]}) -> a valid JSON array, not a JSON object
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("[1,2,3]".getBytes(StandardCharsets.UTF_8));
        try {
            Token.decoded("x." + payload + ".y");
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            // expected: a non-object JSON payload now surfaces as SkyflowException, not IllegalStateException
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN + ": " + e);
        }
    }

    @Test
    public void testIsExpiredTokenMissingExpClaimReturnsTrueWithoutException() {
        // middle segment is base64({"foo":"bar"}) -> valid JSON object, but no "exp" claim
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8));
        try {
            Assert.assertTrue(Token.isExpired("x." + payload + ".y"));
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN + ": " + e);
        }
    }
}
