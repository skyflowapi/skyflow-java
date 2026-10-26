package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.CustomHeaderKey;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This sample demonstrates the Skyflow Java SDK's unary deleteFiles operation — deleting the files
 * in file columns of existing records, and the stored objects behind them. This makes exactly one
 * API call per invocation.
 *
 * Each record sets exactly one of skyflowId or uniqueValues; a uniqueValues entry may match several
 * records, giving one response entry per matched record. Record-level failures are reported on the
 * response, whole-call failures are thrown as a SkyflowException.
 */
public class DeleteFilesExample {

    public static void main(String[] args) {
        try {
            // Step 1: Initialize credentials with the path to your service account key file
//            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            Credentials credentials = new Credentials();
            credentials.setToken("<YOUR_BEARER_TOKEN>");

            // Step 2: Configure the vault with required parameters
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId("<YOUR_VAULT_ID>");
            vaultConfig.setClusterId("<YOUR_CLUSTER_ID>");
            vaultConfig.setEnv(Env.DEV);
            vaultConfig.setCredentials(credentials);

            // Step 3: Create Skyflow client instance with error logging
            Skyflow skyflowClient = Skyflow.builder()
                    .setLogLevel(LogLevel.ERROR)
                    .addVaultConfig(vaultConfig)
                    .build();

            // Step 4: Identify the records and the file columns to delete
            Map<String, Object> uniqueValue = new HashMap<>();
            uniqueValue.put("<YOUR_UNIQUE_COLUMN>", "<UNIQUE_VALUE>");

            List<DeleteFilesRequestRecord> records = Arrays.asList(
                    DeleteFilesRequestRecord.builder()
                            .tableName("<YOUR_TABLE_NAME>")
                            .skyflowId("<SKYFLOW_ID>")
                            .columns(Arrays.asList("<YOUR_FILE_COLUMN>", "<ANOTHER_FILE_COLUMN>"))
                            .build(),
                    DeleteFilesRequestRecord.builder()
                            .tableName("<YOUR_TABLE_NAME>")
                            .uniqueValues(Collections.singletonList(uniqueValue))
                            .columns(Collections.singletonList("<YOUR_FILE_COLUMN>"))
                            .build()
            );

            // Step 5: Build and execute the deleteFiles request
            DeleteFilesRequest request = DeleteFilesRequest.builder()
                    .records(records)
                    .build();

            DeleteFilesOptions options = DeleteFilesOptions.builder()
                    .interceptor(ctx -> {
                        ctx.addHeader(CustomHeaderKey.REQUEST_ID_HEADER, "DeleteFilesOptions"); // pass the request id here
                    })
                    .build();
            DeleteFilesResponse response = skyflowClient.vault().deleteFiles(request, options);

            // Step 6: Read the outcome. A record succeeded when its error is null.
            for (HashMap<String, Object> record : response.getRecords()) {
                if (record.get("error") == null) {
                    System.out.printf("deleteFiles: %s -> %s%n", record.get("skyflowId"), record.get("columns"));
                } else {
                    System.out.printf("deleteFiles failed (%s): %s%n", record.get("httpCode"), record.get("error"));
                }
            }
        } catch (SkyflowException e) {
            // Step 7: Handle any errors that occur during the process
            System.err.println("Error in deleteFiles operation:\t" + e);
        }
    }
}
