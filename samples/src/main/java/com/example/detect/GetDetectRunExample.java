package com.example.detect;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.detect.DeidentifyFileResponse;
import com.skyflow.vault.detect.GetDetectRunRequest;

/**
 * Skyflow Get Detect Run Example
 * <p>
 * This example demonstrates how to:
 * 1. Configure credentials
 * 2. Set up vault configuration
 * 3. Create a get detect run request
 * 4. Call getDetectRun to poll for file processing results
 * 5. Handle response and errors
 */
public class GetDetectRunExample {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Configure credentials
        Credentials credentials = new Credentials();
        credentials.setPath("<YOUR_CREDENTIALS_FILE_PATH>"); // Replace with the path to the credentials file

        // Step 2: Configure the vault config
        VaultConfig vaultConfig = new VaultConfig();
        vaultConfig.setVaultId("<YOUR_VAULT_ID>");         // Replace with the ID of the vault
        vaultConfig.setClusterId("<YOUR_CLUSTER_ID>");     // Replace with the cluster ID of the vault
        vaultConfig.setEnv(Env.PROD);                       // Set the environment (e.g., DEV, STAGE, PROD)
        vaultConfig.setCredentials(credentials);           // Associate the credentials with the vault

        // Step 3: Create a Skyflow client
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)               // Enable debugging for detailed logs
                .addVaultConfig(vaultConfig)               // Add the vault configuration
                .build();

        try {
            // Step 4: Create a get detect run request
            GetDetectRunRequest getDetectRunRequest = GetDetectRunRequest.builder()
                    .runId("<RUN_ID_FROM_DEIDENTIFY_FILE>") // Replace with the runId from deidentifyFile call
                    .build();

            // Step 5: Call getDetectRun to poll for file processing results
            DeidentifyFileResponse deidentifyFileResponse = skyflowClient.detect(vaultConfig.getVaultId()).getDetectRun(getDetectRunRequest);
            System.out.println("Get Detect Run Response: " + deidentifyFileResponse);
        } catch (SkyflowException e) {
            System.err.println("Error occurred during get detect run: ");
            e.printStackTrace();
        }
    }
}