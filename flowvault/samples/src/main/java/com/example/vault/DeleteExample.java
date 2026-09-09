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
import java.util.List;

/**
 * This sample demonstrates the Skyflow Java SDK's unary delete operation — deleting records by
 * skyflowId or unique value. This makes exactly one API call per invocation: there is no internal
 * batching or concurrency to configure.
 *
 * Distinct from deleteTokens/bulkDeleteTokens, which remove tokens only and leave the underlying
 * record in place.
 */
public class DeleteExample {

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

            // Step 4: Prepare the skyflow IDs to delete.
            //         Either ids or uniqueValues is required; specifying both fails validation.
            List<String> ids = new ArrayList<>();
            ids.add("<YOUR_SKYFLOW_ID>");

            // Step 5: Build and execute the delete request
            DeleteRequest request = DeleteRequest.builder()
                    .tableName("<YOUR_TABLE_NAME>")
                    .skyflowIds(ids)
                    .build();

            DeleteOptions options = DeleteOptions.builder()
                    .interceptor(ctx -> {
                        ctx.addHeader(CustomHeaderKey.REQUEST_ID_HEADER, "DeleteOptions"); // pass the request id here
                    })
                    .build();
            DeleteResponse response = skyflowClient.vault().delete(request, options);

            // Step 6: Read the outcome. A record succeeded when its error is null.
            for (DeleteResponseRecord record : response.getRecords()) {
                if (record.getError() == null) {
                    System.out.printf("delete: skyflowId=%s removed%n", record.getSkyflowId());
                } else {
                    System.out.printf("delete failed (%d): %s%n", record.getHttpCode(), record.getError());
                }
            }
        } catch (SkyflowException e) {
            // Step 7: Handle any errors that occur during the process
            System.err.println("Error in delete operation:\t" + e);
        }
    }
}
