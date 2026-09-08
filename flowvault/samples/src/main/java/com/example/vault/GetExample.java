package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.ColumnRedactions;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;
import com.skyflow.vault.data.GetResponseRecord;
import com.skyflow.vault.data.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This sample demonstrates the Skyflow Java SDK's unary get operation. This makes exactly one
 * API call per invocation: there is no internal batching or concurrency to configure.
 *
 * A GetRequest works in one of two mutually exclusive modes: single-table (as shown here — a
 * table plus ids or uniqueValues) or multi-table, via GetRequest#getRecords() (a list of
 * GetRequestRecord, each specifying its own table).
 */
public class GetExample {

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

            // Step 4: Prepare the skyflow IDs to fetch and any column redactions
            ArrayList<String> ids = new ArrayList<>();
            ids.add("<YOUR_SKYFLOW_ID>");

            List<ColumnRedactions> columnRedactions = new ArrayList<>();
            columnRedactions.add(ColumnRedactions.builder()
                    .columnName("<YOUR_COLUMN_NAME_1>")
                    .redaction("PLAIN_TEXT")
                    .build());

            // Step 5: Build and execute the get request
            GetRequest request = GetRequest.builder()
                    .table("<YOUR_TABLE_NAME>")
                    .ids(ids)
                    .columnRedactions(columnRedactions)
                    .build();

            GetResponse response = skyflowClient.vault().get(request);

            // Step 6: Read the fetched records
            for (GetResponseRecord record : response.getRecords()) {
                System.out.printf("get: %s -> skyflowId=%s%n", record.getTableName(), record.getSkyflowId());
                for (Map.Entry<String, List<Token>> column : record.getTokens().entrySet()) {
                    for (Token token : column.getValue()) {
                        System.out.printf("    %s[%s] -> %s%n",
                                column.getKey(), token.getTokenGroupName(), token.getToken());
                    }
                }
            }
        } catch (SkyflowException e) {
            // Step 7: Handle any errors that occur during the process
            System.err.println("Error in get operation:\t" + e.getMessage());
        }
    }
}
