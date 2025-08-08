package com.skyflow.v2.config;

import com.skyflow.common.config.Credentials;
import com.skyflow.common.config.VaultConfig;
import com.skyflow.common.enums.Env;
import com.skyflow.common.errors.ErrorCode;
import com.skyflow.common.errors.ErrorMessage;
import com.skyflow.common.errors.SkyflowException;
import com.skyflow.v2.utils.validations.Validations;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class VaultConfigTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String vaultID = null;
    private static String clusterID = null;
    private static Credentials credentials = null;


    @BeforeClass
    public static void setup() {
        vaultID = "vault123";
        clusterID = "cluster123";

        credentials = new Credentials();
        credentials.setToken("valid-token");

    }

    @Test
    public void testValidVaultConfigWithCredentialsInValidations() {
        try {
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId(vaultID);
            vaultConfig.setClusterId(clusterID);
            vaultConfig.setEnv(Env.SANDBOX);
            vaultConfig.setCredentials(credentials);
            Validations.validateVaultConfig(vaultConfig);

            Assert.assertEquals(vaultID, vaultConfig.getVaultId());
            Assert.assertEquals(clusterID, vaultConfig.getClusterId());
            Assert.assertEquals(Env.SANDBOX, vaultConfig.getEnv());
            Assert.assertNotNull(vaultConfig.getCredentials());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidVaultConfigWithoutCredentialsInValidations() {
        try {
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId(vaultID);
            vaultConfig.setClusterId(clusterID);
            vaultConfig.setEnv(Env.SANDBOX);
            Validations.validateVaultConfig(vaultConfig);

            Assert.assertEquals(vaultID, vaultConfig.getVaultId());
            Assert.assertEquals(clusterID, vaultConfig.getClusterId());
            Assert.assertEquals(Env.SANDBOX, vaultConfig.getEnv());
            Assert.assertNull(vaultConfig.getCredentials());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testDefaultEnvInVaultConfigWithCredentialsInValidations() {
        try {
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId(vaultID);
            vaultConfig.setClusterId(clusterID);
            vaultConfig.setCredentials(credentials);
            Validations.validateVaultConfig(vaultConfig);

            Assert.assertEquals(vaultID, vaultConfig.getVaultId());
            Assert.assertEquals(clusterID, vaultConfig.getClusterId());
            Assert.assertEquals(Env.PROD, vaultConfig.getEnv());
            Assert.assertNotNull(vaultConfig.getCredentials());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testDefaultEnvInVaultConfigWithoutCredentialsInValidations() {
        try {
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId(vaultID);
            vaultConfig.setClusterId(clusterID);
            vaultConfig.setEnv(null);
            Validations.validateVaultConfig(vaultConfig);

            Assert.assertEquals(vaultID, vaultConfig.getVaultId());
            Assert.assertEquals(clusterID, vaultConfig.getClusterId());
            Assert.assertEquals(Env.PROD, vaultConfig.getEnv());
            Assert.assertNull(vaultConfig.getCredentials());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testNoVaultIdInVaultConfigInValidations() {
        VaultConfig vaultConfig = new VaultConfig();
        try {
            vaultConfig.setClusterId(clusterID);
            vaultConfig.setEnv(Env.SANDBOX);
            Validations.validateVaultConfig(vaultConfig);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.InvalidVaultId.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyVaultIdInVaultConfigInValidations() {
        try {
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId("");
            vaultConfig.setClusterId(clusterID);
            vaultConfig.setEnv(Env.SANDBOX);
            Validations.validateVaultConfig(vaultConfig);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyVaultId.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNoClusterIdInVaultConfigInValidations() {
        try {
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId(vaultID);
            vaultConfig.setEnv(Env.SANDBOX);
            Validations.validateVaultConfig(vaultConfig);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.InvalidClusterId.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyClusterIdInVaultConfigInValidations() {
        try {
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId(vaultID);
            vaultConfig.setClusterId("");
            vaultConfig.setEnv(Env.SANDBOX);
            Validations.validateVaultConfig(vaultConfig);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyClusterId.getMessage(), e.getMessage());
        }
    }
}
