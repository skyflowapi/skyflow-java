package com.example.detect;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.detect.DetectRunStatus;
import com.skyflow.detect.FileOutput;
import com.skyflow.detect.GetRunRequest;
import com.skyflow.detect.GetRunResponse;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

/**
 * Retrieves a Detect file run by id and, once it has succeeded, writes the base64 outputs to disk.
 */
public class GetRunSample {
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

        try {
            GetRunResponse run = skyflowClient.detect().getRun(
                    GetRunRequest.builder()
                            .runId("<RUN_ID>") // Replace with the run id returned by deidentifyFile
                            .build());

            System.out.println("Run " + run.getRunId() + " status: " + run.getStatus());
            if (run.getStatus() == DetectRunStatus.SUCCESS) {
                for (FileOutput output : run.getOutput()) {
                    String name = "processed-" + run.getRunId() + "-" + output.getProcessedFileType()
                            + "." + output.getProcessedFileExtension().name().toLowerCase();
                    Files.write(new File(name).toPath(), Base64.getDecoder().decode(output.getProcessedFile()));
                    System.out.println("Wrote " + name);
                }
                System.out.println("Metrics: " + run.getMetrics());
            } else if (run.getStatus() == DetectRunStatus.FAILED) {
                System.err.println("Run failed: " + run.getMessage());
            } else {
                System.out.println("Run still pending, poll again later.");
            }
        } catch (SkyflowException e) {
            System.err.println("Error during getRun:");
            e.printStackTrace();
        }
    }
}
