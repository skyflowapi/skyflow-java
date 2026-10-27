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
import com.skyflow.enums.RedactionType;
import com.skyflow.detect.ReidentifyFileRequest;
import com.skyflow.detect.ReidentifyFileResponse;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Collections;

/**
 * Re-identifies the tokens in a text file with the Detect V2 API. The file is sent as base64 and
 * the re-identified file is returned synchronously and written next to the input.
 */
public class ReidentifyFileSample {
    public static void main(String[] args) throws Exception {
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

        File input = new File("<PATH_TO_TOKENISED_FILE>"); // Replace with a file produced by deidentification
        String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(input.toPath()));

        try {
            ReidentifyFileResponse response = skyflowClient.detect().reidentifyFile(
                    ReidentifyFileRequest.builder()
                            .dataSource(DataSourceType.BASE64)
                            .value(base64)
                            .dataFormat(FileDataFormat.TXT)
                            // Optional: render one entity type with a redaction type instead of its plain value.
                            .redactionLevel(Collections.singletonList(
                                    RedactionLevel.builder()
                                            .entityName(EntityType.NAME)              // Entity type to target
                                            .redactionType(RedactionType.PLAIN_TEXT)  // PLAIN_TEXT, MASKED, REDACTED or DEFAULT
                                            .build()))
                            .build());

            System.out.println("Status: " + response.getStatus());
            if (response.getStatus() == DetectRunStatus.SUCCESS) {
                int index = 0;
                for (FileOutput output : response.getOutput()) {
                    index++;
                    if (response.getOutputType() != DataSourceType.BASE64) {
                        // PRESIGNED_URL outputs carry a URL, not base64: download them instead of decoding.
                        System.out.println(output.getProcessedFileType() + " available at " + output.getProcessedFile());
                        continue;
                    }
                    // One file per output: the output type keeps the names apart, the index guards against
                    // two outputs of the same type (the SDK's own writer follows the same pattern).
                    String type = output.getProcessedFileType() == null ? String.valueOf(index) : output.getProcessedFileType();
                    String name = "reidentified-" + type + "-" + input.getName();
                    File target = new File(input.getParentFile(), name);
                    if (target.exists()) {
                        name = "reidentified-" + type + "-" + index + "-" + input.getName();
                        target = new File(input.getParentFile(), name);
                    }
                    Files.write(target.toPath(), Base64.getDecoder().decode(output.getProcessedFile()));
                    System.out.println("Wrote " + name);
                }
                System.out.println("Metrics: " + response.getMetrics());
            }
        } catch (SkyflowException e) {
            System.err.println("Error during reidentifyFile:");
            e.printStackTrace();
        }
    }
}
