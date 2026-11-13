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
 *
 * Demonstrates how to use the Skyflow SDK to reidentify (restore) sensitive values
 * in a previously deidentified text string. This is the reverse of deidentifyText.
 *
 * Steps:
 * 1. Configure credentials and vault.
 * 2. Create a Skyflow client.
 * 3. Build a ReidentifyTextRequest with the deidentified text and entity-display options.
 * 4. Call reidentifyText and handle the response.
 */
public class ReidentifyTextExample {

    public static void main(String[] args) throws SkyflowException {

        // Step 1: Set up credentials
        Credentials credentials = new Credentials();
        credentials.setPath("<YOUR_CREDENTIALS_FILE_PATH>"); // Replace with path to your credentials JSON file
        // Alternative authentication options (uncomment one if needed):
        // credentials.setApiKey("<YOUR_API_KEY>");
        // credentials.setToken("<YOUR_BEARER_TOKEN>");
        // credentials.setCredentialsString("<YOUR_CREDENTIALS_STRING>");

        // Step 2: Configure the vault
        VaultConfig vaultConfig = new VaultConfig();
        vaultConfig.setVaultId("<YOUR_VAULT_ID>");     // Replace with your vault ID
        vaultConfig.setClusterId("<YOUR_CLUSTER_ID>"); // Replace with your cluster ID (from vault URL)
        vaultConfig.setEnv(Env.PROD);                  // Environment: PROD, SANDBOX, STAGE, or DEV
        vaultConfig.setCredentials(credentials);       // Attach credentials to this vault

        // Step 3: Create a Skyflow client
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.ERROR)     // Log level: DEBUG, INFO, WARN, ERROR, or OFF
                .addVaultConfig(vaultConfig)     // Add vault configuration
                .build();

        try {
            // Step 4: Configure entity display options for the reidentified output.
            // Entities NOT listed in any option list are reidentified to plain text by default.

            // PLAIN TEXT: Return the original sensitive value for these entities
            List<DetectEntities> plainTextEntities = new ArrayList<>();
            plainTextEntities.add(DetectEntities.SSN); // Replace with entities you want in plain text

            // MASKED: Return a masked version of the value (e.g., XXX-XX-6789)
            List<DetectEntities> maskedEntities = new ArrayList<>();
            maskedEntities.add(DetectEntities.CREDIT_CARD); // Replace with entities you want masked

            // REDACTED: Replace entity value with a redaction placeholder
            // List<DetectEntities> redactedEntities = new ArrayList<>();
            // redactedEntities.add(DetectEntities.DOB); // Replace with entities you want redacted

            // Step 5: Build the ReidentifyTextRequest
            // The `text` field should be the output of a prior deidentifyText call —
            // it contains placeholder tokens like [SSN_IWdexZe] that will be resolved.
            ReidentifyTextRequest reidentifyTextRequest = ReidentifyTextRequest.builder()
                    .text("My SSN is [SSN_IWdexZe] and my card is [CREDIT_CARD_rUzMjdQ].") // Replace with your deidentified text
                    .plainTextEntities(plainTextEntities) // Restore these as plain text
                    .maskedEntities(maskedEntities)       // Restore these as masked values
                    // .redactedEntities(redactedEntities) // Restore these as redacted placeholders
                    .build();

            // Step 6: Call reidentifyText on the vault
            ReidentifyTextResponse reidentifyTextResponse = skyflowClient
                    .detect("<YOUR_VAULT_ID>") // Replace with your vault ID
                    .reidentifyText(reidentifyTextRequest);

            // Step 7: Print the processed text
            System.out.println("Reidentify Text Response: " + reidentifyTextResponse);

            // To access the processed text string directly:
            // System.out.println("Processed text: " + reidentifyTextResponse.getProcessedText());

        } catch (SkyflowException e) {
            System.err.println("Error occurred during reidentify text:");
            e.printStackTrace();
        }
    }
}
