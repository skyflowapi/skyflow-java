package com.skyflow.config;

import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.validations.Validations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.skyflow.utils.Utils;

public class CredentialsTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String path = null;
    private static String credentialsString = null;
    private static String token = null;
    private static String validApiKey = null;
    private static String invalidApiKey = null;
    private static ArrayList<String> roles = null;
    private static String role = null;
    private static String context = null;

    @BeforeClass
    public static void setup() {
        path = "valid-path-to-credentials-file";
        credentialsString = "valid-credentials-string";
        token = "valid-token";
        validApiKey = "sky-ab123-abcd1234cdef1234abcd4321cdef4321";
        invalidApiKey = "invalid-api-key";
        roles = new ArrayList<>();
        role = "test_credentials_role";
        context = "test_context_value";
    }

    @Before
    public void setupTest() {
        roles.clear();
    }

    @Test
    public void testValidCredentialsWithPath() {
        try {
            Credentials credentials = new Credentials();
            credentials.setPath(path);
            Validations.validateCredentials(credentials);
            Assert.assertNull(credentials.getCredentialsString());
            Assert.assertNull(credentials.getToken());
            Assert.assertNull(credentials.getApiKey());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidCredentialsWithCredentialsString() {
        try {
            Credentials credentials = new Credentials();
            credentials.setCredentialsString(credentialsString);
            Validations.validateCredentials(credentials);
            Assert.assertNull(credentials.getPath());
            Assert.assertNull(credentials.getToken());
            Assert.assertNull(credentials.getApiKey());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidCredentialsWithToken() {
        try {
            Credentials credentials = new Credentials();
            credentials.setToken(token);
            Validations.validateCredentials(credentials);
            Assert.assertNull(credentials.getPath());
            Assert.assertNull(credentials.getCredentialsString());
            Assert.assertNull(credentials.getApiKey());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidCredentialsWithApikey() {
        try {
            Credentials credentials = new Credentials();
            credentials.setApiKey(validApiKey);
            Validations.validateCredentials(credentials);
            Assert.assertNull(credentials.getPath());
            Assert.assertNull(credentials.getCredentialsString());
            Assert.assertNull(credentials.getToken());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidCredentialsWithRolesAndContext() {
        try {
            roles.add(role);
            Credentials credentials = new Credentials();
            credentials.setApiKey(validApiKey);
            credentials.setRoles(roles);
            credentials.setContext(context);
            Validations.validateCredentials(credentials);
            Assert.assertNull(credentials.getPath());
            Assert.assertNull(credentials.getCredentialsString());
            Assert.assertNull(credentials.getToken());
            Assert.assertEquals(context, credentials.getContext());
            Assert.assertEquals(1, credentials.getRoles().size());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testEmptyPathInCredentials() {
        try {
            Credentials credentials = new Credentials();
            credentials.setPath("");
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyCredentialFilePath.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyCredentialsStringInCredentials() {
        try {
            Credentials credentials = new Credentials();
            credentials.setCredentialsString("");
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyCredentialsString.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyTokenInCredentials() {
        try {
            Credentials credentials = new Credentials();
            credentials.setToken("");
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyToken.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyApikeyInCredentials() {
        try {
            Credentials credentials = new Credentials();
            credentials.setApiKey("");
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyApikey.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInvalidApikeyInCredentials() {
        try {
            Credentials credentials = new Credentials();
            credentials.setApiKey(invalidApiKey);
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.InvalidApikey.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testBothTokenAndPathInCredentials() {
        try {
            Credentials credentials = new Credentials();
            credentials.setPath(path);
            credentials.setToken(token);
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.MultipleTokenGenerationMeansPassed.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNothingPassedInCredentials() {
        try {
            Credentials credentials = new Credentials();
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.NoTokenGenerationMeansPassed.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyRolesInCredentials() {
        try {
            Credentials credentials = new Credentials();
            credentials.setPath(path);
            credentials.setRoles(roles);
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyRoles.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNullRoleInRolesInCredentials() {
        try {
            Credentials credentials = new Credentials();
            credentials.setPath(path);
            roles.add(role);
            roles.add(null);
            credentials.setRoles(roles);
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyRoleInRoles.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyRoleInRolesInCredentials() {
        try {
            Credentials credentials = new Credentials();
            credentials.setPath(path);
            roles.add(role);
            roles.add("");
            credentials.setRoles(roles);
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyRoleInRoles.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyContextInCredentials() {
        try {
            Credentials credentials = new Credentials();
            credentials.setPath(path);
            credentials.setContext("");
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyContext.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidMapContextInCredentials() {
        try {
            Credentials credentials = new Credentials();
            credentials.setPath(path);
            Map<String, Object> ctxMap = new HashMap<>();
            ctxMap.put("role", "admin");
            ctxMap.put("department", "finance");
            ctxMap.put("user_id", "user_12345");
            credentials.setContext(ctxMap);
            Validations.validateCredentials(credentials);
            Assert.assertNull(credentials.getContext());
            Assert.assertEquals(ctxMap, credentials.getContextAsObject());
            Assert.assertEquals(ctxMap, credentials.getContextAsMap());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testEmptyMapContextInCredentials() {
        try {
            Credentials credentials = new Credentials();
            credentials.setPath(path);
            Map<String, Object> ctxMap = new HashMap<>();
            credentials.setContext(ctxMap);
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyContext.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInvalidMapKeyInContextCredentials() {
        try {
            Credentials credentials = new Credentials();
            credentials.setPath(path);
            Map<String, Object> ctxMap = new HashMap<>();
            ctxMap.put("valid_key", "value");
            ctxMap.put("invalid-key", "value");
            credentials.setContext(ctxMap);
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertTrue(e.getMessage().contains("invalid-key"));
        }
    }

    @Test
    public void testMapContextWithNestedObjects() {
        try {
            Credentials credentials = new Credentials();
            credentials.setPath(path);
            Map<String, Object> nested = new HashMap<>();
            nested.put("level", 2);
            Map<String, Object> ctxMap = new HashMap<>();
            ctxMap.put("role", "admin");
            ctxMap.put("metadata", nested);
            credentials.setContext(ctxMap);
            Validations.validateCredentials(credentials);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testMapContextWithMixedValueTypes() {
        try {
            Credentials credentials = new Credentials();
            credentials.setPath(path);
            Map<String, Object> ctxMap = new HashMap<>();
            ctxMap.put("role", "admin");
            ctxMap.put("level", 3);
            ctxMap.put("active", true);
            ctxMap.put("timestamp", "2025-12-25T10:30:00Z");
            credentials.setContext(ctxMap);
            Validations.validateCredentials(credentials);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

}
