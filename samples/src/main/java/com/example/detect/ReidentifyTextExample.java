package com.example.detect;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.DetectEntities;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.detect.ReidentifyTextRequest;
import com.skyflow.vault.detect.ReidentifyTextResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Skyflow Reidentify Text Example
 * <p>
 * This example demonstrates how to use the Skyflow SDK to reidentify text data
 * across multiple vaults. It includes:
 * 1. Setting up credentials and vault configurations.
 * 2. Creating a Skyflow client with multiple vaults.
 * 3. Performing reidentify of text with various options.
 * 4. Handling responses and errors.
 */

public class ReidentifyTextExample {
    public static void main(String[] args) throws SkyflowException {

        // Step 1: Set up credentials for the first vault configuration
        Credentials credentials = new Credentials();
        credentials.setPath("<YOUR_CREDENTIALS_FILE_PATH_1>"); // Replace with the path to the credentials file

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
        Skyflow skyflowClient = Skyflow.builder().setLogLevel(LogLevel.DEBUG)               // Enable debugging for detailed logs
                .addVaultConfig(blitzConfig)               // Add the first vault configuration
                .addVaultConfig(stageConfig)               // Add the second vault configuration
                .addSkyflowCredentials(skyflowCredentials) // Add general Skyflow credentials
                .build();

        // Step 6: Configuring the different options for reidentify
        List<DetectEntities> maskedEntity = new ArrayList<>();
        maskedEntity.add(DetectEntities.CREDIT_CARD); // Replace with the entity you want to mask

        List<DetectEntities> plainTextEntity = new ArrayList<>();
        plainTextEntity.add(DetectEntities.SSN); // Replace with the entity you want to keep in plain text

        // List<DetectEntities> redactedEntity = new ArrayList<>();
        // redactedEntity.add("<YOUR_ENTITY_1>"); // Replace with the entity you want to redact

        // Example 2: Reidentify text on the first vault
        try {
            // Step 7: Create a reidentify text request with the configured entities
            ReidentifyTextRequest reidentifyTextRequest = ReidentifyTextRequest.builder()
                    .text("My SSN is [SSN_IWdexZe] and my card is [CREDIT_CARD_rUzMjdQ].") // Replace with your deidentify text
                    .maskedEntities(maskedEntity)
                    // .redactedEntities(redactedEntity)
                    // .plainTextEntities(plainTextEntity)
                    .build();

            // Handle the response from the reidentify text request
            ReidentifyTextResponse reidentifyTextResponse = skyflowClient.detect(blitzConfig.getVaultId()).reidentifyText(reidentifyTextRequest);
            System.out.println("Reidentify text Response (Vault1): " + reidentifyTextResponse);
        } catch (SkyflowException e) {
            System.err.println("Error occurred during reidentify (Vault1): ");
            e.printStackTrace();
        }

        // Example 2: Reidentify text on the second vault
        try {
            // Step 7: Create a reidentify text request with the configured entities
            ReidentifyTextRequest reidentifyTextRequest = ReidentifyTextRequest.builder()
                    .text("My SSN is [SSN_IWdexZe] and my card is [CREDIT_CARD_rUzMjdQ].") // Replace with your deidentify text
                    // .maskedEntities(maskedEntity)
                    // .redactedEntities(redactedEntity)
                    .plainTextEntities(plainTextEntity)
                    .build();

            // Handle the response from the reidentify text request
            ReidentifyTextResponse reidentifyTextResponse2 = skyflowClient.detect(stageConfig.getVaultId()).reidentifyText(reidentifyTextRequest);
            System.out.println("Reidentify text Response (Vault2): " + reidentifyTextResponse2);
        } catch (SkyflowException e) {
            System.err.println("Error occurred during reidentify (Vault2): ");
            e.printStackTrace();
        }
    }
}