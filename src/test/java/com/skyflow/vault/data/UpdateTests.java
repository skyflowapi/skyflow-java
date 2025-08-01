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
import com.skyflow.utils.Utils;
import com.skyflow.utils.validations.Validations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;

public class UpdateTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String vaultID = null;
    private static String clusterID = null;
    private static String skyflowID = null;
    private static String table = null;
    private static HashMap<String, Object> dataMap = null;
    private static HashMap<String, Object> tokenMap = null;
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

        skyflowID = "test_update_id_1";
        table = "test_table";
        dataMap = new HashMap<>();
        tokenMap = new HashMap<>();
    }

    @Before
    public void setupTest() {
        dataMap.clear();
        tokenMap.clear();
    }

    @Test
    public void testValidInputInUpdateRequestValidations() {
        try {
            dataMap.put("skyflow_id", skyflowID);
            dataMap.put("test_column_1", "test_value_1");
            dataMap.put("test_column_2", "test_value_2");
            tokenMap.put("test_column_1", "test_token_1");
            UpdateRequest request = UpdateRequest.builder()
                    .table(table)
                    .data(dataMap)
                    .tokens(tokenMap)
                    .returnTokens(true)
                    .tokenMode(TokenMode.ENABLE)
                    .build();
            Validations.validateUpdateRequest(request);
            Assert.assertEquals(table, request.getTable());
            Assert.assertEquals(3, request.getData().size());
            Assert.assertEquals(1, request.getTokens().size());
            Assert.assertTrue(request.getReturnTokens());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidInputInUpdateRequestValidationsWithTokenModeDisable() {
        try {
            dataMap.put("skyflow_id", skyflowID);
            dataMap.put("test_column_1", "test_value_1");
            dataMap.put("test_column_2", "test_value_2");
            UpdateRequest request = UpdateRequest.builder()
                    .table(table)
                    .data(dataMap)
                    .returnTokens(null)
                    .tokenMode(null)
                    .build();
            Validations.validateUpdateRequest(request);
            Assert.assertEquals(table, request.getTable());
            Assert.assertEquals(3, request.getData().size());
            Assert.assertFalse(request.getReturnTokens());
            Assert.assertNull(request.getTokens());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testNoSkyflowIdInUpdateRequestValidations() {
        dataMap.put("test_column_1", "test_value_1");
        UpdateRequest request = UpdateRequest.builder().table(table).data(dataMap).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.SkyflowIdKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testInvalidSkyflowIdTypeInUpdateRequestValidations() {
        dataMap.put("skyflow_id", 123);
        UpdateRequest request = UpdateRequest.builder().table(table).data(dataMap).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.InvalidSkyflowIdType.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptySkyflowIdInUpdateRequestValidations() {
        dataMap.put("skyflow_id", "");
        UpdateRequest request = UpdateRequest.builder().table(table).data(dataMap).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptySkyflowId.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testNoTableInUpdateRequestValidations() {
        UpdateRequest request = UpdateRequest.builder().build();
        try {
            Validations.validateUpdateRequest(request);
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
    public void testEmptyTableInUpdateRequestValidations() {
        UpdateRequest request = UpdateRequest.builder().table("").build();
        try {
            Validations.validateUpdateRequest(request);
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
    public void testNoValuesInUpdateRequestValidations() {
        UpdateRequest request = UpdateRequest.builder().table(table).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.DataKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyValuesInUpdateRequestValidations() {
        UpdateRequest request = UpdateRequest.builder().table(table).data(dataMap).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyData.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testNullKeyInValuesInUpdateRequestValidations() {
        dataMap.put("skyflow_id", skyflowID);
        dataMap.put("test_column_1", "test_value_1");
        dataMap.put("test_column_2", "test_value_2");
        dataMap.put(null, "test_value_3");
        UpdateRequest request = UpdateRequest.builder().table(table).data(dataMap).build();
        try {
            Validations.validateUpdateRequest(request);
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
    public void testEmptyKeyInValuesInUpdateRequestValidations() {
        dataMap.put("skyflow_id", skyflowID);
        dataMap.put("test_column_1", "test_value_1");
        dataMap.put("test_column_2", "test_value_2");
        dataMap.put("", "test_value_3");
        UpdateRequest request = UpdateRequest.builder().table(table).data(dataMap).build();
        try {
            Validations.validateUpdateRequest(request);
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
    public void testNullValueInValuesInUpdateRequestValidations() {
        dataMap.put("skyflow_id", skyflowID);
        dataMap.put("test_column_1", "test_value_1");
        dataMap.put("test_column_2", "test_value_2");
        dataMap.put("test_column_3", null);
        UpdateRequest request = UpdateRequest.builder().table(table).data(dataMap).build();
        try {
            Validations.validateUpdateRequest(request);
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
    public void testEmptyValueInValuesInUpdateRequestValidations() {
        dataMap.put("skyflow_id", skyflowID);
        dataMap.put("test_column_1", "test_value_1");
        dataMap.put("test_column_2", "test_value_2");
        dataMap.put("test_column_3", "");
        UpdateRequest request = UpdateRequest.builder().table(table).data(dataMap).build();
        try {
            Validations.validateUpdateRequest(request);
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
    public void testTokensWithTokenModeDisableInUpdateRequestValidations() {
        dataMap.put("skyflow_id", skyflowID);
        dataMap.put("test_column_1", "test_value_1");
        dataMap.put("test_column_2", "test_value_2");
        tokenMap.put("test_column_1", "test_token_1");
        UpdateRequest request = UpdateRequest.builder()
                .table(table).data(dataMap).tokens(tokenMap).tokenMode(TokenMode.DISABLE)
                .build();
        try {
            Validations.validateUpdateRequest(request);
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
    public void testNoTokensWithTokenModeEnableInUpdateRequestValidations() {
        dataMap.put("skyflow_id", skyflowID);
        dataMap.put("test_column_1", "test_value_1");
        dataMap.put("test_column_2", "test_value_2");
        UpdateRequest request = UpdateRequest.builder()
                .table(table).data(dataMap).tokenMode(TokenMode.ENABLE)
                .build();
        try {
            Validations.validateUpdateRequest(request);
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
    public void testNoTokensWithTokenModeEnableStrictInUpdateRequestValidations() {
        dataMap.put("skyflow_id", skyflowID);
        dataMap.put("test_column_1", "test_value_1");
        dataMap.put("test_column_2", "test_value_2");
        UpdateRequest request = UpdateRequest.builder()
                .table(table).data(dataMap).tokenMode(TokenMode.ENABLE_STRICT)
                .build();
        try {
            Validations.validateUpdateRequest(request);
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
    public void testEmptyTokensWithTokenModeEnableInUpdateRequestValidations() {
        dataMap.put("skyflow_id", skyflowID);
        dataMap.put("test_column_1", "test_value_1");
        dataMap.put("test_column_2", "test_value_2");
        UpdateRequest request = UpdateRequest.builder()
                .table(table).data(dataMap).tokens(tokenMap).tokenMode(TokenMode.ENABLE)
                .build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInsufficientTokensWithTokenModeEnableStrictInUpdateRequestValidations() {
        dataMap.put("skyflow_id", skyflowID);
        dataMap.put("test_column_1", "test_value_1");
        dataMap.put("test_column_2", "test_value_2");
        tokenMap.put("test_column_1", "test_token_1");
        UpdateRequest request = UpdateRequest.builder()
                .table(table).data(dataMap).tokens(tokenMap).tokenMode(TokenMode.ENABLE_STRICT)
                .build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.InsufficientTokensPassedForTokenModeEnableStrict.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testTokenValueMismatchInUpdateRequestValidations() {
        dataMap.put("skyflow_id", skyflowID);
        dataMap.put("test_column_1", "test_value_1");
        dataMap.put("test_column_2", "test_value_2");
        tokenMap.put("test_column_1", "test_token_1");
        tokenMap.put("test_column_3", "test_token_3");
        UpdateRequest request = UpdateRequest.builder()
                .table(table).data(dataMap).tokens(tokenMap).tokenMode(TokenMode.ENABLE_STRICT)
                .build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.MismatchOfFieldsAndTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNullKeyInTokensInUpdateRequestValidations() {
        dataMap.put("skyflow_id", skyflowID);
        dataMap.put("test_column_1", "test_value_1");
        dataMap.put("test_column_2", "test_value_2");
        tokenMap.put("test_column_1", "test_token_1");
        tokenMap.put(null, "test_token_2");
        UpdateRequest request = UpdateRequest.builder()
                .table(table).data(dataMap).tokens(tokenMap).tokenMode(TokenMode.ENABLE_STRICT)
                .build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyKeyInTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNullValueInTokensInUpdateRequestValidations() {
        dataMap.put("skyflow_id", skyflowID);
        dataMap.put("test_column_1", "test_value_1");
        dataMap.put("test_column_2", "test_value_2");
        tokenMap.put("test_column_1", "test_token_1");
        tokenMap.put("test_column_2", null);
        UpdateRequest request = UpdateRequest.builder()
                .table(table).data(dataMap).tokens(tokenMap).tokenMode(TokenMode.ENABLE_STRICT)
                .build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyValueInTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testUpdateResponse() {
        try {
            dataMap.put("test_column_1", "test_value_1");
            dataMap.put("test_column_2", "test_value_2");
            tokenMap.put("test_column_1", "test_token_1");
            tokenMap.put("test_column_2", "test_token_2");
            UpdateResponse response = new UpdateResponse(skyflowID, tokenMap);
            String responseString = "{\"updatedField\":{\"skyflowId\":\"" + skyflowID + "\","
                    + "\"test_column_1\":\"test_token_1\",\"test_column_2\":\"test_token_2\"}" +
                    ",\"errors\":null}";
            Assert.assertEquals(skyflowID, response.getSkyflowId());
            Assert.assertEquals(2, response.getTokens().size());
            Assert.assertEquals(responseString, response.toString());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
