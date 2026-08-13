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
import com.skyflow.vault.data.UpsertOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This sample demonstrates how to perform a synchronous bulk insert operation using the Skyflow Java SDK.
 * The process involves:
 * 1. Setting up credentials and vault configuration
 * 2. Creating multiple records to be inserted
 * 3. Building and executing a bulk insert request
 * 4. Reading the per-record outcome and the summary from the response
 * 5. Handling the insert response or any potential errors
 */
public class BulkInsertSync {

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

            BulkInsertRequestRecord insertRecord1 = BulkInsertRequestRecord
                    .builder()
                    .data(recordData1)
                    .build();

            // Step 5: Prepare second record for insertion
            HashMap<String, Object> recordData2 = new HashMap<>();
            recordData2.put("<YOUR_COLUMN_NAME_1>", "<YOUR_VALUE_1>");
            recordData2.put("<YOUR_COLUMN_NAME_2>", "<YOUR_VALUE_1>");

            BulkInsertRequestRecord insertRecord2 = BulkInsertRequestRecord
                    .builder()
                    .data(recordData2)
                    .build();

            // Step 6: Combine records into a Insert record list
            List<InsertRequestRecord> insertRecords = new ArrayList<>();
            insertRecords.add(insertRecord1);
            insertRecords.add(insertRecord2);

            // Step 7: Configure upsert. uniqueColumns is required; updateType accepts "UPDATE"
            //         or "REPLACE" — if omitted, the vault treats it the same as "UPDATE".
            List<String> upsertColumns = new ArrayList<>();
            upsertColumns.add("<YOUR_COLUMN_NAME_1>");

            UpsertOptions upsert = UpsertOptions.builder()
                    .uniqueColumns(upsertColumns)
                    .updateType("REPLACE")
                    .build();

            // Step 8: Build the insert request with table name and insertRecords.
            //         tableName and upsert must sit at the same level — here, the request level.
            BulkInsertRequest request = BulkInsertRequest.builder()
                    .tableName("<YOUR_TABLE_NAME>")
                    .upsert(upsert)
                    .records(insertRecords)
                    .build();

            // Step 9: Execute the bulk insert operation and print the response
            BulkInsertResponse response = skyflowClient.vault().bulkInsert(request);
            System.out.println(response);

            // Step 10: Read the summary, then walk the per-record outcomes. A record succeeded
            // when its error is null; requestId identifies the batch an error came from and is
            // set on failures only.
            System.out.println("inserted:\t" + response.getSummary().getTotalInserted()
                    + " of " + response.getSummary().getTotalRecords());

            for (BulkInsertResponseRecord record : response.getRecords()) {
                if (record.getError() == null) {
                    System.out.printf("[%d] %s -> skyflowId=%s tokens=%s%n",
                            record.getIndex(), record.getTableName(), record.getSkyflowId(), record.getTokens());
                } else {
                    System.out.printf("[%d] failed (%d): %s [requestId=%s]%n",
                            record.getIndex(), record.getHttpCode(), record.getError(), record.getRequestId());
                }
            }

            // Step 11: Optionally retry the records that failed with a retryable status (5xx other
            // than 529). Your original records come back unchanged and can be resubmitted as-is.
            List<BulkInsertRequestRecord> recordsToRetry = response.getRecordsToRetry();
            if (!recordsToRetry.isEmpty()) {
                BulkInsertResponse retryResponse = skyflowClient.vault().bulkInsert(
                        BulkInsertRequest.builder()
                                .tableName("<YOUR_TABLE_NAME>")
                                .upsert(upsert)
                                .records(new ArrayList<>(recordsToRetry))
                                .build());
                System.out.println("retry response:\t" + retryResponse);
            }
        } catch (SkyflowException e) {
            // Step 12: Handle any errors that occur during the process
            System.err.println("Error in Skyflow operations: " + e.getMessage());
        }
    }
}