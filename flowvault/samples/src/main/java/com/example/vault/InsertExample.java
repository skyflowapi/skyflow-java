package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.CustomHeaderKey;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This sample demonstrates the Skyflow Java SDK's unary insert operation. Unlike bulkInsert,
 * this makes exactly one API call per invocation: there is no internal batching or concurrency
 * to configure.
 */
public class InsertExample {

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

            // Step 4: Prepare the record to insert
            Map<String, Object> data = new HashMap<>();
            data.put("<YOUR_COLUMN_NAME_1>", "<YOUR_VALUE_1>");

            InsertRequestRecord record = InsertRequestRecord.builder()
                    .data(data)
                    .tableName("<YOUR_TABLE_NAME>")
                    .build();

            List<InsertRequestRecord> records = new ArrayList<>();
            records.add(record);

            // Step 5: Build and execute the insert request
            InsertRequest request = InsertRequest.builder()
//                    .tableName("<YOUR_TABLE_NAME>")
                    .records(records)
                    .build();
            InsertOptions options = InsertOptions.builder()
                    .interceptor(ctx -> {
                        ctx.addHeader(CustomHeaderKey.REQUEST_ID_HEADER, "demo"); // pass the request id here
                    })
                    .build();
            InsertResponse response = skyflowClient.vault().insert(request, options);

            // Step 6: Print every field on each response record.
            for (InsertResponseRecord insertedRecord : response.getRecords()) {
                System.out.println("tableName:\t" + insertedRecord.getTableName());
                System.out.println("skyflowId:\t" + insertedRecord.getSkyflowId());
                System.out.println("tokens:\t\t" + insertedRecord.getTokens());
                System.out.println("data:\t\t" + insertedRecord.getData());
                System.out.println("hashedData:\t" + insertedRecord.getHashedData());
                System.out.println("httpCode:\t" + insertedRecord.getHttpCode());
                System.out.println("error:\t\t" + insertedRecord.getError());
//                System.out.println("request id " +insertedRecord.ge);
            }
        } catch (SkyflowException e) {
            // Step 7: Handle any errors that occur during the process
            System.err.println("Error in insert operation:\t" + e);
        }
    }
}
