package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.DeleteRequest;
import com.skyflow.vault.data.DeleteResponse;

import java.util.ArrayList;

/**
 * This example demonstrates how to use the Skyflow SDK to delete records from one or more vaults
 * by specifying the vault configurations, credentials, and record IDs to delete.
 * <p>
 * Steps include:
 * 1. Setting up vault configurations.
 * 2. Creating a Skyflow client.
 * 3. Deleting records from the specified vault(s) using record IDs and table names.
 */
public class DeleteExample {
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

        // Example 1: Delete a record from the first vault
        try {
            ArrayList<String> ids1 = new ArrayList<>();
            ids1.add("<YOUR_SKYFLOW_ID_VALUE>");           // Replace with the ID of the record to delete
            DeleteRequest deleteRequest1 = DeleteRequest.builder()
                    .ids(ids1)                             // Specify the record IDs to delete
                    .table("<TABLE_NAME>")                 // Replace with the table name
                    .build();

            DeleteResponse deleteResponse1 = skyflowClient.vault().delete(deleteRequest1); // Perform the delete operation
            System.out.println("Delete Response (Vault 1): " + deleteResponse1);
        } catch (SkyflowException e) {
            System.out.println("Error during delete operation in Vault 1:");
            e.printStackTrace();
        }

        // Example 2: Delete a record from the second vault
        try {
            ArrayList<String> ids2 = new ArrayList<>();
            ids2.add("<YOUR_SKYFLOW_ID_VALUE>");           // Replace with the ID of the record to delete
            DeleteRequest deleteRequest2 = DeleteRequest.builder()
                    .ids(ids2)                             // Specify the record IDs to delete
                    .table("<TABLE_NAME>")                 // Replace with the table name
                    .build();

            DeleteResponse deleteResponse2 = skyflowClient.vault("<YOUR_VAULT_ID_2>").delete(deleteRequest2); // Perform the delete operation
            System.out.println("Delete Response (Vault 2): " + deleteResponse2);
        } catch (SkyflowException e) {
            System.out.println("Error during delete operation in Vault 2:");
            e.printStackTrace();
        }
    }
}
