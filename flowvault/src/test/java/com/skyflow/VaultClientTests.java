package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.errors.SkyflowException;
import org.junit.Assert;
import org.junit.Test;

public class VaultClientTests {

    private static VaultConfig buildConfig(String vaultId, String clusterId, Credentials credentials) {
        VaultConfig config = new VaultConfig();
        config.setVaultId(vaultId);
        config.setClusterId(clusterId);
        config.setEnv(Env.DEV);
        if (credentials != null) {
            config.setCredentials(credentials);
        }
        return config;
    }

    // ── updateVaultURL priority order ────────────────────────────────────────

    @Test
    public void testUpdateVaultURL_usesExplicitVaultURLOverClusterId() throws SkyflowException {
        VaultConfig config = buildConfig("vault1", "cluster1", null);
        config.setVaultURL("https://custom.example.com");

        VaultClient client = new VaultClient(config, null);

        Assert.assertEquals("https://custom.example.com", client.currentVaultURL);
    }

    @Test
    public void testUpdateVaultURL_constructsFromClusterIdWhenNoVaultURL() throws SkyflowException {
        VaultConfig config = buildConfig("vault1", "cluster1", null);

        VaultClient client = new VaultClient(config, null);

        Assert.assertEquals("https://cluster1.skyvault.skyflowapis.dev", client.currentVaultURL);
    }

    // ── setBearerToken ────────────────────────────────────────────────────────

    @Test
    public void testSetBearerToken_usesApiKeyDirectly() throws SkyflowException {
        Credentials creds = new Credentials();
        creds.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        VaultConfig config = buildConfig("vault1", "cluster1", creds);

        VaultClient client = new VaultClient(config, null);
        client.setBearerToken();

        Assert.assertEquals("sky-ab123-abcd1234cdef1234abcd4321cdef4321", client.token);
    }

    @Test
    public void testSetBearerToken_reusesNonExpiredToken() throws SkyflowException {
        Credentials creds = new Credentials();
        creds.setToken("x.eyJleHAiOjk5OTk5OTk5OTl9.y");
        VaultConfig config = buildConfig("vault1", "cluster1", creds);

        VaultClient client = new VaultClient(config, null);
        client.setBearerToken();
        String firstToken = client.token;
        client.setBearerToken();

        Assert.assertEquals(firstToken, client.token);
    }

    @Test
    public void testSetBearerToken_invalidCredentialsThrows() {
        Credentials creds = new Credentials();
        creds.setApiKey("not-a-valid-api-key");
        VaultConfig config = buildConfig("vault1", "cluster1", creds);

        try {
            VaultClient client = new VaultClient(config, null);
            client.setBearerToken();
            Assert.fail("Should have thrown an exception");
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    // ── getRecordsApi ─────────────────────────────────────────────────────────

    @Test
    public void testGetRecordsApi_availableAfterSetBearerToken() throws SkyflowException {
        Credentials creds = new Credentials();
        creds.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        VaultConfig config = buildConfig("vault1", "cluster1", creds);

        VaultClient client = new VaultClient(config, null);
        client.setBearerToken();

        Assert.assertNotNull(client.getRecordsApi());
    }

    // ── setCommonCredentials ──────────────────────────────────────────────────

    @Test
    public void testSetCommonCredentials_vaultSpecificCredentialsTakePriority() throws SkyflowException {
        Credentials vaultCreds = new Credentials();
        vaultCreds.setToken("vault-specific-token");
        VaultConfig config = buildConfig("vault1", "cluster1", vaultCreds);

        VaultClient client = new VaultClient(config, null);
        Credentials commonCreds = new Credentials();
        commonCreds.setToken("common-token");
        client.setCommonCredentials(commonCreds);
        client.setBearerToken();

        Assert.assertEquals("vault-specific-token", client.token);
    }

    @Test
    public void testSetCommonCredentials_usedWhenNoVaultSpecificCredentials() throws SkyflowException {
        VaultConfig config = buildConfig("vault1", "cluster1", null);

        VaultClient client = new VaultClient(config, null);
        Credentials commonCreds = new Credentials();
        commonCreds.setToken("common-token");
        client.setCommonCredentials(commonCreds);
        client.setBearerToken();

        Assert.assertEquals("common-token", client.token);
    }
}
