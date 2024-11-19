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
        credentials.setPath("<path_to_your_credentials_file_1>");

        ConnectionConfig connectionConfig1 = new ConnectionConfig();
        connectionConfig1.setConnectionId("<your_connection_id_1>");
        connectionConfig1.setConnectionUrl("<your_connection_url_1>");
        connectionConfig1.setCredentials(credentials);

        ConnectionConfig connectionConfig2 = new ConnectionConfig();
        connectionConfig2.setConnectionId("<your_connection_id_2>");
        connectionConfig2.setConnectionUrl("<your_connection_url_2>");

        Credentials skyflowCredentials = new Credentials();
        credentials.setPath("<path_to_your_credentials_file_2>");

        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)
                .addConnectionConfig(connectionConfig1)
                .addConnectionConfig(connectionConfig2)
                .addSkyflowCredentials(skyflowCredentials)
                .build();

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("<column_name_1>", "<column_value_1>");
        requestBody.put("<column_name_2>", "<column_value_2>");
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("<header_name_1>", "<header_value_1>");
        requestHeaders.put("<header_name_2>", "<header_value_2>");
        InvokeConnectionRequest invokeConnectionRequest1 = InvokeConnectionRequest.builder()
                .methodName(RequestMethod.POST)
                .requestBody(requestBody)
                .requestHeaders(requestHeaders)
                .build();
        InvokeConnectionResponse invokeConnectionResponse1 = skyflowClient.connection().invoke(invokeConnectionRequest1);
        System.out.println(invokeConnectionResponse1);

        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("<your_path_param_key_1>", "<your_path_param_value_1>");
        pathParams.put("<your_path_param_key_2>", "<your_path_param_value_2>");
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("<your_query_param_key_1>", "<your_query_param_value_1>");
        queryParams.put("<your_query_param_key_2>", "<your_query_param_value_2>");
        InvokeConnectionRequest invokeConnectionRequest2 = InvokeConnectionRequest.builder()
                .methodName(RequestMethod.GET)
                .pathParams(pathParams)
                .queryParams(queryParams)
                .build();
        InvokeConnectionResponse invokeConnectionResponse2 = skyflowClient
                .connection("<your_connection_id_2>").invoke(invokeConnectionRequest2);
        System.out.println(invokeConnectionResponse2);
    }
}
