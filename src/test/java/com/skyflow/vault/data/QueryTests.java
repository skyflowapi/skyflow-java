package com.skyflow.vault.data;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
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
public class QueryTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String vaultID = null;
    private static String clusterID = null;
    private static String query = null;
    private static Skyflow skyflowClient = null;
    private static HashMap<String, Object> queryRecord = null;

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

        query = "test_query";
        queryRecord = new HashMap<>();
        queryRecord.put("name", "test_name");
        queryRecord.put("card_number", "test_card_number");

//        skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addVaultConfig(vaultConfig).build();
    }

    @Test
    public void testValidInputInQueryRequestValidations() {
        try {
            QueryRequest request = QueryRequest.builder().query(query).build();
            Validations.validateQueryRequest(request);
            Assert.assertEquals(query, request.getQuery());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testNoQueryInQueryRequestValidations() {
        QueryRequest request = QueryRequest.builder().build();
        try {
            Validations.validateQueryRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.QueryKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyQueryInQueryRequestValidations() {
        QueryRequest request = QueryRequest.builder().query("").build();
        try {
            Validations.validateQueryRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyQuery.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testQueryResponse() {
        try {
            ArrayList<HashMap<String, Object>> fields = new ArrayList<>();
            fields.add(queryRecord);
            fields.add(queryRecord);
            QueryResponse response = new QueryResponse(fields);
            String responseString = "{\n\t\"fields\": " +
                    "[{\n\t\t\"card_number\": \"test_card_number\",\n\t\t\"name\": \"test_name\"," +
                    "\n\t\t\"tokenizedData\": " + null + "\n\t}, " +
                    "{\n\t\t\"card_number\": \"test_card_number\",\n\t\t\"name\": \"test_name\"," +
                    "\n\t\t\"tokenizedData\": " + null + "\n\t}]\n}";
            Assert.assertEquals(2, response.getFields().size());
            Assert.assertEquals(responseString, response.toString());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
