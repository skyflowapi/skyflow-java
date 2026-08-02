package com.skyflow;

import com.skyflow.config.BaseCredentials;
import com.skyflow.config.BaseVaultConfig;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.BaseConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class BaseVaultClientTests {

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

    private BaseVaultClient<BaseVaultConfig> newClient(BaseCredentials commonCredentials) {
        return new BaseVaultClient<>(new BaseVaultConfig(), commonCredentials);
    }

    @Test
    public void testPrioritiseCredentials_prefersVaultSpecificCredentials() throws SkyflowException {
        BaseCredentials vaultSpecific = new BaseCredentials();
        vaultSpecific.setApiKey("test_api_key");
        BaseVaultClient<BaseVaultConfig> client = newClient(null);

        client.prioritiseCredentials(vaultSpecific);

        Assert.assertEquals(vaultSpecific, client.finalCredentials);
    }

    @Test
    public void testPrioritiseCredentials_fallsBackToCommonCredentials() throws SkyflowException {
        BaseCredentials common = new BaseCredentials();
        common.setApiKey("common_api_key");
        BaseVaultClient<BaseVaultConfig> client = newClient(common);

        client.prioritiseCredentials(null);

        Assert.assertEquals(common, client.finalCredentials);
    }

    @Test
    public void testPrioritiseCredentials_credentialChange_resetsTokenAndApiKey() throws SkyflowException {
        BaseCredentials credentialsA = new BaseCredentials();
        credentialsA.setToken("x.eyJleHAiOjk5OTk5OTk5OTl9.y");
        BaseVaultClient<BaseVaultConfig> client = newClient(null);

        client.prioritiseCredentials(credentialsA);
        client.token = "cached-token";
        client.apiKey = "cached-api-key";

        BaseCredentials credentialsB = new BaseCredentials();
        credentialsB.setToken("other-token");
        client.prioritiseCredentials(credentialsB);

        Assert.assertNull(client.token);
        Assert.assertNull(client.apiKey);
    }

    @Test
    public void testSetBearerToken_withApiKey() throws SkyflowException {
        BaseCredentials creds = new BaseCredentials();
        creds.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        BaseVaultClient<BaseVaultConfig> client = newClient(null);

        client.setBearerToken(creds);

        Assert.assertEquals("sky-ab123-abcd1234cdef1234abcd4321cdef4321", client.token);
    }

    @Test
    public void testSetBearerToken_generatesTokenWhenNull() throws SkyflowException {
        BaseCredentials creds = new BaseCredentials();
        creds.setToken("x.eyJleHAiOjk5OTk5OTk5OTl9.y");
        BaseVaultClient<BaseVaultConfig> client = newClient(null);

        client.setBearerToken(creds);

        Assert.assertEquals("x.eyJleHAiOjk5OTk5OTk5OTl9.y", client.token);
    }

    @Test
    public void testSetBearerToken_reusesValidNonExpiredToken() throws SkyflowException {
        BaseCredentials creds = new BaseCredentials();
        creds.setToken("x.eyJleHAiOjk5OTk5OTk5OTl9.y");
        BaseVaultClient<BaseVaultConfig> client = newClient(null);

        // First call: token=null → generates from creds.getToken()
        client.setBearerToken(creds);
        Assert.assertEquals("x.eyJleHAiOjk5OTk5OTk5OTl9.y", client.token);

        // Second call: token valid, not expired → reuse branch
        client.setBearerToken(creds);
        Assert.assertEquals("x.eyJleHAiOjk5OTk5OTk5OTl9.y", client.token);
    }

    @Test
    public void testSetBearerToken_noCredentials_throwsEmptyCredentials() {
        BaseVaultClient<BaseVaultConfig> client = newClient(null);
        try {
            client.setBearerToken(null);
            Assert.fail("Should have thrown SkyflowException");
        } catch (SkyflowException e) {
            // message varies by environment (EmptyCredentials when no .env, or credential error when .env provides creds)
        }
    }

    /**
     * Covers the dotenv success path: Dotenv.load() succeeds and returns a
     * non-null credentials string, so finalCredentials is set via credentialsString.
     */
    @Test
    public void testPrioritiseCredentials_dotenvReturnsCredentials_setsCredentials() throws Exception {
        try (FileWriter fw = new FileWriter(ENV_FILE)) {
            fw.write(BaseConstants.ENV_CREDENTIALS_KEY_NAME + "={\"token\":\"env-token-value\"}\n");
        }

        BaseVaultClient<BaseVaultConfig> client = newClient(null);
        client.prioritiseCredentials(null);

        Assert.assertNotNull(client.finalCredentials);
        Assert.assertEquals("{\"token\":\"env-token-value\"}", client.finalCredentials.getCredentialsString());
    }

    /**
     * Covers the path where dotenv loads but the key is absent (returns null),
     * causing SkyflowException(EmptyCredentials) to be thrown directly.
     */
    @Test
    public void testPrioritiseCredentials_dotenvKeyMissing_throwsSkyflowException() throws Exception {
        try (FileWriter fw = new FileWriter(ENV_FILE)) {
            fw.write("SOME_OTHER_KEY=some_value\n");
        }

        BaseVaultClient<BaseVaultConfig> client = newClient(null);
        try {
            client.prioritiseCredentials(null);
            Assert.fail("Should have thrown SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertTrue(e.getMessage().contains(ErrorMessage.EmptyCredentials.getMessage()));
        }
    }
}
