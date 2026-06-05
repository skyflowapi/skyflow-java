package com.skyflow;

import com.skyflow.config.ConnectionConfig;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Constants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Tests for ConnectionClient's prioritiseCredentials dotenv path.
 *
 * These tests write a temporary .env file to exercise the code path where
 * no ConnectionConfig credentials and no common credentials are set, so the
 * code falls through to read from a .env file.
 */
public class ConnectionClientDotenvTests {

    private static final String ENV_FILE = ".env";
    private byte[] originalEnvContent;

    @Before
    public void saveEnvFileState() throws IOException {
        File f = new File(ENV_FILE);
        originalEnvContent = f.exists() ? Files.readAllBytes(Paths.get(ENV_FILE)) : null;
    }

    @After
    public void restoreEnvFile() throws IOException {
        if (originalEnvContent != null) {
            Files.write(Paths.get(ENV_FILE), originalEnvContent);
        } else {
            Files.deleteIfExists(Paths.get(ENV_FILE));
        }
    }

    private ConnectionClient buildClientWithNoCreds(String id) {
        ConnectionConfig config = new ConnectionConfig();
        config.setConnectionId(id);
        config.setConnectionUrl("https://test.dotenv.url");
        // No credentials on config, no commonCredentials
        return new ConnectionClient(config, null);
    }

    @Test
    public void testPrioritiseCredentials_dotenvReturnsCredentials_setsCredentials() throws Exception {
        // Write a .env file with a valid credentials string value
        try (FileWriter fw = new FileWriter(ENV_FILE)) {
            fw.write(Constants.ENV_CREDENTIALS_KEY_NAME + "={\"token\":\"env-token-value\"}\n");
        }

        ConnectionClient client = buildClientWithNoCreds("dotenv-valid-1");
        // updateConnectionConfig calls prioritiseCredentials which reads from .env
        client.updateConnectionConfig();
    }

    @Test
    public void testPrioritiseCredentials_dotenvReturnsNullKey_throwsSkyflowException() throws Exception {
        // Write a .env file WITHOUT the SKYFLOW_CREDENTIALS key
        try (FileWriter fw = new FileWriter(ENV_FILE)) {
            fw.write("SOME_OTHER_KEY=some_value\n");
        }

        ConnectionClient client = buildClientWithNoCreds("dotenv-null-1");
        // Null sysCredentials → SkyflowException thrown directly
        try {
            client.updateConnectionConfig();
            Assert.fail("Should have thrown SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertTrue(e.getMessage().contains(ErrorMessage.EmptyCredentials.getMessage()));
        } catch (RuntimeException e) {
            Assert.fail("Expected direct SkyflowException, not RuntimeException wrapping it");
        }
    }
}
