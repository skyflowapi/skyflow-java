package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.CustomHeaderKey;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This sample demonstrates the Skyflow Java SDK's unary uploadFiles operation — uploading files
 * into the file columns of new or existing records.
 *
 * One call runs two steps inside the SDK: it asks the vault for a signed upload URL per column,
 * then PUTs each file's bytes to its URL. A record that fails in the first step has its columns
 * SKIPPED; a column whose upload fails is FAILED and leaves the rest unaffected. Whole-call
 * failures are thrown as a SkyflowException.
 */
public class UploadFilesExample {

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

            // Step 4: Describe the files to upload. Omit skyflowId to create a new record.
            List<UploadFilesRequestRecord> records = Arrays.asList(
                    UploadFilesRequestRecord.builder()
                            .tableName("<YOUR_TABLE_NAME>")
                            .columns(Arrays.asList(
                                    UploadFilesRequestColumn.builder()
                                            .column("<YOUR_FILE_COLUMN>")
                                            .filePath("<PATH_TO_FILE>")
                                            .build(),
                                    UploadFilesRequestColumn.builder()
                                            .column("<ANOTHER_FILE_COLUMN>")
                                            .base64("<BASE64_FILE_CONTENT>")
                                            .fileName("<FILE_NAME>") // required with base64
                                            .build()))
                            .build(),
                    UploadFilesRequestRecord.builder()
                            .tableName("<YOUR_TABLE_NAME>")
                            .skyflowId("<EXISTING_SKYFLOW_ID>")
                            .columns(Collections.singletonList(
                                    UploadFilesRequestColumn.builder()
                                            .column("<YOUR_FILE_COLUMN>")
                                            .fileObject(new File("<PATH_TO_FILE>"))
                                            .build()))
                            .build()
            );

            // Step 5: Build and execute the uploadFiles request
            UploadFilesRequest request = UploadFilesRequest.builder()
                    .records(records)
                    .build();

            UploadFilesOptions options = UploadFilesOptions.builder()
                    .interceptor(ctx -> {
                        ctx.addHeader(CustomHeaderKey.REQUEST_ID_HEADER, "UploadFilesOptions"); // pass the request id here
                    })
                    .build();
            UploadFilesResponse response = skyflowClient.vault().uploadFiles(request, options);

            // Step 6: Read the outcome per column. A record's error stays null when only some of
            // its columns failed to upload.
            for (HashMap<String, Object> record : response.getRecords()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> columns = (List<Map<String, Object>>) record.get("columns");
                for (Map<String, Object> column : columns) {
                    System.out.printf("uploadFiles: %s.%s -> %s%s%n", record.get("skyflowId"), column.get("column"),
                            column.get("uploadStatus"), column.get("error") == null ? "" : " (" + column.get("error") + ")");
                }
            }
        } catch (SkyflowException e) {
            // Step 7: Handle any errors that occur during the process
            System.err.println("Error in uploadFiles operation:\t" + e);
        }
    }
}
