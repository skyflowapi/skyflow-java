package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.logger.LogUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class SkyflowTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";

    private static class CapturingHandler extends Handler {
        final List<LogRecord> records = new ArrayList<>();
        @Override public void publish(LogRecord r) { records.add(r); }
        @Override public void flush() {}
        @Override public void close() {}
    }

    private CapturingHandler attachCapture() {
        CapturingHandler handler = new CapturingHandler();
        handler.setLevel(Level.ALL);
        Logger.getLogger(LogUtil.class.getName()).addHandler(handler);
        return handler;
    }
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
    public void testUpdateConnectionConfigWithNullFieldsFallsBackToPrevious() {
        try {
            ConnectionConfig config = new ConnectionConfig();
            config.setConnectionId(connectionID);
            config.setConnectionUrl(connectionURL);

            Credentials creds = new Credentials();
            creds.setToken(token);
            config.setCredentials(creds);

            Skyflow skyflowClient = Skyflow.builder().addConnectionConfig(config).build();

            // Update with null credentials — validation requires connectionUrl, so provide it;
            // credentials should fall back to previous value
            ConnectionConfig partialUpdate = new ConnectionConfig();
            partialUpdate.setConnectionId(connectionID);
            partialUpdate.setConnectionUrl(connectionURL);
            // credentials is null → should retain previous value
            skyflowClient.updateConnectionConfig(partialUpdate);
            Assert.assertNotNull(skyflowClient.getConnectionConfig(connectionID).getCredentials());
        } catch (SkyflowException e) {
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
    @SuppressWarnings("deprecation")
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

    @Test
    @SuppressWarnings("deprecation")
    public void testUpdateLogLevelEmitsDeprecationWarning() {
        try {
            // build() calls setupLogger internally — attach capture after so it isn't wiped
            Skyflow skyflowClient = Skyflow.builder().setLogLevel(LogLevel.INFO).build();
            CapturingHandler handler = attachCapture();
            skyflowClient.updateLogLevel(LogLevel.WARN);
            boolean warnFired = handler.records.stream()
                    .anyMatch(r -> r.getLevel().equals(Level.WARNING)
                            && r.getMessage().contains(InfoLogs.DEPRECATED_UPDATE_LOG_LEVEL.getLog()));
            Assert.assertTrue("updateLogLevel() should emit a deprecation warning log", warnFired);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testUpdateLogLevelWarningIsSuppressedAtErrorLevel() {
        try {
            Skyflow skyflowClient = Skyflow.builder().setLogLevel(LogLevel.ERROR).build();
            CapturingHandler handler = attachCapture();
            skyflowClient.updateLogLevel(LogLevel.WARN);
            boolean warnFired = handler.records.stream()
                    .anyMatch(r -> r.getLevel().equals(Level.WARNING));
            Assert.assertFalse("updateLogLevel() warning should be suppressed at ERROR log level", warnFired);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testVaultMethodWithNoConfig() {
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
    public void testVaultMethodWithValidConfig() {
        try {
            VaultConfig config = new VaultConfig();
            config.setVaultId(vaultID);
            config.setClusterId(clusterID);
            config.setEnv(Env.SANDBOX);
            Skyflow skyflowClient = Skyflow.builder().addVaultConfig(config).build();
            Assert.assertNotNull(skyflowClient.vault());
            Assert.assertNotNull(skyflowClient.vault(vaultID));
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testVaultMethodWithInvalidVaultId() {
        try {
            VaultConfig config = new VaultConfig();
            config.setVaultId(vaultID);
            config.setClusterId(clusterID);
            config.setEnv(Env.SANDBOX);
            Skyflow skyflowClient = Skyflow.builder().addVaultConfig(config).build();
            skyflowClient.vault("invalid_vault_id");
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.VaultIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testConnectionMethodWithNoConfig() {
        try {
            Skyflow skyflowClient = Skyflow.builder().build();
            skyflowClient.connection();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.ConnectionIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testConnectionMethodWithValidConfig() {
        try {
            ConnectionConfig config = new ConnectionConfig();
            config.setConnectionId(connectionID);
            config.setConnectionUrl(connectionURL);
            Skyflow skyflowClient = Skyflow.builder().addConnectionConfig(config).build();
            Assert.assertNotNull(skyflowClient.connection());
            Assert.assertNotNull(skyflowClient.connection(connectionID));
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testConnectionMethodWithInvalidConnectionId() {
        try {
            ConnectionConfig config = new ConnectionConfig();
            config.setConnectionId(connectionID);
            config.setConnectionUrl(connectionURL);
            Skyflow skyflowClient = Skyflow.builder().addConnectionConfig(config).build();
            skyflowClient.connection("invalid_connection_id");
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.ConnectionIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testDetectMethodWithNoConfig() {
        try {
            Skyflow skyflowClient = Skyflow.builder().build();
            skyflowClient.detect();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.VaultIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testDetectMethodWithValidConfig() {
        try {
            VaultConfig config = new VaultConfig();
            config.setVaultId(vaultID);
            config.setClusterId(clusterID);
            config.setEnv(Env.SANDBOX);
            Skyflow skyflowClient = Skyflow.builder().addVaultConfig(config).build();
            Assert.assertNotNull(skyflowClient.detect());
            Assert.assertNotNull(skyflowClient.detect(vaultID));
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testDetectMethodWithInvalidVaultId() {
        try {
            VaultConfig config = new VaultConfig();
            config.setVaultId(vaultID);
            config.setClusterId(clusterID);
            config.setEnv(Env.SANDBOX);
            Skyflow skyflowClient = Skyflow.builder().addVaultConfig(config).build();
            skyflowClient.detect("invalid_vault_id");
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.VaultIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testUpdateConnectionConfig_withNewCredentials_updatesCredentials() {
        try {
            ConnectionConfig config = new ConnectionConfig();
            config.setConnectionId(connectionID);
            config.setConnectionUrl(connectionURL);
            Credentials oldCreds = new Credentials();
            oldCreds.setToken(token);
            config.setCredentials(oldCreds);
            Skyflow skyflowClient = Skyflow.builder().addConnectionConfig(config).build();

            // Update with new non-null credentials and new non-null connectionUrl — covers
            // the non-null (true) branches for both ternaries in findAndUpdateConnectionConfig
            Credentials newCreds = new Credentials();
            newCreds.setToken("new-token-value");
            ConnectionConfig update = new ConnectionConfig();
            update.setConnectionId(connectionID);
            update.setConnectionUrl(newConnectionURL);
            update.setCredentials(newCreds);
            skyflowClient.updateConnectionConfig(update);
            Assert.assertEquals("new-token-value", skyflowClient.getConnectionConfig(connectionID).getCredentials().getToken());
            Assert.assertEquals(newConnectionURL, skyflowClient.getConnectionConfig(connectionID).getConnectionUrl());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testFindAndUpdateConnectionConfig_withNullConnectionUrl_fallsBackToPreviousUrl() {
        // `findAndUpdateConnectionConfig` has a ternary for connectionUrl that falls back
        // to previousConfig.getConnectionUrl() when the incoming url is null.
        // Since validation enforces non-null url, we call the private method directly
        // via reflection to cover the false branch.
        try {
            ConnectionConfig initial = new ConnectionConfig();
            initial.setConnectionId(connectionID);
            initial.setConnectionUrl(connectionURL);
            Skyflow skyflowClient = Skyflow.builder().addConnectionConfig(initial).build();

            java.lang.reflect.Field builderField = Skyflow.class.getDeclaredField("builder");
            builderField.setAccessible(true);
            Object builder = builderField.get(skyflowClient);

            ConnectionConfig nullUrlConfig = new ConnectionConfig();
            nullUrlConfig.setConnectionId(connectionID);
            // connectionUrl not set → remains null

            java.lang.reflect.Method method = builder.getClass().getDeclaredMethod(
                    "findAndUpdateConnectionConfig", ConnectionConfig.class);
            method.setAccessible(true);
            ConnectionConfig result = (ConnectionConfig) method.invoke(builder, nullUrlConfig);

            Assert.assertEquals(connectionURL, result.getConnectionUrl());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        } catch (Exception e) {
            Assert.fail("Reflection failed: " + e.getMessage());
        }
    }
}
