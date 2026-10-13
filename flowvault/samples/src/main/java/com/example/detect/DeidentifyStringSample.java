package com.example.detect;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.detect.DeidentificationType;
import com.skyflow.detect.DeidentifyStringRequest;
import com.skyflow.detect.DeidentifyStringResponse;
import com.skyflow.detect.Detect;
import com.skyflow.detect.DetectConfiguration;
import com.skyflow.detect.DetectedEntity;
import com.skyflow.detect.Entity;
import com.skyflow.detect.EntityType;
import com.skyflow.detect.ReturnEntitiesType;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;

import java.util.Arrays;
import java.util.Collections;

/**
 * De-identifies sensitive data in a string with the Detect V2 API, first with a stored
 * configuration id and then with an inline configuration.
 */
public class DeidentifyStringSample {
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

        String text = "My name is John Doe, and my email is johndoe@acme.com.";

        // 1. Stored configuration, created in Studio or through the management API.
        try {
            DeidentifyStringResponse response = skyflowClient.detect().deidentifyString(
                    DeidentifyStringRequest.builder()
                            .text(text)
                            .configurationId("<CONFIGURATION_ID>") // Replace with a Detect configuration id
                            .build());
            System.out.println("Processed text: " + response.getProcessedText());
        } catch (SkyflowException e) {
            System.err.println("Error during deidentifyString (stored configuration):");
            e.printStackTrace();
        }

        // 2. Inline configuration, nothing to create beforehand.
        DetectConfiguration configuration = DetectConfiguration.builder()
                .detect(Detect.builder()
                        .entities(Arrays.asList(
                                Entity.builder()
                                        .entityType(EntityType.NAME)
                                        .deidentificationType(DeidentificationType.VAULT_TOKEN)
                                        .destination("<TOKEN_GROUP_NAME>") // flowvault: token group that stores the value
                                        .build(),
                                Entity.builder()
                                        .entityType(EntityType.ALL)
                                        .deidentificationType(DeidentificationType.ENTITY_UNIQUE_COUNTER)
                                        .build()))
                        .skip(Collections.singletonList("Skyflow"))
                        .returnEntities(ReturnEntitiesType.EXCLUDE_SENSITIVE_DATA)
                        .build())
                .build();

        try {
            DeidentifyStringResponse response = skyflowClient.detect().deidentifyString(
                    DeidentifyStringRequest.builder()
                            .text(text)
                            .configuration(configuration)
                            .build());
            System.out.println("Processed text: " + response.getProcessedText());
            for (DetectedEntity entity : response.getEntities()) {
                System.out.printf("%-16s %-18s [%d,%d] %s%n",
                        entity.getEntityType(), entity.getToken(),
                        entity.getLocation().getStartIndex(), entity.getLocation().getEndIndex(),
                        entity.getEntityScores());
            }
            System.out.println("Metrics: " + response.getMetrics());
        } catch (SkyflowException e) {
            System.err.println("Error during deidentifyString (inline configuration):");
            e.printStackTrace();
        }
    }
}
