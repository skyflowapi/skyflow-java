package com.skyflow.v2.vault.tokens;

import com.skyflow.v2.Skyflow;
import com.skyflow.v2.config.Credentials;
import com.skyflow.v2.config.VaultConfig;
import com.skyflow.v2.enums.Env;
import com.skyflow.v2.errors.ErrorCode;
import com.skyflow.v2.errors.ErrorMessage;
import com.skyflow.v2.errors.SkyflowException;
import com.skyflow.v2.utils.validations.Validations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TokenizeTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String vaultID = null;
    private static String clusterID = null;
    private static VaultConfig vaultConfig = null;
    private static String token = null;
    private static ArrayList<String> tokens = null;
    private static Skyflow skyflowClient = null;
    private static List<ColumnValue> columnValues = null;
    private static ColumnValue columnValue = null;
    private static String value = null;
    private static String group = null;

    @BeforeClass
    public static void setup() {
        vaultID = "vault123";
        clusterID = "cluster123";

        Credentials credentials = new Credentials();
        credentials.setToken("valid-token");

        vaultConfig = new VaultConfig();
        vaultConfig.setVaultId(vaultID);
        vaultConfig.setClusterId(clusterID);
        vaultConfig.setEnv(Env.DEV);
        vaultConfig.setCredentials(credentials);

        columnValues = new ArrayList<>();
        value = "test_value";
        group = "test_group";

        tokens = new ArrayList<>();
    }

    @Before
    public void setupTest() {
        columnValues.clear();
    }

    @Test
    public void testValidInputInTokenizeRequestValidations() {
        try {
            columnValue = ColumnValue.builder().value(value).columnGroup(group).build();
            columnValues.add(columnValue);
            TokenizeRequest request = TokenizeRequest.builder().values(columnValues).build();
            Validations.validateTokenizeRequest(request);
            Assert.assertEquals(1, request.getColumnValues().size());
            Assert.assertEquals(value, request.getColumnValues().get(0).getValue());
            Assert.assertEquals(group, request.getColumnValues().get(0).getColumnGroup());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testNoColumnValuesInTokenizeRequestValidations() {
        try {
            TokenizeRequest request = TokenizeRequest.builder().build();
            Validations.validateTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.ColumnValuesKeyErrorTokenize.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyColumnValuesInTokenizeRequestValidations() {
        try {
            TokenizeRequest request = TokenizeRequest.builder().values(columnValues).build();
            Validations.validateTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyColumnValues.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNullColumnValueInColumnValuesInTokenizeRequestValidations() {
        try {
            columnValue = ColumnValue.builder().value(null).columnGroup(group).build();
            columnValues.add(columnValue);
            TokenizeRequest request = TokenizeRequest.builder().values(columnValues).build();
            Validations.validateTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyValueInColumnValues.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyColumnValueInColumnValuesInTokenizeRequestValidations() {
        try {
            columnValue = ColumnValue.builder().value("").columnGroup(group).build();
            columnValues.add(columnValue);
            TokenizeRequest request = TokenizeRequest.builder().values(columnValues).build();
            Validations.validateTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyValueInColumnValues.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNullColumnGroupInColumnValuesInTokenizeRequestValidations() {
        try {
            columnValue = ColumnValue.builder().value(value).columnGroup(null).build();
            columnValues.add(columnValue);
            TokenizeRequest request = TokenizeRequest.builder().values(columnValues).build();
            Validations.validateTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyColumnGroupInColumnValue.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyColumnGroupInColumnValuesInTokenizeRequestValidations() {
        try {
            columnValue = ColumnValue.builder().value(value).columnGroup("").build();
            columnValues.add(columnValue);
            TokenizeRequest request = TokenizeRequest.builder().values(columnValues).build();
            Validations.validateTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyColumnGroupInColumnValue.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testTokenizeResponse() {
        try {
            tokens.add("1234-5678-9012-3456");
            tokens.add("5678-9012-3456-7890");
            TokenizeResponse response = new TokenizeResponse(tokens);
            String responseString = "{\"tokens\":[" +
                    "{\"token\":\"1234-5678-9012-3456\"},{\"token\":\"5678-9012-3456-7890\"}]" +
                    ",\"errors\":null}";
            Assert.assertEquals(2, response.getTokens().size());
            Assert.assertEquals(responseString, response.toString());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
