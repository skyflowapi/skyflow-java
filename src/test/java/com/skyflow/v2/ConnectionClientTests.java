package com.skyflow.v2;

import com.skyflow.v2.config.ConnectionConfig;
import com.skyflow.v2.config.Credentials;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConnectionClientTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static ConnectionClient connectionClient;
    private static String connectionID = null;
    private static String connectionURL = null;
    private static String apiKey = null;
    private static ConnectionConfig connectionConfig;

    @BeforeClass
    public static void setup() {
        connectionID = "connection123";
        connectionURL = "https://test.connection.url";
        apiKey = "sky-ab123-abcd1234cdef1234abcd4321cdef4321";

        Credentials credentials = new Credentials();
        credentials.setApiKey(apiKey);

        connectionConfig = new ConnectionConfig();
        connectionConfig.setConnectionId(connectionID);
        connectionConfig.setConnectionUrl(connectionURL);
        connectionClient = new ConnectionClient(connectionConfig, credentials);
    }

    @Test
    public void getConnectionConfig() {
        try {
            ConnectionConfig config = connectionClient.getConnectionConfig();
            Assert.assertEquals(connectionID, config.getConnectionId());
            Assert.assertEquals(connectionURL, config.getConnectionUrl());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testSetBearerToken() {
        try {
            Dotenv dotenv = Dotenv.load();
            String bearerToken = dotenv.get("TEST_REUSABLE_TOKEN");
            Credentials credentials = new Credentials();
            credentials.setToken(bearerToken);
            connectionConfig.setCredentials(credentials);
            connectionClient.updateConnectionConfig(connectionConfig);

            // regular scenario
            connectionClient.setBearerToken();

            // re-use scenario
            connectionClient.setBearerToken();
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testSetBearerTokenWithApiKey() {
        try {
            Credentials credentials = new Credentials();
            credentials.setApiKey(apiKey);
            connectionConfig.setCredentials(null);
            connectionClient.updateConnectionConfig(connectionConfig);
            connectionClient.setCommonCredentials(credentials);

            // regular scenario
            connectionClient.setBearerToken();

            // re-use scenario
            connectionClient.setBearerToken();
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testSetBearerTokenWithEnvCredentials() {
        try {
            connectionConfig.setCredentials(null);
            connectionClient.updateConnectionConfig(connectionConfig);
            connectionClient.setCommonCredentials(null);
            Assert.assertNull(connectionClient.getConnectionConfig().getCredentials());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}