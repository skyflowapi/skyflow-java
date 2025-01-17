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
 * This example demonstrates how to use the Skyflow SDK to invoke connections for different endpoints with different configurations.
 * It includes:
 * 1. Setting up connection configurations.
 * 2. Creating a Skyflow client.
 * 3. Sending POST and GET requests to connections.
 */
public class InvokeConnectionExample {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Set up credentials for the first connection configuration
        Credentials credentials = new Credentials();
        credentials.setPath("<YOUR_CREDENTIALS_FILE_PATH_1>"); // Replace with the path to the credentials file

        // Step 2: Configure the first connection (Connection 1)
        ConnectionConfig connectionConfig1 = new ConnectionConfig();
        connectionConfig1.setConnectionId("<YOUR_CONNECTION_ID_1>"); // Replace with the ID of the first connection
        connectionConfig1.setConnectionUrl("<YOUR_CONNECTION_URL_1>"); // Replace with the URL of the first connection
        connectionConfig1.setCredentials(credentials); // Associate credentials for the first connection

        // Step 3: Configure the second connection (Connection 2)
        ConnectionConfig connectionConfig2 = new ConnectionConfig();
        connectionConfig2.setConnectionId("<YOUR_CONNECTION_ID_2>"); // Replace with the ID of the second connection
        connectionConfig2.setConnectionUrl("<YOUR_CONNECTION_URL_2>"); // Replace with the URL of the second connection

        // Step 4: Set up credentials for the Skyflow client
        Credentials skyflowCredentials = new Credentials();
        skyflowCredentials.setPath("<YOUR_CREDENTIALS_FILE_PATH_2>"); // Replace with the path to another credentials file

        // Step 5: Create a Skyflow client and add connection configurations
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)               // Enable debugging for detailed logs
                .addConnectionConfig(connectionConfig1)    // Add the first connection configuration
                .addConnectionConfig(connectionConfig2)    // Add the second connection configuration
                .addSkyflowCredentials(skyflowCredentials)  // Add general Skyflow credentials
                .build();

        // Example 1: Sending a POST request to the first connection
        try {
            // Set up the request body and headers for the POST request
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>"); // Replace with the actual column name and value
            requestBody.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>"); // Replace with another column name and value

            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("<HEADER_NAME_1>", "<HEADER_VALUE_1>"); // Replace with header name and value
            requestHeaders.put("<HEADER_NAME_2>", "<HEADER_VALUE_2>"); // Replace with another header name and value

            // Build the POST request to invoke the connection
            InvokeConnectionRequest invokeConnectionRequest1 = InvokeConnectionRequest.builder()
                    .method(RequestMethod.POST)       // Set the HTTP method to POST
                    .requestBody(requestBody)             // Set the request body
                    .requestHeaders(requestHeaders)       // Set the request headers
                    .build();

            // Execute the POST request to the first connection
            InvokeConnectionResponse invokeConnectionResponse1 = skyflowClient.connection().invoke(invokeConnectionRequest1);
            System.out.println("Invoke Connection Response (POST): " + invokeConnectionResponse1); // Print the response
        } catch (SkyflowException e) {
            System.out.println("Error while invoking connection (POST):");
            e.printStackTrace();
        }

        // Example 2: Sending a GET request to the second connection
        try {
            // Set up path parameters and query parameters for the GET request
            Map<String, String> pathParams = new HashMap<>();
            pathParams.put("<YOUR_PATH_PARAM_KEY_1>", "<YOUR_PATH_PARAM_VALUE_1>"); // Replace with path parameters
            pathParams.put("<YOUR_PATH_PARAM_KEY_2>", "<YOUR_PATH_PARAM_VALUE_2>"); // Replace with another path parameter

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("<YOUR_QUERY_PARAM_KEY_1>", "<YOUR_QUERY_PARAM_VALUE_1>"); // Replace with query parameters
            queryParams.put("<YOUR_QUERY_PARAM_KEY_2>", "<YOUR_QUERY_PARAM_VALUE_2>"); // Replace with another query parameter

            // Build the GET request to invoke the connection
            InvokeConnectionRequest invokeConnectionRequest2 = InvokeConnectionRequest.builder()
                    .method(RequestMethod.GET)       // Set the HTTP method to GET
                    .pathParams(pathParams)              // Set the path parameters
                    .queryParams(queryParams)            // Set the query parameters
                    .build();

            // Execute the GET request to the second connection
            InvokeConnectionResponse invokeConnectionResponse2 = skyflowClient
                    .connection("<YOUR_CONNECTION_ID_2>").invoke(invokeConnectionRequest2); // Invoke connection with ID 2
            System.out.println("Invoke Connection Response (GET): " + invokeConnectionResponse2); // Print the response
        } catch (SkyflowException e) {
            System.out.println("Error while invoking connection (GET):");
            e.printStackTrace();
        }
    }
}
