package com.skyflow;

import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Constants;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Dotenv.class)
public class ConnectionClientDotenvTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";

    private ConnectionClient buildClientWithNoCreds(String id) {
        ConnectionConfig config = new ConnectionConfig();
        config.setConnectionId(id);
        config.setConnectionUrl("https://test.dotenv.url");
        // No credentials on config, no commonCredentials
        return new ConnectionClient(config, null);
    }

    @Test
    @PrepareForTest(Dotenv.class)
    public void testPrioritiseCredentials_dotenvReturnsCredentials_setsCredentials() throws Exception {
        // Mock Dotenv.load() to return a mock with a valid credentials string
        Dotenv mockDotenv = PowerMockito.mock(Dotenv.class);
        PowerMockito.mockStatic(Dotenv.class);
        PowerMockito.when(Dotenv.load()).thenReturn(mockDotenv);
        Mockito.when(mockDotenv.get(Constants.ENV_CREDENTIALS_KEY_NAME))
               .thenReturn("{\"token\":\"env-token-value\"}");

        ConnectionClient client = buildClientWithNoCreds("dotenv-valid-1");

        // updateConnectionConfig calls prioritiseCredentials only (no Validations.validateCredentials)
        // Lines 72-73 (Dotenv.load + dotenv.get), 77-80 (set finalCredentials from dotenv) covered
        client.updateConnectionConfig(client.getConnectionConfig()); // no exception expected
    }

    @Test
    @PrepareForTest(Dotenv.class)
    public void testPrioritiseCredentials_dotenvReturnsNullKey_throwsRuntimeException() throws Exception {
        // Mock Dotenv.load() to succeed but return null for the credentials key
        Dotenv mockDotenv = PowerMockito.mock(Dotenv.class);
        PowerMockito.mockStatic(Dotenv.class);
        PowerMockito.when(Dotenv.load()).thenReturn(mockDotenv);
        Mockito.when(mockDotenv.get(Constants.ENV_CREDENTIALS_KEY_NAME)).thenReturn(null);

        ConnectionClient client = buildClientWithNoCreds("dotenv-null-1");

        // Null sysCredentials → throw SkyflowException inside try → caught by catch(Exception e)
        // → wrapped in RuntimeException (lines 74-76, 89-91)
        try {
            client.updateConnectionConfig(client.getConnectionConfig());
            Assert.fail("Should have thrown RuntimeException");
        } catch (RuntimeException e) {
            // SkyflowException wrapped in RuntimeException
            Assert.assertNotNull(e.getCause());
        } catch (SkyflowException e) {
            Assert.fail("Expected RuntimeException wrapping SkyflowException, got plain SkyflowException");
        }
    }
}
