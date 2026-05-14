package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.CustomHeaderKey;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.enums.UpsertType;
import com.skyflow.vault.data.InsertOptions;
import com.skyflow.vault.data.InsertRecord;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class CustomHeaderExample {
    public static void main(String[] args) {
        try {
            // Step 1: Initialize credentials with the path to your service account key file
//            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            Credentials credentials = new Credentials();
            credentials.setToken("<BEARER_TOKEN>");

            // Step 2: Configure the vault with required parameters
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId("<VAULT_ID>");
            vaultConfig.setClusterId("<CLUSTER_ID>");
            vaultConfig.setEnv(Env.DEV);
            vaultConfig.setCredentials(credentials);

            // Step 3: Create Skyflow client instance with error logging
            Skyflow skyflowClient = Skyflow.builder()
                    .setLogLevel(LogLevel.DEBUG)
                    .addVaultConfig(vaultConfig)
                    .build();
            ArrayList<InsertRecord> insertRecords = new ArrayList<>();

            for (int i = 0; i < 100; i++) {
                // Step 4: Prepare first record for insertion
                HashMap<String, Object> recordData1 = new HashMap<>();
                recordData1.put("<YOUR_COLUMN_NAME_2>", "<YOUR_VALUE_1>");

                InsertRecord insertRecord1 = InsertRecord
                        .builder()
                        .data(recordData1)
                        .build();

                // Step 6: Combine records into a Insert record list
                insertRecords.add(insertRecord1);
            }
            ArrayList<String> upsertColumns = new ArrayList<>();
            upsertColumns.add("<UPSERT_COLUMN_NAME>");
            InsertRequest request = InsertRequest.builder()
                    .table("<TABLE_NAME>")
                    .upsert(upsertColumns)
                    .upsertType(UpsertType.REPLACE)
                    .records(insertRecords)
                    .build();
            InsertOptions options = InsertOptions.builder()
                    .interceptor((ctx) ->{
                        ctx.addHeader(CustomHeaderKey.RequestIDHeader, getRequestId()); // pass the request id here
                    })
                    .build();
            // Step 8: Execute the async bulk insert operation and handle response using callbacks
            CompletableFuture<InsertResponse> future = skyflowClient.vault().bulkInsertAsync(request, options);
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
    public static String getRequestId(){
        String id = UUID.randomUUID().toString();
        System.out.println("id=>"+ id);
        return id;
    }
}
