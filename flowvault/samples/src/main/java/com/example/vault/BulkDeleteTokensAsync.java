package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.BulkDeleteTokensRequest;
import com.skyflow.vault.data.BulkDeleteTokensResponse;
import com.skyflow.vault.data.BulkDeleteTokensResponseRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * This sample demonstrates how to perform an asynchronous bulk delete tokens operation using the Skyflow Java SDK.
 * The process involves:
 * 1. Setting up credentials and vault configuration
 * 2. Creating a list of tokens to delete
 * 3. Building and executing an async bulk delete tokens request
 * 4. Reading the per-token outcome and the summary from the response
 * 5. Handling the delete response or errors using CompletableFuture
 */
public class BulkDeleteTokensAsync {

    public static void main(String[] args) {
        try {
            // Step 1: Initialize credentials using credentials string
            String credentialsString = "<YOUR_CREDENTIALS_STRING>";
            Credentials credentials = new Credentials();
            credentials.setCredentialsString(credentialsString);

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

            // Step 4: Prepare list of tokens to delete. The SDK assigns each token an index from its
            // position in this list and returns it on the matching response record, so results stay
            // correlated even though the batches complete out of order.
            List<String> tokens = new ArrayList<>();
            tokens.add("<YOUR_TOKEN_1>");
            tokens.add("<YOUR_TOKEN_2>");

            // Step 5: Build the bulk delete tokens request
            BulkDeleteTokensRequest deleteTokensRequest = BulkDeleteTokensRequest.builder()
                    .tokens(tokens)
                    .build();

            // Step 6: Execute the async bulk delete tokens operation and handle response using callbacks
            CompletableFuture<BulkDeleteTokensResponse> future =
                    skyflowClient.vault().bulkDeleteTokensAsync(deleteTokensRequest);
            future.thenAccept(response -> {
                System.out.println("Async bulk delete tokens resolved with response:\t" + response);

                // Successes and failures share one list: a record succeeded when its error is null.
                // requestId identifies the API call an error came from and is set on failures only.
                for (BulkDeleteTokensResponseRecord record : response.getRecords()) {
                    if (record.getError() == null) {
                        System.out.printf("[%d] %s deleted (%d)%n",
                                record.getIndex(), record.getToken(), record.getHttpCode());
                    } else {
                        System.out.printf("[%d] %s failed (%d): %s [requestId=%s]%n",
                                record.getIndex(), record.getToken(), record.getHttpCode(),
                                record.getError(), record.getRequestId());
                    }
                }

                // Tokens that failed with a retryable status (5xx other than 529) can be resubmitted
                if (!response.getTokensToRetry().isEmpty()) {
                    System.out.println("tokens to retry:\t" + response.getTokensToRetry());
                }
            }).exceptionally(throwable -> {
                System.err.println("Async bulk delete tokens rejected with error:\t" + throwable.getMessage());
                throw new CompletionException(throwable);
            });
        } catch (SkyflowException e) {
            // Step 7: Handle any synchronous errors that occur during setup
            System.err.println("Error in Skyflow operations: " + e.getMessage());
        }
    }
}
