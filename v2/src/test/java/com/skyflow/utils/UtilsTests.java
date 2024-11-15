package com.skyflow.utils;

import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.enums.Env;
import com.skyflow.enums.RequestMethod;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.connection.InvokeConnectionRequest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UtilsTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String clusterId = null;
    private static String url = null;
    private static String filePath = null;
    private static String credentialsString = null;
    private static String token = null;
    private static String context = null;
    private static ArrayList<String> roles = null;
    private static String connectionId = null;
    private static String connectionUrl = null;
    private static Map<String, String> queryParams;
    private static Map<String, String> pathParams;
    private static Map<String, String> requestHeaders;

    @BeforeClass
    public static void setup() {
        clusterId = "test_cluster_id";
        url = "https://test-url.com/java/unit/tests";
        filePath = "invalid/file/path/credentials.json";
        credentialsString = "invalid credentials string";
        token = "invalid-token";
        context = "test_context";
        roles = new ArrayList<>();
        String role = "test_role";
        roles.add(role);
        connectionId = "test_connection_id";
        connectionUrl = "https://test.connection.url";
        pathParams = new HashMap<>();
        queryParams = new HashMap<>();
        requestHeaders = new HashMap<>();
    }

    @Test
    public void testGetVaultURLForDev() {
        try {
            String vaultURL = Utils.getVaultURL(clusterId, Env.DEV);
            String devUrl = "https://test_cluster_id.vault.skyflowapis.dev";
            Assert.assertEquals(devUrl, vaultURL);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetVaultURLForStage() {
        try {
            String vaultURL = Utils.getVaultURL(clusterId, Env.STAGE);
            String stageUrl = "https://test_cluster_id.vault.skyflowapis.tech";
            Assert.assertEquals(stageUrl, vaultURL);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetVaultURLForSandbox() {
        try {
            String vaultURL = Utils.getVaultURL(clusterId, Env.SANDBOX);
            String sandboxUrl = "https://test_cluster_id.vault.skyflowapis-preview.com";
            Assert.assertEquals(sandboxUrl, vaultURL);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetVaultURLForProd() {
        try {
            String vaultURL = Utils.getVaultURL(clusterId, Env.PROD);
            String prodUrl = "https://test_cluster_id.vault.skyflowapis.com";
            Assert.assertEquals(prodUrl, vaultURL);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetBaseURL() {
        try {
            String baseURL = Utils.getBaseURL(url);
            String url = "https://test-url.com";
            Assert.assertEquals(url, baseURL);
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
            Utils.generateBearerToken(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.InvalidCredentials.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testGenerateBearerTokenWithCredentialsString() {
        try {
            Credentials credentials = new Credentials();
            credentials.setCredentialsString(credentialsString);
            credentials.setContext(context);
            credentials.setRoles(roles);
            Utils.generateBearerToken(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
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
            String bearerToken = Utils.generateBearerToken(credentials);
            Assert.assertEquals(token, bearerToken);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testConstructConnectionURL() {
        try {
            queryParams.put("query_param", "value");
            pathParams.put("path_param", "value");

            ConnectionConfig connectionConfig = new ConnectionConfig();
            connectionConfig.setConnectionId(connectionId);
            connectionConfig.setConnectionUrl(connectionUrl);

            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .methodName(RequestMethod.POST).pathParams(pathParams).queryParams(queryParams).build();
            String filledUrl = Utils.constructConnectionURL(connectionConfig, request);
            Assert.assertEquals(connectionUrl + "?" + "query_param=value", filledUrl);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testConstructConnectionHeaderMap() {
        try {
            requestHeaders.put("HEADER", "value");
            Map<String, String> headers = Utils.constructConnectionHeadersMap(requestHeaders);
            Assert.assertEquals(1, headers.size());
            Assert.assertTrue(headers.containsKey("header"));
            Assert.assertFalse(headers.containsKey("HEADER"));
            Assert.assertEquals("value", headers.get("header"));
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
