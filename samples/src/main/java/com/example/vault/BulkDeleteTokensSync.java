package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.DeleteTokensRequest;
import com.skyflow.vault.data.DeleteTokensResponse;

import java.util.ArrayList;

/**
 * This sample demonstrates how to perform a synchronous bulk delete tokens operation using the Skyflow Java SDK.
 * The process involves:
 * 1. Setting up credentials and vault configuration
 * 2. Creating a list of tokens to delete
 * 3. Building and executing a bulk delete tokens request
 * 4. Handling the delete tokens response or any potential errors
 */
public class BulkDeleteTokensSync {

    public static void main(String[] args) {
        try {
            // Step 1: Initialize credentials with the path to your service account key file
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            Credentials credentials = new Credentials();
            credentials.setPath(filePath);

            // Step 2: Configure the vault with required parameters
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId("<YOUR_VAULT_ID>");
            vaultConfig.setClusterId("<YOUR_CLUSTER_ID>");
            vaultConfig.setEnv(Env.PROD);
            vaultConfig.setCredentials(credentials);

            // Step 3: Create Skyflow client instance with error logging
            Skyflow skyflowClient = Skyflow.builder()
                    .setLogLevel(LogLevel.ERROR)
                    .addVaultConfig(vaultConfig)
                    .build();

            // Step 4: Prepare the list of tokens to delete
            ArrayList<String> tokens = new ArrayList<>();
            tokens.add("<YOUR_TOKEN_VALUE_1>");
            tokens.add("<YOUR_TOKEN_VALUE_2>");

            // Step 5: Build the delete tokens request
            DeleteTokensRequest deleteTokensRequest = DeleteTokensRequest.builder()
                    .tokens(tokens)
                    .build();

            // Step 6: Execute the bulk delete tokens operation and print the response
            DeleteTokensResponse deleteTokensResponse = skyflowClient.vault().bulkDeleteTokens(deleteTokensRequest);
            System.out.println(deleteTokensResponse);
        } catch (SkyflowException e) {
            // Step 7: Handle any errors that occur during the process
            System.err.println("Error in Skyflow operations: " + e.getMessage());
        }
    }
}
