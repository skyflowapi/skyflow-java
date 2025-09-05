package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * This sample demonstrates how to perform an asynchronous bulk insert operation using the Skyflow Java SDK.
 * The process involves:
 * 1. Setting up credentials and vault configuration
 * 2. Creating multiple records to be inserted
 * 3. Building and executing an async bulk insert request
 * 4. Handling the insert response or errors using CompletableFuture
 */
public class BulkInsertAsync {

    public static void main(String[] args) {
        try {
            // Step 1: Initialize credentials with the path to your service account key file
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            Credentials credentials = new Credentials();
            credentials.setPath(filePath);

            // Step 2: Configure the vault with required parameters
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId("<YOUR_VAULT_ID>");
            vaultConfig.setClusterId("<YOUR_CLUSTER_ID>");
            vaultConfig.setEnv(Env.PROD);
            vaultConfig.setCredentials(credentials);

            // Step 3: Create Skyflow client instance with error logging
            Skyflow skyflowClient = Skyflow.builder()
                    .setLogLevel(LogLevel.ERROR)
                    .addVaultConfig(vaultConfig)
                    .build();

            // Step 4: Prepare first record for insertion
            HashMap<String, Object> record1 = new HashMap<>();
            record1.put("<YOUR_COLUMN_NAME_1>", "<YOUR_VALUE_1>");
            record1.put("<YOUR_COLUMN_NAME_2>", "<YOUR_VALUE_1>");

            // Step 5: Prepare second record for insertion
            HashMap<String, Object> record2 = new HashMap<>();
            record2.put("<YOUR_COLUMN_NAME_1>", "<YOUR_VALUE_1>");
            record2.put("<YOUR_COLUMN_NAME_2>", "<YOUR_VALUE_1>");

            // Step 6: Combine records into a single list
            ArrayList<HashMap<String, Object>> values = new ArrayList<>();
            values.add(record1);
            values.add(record2);

            // Step 7: Build the insert request with table name and values
            InsertRequest request = InsertRequest.builder()
                    .table("<YOUR_TABLE_NAME>")
                    .values(values)
                    .build();

            // Step 8: Execute the async bulk insert operation and handle response using callbacks
            CompletableFuture<InsertResponse> future = skyflowClient.vault().bulkInsertAsync(request);
            // Add success and error callbacks
            future.thenAccept(response -> {
                System.out.println("Async bulk insert resolved with response:\t" + response);
            }).exceptionally(throwable -> {
                System.err.println("Async bulk insert rejected with error:\t" + throwable.getMessage());
                throw new CompletionException(throwable);
            });
        } catch (Exception e) {
            // Step 9: Handle any synchronous errors that occur during setup
            System.err.println("Error in Skyflow operations:\t" + e.getMessage());
        }
    }
}