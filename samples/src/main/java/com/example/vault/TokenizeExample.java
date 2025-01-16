package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.tokens.ColumnValue;
import com.skyflow.vault.tokens.TokenizeRequest;
import com.skyflow.vault.tokens.TokenizeResponse;

import java.util.ArrayList;

/**
 * This example demonstrates how to use the Skyflow SDK to tokenize data using multiple vault configurations.
 * It includes:
 * 1. Setting up vault configurations.
 * 2. Creating a Skyflow client.
 * 3. Performing tokenization on the vaults.
 */
public class TokenizeExample {
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

        // Example 1: Tokenize data for the first vault
        try {
            ArrayList<ColumnValue> columnValues1 = new ArrayList<>();
            ColumnValue value1 = ColumnValue.builder()
                    .value("<VALUE>")                    // Replace with the actual value to tokenize
                    .columnGroup("<COLUMN_GROUP>")       // Replace with the actual column group name
                    .build();
            ColumnValue value2 = ColumnValue.builder()
                    .value("<VALUE>")                    // Replace with another value to tokenize
                    .columnGroup("<COLUMN_GROUP>")       // Replace with the column group name
                    .build();

            columnValues1.add(value1);
            columnValues1.add(value2);

            // Build the tokenization request
            TokenizeRequest tokenizeRequest1 = TokenizeRequest.builder()
                    .values(columnValues1)             // Set the column values to tokenize
                    .build();

            // Execute tokenization request
            TokenizeResponse tokenizeResponse1 = skyflowClient.vault().tokenize(tokenizeRequest1);
            System.out.println("Tokenization Response (Vault 1): " + tokenizeResponse1); // Print the tokenization response
        } catch (SkyflowException e) {
            System.out.println("Error while tokenizing data for Vault 1:");
            e.printStackTrace();
        }

        // Example 2: Tokenize data for the second vault
        try {
            ArrayList<ColumnValue> columnValues2 = new ArrayList<>();
            ColumnValue value3 = ColumnValue.builder()
                    .value("<VALUE>")                    // Replace with the actual value to tokenize
                    .columnGroup("<COLUMN_GROUP>")       // Replace with the column group name
                    .build();
            ColumnValue value4 = ColumnValue.builder()
                    .value("<VALUE>")                    // Replace with another value to tokenize
                    .columnGroup("<COLUMN_GROUP>")       // Replace with the column group name
                    .build();

            columnValues2.add(value3);
            columnValues2.add(value4);

            // Build the tokenization request for the second vault
            TokenizeRequest tokenizeRequest2 = TokenizeRequest.builder()
                    .values(columnValues2)             // Set the column values to tokenize
                    .build();

            // Execute tokenization request for the second vault
            TokenizeResponse tokenizeResponse2 = skyflowClient.vault("<YOUR_VAULT_ID_2>").tokenize(tokenizeRequest2);
            System.out.println("Tokenization Response (Vault 2): " + tokenizeResponse2); // Print the tokenization response
        } catch (SkyflowException e) {
            System.out.println("Error while tokenizing data for Vault 2:");
            e.printStackTrace();
        }
    }
}
