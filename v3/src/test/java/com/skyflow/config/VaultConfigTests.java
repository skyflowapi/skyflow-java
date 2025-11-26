package com.skyflow.config;

import com.skyflow.enums.Env;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.validations.Validations;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class VaultConfigTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String vaultID = null;
    private static String clusterID = null;
    private static Credentials credentials = null;
    private static String vaultURL = null;

    @BeforeClass
    public static void setup() {
        vaultID = "vault123";
        clusterID = "cluster123";
        vaultURL = "https://vault.url.com";

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
            Validations.validateVaultConfiguration(vaultConfig);

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
            Validations.validateVaultConfiguration(vaultConfig);

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
            Validations.validateVaultConfiguration(vaultConfig);

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
            Validations.validateVaultConfiguration(vaultConfig);

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
            Validations.validateVaultConfiguration(vaultConfig);
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
            Validations.validateVaultConfiguration(vaultConfig);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyVaultId.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyClusterIdInVaultConfigInValidations() {
        try {
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId(vaultID);
            vaultConfig.setClusterId("");
            vaultConfig.setEnv(Env.SANDBOX);
            Validations.validateVaultConfiguration(vaultConfig);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyClusterId.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyVaultURLInVaultConfigInValidations() {
        try {
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId(vaultID);
            vaultConfig.setVaultURL("");
            vaultConfig.setEnv(Env.SANDBOX);
            Validations.validateVaultConfiguration(vaultConfig);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyVaultUrl.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNeitherVaultURLNorClusterIdInVaultConfigInValidations() {
        try {
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId(vaultID);
            vaultConfig.setEnv(Env.SANDBOX);
            Validations.validateVaultConfiguration(vaultConfig);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EitherVaultUrlOrClusterIdRequired.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testMalformedVaultURLInVaultConfigInValidations() {
        try {
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId(vaultID);
            vaultConfig.setEnv(Env.SANDBOX);

            String[] malformedUrls = new String[]{"malformed url", "http://www.url.com", "https://"};

            for (String malformedUrl : malformedUrls) {
                try {
                    System.out.println(malformedUrl);
                    vaultConfig.setVaultURL(malformedUrl);
                    Validations.validateVaultConfiguration(vaultConfig);
                    Assert.fail(EXCEPTION_NOT_THROWN);
                } catch (SkyflowException e) {
                    Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
                    Assert.assertEquals(ErrorMessage.InvalidVaultUrlFormat.getMessage(), e.getMessage());
                }
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testValidVaultURLInVaultConfigInValidations() {
        try {
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId(vaultID);
            vaultConfig.setVaultURL(vaultURL);
            vaultConfig.setEnv(Env.SANDBOX);
            Validations.validateVaultConfiguration(vaultConfig);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
