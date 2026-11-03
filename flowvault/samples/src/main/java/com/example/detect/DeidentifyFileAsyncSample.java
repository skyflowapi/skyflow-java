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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * De-identifies several local files concurrently with polling enabled. Each task calls
 * deidentifyFile with pollOptions, so the SDK polls the run with exponential backoff and writes
 * the outputs to outputDirectory on success. The blocking poll runs on a pool thread, not on the
 * caller's thread, and the pool is bounded because every in-flight run parks one thread.
 */
public class DeidentifyFileAsyncSample {
    public static void main(String[] args) throws SkyflowException, InterruptedException {
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

        List<String> files = Arrays.asList("<PATH_TO_FILE_1>", "<PATH_TO_FILE_2>"); // Replace with the files to de-identify

        // One thread per concurrent run; size this to how many runs you want in flight at once.
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<DeidentifyFileResponse>> futures = new ArrayList<>();
        try {
            for (String file : files) {
                // submit() rather than execute(): the Future carries the response or the SkyflowException,
                // and future.cancel(true) interrupts the poll, which returns the last run the SDK saw.
                futures.add(pool.submit(() -> skyflowClient.detect().deidentifyFile(
                        DeidentifyFileRequest.builder()
                                .filePath(file)                           // BASE64 and the format are derived from the file
                                .configurationId("<CONFIGURATION_ID>")    // Replace with a Detect configuration id
                                .pollOptions(PollOptions.builder().waitTime(120).build())
                                .outputDirectory("<OUTPUT_DIRECTORY>")    // optional; defaults to the working directory
                                .build())));
            }

            // The main thread is free until here; block on each result only when it is needed.
            for (int i = 0; i < futures.size(); i++) {
                String file = files.get(i);
                try {
                    DeidentifyFileResponse response = futures.get(i).get();
                    System.out.println(file + ": run " + response.getRunId() + " " + response.getStatus());
                    if (response.getStatus() == DetectRunStatus.SUCCESS) {
                        // Outputs are already on disk as processed-<name>.<ext> and processed-entities-<name>.json
                        System.out.println("  outputs: " + response.getOutput().size() + ", metrics: " + response.getMetrics());
                    } else if (response.getStatus() == DetectRunStatus.FAILED) {
                        System.err.println("  run failed: " + response.getMessage());
                    } else {
                        // IN_PROGRESS or QUEUED: the budget ran out; resume with getRun(runId)
                        System.out.println("  still running; poll later with getRun(" + response.getRunId() + ")");
                    }
                } catch (ExecutionException e) {
                    // The task threw: unwrap the SkyflowException raised inside deidentifyFile.
                    Throwable cause = e.getCause();
                    if (cause instanceof SkyflowException) {
                        System.err.println(file + ": error during deidentifyFile: " + cause.getMessage());
                    } else {
                        System.err.println(file + ": unexpected error");
                        cause.printStackTrace();
                    }
                }
            }
        } finally {
            pool.shutdown();
            pool.awaitTermination(1, TimeUnit.MINUTES);
        }
    }
}
