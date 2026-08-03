package com.skyflow;

import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.controller.VaultController;
import org.junit.Assert;
import org.junit.Test;

/**
 * The four client lifecycle scenarios, mirrored by the ClientOperationsExample sample.
 *
 * <p>Each scenario runs twice: once through {@code SkyflowClientBuilder} and once through the built
 * {@code Skyflow} client. The two go down different code paths — BaseSkyflow.updateVaultConfig calls
 * the template directly and skips the builder's own override — and a bug that dropped the
 * flowvault-specific fields on the client path only was found exactly this way.
 *
 * <p>Unlike the sample, these run inside the com.skyflow package, so they can assert against the
 * controller's own config rather than only the stored copy.
 */
public class ClientLifecycleScenarioTests {

    private static final String VAULT_ID = "vault1";
    private static final String NOT_IN_CONFIG_LIST = "VaultId is missing from the config";

    private static VaultConfig config(String clusterId) {
        VaultConfig config = new VaultConfig();
        config.setVaultId(VAULT_ID);
        config.setClusterId(clusterId);
        config.setEnv(Env.DEV);
        return config;
    }

    /** An update carrying a new cluster, env and timeout. clusterId is resent because the incoming
     *  config is validated on its own before being merged. */
    private static VaultConfig update(String clusterId, Env env, Integer timeout) {
        VaultConfig update = config(clusterId);
        update.setEnv(env);
        update.setTimeout(timeout);
        return update;
    }

    // ── A: add -> update -> delete -> vault() must fail ───────────────────────

    @Test
    public void testScenarioA_viaBuilder_addUpdateDeleteThenVaultFails() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(config("cluster1"));

        // add
        Assert.assertEquals("cluster1", builder.build().getVaultConfig(VAULT_ID).getClusterId());
        // update - no error
        builder.updateVaultConfig(update("cluster2", Env.PROD, 30));
        Assert.assertEquals("cluster2", builder.build().getVaultConfig(VAULT_ID).getClusterId());
        Assert.assertEquals(Env.PROD, builder.build().getVaultConfig(VAULT_ID).getEnv());
        Assert.assertEquals(Integer.valueOf(30), builder.build().getVaultConfig(VAULT_ID).getTimeout());
        // delete
        builder.removeVaultConfig(VAULT_ID);
        Assert.assertNull(builder.build().getVaultConfig(VAULT_ID));

        // vault() -> vault id not found
        Skyflow client = builder.build();
        try {
            client.vault();
            Assert.fail("vault() must fail once the vault is removed");
        } catch (SkyflowException e) {
            Assert.assertTrue(e.getMessage().contains(NOT_IN_CONFIG_LIST));
        }
    }

    @Test
    public void testScenarioA_viaClient_addUpdateDeleteThenVaultFails() throws SkyflowException {
        Skyflow client = Skyflow.builder().addVaultConfig(config("cluster1")).build();

        client.updateVaultConfig(update("cluster2", Env.PROD, 30));
        Assert.assertEquals("cluster2", client.getVaultConfig(VAULT_ID).getClusterId());
        Assert.assertEquals(Integer.valueOf(30), client.getVaultConfig(VAULT_ID).getTimeout());

        client.removeVaultConfig(VAULT_ID);
        Assert.assertNull(client.getVaultConfig(VAULT_ID));

        try {
            client.vault();
            Assert.fail("vault() must fail once the vault is removed");
        } catch (SkyflowException e) {
            Assert.assertTrue(e.getMessage().contains(NOT_IN_CONFIG_LIST));
        }
    }

    // ── B: add -> delete -> update must throw ─────────────────────────────────

    @Test
    public void testScenarioB_viaBuilder_addDeleteThenUpdateThrows() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder()
                .addVaultConfig(config("cluster1"))
                .removeVaultConfig(VAULT_ID);

        try {
            builder.updateVaultConfig(update("cluster2", Env.PROD, 30));
            Assert.fail("updating a removed vault must throw, not silently re-create it");
        } catch (SkyflowException e) {
            Assert.assertTrue(e.getMessage().contains(NOT_IN_CONFIG_LIST));
        }

        // the failed update must not have resurrected the vault
        Assert.assertNull(builder.build().getVaultConfig(VAULT_ID));
    }

    @Test
    public void testScenarioB_viaClient_addDeleteThenUpdateThrows() throws SkyflowException {
        Skyflow client = Skyflow.builder().addVaultConfig(config("cluster1")).build();
        client.removeVaultConfig(VAULT_ID);

        try {
            client.updateVaultConfig(update("cluster2", Env.PROD, 30));
            Assert.fail("updating a removed vault must throw, not silently re-create it");
        } catch (SkyflowException e) {
            Assert.assertTrue(e.getMessage().contains(NOT_IN_CONFIG_LIST));
        }

        Assert.assertNull(client.getVaultConfig(VAULT_ID));
        try {
            client.vault();
            Assert.fail("vault() must still fail after the rejected update");
        } catch (SkyflowException e) {
            Assert.assertTrue(e.getMessage().contains(NOT_IN_CONFIG_LIST));
        }
    }

    // ── C: add -> update -> vault() carries the latest config ─────────────────

    @Test
    public void testScenarioC_viaBuilder_vaultAfterUpdateHasLatest() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(config("cluster1"));

        builder.updateVaultConfig(update("cluster2", Env.PROD, 15));
        VaultController vault = builder.build().vault();

        Assert.assertEquals("cluster2", vault.getVaultConfig().getClusterId());
        Assert.assertEquals(Env.PROD, vault.getVaultConfig().getEnv());
        Assert.assertEquals(Integer.valueOf(15), vault.getVaultConfig().getTimeout());
        Assert.assertEquals("https://cluster2.skyvault.skyflowapis.com", vault.currentVaultURL);
        vault.updateExecutorInHTTP();
        Assert.assertEquals(15_000, vault.sharedHttpClient.callTimeoutMillis());
    }

    @Test
    public void testScenarioC_viaClient_vaultAfterUpdateHasLatest() throws SkyflowException {
        Skyflow client = Skyflow.builder().addVaultConfig(config("cluster1")).build();

        client.updateVaultConfig(update("cluster2", Env.PROD, 15));
        VaultController vault = client.vault();

        Assert.assertEquals("cluster2", vault.getVaultConfig().getClusterId());
        Assert.assertEquals(Env.PROD, vault.getVaultConfig().getEnv());
        Assert.assertEquals(Integer.valueOf(15), vault.getVaultConfig().getTimeout());
        Assert.assertEquals("https://cluster2.skyvault.skyflowapis.com", vault.currentVaultURL);
        vault.updateExecutorInHTTP();
        Assert.assertEquals(15_000, vault.sharedHttpClient.callTimeoutMillis());
    }

    // ── D: add -> vault() -> update -> vault() carries the latest config ──────

    @Test
    public void testScenarioD_viaBuilder_heldControllerSeesTheUpdate() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(config("cluster1"));
        VaultController held = builder.build().vault();
        Assert.assertEquals("cluster1", held.getVaultConfig().getClusterId());
        Assert.assertEquals("https://cluster1.skyvault.skyflowapis.dev", held.currentVaultURL);

        builder.updateVaultConfig(update("cluster2", Env.PROD, 45));
        VaultController after = builder.build().vault();

        Assert.assertSame("the reference taken before the update must still be current", held, after);
        Assert.assertEquals("cluster2", held.getVaultConfig().getClusterId());
        Assert.assertEquals(Integer.valueOf(45), held.getVaultConfig().getTimeout());
        Assert.assertEquals("https://cluster2.skyvault.skyflowapis.com", held.currentVaultURL);
    }

    @Test
    public void testScenarioD_viaClient_heldControllerSeesTheUpdate() throws SkyflowException {
        Skyflow client = Skyflow.builder().addVaultConfig(config("cluster1")).build();
        VaultController held = client.vault();
        Assert.assertEquals("cluster1", held.getVaultConfig().getClusterId());

        client.updateVaultConfig(update("cluster2", Env.PROD, 45));

        Assert.assertSame("the reference taken before the update must still be current",
                held, client.vault());
        Assert.assertEquals("cluster2", held.getVaultConfig().getClusterId());
        Assert.assertEquals(Integer.valueOf(45), held.getVaultConfig().getTimeout());
        Assert.assertEquals("https://cluster2.skyvault.skyflowapis.com", held.currentVaultURL);
        held.updateExecutorInHTTP();
        Assert.assertEquals(45_000, held.sharedHttpClient.callTimeoutMillis());
    }

    // ── the vaultUrl variant of C/D, since it resolves differently to clusterId ──

    @Test
    public void testScenarioD_viaClient_heldControllerSeesANewVaultUrl() throws SkyflowException {
        VaultConfig initial = config("cluster1");
        initial.setVaultUrl("https://first.example.com");
        Skyflow client = Skyflow.builder().addVaultConfig(initial).build();
        VaultController held = client.vault();
        Assert.assertEquals("https://first.example.com", held.currentVaultURL);

        VaultConfig update = config("cluster1");
        update.setVaultUrl("https://second.example.com");
        client.updateVaultConfig(update);

        Assert.assertEquals("https://second.example.com", held.currentVaultURL);
    }
}
