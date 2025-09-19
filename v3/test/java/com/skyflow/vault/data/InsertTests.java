package com.skyflow.vault.data;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
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
import java.util.List;
import java.util.Map;

public class InsertTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static final String requestId = "95be08fc-4d13-4335-8b8d-24e85d53ed1d";
    private static String vaultID = null;
    private static String clusterID = null;
    private static String table = null;
    private static ArrayList<InsertRecord> values = null;
    private static ArrayList<HashMap<String, Object>> tokens = null;
    private static HashMap<String, Object> valueMap = null;
    private static HashMap<String, Object> tokenMap = null;
    private static List<String> upsert = new ArrayList<>();
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
        upsert.add("upsert_column");
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
            InsertRecord record = InsertRecord.builder().data(valueMap).build();
            values.add(record);
            tokens.add(tokenMap);

            InsertRequest request = InsertRequest.builder()
                    .table(table)
                    .upsert(upsert)
                    .records(values)
                    .build();
            Validations.validateInsertRequest(request);

            Assert.assertEquals(table, request.getTable());
            Assert.assertEquals(upsert, request.getUpsert());
            Assert.assertEquals(1, request.getRecords().size());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidInputInInsertRequestValidationsWithTokenModeDisable() {
        try {
            InsertRecord record = InsertRecord.builder().data(valueMap).build();
            values.add(record);
            tokens.add(tokenMap);
            InsertRequest request = InsertRequest.builder()
                    .table(table)
                    .upsert(upsert)
                    .records(values)
                    .build();
            Validations.validateInsertRequest(request);

            Assert.assertEquals(table, request.getTable());
            Assert.assertEquals(upsert, request.getUpsert());
            Assert.assertEquals(1, request.getRecords().size());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testNoTableInInsertRequestValidations() {
        ArrayList<InsertRecord> records = new ArrayList<>();
        InsertRecord record = InsertRecord.builder().data(valueMap).build();
        records.add(record);
        InsertRequest request = InsertRequest.builder().records(records).table("").build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TableNotSpecifiedInRequestAndRecordObject.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyTableInInsertRequestValidations() {
        ArrayList<InsertRecord> records = new ArrayList<>();
        InsertRecord record = InsertRecord.builder().data(valueMap).build();
        records.add(record);
        InsertRequest request = InsertRequest.builder().records(records).table("").build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TableNotSpecifiedInRequestAndRecordObject.getMessage(), Constants.SDK_PREFIX),
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
        InsertRequest request = InsertRequest.builder().table(table).records(values).build();
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
        InsertRecord record = InsertRecord.builder().data(valueMap).build();
        values.add(record);
        InsertRequest request = InsertRequest.builder().table(table).records(values).build();
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
        InsertRecord record = InsertRecord.builder().data(valueMap).build();
        values.add(record);
        InsertRequest request = InsertRequest.builder().table(table).records(values).build();
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
        InsertRecord record = InsertRecord.builder().data(valueMap).build();
        values.add(record);
        InsertRequest request = InsertRequest.builder().table(table).records(values).upsert(new ArrayList<>()).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyUpsertValues.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testInsertResponse() {
        try {
            Map<String, Object> value = new HashMap<>();
            value.put("test_column_1", "test_value_1");
            Success success = new Success(0, "id", null, null, "table");

            List<Success> successList = new ArrayList<>();
            successList.add(success);
            InsertResponse response = new InsertResponse(successList, null);
            String responseString = "{\"success\":[{\"index\":0,\"skyflow_id\":\"id\",\"table\":\"table\"}]}";
            Assert.assertEquals(1, response.getSuccess().size());
            Assert.assertNull(response.getErrors());
            Assert.assertEquals(responseString, response.toString());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testInsertErrorResponse() {
        try {
            HashMap<String, Object> value = new HashMap<>();
            value.put("test_column_1", "test_value_1");
            Success success = new Success(0, "id", null, value, "table");

            List<Success> successList = new ArrayList<>();
            successList.add(success);

            List<ErrorRecord> errorList = new ArrayList<>();
            ErrorRecord error = new ErrorRecord(1, "Bad Request", 400);
            errorList.add(error);

            InsertResponse response1 = new InsertResponse(successList, errorList);
            String responseString = "{\"success\":[{\"index\":0,\"skyflow_id\":\"id\",\"data\":{\"test_column_1\":\"test_value_1\"},\"table\":\"table\"}],\"errors\":[{\"index\":1,\"error\":\"Bad Request\",\"code\":400}]}";
            Assert.assertEquals(1, response1.getSuccess().size());
            Assert.assertEquals(1, response1.getErrors().size());
            Assert.assertEquals(responseString, response1.toString());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
// Java

   @Test
   public void testTableSpecifiedAtBothRequestAndRecordLevel() {
    InsertRecord record = InsertRecord.builder().data(valueMap).table("record_table").build();
    values.add(record);
    InsertRequest request = InsertRequest.builder().table(table).records(values).build();
    try {
        Validations.validateInsertRequest(request);
        Assert.fail(EXCEPTION_NOT_THROWN);
    } catch (SkyflowException e) {
        Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        Assert.assertEquals(
            Utils.parameterizedString(ErrorMessage.TableSpecifiedInRequestAndRecordObject.getMessage(), Constants.SDK_PREFIX),
            e.getMessage()
        );
    }
}

   @Test
   public void testUpsertSpecifiedAtBothRequestAndRecordLevel() {
    InsertRecord record = InsertRecord.builder().data(valueMap).upsert(upsert).build();
    values.add(record);
    InsertRequest request = InsertRequest.builder().table(table).records(values).upsert(upsert).build();
    try {
        Validations.validateInsertRequest(request);
        Assert.fail(EXCEPTION_NOT_THROWN);
    } catch (SkyflowException e) {
        Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        Assert.assertEquals(
            Utils.parameterizedString(ErrorMessage.UpsertTableRequestAtRecordLevel.getMessage(), Constants.SDK_PREFIX),
            e.getMessage()
        );
    }
}

   @Test
   public void testUpsertAtRecordLevelWithTableAtRequestLevel() {
    InsertRecord record = InsertRecord.builder().data(valueMap).upsert(upsert).build();
    values.add(record);
    InsertRequest request = InsertRequest.builder().table(table).records(values).build();
    try {
        Validations.validateInsertRequest(request);
        Assert.fail(EXCEPTION_NOT_THROWN);
    } catch (SkyflowException e) {
        Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        Assert.assertEquals(
            Utils.parameterizedString(ErrorMessage.UpsertTableRequestAtRecordLevel.getMessage(), Constants.SDK_PREFIX),
            e.getMessage()
        );
    }
}

   @Test
   public void testUpsertAtRequestLevelWithNoTable() {
    InsertRecord record = InsertRecord.builder().table("table").data(valueMap).build();
    values.add(record);
    InsertRequest request = InsertRequest.builder().records(values).upsert(upsert).build();
    try {
        Validations.validateInsertRequest(request);
        Assert.fail(EXCEPTION_NOT_THROWN);
    } catch (SkyflowException e) {
        Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        Assert.assertEquals(
            Utils.parameterizedString(ErrorMessage.UpsertTableRequestAtRequestLevel.getMessage(), Constants.SDK_PREFIX),
            e.getMessage()
        );
    }
}

   @Test
   public void testUpsertAtRequestLevelWithEmptyTable() {
        InsertRecord record = InsertRecord.builder().table("table").data(valueMap).build();
        values.add(record);
        List<String> upsert = new ArrayList<>();
        upsert.add("upsert_column");

        InsertRequest request = InsertRequest.builder().table("").records(values).upsert(upsert).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.UpsertTableRequestAtRequestLevel.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }


}
