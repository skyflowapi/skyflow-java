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

/**
 * This sample demonstrates how to perform a synchronous bulk delete tokens operation using the Skyflow Java SDK.
 * The process involves:
 * 1. Setting up credentials and vault configuration
 * 2. Creating a list of tokens to delete
 * 3. Building and executing a bulk delete tokens request
 * 4. Reading the per-token outcome and the summary from the response
 * 5. Handling the delete response or any potential errors
 */
public class BulkDeleteTokensSync {

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
            // correlated even though large requests are split into batches that run concurrently.
            List<String> tokens = new ArrayList<>();
            tokens.add("<YOUR_TOKEN_1>");
            tokens.add("<YOUR_TOKEN_2>");

            // Step 5: Build the bulk delete tokens request
            BulkDeleteTokensRequest deleteTokensRequest = BulkDeleteTokensRequest.builder()
                    .tokens(tokens)
                    .build();

            // Step 6: Execute the bulk delete tokens operation and print the response
            BulkDeleteTokensResponse deleteTokensResponse =
                    skyflowClient.vault().bulkDeleteTokens(deleteTokensRequest);
            System.out.println(deleteTokensResponse);

            // Step 7: Read the summary. totalTokens counts the tokens you submitted, and the other
            // two classify each one, so together they sum to that count.
            System.out.println("total tokens:\t" + deleteTokensResponse.getSummary().getTotalTokens());
            System.out.println("deleted:\t" + deleteTokensResponse.getSummary().getTotalDeleted());
            System.out.println("failed:\t\t" + deleteTokensResponse.getSummary().getTotalFailed());

            // Step 8: Walk the per-token outcomes. Successes and failures share one list: a record
            // succeeded when its error is null, and its index is the token's position in the request
            // you submitted. requestId identifies the API call an error came from and is set on
            // failures only.
            for (BulkDeleteTokensResponseRecord record : deleteTokensResponse.getRecords()) {
                if (record.getError() == null) {
                    System.out.printf("[%d] %s deleted (%d)%n",
                            record.getIndex(), record.getToken(), record.getHttpCode());
                } else {
                    System.out.printf("[%d] %s failed (%d): %s [requestId=%s]%n",
                            record.getIndex(), record.getToken(), record.getHttpCode(),
                            record.getError(), record.getRequestId());
                }
            }

            // Step 9: Optionally retry the tokens that failed with a retryable status (5xx other
            // than 529). A token that simply does not exist fails with a 4xx, so it is not included.
            List<String> tokensToRetry = deleteTokensResponse.getTokensToRetry();
            if (!tokensToRetry.isEmpty()) {
                System.out.println("retrying:\t" + tokensToRetry);
                BulkDeleteTokensResponse retryResponse = skyflowClient.vault().bulkDeleteTokens(
                        BulkDeleteTokensRequest.builder().tokens(tokensToRetry).build());
                System.out.println("retry response:\t" + retryResponse);
            }
        } catch (SkyflowException e) {
            // Step 10: Handle any errors that occur during the process
            System.err.println("Error in Skyflow operations: " + e.getMessage());
        }
    }
}
