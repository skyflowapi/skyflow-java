package com.example.detect;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.detect.DataSourceType;
import com.skyflow.detect.DetectRunStatus;
import com.skyflow.detect.FileDataFormat;
import com.skyflow.detect.FileOutput;
import com.skyflow.detect.RedactionLevel;
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
                            // Optional: render one token group with a named redaction pattern instead of its plain value.
                            .redactionLevel(Collections.singletonList(
                                    RedactionLevel.builder()
                                            .tokenGroupName("<TOKEN_GROUP_NAME>")     // Replace with a token group name
                                            .redactionPattern("<REDACTION_PATTERN>")  // Replace with a redaction pattern name
                                            .build()))
                            .build());

            System.out.println("Status: " + response.getStatus());
            if (response.getStatus() == DetectRunStatus.SUCCESS) {
                for (FileOutput output : response.getOutput()) {
                    String name = "reidentified-" + input.getName();
                    Files.write(new File(input.getParentFile(), name).toPath(),
                            Base64.getDecoder().decode(output.getProcessedFile()));
                    System.out.println("Wrote " + name + " (" + output.getProcessedFileType() + ")");
                }
                System.out.println("Metrics: " + response.getMetrics());
            }
        } catch (SkyflowException e) {
            System.err.println("Error during reidentifyFile:");
            e.printStackTrace();
        }
    }
}
