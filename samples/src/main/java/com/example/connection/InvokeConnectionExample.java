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

/**
 * This example demonstrates how to use the Skyflow SDK to invoke API connections.
 * It includes:
 * 1. Setting up credentials and connection configurations.
 * 2. Creating a Skyflow client with multiple connections.
 * 3. Sending a POST request with request body and headers.
 * 4. Sending a GET request with path and query parameters.
 */
public class InvokeConnectionExample {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Set up credentials for API authentication
        Credentials credentials = new Credentials();
        credentials.setApiKey("<YOUR_API_KEY>"); // Replace with the actual API key

        // Step 2: Configure the first connection
        ConnectionConfig primaryConnectionConfig = new ConnectionConfig();
        primaryConnectionConfig.setConnectionId("<YOUR_CONNECTION_ID_1>"); // Replace with first connection ID
        primaryConnectionConfig.setConnectionUrl("<YOUR_CONNECTION_URL_1>"); // Replace with first connection URL
        primaryConnectionConfig.setCredentials(credentials); // Assign credentials

        // Step 3: Configure the second connection
        ConnectionConfig secondaryConnectionConfig = new ConnectionConfig();
        secondaryConnectionConfig.setConnectionId("<YOUR_CONNECTION_ID_2>"); // Replace with second connection ID
        secondaryConnectionConfig.setConnectionUrl("<YOUR_CONNECTION_URL_2>"); // Replace with second connection URL

        // Step 4: Set up credentials for the Skyflow client
        Credentials skyflowCredentials = new Credentials();
        skyflowCredentials.setCredentialsString("<YOUR_CREDENTIALS_STRING>"); // Replace with the credentials string

        // Step 5: Create a Skyflow client with connection configurations
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.ERROR)               // Set log level to ERROR
                .addConnectionConfig(primaryConnectionConfig)  // Add the first connection
                .addConnectionConfig(secondaryConnectionConfig) // Add the second connection
                .addSkyflowCredentials(skyflowCredentials)  // Provide Skyflow credentials
                .build();

        // Example 1: Sending a POST request to the first connection
        try {
            // Set up request body and headers
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>"); // Replace with actual column name and value
            requestBody.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>"); // Replace with another column name and value

            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("<HEADER_NAME_1>", "<HEADER_VALUE_1>"); // Replace with actual header name and value
            requestHeaders.put("<HEADER_NAME_2>", "<HEADER_VALUE_2>"); // Replace with another header name and value

            // Build and send the POST request
            InvokeConnectionRequest invokeConnectionRequest1 = InvokeConnectionRequest.builder()
                    .method(RequestMethod.POST)        // HTTP method set to POST
                    .requestBody(requestBody)          // Include request body
                    .requestHeaders(requestHeaders)    // Include request headers
                    .build();

            InvokeConnectionResponse invokeConnectionResponse1 = skyflowClient.connection().invoke(invokeConnectionRequest1);
            System.out.println("Invoke Connection Response (POST): " + invokeConnectionResponse1);
        } catch (SkyflowException e) {
            System.out.println("Error while invoking connection (POST):" + e);
        }

        // Example 2: Sending a GET request to the second connection
        try {
            // Set up path and query parameters
            Map<String, String> pathParams = new HashMap<>();
            pathParams.put("<YOUR_PATH_PARAM_KEY_1>", "<YOUR_PATH_PARAM_VALUE_1>"); // Replace with actual path parameter
            pathParams.put("<YOUR_PATH_PARAM_KEY_2>", "<YOUR_PATH_PARAM_VALUE_2>"); // Replace with another path parameter

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("<YOUR_QUERY_PARAM_KEY_1>", "<YOUR_QUERY_PARAM_VALUE_1>"); // Replace with actual query parameter
            queryParams.put("<YOUR_QUERY_PARAM_KEY_2>", "<YOUR_QUERY_PARAM_VALUE_2>"); // Replace with another query parameter

            // Build and send the GET request
            InvokeConnectionRequest invokeConnectionRequest2 = InvokeConnectionRequest.builder()
                    .method(RequestMethod.GET)      // HTTP method set to GET
                    .pathParams(pathParams)         // Include path parameters
                    .queryParams(queryParams)       // Include query parameters
                    .build();

            InvokeConnectionResponse invokeConnectionResponse2 = skyflowClient
                    .connection("<YOUR_CONNECTION_ID_2>").invoke(invokeConnectionRequest2);
            System.out.println("Invoke Connection Response (GET): " + invokeConnectionResponse2);
        } catch (SkyflowException e) {
            System.out.println("Error while invoking connection (GET):" + e);
        }
    }
}
