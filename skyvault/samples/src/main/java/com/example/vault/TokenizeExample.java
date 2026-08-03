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
 * This example demonstrates how to use the Skyflow SDK to tokenize data using a vault configuration.
 * It includes:
 * 1. Setting up a vault configuration.
 * 2. Creating a Skyflow client.
 * 3. Performing tokenization.
 */
public class TokenizeExample {
    public static void main(String[] args) {
        try {
            // Step 1: Set up credentials
            Credentials credentials = new Credentials();
            credentials.setPath("<YOUR_CREDENTIALS_FILE_PATH>"); // Replace with the path to the credentials file

            // Step 2: Configure the vault
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId("<YOUR_VAULT_ID>");        // Replace with the vault ID
            vaultConfig.setClusterId("<YOUR_CLUSTER_ID>");    // Replace with the cluster ID
            vaultConfig.setEnv(Env.DEV);                       // Set the environment (e.g., DEV, STAGE, PROD)
            vaultConfig.setCredentials(credentials);           // Associate credentials with the vault

            Credentials skyflowCredentials = new Credentials();
            skyflowCredentials.setCredentialsString("<YOUR_CREDENTIALS_STRING>"); // Replace with the actual credentials string

            // Step 3: Create a Skyflow client
            Skyflow skyflowClient = Skyflow.builder().setLogLevel(LogLevel.ERROR)  // Set log level
                    .addVaultConfig(vaultConfig)  // Add vault configuration
                    .addSkyflowCredentials(skyflowCredentials) // Add general Skyflow credentials
                    .build();

            // Step 4: Prepare data for tokenization
            ArrayList<ColumnValue> columnValues = new ArrayList<>();
            columnValues.add(ColumnValue.builder().value("<VALUE>")               // Replace with the actual value to tokenize
                    .columnGroup("<COLUMN_GROUP>") // Replace with the actual column group name
                    .build());
            columnValues.add(ColumnValue.builder().value("<VALUE>")               // Replace with another value to tokenize
                    .columnGroup("<COLUMN_GROUP>") // Replace with the column group name
                    .build());

            // Step 5: Build and execute the tokenization request
            TokenizeRequest tokenizeRequest = TokenizeRequest.builder().values(columnValues).build();

            TokenizeResponse tokenizeResponse = skyflowClient.vault().tokenize(tokenizeRequest);
            System.out.println("Tokenization Response: " + tokenizeResponse);
        } catch (SkyflowException e) {
            System.out.println("Error while tokenizing data for Vault:" + e);
        }
    }
}
