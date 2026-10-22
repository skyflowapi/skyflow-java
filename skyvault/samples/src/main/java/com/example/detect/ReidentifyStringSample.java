package com.example.detect;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.detect.EntityType;
import com.skyflow.detect.RedactionLevel;
import com.skyflow.detect.RedactionType;
import com.skyflow.detect.ReidentifyStringRequest;
import com.skyflow.detect.ReidentifyStringResponse;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;

import java.util.Arrays;

/**
 * Replaces tokens in a de-identified string with their original values, or with a redacted form
 * chosen per entity type. For a skyvault vault each RedactionLevel names an entityName and a
 * redactionType.
 */
public class ReidentifyStringSample {
    public static void main(String[] args) throws SkyflowException {
        Credentials credentials = new Credentials();
        credentials.setPath("<YOUR_CREDENTIALS_FILE_PATH>");   // Replace with the path to the credentials file

        VaultConfig vaultConfig = new VaultConfig();
        vaultConfig.setVaultId("<YOUR_VAULT_ID>");              // Replace with the ID of the vault
        vaultConfig.setClusterId("<YOUR_CLUSTER_ID>");          // Replace with the cluster ID of the vault
        vaultConfig.setEnv(Env.PROD);                           // Set the environment (e.g., DEV, STAGE, PROD)

        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.ERROR)
                .addVaultConfig(vaultConfig)
                .addSkyflowCredentials(credentials)
                .build();

        try {
            ReidentifyStringResponse response = skyflowClient.detect().reidentifyString(
                    ReidentifyStringRequest.builder()
                            .text("My name is [NAME_1] and my email is [EMAIL_ADDRESS_1].")
                            .redactionLevel(Arrays.asList(
                                    RedactionLevel.builder()
                                            .entityName(EntityType.NAME)
                                            .redactionType(RedactionType.PLAIN_TEXT)
                                            .build(),
                                    RedactionLevel.builder()
                                            .entityName(EntityType.EMAIL_ADDRESS)
                                            .redactionType(RedactionType.MASKED)
                                            .build()))
                            .build());
            System.out.println("Processed text: " + response.getProcessedText());
        } catch (SkyflowException e) {
            System.err.println("Error during reidentifyString:");
            e.printStackTrace();
        }
    }
}
