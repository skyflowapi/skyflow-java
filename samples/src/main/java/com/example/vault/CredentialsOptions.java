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
 * This class demonstrates multiple authentication methods and deletion operations across different Skyflow vaults.
 * <p>
 * The operations performed in this class include:
 * 1. Setting up authentication credentials with multiple options.
 * 2. Configuring primary and secondary vaults.
 * 3. Initializing a Skyflow client with multiple vault configurations.
 * 4. Performing secure deletion of records from both vaults.
 */
public class CredentialsOptions {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Set up authentication credentials using an API key
        Credentials credentials = new Credentials();
        credentials.setApiKey("<YOUR_API_KEY>"); // Replace with your actual API key

        // Alternative authentication methods (uncomment if needed)
        // credentials.setToken("<YOUR_BEARER_TOKEN>");
        // credentials.setPath("<YOUR_CREDENTIALS_FILE_PATH>");
        // credentials.setCredentialsString("<YOUR_CREDENTIALS_STRING>");

        // Step 2: Configure the primary vault
        VaultConfig primaryVaultConfig = new VaultConfig();
        primaryVaultConfig.setVaultId("<YOUR_VAULT_ID_1>"); // Set first vault ID
        primaryVaultConfig.setClusterId("<YOUR_CLUSTER_ID_1>"); // Set first cluster ID
        primaryVaultConfig.setEnv(Env.PROD); // Define the environment (e.g., PROD, DEV, STAGE, SANDBOX)

        // Step 3: Configure the secondary vault with credentials
        VaultConfig secondaryVaultConfig = new VaultConfig();
        secondaryVaultConfig.setVaultId("<YOUR_VAULT_ID_2>"); // Set second vault ID
        secondaryVaultConfig.setClusterId("<YOUR_CLUSTER_ID_2>"); // Set second cluster ID
        secondaryVaultConfig.setEnv(Env.PROD); // Define the environment
        secondaryVaultConfig.setCredentials(credentials); // Attach authentication credentials

        // Step 4: Create a Skyflow client instance with both vault configurations
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.ERROR) // Set logging level to ERROR
                .addVaultConfig(primaryVaultConfig) // Associate the primary vault configuration
                .addVaultConfig(secondaryVaultConfig) // Associate the secondary vault configuration
                .build();

        // Step 5: Perform secure deletion from the first vault
        try {
            ArrayList<String> ids1 = new ArrayList<>();
            ids1.add("<YOUR_SKYFLOW_ID_VALUE>"); // Replace with the actual ID to delete
            DeleteRequest deleteRequest1 = DeleteRequest.builder()
                    .ids(ids1) // Specify record IDs targeted for deletion
                    .table("<TABLE_NAME>") // Set the table name from which records should be deleted
                    .build();

            DeleteResponse deleteResponse1 = skyflowClient.vault("<YOUR_VAULT_ID_1>").delete(deleteRequest1);
            System.out.println("Delete Response (Vault 1): " + deleteResponse1);
        } catch (SkyflowException e) {
            System.out.println("Error during delete operation in Vault 1: " + e);
        }

        // Step 6: Perform secure deletion from the second vault
        try {
            ArrayList<String> ids2 = new ArrayList<>();
            ids2.add("<YOUR_SKYFLOW_ID_VALUE>"); // Replace with the actual ID to delete
            DeleteRequest deleteRequest2 = DeleteRequest.builder()
                    .ids(ids2) // Specify record IDs targeted for deletion
                    .table("<TABLE_NAME>") // Set the table name from which records should be deleted
                    .build();

            DeleteResponse deleteResponse2 = skyflowClient.vault("<YOUR_VAULT_ID_2>").delete(deleteRequest2);
            System.out.println("Delete Response (Vault 2): " + deleteResponse2);
        } catch (SkyflowException e) {
            System.out.println("Error during delete operation in Vault 2: " + e);
        }
    }
}
