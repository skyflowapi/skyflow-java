package com.skyflow.config;

import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.utils.validations.Validations;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.skyflow.serviceaccount.util.Token")
public class ConnectionConfigTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String connectionID = null;
    private static String connectionURL = null;
    private static String invalidConnectionURL = null;
    private static Credentials credentials = null;

    @BeforeClass
    public static void setup() throws SkyflowException, NoSuchMethodException {
        PowerMockito.mockStatic(Token.class);
        PowerMockito.when(Token.isExpired("valid_token")).thenReturn(true);
        PowerMockito.when(Token.isExpired("not_a_valid_token")).thenReturn(false);
        PowerMockito.mock(ApiClient.class);

        connectionID = "connection123";
        connectionURL = "https://connection.url.com";
        invalidConnectionURL = "invalid.connection.url.com";

        credentials = new Credentials();
        credentials.setToken("valid-token");
    }

    @Test
    public void testValidConnectionConfigWithCredentials() {
        try {
            ConnectionConfig connectionConfig = new ConnectionConfig();
            connectionConfig.setConnectionId(connectionID);
            connectionConfig.setConnectionUrl(connectionURL);
            connectionConfig.setCredentials(credentials);
            Validations.validateConnectionConfig(connectionConfig);

            Assert.assertEquals(connectionID, connectionConfig.getConnectionId());
            Assert.assertEquals(connectionURL, connectionConfig.getConnectionUrl());
            Assert.assertNotNull(connectionConfig.getCredentials());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidConnectionConfigWithoutCredentials() {
        try {
            ConnectionConfig connectionConfig = new ConnectionConfig();
            connectionConfig.setConnectionId(connectionID);
            connectionConfig.setConnectionUrl(connectionURL);
            Validations.validateConnectionConfig(connectionConfig);

            Assert.assertEquals(connectionID, connectionConfig.getConnectionId());
            Assert.assertEquals(connectionURL, connectionConfig.getConnectionUrl());
            Assert.assertNull(connectionConfig.getCredentials());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testNoConnectionIdInConnectionConfig() {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        try {
            connectionConfig.setConnectionUrl(connectionURL);
            Validations.validateConnectionConfig(connectionConfig);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.InvalidConnectionId.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyConnectionIdInConnectionConfig() {
        try {
            ConnectionConfig connectionConfig = new ConnectionConfig();
            connectionConfig.setConnectionId("");
            connectionConfig.setConnectionUrl(connectionURL);
            Validations.validateConnectionConfig(connectionConfig);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.EmptyConnectionId.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNoConnectionURLInConnectionConfig() {
        try {
            ConnectionConfig connectionConfig = new ConnectionConfig();
            connectionConfig.setConnectionId(connectionID);
            Validations.validateConnectionConfig(connectionConfig);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.InvalidConnectionUrl.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyConnectionURLInConnectionConfig() {
        try {
            ConnectionConfig connectionConfig = new ConnectionConfig();
            connectionConfig.setConnectionId(connectionID);
            connectionConfig.setConnectionUrl("");
            Validations.validateConnectionConfig(connectionConfig);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.EmptyConnectionUrl.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInvalidConnectionURLInConnectionConfig() {
        try {
            ConnectionConfig connectionConfig = new ConnectionConfig();
            connectionConfig.setConnectionId(connectionID);
            connectionConfig.setConnectionUrl(invalidConnectionURL);
            Validations.validateConnectionConfig(connectionConfig);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.InvalidConnectionUrlFormat.getMessage(), e.getMessage());
        }
    }
}
