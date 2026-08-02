package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.controller.VaultController;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies that an update reaches every place the value is actually consumed — the stored config,
 * the controller holding it, and everything derived from it (vault URL, HTTP client, bearer token)
 * — rather than only the copy in vaultConfigMap.
 */
public class UpdatePropagationTests {

    private static VaultConfig buildConfig(String vaultId, String clusterId) {
        VaultConfig config = new VaultConfig();
        config.setVaultId(vaultId);
        config.setClusterId(clusterId);
        config.setEnv(Env.DEV);
        return config;
    }

    private static Credentials tokenCredentials(String token) {
        Credentials credentials = new Credentials();
        credentials.setToken(token);
        return credentials;
    }

    // ── updateVaultConfig reaches the controller, not just the stored config ──

    @Test
    public void testUpdateVaultConfig_storedConfigAndControllerConfigAgree() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1"));

        VaultConfig update = buildConfig("vault1", "cluster2");
        update.setEnv(Env.PROD);
        Skyflow client = builder.updateVaultConfig(update).build();

        // The copy the user can read back...
        Assert.assertEquals("cluster2", client.getVaultConfig("vault1").getClusterId());
        Assert.assertEquals(Env.PROD, client.getVaultConfig("vault1").getEnv());
        // ...and the copy the controller actually builds requests from.
        Assert.assertEquals("cluster2", client.vault().getVaultConfig().getClusterId());
        Assert.assertEquals(Env.PROD, client.vault().getVaultConfig().getEnv());
    }

    @Test
    public void testUpdateVaultConfig_reconfiguresTheControllerInPlace() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1"));
        VaultController before = builder.build().vault();

        VaultController after = builder.updateVaultConfig(buildConfig("vault1", "cluster2")).build().vault();

        Assert.assertSame(before, after);
    }

    @Test
    public void testUpdateVaultConfig_handleHeldAcrossTheUpdateSeesTheNewConfig() throws SkyflowException {
        // Replacing the controller instead of reconfiguring it would leave this reference talking
        // to the old vault, with no error to say so.
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1"));
        VaultController held = builder.build().vault();
        Assert.assertEquals("https://cluster1.skyvault.skyflowapis.dev", held.currentVaultURL);

        VaultConfig update = buildConfig("vault1", "cluster2");
        update.setEnv(Env.PROD);
        builder.updateVaultConfig(update);

        Assert.assertEquals("cluster2", held.getVaultConfig().getClusterId());
        Assert.assertEquals(Env.PROD, held.getVaultConfig().getEnv());
        Assert.assertEquals("https://cluster2.skyvault.skyflowapis.com", held.currentVaultURL);
    }

    @Test
    public void testUpdateVaultConfig_handleHeldAcrossTheUpdateSeesNewCredentials() throws SkyflowException {
        VaultConfig config = buildConfig("vault1", "cluster1");
        config.setCredentials(tokenCredentials("first-token"));
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(config);
        VaultController held = builder.build().vault();
        held.setBearerToken();
        Assert.assertEquals("first-token", held.token);

        VaultConfig update = buildConfig("vault1", "cluster1");
        update.setCredentials(tokenCredentials("second-token"));
        builder.updateVaultConfig(update);
        held.setBearerToken();

        Assert.assertEquals("second-token", held.token);
    }

    @Test
    public void testUpdateVaultConfig_handleHeldAcrossTheUpdateSeesNewHttpSettings() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1"));
        VaultController held = builder.build().vault();
        held.updateExecutorInHTTP();
        Assert.assertEquals(60_000, held.sharedHttpClient.callTimeoutMillis());

        VaultConfig update = buildConfig("vault1", "cluster1");
        update.setTimeout(45);
        builder.updateVaultConfig(update);
        held.updateExecutorInHTTP();

        Assert.assertEquals(45_000, held.sharedHttpClient.callTimeoutMillis());
    }

    @Test
    public void testUpdateVaultConfig_derivedVaultURLIsRebuilt() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1"));
        Assert.assertEquals("https://cluster1.skyvault.skyflowapis.dev", builder.build().vault().currentVaultURL);

        VaultConfig update = buildConfig("vault1", "cluster2");
        update.setEnv(Env.PROD);

        Assert.assertEquals("https://cluster2.skyvault.skyflowapis.com",
                builder.updateVaultConfig(update).build().vault().currentVaultURL);
    }

    @Test
    public void testUpdateVaultConfig_newVaultLevelCredentialsReachTheToken() throws SkyflowException {
        VaultConfig config = buildConfig("vault1", "cluster1");
        config.setCredentials(tokenCredentials("first-token"));
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(config);

        VaultController before = builder.build().vault();
        before.setBearerToken();
        Assert.assertEquals("first-token", before.token);

        VaultConfig update = buildConfig("vault1", "cluster1");
        update.setCredentials(tokenCredentials("second-token"));
        VaultController after = builder.updateVaultConfig(update).build().vault();
        after.setBearerToken();

        Assert.assertEquals("second-token", after.token);
    }

    @Test
    public void testUpdateVaultConfig_omittingCredentialsKeepsTheExistingOnes() throws SkyflowException {
        VaultConfig config = buildConfig("vault1", "cluster1");
        config.setCredentials(tokenCredentials("first-token"));
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(config);

        // No credentials on the update: null means "leave as is".
        VaultController after = builder.updateVaultConfig(buildConfig("vault1", "cluster2")).build().vault();
        after.setBearerToken();

        Assert.assertEquals("first-token", after.token);
    }

    @Test
    public void testUpdateVaultConfig_stillHasTheClientWideCredentialsAfterwards() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder()
                .addSkyflowCredentials(tokenCredentials("common-token"))
                .addVaultConfig(buildConfig("vault1", "cluster1"));

        // Reconfiguring must not clear the client-wide credentials the controller already holds.
        VaultController after = builder.updateVaultConfig(buildConfig("vault1", "cluster2")).build().vault();
        after.setBearerToken();

        Assert.assertEquals("common-token", after.token);
    }

    @Test
    public void testUpdateVaultConfig_vaultLevelCredentialsStillBeatClientWide() throws SkyflowException {
        VaultConfig config = buildConfig("vault1", "cluster1");
        config.setCredentials(tokenCredentials("vault-token"));
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder()
                .addSkyflowCredentials(tokenCredentials("common-token"))
                .addVaultConfig(config);

        VaultController after = builder.updateVaultConfig(buildConfig("vault1", "cluster2")).build().vault();
        after.setBearerToken();

        Assert.assertEquals("vault-token", after.token);
    }

    // ── Credentials updates reach every controller ───────────────────────────

    @Test
    public void testAddSkyflowCredentials_afterVaultExists_reachesTheController() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(buildConfig("vault1", "cluster1"));

        VaultController controller = builder.addSkyflowCredentials(tokenCredentials("common-token")).build().vault();
        controller.setBearerToken();

        Assert.assertEquals("common-token", controller.token);
    }

    @Test
    public void testAddSkyflowCredentials_beforeVaultExists_reachesTheNewController() throws SkyflowException {
        Skyflow client = Skyflow.builder()
                .addSkyflowCredentials(tokenCredentials("common-token"))
                .addVaultConfig(buildConfig("vault1", "cluster1"))
                .build();

        VaultController controller = client.vault();
        controller.setBearerToken();

        Assert.assertEquals("common-token", controller.token);
    }

    @Test
    public void testAddSkyflowCredentials_reachesEveryVaultNotJustTheFirst() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder()
                .addVaultConfig(buildConfig("vault1", "cluster1"))
                .addVaultConfig(buildConfig("vault2", "cluster2"))
                .addSkyflowCredentials(tokenCredentials("common-token"));

        VaultController first = builder.build().vault();
        first.setBearerToken();
        Assert.assertEquals("common-token", first.token);

        // vault() resolves the first entry, so drop vault1 to reach the second controller.
        VaultController second = builder.removeVaultConfig("vault1").build().vault();
        second.setBearerToken();
        Assert.assertEquals("common-token", second.token);
    }

    @Test
    public void testUpdateSkyflowCredentials_invalidatesTheCachedToken() throws SkyflowException {
        Skyflow client = Skyflow.builder()
                .addSkyflowCredentials(tokenCredentials("first-token"))
                .addVaultConfig(buildConfig("vault1", "cluster1"))
                .build();

        VaultController controller = client.vault();
        controller.setBearerToken();
        Assert.assertEquals("first-token", controller.token);

        client.updateSkyflowCredentials(tokenCredentials("second-token"));
        controller.setBearerToken();

        Assert.assertEquals("A cached token must not survive a credentials change",
                "second-token", controller.token);
    }

    @Test
    public void testUpdateSkyflowCredentials_doesNotOverrideVaultLevelCredentials() throws SkyflowException {
        VaultConfig config = buildConfig("vault1", "cluster1");
        config.setCredentials(tokenCredentials("vault-token"));
        Skyflow client = Skyflow.builder().addVaultConfig(config).build();

        client.updateSkyflowCredentials(tokenCredentials("common-token"));
        VaultController controller = client.vault();
        controller.setBearerToken();

        Assert.assertEquals("vault-token", controller.token);
    }

    @Test
    public void testUpdateSkyflowCredentials_appliesToTheSameControllerInstance() throws SkyflowException {
        // Unlike updateVaultConfig, a credentials change must not swap the controller out — it
        // reconfigures the existing one, so a handle the caller already holds stays valid.
        Skyflow client = Skyflow.builder()
                .addSkyflowCredentials(tokenCredentials("first-token"))
                .addVaultConfig(buildConfig("vault1", "cluster1"))
                .build();
        VaultController held = client.vault();

        client.updateSkyflowCredentials(tokenCredentials("second-token"));

        Assert.assertSame(held, client.vault());
        held.setBearerToken();
        Assert.assertEquals("second-token", held.token);
    }
}
