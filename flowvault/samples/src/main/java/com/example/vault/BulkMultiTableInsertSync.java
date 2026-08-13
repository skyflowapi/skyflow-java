package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.BulkInsertRequest;
import com.skyflow.vault.data.BulkInsertRequestRecord;
import com.skyflow.vault.data.BulkInsertResponse;
import com.skyflow.vault.data.BulkInsertResponseRecord;
import com.skyflow.vault.data.InsertRequestRecord;
import com.skyflow.vault.data.Token;
import com.skyflow.vault.data.UpsertOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This sample demonstrates how to perform a synchronous bulk insert operation using the Skyflow Java SDK.
 * The process involves:
 * 1. Setting up credentials and vault configuration
 * 2. Creating multiple records to be inserted
 * 3. Building and executing a bulk insert request
 * 4. Reading the per-record outcome and the summary from the response
 * 5. Handling the insert response or any potential errors
 *
 * <p>Multi-table mode: the table name is set on <b>every</b> record instead of on the request.
 * The SDK rejects a request that sets it at both levels, or on only some of the records.
 */
public class BulkMultiTableInsertSync {

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

            // Step 4: Prepare first record for insertion
            HashMap<String, Object> recordData1 = new HashMap<>();
            recordData1.put("<YOUR_COLUMN_NAME_1>", "<YOUR_VALUE_1>");
            recordData1.put("<YOUR_COLUMN_NAME_2>", "<YOUR_VALUE_1>");

            List<String> upsertColumns = new ArrayList<>();
            upsertColumns.add("<YOUR_COLUMN_NAME_1>");

            // upsert is optional; when updateType is omitted the vault treats it the same as "UPDATE".
            // Set .updateType("REPLACE") to replace the matched row instead.
            UpsertOptions upsert = UpsertOptions.builder()
                    .uniqueColumns(upsertColumns)
                    .build();

            BulkInsertRequestRecord insertRecord1 = BulkInsertRequestRecord
                    .builder()
                    .data(recordData1)
                    .tableName("<YOUR_TABLE_NAME_1>")
                    .upsert(upsert)
                    .build();

            // Step 5: Prepare second record for insertion
            HashMap<String, Object> recordData2 = new HashMap<>();
            recordData2.put("<YOUR_COLUMN_NAME_1>", "<YOUR_VALUE_1>");
            recordData2.put("<YOUR_COLUMN_NAME_2>", "<YOUR_VALUE_1>");

            BulkInsertRequestRecord insertRecord2 = BulkInsertRequestRecord
                    .builder()
                    .data(recordData2)
                    .tableName("<YOUR_TABLE_NAME_2>")
                    .build();

            // Step 6: Combine records into a Insert record list
            List<InsertRequestRecord> insertRecords = new ArrayList<>();
            insertRecords.add(insertRecord1);
            insertRecords.add(insertRecord2);

            // Step 7: Build the insert request. No tableName here — each record carries its own.
            BulkInsertRequest request = BulkInsertRequest.builder()
                    .records(insertRecords)
                    .build();

            // Step 8: Execute the bulk insert operation and print the response
            BulkInsertResponse response = skyflowClient.vault().bulkInsert(request);
            System.out.println(response);

            // Step 9: Read the summary, then walk the per-record outcomes. A record succeeded
            // when its error is null; requestId identifies the batch an error came from and is
            // set on failures only.
            System.out.println("inserted:\t" + response.getSummary().getTotalInserted()
                    + " of " + response.getSummary().getTotalRecords());

            for (BulkInsertResponseRecord record : response.getRecords()) {
                if (record.getError() == null) {
                    System.out.printf("[%d] %s -> skyflowId=%s%n",
                            record.getIndex(), record.getTableName(), record.getSkyflowId());
                    // getTokens() returns a typed Map<String, List<Token>>
                    // - no casting needed to read token/tokenGroupName.
                    for (Map.Entry<String, List<Token>> column : record.getTokens().entrySet()) {
                        for (Token token : column.getValue()) {
                            System.out.printf("    %s[%s] -> %s%n",
                                    column.getKey(), token.getTokenGroupName(), token.getToken());
                        }
                    }
                } else {
                    System.out.printf("[%d] failed (%d): %s [requestId=%s]%n",
                            record.getIndex(), record.getHttpCode(), record.getError(), record.getRequestId());
                }
            }

            // Step 10: Optionally retry the records that failed with a retryable status (5xx other
            // than 529). Each returned record still carries its own tableName/upsert, so the retry
            // request needs no request-level tableName.
            List<BulkInsertRequestRecord> recordsToRetry = response.getRecordsToRetry();
            if (!recordsToRetry.isEmpty()) {
                BulkInsertResponse retryResponse = skyflowClient.vault().bulkInsert(
                        BulkInsertRequest.builder()
                                .records(new ArrayList<>(recordsToRetry))
                                .build());
                System.out.println("retry response:\t" + retryResponse);
            }
        } catch (SkyflowException e) {
            // Step 11: Handle any errors that occur during the process
            System.err.println("Error in Skyflow operations: " + e.getMessage());
        }
    }
}
