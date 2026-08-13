package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.CustomHeaderKey;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.vault.data.BulkInsertRequest;
import com.skyflow.vault.data.BulkInsertRequestRecord;
import com.skyflow.vault.data.BulkInsertResponse;
import com.skyflow.vault.data.BulkInsertOptions;
import com.skyflow.vault.data.BulkInsertResponseRecord;
import com.skyflow.vault.data.InsertRequestRecord;
import com.skyflow.vault.data.Token;
import com.skyflow.vault.data.UpsertOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * This sample demonstrates how to attach custom headers to outgoing requests via a request
 * interceptor on the options object.
 *
 * <p>Available keys on {@link CustomHeaderKey}: {@code SKYFLOW_ACCOUNT_ID} ({@code x-skyflow-account-id}),
 * {@code SKYFLOW_ACCOUNT_NAME} ({@code x-skyflow-account-name}) and {@code REQUEST_ID_HEADER}
 * ({@code x-request-id}).
 *
 * <p>The interceptor runs once per batch, so a per-request value such as a request id is generated
 * fresh for each outgoing call rather than reused across the whole bulk operation.
 */
public class CustomHeaderExample {
    public static void main(String[] args) {
        try {
            // Step 1: Initialize credentials with a bearer token
            Credentials credentials = new Credentials();
            credentials.setToken("<BEARER_TOKEN>");

            // Step 2: Configure the vault with required parameters
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId("<VAULT_ID>");
            vaultConfig.setClusterId("<CLUSTER_ID>");
            vaultConfig.setEnv(Env.DEV);
            vaultConfig.setCredentials(credentials);

            // Step 3: Create Skyflow client instance with debug logging
            Skyflow skyflowClient = Skyflow.builder()
                    .setLogLevel(LogLevel.DEBUG)
                    .addVaultConfig(vaultConfig)
                    .build();

            // Step 4: Prepare the records for insertion
            List<InsertRequestRecord> insertRecords = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                HashMap<String, Object> recordData = new HashMap<>();
                recordData.put("<YOUR_COLUMN_NAME>", "<YOUR_VALUE>");

                BulkInsertRequestRecord insertRecord = BulkInsertRequestRecord
                        .builder()
                        .data(recordData)
                        .build();

                insertRecords.add(insertRecord);
            }

            // Step 5: Configure upsert. uniqueColumns is required; updateType accepts "UPDATE"
            //         or "REPLACE" — if omitted, the vault treats it the same as "UPDATE".
            List<String> upsertColumns = new ArrayList<>();
            upsertColumns.add("<UPSERT_COLUMN_NAME>");

            UpsertOptions upsert = UpsertOptions.builder()
                    .uniqueColumns(upsertColumns)
                    .updateType("REPLACE")
                    .build();

            // Step 6: Build the insert request. tableName and upsert both sit at the request level.
            BulkInsertRequest request = BulkInsertRequest.builder()
                    .tableName("<TABLE_NAME>")
                    .upsert(upsert)
                    .records(insertRecords)
                    .build();

            // Step 7: Attach a custom header through the interceptor
            BulkInsertOptions options = BulkInsertOptions.builder()
                    .interceptor(ctx -> {
                        ctx.addHeader(CustomHeaderKey.REQUEST_ID_HEADER, getRequestId()); // pass the request id here
                    })
                    .build();

            // Step 8: Execute the async bulk insert operation and handle response using callbacks
            CompletableFuture<BulkInsertResponse> future =
                    skyflowClient.vault().bulkInsertAsync(request, options);
            future.thenAccept(response -> {
                System.out.println("Async bulk insert resolved with response:\t" + response);

                // Read the summary, then walk the per-record outcomes. A record succeeded when
                // its error is null; requestId identifies the batch an error came from and is
                // set on failures only — the same header this sample attaches to the outgoing
                // request shows up here on any batch that failed.
                System.out.println("inserted:\t" + response.getSummary().getTotalInserted()
                        + " of " + response.getSummary().getTotalRecords());

                for (BulkInsertResponseRecord record : response.getRecords()) {
                    if (record.getError() == null) {
                        System.out.printf("[%d] skyflowId=%s%n", record.getIndex(), record.getSkyflowId());
                        // getTokenDetails() is a typed view of getTokens(): Map<String, List<Token>>
                        // instead of Map<String, Object>, so no casting to read token/tokenGroupName.
                        for (Map.Entry<String, List<Token>> column : record.getTokenDetails().entrySet()) {
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
            }).exceptionally(throwable -> {
                System.err.println("Async bulk insert rejected with error:\t" + throwable.getMessage());
                throw new CompletionException(throwable);
            }).join(); // sample-run only: block so main() doesn't exit before the async callback prints
        } catch (Exception e) {
            // Step 9: Handle any synchronous errors that occur during setup
            System.err.println("Error in Skyflow operations:\t" + e.getMessage());
        }
    }

    public static String getRequestId() {
        String id = UUID.randomUUID().toString();
        System.out.println("id=>" + id);
        return id;
    }
}