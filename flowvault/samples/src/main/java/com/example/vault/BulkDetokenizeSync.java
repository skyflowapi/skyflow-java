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

/**
 * This sample demonstrates how to perform a synchronous bulk detokenize operation using the Skyflow Java SDK.
 * The process involves:
 * 1. Setting up credentials and vault configuration
 * 2. Creating a list of tokens to detokenize
 * 3. Configuring token group redactions
 * 4. Building and executing a bulk detokenize request
 * 5. Reading the per-token outcome and the summary from the response
 * 6. Handling the detokenize response or any potential errors
 */
public class BulkDetokenizeSync {

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
            // results stay correlated even though large requests are split into batches that run
            // concurrently.
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

            // Step 7: Execute the bulk detokenize operation and print the response
            BulkDetokenizeResponse detokenizeResponse = skyflowClient.vault().bulkDetokenize(detokenizeRequest);
            System.out.println(detokenizeResponse);

            // Step 8: Read the summary. totalTokens counts the tokens you submitted, and the
            // other two classify each one, so together they sum to that count.
            System.out.println("total tokens:\t" + detokenizeResponse.getSummary().getTotalTokens());
            System.out.println("detokenized:\t" + detokenizeResponse.getSummary().getTotalDetokenized());
            System.out.println("failed:\t\t" + detokenizeResponse.getSummary().getTotalFailed());

            // Step 9: Walk the per-token outcomes. A record succeeded when its error is null;
            // requestId identifies the batch an error came from and is set on failures only.
            for (BulkDetokenizeResponseRecord record : detokenizeResponse.getRecords()) {
                if (record.getError() == null) {
                    System.out.printf("[%d] %s -> value=%s group=%s%n",
                            record.getIndex(), record.getToken(), record.getValue(), record.getTokenGroupName());
                } else {
                    System.out.printf("[%d] %s failed (%d): %s [requestId=%s]%n",
                            record.getIndex(), record.getToken(), record.getHttpCode(),
                            record.getError(), record.getRequestId());
                }
            }

            // Step 10: Optionally retry the tokens that failed with a retryable status (5xx other
            // than 529). A token that simply does not exist fails with a 4xx, so it is not included.
            List<String> tokensToRetry = detokenizeResponse.getTokensToRetry();
            if (!tokensToRetry.isEmpty()) {
                System.out.println("retrying:\t" + tokensToRetry);
                BulkDetokenizeResponse retryResponse = skyflowClient.vault().bulkDetokenize(
                        BulkDetokenizeRequest.builder()
                                .tokens(tokensToRetry)
                                .tokenGroupRedactions(tokenGroupRedactions)
                                .build());
                System.out.println("retry response:\t" + retryResponse);
            }
        } catch (SkyflowException e) {
            // Step 11: Handle any errors that occur during the process
            System.err.println("Error in Skyflow operations: " + e.getMessage());
        }
    }
}