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
 * This example demonstrates how to use the Skyflow SDK to perform secure queries on a vault.
 * It includes:
 * 1. Setting up vault configurations.
 * 2. Creating a Skyflow client.
 * 3. Performing SQL queries on the vault.
 */
public class QueryExample {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Set up credentials for the vault configuration
        Credentials credentials = new Credentials();
        credentials.setApiKey("<YOUR_API_KEY>"); // Replace with the actual API key

        // Step 2: Configure the vault
        VaultConfig vaultConfig = new VaultConfig();
        vaultConfig.setVaultId("<YOUR_VAULT_ID>");         // Replace with the ID of the vault
        vaultConfig.setClusterId("<YOUR_CLUSTER_ID>");     // Replace with the cluster ID of the vault
        vaultConfig.setEnv(Env.PROD);                        // Set the environment (e.g., DEV, STAGE, PROD)
        vaultConfig.setCredentials(credentials);             // Associate the credentials with the vault

        // Step 3: Set up credentials for the Skyflow client
        Credentials skyflowCredentials = new Credentials();
        skyflowCredentials.setCredentialsString("<YOUR_CREDENTIALS_STRING>"); // Replace with the actual credentials string

        // Step 4: Create a Skyflow client and add vault configuration
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.ERROR)        // Set log level to ERROR for minimal logging
                .addVaultConfig(vaultConfig)        // Add the vault configuration
                .addSkyflowCredentials(skyflowCredentials) // Add general Skyflow credentials
                .build();

        // Example: Perform a query on the vault
        try {
            String query = "<YOUR_SQL_QUERY>"; // Replace with a valid SQL query for the vault
            QueryRequest queryRequest = QueryRequest.builder()
                    .query(query)    // Build the query request
                    .build();

            QueryResponse queryResponse = skyflowClient.vault().query(queryRequest); // Execute the query
            System.out.println("Query Response: " + queryResponse); // Print the query response
        } catch (SkyflowException e) {
            System.out.println("Error while querying the vault: " + e);
        }
    }
}
