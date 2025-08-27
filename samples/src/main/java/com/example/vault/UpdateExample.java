package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.enums.TokenMode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateResponse;

import java.util.HashMap;

/**
 * This example demonstrates how to use the Skyflow SDK to securely update records in a vault.
 * It includes:
 * 1. Setting up vault configurations.
 * 2. Creating a Skyflow client.
 * 3. Updating records with and without TokenMode.
 */
public class UpdateExample {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Set up credentials for the first vault configuration
        Credentials credentials = new Credentials();
        credentials.setApiKey("<YOUR_API_KEY>"); // Replace with the actual API key

        // Step 2: Configure the first vault
        VaultConfig primaryVaultConfig = new VaultConfig();
        primaryVaultConfig.setVaultId("<YOUR_VAULT_ID_1>"); // Replace with the ID of the first vault
        primaryVaultConfig.setClusterId("<YOUR_CLUSTER_ID_1>"); // Replace with the cluster ID of the first vault
        primaryVaultConfig.setEnv(Env.PROD); // Set the environment (e.g., DEV, STAGE, PROD)
        primaryVaultConfig.setCredentials(credentials); // Associate the credentials with the vault

        // Step 3: Set up credentials for the Skyflow client
        Credentials skyflowCredentials = new Credentials();
        skyflowCredentials.setCredentialsString("<YOUR_CREDENTIALS_STRING>"); // Replace with the actual credentials string

        // Step 4: Create a Skyflow client and add vault configurations
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.ERROR) // Enable debugging for detailed logs
                .addVaultConfig(primaryVaultConfig) // Add the first vault configuration
                .addSkyflowCredentials(skyflowCredentials) // Add general Skyflow credentials
                .build();

        // Step 5: Update records with TokenMode enabled
        try {
            HashMap<String, Object> data1 = new HashMap<>();
            data1.put("skyflow_id", "<YOUR_SKYFLOW_ID>"); // Replace with the Skyflow ID of the record
            data1.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>"); // Replace with column name and value to update
            data1.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>"); // Replace with another column name and value

            HashMap<String, Object> tokens = new HashMap<>();
            tokens.put("<COLUMN_NAME_2>", "<TOKEN_VALUE_2>"); // Replace with the token for COLUMN_NAME_2

            UpdateRequest updateRequest1 = UpdateRequest.builder()
                    .table("<TABLE_NAME>") // Replace with the table name
                    .tokenMode(TokenMode.ENABLE) // Enable TokenMode for token validation
                    .data(data1) // Data to update
                    .tokens(tokens) // Provide tokens for TokenMode columns
                    .returnTokens(true) // Return tokens along with the update response
                    .build();

            UpdateResponse updateResponse1 = skyflowClient.vault().update(updateRequest1); // Perform the update
            System.out.println("Update Response (TokenMode Enabled): " + updateResponse1);
        } catch (SkyflowException e) {
            System.out.println("Error during update with TokenMode enabled:");
            e.printStackTrace();
        }

        // Step 6: Update records with TokenMode disabled
        try {
            HashMap<String, Object> data2 = new HashMap<>();
            data2.put("skyflow_id", "<YOUR_SKYFLOW_ID>"); // Replace with the Skyflow ID of the record
            data2.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>"); // Replace with column name and value to update
            data2.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>"); // Replace with another column name and value

            UpdateRequest updateRequest2 = UpdateRequest.builder()
                    .table("<TABLE_NAME>") // Replace with the table name
                    .tokenMode(TokenMode.DISABLE) // Disable TokenMode
                    .data(data2) // Data to update
                    .returnTokens(false) // Do not return tokens
                    .build();

            UpdateResponse updateResponse2 = skyflowClient.vault().update(updateRequest2); // Perform the update
            System.out.println("Update Response (TokenMode Disabled): " + updateResponse2);
        } catch (SkyflowException e) {
            System.out.println("Error during update with TokenMode disabled:" + e);
        }
    }
}
