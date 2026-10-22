package com.example.detect;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.detect.DeidentifyFileRequest;
import com.skyflow.detect.DeidentifyFileResponse;
import com.skyflow.detect.DetectRunStatus;
import com.skyflow.detect.FileOutput;
import com.skyflow.detect.PollOptions;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;

/**
 * De-identifies a local file. The first call submits the run and returns its id; the second
 * submits and polls until the run completes, writing the outputs to disk on success.
 */
public class DeidentifyFileSample {
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

        // 1. Submit only: returns the run id, retrieve the result later with getRun.
        try {
            DeidentifyFileResponse submitted = skyflowClient.detect().deidentifyFile(
                    DeidentifyFileRequest.builder()
                            .filePath("<PATH_TO_FILE>")                   // BASE64 data source, format inferred from the extension
                            .configurationId("<CONFIGURATION_ID>")      // Replace with a Detect configuration id
                            .build());
            System.out.println("Run id: " + submitted.getRunId());    // getStatus() is null, nothing written to disk
        } catch (SkyflowException e) {
            System.err.println("Error during deidentifyFile (submit only):");
            e.printStackTrace();
        }

        // 2. Submit and poll: blocks the calling thread until the run is terminal or the budget runs out.
        try {
            DeidentifyFileResponse response = skyflowClient.detect().deidentifyFile(
                    DeidentifyFileRequest.builder()
                            .filePath("<PATH_TO_FILE>")
                            .configurationId("<CONFIGURATION_ID>")
                            .pollOptions(PollOptions.builder().waitTime(120).build())   // 1..300 s, default 60
                            .outputDirectory("<OUTPUT_DIRECTORY>")                      // optional, defaults to the working directory
                            .build());
            System.out.println("Run id: " + response.getRunId() + " status: " + response.getStatus());
            if (response.getStatus() == DetectRunStatus.SUCCESS) {
                // Outputs are already on disk as processed-<name>.<ext> and processed-entities-<name>.json
                for (FileOutput output : response.getOutput()) {
                    System.out.println(output.getProcessedFileType() + " -> " + output.getProcessedFileExtension());
                }
                System.out.println("Pages: " + response.getMetrics().getPages());
            } else if (response.getStatus() == DetectRunStatus.FAILED) {
                System.err.println("Run failed: " + response.getMessage());
            } else {
                System.out.println("Budget ran out, resume with getRun(" + response.getRunId() + ")");
            }
        } catch (SkyflowException e) {
            System.err.println("Error during deidentifyFile (submit and poll):");
            e.printStackTrace();
        }
    }
}
