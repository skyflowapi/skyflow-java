package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.BulkDetokenizeRequest;
import com.skyflow.vault.data.BulkDetokenizeResponse;
import com.skyflow.vault.data.BulkDetokenizeResponseRecord;
import com.skyflow.vault.data.TokenGroupRedactions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * This sample demonstrates how to perform an asynchronous bulk detokenize operation using the Skyflow Java SDK.
 * The process involves:
 * 1. Setting up credentials and vault configuration
 * 2. Creating a list of tokens to detokenize
 * 3. Configuring token group redactions
 * 4. Building and executing an async bulk detokenize request
 * 5. Reading the per-token outcome and the summary from the response
 * 6. Handling the detokenize response or errors using CompletableFuture
 */
public class BulkDetokenizeAsync {

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

            // Step 4: Prepare list of tokens to detokenize. The SDK assigns each token an index
            // from its position in this list and returns it on the matching response record, so
            // results stay correlated even though the batches complete out of order.
            List<String> tokens = new ArrayList<>();
            tokens.add("<YOUR_TOKEN_1>");
            tokens.add("<YOUR_TOKEN_2>");

            // Step 5: Configure token group redactions
            TokenGroupRedactions tokenGroupRedaction = TokenGroupRedactions.builder()
                    .tokenGroupName("<YOUR_TOKEN_GROUP_NAME>")
                    .redaction("<YOUR_REDACTION>")
                    .build();
            List<TokenGroupRedactions> tokenGroupRedactions = new ArrayList<>();
            tokenGroupRedactions.add(tokenGroupRedaction);

            // Step 6: Build the detokenize request
            BulkDetokenizeRequest detokenizeRequest = BulkDetokenizeRequest.builder()
                    .tokens(tokens)
                    .tokenGroupRedactions(tokenGroupRedactions)
                    .build();

            // Step 7: Execute the async bulk detokenize operation and handle response using callbacks
            CompletableFuture<BulkDetokenizeResponse> future =
                    skyflowClient.vault().bulkDetokenizeAsync(detokenizeRequest);
            future.thenAccept(response -> {
                System.out.println("Async bulk detokenize resolved with response:\t" + response);

                // Read the summary. totalTokens counts the tokens you submitted, and the other
                // two classify each one, so together they sum to that count.
                System.out.println("total tokens:\t" + response.getSummary().getTotalTokens());
                System.out.println("detokenized:\t" + response.getSummary().getTotalDetokenized());
                System.out.println("failed:\t\t" + response.getSummary().getTotalFailed());

                // Walk the per-token outcomes. A record succeeded when its error is null;
                // requestId identifies the batch an error came from and is set on failures only.
                for (BulkDetokenizeResponseRecord record : response.getRecords()) {
                    if (record.getError() == null) {
                        System.out.printf("[%d] %s -> value=%s group=%s%n",
                                record.getIndex(), record.getToken(), record.getValue(), record.getTokenGroupName());
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
                System.err.println("Async bulk detokenize rejected with error:\t" + throwable.getMessage());
                throw new CompletionException(throwable);
            }).join(); // sample-run only: block so main() doesn't exit before the async callback prints
        } catch (Exception e) {
            // Step 8: Handle any synchronous errors that occur during setup
            System.err.println("Error in Skyflow operations: " + e.getMessage());
        }
    }
}