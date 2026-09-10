package com.example.vault;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.enums.UpdateType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateRequestRecord;
import com.skyflow.vault.data.UpdateResponse;
import com.skyflow.vault.data.UpdateResponseRecord;

/**
 * This sample demonstrates the Skyflow Java SDK's unary update operation. This makes exactly one
 * API call per invocation: there is no internal batching or concurrency to configure.
 */
public class UpdateExample {

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

            // Step 4: Prepare the record to update, identified by its skyflow ID
            Map<String, Object> data = new HashMap<>();
            data.put("<YOUR_COLUMN_NAME>", "<YOUR_COLUMN_VALUE>");

            UpdateRequestRecord updateRecord = UpdateRequestRecord.builder()
                    .skyflowId("<YOUR_SKYFLOW_ID>")
                    .data(data)
                    .build();

            List<UpdateRequestRecord> records = new ArrayList<>();
            records.add(updateRecord);

            // Step 5: Build and execute the update request.
            //         updateType accepts UpdateType.UPDATE (default) or UpdateType.REPLACE.
            UpdateRequest request = UpdateRequest.builder()
                    .tableName("table5")
                    .records(records)
                    .updateType(UpdateType.REPLACE)
                    .build();

            UpdateResponse response = skyflowClient.vault().update(request);

            // Step 6: Read the outcome. A record succeeded when its error is null.
            for (UpdateResponseRecord record : response.getRecords()) {
                if (record.getError() == null) {
                    System.out.println("data" + record.getTokens());
                    System.out.printf("update: %s -> skyflowId=%s%n", record.getTableName(), record.getSkyflowId());
                } else {
                    System.out.printf("update failed (%d): %s%n", record.getHttpCode(), record.getError());
                }
            }
        } catch (SkyflowException e) {
            // Step 7: Handle any errors that occur during the process
            System.err.println("Error in update operation:\t" + e.getMessage());
        }
    }
}
