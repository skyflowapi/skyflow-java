package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.enums.TokenMode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This example demonstrates how to use the Skyflow SDK to securely insert records into a vault.
 * It includes:
 * 1. Setting up vault configurations.
 * 2. Creating a Skyflow client.
 * 3. Performing record insertion with and without TokenMode.
 * 4. Using upsert functionality to handle conflicts.
 */
public class InsertExample {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Set up credentials for the first vault configuration
        Credentials credentials = new Credentials();
        credentials.setApiKey("<YOUR_API_KEY>"); // Replace with the actual API key

        // Step 2: Configure the first vault
        VaultConfig primaryVaultConfig = new VaultConfig();
        primaryVaultConfig.setVaultId("<YOUR_VAULT_ID_1>");         // Replace with the first vault ID
        primaryVaultConfig.setClusterId("<YOUR_CLUSTER_ID_1>");     // Replace with the first vault cluster ID
        primaryVaultConfig.setEnv(Env.PROD);                        // Set the environment (e.g., DEV, STAGE, SANDBOX)
        primaryVaultConfig.setCredentials(credentials);             // Associate credentials with the vault

        // Step 3: Set up credentials for the Skyflow client
        Credentials skyflowCredentials = new Credentials();
        skyflowCredentials.setCredentialsString("<YOUR_CREDENTIALS_STRING>"); // Replace with the actual credentials string

        // Step 4: Create a Skyflow client and add vault configurations
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.ERROR)               // Set log level to ERROR to limit output
                .addVaultConfig(primaryVaultConfig)        // Add the vault configuration
                .addSkyflowCredentials(skyflowCredentials) // Add general Skyflow credentials
                .build();

        // Example 1: Insert records into the first vault with TokenMode enabled
        try {
            ArrayList<HashMap<String, Object>> values1 = new ArrayList<>();
            HashMap<String, Object> value1 = new HashMap<>();
            value1.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>"); // Replace with actual column name and value
            value1.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>"); // Replace with actual column name and value
            values1.add(value1);

            ArrayList<HashMap<String, Object>> tokens = new ArrayList<>();
            HashMap<String, Object> token = new HashMap<>();
            token.put("<COLUMN_NAME_2>", "<TOKEN_VALUE_2>"); // Replace with actual token value for COLUMN_NAME_2
            tokens.add(token);

            InsertRequest insertRequest = InsertRequest.builder()
                    .table("<TABLE_NAME>")                     // Replace with the actual table name
                    .continueOnError(true)                     // Continue inserting even if some records fail
                    .tokenMode(TokenMode.ENABLE)               // Enable TokenMode for token validation
                    .values(values1)                           // Data to insert
                    .tokens(tokens)                            // Provide tokens for TokenMode columns
                    .returnTokens(true)                        // Return tokens in the response
                    .build();

            InsertResponse insertResponse = skyflowClient.vault().insert(insertRequest); // Perform the insertion
            System.out.println("Insert Response (TokenMode Enabled): " + insertResponse);
        } catch (SkyflowException e) {
            System.out.println("Error during insertion with TokenMode enabled:" + e);
        }

        // Example 2: Insert records into the first vault with TokenMode disabled and upsert enabled
        try {
            ArrayList<HashMap<String, Object>> values2 = new ArrayList<>();
            HashMap<String, Object> value2 = new HashMap<>();
            value2.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>"); // Replace with actual column name and value
            value2.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>"); // Replace with actual column name and value
            values2.add(value2);

            InsertRequest upsertRequest = InsertRequest.builder()
                    .table("<TABLE_NAME>")                     // Replace with the actual table name
                    .continueOnError(false)                    // Stop inserting if any record fails
                    .tokenMode(TokenMode.DISABLE)              // Disable TokenMode
                    .values(values2)                           // Data to insert
                    .returnTokens(false)                       // Do not return tokens
                    .upsert("<UPSERT_COLUMN>")                 // Replace with the actual column name used for upsert logic
                    .build();

            InsertResponse upsertResponse = skyflowClient.vault().insert(upsertRequest); // Perform upsert operation
            System.out.println("Insert Response (Upsert Enabled): " + upsertResponse);
        } catch (SkyflowException e) {
            System.out.println("Error during insertion with upsert enabled:" + e);
        }
    }
}