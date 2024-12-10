package com.skyflow;

import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SkyflowTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String vaultID = null;
    private static String clusterID = null;
    private static String newClusterID = null;
    private static String connectionID = null;
    private static String connectionURL = null;
    private static String newConnectionURL = null;
    private static String token = null;

    @BeforeClass
    public static void setup() {
        vaultID = "test_vault_id";
        clusterID = "test_cluster_id";
        newClusterID = "new_test_cluster_id";
        connectionID = "test_connection_id";
        connectionURL = "https://test.connection.url";
        newConnectionURL = "https://new.test.connection.url";
        token = "test_token";
    }

    @Test
    public void testAddingInvalidVaultConfigInSkyflowBuilder() {
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
    public void testAddingInvalidVaultConfigInSkyflowClient() {
        try {
            VaultConfig config = new VaultConfig();
            config.setVaultId("");
            config.setClusterId(clusterID);
            config.setEnv(Env.SANDBOX);
            Skyflow skyflowClient = Skyflow.builder().build();
            skyflowClient.addVaultConfig(config);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyVaultId.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testAddingValidVaultConfigInSkyflowClient() {
        try {
            VaultConfig config = new VaultConfig();
            config.setVaultId(vaultID);
            config.setClusterId(clusterID);
            config.setEnv(Env.SANDBOX);
            Skyflow skyflowClient = Skyflow.builder().build();
            skyflowClient.addVaultConfig(config);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testAddingExistingVaultConfigInSkyflowClient() {
        try {
            VaultConfig config = new VaultConfig();
            config.setVaultId(vaultID);
            config.setClusterId(clusterID);
            config.setEnv(Env.SANDBOX);
            Skyflow skyflowClient = Skyflow.builder().build();
            skyflowClient.addVaultConfig(config).addVaultConfig(config);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.VaultIdAlreadyInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testUpdatingNonExistentVaultConfigInSkyflowBuilder() {
        try {
            VaultConfig config = new VaultConfig();
            config.setVaultId(vaultID);
            config.setClusterId(clusterID);
            config.setEnv(Env.SANDBOX);
            Skyflow.builder().updateVaultConfig(config).build();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.VaultIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testUpdatingNonExistentVaultConfigInSkyflowClient() {
        try {
            VaultConfig config = new VaultConfig();
            config.setVaultId(vaultID);
            config.setClusterId(clusterID);
            config.setEnv(Env.SANDBOX);
            Skyflow skyflowClient = Skyflow.builder().build();
            skyflowClient.updateVaultConfig(config);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.VaultIdNotInConfigList.getMessage(), e.getMessage());
        } catch (Exception e) {
            Assert.assertEquals(ErrorMessage.VaultIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testUpdatingValidVaultConfigInSkyflowClient() {
        try {
            VaultConfig config = new VaultConfig();
            config.setVaultId(vaultID);
            config.setClusterId(clusterID);
            config.setEnv(Env.SANDBOX);
            Skyflow skyflowClient = Skyflow.builder().addVaultConfig(config).build();

            Credentials credentials = new Credentials();
            credentials.setToken(token);

            config.setClusterId(newClusterID);
            config.setEnv(Env.PROD);
            config.setCredentials(credentials);

            skyflowClient.updateVaultConfig(config);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testRemovingNonExistentVaultConfigInSkyflowBuilder() {
        try {
            Skyflow.builder().removeVaultConfig(vaultID).build();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.VaultIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testRemovingNonExistentVaultConfigInSkyflowClient() {
        try {
            VaultConfig config = new VaultConfig();
            config.setVaultId(vaultID);
            config.setClusterId(clusterID);
            config.setEnv(Env.SANDBOX);
            Skyflow skyflowClient = Skyflow.builder().addVaultConfig(config).build();
            skyflowClient.removeVaultConfig(vaultID);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testRemovingValidVaultConfigInSkyflowClient() {
        try {
            Skyflow skyflowClient = Skyflow.builder().build();
            skyflowClient.removeVaultConfig(vaultID);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.VaultIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testGettingNonExistentVaultConfigInSkyflowClient() {
        try {
            Skyflow skyflowClient = Skyflow.builder().build();
            VaultConfig config = skyflowClient.getVaultConfig(vaultID);
            Assert.assertNull(config);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGettingAlreadyRemovedVaultFromEmptyConfigs() {
        try {
            Skyflow skyflowClient = Skyflow.builder().build();
            skyflowClient.vault();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.VaultIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testGettingAlreadyRemovedVaultFromNonEmptyConfigs() {
        try {
            VaultConfig primaryConfig = new VaultConfig();
            primaryConfig.setVaultId(vaultID);
            primaryConfig.setClusterId(clusterID);
            primaryConfig.setEnv(Env.SANDBOX);

            VaultConfig secondaryConfig = new VaultConfig();
            secondaryConfig.setVaultId(vaultID + "123");
            secondaryConfig.setClusterId(clusterID);
            secondaryConfig.setEnv(Env.SANDBOX);
            Skyflow skyflowClient = Skyflow.builder().addVaultConfig(primaryConfig).addVaultConfig(secondaryConfig).build();
            skyflowClient.removeVaultConfig(vaultID);
            skyflowClient.vault(vaultID);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.VaultIdNotInConfigList.getMessage(), e.getMessage());
        }
    }


    @Test
    public void testAddingInvalidConnectionConfigInSkyflowBuilder() {
        try {
            ConnectionConfig config = new ConnectionConfig();
            config.setConnectionId("");
            config.setConnectionUrl(connectionURL);
            Skyflow.builder().addConnectionConfig(config).build();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyConnectionId.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testAddingInvalidConnectionConfigInSkyflowClient() {
        try {
            ConnectionConfig config = new ConnectionConfig();
            config.setConnectionId("");
            config.setConnectionUrl(connectionURL);
            Skyflow skyflowClient = Skyflow.builder().build();
            skyflowClient.addConnectionConfig(config);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyConnectionId.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testAddingValidConnectionConfigInSkyflowClient() {
        try {
            ConnectionConfig config = new ConnectionConfig();
            config.setConnectionId(connectionID);
            config.setConnectionUrl(connectionURL);
            Skyflow skyflowClient = Skyflow.builder().build();
            skyflowClient.addConnectionConfig(config);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testAddingExistingConnectionConfigInSkyflowClient() {
        try {
            ConnectionConfig config = new ConnectionConfig();
            config.setConnectionId(connectionID);
            config.setConnectionUrl(connectionURL);
            Skyflow skyflowClient = Skyflow.builder().addConnectionConfig(config).build();
            skyflowClient.addConnectionConfig(config);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.ConnectionIdAlreadyInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testUpdatingNonExistentConnectionConfigInSkyflowBuilder() {
        try {
            ConnectionConfig config = new ConnectionConfig();
            config.setConnectionId(connectionID);
            config.setConnectionUrl(connectionURL);
            Skyflow.builder().updateConnectionConfig(config).build();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.ConnectionIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testUpdatingNonExistentConnectionConfigInSkyflowClient() {
        try {
            ConnectionConfig config = new ConnectionConfig();
            config.setConnectionId(connectionID);
            config.setConnectionUrl(connectionURL);
            Skyflow skyflowClient = Skyflow.builder().build();
            skyflowClient.updateConnectionConfig(config);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.ConnectionIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testUpdatingValidConnectionConfigInSkyflowClient() {
        try {
            ConnectionConfig config = new ConnectionConfig();
            config.setConnectionId(connectionID);
            config.setConnectionUrl(connectionURL);
            Skyflow skyflowClient = Skyflow.builder().addConnectionConfig(config).build();

            Credentials credentials = new Credentials();
            credentials.setToken(token);

            config.setConnectionUrl(newConnectionURL);
            config.setCredentials(credentials);

            skyflowClient.updateConnectionConfig(config);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testRemovingNonExistentConnectionConfigInSkyflowBuilder() {
        try {
            Skyflow.builder().removeConnectionConfig(connectionID).build();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.ConnectionIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testRemovingNonExistentConnectionConfigInSkyflowClient() {
        try {
            Skyflow skyflowClient = Skyflow.builder().build();
            skyflowClient.removeConnectionConfig(connectionID);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.ConnectionIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testRemovingValidConnectionConfigInSkyflowClient() {
        try {
            ConnectionConfig config = new ConnectionConfig();
            config.setConnectionId(connectionID);
            config.setConnectionUrl(connectionURL);
            Skyflow skyflowClient = Skyflow.builder().addConnectionConfig(config).build();
            skyflowClient.removeConnectionConfig(connectionID);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGettingNonExistentConnectionConfigInSkyflowClient() {
        try {
            Skyflow skyflowClient = Skyflow.builder().build();
            ConnectionConfig config = skyflowClient.getConnectionConfig(connectionID);
            Assert.assertNull(config);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testAddingInvalidSkyflowCredentialsInSkyflowBuilder() {
        try {
            Credentials credentials = new Credentials();
            Skyflow.builder().addSkyflowCredentials(credentials).build();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.NoTokenGenerationMeansPassed.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testUpdatingValidSkyflowCredentialsInSkyflowClient() {
        try {
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId(vaultID);
            vaultConfig.setClusterId(clusterID);

            ConnectionConfig connectionConfig = new ConnectionConfig();
            connectionConfig.setConnectionId(connectionID);
            connectionConfig.setConnectionUrl(connectionURL);

            Credentials credentials = new Credentials();
            credentials.setToken(token);
            Skyflow skyflowClient = Skyflow.builder().addVaultConfig(vaultConfig).addConnectionConfig(connectionConfig).build();
            skyflowClient.updateSkyflowCredentials(credentials);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testDefaultLogLevel() {
        try {
            Skyflow skyflowClient = Skyflow.builder().setLogLevel(null).build();
            Assert.assertEquals(LogLevel.ERROR, skyflowClient.getLogLevel());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testUpdateLogLevel() {
        try {
            Skyflow skyflowClient = Skyflow.builder().setLogLevel(LogLevel.INFO).build();
            Assert.assertEquals(LogLevel.INFO, skyflowClient.getLogLevel());
            skyflowClient.updateLogLevel(LogLevel.WARN);
            Assert.assertEquals(LogLevel.WARN, skyflowClient.getLogLevel());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

}
