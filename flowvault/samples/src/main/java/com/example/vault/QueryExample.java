package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.CustomHeaderKey;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.*;

import java.util.HashMap;

/**
 * This sample demonstrates the Skyflow Java SDK's unary query operation — running a SQL SELECT
 * against the vault. This makes exactly one API call per invocation.
 *
 * Only SELECT is supported, and a call returns at most 25 records; page with SQL OFFSET. Values
 * may come back masked, but are never tokens or file URLs. There is no per-record status: any
 * failure is thrown as a SkyflowException.
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
            vaultConfig.setEnv(Env.DEV);
            vaultConfig.setCredentials(credentials);

            // Step 3: Create Skyflow client instance with error logging
            Skyflow skyflowClient = Skyflow.builder()
                    .setLogLevel(LogLevel.ERROR)
                    .addVaultConfig(vaultConfig)
                    .build();

            // Step 4: Build and execute the query request
            QueryRequest request = QueryRequest.builder()
                    .query("SELECT * FROM <YOUR_TABLE_NAME> LIMIT 25 OFFSET 0")
                    .build();

            QueryOptions options = QueryOptions.builder()
                    .interceptor(ctx -> {
                        ctx.addHeader(CustomHeaderKey.REQUEST_ID_HEADER, "QueryOptions"); // pass the request id here
                    })
                    .build();
            QueryResponse response = skyflowClient.vault().query(request, options);

            // Step 5: Read the rows. Each map holds one record's column/value pairs.
            System.out.println("columns: " + response.getMetadata().getColumns());
            for (HashMap<String, Object> row : response.getFields()) {
                System.out.println("row: " + row);
            }
            System.out.println("requestId: " + response.getRequestId());
        } catch (SkyflowException e) {
            // Step 6: Handle any errors that occur during the process
            System.err.println("Error in query operation:\t" + e);
        }
    }
}
