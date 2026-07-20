package com.skyflow;

import com.skyflow.config.BaseVaultConfig;
import com.skyflow.config.Credentials;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class BaseSkyflowTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String vaultID = null;
    private static String clusterID = null;
    private static String newClusterID = null;
    private static String token = null;

    @BeforeClass
    public static void setup() {
        vaultID = "test_vault_id";
        clusterID = "test_cluster_id";
        newClusterID = "new_test_cluster_id";
        token = "test_token";
    }

    private static BaseVaultConfig newConfig(String vaultId, String clusterId, Env env) {
        BaseVaultConfig config = new BaseVaultConfig();
        config.setVaultId(vaultId);
        config.setClusterId(clusterId);
        config.setEnv(env);
        return config;
    }

    @Test
    public void testAddingExistingVaultConfigThrows() {
        try {
            BaseVaultConfig config = newConfig(vaultID, clusterID, Env.SANDBOX);
            TestSkyflow client = TestSkyflow.builder().build();
            client.addVaultConfig(config).addVaultConfig(config);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.VaultIdAlreadyInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testUpdatingNonExistentVaultConfigInBuilderThrows() {
        try {
            BaseVaultConfig config = newConfig(vaultID, clusterID, Env.SANDBOX);
            TestSkyflow.builder().updateVaultConfig(config).build();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.VaultIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testUpdatingNonExistentVaultConfigInClientThrows() {
        try {
            BaseVaultConfig config = newConfig(vaultID, clusterID, Env.SANDBOX);
            TestSkyflow client = TestSkyflow.builder().build();
            client.updateVaultConfig(config);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.VaultIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testRemovingNonExistentVaultConfigInBuilderThrows() {
        try {
            TestSkyflow.builder().removeVaultConfig(vaultID).build();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.VaultIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testRemovingExistingVaultConfigSucceeds() {
        try {
            BaseVaultConfig config = newConfig(vaultID, clusterID, Env.SANDBOX);
            TestSkyflow client = TestSkyflow.builder().addVaultConfig(config).build();
            client.removeVaultConfig(vaultID);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testRemovingVaultConfigWithoutAddingThrows() {
        try {
            TestSkyflow client = TestSkyflow.builder().build();
            client.removeVaultConfig(vaultID);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.VaultIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testGettingNonExistentVaultConfigReturnsNull() {
        TestSkyflow client = TestSkyflow.builder().build();
        Assert.assertNull(client.getVaultConfig(vaultID));
    }

    @Test
    public void testVaultThrowsWhenNoConfigAdded() {
        try {
            TestSkyflow client = TestSkyflow.builder().build();
            client.vault();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.VaultIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testVaultByIdThrowsAfterRemoval() {
        try {
            BaseVaultConfig primary = newConfig(vaultID, clusterID, Env.SANDBOX);
            BaseVaultConfig secondary = newConfig(vaultID + "123", clusterID, Env.SANDBOX);
            TestSkyflow client = TestSkyflow.builder().addVaultConfig(primary).addVaultConfig(secondary).build();
            client.removeVaultConfig(vaultID);
            client.vault(vaultID);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.VaultIdNotInConfigList.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testDefaultLogLevel() {
        TestSkyflow client = TestSkyflow.builder().setLogLevel(null).build();
        Assert.assertEquals(LogLevel.ERROR, client.getLogLevel());
    }

    @Test
    public void testSetLogLevel() {
        TestSkyflow client = TestSkyflow.builder().setLogLevel(LogLevel.INFO).build();
        Assert.assertEquals(LogLevel.INFO, client.getLogLevel());
        client.setLogLevel(LogLevel.WARN);
        Assert.assertEquals(LogLevel.WARN, client.getLogLevel());
    }

    @Test
    public void testAddingInvalidSkyflowCredentialsThrows() {
        try {
            Credentials credentials = new Credentials();
            TestSkyflow.builder().addSkyflowCredentials(credentials).build();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.NoTokenGenerationMeansPassed.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testUpdatingValidSkyflowCredentialsSucceeds() {
        try {
            BaseVaultConfig config = newConfig(vaultID, clusterID, Env.SANDBOX);
            Credentials credentials = new Credentials();
            credentials.setToken(token);
            TestSkyflow client = TestSkyflow.builder().addVaultConfig(config).build();
            client.updateSkyflowCredentials(credentials);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testUpdateVaultConfigNullCredentialsFallsBackToPrevious() {
        try {
            Credentials creds = new Credentials();
            creds.setToken(token);
            BaseVaultConfig config = newConfig(vaultID, clusterID, Env.SANDBOX);
            config.setCredentials(creds);
            TestSkyflow client = TestSkyflow.builder().addVaultConfig(config).build();

            BaseVaultConfig partialUpdate = newConfig(vaultID, clusterID, Env.SANDBOX);
            client.updateVaultConfig(partialUpdate);
            Assert.assertNotNull(client.getVaultConfig(vaultID).getCredentials());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testUpdateVaultConfigWithNewClusterIdAndCredentialsUpdatesAllFields() {
        try {
            Credentials creds = new Credentials();
            creds.setToken(token);
            BaseVaultConfig config = newConfig(vaultID, clusterID, Env.DEV);
            config.setCredentials(creds);
            TestSkyflow client = TestSkyflow.builder().addVaultConfig(config).build();

            Credentials newCreds = new Credentials();
            newCreds.setToken("updated-token-value");
            BaseVaultConfig update = newConfig(vaultID, newClusterID, Env.PROD);
            update.setCredentials(newCreds);
            client.updateVaultConfig(update);

            Assert.assertEquals(newClusterID, client.getVaultConfig(vaultID).getClusterId());
            Assert.assertEquals(Env.PROD, client.getVaultConfig(vaultID).getEnv());
            Assert.assertEquals("updated-token-value", client.getVaultConfig(vaultID).getCredentials().getToken());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testUpdateVaultConfigWithNullEnvFallsBackToPreviousEnv() {
        // BaseVaultConfig's setEnv/constructor never store null (default to PROD), so getEnv()
        // never returns null via the normal API. Override it to exercise the fallback branch
        // in mergeVaultConfig.
        try {
            Credentials creds = new Credentials();
            creds.setToken(token);
            BaseVaultConfig initial = newConfig(vaultID, clusterID, Env.SANDBOX);
            initial.setCredentials(creds);
            TestSkyflow client = TestSkyflow.builder().addVaultConfig(initial).build();

            BaseVaultConfig updateWithNullEnv = new BaseVaultConfig() {
                @Override
                public Env getEnv() {
                    return null;
                }
            };
            updateWithNullEnv.setVaultId(vaultID);
            updateWithNullEnv.setClusterId(clusterID);
            updateWithNullEnv.setCredentials(creds);

            client.updateVaultConfig(updateWithNullEnv);
            Assert.assertEquals(Env.SANDBOX, client.getVaultConfig(vaultID).getEnv());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testMergeVaultConfigWithNullClusterIdFallsBackToPreviousClusterId() {
        BaseVaultConfig existing = newConfig(vaultID, clusterID, Env.DEV);
        BaseVaultConfig incoming = new BaseVaultConfig();
        incoming.setVaultId(vaultID);
        // clusterId intentionally left null

        BaseVaultConfig result = TestSkyflow.builder().mergeVaultConfig(incoming, existing);
        Assert.assertEquals(clusterID, result.getClusterId());
    }

    private static class TestSkyflow extends BaseSkyflow<TestSkyflow, BaseVaultConfig> {
        private final TestSkyflowClientBuilder builder;

        private TestSkyflow(TestSkyflowClientBuilder builder) {
            super(builder);
            this.builder = builder;
        }

        static TestSkyflowClientBuilder builder() {
            return new TestSkyflowClientBuilder();
        }

        @Override
        protected TestSkyflow self() {
            return this;
        }

        Object vault() throws SkyflowException {
            return resolveOrThrow(this.builder.vaultClientsMap, null,
                    ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST, ErrorMessage.VaultIdNotInConfigList);
        }

        Object vault(String vaultId) throws SkyflowException {
            return resolveOrThrow(this.builder.vaultClientsMap, vaultId,
                    ErrorLogs.VAULT_CONFIG_DOES_NOT_EXIST, ErrorMessage.VaultIdNotInConfigList);
        }

        private static class TestSkyflowClientBuilder extends BaseSkyflowClientBuilder<BaseVaultConfig> {
            private final java.util.LinkedHashMap<String, Object> vaultClientsMap = new java.util.LinkedHashMap<>();

            @Override
            protected void validateVaultConfig(BaseVaultConfig vaultConfig) throws SkyflowException {
                if (vaultConfig.getVaultId() == null || vaultConfig.getVaultId().trim().isEmpty()) {
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyVaultId.getMessage());
                }
            }

            @Override
            protected void onVaultConfigAdded(BaseVaultConfig vaultConfig) {
                this.vaultClientsMap.put(vaultConfig.getVaultId(), new Object());
            }

            @Override
            protected void onVaultConfigUpdated(BaseVaultConfig updatedConfig) {
                this.vaultClientsMap.put(updatedConfig.getVaultId(), new Object());
            }

            @Override
            protected void onVaultConfigRemoved(String vaultId) {
                this.vaultClientsMap.remove(vaultId);
            }

            @Override
            protected void onCredentialsUpdated(Credentials credentials) {
                // no-op: this test double only exercises template orchestration, not propagation
            }

            @Override
            public TestSkyflowClientBuilder addVaultConfig(BaseVaultConfig vaultConfig) throws SkyflowException {
                super.addVaultConfig(vaultConfig);
                return this;
            }

            @Override
            public TestSkyflowClientBuilder updateVaultConfig(BaseVaultConfig vaultConfig) throws SkyflowException {
                super.updateVaultConfig(vaultConfig);
                return this;
            }

            @Override
            public TestSkyflowClientBuilder removeVaultConfig(String vaultId) throws SkyflowException {
                super.removeVaultConfig(vaultId);
                return this;
            }

            @Override
            public TestSkyflowClientBuilder addSkyflowCredentials(Credentials credentials) throws SkyflowException {
                super.addSkyflowCredentials(credentials);
                return this;
            }

            @Override
            public TestSkyflowClientBuilder setLogLevel(LogLevel logLevel) {
                super.setLogLevel(logLevel);
                return this;
            }

            TestSkyflow build() {
                return new TestSkyflow(this);
            }
        }
    }
}
