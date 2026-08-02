package com.skyflow.config;

import com.skyflow.enums.Env;
import org.junit.Assert;
import org.junit.Test;

public class VaultConfigTests {

    @Test
    public void testDefaultEnvIsProd() {
        VaultConfig config = new VaultConfig();
        Assert.assertEquals(Env.PROD, config.getEnv());
    }

    @Test
    public void testSettingNullEnvFallsBackToProd() {
        VaultConfig config = new VaultConfig();
        config.setEnv(null);
        Assert.assertEquals(Env.PROD, config.getEnv());
    }

    @Test
    public void testVaultIdGetterSetter() {
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");
        Assert.assertEquals("vault123", config.getVaultId());
    }

    @Test
    public void testClusterIdGetterSetter() {
        VaultConfig config = new VaultConfig();
        config.setClusterId("cluster123");
        Assert.assertEquals("cluster123", config.getClusterId());
    }

    @Test
    public void testEnvGetterSetter() {
        VaultConfig config = new VaultConfig();
        config.setEnv(Env.SANDBOX);
        Assert.assertEquals(Env.SANDBOX, config.getEnv());
    }

    @Test
    public void testCredentialsGetterSetter() {
        VaultConfig config = new VaultConfig();
        Credentials credentials = new Credentials();
        credentials.setToken("token1");
        config.setCredentials(credentials);
        Assert.assertEquals(credentials, config.getCredentials());
    }

    @Test
    public void testVaultURLDefaultsToNull() {
        VaultConfig config = new VaultConfig();
        Assert.assertNull(config.getVaultURL());
    }

    @Test
    public void testVaultURLGetterSetter() {
        VaultConfig config = new VaultConfig();
        config.setVaultURL("https://myvault.example.com");
        Assert.assertEquals("https://myvault.example.com", config.getVaultURL());
    }

    @Test
    public void testClone_copiesFieldsAndDeepCopiesCredentials() throws CloneNotSupportedException {
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");
        config.setClusterId("cluster123");
        config.setEnv(Env.DEV);
        Credentials credentials = new Credentials();
        credentials.setToken("token1");
        config.setCredentials(credentials);

        VaultConfig cloned = (VaultConfig) config.clone();

        Assert.assertEquals(config.getVaultId(), cloned.getVaultId());
        Assert.assertEquals(config.getClusterId(), cloned.getClusterId());
        Assert.assertEquals(config.getEnv(), cloned.getEnv());
        Assert.assertNotSame(config.getCredentials(), cloned.getCredentials());
        Assert.assertEquals(config.getCredentials().getToken(), cloned.getCredentials().getToken());
    }

    @Test
    public void testClone_withNullCredentials() throws CloneNotSupportedException {
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");
        config.setClusterId("cluster123");

        VaultConfig cloned = (VaultConfig) config.clone();

        Assert.assertNull(cloned.getCredentials());
    }
}
