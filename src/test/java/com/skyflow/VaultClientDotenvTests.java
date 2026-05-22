package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
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
 * Tests for VaultClient's prioritiseCredentials dotenv path.
 *
 * These tests write a temporary .env file to exercise the code path where
 * no VaultConfig credentials and no common credentials are set, so the code
 * falls through to read from a .env file.
 */
public class VaultClientDotenvTests {

    private static final String ENV_FILE = ".env";
    private boolean envFileExistedBefore;

    @Before
    public void recordEnvFileState() {
        envFileExistedBefore = new File(ENV_FILE).exists();
    }

    @After
    public void restoreEnvFile() throws IOException {
        // Remove our test .env file unless it existed before the test
        if (!envFileExistedBefore) {
            Files.deleteIfExists(Paths.get(ENV_FILE));
        }
    }

    private VaultClient buildClientWithNoCreds(String vaultId, String clusterId) {
        VaultConfig config = new VaultConfig();
        config.setVaultId(vaultId);
        config.setClusterId(clusterId);
        config.setEnv(Env.DEV);
        // No credentials set
        return new VaultClient(config, null);
    }

    /**
     * Covers the dotenv success path: Dotenv.load() succeeds and returns a
     * non-null credentials string, so finalCredentials is set via
     * credentialsString. Lines ~862-870 of VaultClient.java.
     */
    @Test
    public void testPrioritiseCredentials_dotenvReturnsCredentials_setsCredentials() throws Exception {
        // Write a .env file with a valid credentials string value
        try (FileWriter fw = new FileWriter(ENV_FILE)) {
            fw.write(Constants.ENV_CREDENTIALS_KEY_NAME + "={\"token\":\"env-token-value\"}\n");
        }

        VaultClient client = buildClientWithNoCreds("dotenv-vault-1", "cluster1");
        // updateVaultConfig() calls prioritiseCredentials() which reads from .env
        // Should not throw since sysCredentials is non-null
        client.updateVaultConfig();

        // finalCredentials should be set with credentials string
        java.lang.reflect.Field field = VaultClient.class.getDeclaredField("finalCredentials");
        field.setAccessible(true);
        Credentials finalCreds = (Credentials) field.get(client);
        Assert.assertNotNull(finalCreds);
        Assert.assertEquals("{\"token\":\"env-token-value\"}", finalCreds.getCredentialsString());
    }

    /**
     * Covers the path where dotenv loads but the key is absent (returns null),
     * causing SkyflowException(EmptyCredentials) to be thrown inside the try
     * block → caught by catch(Exception e) → re-thrown as RuntimeException.
     * Lines ~864-876 of VaultClient.java.
     */
    @Test
    public void testPrioritiseCredentials_dotenvKeyMissing_throwsRuntimeException() throws Exception {
        // Write a .env file WITHOUT the SKYFLOW_CREDENTIALS key
        try (FileWriter fw = new FileWriter(ENV_FILE)) {
            fw.write("SOME_OTHER_KEY=some_value\n");
        }

        VaultClient client = buildClientWithNoCreds("dotenv-vault-2", "cluster2");
        try {
            client.updateVaultConfig();
            Assert.fail("Should have thrown RuntimeException");
        } catch (RuntimeException e) {
            // Expected: SkyflowException(EmptyCredentials) was wrapped as RuntimeException
            Assert.assertNotNull(e.getCause());
            Assert.assertTrue(e.getCause() instanceof SkyflowException);
        } catch (SkyflowException e) {
            Assert.fail("Expected RuntimeException wrapping SkyflowException, not direct SkyflowException");
        }
    }
}
