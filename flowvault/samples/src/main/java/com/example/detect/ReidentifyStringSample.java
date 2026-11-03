package com.example.detect;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.detect.RedactionLevel;
import com.skyflow.detect.ReidentifyStringRequest;
import com.skyflow.detect.ReidentifyStringResponse;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;

import java.util.Collections;

/**
 * Re-identifies tokens in a string with the Detect V2 API. For flowvault vaults, redaction
 * levels target a token group and apply a named redaction pattern.
 */
public class ReidentifyStringSample {
    public static void main(String[] args) throws SkyflowException {
        Credentials credentials = new Credentials();
        credentials.setPath("<YOUR_CREDENTIALS_FILE_PATH>"); // Replace with the path to the credentials file

        VaultConfig vaultConfig = new VaultConfig();
        vaultConfig.setVaultId("<YOUR_VAULT_ID>");         // Replace with the ID of the vault
        vaultConfig.setClusterId("<YOUR_CLUSTER_ID>");     // Replace with the cluster ID of the vault
        vaultConfig.setEnv(Env.PROD);                      // Set the environment (e.g., DEV, STAGE, PROD)

        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.ERROR)
                .addVaultConfig(vaultConfig)
                .addSkyflowCredentials(credentials)
                .build();

        try {
            // Plain re-identification: every token is replaced by its original value.
            ReidentifyStringResponse response = skyflowClient.detect().reidentifyString(
                    ReidentifyStringRequest.builder()
                            .text("My name is [NAME_1] and my email is [EMAIL_ADDRESS_1].")
                            .build());
            System.out.println("Processed text: " + response.getProcessedText());

            // Targeted rendering: tokens from one token group are rendered with a named pattern.
            ReidentifyStringResponse masked = skyflowClient.detect().reidentifyString(
                    ReidentifyStringRequest.builder()
                            .text("My name is [NAME_1].")
                            .redactionLevel(Collections.singletonList(
                                    RedactionLevel.builder()
                                            .tokenGroupName("<TOKEN_GROUP_NAME>")     // Replace with a token group name
                                            .redactionPattern("<REDACTION_PATTERN>")  // Replace with a redaction pattern name
                                            .build()))
                            .build());
            System.out.println("Masked text: " + masked.getProcessedText());
        } catch (SkyflowException e) {
            System.err.println("Error during reidentifyString:");
            e.printStackTrace();
        }
    }
}
