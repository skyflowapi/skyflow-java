package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.controller.VaultController;
import org.junit.Assert;
import org.junit.Test;

public class SkyflowTests {
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";

    private static VaultConfig buildConfig(String vaultId, String clusterId) {
        VaultConfig config = new VaultConfig();
        config.setVaultId(vaultId);
        config.setClusterId(clusterId);
        config.setEnv(Env.DEV);
        return config;
    }

    // ── addVaultConfig ────────────────────────────────────────────────────────

    @Test
    public void testAddVaultConfig_success() throws SkyflowException {
        Skyflow client = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1")).build();
        Assert.assertEquals("vault1", client.getVaultConfig("vault1").getVaultId());
    }

    @Test
    public void testAddVaultConfig_duplicateVaultIdThrows() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1"));
        try {
            builder.addVaultConfig(buildConfig("vault1", "cluster2"));
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testAddVaultConfig_invalidConfigThrows() {
        VaultConfig config = new VaultConfig();
        // no vaultId set
        try {
            Skyflow.builder().addVaultConfig(config);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    // ── updateVaultConfig ─────────────────────────────────────────────────────

    @Test
    public void testUpdateVaultConfig_success() throws SkyflowException {
        Skyflow client = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1")).build();

        VaultConfig update = new VaultConfig();
        update.setVaultId("vault1");
        update.setClusterId("cluster2");
        update.setEnv(Env.PROD);
        client.updateVaultConfig(update);

        Assert.assertEquals("cluster2", client.getVaultConfig("vault1").getClusterId());
        Assert.assertEquals(Env.PROD, client.getVaultConfig("vault1").getEnv());
    }

    @Test
    public void testUpdateVaultConfig_nonExistentVaultIdThrows() {
        try {
            Skyflow.builder().updateVaultConfig(buildConfig("vault-unknown", "cluster1"));
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    // ── removeVaultConfig ─────────────────────────────────────────────────────

    @Test
    public void testRemoveVaultConfig_success() throws SkyflowException {
        Skyflow client = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1")).build();
        client.removeVaultConfig("vault1");
        Assert.assertNull(client.getVaultConfig("vault1"));
    }

    @Test
    public void testRemoveVaultConfig_nonExistentVaultIdThrows() {
        try {
            Skyflow.builder().removeVaultConfig("vault-unknown");
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    // ── addSkyflowCredentials ─────────────────────────────────────────────────

    @Test
    public void testAddSkyflowCredentials_success() throws SkyflowException {
        Credentials credentials = new Credentials();
        credentials.setToken("some-token");
        Skyflow client = Skyflow.builder()
                .addVaultConfig(buildConfig("vault1", "cluster1"))
                .addSkyflowCredentials(credentials)
                .build();
        Assert.assertNotNull(client);
    }

    @Test
    public void testAddSkyflowCredentials_invalidCredentialsThrows() {
        Credentials credentials = new Credentials();
        credentials.setApiKey("not-a-valid-api-key");
        try {
            Skyflow.builder().addSkyflowCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    // ── setLogLevel / getLogLevel ─────────────────────────────────────────────

    @Test
    public void testSetLogLevel_updatesLogLevel() throws SkyflowException {
        Skyflow client = Skyflow.builder()
                .addVaultConfig(buildConfig("vault1", "cluster1"))
                .setLogLevel(LogLevel.DEBUG)
                .build();
        Assert.assertEquals(LogLevel.DEBUG, client.getLogLevel());
    }

    @Test
    public void testGetLogLevel_defaultsToError() throws SkyflowException {
        Skyflow client = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1")).build();
        Assert.assertEquals(LogLevel.ERROR, client.getLogLevel());
    }

    // ── vault() ───────────────────────────────────────────────────────────────

    @Test
    public void testVault_returnsVaultControllerWhenConfigured() throws SkyflowException {
        Skyflow client = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1")).build();
        VaultController controller = client.vault();
        Assert.assertNotNull(controller);
    }

    @Test
    public void testVault_throwsWhenNoConfigExists() {
        try {
            Skyflow.builder().build().vault();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    // ── getVaultConfig ────────────────────────────────────────────────────────

    @Test
    public void testGetVaultConfig_returnsNullForUnknownVaultId() throws SkyflowException {
        Skyflow client = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1")).build();
        Assert.assertNull(client.getVaultConfig("vault-unknown"));
    }
}
