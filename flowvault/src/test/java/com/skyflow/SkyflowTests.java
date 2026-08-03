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

    // ── updateVaultConfig: flowvault-specific fields ─────────────────────────
    // BaseSkyflow.mergeVaultConfig() only carries env/clusterId/credentials, so vaultUrl needs
    // SkyflowClientBuilder.carryVaultOverrides() to survive an update.

    @Test
    public void testUpdateVaultConfig_changesVaultURL() throws SkyflowException {
        VaultConfig config = buildConfig("vault1", "cluster1");
        config.setVaultUrl("https://first.example.com");
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(config);
        Assert.assertEquals("https://first.example.com", builder.build().vault().currentVaultURL);

        VaultConfig update = buildConfig("vault1", "cluster1");
        update.setVaultUrl("https://second.example.com");

        Assert.assertEquals("https://second.example.com",
                builder.updateVaultConfig(update).build().vault().currentVaultURL);
    }

    @Test
    public void testUpdateVaultConfig_storesTheNewVaultURLOnTheConfig() throws SkyflowException {
        VaultConfig config = buildConfig("vault1", "cluster1");
        config.setVaultUrl("https://first.example.com");
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(config);

        VaultConfig update = buildConfig("vault1", "cluster1");
        update.setVaultUrl("https://second.example.com");

        Assert.assertEquals("https://second.example.com",
                builder.updateVaultConfig(update).build().getVaultConfig("vault1").getVaultUrl());
    }

    @Test
    public void testUpdateVaultConfig_omittingVaultURLKeepsTheExistingOne() throws SkyflowException {
        VaultConfig config = buildConfig("vault1", "cluster1");
        config.setVaultUrl("https://first.example.com");
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(config);

        // No vaultUrl on the update: null means "leave as is", as elsewhere in the merge.
        Skyflow client = builder.updateVaultConfig(buildConfig("vault1", "cluster2")).build();

        Assert.assertEquals("https://first.example.com", client.vault().currentVaultURL);
    }

    @Test
    public void testUpdateVaultConfig_canIntroduceAVaultURLWhereClusterIdWasUsed() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1"));
        Assert.assertEquals("https://cluster1.skyvault.skyflowapis.dev", builder.build().vault().currentVaultURL);

        VaultConfig update = buildConfig("vault1", "cluster1");
        update.setVaultUrl("https://explicit.example.com");

        Assert.assertEquals("https://explicit.example.com",
                builder.updateVaultConfig(update).build().vault().currentVaultURL);
    }

    @Test
    public void testUpdateVaultConfig_clusterIdChangeStillRebuildsTheURL() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1"));

        Skyflow client = builder.updateVaultConfig(buildConfig("vault1", "cluster2")).build();

        Assert.assertEquals("https://cluster2.skyvault.skyflowapis.dev", client.vault().currentVaultURL);
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

    // ── updateVaultConfig validates the incoming config, not the merged result ──

    @Test
    public void testUpdateVaultConfig_partialUpdateWithoutClusterIdOrVaultUrlIsRejected() throws SkyflowException {
        // mergeVaultConfig only copies non-null fields across, which implies "send just what you
        // want to change". But updateVaultConfigTemplate validates the INCOMING config first, and
        // validateVaultConfiguration requires clusterId or vaultUrl - so a partial update is
        // rejected even though the merge would have preserved the existing values.
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1"));

        VaultConfig partial = new VaultConfig();
        partial.setVaultId("vault1");
        partial.setTimeout(30);

        try {
            builder.updateVaultConfig(partial);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertTrue(e.getMessage().contains("clusterId"));
        }
    }

    @Test
    public void testUpdateVaultConfig_rejectedPartialUpdateChangesNothing() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1"));

        VaultConfig partial = new VaultConfig();
        partial.setVaultId("vault1");
        partial.setTimeout(30);
        try {
            builder.updateVaultConfig(partial);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException expected) {
            // asserted above
        }

        Skyflow client = builder.build();
        Assert.assertEquals("cluster1", client.getVaultConfig("vault1").getClusterId());
        Assert.assertNull("the rejected timeout must not have been applied",
                client.getVaultConfig("vault1").getTimeout());
    }

    @Test
    public void testUpdateVaultConfig_partialUpdateIsAcceptedWhenClusterIdIsRepeated() throws SkyflowException {
        // The workaround: resend clusterId even when it is not changing.
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1"));

        VaultConfig update = new VaultConfig();
        update.setVaultId("vault1");
        update.setClusterId("cluster1");
        update.setTimeout(30);

        Skyflow client = builder.updateVaultConfig(update).build();

        Assert.assertEquals(Integer.valueOf(30), client.getVaultConfig("vault1").getTimeout());
        Assert.assertEquals("cluster1", client.getVaultConfig("vault1").getClusterId());
    }

    @Test
    public void testUpdateVaultConfig_vaultUrlAloneSatisfiesTheRequirement() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1"));

        VaultConfig update = new VaultConfig();
        update.setVaultId("vault1");
        update.setVaultUrl("https://custom.example.com");
        update.setTimeout(30);

        Skyflow client = builder.updateVaultConfig(update).build();

        Assert.assertEquals("https://custom.example.com", client.vault().currentVaultURL);
        Assert.assertEquals(Integer.valueOf(30), client.getVaultConfig("vault1").getTimeout());
    }

    // ── Client management lifecycles ─────────────────────────────────────────
    // Whole add/update/remove sequences, asserting both the stored config and the controller
    // behind vault() stay in step at every stage.

    private static final String NOT_IN_CONFIG_LIST = "VaultId is missing from the config";

    @Test
    public void testLifecycle_addThenRemove() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1"));

        Skyflow afterAdd = builder.build();
        Assert.assertEquals("cluster1", afterAdd.getVaultConfig("vault1").getClusterId());
        Assert.assertNotNull(afterAdd.vault());

        Skyflow afterRemove = builder.removeVaultConfig("vault1").build();

        Assert.assertNull(afterRemove.getVaultConfig("vault1"));
        try {
            afterRemove.vault();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertTrue(e.getMessage().contains(NOT_IN_CONFIG_LIST));
        }
    }

    @Test
    public void testLifecycle_addThenRemoveThenReAddSameVaultId() throws SkyflowException {
        // Removing must clear the id, otherwise re-adding would trip the duplicate check.
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder()
                .addVaultConfig(buildConfig("vault1", "cluster1"))
                .removeVaultConfig("vault1")
                .addVaultConfig(buildConfig("vault1", "cluster2"));

        Skyflow client = builder.build();

        Assert.assertEquals("cluster2", client.getVaultConfig("vault1").getClusterId());
        Assert.assertEquals("https://cluster2.skyvault.skyflowapis.dev", client.vault().currentVaultURL);
    }

    @Test
    public void testLifecycle_addRemoveThenUpdateFails() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder()
                .addVaultConfig(buildConfig("vault1", "cluster1"))
                .removeVaultConfig("vault1");

        try {
            builder.updateVaultConfig(buildConfig("vault1", "cluster2"));
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertTrue(e.getMessage().contains(NOT_IN_CONFIG_LIST));
        }
    }

    @Test
    public void testLifecycle_addRemoveThenFailedUpdateLeavesNoConfigBehind() throws SkyflowException {
        // A rejected update must not resurrect the removed vault.
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder()
                .addVaultConfig(buildConfig("vault1", "cluster1"))
                .removeVaultConfig("vault1");
        try {
            builder.updateVaultConfig(buildConfig("vault1", "cluster2"));
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException expected) {
            // asserted in testLifecycle_addRemoveThenUpdateFails
        }

        Skyflow client = builder.build();

        Assert.assertNull(client.getVaultConfig("vault1"));
        try {
            client.vault();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertTrue(e.getMessage().contains(NOT_IN_CONFIG_LIST));
        }
    }

    @Test
    public void testLifecycle_addUpdateThenRemove() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1"));

        VaultConfig update = buildConfig("vault1", "cluster2");
        update.setEnv(Env.PROD);
        Skyflow afterUpdate = builder.updateVaultConfig(update).build();

        Assert.assertEquals("cluster2", afterUpdate.getVaultConfig("vault1").getClusterId());
        Assert.assertEquals(Env.PROD, afterUpdate.getVaultConfig("vault1").getEnv());
        Assert.assertEquals("https://cluster2.skyvault.skyflowapis.com", afterUpdate.vault().currentVaultURL);

        Skyflow afterRemove = builder.removeVaultConfig("vault1").build();

        Assert.assertNull(afterRemove.getVaultConfig("vault1"));
        try {
            afterRemove.vault();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertTrue(e.getMessage().contains(NOT_IN_CONFIG_LIST));
        }
    }

    @Test
    public void testLifecycle_addUpdateRemoveThenRemoveAgainFails() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder()
                .addVaultConfig(buildConfig("vault1", "cluster1"))
                .updateVaultConfig(buildConfig("vault1", "cluster2"))
                .removeVaultConfig("vault1");

        try {
            builder.removeVaultConfig("vault1");
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertTrue(e.getMessage().contains(NOT_IN_CONFIG_LIST));
        }
    }

    @Test
    public void testLifecycle_removingOneVaultLeavesTheOtherIntact() throws SkyflowException {
        Skyflow client = Skyflow.builder()
                .addVaultConfig(buildConfig("vault1", "cluster1"))
                .addVaultConfig(buildConfig("vault2", "cluster2"))
                .removeVaultConfig("vault1")
                .build();

        Assert.assertNull(client.getVaultConfig("vault1"));
        Assert.assertEquals("cluster2", client.getVaultConfig("vault2").getClusterId());
        Assert.assertEquals("https://cluster2.skyvault.skyflowapis.dev", client.vault().currentVaultURL);
    }

    @Test
    public void testLifecycle_onTheBuiltClientRatherThanTheBuilder() throws SkyflowException {
        // Skyflow exposes the same add/update/remove surface as the builder; exercise that path too.
        Skyflow client = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1")).build();

        client.updateVaultConfig(buildConfig("vault1", "cluster2"));
        Assert.assertEquals("cluster2", client.getVaultConfig("vault1").getClusterId());

        client.removeVaultConfig("vault1");
        Assert.assertNull(client.getVaultConfig("vault1"));

        try {
            client.updateVaultConfig(buildConfig("vault1", "cluster3"));
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertTrue(e.getMessage().contains(NOT_IN_CONFIG_LIST));
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

    // ── vault(vaultId) ────────────────────────────────────────────────────────

    @Test
    public void testVaultById_returnsTheControllerForTheConfiguredId() throws SkyflowException {
        Skyflow client = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1")).build();
        Assert.assertSame(client.vault(), client.vault("vault1"));
    }

    @Test
    public void testVaultById_selectsTheMatchingVaultAmongSeveral() throws SkyflowException {
        VaultConfig first = buildConfig("vault1", "cluster1");
        first.setVaultUrl("https://first.example.com");
        VaultConfig second = buildConfig("vault2", "cluster2");
        second.setVaultUrl("https://second.example.com");
        Skyflow client = Skyflow.builder().addVaultConfig(first).addVaultConfig(second).build();

        Assert.assertEquals("https://first.example.com", client.vault("vault1").currentVaultURL);
        Assert.assertEquals("https://second.example.com", client.vault("vault2").currentVaultURL);
    }

    @Test
    public void testVaultById_nullIdResolvesToTheFirstConfiguredVault() throws SkyflowException {
        Skyflow client = Skyflow.builder()
                .addVaultConfig(buildConfig("vault1", "cluster1"))
                .addVaultConfig(buildConfig("vault2", "cluster2"))
                .build();
        Assert.assertSame(client.vault(), client.vault(null));
    }

    @Test
    public void testVaultById_throwsForUnknownVaultId() throws SkyflowException {
        Skyflow client = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1")).build();
        try {
            client.vault("vault-unknown");
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testVaultById_throwsWhenNoConfigExists() {
        try {
            Skyflow.builder().build().vault("vault1");
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testVaultById_removedVaultThrowsWhileOthersStillResolve() throws SkyflowException {
        Skyflow client = Skyflow.builder()
                .addVaultConfig(buildConfig("vault1", "cluster1"))
                .addVaultConfig(buildConfig("vault2", "cluster2"))
                .build();
        client.removeVaultConfig("vault1");

        Assert.assertNotNull(client.vault("vault2"));
        try {
            client.vault("vault1");
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
