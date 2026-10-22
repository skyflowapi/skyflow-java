package com.example.detect;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.detect.FileOutput;
import com.skyflow.detect.GetRunRequest;
import com.skyflow.detect.GetRunResponse;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;

/**
 * Retrieves the status and, once complete, the outputs of a file de-identification run submitted
 * with deidentifyFile.
 */
public class GetRunSample {
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
            GetRunResponse run = skyflowClient.detect().getRun(
                    GetRunRequest.builder().runId("<RUN_ID>").build());   // Replace with the run id from deidentifyFile
            System.out.println("Status: " + run.getStatus());
            switch (run.getStatus()) {
                case SUCCESS:
                    for (FileOutput output : run.getOutput()) {
                        // output.getProcessedFile() is base64 or a presigned URL, per run.getOutputType()
                        System.out.println(output.getProcessedFileType() + " -> " + output.getProcessedFileExtension());
                    }
                    break;
                case FAILED:
                    System.err.println("Run failed: " + run.getMessage());
                    break;
                default:    // QUEUED, IN_PROGRESS, UNKNOWN: poll again later
                    break;
            }
        } catch (SkyflowException e) {
            System.err.println("Error during getRun:");
            e.printStackTrace();
        }
    }
}
