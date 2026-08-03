package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.tokens.DetokenizeData;
import com.skyflow.vault.tokens.DetokenizeRecordResponse;
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

        // Step 2: Configure the vault
        VaultConfig vaultConfig = new VaultConfig();
        vaultConfig.setVaultId("<YOUR_VAULT_ID_1>");         // Replace with the ID of the first vault
        vaultConfig.setClusterId("<YOUR_CLUSTER_ID_1>");     // Replace with the cluster ID of the first vault
        vaultConfig.setEnv(Env.PROD);                        // Set the environment (e.g., DEV, STAGE, PROD)
        vaultConfig.setCredentials(credentials);            // Associate the credentials with the vault

        // Step 3: Set up credentials for the Skyflow client
        Credentials skyflowCredentials = new Credentials();
        skyflowCredentials.setCredentialsString("<YOUR_CREDENTIALS_STRING>"); // Replace with credentials string

        // Step 4: Create a Skyflow client and add vault configurations
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.ERROR)               // Set log level to ERROR to capture only critical logs
                .addVaultConfig(vaultConfig)               // Add the vault configuration
                .addSkyflowCredentials(skyflowCredentials) // Add general Skyflow credentials
                .build();

        // Step 5: Detokenize tokens from the vault
        try {
            ArrayList<DetokenizeData> detokenizeData1 = new ArrayList<>();

            // DetokenizeData accepts a token and an optional RedactionType:
            //   RedactionType.PLAIN_TEXT — return the full original value (default)
            //   RedactionType.MASKED     — return a partially masked value
            //   RedactionType.REDACTED   — return a fully redacted placeholder
            //   RedactionType.DEFAULT    — use the vault-configured default redaction
            DetokenizeData detokenizeDataRecord1 = new DetokenizeData("<YOUR_TOKEN_VALUE_1>", RedactionType.MASKED);      // MASKED redaction
            DetokenizeData detokenizeDataRecord2 = new DetokenizeData("<YOUR_TOKEN_VALUE_2>", RedactionType.PLAIN_TEXT);  // PLAIN_TEXT redaction
            // DetokenizeData detokenizeDataRecord3 = new DetokenizeData("<YOUR_TOKEN_VALUE_3>", RedactionType.REDACTED); // REDACTED
            // DetokenizeData detokenizeDataRecord4 = new DetokenizeData("<YOUR_TOKEN_VALUE_4>", RedactionType.DEFAULT);  // vault default
            detokenizeData1.add(detokenizeDataRecord1);
            detokenizeData1.add(detokenizeDataRecord2);

            DetokenizeRequest detokenizeRequest1 = DetokenizeRequest.builder()
                    .detokenizeData(detokenizeData1) // Tokens to detokenize with their redaction types
                    .continueOnError(true)           // true: return partial results on error; false: fail on first error
                    .build();

            DetokenizeResponse detokenizeResponse1 = skyflowClient.vault().detokenize(detokenizeRequest1);

            // Option A: print the full response object
            System.out.println("Detokenize Response: " + detokenizeResponse1);

            // Option B: iterate DetokenizeRecordResponse to access individual fields
            // for (DetokenizeRecordResponse record : detokenizeResponse1.getDetokenizedFields()) {
            //     System.out.println("Token     : " + record.getToken());
            //     System.out.println("Value     : " + record.getValue());
            //     System.out.println("Type      : " + record.getType());      // e.g. "STRING"
            //     System.out.println("Request ID: " + record.getRequestId());
            // }
            // for (DetokenizeRecordResponse err : detokenizeResponse1.getErrors()) {
            //     System.out.println("Failed token: " + err.getToken());
            //     System.out.println("Error       : " + err.getError());
            // }

        } catch (SkyflowException e) {
            System.out.println("Error during detokenization:");
            e.printStackTrace();
        }
    }
}
