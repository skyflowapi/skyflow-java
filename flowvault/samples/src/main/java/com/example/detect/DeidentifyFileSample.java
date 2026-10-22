package com.example.detect;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.detect.DeidentifyFileRequest;
import com.skyflow.detect.DeidentifyFileResponse;
import com.skyflow.detect.DetectRunStatus;
import com.skyflow.detect.PollOptions;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;

/**
 * De-identifies a local file with the Detect V2 API. The first call submits the run and returns its
 * id; the second submits and polls, and writes the outputs next to the input on success.
 */
public class DeidentifyFileSample {
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
            // Submit only: the API's own behaviour. Keep the run id and retrieve the result with getRun.
            DeidentifyFileResponse submitted = skyflowClient.detect().deidentifyFile(
                    DeidentifyFileRequest.builder()
                            .filePath("<PATH_TO_FILE>")             // BASE64 and the format are derived from the file
                            .configurationId("<CONFIGURATION_ID>")  // Replace with a Detect configuration id
                            .build());
            System.out.println("Submitted run " + submitted.getRunId());

            // Submit and poll: waits up to 90 seconds with exponential backoff, then writes
            // processed-<name>.<ext> and processed-<name>.json into the output directory.
            DeidentifyFileResponse polled = skyflowClient.detect().deidentifyFile(
                    DeidentifyFileRequest.builder()
                            .filePath("<PATH_TO_FILE>")
                            .configurationId("<CONFIGURATION_ID>")
                            .pollOptions(PollOptions.builder().waitTime(90).build())
                            .outputDirectory("<OUTPUT_DIRECTORY>")   // optional; defaults to the working directory
                            .build());

            System.out.println("Run " + polled.getRunId() + " status: " + polled.getStatus());
            if (polled.getStatus() == DetectRunStatus.SUCCESS) {
                System.out.println("Outputs: " + polled.getOutput().size() + ", metrics: " + polled.getMetrics());
            } else if (polled.getStatus() == DetectRunStatus.FAILED) {
                System.err.println("Run failed: " + polled.getMessage());
            } else {
                System.out.println("Still running; poll later with getRun(" + polled.getRunId() + ")");
            }
        } catch (SkyflowException e) {
            System.err.println("Error during deidentifyFile:");
            e.printStackTrace();
        }
    }
}
