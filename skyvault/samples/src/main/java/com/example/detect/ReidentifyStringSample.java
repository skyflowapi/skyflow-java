package com.example.detect;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.vault.detect.DeidentificationType;
import com.skyflow.vault.detect.DeidentifyStringRequest;
import com.skyflow.vault.detect.DeidentifyStringResponse;
import com.skyflow.vault.detect.Detect;
import com.skyflow.vault.detect.DetectConfiguration;
import com.skyflow.vault.detect.Entity;
import com.skyflow.vault.detect.EntityType;
import com.skyflow.vault.detect.RedactionLevel;
import com.skyflow.enums.RedactionType;
import com.skyflow.vault.detect.ReidentifyStringRequest;
import com.skyflow.vault.detect.ReidentifyStringResponse;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;

import java.util.Arrays;

/**
 * The de-identify / re-identify round trip with the Detect API. The text is first de-identified with
 * VAULT_TOKEN, which stores each value in a vault column and replaces it with that value's vault token
 * (for example [NAME_daa78453-9ca9-4ba5-a3ef-91934c11196c]); re-identification then looks the tokens up
 * again. Counter placeholders such as [NAME_1] are never stored and cannot be re-identified. For skyvault
 * vaults, redaction levels target an entity type and apply a redaction type.
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

        String text = "My name is John Doe and my email is johndoe@acme.com.";

        try {
            // 1. De-identify with VAULT_TOKEN: each value is stored in <TABLE>.<COLUMN> (a column with
            //    tokenization enabled) and replaced by its vault token.
            DeidentifyStringResponse deidentified = skyflowClient.detect().deidentifyString(
                    DeidentifyStringRequest.builder()
                            .text(text)
                            .configuration(DetectConfiguration.builder()
                                    .detect(Detect.builder()
                                            .entities(Arrays.asList(
                                                    vaultToken(EntityType.NAME, "<TABLE>.<COLUMN>"),            // Replace with table.column
                                                    vaultToken(EntityType.EMAIL_ADDRESS, "<TABLE>.<COLUMN>")))  // Replace with table.column
                                            .build())
                                    .build())
                            .build());
            String tokenized = deidentified.getProcessedText();
            System.out.println("De-identified text: " + tokenized);
            // My name is [NAME_daa78453-9ca9-4ba5-a3ef-91934c11196c] and my email is [EMAIL_ADDRESS_3f0c...].

            // 2. Plain re-identification: every vault token is replaced by its original value.
            ReidentifyStringResponse response = skyflowClient.detect().reidentifyString(
                    ReidentifyStringRequest.builder()
                            .text(tokenized)
                            .build());
            System.out.println("Re-identified text: " + response.getProcessedText());

            // 3. Targeted rendering: choose how each entity type is rendered.
            ReidentifyStringResponse rendered = skyflowClient.detect().reidentifyString(
                    ReidentifyStringRequest.builder()
                            .text(tokenized)
                            .redactionLevel(Arrays.asList(
                                    RedactionLevel.builder()
                                            .entityName(EntityType.NAME)                  // Entity type to target
                                            .redactionType(RedactionType.PLAIN_TEXT)      // PLAIN_TEXT, MASKED or REDACTED
                                            .build(),
                                    RedactionLevel.builder()
                                            .entityName(EntityType.EMAIL_ADDRESS)
                                            .redactionType(RedactionType.MASKED)
                                            .build()))
                            .build());
            System.out.println("Rendered text: " + rendered.getProcessedText());
            // My name is John Doe and my email is j*******@acme.com.
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
