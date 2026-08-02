package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;

import java.util.ArrayList;

/**
 * This example demonstrates how to use the Skyflow SDK to fetch sensitive data securely.
 * The steps include:
 * 1. Setting up vault configurations.
 * 2. Creating a Skyflow client.
 * 3. Retrieving records using Skyflow IDs and column values.
 */
public class GetExample {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Set up credentials for the first vault configuration
        Credentials credentials = new Credentials();
        credentials.setCredentialsString("<YOUR_CREDENTIALS_STRING_1>"); // Replace with the actual credentials string

        // Step 2: Configure the vault
        VaultConfig vaultConfig = new VaultConfig();
        vaultConfig.setVaultId("<YOUR_VAULT_ID_1>");         // Replace with the ID of the first vault
        vaultConfig.setClusterId("<YOUR_CLUSTER_ID_1>");     // Replace with the cluster ID of the first vault
        vaultConfig.setEnv(Env.PROD);                        // Set the environment (e.g., DEV, STAGE, PROD)
        vaultConfig.setCredentials(credentials);            // Associate the credentials with the vault

        // Step 3: Set up credentials for the Skyflow client
        Credentials skyflowCredentials = new Credentials();
        skyflowCredentials.setCredentialsString("<YOUR_CREDENTIALS_STRING_2>"); // Replace with another credentials string

        // Step 4: Create a Skyflow client and add vault configurations
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.ERROR)               // Set log level to ERROR to minimize log output
                .addVaultConfig(vaultConfig)        // Add the first vault configuration
                .addSkyflowCredentials(skyflowCredentials) // Add general Skyflow credentials
                .build();

        // Example 1: Fetch records by Skyflow IDs — plain text values
        try {
            ArrayList<String> ids = new ArrayList<>();
            ids.add("<YOUR_SKYFLOW_ID>"); // Replace with the Skyflow ID of the record to fetch

            GetRequest getByIdRequest = GetRequest.builder()
                    .ids(ids)                                   // Skyflow IDs to retrieve
                    .table("<TABLE_NAME>")                      // Replace with your table name
                    .redactionType(RedactionType.PLAIN_TEXT)    // PLAIN_TEXT / MASKED / REDACTED / DEFAULT
                    .returnTokens(false)                        // true: return tokens instead of values
                    // .fields(fields)                          // Optional: ArrayList<String> of column names to return (subset of columns)
                    // .offset("<OFFSET>")                      // Optional: pagination offset (record index to start from)
                    // .limit("<LIMIT>")                        // Optional: max number of records to return per page
                    // .orderBy("<COLUMN_NAME>")                // Optional: column name to sort results by
                    // .downloadURL(true)                       // Optional: return a signed download URL for file columns
                    .build();

            GetResponse getByIdResponse = skyflowClient.vault().get(getByIdRequest);
            System.out.println("Get Response (By Skyflow ID): " + getByIdResponse);
        } catch (SkyflowException e) {
            System.out.println("Error during fetch by ID:");
            e.printStackTrace();
        }

        // Example 2: Fetch records by Skyflow IDs — return tokens instead of values
        try {
            ArrayList<String> ids = new ArrayList<>();
            ids.add("<YOUR_SKYFLOW_ID>"); // Replace with the Skyflow ID of the record to fetch

            GetRequest getTokensRequest = GetRequest.builder()
                    .ids(ids)
                    .table("<TABLE_NAME>")  // Replace with your table name
                    .returnTokens(true)    // Return vault tokens for sensitive columns instead of plain values
                    .build();

            GetResponse getTokensResponse = skyflowClient.vault().get(getTokensRequest);
            System.out.println("Get Response (Tokens): " + getTokensResponse);
        } catch (SkyflowException e) {
            System.out.println("Error during fetch tokens:");
            e.printStackTrace();
        }

        // Example 3: Fetch records by unique column values
        try {
            ArrayList<String> columnValues = new ArrayList<>();
            columnValues.add("<YOUR_COLUMN_VALUE>"); // Replace with the column value to match

            GetRequest getByColumnRequest = GetRequest.builder()
                    .table("<TABLE_NAME>")                      // Replace with your table name
                    .columnName("<COLUMN_NAME>")                // Unique column to filter by (e.g. "email")
                    .columnValues(columnValues)                 // Values to match in that column
                    .redactionType(RedactionType.PLAIN_TEXT)    // PLAIN_TEXT / MASKED / REDACTED / DEFAULT
                    // .fields(fields)                          // Optional: limit which columns are returned
                    // .offset("<OFFSET>")                      // Optional: pagination offset
                    // .limit("<LIMIT>")                        // Optional: max records per page
                    // .orderBy("<COLUMN_NAME>")                // Optional: sort column
                    .build();

            GetResponse getByColumnResponse = skyflowClient.vault().get(getByColumnRequest);
            System.out.println("Get Response (By Column): " + getByColumnResponse);
        } catch (SkyflowException e) {
            System.out.println("Error during fetch by column:");
            e.printStackTrace();
        }
    }
}
