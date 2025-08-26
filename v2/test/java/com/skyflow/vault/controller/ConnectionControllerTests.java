package com.skyflow.vault.controller;

import com.skyflow.Skyflow;
import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Constants;
import com.skyflow.utils.SdkVersion;
import com.skyflow.vault.connection.InvokeConnectionRequest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;

public class ConnectionControllerTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String connectionID = null;
    private static String connectionURL = null;
    private static ConnectionConfig connectionConfig = null;
    private static Skyflow skyflowClient = null;

    @BeforeClass
    public static void setup() {
        connectionID = "vault123";
        connectionURL = "https://test.connection.url";

        Credentials credentials = new Credentials();
        credentials.setToken("valid-token");

        connectionConfig = new ConnectionConfig();
        connectionConfig.setConnectionId(connectionID);
        connectionConfig.setConnectionUrl(connectionURL);
        connectionConfig.setCredentials(credentials);
        SdkVersion.setSdkPrefix(Constants.SDK_PREFIX);
    }

    @Test
    public void testInvalidRequestInInvokeConnectionMethod() {
        try {
            HashMap<String, String> requestBody = new HashMap<>();
            InvokeConnectionRequest connectionRequest = InvokeConnectionRequest.builder().requestBody(requestBody).build();
            Skyflow skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG).addConnectionConfig(connectionConfig).build();
            skyflowClient.connection().invoke(connectionRequest);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.EmptyRequestBody.getMessage(), e.getMessage());
        }
    }
}
