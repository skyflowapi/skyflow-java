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

        // Example 1: Fetch records by Skyflow IDs from the vault
        try {
            ArrayList<String> ids = new ArrayList<>();
            ids.add("<YOUR_SKYFLOW_ID>");                  // Replace with the Skyflow ID to fetch the record

            GetRequest getByIdRequest = GetRequest.builder()
                    .ids(ids)                             // Specify the list of Skyflow IDs
                    .table("<TABLE_NAME>")                // Replace with the table name
                    .build();

            GetResponse getByIdResponse = skyflowClient.vault().get(getByIdRequest); // Fetch via skyflow IDs
            System.out.println("Get Response (By ID): " + getByIdResponse);
        } catch (SkyflowException e) {
            System.out.println("Error during fetch by ID:");
            e.printStackTrace();
        }

        // Example 2: Fetch records by column values from the vault
        try {
            ArrayList<String> columnValues = new ArrayList<>();
            columnValues.add("<YOUR_COLUMN_VALUE>");       // Replace with the column value to fetch the record

            GetRequest getByColumnRequest = GetRequest.builder()
                    .table("<TABLE_NAME>")                // Replace with the table name
                    .columnName("<COLUMN_NAME>")          // Replace with the column name
                    .columnValues(columnValues)           // Specify the list of column values
                    .redactionType(RedactionType.PLAIN_TEXT) // Fetch the data in plain text format
                    .build();

            GetResponse getByColumnResponse = skyflowClient.vault().get(getByColumnRequest); // Fetch via column values
            System.out.println("Get Response (By Column): " + getByColumnResponse);
        } catch (SkyflowException e) {
            System.out.println("Error during fetch by column:");
            e.printStackTrace();
        }
    }
}
