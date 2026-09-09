package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.DetokenizeResponse;
import com.skyflow.vault.data.DetokenizeResponseRecord;
import com.skyflow.vault.data.TokenGroupRedactions;

import java.util.ArrayList;
import java.util.List;

/**
 * This sample demonstrates the Skyflow Java SDK's unary detokenize operation. Unlike
 * bulkDetokenize, this makes exactly one API call per invocation: there is no internal batching
 * or concurrency to configure.
 */
public class DetokenizeExample {

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

            // Step 4: Prepare the tokens to detokenize and any per-group redactions
            List<String> tokens = new ArrayList<>();
            tokens.add("98579059870301");

            List<TokenGroupRedactions> tokenGroupRedactions = new ArrayList<>();
            tokenGroupRedactions.add(TokenGroupRedactions.builder()
                    .tokenGroupName("nondeterministic")
                    .redaction("plain_text")
                    .build());

            // Step 5: Build and execute the detokenize request
            DetokenizeRequest request = DetokenizeRequest.builder()
                    .tokens(tokens)
                    .tokenGroupRedactions(tokenGroupRedactions)
                    .build();

            DetokenizeResponse response = skyflowClient.vault().detokenize(request);

            // Step 6: Read the outcome. A record succeeded when its error is null.
            for (DetokenizeResponseRecord record : response.getRecords()) {
                if (record.getError() == null) {
                    System.out.printf("detokenize: %s -> %s (tableName=%s)%n",
                            record.getToken(), record.getValue(), record.getMetadata().getTableName());
                } else {
                    System.out.printf("detokenize failed (%d): %s%n", record.getHttpCode(), record.getError());
                }
            }
        } catch (SkyflowException e) {
            // Step 7: Handle any errors that occur during the process
            System.err.println("Error in detokenize operation:\t" + e.getMessage());
        }
    }
}
