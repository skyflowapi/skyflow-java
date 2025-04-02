package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.tokens.DetokenizeData;
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
            ArrayList<DetokenizeData> detokenizeData1 = new ArrayList<>();
            DetokenizeData detokenizeDataRecord1 = new DetokenizeData("<YOUR_TOKEN_VALUE_1>", RedactionType.MASKED);   // Replace with a token to detokenize with MASKED redaction
            DetokenizeData detokenizeDataRecord2 = new DetokenizeData("<YOUR_TOKEN_VALUE_2>"); // Replace with another token to detokenize with PLAIN_TEXT redaction
            detokenizeData1.add(detokenizeDataRecord1);
            detokenizeData1.add(detokenizeDataRecord2);

            DetokenizeRequest detokenizeRequest1 = DetokenizeRequest.builder()
                    .detokenizeData(detokenizeData1)     // Specify the tokens to detokenize with specified redaction types
                    .continueOnError(true)              // Continue processing even if an error occurs for some tokens
                    .build();

            DetokenizeResponse detokenizeResponse1 = skyflowClient.vault().detokenize(detokenizeRequest1); // Perform detokenization
            System.out.println("Detokenize Response (Vault 1): " + detokenizeResponse1);
        } catch (SkyflowException e) {
            System.out.println("Error during detokenization in Vault 1:");
            e.printStackTrace();
        }

        // Example 2: Detokenize tokens from the second vault
        try {
            ArrayList<DetokenizeData> detokenizeData2 = new ArrayList<>();
            DetokenizeData detokenizeDataRecord3 = new DetokenizeData("<YOUR_TOKEN_VALUE_3>", RedactionType.DEFAULT);   // Replace with a token to detokenize
            DetokenizeData detokenizeDataRecord4 = new DetokenizeData("<YOUR_TOKEN_VALUE_4>"); // Replace with another token to detokenize
            detokenizeData2.add(detokenizeDataRecord3);
            detokenizeData2.add(detokenizeDataRecord4);

            DetokenizeRequest detokenizeRequest2 = DetokenizeRequest.builder()
                    .detokenizeData(detokenizeData2)        // Specify the tokens to detokenize with specified redaction types
                    .continueOnError(false)                 // Stop processing on the first error
                    .downloadURL(true)                      // Specify whether to return URLs for file data type
                    .build();

            DetokenizeResponse detokenizeResponse2 = skyflowClient.vault("<YOUR_VAULT_ID_2>").detokenize(detokenizeRequest2); // Perform detokenization
            System.out.println("Detokenize Response (Vault 2): " + detokenizeResponse2);
        } catch (SkyflowException e) {
            System.out.println("Error during detokenization in Vault: " + e);
        }
    }
}
