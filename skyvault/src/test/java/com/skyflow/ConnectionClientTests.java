package com.skyflow;

import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.errors.SkyflowException;
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

    @Test
    public void testSetBearerToken_withApiKey_setsAndReusesApiKey() {
        try {
            Credentials creds = new Credentials();
            creds.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
            ConnectionConfig config = new ConnectionConfig();
            config.setConnectionId("isolated-apikey-1");
            config.setConnectionUrl("https://test.isolated.url");
            config.setCredentials(creds);
            ConnectionClient client = new ConnectionClient(config, null);

            // First call: apiKey == null → setApiKey() sets it
            client.setBearerToken();
            Assert.assertEquals("sky-ab123-abcd1234cdef1234abcd4321cdef4321", client.apiKey);

            // Second call: apiKey != null → setApiKey() logs REUSE_API_KEY (line 60)
            client.setBearerToken();
            Assert.assertEquals("sky-ab123-abcd1234cdef1234abcd4321cdef4321", client.apiKey);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testSetBearerToken_withValidNonExpiredToken_reusesBearerToken() {
        try {
            // far-future JWT: base64({"exp":9999999999}) = eyJleHAiOjk5OTk5OTk5OTl9 — never expires
            Credentials creds = new Credentials();
            creds.setToken("x.eyJleHAiOjk5OTk5OTk5OTl9.y");
            ConnectionConfig config = new ConnectionConfig();
            config.setConnectionId("isolated-token-1");
            config.setConnectionUrl("https://test.isolated.url");
            config.setCredentials(creds);
            ConnectionClient client = new ConnectionClient(config, null);

            // First call: this.token == null → Token.isExpired(null)=true → generates token from creds.getToken()
            client.setBearerToken();
            Assert.assertEquals("x.eyJleHAiOjk5OTk5OTk5OTl9.y", client.token);

            // Second call: token not null, not empty, not expired → REUSE_BEARER_TOKEN else branch (line 52)
            client.setBearerToken();
            Assert.assertEquals("x.eyJleHAiOjk5OTk5OTk5OTl9.y", client.token);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testPrioritiseCredentials_credentialChange_resetsToken() {
        try {
            Credentials credentialsA = new Credentials();
            credentialsA.setToken("x.eyJleHAiOjk5OTk5OTk5OTl9.y");
            ConnectionConfig config = new ConnectionConfig();
            config.setConnectionId("isolated-change-1");
            config.setConnectionUrl("https://test.isolated.url");
            config.setCredentials(credentialsA);
            ConnectionClient client = new ConnectionClient(config, null);

            client.updateConnectionConfig(config); // sets finalCredentials = credentialsA (original=null → no reset)
            client.token = "cached-token-value"; // simulate previously obtained bearer token

            // Change to different credentials object
            Credentials credentialsB = new Credentials();
            credentialsB.setToken("different-token");
            config.setCredentials(credentialsB);

            client.updateConnectionConfig(config); // original=A, new=B → !A.equals(B) → reset (lines 83-84)
            Assert.assertNull(client.token);
            Assert.assertNull(client.apiKey);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testSetBearerToken_noCredentials_throwsEmptyCredentials() {
        ConnectionConfig config = new ConnectionConfig();
        config.setConnectionId("isolated-nocreds-1");
        config.setConnectionUrl("https://test.isolated.url");
        // No credentials on config, no commonCredentials
        ConnectionClient client = new ConnectionClient(config, null);
        try {
            client.setBearerToken();
            Assert.fail("Should have thrown SkyflowException");
        } catch (SkyflowException e) {
            // SkyflowException expected — message varies by environment
            // (EmptyCredentials when no .env, or credential error when .env provides creds)
        } catch (Exception e) {
            Assert.fail("Expected SkyflowException, got: " + e.getClass().getName());
        }
    }
}