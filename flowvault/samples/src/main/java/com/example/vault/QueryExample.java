package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.QueryResponse;
import com.skyflow.vault.data.QueryResponseRecord;

/**
 * This sample demonstrates the Skyflow Java SDK's query operation. There is no bulk/batched
 * counterpart of this operation — a single call runs the query as-is.
 */
public class QueryExample {

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
            vaultConfig.setEnv(Env.PROD);
            vaultConfig.setCredentials(credentials);

            // Step 3: Create Skyflow client instance with error logging
            Skyflow skyflowClient = Skyflow.builder()
                    .setLogLevel(LogLevel.ERROR)
                    .addVaultConfig(vaultConfig)
                    .build();

            // Step 4: Build and execute the query request
            QueryRequest request = QueryRequest.builder()
                    .query("SELECT * FROM <YOUR_TABLE_NAME> LIMIT 1")
                    .build();

            QueryResponse response = skyflowClient.vault().query(request);

            // Step 5: Read the returned rows and the reported columns
            for (QueryResponseRecord record : response.getRecords()) {
                System.out.println("query row: " + record.getData());
            }
            System.out.println("columns: " + (response.getMetadata() != null ? response.getMetadata().getColumns() : null));
        } catch (SkyflowException e) {
            // Step 6: Handle any errors that occur during the process
            System.err.println("Error in query operation:\t" + e.getMessage());
        }
    }
}
