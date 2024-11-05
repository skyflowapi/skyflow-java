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

import java.util.ArrayList;
import java.util.HashMap;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.skyflow.serviceaccount.util.Token")
public class InsertTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
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

        table = "test_table";
        values = new ArrayList<>();
        tokens = new ArrayList<>();
        valueMap = new HashMap<>();
        valueMap.put("test_column_1", "test_value_1");
        valueMap.put("test_column_2", "test_value_2");
        tokenMap = new HashMap<>();
        tokenMap.put("test_column_1", "test_token_1");
        upsert = "upsert_column";
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
                    .tokenMode(true)
                    .tokenStrict(Byot.ENABLE)
                    .build();
            Validations.validateInsertRequest(request);

            Assert.assertEquals(table, request.getTable());
            Assert.assertEquals(upsert, request.getUpsert());
            Assert.assertEquals(1, request.getValues().size());
            Assert.assertEquals(1, request.getTokens().size());
            Assert.assertEquals(Byot.ENABLE, request.getTokenStrict());
            Assert.assertTrue(request.getContinueOnError());
            Assert.assertTrue(request.getReturnTokens());
            Assert.assertTrue(request.getTokenMode());
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
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
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
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
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
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.ValuesKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyValuesInInsertRequestValidations() {
        values.clear();
        InsertRequest request = InsertRequest.builder().table(table).values(values).build();
        try {
            Validations.validateInsertRequest(request);
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
    public void testEmptyKeyInValuesInInsertRequestValidations() {
        valueMap.put("", "test_value_3");
        values.add(valueMap);
        InsertRequest request = InsertRequest.builder().table(table).values(values).build();
        try {
            Validations.validateInsertRequest(request);
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
    public void testEmptyValueInValuesInInsertRequestValidations() {
        valueMap.remove("");
        valueMap.put("test_column_3", "");
        InsertRequest request = InsertRequest.builder().table(table).values(values).build();
        try {
            Validations.validateInsertRequest(request);
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
    public void testEmptyUpsertInInsertRequestValidations() {
        valueMap.remove("test_column_3");
        InsertRequest request = InsertRequest.builder().table(table).values(values).upsert("").build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyUpsert.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testUpsertWithHomogenousInInsertRequestValidations() {
        InsertRequest request = InsertRequest.builder()
                .table(table).values(values).upsert(upsert).homogeneous(true)
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.HomogenousNotSupportedWithUpsert.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testTokensWithTokenStrictDisableInInsertRequestValidations() {
        InsertRequest request = InsertRequest.builder().table(table).values(values).tokens(tokens).build();
        try {
            Validations.validateInsertRequest(request);
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
    public void testNoTokensWithTokenStrictEnableInInsertRequestValidations() {
        InsertRequest request = InsertRequest.builder()
                .table(table).values(values).tokenStrict(Byot.ENABLE)
                .build();
        try {
            Validations.validateInsertRequest(request);
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
    public void testNoTokensWithTokenStrictEnableStrictInInsertRequestValidations() {
        InsertRequest request = InsertRequest.builder()
                .table(table).values(values).tokenStrict(Byot.ENABLE_STRICT)
                .build();
        try {
            Validations.validateInsertRequest(request);
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
    public void testEmptyTokensWithTokenStrictEnableInInsertRequestValidations() {
        tokens.clear();
        InsertRequest request = InsertRequest.builder()
                .table(table).values(values).tokens(tokens).tokenStrict(Byot.ENABLE)
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.EmptyTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInsufficientTokensWithTokenStrictEnableStrictInInsertRequestValidations1() {
        tokens.add(tokenMap);
        InsertRequest request = InsertRequest.builder()
                .table(table).values(values).tokens(tokens).tokenStrict(Byot.ENABLE_STRICT)
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.InsufficientTokensPassedForByotEnableStrict.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInsufficientTokensWithTokenStrictEnableStrictInInsertRequestValidations2() {
        values.add(valueMap);
        InsertRequest request = InsertRequest.builder()
                .table(table).values(values).tokens(tokens).tokenStrict(Byot.ENABLE_STRICT)
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.InsufficientTokensPassedForByotEnableStrict.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testTokenValueMismatchInInsertRequestValidations() {
        tokenMap.put("test_column_3", "test_token_3");
        values.remove(1);
        InsertRequest request = InsertRequest.builder()
                .table(table).values(values).tokens(tokens).tokenStrict(Byot.ENABLE_STRICT)
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.MismatchOfFieldsAndTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyKeyInTokensInInsertRequestValidations() {
        tokenMap.remove("test_column_2");
        tokenMap.remove("test_column_3");
        tokenMap.put("", "test_token_2");
        InsertRequest request = InsertRequest.builder()
                .table(table).values(values).tokens(tokens).tokenStrict(Byot.ENABLE_STRICT)
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.EmptyKeyInTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyValueInTokensInInsertRequestValidations() {
        tokenMap.remove("");
        tokenMap.put("test_column_2", "");
        InsertRequest request = InsertRequest.builder()
                .table(table).values(values).tokens(tokens).tokenStrict(Byot.ENABLE_STRICT)
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.EmptyValueInTokens.getMessage(), e.getMessage());
        }
    }

}
