package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.DetokenizeResponse;

import java.util.ArrayList;

/**
 * This example demonstrates how to use the Skyflow SDK to detokenize sensitive data.
 * The steps include:
 * 1. Setting up Skyflow credentials.
 * 2. Configuring the vault.
 * 3. Creating a Skyflow client.
 * 4. Detokenizing tokens from specified vaults.
 */
public class DetokenizeExample {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Set up Skyflow credentials
        Credentials credentials = new Credentials();
        credentials.setToken("<YOUR_BEARER_TOKEN>"); // Replace with the actual bearer token

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
                .setLogLevel(LogLevel.ERROR)               // Set log level to ERROR to capture only critical logs
                .addVaultConfig(primaryVaultConfig)               // Add the first vault configuration
                .addSkyflowCredentials(skyflowCredentials) // Add general Skyflow credentials
                .build();

        // Step 5: Detokenize tokens from the first vault
        try {
            ArrayList<String> tokens = new ArrayList<>();
            tokens.add("<YOUR_TOKEN_VALUE_1>");            // Replace with the first token to detokenize
            tokens.add("<YOUR_TOKEN_VALUE_2>");            // Replace with the second token to detokenize

            DetokenizeRequest detokenizeRequest = DetokenizeRequest.builder()
                    .tokens(tokens)                       // Specify the tokens to detokenize
                    .continueOnError(true)                 // Continue processing even if an error occurs for some tokens
                    .build();

            DetokenizeResponse detokenizeResponse = skyflowClient.vault().detokenize(detokenizeRequest); // Perform detokenization
            System.out.println("Detokenize Response: " + detokenizeResponse);
        } catch (SkyflowException e) {
            System.out.println("Error during detokenization in Vault: " + e);
        }
    }
}
