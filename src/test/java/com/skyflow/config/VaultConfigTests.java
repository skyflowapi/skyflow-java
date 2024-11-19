package com.skyflow.config;

import com.skyflow.enums.Env;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.utils.validations.Validations;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.skyflow.serviceaccount.util.Token")
public class VaultConfigTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String vaultID = null;
    private static String clusterID = null;
    private static Credentials credentials = null;


    @BeforeClass
    public static void setup() throws SkyflowException, NoSuchMethodException {
        PowerMockito.mockStatic(Token.class);
        PowerMockito.when(Token.isExpired("valid_token")).thenReturn(true);
        PowerMockito.when(Token.isExpired("not_a_valid_token")).thenReturn(false);
        PowerMockito.mock(ApiClient.class);

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
