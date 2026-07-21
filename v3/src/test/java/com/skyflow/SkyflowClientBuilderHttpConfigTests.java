package com.skyflow;

import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.vault.controller.VaultController;
import java.lang.reflect.Field;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies the client-wide HTTP config setters on {@code Skyflow.builder()} store their values and
 * propagate to the vault controller(s) — both when set before {@code addVaultConfig} and after
 * (which exercises the propagation loop over already-created controllers).
 */
public class SkyflowClientBuilderHttpConfigTests {

    private VaultConfig vaultConfig() {
        VaultConfig cfg = new VaultConfig();
        cfg.setVaultId("test_vault_id");
        cfg.setClusterId("test_cluster_id");
        cfg.setEnv(Env.PROD);
        return cfg;
    }

    private Object commonField(VaultController controller, String name) throws Exception {
        Field field = VaultClient.class.getDeclaredField(name); // fields declared on VaultClient superclass
        field.setAccessible(true);
        return field.get(controller);
    }

    @Test
    public void clientWideConfigSetBeforeAddVaultReachesController() throws Exception {
        Skyflow client = Skyflow.builder()
                .timeout(30)
                .maxRetries(2)
                .initialRetryDelay(300L)
                .maxRetryDelay(1500L)
                .addVaultConfig(vaultConfig())
                .build();

        VaultController controller = client.vault();
        Assert.assertEquals(Integer.valueOf(30), commonField(controller, "commonTimeout"));
        Assert.assertEquals(Integer.valueOf(2), commonField(controller, "commonMaxRetries"));
        Assert.assertEquals(Long.valueOf(300L), commonField(controller, "commonInitialRetryDelay"));
        Assert.assertEquals(Long.valueOf(1500L), commonField(controller, "commonMaxRetryDelay"));
    }

    @Test
    public void clientWideConfigSetAfterAddVaultPropagatesToExistingController() throws Exception {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(vaultConfig());
        // Set config AFTER the controller already exists -> exercises propagateHttpConfig's loop.
        builder.timeout(45).maxRetries(4).initialRetryDelay(700L).maxRetryDelay(3000L);
        Skyflow client = builder.build();

        VaultController controller = client.vault();
        Assert.assertEquals(Integer.valueOf(45), commonField(controller, "commonTimeout"));
        Assert.assertEquals(Integer.valueOf(4), commonField(controller, "commonMaxRetries"));
        Assert.assertEquals(Long.valueOf(700L), commonField(controller, "commonInitialRetryDelay"));
        Assert.assertEquals(Long.valueOf(3000L), commonField(controller, "commonMaxRetryDelay"));
    }

    @Test
    public void noClientWideConfigLeavesCommonFieldsNull() throws Exception {
        Skyflow client = Skyflow.builder().addVaultConfig(vaultConfig()).build();

        VaultController controller = client.vault();
        Assert.assertNull(commonField(controller, "commonTimeout"));
        Assert.assertNull(commonField(controller, "commonMaxRetries"));
        Assert.assertNull(commonField(controller, "commonInitialRetryDelay"));
        Assert.assertNull(commonField(controller, "commonMaxRetryDelay"));
    }
}
