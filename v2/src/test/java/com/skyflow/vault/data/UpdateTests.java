package com.skyflow.vault.data;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Byot;
import com.skyflow.enums.Env;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.validations.Validations;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.skyflow.serviceaccount.util.Token")
public class UpdateTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String vaultID = null;
    private static String clusterID = null;
    private static String skyflowID = null;
    private static String table = null;
    private static HashMap<String, Object> valueMap = null;
    private static HashMap<String, Object> tokenMap = null;
    private static Skyflow skyflowClient = null;

    @BeforeClass
    public static void setup() throws SkyflowException {
        PowerMockito.mockStatic(Token.class);
        PowerMockito.when(Token.isExpired("valid_token")).thenReturn(true);
        PowerMockito.when(Token.isExpired("not_a_valid_token")).thenReturn(false);

        vaultID = "vault123";
        clusterID = "cluster123";

        Credentials credentials = new Credentials();
        credentials.setToken("valid-token");

        VaultConfig vaultConfig = new VaultConfig();
        vaultConfig.setVaultId(vaultID);
        vaultConfig.setClusterId(clusterID);
        vaultConfig.setEnv(Env.DEV);
        vaultConfig.setCredentials(credentials);

//        skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();

        skyflowID = "test_update_id_1";
        table = "test_table";
        valueMap = new HashMap<>();
        valueMap.put("test_column_1", "test_value_1");
        valueMap.put("test_column_2", "test_value_2");
        tokenMap = new HashMap<>();
        tokenMap.put("test_column_1", "test_token_1");
    }

    @Test
    public void testValidInputInUpdateRequestValidations() {
        try {
            UpdateRequest request = UpdateRequest.builder()
                    .id(skyflowID)
                    .table(table)
                    .values(valueMap)
                    .tokens(tokenMap)
                    .returnTokens(true)
                    .tokenStrict(Byot.ENABLE)
                    .build();
            Validations.validateUpdateRequest(request);
            Assert.assertEquals(table, request.getTable());
            Assert.assertEquals(skyflowID, request.getId());
            Assert.assertEquals(2, request.getValues().size());
            Assert.assertEquals(1, request.getTokens().size());
            Assert.assertTrue(request.getReturnTokens());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testNoSkyflowIdInUpdateRequestValidations() {
        UpdateRequest request = UpdateRequest.builder().table(table).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.SkyflowIdKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptySkyflowIdInUpdateRequestValidations() {
        UpdateRequest request = UpdateRequest.builder().id("").table(table).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptySkyflowId.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testNoTableInUpdateRequestValidations() {
        UpdateRequest request = UpdateRequest.builder().id(skyflowID).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TableKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyTableInUpdateRequestValidations() {
        UpdateRequest request = UpdateRequest.builder().id(skyflowID).table("").build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyTable.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testNoValuesInUpdateRequestValidations() {
        UpdateRequest request = UpdateRequest.builder().id(skyflowID).table(table).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.ValuesKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyValuesInUpdateRequestValidations() {
        HashMap<String, Object> emptyValueMap = new HashMap<>();
        UpdateRequest request = UpdateRequest.builder().id(skyflowID).table(table).values(emptyValueMap).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyValues.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyKeyInValuesInUpdateRequestValidations() {
        valueMap.put("", "test_value_3");
        UpdateRequest request = UpdateRequest.builder().id(skyflowID).table(table).values(valueMap).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyKeyInValues.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyValueInValuesInUpdateRequestValidations() {
        valueMap.remove("");
        valueMap.put("test_column_3", "");
        UpdateRequest request = UpdateRequest.builder().id(skyflowID).table(table).values(valueMap).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyValueInValues.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testTokensWithTokenStrictDisableInUpdateRequestValidations() {
        valueMap.remove("test_column_3");
        UpdateRequest request = UpdateRequest.builder()
                .id(skyflowID).table(table).values(valueMap).tokens(tokenMap).tokenStrict(Byot.DISABLE)
                .build();
        try {
            System.out.println(valueMap);
            System.out.println(tokenMap);
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TokensPassedForByotDisable.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testNoTokensWithTokenStrictEnableInUpdateRequestValidations() {
        UpdateRequest request = UpdateRequest.builder()
                .id(skyflowID).table(table).values(valueMap).tokenStrict(Byot.ENABLE)
                .build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.NoTokensWithByot.getMessage(), Byot.ENABLE.toString()),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testNoTokensWithTokenStrictEnableStrictInUpdateRequestValidations() {
        UpdateRequest request = UpdateRequest.builder()
                .id(skyflowID).table(table).values(valueMap).tokenStrict(Byot.ENABLE_STRICT)
                .build();
        try {
            System.out.println(valueMap);
            System.out.println(tokenMap);
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.NoTokensWithByot.getMessage(), Byot.ENABLE_STRICT.toString()),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyTokensWithTokenStrictEnableInUpdateRequestValidations() {
        HashMap<String, Object> emptyTokenMap = new HashMap<>();
        UpdateRequest request = UpdateRequest.builder()
                .id(skyflowID).table(table).values(valueMap).tokens(emptyTokenMap).tokenStrict(Byot.ENABLE)
                .build();
        try {
            System.out.println(valueMap);
            System.out.println(tokenMap);
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.EmptyTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInsufficientTokensWithTokenStrictEnableStrictInUpdateRequestValidations1() {
        UpdateRequest request = UpdateRequest.builder()
                .id(skyflowID).table(table).values(valueMap).tokens(tokenMap).tokenStrict(Byot.ENABLE_STRICT)
                .build();
        try {
            System.out.println(valueMap);
            System.out.println(tokenMap);
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.InsufficientTokensPassedForByotEnableStrict.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInsufficientTokensWithTokenStrictEnableStrictInUpdateRequestValidations2() {
        UpdateRequest request = UpdateRequest.builder()
                .id(skyflowID).table(table).values(valueMap).tokens(tokenMap).tokenStrict(Byot.ENABLE_STRICT)
                .build();
        try {
            System.out.println(valueMap);
            System.out.println(tokenMap);
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.InsufficientTokensPassedForByotEnableStrict.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testTokenValueMismatchInUpdateRequestValidations() {
        tokenMap.put("test_column_3", "test_token_3");
        UpdateRequest request = UpdateRequest.builder()
                .id(skyflowID).table(table).values(valueMap).tokens(tokenMap).tokenStrict(Byot.ENABLE_STRICT)
                .build();
        try {
            System.out.println(valueMap);
            System.out.println(tokenMap);
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.MismatchOfFieldsAndTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyKeyInTokensInUpdateRequestValidations() {
        tokenMap.remove("test_column_2");
        tokenMap.remove("test_column_3");
        tokenMap.put("", "test_token_2");
        UpdateRequest request = UpdateRequest.builder()
                .id(skyflowID).table(table).values(valueMap).tokens(tokenMap).tokenStrict(Byot.ENABLE_STRICT)
                .build();
        try {
            System.out.println(valueMap);
            System.out.println(tokenMap);
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.EmptyKeyInTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyValueInTokensInUpdateRequestValidations() {
        tokenMap.remove("");
        tokenMap.put("test_column_2", "");
        UpdateRequest request = UpdateRequest.builder()
                .id(skyflowID).table(table).values(valueMap).tokens(tokenMap).tokenStrict(Byot.ENABLE_STRICT)
                .build();
        try {
            System.out.println(valueMap);
            System.out.println(tokenMap);
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.EmptyValueInTokens.getMessage(), e.getMessage());
        }
    }

}
