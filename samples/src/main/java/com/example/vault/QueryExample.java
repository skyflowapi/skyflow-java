package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.QueryResponse;

/**
 * This example demonstrates how to use the Skyflow SDK to perform secure queries on multiple vaults.
 * It includes:
 * 1. Setting up vault configurations.
 * 2. Creating a Skyflow client.
 * 3. Performing SQL queries on the vaults.
 */
public class QueryExample {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Set up credentials for the first vault configuration
        Credentials credentials = new Credentials();
        credentials.setPath("<YOUR_CREDENTIALS_FILE_PATH_1>"); // Replace with the path to the credentials file

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

        // Example 1: Perform a query on the first vault
        try {
            String query1 = "<YOUR_SQL_QUERY>"; // Replace with a valid SQL query for the first vault
            QueryRequest queryRequest1 = QueryRequest.builder()
                    .query(query1)             // Build the query request
                    .build();

            QueryResponse queryResponse1 = skyflowClient.vault().query(queryRequest1); // Execute the query
            System.out.println("Query Response (Vault 1): " + queryResponse1);        // Print the query response
        } catch (SkyflowException e) {
            System.out.println("Error while querying Vault 1:");
            e.printStackTrace();
        }

        // Example 2: Perform a query on the second vault
        try {
            String query2 = "<YOUR_SQL_QUERY>"; // Replace with a valid SQL query for the second vault
            QueryRequest queryRequest2 = QueryRequest.builder()
                    .query(query2)             // Build the query request
                    .build();

            QueryResponse queryResponse2 = skyflowClient.vault("<YOUR_VAULT_ID_2>").query(queryRequest2); // Execute the query
            System.out.println("Query Response (Vault 2): " + queryResponse2);                           // Print the query response
        } catch (SkyflowException e) {
            System.out.println("Error while querying Vault 2:");
            e.printStackTrace();
        }
    }
}

