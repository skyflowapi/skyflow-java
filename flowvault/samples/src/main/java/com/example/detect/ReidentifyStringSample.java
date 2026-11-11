package com.example.detect;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.detect.DeidentificationType;
import com.skyflow.detect.DeidentifyStringRequest;
import com.skyflow.detect.DeidentifyStringResponse;
import com.skyflow.detect.Detect;
import com.skyflow.detect.DetectConfiguration;
import com.skyflow.detect.Entity;
import com.skyflow.detect.EntityType;
import com.skyflow.detect.RedactionLevel;
import com.skyflow.detect.ReidentifyStringRequest;
import com.skyflow.detect.ReidentifyStringResponse;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;

import java.util.Arrays;
import java.util.Collections;

/**
 * The de-identify / re-identify round trip with the Detect API on the FlowVault_AllEntities_IdentityRecon
 * vault (DEV). The text is first de-identified with VAULT_TOKEN, which stores each value in its token group
 * and replaces it with a vault token such as [NAME_QUo9AjT]; re-identification then looks those tokens up
 * again. Counter placeholders such as [NAME_1] are never stored and cannot be re-identified.
 */
public class ReidentifyStringSample {
    public static void main(String[] args) throws SkyflowException {
        String text = "My name is John Doe, my email is johndoe@acme.com and my phone is 415-555-0199.";

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
            // 1. De-identify with VAULT_TOKEN. On this vault every entity type has a table, a column and a
            //    token group of its own name, so a destination is "<ENTITY>.<ENTITY>".
            DeidentifyStringResponse deidentified = skyflowClient.detect().deidentifyString(
                    DeidentifyStringRequest.builder()
                            .text(text)
                            .configuration(DetectConfiguration.builder()
                                    .detect(Detect.builder()
                                            .entities(Arrays.asList(
                                                    vaultToken(EntityType.NAME, "NAME.NAME"),
                                                    vaultToken(EntityType.EMAIL_ADDRESS, "EMAIL_ADDRESS.EMAIL_ADDRESS"),
                                                    vaultToken(EntityType.PHONE_NUMBER, "PHONE_NUMBER.PHONE_NUMBER")))
                                            .build())
                                    .build())
                            .build());
            String tokenized = deidentified.getProcessedText();
            System.out.println("De-identified text: " + tokenized);
            // My name is [NAME_QUo9AjT], my email is [EMAIL_ADDRESS_NoHNc2i] and my phone is [PHONE_NUMBER_FjBfw88].

            // 2. Plain re-identification: every vault token is replaced by its original value.
            ReidentifyStringResponse response = skyflowClient.detect().reidentifyString(
                    ReidentifyStringRequest.builder()
                            .text(tokenized)
                            .build());
            System.out.println("Re-identified text: " + response.getProcessedText());
            System.out.println("Metrics: " + response.getMetrics());

            // 3. Targeted rendering: tokens of one token group rendered with a named redaction pattern.
            //    The FlowVault_AllEntities_IdentityRecon schema defines no redaction patterns yet, so this
            //    stays commented until one exists in the vault.
//            ReidentifyStringResponse masked = skyflowClient.detect().reidentifyString(
//                    ReidentifyStringRequest.builder()
//                            .text(tokenized)
//                            .redactionLevel(Collections.singletonList(
//                                    RedactionLevel.builder()
//                                            .tokenGroupName("NAME")                   // token group of the NAME table
//                                            .redactionPattern("<REDACTION_PATTERN>")  // a pattern configured in the vault
//                                            .build()))
//                            .build());
//            System.out.println("Masked text: " + masked.getProcessedText());
        } catch (SkyflowException e) {
            // Quote the request id when contacting Skyflow support.
            System.err.println("Error during the re-identify round trip: [" + e.getHttpCode() + "] " + e.getMessage()
                    + " (requestId=" + e.getRequestId() + ")");
        }
    }

    private static Entity vaultToken(EntityType entityType, String destination) {
        return Entity.builder()
                .entityType(entityType)
                .deidentificationType(DeidentificationType.VAULT_TOKEN)
                .destination(destination)
                .build();
    }
}
