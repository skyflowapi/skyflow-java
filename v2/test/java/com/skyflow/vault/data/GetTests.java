package com.skyflow.vault.data;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.RedactionType;
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

public class GetTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String vaultID = null;
    private static String clusterID = null;
    private static String skyflowID = null;
    private static String field = null;
    private static String table = null;
    private static ArrayList<String> ids = null;
    private static ArrayList<String> fields = null;
    private static String columnName = null;
    private static String columnValue = null;
    private static ArrayList<String> columnValues = null;
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

        skyflowID = "test_get_id_1";
        ids = new ArrayList<>();
        field = "test_get_field";
        fields = new ArrayList<>();
        columnName = "test_column_name";
        columnValue = "test_column_value";
        columnValues = new ArrayList<>();
        table = "test_table";
        SdkVersion.setSdkPrefix(Constants.SDK_PREFIX);
    }

    @Before
    public void setupTest() {
        ids.clear();
        fields.clear();
        columnValues.clear();
    }

    @Test
    public void testValidGetByIdInputInGetRequestValidations() {
        try {
            ids.add(skyflowID);
            fields.add(field);
            GetRequest request = GetRequest.builder()
                    .ids(ids)
                    .table(table)
                    .returnTokens(true)
                    .redactionType(null)
                    .downloadURL(false)
                    .offset("2")
                    .limit("1")
                    .fields(fields)
                    .orderBy(Constants.ORDER_ASCENDING)
                    .build();
            Validations.validateGetRequest(request);
            Assert.assertEquals(table, request.getTable());
            Assert.assertEquals(1, request.getIds().size());
            Assert.assertEquals(1, request.getFields().size());
            Assert.assertEquals("2", request.getOffset());
            Assert.assertEquals("1", request.getLimit());
            Assert.assertEquals(Constants.ORDER_ASCENDING, request.getOrderBy());
            Assert.assertTrue(request.getReturnTokens());
            Assert.assertFalse(request.getDownloadURL());
            Assert.assertNull(request.getRedactionType());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidGetByColumnValuesInputInGetRequestValidations() {
        try {
            columnValues.add(columnValue);
            fields.add(field);
            GetRequest request = GetRequest.builder()
                    .table(table)
                    .columnName(columnName)
                    .columnValues(columnValues)
                    .redactionType(RedactionType.PLAIN_TEXT)
                    .downloadURL(null)
                    .offset("2")
                    .limit("1")
                    .fields(fields)
                    .orderBy(null)
                    .build();
            Validations.validateGetRequest(request);
            Assert.assertEquals(table, request.getTable());
            Assert.assertEquals(columnName, request.getColumnName());
            Assert.assertEquals(1, request.getColumnValues().size());
            Assert.assertEquals(1, request.getFields().size());
            Assert.assertEquals(RedactionType.PLAIN_TEXT, request.getRedactionType());
            Assert.assertEquals("2", request.getOffset());
            Assert.assertEquals("1", request.getLimit());
            Assert.assertEquals(Constants.ORDER_ASCENDING, request.getOrderBy());
            Assert.assertTrue(request.getDownloadURL());
            Assert.assertNull(request.getReturnTokens());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testNoTableInGetRequestValidations() {
        ids.add(skyflowID);
        GetRequest request = GetRequest.builder().ids(ids).build();
        try {
            Validations.validateGetRequest(request);
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
    public void testEmptyTableInGetRequestValidations() {
        ids.add(skyflowID);
        GetRequest request = GetRequest.builder().ids(ids).table("").build();
        try {
            Validations.validateGetRequest(request);
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
    public void testEmptyIdsInGetRequestValidations() {
        GetRequest request = GetRequest.builder().ids(ids).table(table).build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyIds.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testNullIdInIdsInGetRequestValidations() {
        ids.add(skyflowID);
        ids.add(null);
        GetRequest request = GetRequest.builder().ids(ids).table(table).build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyIdInIds.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyIdInIdsInGetRequestValidations() {
        ids.add(skyflowID);
        ids.add("");
        GetRequest request = GetRequest.builder().ids(ids).table(table).build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyIdInIds.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyFieldsInGetRequestValidations() {
        ids.add(skyflowID);
        GetRequest request = GetRequest.builder().ids(ids).table(table).fields(fields).build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyFields.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testNullFieldInFieldsInGetRequestValidations() {
        ids.add(skyflowID);
        fields.add(field);
        fields.add(null);
        GetRequest request = GetRequest.builder().ids(ids).table(table).fields(fields).build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyFieldInFields.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyFieldInFieldsInGetRequestValidations() {
        ids.add(skyflowID);
        fields.add(field);
        fields.add("");
        GetRequest request = GetRequest.builder().ids(ids).table(table).fields(fields).build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyFieldInFields.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testNoRedactionInGetRequestValidations() {
        GetRequest request = GetRequest.builder().table(table).returnTokens(false).redactionType(null).build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.RedactionKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testReturnTokensWithRedactionInGetRequestValidations() {
        GetRequest request = GetRequest.builder()
                .table(table).redactionType(RedactionType.PLAIN_TEXT).returnTokens(true)
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.RedactionWithTokensNotSupported.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testReturnTokensWithColumnNameInGetRequestValidations() {
        GetRequest request = GetRequest.builder()
                .table(table).returnTokens(true).redactionType(null).columnName(columnName)
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TokensGetColumnNotSupported.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testReturnTokensWithColumnValuesInGetRequestValidations() {
        GetRequest request = GetRequest.builder()
                .table(table).returnTokens(true).redactionType(null).columnValues(columnValues)
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TokensGetColumnNotSupported.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyOffsetInGetRequestValidations() {
        GetRequest request = GetRequest.builder()
                .table(table).redactionType(RedactionType.PLAIN_TEXT).offset("")
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyOffset.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyLimitInGetRequestValidations() {
        GetRequest request = GetRequest.builder()
                .table(table).redactionType(RedactionType.PLAIN_TEXT).limit("")
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyLimit.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testNoIdsOrColumnNameInGetRequestValidations() {
        GetRequest request = GetRequest.builder().table(table).redactionType(RedactionType.PLAIN_TEXT).build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.UniqueColumnOrIdsKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testBothIdsAndColumnNameInGetRequestValidations() {
        ids.add(skyflowID);
        GetRequest request = GetRequest.builder()
                .table(table).redactionType(RedactionType.PLAIN_TEXT).ids(ids).columnName(columnName)
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.BothIdsAndColumnDetailsSpecified.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testBothIdsAndColumnValuesInGetRequestValidations() {
        ids.add(skyflowID);
        GetRequest request = GetRequest.builder()
                .table(table).redactionType(RedactionType.PLAIN_TEXT).ids(ids).columnValues(columnValues)
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.BothIdsAndColumnDetailsSpecified.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testColumnNameWithoutColumnValuesInGetRequestValidations() {
        GetRequest request = GetRequest.builder()
                .table(table).redactionType(RedactionType.PLAIN_TEXT).columnName(columnName)
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.ColumnValuesKeyErrorGet.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testColumnValuesWithoutColumnNameInGetRequestValidations() {
        GetRequest request = GetRequest.builder()
                .table(table).redactionType(RedactionType.PLAIN_TEXT).columnValues(columnValues)
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.ColumnNameKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyColumnNameInGetRequestValidations() {
        GetRequest request = GetRequest.builder()
                .table(table).redactionType(RedactionType.PLAIN_TEXT).columnName("").columnValues(columnValues)
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyColumnName.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyColumnValuesInGetRequestValidations() {
        GetRequest request = GetRequest.builder()
                .table(table).redactionType(RedactionType.PLAIN_TEXT).columnName(columnName).columnValues(columnValues)
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyColumnValues.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyValueInColumnValuesInGetRequestValidations() {
        columnValues.add(columnValue);
        columnValues.add("");
        GetRequest request = GetRequest.builder()
                .table(table).redactionType(RedactionType.PLAIN_TEXT).columnName(columnName).columnValues(columnValues)
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyValueInColumnValues.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testGetResponse() {
        try {
            ArrayList<HashMap<String, Object>> data = new ArrayList<>();
            HashMap<String, Object> record = new HashMap<>();
            record.put("test_column_1", "test_value_1");
            record.put("test_column_2", "test_value_2");
            data.add(record);
            data.add(record);
            ArrayList<HashMap<String, Object>> errors = new ArrayList<>();
            GetResponse response = new GetResponse(data, errors);
            String responseString = "{\"data\":[" +
                    "{\"test_column_1\":\"test_value_1\"," +
                    "\"test_column_2\":\"test_value_2\"}," +
                    "{\"test_column_1\":\"test_value_1\"," +
                    "\"test_column_2\":\"test_value_2\"}]" +
                    ",\"errors\":" + errors + "}";
            Assert.assertEquals(2, response.getData().size());
            Assert.assertTrue(response.getErrors().isEmpty());
            Assert.assertEquals(responseString, response.toString());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }

    }
}
