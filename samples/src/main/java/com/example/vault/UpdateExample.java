package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Byot;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateResponse;

import java.util.HashMap;

/**
 * This example demonstrates how to use the Skyflow SDK to securely update records in a vault.
 * It includes:
 * 1. Setting up vault configurations.
 * 2. Creating a Skyflow client.
 * 3. Updating records using different configurations and data.
 */
public class UpdateExample {
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

        // Example 1: Update records in the first vault with BYOT (Bring Your Own Token) enabled
        try {
            HashMap<String, Object> data1 = new HashMap<>();
            data1.put("skyflow_id", "<YOUR_SKYFLOW_ID>");       // Replace with the Skyflow ID of the record
            data1.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>");  // Replace with column name and value to update
            data1.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>");  // Replace with another column name and value

            HashMap<String, Object> tokens = new HashMap<>();
            tokens.put("<COLUMN_NAME_2>", "<TOKEN_VALUE_2>");  // Replace with the token for COLUMN_NAME_2

            UpdateRequest updateRequest1 = UpdateRequest.builder()
                    .table("<TABLE_NAME>")                     // Replace with the table name
                    .tokenStrict(Byot.ENABLE)                 // Enable BYOT for token validation
                    .data(data1)                               // Data to update
                    .tokens(tokens)                            // Provide tokens for BYOT columns
                    .returnTokens(true)                        // Return tokens along with the update response
                    .build();

            UpdateResponse updateResponse1 = skyflowClient.vault().update(updateRequest1); // Perform the update
            System.out.println("Update Response (BYOT Enabled): " + updateResponse1);
        } catch (SkyflowException e) {
            System.out.println("Error during update with BYOT enabled:");
            e.printStackTrace();
        }

        // Example 2: Update records in the second vault with BYOT disabled
        try {
            HashMap<String, Object> data2 = new HashMap<>();
            data2.put("skyflow_id", "<YOUR_SKYFLOW_ID>");       // Replace with the Skyflow ID of the record
            data2.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>");  // Replace with column name and value to update
            data2.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>");  // Replace with another column name and value

            UpdateRequest updateRequest2 = UpdateRequest.builder()
                    .table("<TABLE_NAME>")                     // Replace with the table name
                    .tokenStrict(Byot.DISABLE)                // Disable BYOT
                    .data(data2)                               // Data to update
                    .returnTokens(false)                       // Do not return tokens
                    .build();

            UpdateResponse updateResponse2 = skyflowClient.vault("<YOUR_VAULT_ID_2>").update(updateRequest2); // Perform the update
            System.out.println("Update Response (BYOT Disabled): " + updateResponse2);
        } catch (SkyflowException e) {
            System.out.println("Error during update with BYOT disabled:");
            e.printStackTrace();
        }
    }
}
