package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.controller.VaultController;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;

public class SkyflowTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String vaultID = null;
    private static String clusterID = null;
    private static String newClusterID = null;
    private static String token = null;
    private static String anotherToken = null;

    @BeforeClass
    public static void setup() {
        vaultID = "test_vault_id";
        clusterID = "test_cluster_id";
        newClusterID = "new_test_cluster_id";
        token = "test_token";
        anotherToken = "another_test_token";
    }

    @Test
    public void testAddingInvalidVaultConfig() {
        try {
            VaultConfig config = new VaultConfig();
            config.setVaultId("");
            config.setClusterId(clusterID);
            config.setEnv(Env.SANDBOX);
            Skyflow.builder().addVaultConfig(config).build();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyVaultId.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testAddingAnotherVaultConfig() {
        try {
            VaultConfig config = new VaultConfig();
            config.setVaultId(vaultID);
            config.setClusterId(clusterID);
            config.setEnv(Env.SANDBOX);
            Skyflow.builder().addVaultConfig(config).addVaultConfig(config).build();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.VaultIdAlreadyInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testUpdatingValidVaultConfig() {
        try {
            VaultConfig config = new VaultConfig();
            config.setVaultId(vaultID);
            config.setClusterId(clusterID);
            config.setEnv(Env.SANDBOX);

            // Set the config
            Skyflow skyflowClient = Skyflow.builder().addVaultConfig(config).build();

            Credentials credentials = new Credentials();
            credentials.setToken(token);

            // Updating the config directly
            config.setClusterId(newClusterID);
            config.setEnv(Env.PROD);
            config.setCredentials(credentials);

            Assert.assertNotEquals(newClusterID, skyflowClient.getVaultConfig().getClusterId());
            Assert.assertEquals(clusterID, skyflowClient.getVaultConfig().getClusterId());
            Assert.assertNotEquals(Env.PROD, skyflowClient.getVaultConfig().getEnv());
            Assert.assertEquals(Env.SANDBOX, skyflowClient.getVaultConfig().getEnv());
            Assert.assertNotEquals(credentials, skyflowClient.getVaultConfig().getCredentials());
            Assert.assertNull(skyflowClient.getVaultConfig().getCredentials());

        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testVaultWithoutConfigThrows() {
        try {
            Skyflow skyflow = Skyflow.builder().build();
            skyflow.vault();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.VaultIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testVaultNullControllerThrows() {
        try {
            VaultConfig config = new VaultConfig();
            config.setVaultId(vaultID);
            config.setClusterId(clusterID);
            config.setEnv(Env.SANDBOX);

            Skyflow skyflow = Skyflow.builder().addVaultConfig(config).build();

            Object builder = getField(skyflow, "builder");
            @SuppressWarnings("unchecked")
            LinkedHashMap<String, VaultController> clients =
                    (LinkedHashMap<String, VaultController>) getField(builder, "vaultClientsMap");
            clients.put(vaultID, null);

            skyflow.vault();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.VaultIdNotInConfigList.getMessage(), e.getMessage());
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testVaultReturnsController() {
        try {
            VaultConfig config = new VaultConfig();
            config.setVaultId(vaultID);
            config.setClusterId(clusterID);
            config.setEnv(Env.SANDBOX);

            Skyflow skyflow = Skyflow.builder().addVaultConfig(config).build();
            VaultController controller = skyflow.vault();
            Assert.assertNotNull(controller);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testAddSkyflowCredentialsClonesAndSets() {
        try {
            VaultConfig config = new VaultConfig();
            config.setVaultId(vaultID);
            config.setClusterId(clusterID);
            config.setEnv(Env.SANDBOX);

            Credentials creds = new Credentials();
            creds.setToken(token);

            Skyflow skyflow = Skyflow.builder()
                    .addVaultConfig(config)
                    .addSkyflowCredentials(creds)
                    .build();

            creds.setToken("mutated");

            Object builder = getField(skyflow, "builder");
            Credentials storedCreds = (Credentials) getField(builder.getClass().getSuperclass(), builder, "skyflowCredentials");

            Assert.assertEquals(token, storedCreds.getToken());
            Assert.assertNotEquals(creds.getToken(), storedCreds.getToken());
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testSetLogLevelReturnsBuilder() {
        try {
            Skyflow.SkyflowClientBuilder builder = Skyflow.builder();
            Skyflow.SkyflowClientBuilder returned = builder.setLogLevel(LogLevel.INFO);
            Assert.assertSame(builder, returned);

            VaultConfig config = new VaultConfig();
            config.setVaultId(vaultID);
            config.setClusterId(clusterID);
            config.setEnv(Env.SANDBOX);
            returned.addVaultConfig(config).build();
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
    }

    private Object getField(Object instance, String fieldName) throws Exception {
        Field f = instance.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(instance);
    }

    private Object getField(Class<?> declaring, Object instance, String fieldName) throws Exception {
        Field f = declaring.getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(instance);
    }
}
