package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.DetokenizeResponse;

import java.util.ArrayList;

/**
 * This example demonstrates how to use the Skyflow SDK to detokenize sensitive data.
 * The steps include:
 * 1. Setting up vault configurations.
 * 2. Creating a Skyflow client.
 * 3. Detokenizing tokens from specified vaults.
 */
public class DetokenizeExample {
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

        // Example 1: Detokenize tokens from the first vault
        try {
            ArrayList<String> tokens1 = new ArrayList<>();
            tokens1.add("<YOUR_TOKEN_VALUE_1>");            // Replace with the first token to detokenize
            tokens1.add("<YOUR_TOKEN_VALUE_2>");            // Replace with the second token to detokenize

            DetokenizeRequest detokenizeRequest1 = DetokenizeRequest.builder()
                    .tokens(tokens1)                       // Specify the tokens to detokenize
                    .continueOnError(true)                 // Continue processing even if an error occurs for some tokens
                    .build();

            DetokenizeResponse detokenizeResponse1 = skyflowClient.vault().detokenize(detokenizeRequest1); // Perform detokenization
            System.out.println("Detokenize Response (Vault 1): " + detokenizeResponse1);
        } catch (SkyflowException e) {
            System.out.println("Error during detokenization in Vault 1:");
            e.printStackTrace();
        }

        // Example 2: Detokenize tokens from the second vault
        try {
            ArrayList<String> tokens2 = new ArrayList<>();
            tokens2.add("<YOUR_TOKEN_VALUE_1>");            // Replace with the first token to detokenize
            tokens2.add("<YOUR_TOKEN_VALUE_2>");            // Replace with the second token to detokenize

            DetokenizeRequest detokenizeRequest2 = DetokenizeRequest.builder()
                    .tokens(tokens2)                       // Specify the tokens to detokenize
                    .continueOnError(false)                // Stop processing on the first error
                    .redactionType(RedactionType.DEFAULT)  // Use the default redaction type for detokenization
                    .build();

            DetokenizeResponse detokenizeResponse2 = skyflowClient.vault("<YOUR_VAULT_ID_2>").detokenize(detokenizeRequest2); // Perform detokenization
            System.out.println("Detokenize Response (Vault 2): " + detokenizeResponse2);
        } catch (SkyflowException e) {
            System.out.println("Error during detokenization in Vault 2:");
            e.printStackTrace();
        }
    }
}
