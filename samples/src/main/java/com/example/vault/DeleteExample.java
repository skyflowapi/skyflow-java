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
 * 1. Setting up Skyflow credentials.
 * 2. Configuring the vault.
 * 3. Creating a Skyflow client.
 * 4. Setting the log level for debugging and error tracking.
 * 5. Deleting records from the specified vault(s) using record IDs and table names.
 */
public class DeleteExample {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Set up Skyflow credentials
        Credentials credentials = new Credentials();
        credentials.setPath("<YOUR_CREDENTIALS_FILE_PATH>"); // Replace with the actual path to the credentials file

        // Step 2: Configure the first vault
        VaultConfig primaryVaultConfig = new VaultConfig();
        primaryVaultConfig.setVaultId("<YOUR_VAULT_ID_1>");         // Replace with the ID of the first vault
        primaryVaultConfig.setClusterId("<YOUR_CLUSTER_ID_1>");     // Replace with the cluster ID of the first vault
        primaryVaultConfig.setEnv(Env.PROD);                        // Set the environment (e.g., DEV, STAGE, PROD)
        primaryVaultConfig.setCredentials(credentials);            // Associate the credentials with the vault

        // Step 3: Set up credentials for the Skyflow client
        Credentials skyflowCredentials = new Credentials();
        skyflowCredentials.setCredentialsString("<YOUR_CREDENTIALS_STRING>"); // Replace with credentials string

        // Step 4: Create a Skyflow client and add vault configurations
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.ERROR) // Set log level for debugging and error tracking
                .addVaultConfig(primaryVaultConfig)               // Add the first vault configuration
                .addSkyflowCredentials(skyflowCredentials) // Add general Skyflow credentials
                .build();

        // Step 5: Delete a record from the first vault
        try {
            ArrayList<String> ids = new ArrayList<>();
            ids.add("<YOUR_SKYFLOW_ID_VALUE>");           // Replace with the ID of the record to delete
            DeleteRequest deleteRequest = DeleteRequest.builder()
                    .ids(ids)                             // Specify the record IDs to delete
                    .table("<TABLE_NAME>")                 // Replace with the table name
                    .build();

            DeleteResponse deleteResponse = skyflowClient.vault().delete(deleteRequest); // Perform the delete operation
            System.out.println("Delete Response: " + deleteResponse);
        } catch (SkyflowException e) {
            System.out.println("Error during delete operation in Vault: " + e);
        }
    }
}
