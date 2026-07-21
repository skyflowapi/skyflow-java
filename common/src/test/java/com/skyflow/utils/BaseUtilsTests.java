package com.skyflow.utils;

import com.skyflow.config.Credentials;
import com.skyflow.enums.Env;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

public class BaseUtilsTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String clusterId = null;
    private static String url = null;
    private static String filePath = null;
    private static String credentialsString = null;
    private static String token = null;
    private static String context = null;
    private static ArrayList<String> roles = null;

    @BeforeClass
    public static void setup() {
        clusterId = "test_cluster_id";
        url = "https://test-url.com/java/unit/tests";
        filePath = "invalid/file/path/credentials.json";
        credentialsString = "invalid credentials string";
        token = "invalid-token";
        context = "test_context";
        roles = new ArrayList<>();
        roles.add("test_role");
    }

    @Test
    public void testGetVaultURLForDev() {
        try {
            String vaultURL = BaseUtils.getVaultURL(clusterId, Env.DEV, BaseConstants.V2_VAULT_DOMAIN);
            String devUrl = "https://test_cluster_id.vault.skyflowapis.dev";
            Assert.assertEquals(devUrl, vaultURL);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetVaultURLForStage() {
        try {
            String vaultURL = BaseUtils.getVaultURL(clusterId, Env.STAGE, BaseConstants.V2_VAULT_DOMAIN);
            String stageUrl = "https://test_cluster_id.vault.skyflowapis.tech";
            Assert.assertEquals(stageUrl, vaultURL);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetVaultURLForSandbox() {
        try {
            String vaultURL = BaseUtils.getVaultURL(clusterId, Env.SANDBOX, BaseConstants.V2_VAULT_DOMAIN);
            String sandboxUrl = "https://test_cluster_id.vault.skyflowapis-preview.com";
            Assert.assertEquals(sandboxUrl, vaultURL);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetVaultURLForProd() {
        try {
            String vaultURL = BaseUtils.getVaultURL(clusterId, Env.PROD, BaseConstants.V2_VAULT_DOMAIN);
            String prodUrl = "https://test_cluster_id.vault.skyflowapis.com";
            Assert.assertEquals(prodUrl, vaultURL);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetBaseURL() {
        try {
            String baseURL = BaseUtils.getBaseURL(url);
            String expected = "https://test-url.com";
            Assert.assertEquals(expected, baseURL);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGenerateBearerTokenWithCredentialsFile() {
        try {
            Credentials credentials = new Credentials();
            credentials.setPath(filePath);
            credentials.setContext(context);
            credentials.setRoles(roles);
            BaseUtils.generateBearerToken(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    BaseUtils.parameterizedString(ErrorMessage.FileNotFound.getMessage(), filePath),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testGenerateBearerTokenWithCredentialsString() {
        try {
            Credentials credentials = new Credentials();
            credentials.setCredentialsString(credentialsString);
            credentials.setContext(context);
            credentials.setRoles(roles);
            BaseUtils.generateBearerToken(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.CredentialsStringInvalidJson.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testGenerateBearerTokenWithToken() {
        try {
            Credentials credentials = new Credentials();
            credentials.setToken(token);
            credentials.setContext(context);
            credentials.setRoles(roles);
            String bearerToken = BaseUtils.generateBearerToken(credentials);
            Assert.assertEquals(token, bearerToken);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
