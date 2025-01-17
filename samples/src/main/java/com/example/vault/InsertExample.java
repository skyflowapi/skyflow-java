package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.TokenMode;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
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
 * 3. Performing record insertion with and without BYOT (Bring Your Own Token).
 * 4. Using upsert functionality to handle conflicts.
 */
public class InsertExample {
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

        // Example 1: Insert records into the first vault with BYOT enabled
        try {
            ArrayList<HashMap<String, Object>> values1 = new ArrayList<>();
            HashMap<String, Object> value1 = new HashMap<>();
            value1.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>"); // Replace with column name and value
            value1.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>"); // Replace with another column name and value
            values1.add(value1);

            ArrayList<HashMap<String, Object>> tokens = new ArrayList<>();
            HashMap<String, Object> token = new HashMap<>();
            token.put("<COLUMN_NAME_2>", "<TOKEN_VALUE_2>"); // Replace with the token for COLUMN_NAME_2
            tokens.add(token);

            InsertRequest insertRequest = InsertRequest.builder()
                    .table("<TABLE_NAME>")                     // Replace with the table name
                    .continueOnError(true)                     // Continue inserting even if some records fail
                    .tokenMode(TokenMode.ENABLE)                 // Enable BYOT for token validation
                    .values(values1)                           // Data to insert
                    .tokens(tokens)                            // Provide tokens for BYOT columns
                    .returnTokens(true)                        // Return tokens along with the response
                    .build();

            InsertResponse insertResponse = skyflowClient.vault().insert(insertRequest); // Perform the insertion
            System.out.println("Insert Response (BYOT Enabled): " + insertResponse);
        } catch (SkyflowException e) {
            System.out.println("Error during insertion with BYOT enabled:");
            e.printStackTrace();
        }

        // Example 2: Insert records into the second vault with BYOT disabled and upsert enabled
        try {
            ArrayList<HashMap<String, Object>> values2 = new ArrayList<>();
            HashMap<String, Object> value2 = new HashMap<>();
            value2.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>"); // Replace with column name and value
            value2.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>"); // Replace with another column name and value
            values2.add(value2);

            InsertRequest upsertRequest = InsertRequest.builder()
                    .table("<TABLE_NAME>")                     // Replace with the table name
                    .continueOnError(false)                    // Stop inserting if any record fails
                    .tokenMode(TokenMode.DISABLE)                // Disable BYOT
                    .values(values2)                           // Data to insert
                    .returnTokens(false)                       // Do not return tokens
                    .upsert("<UPSERT_COLUMN>")                 // Replace with the column name used for upsert logic
                    .build();

            InsertResponse upsertResponse = skyflowClient.vault("<YOUR_VAULT_ID_2>").insert(upsertRequest); // Perform the insertion
            System.out.println("Insert Response (Upsert Enabled): " + upsertResponse);
        } catch (SkyflowException e) {
            System.out.println("Error during insertion with upsert enabled:");
            e.printStackTrace();
        }
    }
}
