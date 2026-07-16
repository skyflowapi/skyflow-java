package com.skyflow.config;

import com.skyflow.enums.Env;
import org.junit.Assert;
import org.junit.Test;

public class BaseVaultConfigTests {

    @Test
    public void testDefaultConstructorDefaults() {
        BaseVaultConfig config = new BaseVaultConfig();
        Assert.assertNull(config.getVaultId());
        Assert.assertNull(config.getClusterId());
        Assert.assertEquals(Env.PROD, config.getEnv());
        Assert.assertNull(config.getCredentials());
    }

    @Test
    public void testGettersAndSetters() {
        BaseVaultConfig config = new BaseVaultConfig();
        Credentials credentials = new Credentials();
        credentials.setToken("test_token");

        config.setVaultId("vault_id");
        config.setClusterId("cluster_id");
        config.setEnv(Env.SANDBOX);
        config.setCredentials(credentials);

        Assert.assertEquals("vault_id", config.getVaultId());
        Assert.assertEquals("cluster_id", config.getClusterId());
        Assert.assertEquals(Env.SANDBOX, config.getEnv());
        Assert.assertEquals(credentials, config.getCredentials());
    }

    @Test
    public void testSetEnvNullFallsBackToProd() {
        BaseVaultConfig config = new BaseVaultConfig();
        config.setEnv(Env.DEV);
        config.setEnv(null);
        Assert.assertEquals(Env.PROD, config.getEnv());
    }

    @Test
    public void testCloneProducesIndependentCopy() throws CloneNotSupportedException {
        BaseVaultConfig original = new BaseVaultConfig();
        original.setVaultId("vault_id");
        original.setClusterId("cluster_id");
        original.setEnv(Env.STAGE);
        Credentials credentials = new Credentials();
        credentials.setToken("original_token");
        original.setCredentials(credentials);

        BaseVaultConfig cloned = (BaseVaultConfig) original.clone();

        Assert.assertNotSame(original, cloned);
        Assert.assertEquals(original.getVaultId(), cloned.getVaultId());
        Assert.assertEquals(original.getClusterId(), cloned.getClusterId());
        Assert.assertEquals(original.getEnv(), cloned.getEnv());
        Assert.assertNotSame(original.getCredentials(), cloned.getCredentials());
        Assert.assertEquals("original_token", cloned.getCredentials().getToken());

        // Mutating the clone's credentials must not affect the original
        cloned.getCredentials().setToken("mutated_token");
        Assert.assertEquals("original_token", original.getCredentials().getToken());
    }

    @Test
    public void testCloneWithNullCredentials() throws CloneNotSupportedException {
        BaseVaultConfig original = new BaseVaultConfig();
        original.setVaultId("vault_id");

        BaseVaultConfig cloned = (BaseVaultConfig) original.clone();
        Assert.assertNull(cloned.getCredentials());
    }
}
