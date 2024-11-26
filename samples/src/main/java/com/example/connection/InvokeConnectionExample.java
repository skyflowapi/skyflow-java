package com.example.connection;

import com.skyflow.Skyflow;
import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.enums.LogLevel;
import com.skyflow.enums.RequestMethod;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.connection.InvokeConnectionRequest;
import com.skyflow.vault.connection.InvokeConnectionResponse;

import java.util.HashMap;
import java.util.Map;

public class InvokeConnectionExample {
    public static void main(String[] args) throws SkyflowException {
        Credentials credentials = new Credentials();
        credentials.setPath("<YOUR_CREDENTIALS_FILE_PATH_1>");

        ConnectionConfig connectionConfig1 = new ConnectionConfig();
        connectionConfig1.setConnectionId("<YOUR_CONNECTION_ID_1>");
        connectionConfig1.setConnectionUrl("<YOUR_CONNECTION_URL_1>");
        connectionConfig1.setCredentials(credentials);

        ConnectionConfig connectionConfig2 = new ConnectionConfig();
        connectionConfig2.setConnectionId("<YOUR_CONNECTION_ID_2>");
        connectionConfig2.setConnectionUrl("<YOUR_CONNECTION_URL_2>");

        Credentials skyflowCredentials = new Credentials();
        skyflowCredentials.setPath("<YOUR_CREDENTIALS_FILE_PATH_2>");

        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)
                .addConnectionConfig(connectionConfig1)
                .addConnectionConfig(connectionConfig2)
                .addSkyflowCredentials(skyflowCredentials)
                .build();

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>");
        requestBody.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>");
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("<HEADER_NAME_1>", "<HEADER_VALUE_1>");
        requestHeaders.put("<HEADER_NAME_2>", "<HEADER_VALUE_2>");
        InvokeConnectionRequest invokeConnectionRequest1 = InvokeConnectionRequest.builder()
                .methodName(RequestMethod.POST)
                .requestBody(requestBody)
                .requestHeaders(requestHeaders)
                .build();
        InvokeConnectionResponse invokeConnectionResponse1 = skyflowClient.connection().invoke(invokeConnectionRequest1);
        System.out.println(invokeConnectionResponse1);

        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("<YOUR_PATH_PARAM_KEY_1>", "<YOUR_PATH_PARAM_VALUE_1>");
        pathParams.put("<YOUR_PATH_PARAM_KEY_2>", "<YOUR_PATH_PARAM_VALUE_2>");
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("<YOUR_QUERY_PARAM_KEY_1>", "<YOUR_QUERY_PARAM_VALUE_1>");
        queryParams.put("<YOUR_QUERY_PARAM_KEY_2>", "<YOUR_QUERY_PARAM_VALUE_2>");
        InvokeConnectionRequest invokeConnectionRequest2 = InvokeConnectionRequest.builder()
                .methodName(RequestMethod.GET)
                .pathParams(pathParams)
                .queryParams(queryParams)
                .build();
        InvokeConnectionResponse invokeConnectionResponse2 = skyflowClient
                .connection("<YOUR_CONNECTION_ID_2>").invoke(invokeConnectionRequest2);
        System.out.println(invokeConnectionResponse2);
    }
}
