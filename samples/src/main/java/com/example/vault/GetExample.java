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
        credentials.setPath("<YOUR_CREDENTIALS_FILE_PATH_1>"); // Replace with the actual path to the credentials file

        // Step 2: Configure the first vault (Blitz)
        VaultConfig blitzConfig = new VaultConfig();
        blitzConfig.setVaultId("<YOUR_VAULT_ID_1>");         // Replace with the ID of the first vault
        blitzConfig.setClusterId("<YOUR_CLUSTER_ID_1>");     // Replace with the cluster ID of the first vault
        blitzConfig.setEnv(Env.DEV);                        // Set the environment (e.g., DEV, STAGE, PROD)
        blitzConfig.setCredentials(credentials);            // Associate the credentials with the vault

        // Step 3: Configure the second vault (Stage)
        VaultConfig stageConfig = new VaultConfig();
        stageConfig.setVaultId("<YOUR_VAULT_ID_2>");         // Replace with the ID of the second vault
        stageConfig.setClusterId("<YOUR_CLUSTER_ID_2>");     // Replace with the cluster ID of the second vault
        stageConfig.setEnv(Env.STAGE);                      // Set the environment for the second vault

        // Step 4: Set up credentials for the Skyflow client
        Credentials skyflowCredentials = new Credentials();
        skyflowCredentials.setPath("<YOUR_CREDENTIALS_FILE_PATH_2>"); // Replace with the path to another credentials file

        // Step 5: Create a Skyflow client and add vault configurations
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)               // Enable debugging for detailed logs
                .addVaultConfig(blitzConfig)               // Add the first vault configuration
                .addVaultConfig(stageConfig)               // Add the second vault configuration
                .addSkyflowCredentials(skyflowCredentials) // Add general Skyflow credentials
                .build();

        // Example 1: Fetch records by Skyflow IDs from the first vault
        try {
            ArrayList<String> ids = new ArrayList<>();
            ids.add("<YOUR_SKYFLOW_ID>");                  // Replace with the Skyflow ID to fetch the record

            GetRequest getByIdRequest = GetRequest.builder()
                    .returnTokens(true)                   // Return tokens along with the fetched data
                    .ids(ids)                             // Specify the list of Skyflow IDs
                    .table("<TABLE_NAME>")                // Replace with the table name
                    .build();

            GetResponse getByIdResponse = skyflowClient.vault().get(getByIdRequest); // Perform the get operation
            System.out.println("Get Response (By ID): " + getByIdResponse);
        } catch (SkyflowException e) {
            System.out.println("Error during fetch by ID:");
            e.printStackTrace();
        }

        // Example 2: Fetch records by column values from the second vault
        try {
            ArrayList<String> columnValues = new ArrayList<>();
            columnValues.add("<YOUR_COLUMN_VALUE>");       // Replace with the column value to fetch the record

            GetRequest getByColumnRequest = GetRequest.builder()
                    .table("<TABLE_NAME>")                // Replace with the table name
                    .columnName("<COLUMN_NAME>")          // Replace with the column name
                    .columnValues(columnValues)           // Specify the list of column values
                    .redactionType(RedactionType.PLAIN_TEXT) // Fetch the data in plain text format
                    .build();

            GetResponse getByColumnResponse = skyflowClient.vault("<YOUR_VAULT_ID_2>").get(getByColumnRequest); // Fetch from the second vault
            System.out.println("Get Response (By Column): " + getByColumnResponse);
        } catch (SkyflowException e) {
            System.out.println("Error during fetch by column:");
            e.printStackTrace();
        }
    }
}
