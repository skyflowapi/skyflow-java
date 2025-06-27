package com.skyflow.vault.data;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
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

import java.util.ArrayList;

public class DeleteTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String vaultID = null;
    private static String clusterID = null;
    private static String skyflowID = null;
    private static String table = null;
    private static ArrayList<String> ids = null;
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
        skyflowID = "test_delete_id_1";
        ids = new ArrayList<>();
        table = "test_table";
    }

    @Before
    public void setupTest() {
        ids.clear();
    }

    @Test
    public void testValidInputInDeleteRequestValidations() {
        try {
            ids.add(skyflowID);
            DeleteRequest request = DeleteRequest.builder().ids(ids).table(table).build();
            Validations.validateDeleteRequest(request);
            Assert.assertEquals(1, ids.size());
            Assert.assertEquals(table, request.getTable());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testNoIdsInDeleteRequestValidations() {
        DeleteRequest request = DeleteRequest.builder().table(table).build();
        try {
            Validations.validateDeleteRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.IdsKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyIdsInDeleteRequestValidations() {
        DeleteRequest request = DeleteRequest.builder().ids(ids).table(table).build();
        try {
            Validations.validateDeleteRequest(request);
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
    public void testNullIdInIdsInDeleteRequestValidations() {
        ids.add(skyflowID);
        ids.add(null);
        DeleteRequest request = DeleteRequest.builder().ids(ids).table(table).build();
        try {
            Validations.validateDeleteRequest(request);
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
    public void testEmptyIdInIdsInDeleteRequestValidations() {
        ids.add(skyflowID);
        ids.add("");
        DeleteRequest request = DeleteRequest.builder().ids(ids).table(table).build();
        try {
            Validations.validateDeleteRequest(request);
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
    public void testNoTableInDeleteRequestValidations() {
        ids.add(skyflowID);
        DeleteRequest request = DeleteRequest.builder().ids(ids).build();
        try {
            Validations.validateDeleteRequest(request);
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
    public void testEmptyTableInDeleteRequestValidations() {
        ids.add(skyflowID);
        DeleteRequest request = DeleteRequest.builder().ids(ids).table("").build();
        try {
            Validations.validateDeleteRequest(request);
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
    public void testDeleteResponse() {
        try {
            ids.add(skyflowID);
            DeleteResponse response = new DeleteResponse(ids);
            String responseString = "{\"deletedIds\":[\"" + skyflowID + "\"],\"errors\":null}";
            Assert.assertEquals(1, response.getDeletedIds().size());
            Assert.assertEquals(responseString, response.toString());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
