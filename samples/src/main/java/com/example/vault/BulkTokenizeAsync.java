package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.TokenizeRecord;
import com.skyflow.vault.data.TokenizeRequest;
import com.skyflow.vault.data.TokenizeResponse;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * This sample demonstrates how to perform an asynchronous bulk tokenize operation using the Skyflow Java SDK.
 * The process involves:
 * 1. Setting up credentials and vault configuration
 * 2. Creating tokenize records with values and token group names
 * 3. Building and executing an async bulk tokenize request
 * 4. Handling the tokenize response or errors using CompletableFuture
 */
public class BulkTokenizeAsync {

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

            // Step 4: Specify token group names to tokenize the value against
            ArrayList<String> tokenGroupNames = new ArrayList<>();
            tokenGroupNames.add("<YOUR_TOKEN_GROUP_NAME_1>");
            tokenGroupNames.add("<YOUR_TOKEN_GROUP_NAME_2>");

            // Step 5: Build tokenize records, each with a value and one or more token group names
            TokenizeRecord record1 = TokenizeRecord.builder()
                    .value("<YOUR_VALUE_1>")
                    .tokenGroupNames(tokenGroupNames)
                    .build();

            TokenizeRecord record2 = TokenizeRecord.builder()
                    .value("<YOUR_VALUE_2>")
                    .tokenGroupNames(tokenGroupNames)
                    .build();

            ArrayList<TokenizeRecord> records = new ArrayList<>();
            records.add(record1);
            records.add(record2);

            // Step 6: Build the tokenize request
            TokenizeRequest tokenizeRequest = TokenizeRequest.builder()
                    .data(records)
                    .build();

            // Step 7: Execute the async bulk tokenize operation and handle response using callbacks
            CompletableFuture<TokenizeResponse> future = skyflowClient.vault().bulkTokenizeAsync(tokenizeRequest);
            future.thenAccept(response -> {
                System.out.println("Async bulk tokenize resolved with response:\t" + response);
            }).exceptionally(throwable -> {
                System.err.println("Async bulk tokenize rejected with error:\t" + throwable.getMessage());
                throw new CompletionException(throwable);
            });
        } catch (SkyflowException e) {
            // Step 8: Handle any synchronous errors that occur during setup
            System.err.println("Error in Skyflow operations: " + e.getMessage());
        }
    }
}
