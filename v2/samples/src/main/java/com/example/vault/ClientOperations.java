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
 * This class demonstrates how to configure and interact with Skyflow vaults using the Skyflow Java SDK.
 * <p>
 * The operations performed in this class include:
 * 1. Setting up authentication credentials.
 * 2. Configuring a primary vault and initializing a Skyflow client.
 * 3. Adding a secondary vault to the client.
 * 4. Updating vault configuration.
 * 5. Updating Skyflow API credentials dynamically.
 * 6. Performing a secure deletion of a record in the secondary vault.
 * 7. Removing the secondary vault configuration after the operation.
 * <p>
 * This example illustrates how to securely manage and delete sensitive data using Skyflow.
 */
public class ClientOperations {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Set up authentication credentials for accessing Skyflow vault
        Credentials credentials = new Credentials();
        credentials.setToken("<YOUR_BEARER_TOKEN>"); // Replace with the actual bearer token
        // Alternative authentication methods include API key, credentials file path, or credentialsString

        // Step 2: Configure the primary vault with necessary identifiers and credentials
        VaultConfig primaryVaultConfig = new VaultConfig();
        primaryVaultConfig.setVaultId("<YOUR_VAULT_ID_1>");         // Set first vault ID
        primaryVaultConfig.setClusterId("<YOUR_CLUSTER_ID_1>");     // Set first cluster ID
        primaryVaultConfig.setEnv(Env.PROD);                         // Define the environment (e.g., PROD, DEV, STAGE, SANDBOX)
        primaryVaultConfig.setCredentials(credentials);               // Attach authentication credentials

        // Step 3: Create a Skyflow client instance to interact with the vault
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.ERROR)         // Set logging level (ERROR to reduce verbosity)
                .addVaultConfig(primaryVaultConfig)  // Associate the primary vault configuration
                .build();                            // Build the Skyflow client instance

        // Step 4: Configure the secondary vault, which will be used later for deletion operations
        VaultConfig secondaryVaultConfig = new VaultConfig();
        secondaryVaultConfig.setVaultId("<YOUR_VAULT_ID_2>");     // Set second vault ID
        secondaryVaultConfig.setClusterId("<YOUR_CLUSTER_ID_2>"); // Set second cluster ID
        secondaryVaultConfig.setEnv(Env.PROD);                      // Define the environment

        // Add the secondary vault configuration to the existing Skyflow client
        skyflowClient.addVaultConfig(secondaryVaultConfig);

        // Step 5: Update the secondary vault configuration with credentials
        VaultConfig updatedVaultConfig = new VaultConfig();
        updatedVaultConfig.setVaultId("<YOUR_VAULT_ID_2>");      // Ensure update applies to the correct vault
        updatedVaultConfig.setClusterId("<YOUR_CLUSTER_ID_2>");  // Maintain correct cluster association
        updatedVaultConfig.setCredentials(credentials);           // Attach authentication credentials

        // Apply the updated vault configuration
        skyflowClient.updateVaultConfig(updatedVaultConfig);

        // Step 6: Update Skyflow API credentials dynamically
        Credentials skyflowCredentials = new Credentials();
        skyflowCredentials.setApiKey("<YOUR_API_KEY>"); // Replace with the actual API key

        // Apply the updated credentials to the Skyflow client
        skyflowClient.updateSkyflowCredentials(skyflowCredentials);     // Used when individual credentials are not provided

        try {
            // Step 7: Prepare a delete request to securely remove data from the secondary vault
            ArrayList<String> ids = new ArrayList<>();
            ids.add("<YOUR_SKYFLOW_ID_VALUE>");    // Replace with the actual ID of the record to delete

            DeleteRequest deleteRequest = DeleteRequest.builder()
                    .ids(ids)                       // Specify record IDs targeted for deletion
                    .table("<TABLE_NAME>")         // Set the table name from which records should be deleted
                    .build();

            // Step 8: Execute the secure delete operation on the secondary vault
            DeleteResponse deleteResponse = skyflowClient.vault("<YOUR_VAULT_ID_2>").delete(deleteRequest);
            System.out.println("Delete Response (Vault 2): " + deleteResponse);

            // Step 9: Remove the secondary vault configuration after the operation is completed
            skyflowClient.removeVaultConfig("<YOUR_VAULT_ID_2>");

        } catch (SkyflowException e) {
            // Handle any errors that occur during the delete operation
            System.out.println("Error during delete operation in vault 2: " + e);
        }
    }
}
