package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.BulkTokenizeRequest;
import com.skyflow.vault.data.BulkTokenizeRequestRecord;
import com.skyflow.vault.data.BulkTokenizeResponse;
import com.skyflow.vault.data.BulkTokenizeResponseRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * This sample demonstrates how to perform a synchronous bulk tokenize operation using the Skyflow Java SDK.
 * The process involves:
 * 1. Setting up credentials and vault configuration
 * 2. Creating a list of records, each with a value and one or more token group names
 * 3. Building and executing a bulk tokenize request
 * 4. Reading the per-token-group outcome for each value
 * 5. Handling the tokenize response or any potential errors
 */
public class BulkTokenizeSync {

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

            // Step 4: Specify the token groups to tokenize each value against
            List<String> tokenGroupNames = new ArrayList<>();
            tokenGroupNames.add("<YOUR_TOKEN_GROUP_NAME_1>");
            tokenGroupNames.add("<YOUR_TOKEN_GROUP_NAME_2>");

            // Step 5: Prepare the records to tokenize. The SDK assigns each record an index from its
            // position in this list and returns it on the matching response record, so results stay
            // correlated even though large requests are split into batches that run concurrently.
            List<BulkTokenizeRequestRecord> records = new ArrayList<>();
            records.add(BulkTokenizeRequestRecord.builder()
                    .value("<YOUR_VALUE_1>")
                    .tokenGroupNames(tokenGroupNames)
                    .build());
            records.add(BulkTokenizeRequestRecord.builder()
                    .value("<YOUR_VALUE_2>")
                    // Optional: supply your own token instead of having one generated (BYOT).
                    // A BYOT record must name exactly one token group.
                    // .token("<YOUR_OWN_TOKEN>")
                    .tokenGroupNames(tokenGroupNames)
                    .build());

            // Step 6: Build the bulk tokenize request
            BulkTokenizeRequest tokenizeRequest = BulkTokenizeRequest.builder()
                    .records(records)
                    .build();

            // Step 7: Execute the bulk tokenize operation and print the response
            BulkTokenizeResponse tokenizeResponse = skyflowClient.vault().bulkTokenize(tokenizeRequest);
            System.out.println(tokenizeResponse);

            // Step 8: Read the summary. totalTokens counts the values you submitted; the other three
            // classify each value by how its token groups fared, so together they sum to that count.
            System.out.println("total values:\t" + tokenizeResponse.getSummary().getTotalTokens());
            System.out.println("tokenized:\t" + tokenizeResponse.getSummary().getTotalTokenized());
            System.out.println("partial:\t" + tokenizeResponse.getSummary().getTotalPartial());
            System.out.println("failed:\t\t" + tokenizeResponse.getSummary().getTotalFailed());

            // Step 9: Walk the results. Each value reports one entry per token group, so a value can
            // partially succeed: some groups return a token while others return an error. requestId
            // identifies the API call an error came from and is set on failures only.
            for (BulkTokenizeResponseRecord record : tokenizeResponse.getRecords()) {
                if (record.getError() == null) {
                    System.out.printf("[%d] group '%s' -> %s%n",
                            record.getIndex(), record.getTokenGroupName(), record.getToken());
                } else {
                    System.out.printf("[%d] group '%s' failed (%d): %s [requestId=%s]%n",
                            record.getIndex(), record.getTokenGroupName(),
                            record.getHttpCode(), record.getError(), record.getRequestId());
                }
            }

            // Step 10: Optionally retry the records that failed with a retryable status (5xx other
            // than 529). Your original records come back unchanged and can be resubmitted as-is.
            List<BulkTokenizeRequestRecord> recordsToRetry = tokenizeResponse.getRecordsToRetry();
            if (!recordsToRetry.isEmpty()) {
                BulkTokenizeResponse retryResponse = skyflowClient.vault().bulkTokenize(
                        BulkTokenizeRequest.builder().records(recordsToRetry).build());
                System.out.println("retry response:\t" + retryResponse);
            }
        } catch (SkyflowException e) {
            // Step 11: Handle any errors that occur during the process
            System.err.println("Error in Skyflow operations: " + e.getMessage());
        }
    }
}
