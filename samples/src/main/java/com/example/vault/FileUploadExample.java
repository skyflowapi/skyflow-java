package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.FileUploadRequest;
import com.skyflow.vault.data.FileUploadResponse;

import java.io.File;

/**
 * This example demonstrates how to use the Skyflow SDK to securely upload a file into a vault.
 *
 * It includes:
 * 1. Setting up vault configurations.
 * 2. Creating a Skyflow client.
 * 3. Uploading a file to the vault.
 * 4. Handling errors during the upload process.
 */
public class FileUploadExample {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Set up credentials for the vault configuration
        Credentials credentials = new Credentials();
        credentials.setApiKey("<YOUR_API_KEY>"); // Replace with the actual API key

        // Step 2: Configure the vault
        VaultConfig vaultConfig = new VaultConfig();
        vaultConfig.setVaultId("<VAULT_ID>");         // Replace with your vault ID
        vaultConfig.setClusterId("<CLUSTER_ID>");     // Replace with your vault cluster ID
        vaultConfig.setEnv(Env.PROD);                        // Set the environment (e.g., DEV, STAGE, SANDBOX)
        vaultConfig.setCredentials(credentials);             // Associate credentials with the vault configuration

        // Step 3: Set up general Skyflow client credentials
        Credentials skyflowCredentials = new Credentials();
        skyflowCredentials.setCredentialsString("<YOUR_CREDENTIALS_STRING>"); // Replace with the actual credentials string

        // Step 4: Create the Skyflow client and add vault configurations
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.ERROR)               // Set log level to ERROR to limit output
                .addVaultConfig(vaultConfig)        // Add the vault configuration
                .addSkyflowCredentials(skyflowCredentials) // Add general Skyflow credentials
                .build();

        // Example: Upload a file to the vault
        try {
            // Step 5: Specify the file to be uploaded
            File file = new File("<FILE_PATH>"); // Replace with the path to the file you want to upload

            // Step 6: Create the file upload request
            FileUploadRequest uploadFileRequest = FileUploadRequest.builder()
                    .fileObject(file)            // File object
                    .table("<SENSITIVE_TABLE_NAME>") // Vault table to upload into
                    .columnName("<COLUMN_NAME>")     // Column to assign to the uploaded file
                    .skyflowId("<SKYFLOW_ID>")       // Skyflow id of the record
                    .build();

            // Step 7: Perform the file upload operation
            FileUploadResponse fileUploadResponse = skyflowClient.vault().uploadFile(uploadFileRequest);
            System.out.println("File Upload Response: " + fileUploadResponse);

        } catch (SkyflowException e) {
            // Handle any errors that occur during the file upload process
            System.out.println("Error during file upload: " + e);
        }
    }
}
