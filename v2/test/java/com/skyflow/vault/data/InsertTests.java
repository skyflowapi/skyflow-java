package com.skyflow.vault.data;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.TokenMode;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Constants;
import com.skyflow.utils.SdkVersion;
import com.skyflow.utils.Utils;
import com.skyflow.utils.validations.Validations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

public class InsertTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static final String requestId = "95be08fc-4d13-4335-8b8d-24e85d53ed1d";
    private static String vaultID = null;
    private static String clusterID = null;
    private static String table = null;
    private static ArrayList<HashMap<String, Object>> values = null;
    private static ArrayList<HashMap<String, Object>> tokens = null;
    private static HashMap<String, Object> valueMap = null;
    private static HashMap<String, Object> tokenMap = null;
    private static String upsert = null;
    private static Skyflow skyflowClient = null;

    @BeforeClass
    public static void setup() {

        vaultID = "vault123";
        clusterID = "cluster123";

        Credentials credentials = new Credentials();
        credentials.setToken("valid-token");

        VaultConfig vaultConfig = new VaultConfig();
        vaultConfig.setVaultId(vaultID);
        vaultConfig.setClusterId(clusterID);
        vaultConfig.setEnv(Env.DEV);
        vaultConfig.setCredentials(credentials);

        table = "test_table";
        values = new ArrayList<>();
        tokens = new ArrayList<>();
        valueMap = new HashMap<>();
        tokenMap = new HashMap<>();
        upsert = "upsert_column";
        SdkVersion.setSdkPrefix(Constants.SDK_PREFIX);
    }

    @Before
    public void setupTest() {
        values.clear();
        tokens.clear();
        valueMap.clear();
        valueMap.put("test_column_1", "test_value_1");
        valueMap.put("test_column_2", "test_value_2");
        tokenMap.clear();
        tokenMap.put("test_column_1", "test_token_1");
    }

    @Test
    public void testValidInputInInsertRequestValidations() {
        try {
            values.add(valueMap);
            tokens.add(tokenMap);
            InsertRequest request = InsertRequest.builder()
                    .table(table)
                    .continueOnError(true)
                    .returnTokens(true)
                    .homogeneous(false)
                    .upsert(upsert)
                    .values(values)
                    .tokens(tokens)
                    .tokenMode(TokenMode.ENABLE)
                    .build();
            Validations.validateInsertRequest(request);

            Assert.assertEquals(table, request.getTable());
            Assert.assertEquals(upsert, request.getUpsert());
            Assert.assertEquals(1, request.getValues().size());
            Assert.assertEquals(1, request.getTokens().size());
            Assert.assertEquals(TokenMode.ENABLE, request.getTokenMode());
            Assert.assertTrue(request.getContinueOnError());
            Assert.assertTrue(request.getReturnTokens());
            Assert.assertFalse(request.getHomogeneous());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidInputInInsertRequestValidationsWithTokenModeDisable() {
        try {
            values.add(valueMap);
            tokens.add(tokenMap);
            InsertRequest request = InsertRequest.builder()
                    .table(table)
                    .continueOnError(null)
                    .returnTokens(null)
                    .homogeneous(false)
                    .upsert(upsert)
                    .values(values)
                    .tokenMode(null)
                    .build();
            Validations.validateInsertRequest(request);

            Assert.assertEquals(table, request.getTable());
            Assert.assertEquals(upsert, request.getUpsert());
            Assert.assertEquals(1, request.getValues().size());
            Assert.assertEquals(TokenMode.DISABLE, request.getTokenMode());
            Assert.assertNull(request.getTokens());
            Assert.assertFalse(request.getReturnTokens());
            Assert.assertFalse(request.getContinueOnError());
            Assert.assertFalse(request.getHomogeneous());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testNoTableInInsertRequestValidations() {
        InsertRequest request = InsertRequest.builder().build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TableKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyTableInInsertRequestValidations() {
        InsertRequest request = InsertRequest.builder().table("").build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyTable.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testNoValuesInInsertRequestValidations() {
        InsertRequest request = InsertRequest.builder().table(table).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.ValuesKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyValuesInInsertRequestValidations() {
        InsertRequest request = InsertRequest.builder().table(table).values(values).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyValues.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyKeyInValuesInInsertRequestValidations() {
        valueMap.put("", "test_value_3");
        values.add(valueMap);
        InsertRequest request = InsertRequest.builder().table(table).values(values).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyKeyInValues.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyValueInValuesInInsertRequestValidations() {
        valueMap.put("test_column_3", "");
        values.add(valueMap);
        InsertRequest request = InsertRequest.builder().table(table).values(values).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyValueInValues.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyUpsertInInsertRequestValidations() {
        values.add(valueMap);
        InsertRequest request = InsertRequest.builder().table(table).values(values).upsert("").build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyUpsert.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testUpsertWithHomogenousInInsertRequestValidations() {
        values.add(valueMap);
        InsertRequest request = InsertRequest.builder()
                .table(table).values(values).upsert(upsert).homogeneous(true)
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.HomogenousNotSupportedWithUpsert.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testTokensWithTokenStrictDisableInInsertRequestValidations() {
        values.add(valueMap);
        InsertRequest request = InsertRequest.builder().table(table).values(values).tokens(tokens).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TokensPassedForTokenModeDisable.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testNoTokensWithTokenStrictEnableInInsertRequestValidations() {
        values.add(valueMap);
        InsertRequest request = InsertRequest.builder()
                .table(table).values(values).tokenMode(TokenMode.ENABLE)
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.NoTokensWithTokenMode.getMessage(), TokenMode.ENABLE.toString()),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testNoTokensWithTokenStrictEnableStrictInInsertRequestValidations() {
        values.add(valueMap);
        InsertRequest request = InsertRequest.builder()
                .table(table).values(values).tokenMode(TokenMode.ENABLE_STRICT)
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.NoTokensWithTokenMode.getMessage(), TokenMode.ENABLE_STRICT.toString()),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyTokensWithTokenStrictEnableInInsertRequestValidations() {
        values.add(valueMap);
        InsertRequest request = InsertRequest.builder()
                .table(table).values(values).tokens(tokens).tokenMode(TokenMode.ENABLE)
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInsufficientTokensWithTokenStrictEnableStrictInInsertRequestValidations1() {
        values.add(valueMap);
        tokens.add(tokenMap);
        InsertRequest request = InsertRequest.builder()
                .table(table).values(values).tokens(tokens).tokenMode(TokenMode.ENABLE_STRICT)
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.InsufficientTokensPassedForTokenModeEnableStrict.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInsufficientTokensWithTokenStrictEnableStrictInInsertRequestValidations2() {
        values.add(valueMap);
        values.add(valueMap);
        tokens.add(tokenMap);
        InsertRequest request = InsertRequest.builder()
                .table(table).values(values).tokens(tokens).tokenMode(TokenMode.ENABLE_STRICT)
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.InsufficientTokensPassedForTokenModeEnableStrict.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testTokenValueMismatchInInsertRequestValidations() {
        values.add(valueMap);
        tokenMap.put("test_column_3", "test_token_3");
        tokens.add(tokenMap);
        InsertRequest request = InsertRequest.builder()
                .table(table).values(values).tokens(tokens).tokenMode(TokenMode.ENABLE_STRICT)
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.MismatchOfFieldsAndTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyKeyInTokensInInsertRequestValidations() {
        tokenMap.put("", "test_token_2");
        values.add(valueMap);
        tokens.add(tokenMap);
        InsertRequest request = InsertRequest.builder()
                .table(table).values(values).tokens(tokens).tokenMode(TokenMode.ENABLE_STRICT)
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyKeyInTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyValueInTokensInInsertRequestValidations() {
        tokenMap.put("test_column_2", "");
        values.add(valueMap);
        tokens.add(tokenMap);
        InsertRequest request = InsertRequest.builder()
                .table(table).values(values).tokens(tokens).tokenMode(TokenMode.ENABLE_STRICT)
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyValueInTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInsertResponse() {
        try {
            ArrayList<HashMap<String, Object>> errorFields = new ArrayList<>();
            HashMap<String, Object> error = new HashMap<>();
            error.put("requestIndex", 0);
            error.put("requestId", requestId);
            error.put("error", "Insert failed");
            errorFields.add(error);
            values.add(valueMap);
            values.add(valueMap);
            InsertResponse response = new InsertResponse(values, errorFields);
            String responseString = "{\"insertedFields\":[" +
                    "{\"test_column_1\":\"test_value_1\"," +
                    "\"test_column_2\":\"test_value_2\"}," +
                    "{\"test_column_1\":\"test_value_1\"," +
                    "\"test_column_2\":\"test_value_2\"}]" +
                    ",\"errors\":[{\"requestIndex\":0,\"requestId\":\"" + requestId + "\",\"error\":\"Insert failed\"}]}";
            Assert.assertEquals(2, response.getInsertedFields().size());
            Assert.assertEquals(1, response.getErrors().size());
            Assert.assertEquals(responseString, response.toString());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

}
