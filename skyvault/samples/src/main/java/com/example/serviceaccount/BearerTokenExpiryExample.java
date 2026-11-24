package com.example.serviceaccount;

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
 * This example demonstrates how to handle a bearer token that expires while a
 * connection request is in flight. Vault and Detect calls retry once on their own
 * when this happens, but connection calls return the 401 error, so this example
 * retries the connection call once when it fails with HTTP 401.
 */
public class BearerTokenExpiryExample {
    public static void main(String[] args) {
        try {
            // Let the SDK generate bearer tokens from service account credentials
            Credentials credentials = new Credentials();
            credentials.setCredentialsString("<YOUR_CREDENTIALS_STRING>");

            ConnectionConfig connectionConfig = new ConnectionConfig();
            connectionConfig.setConnectionId("<CONNECTION_ID>");
            connectionConfig.setConnectionUrl("<CONNECTION_URL>");
            connectionConfig.setCredentials(credentials);

            Skyflow skyflowClient = Skyflow.builder()
                    .setLogLevel(LogLevel.ERROR)
                    .addConnectionConfig(connectionConfig)
                    .build();

            try {
                invokeConnection(skyflowClient);
            } catch (SkyflowException e) {
                if (e.getHttpCode() == 401) {
                    // The SDK generates a new bearer token at the start of this call
                    invokeConnection(skyflowClient);
                } else {
                    throw e;
                }
            }
        } catch (SkyflowException e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
    }

    public static void invokeConnection(Skyflow skyflowClient) throws SkyflowException {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("<FIELD_NAME>", "<FIELD_VALUE>");

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Type", "application/json");

        InvokeConnectionRequest invokeConnectionRequest = InvokeConnectionRequest.builder()
                .method(RequestMethod.POST)
                .requestBody(requestBody)
                .requestHeaders(requestHeaders)
                .build();

        InvokeConnectionResponse invokeConnectionResponse =
                skyflowClient.connection("<CONNECTION_ID>").invoke(invokeConnectionRequest);
        System.out.println(invokeConnectionResponse);
    }
}
