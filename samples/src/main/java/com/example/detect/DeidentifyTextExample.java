package com.example.detect;

import java.util.ArrayList;
import java.util.List;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.DetectEntities;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.detect.DeidentifyTextRequest;
import com.skyflow.vault.detect.DeidentifyTextResponse;
import com.skyflow.vault.detect.TokenFormat;

/**
 * Skyflow Deidentify Text Example
 * <p>
 * This example demonstrates how to use the Skyflow SDK to deidentify text data
 * across multiple vaults. It includes:
 * 1. Setting up credentials and vault configurations.
 * 2. Creating a Skyflow client with multiple vaults.
 * 3. Performing deidentify of text with various options.
 * 4. Handling responses and errors.
 */

public class DeidentifyTextExample {
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
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)               // Enable debugging for detailed logs
                .addVaultConfig(blitzConfig)               // Add the first vault configuration
                .addVaultConfig(stageConfig)               // Add the second vault configuration
                .addSkyflowCredentials(skyflowCredentials) // Add general Skyflow credentials
                .build();

        // Step 6: Configuring the different options for deidentify
        // List of entities to detect
        List<DetectEntities> detectEntitiesList = new ArrayList<>();
        detectEntitiesList.add(DetectEntities.SSN);
        detectEntitiesList.add(DetectEntities.CREDIT_CARD);

        // List of entities to detect with vault token
        List<DetectEntities> vaultTokenList = new ArrayList<>();
        vaultTokenList.add(DetectEntities.SSN);
        vaultTokenList.add(DetectEntities.CREDIT_CARD);

        // List<DetectEntities> entityOnlyList = new ArrayList<>();
        // entityOnlyList.add(DetectEntities.SSN);

        // List<DetectEntities> entityUniqueCounterList = new ArrayList<>();
        // entityUniqueCounterList.add(DetectEntities.SSN);

        // List<String> allowRegexList = new ArrayList<>();
        // allowRegexList.add("<YOUR_ALLOW_REGEX_LIST>");

        // List<String> restrictRegexList = new ArrayList<>();
        // restrictRegexList.add("YOUR_RESTRICT_REGEX_LIST");

        //  Configure Token Format 
        TokenFormat tokenFormat = TokenFormat.builder()
                .vaultToken(vaultTokenList)
                // .entityOnly(entityOnlyList)
                // .entityUniqueCounter(entityUniqueCounterList)
                .build();

        // Configure Transformation
        // List<DetectEntities> detectEntitiesTransformationList = new ArrayList<>();
        // detectEntitiesTransformationList.add(DetectEntities.DOB);
        // detectEntitiesTransformationList.add(DetectEntities.DATE);

        // DateTransformation dateTransformation = new DateTransformation(20, 5, detectEntitiesTransformationList);
        // Transformations transformations = new Transformations(dateTransformation);

        // Example 1: Deidentify text on the first vault
        try {
            // Create a deidentify text request for the first vault
            DeidentifyTextRequest deidentifyTextRequest = DeidentifyTextRequest.builder()
                    .text("My SSN is 123-45-6789 and my card is 4111 1111 1111 1111.")
                    .entities(detectEntitiesList)
                    //     .allowRegexList(allowRegexList)
                    //     .restrictRegexList(restrictRegexList)
                    .tokenFormat(tokenFormat)
                    // .transformations(transformations)
                    .build();


            DeidentifyTextResponse deidentifyTextResponse = skyflowClient.detect(blitzConfig.getVaultId()).deidentifyText(deidentifyTextRequest);

            System.out.println("Deidentify text Response (Vault1): " + deidentifyTextResponse);
        } catch (SkyflowException e) {
            System.err.println("Error occurred during deidentify (Vault1): ");
            e.printStackTrace();
        }
        // Example 2: Deidentify text on the second vault
        try {
            // Create a deidentify text request for the second vault
            DeidentifyTextRequest deidentifyTextRequest2 = DeidentifyTextRequest.builder()
                    .text("My SSN is 123-45-6789 and my card is 4111 1111 1111 1111.")
                    .entities(detectEntitiesList)
                    //     .allowRegexList(allowRegexList)
                    //     .restrictRegexList(restrictRegexList)
                    .tokenFormat(tokenFormat)
                    // .transformations(transformations)
                    .build();

            DeidentifyTextResponse deidentifyTextResponse2 = skyflowClient.detect(stageConfig.getVaultId()).deidentifyText(deidentifyTextRequest2);
            System.out.println("Deidentify text Response (Vault2): " + deidentifyTextResponse2);

        } catch (SkyflowException e) {
            System.err.println("Error occurred during deidentify (Vault2): ");
            e.printStackTrace();
        }
    }
}