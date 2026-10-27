package com.example.detect;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.detect.DataSourceType;
import com.skyflow.detect.DetectRunStatus;
import com.skyflow.detect.EntityType;
import com.skyflow.detect.FileDataFormat;
import com.skyflow.detect.FileOutput;
import com.skyflow.detect.RedactionLevel;
import com.skyflow.detect.RedactionType;
import com.skyflow.detect.ReidentifyFileRequest;
import com.skyflow.detect.ReidentifyFileResponse;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;

/**
 * Replaces tokens in a de-identified file with their original values, or with a redacted form
 * chosen per entity type. Synchronous: the re-identified file comes back in the response.
 */
public class ReidentifyFileSample {
    public static void main(String[] args) throws Exception {
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

        byte[] content = Files.readAllBytes(Paths.get("<PATH_TO_DEIDENTIFIED_FILE>"));   // e.g. processed-notes.txt

        try {
            ReidentifyFileResponse response = skyflowClient.detect().reidentifyFile(
                    ReidentifyFileRequest.builder()
                            .dataSource(DataSourceType.BASE64)
                            .value(Base64.getEncoder().encodeToString(content))
                            .dataFormat(FileDataFormat.TXT)
                            .redactionLevel(Collections.singletonList(
                                    RedactionLevel.builder()
                                            .entityName(EntityType.NAME)
                                            .redactionType(RedactionType.PLAIN_TEXT)
                                            .build()))
                            .build());
            System.out.println("Status: " + response.getStatus());
            if (response.getStatus() == DetectRunStatus.SUCCESS) {
                for (FileOutput output : response.getOutput()) {
                    if (response.getOutputType() != DataSourceType.BASE64) {
                        // PRESIGNED_URL outputs carry a URL, not base64: download them instead of decoding.
                        System.out.println(output.getProcessedFileType() + " available at " + output.getProcessedFile());
                        continue;
                    }
                    byte[] processed = Base64.getDecoder().decode(output.getProcessedFile());
                    System.out.println(output.getProcessedFileType() + ": " + processed.length + " bytes");
                }
            }
        } catch (SkyflowException e) {
            System.err.println("Error during reidentifyFile:");
            e.printStackTrace();
        }
    }
}
