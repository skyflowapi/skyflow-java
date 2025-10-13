package com.example.serviceaccount;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.tokens.DetokenizeData;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.DetokenizeResponse;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.ArrayList;

/**
 * This example demonstrates how to configure and use the Skyflow SDK
 * to detokenize sensitive data stored in a Skyflow vault.
 * It includes setting up credentials, configuring the vault, and
 * making a detokenization request. The code also implements a retry
 * mechanism to handle unauthorized access errors (HTTP 401).
 */
public class BearerTokenExpiryExample {
    public static void main(String[] args) {
        try {
            // Setting up credentials for accessing the Skyflow vault
            Credentials vaultCredentials = new Credentials();
            vaultCredentials.setCredentialsString("<YOUR_CREDENTIALS_STRING>");

            // Configuring the Skyflow vault with necessary details
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId("<YOUR_VAULT_ID>"); // Vault ID
            vaultConfig.setClusterId("<YOUR_CLUSTER_ID>"); // Cluster ID
            vaultConfig.setEnv(Env.PROD); // Environment (e.g., DEV, PROD)
            vaultConfig.setCredentials(vaultCredentials); // Setting credentials

            // Creating a Skyflow client instance with the configured vault
            Skyflow skyflowClient = Skyflow.builder()
                    .setLogLevel(LogLevel.ERROR) // Setting log level to ERROR
                    .addVaultConfig(vaultConfig) // Adding vault configuration
                    .build();

            // Attempting to detokenize data using the Skyflow client
            try {
                detokenizeData(skyflowClient);
            } catch (SkyflowException e) {
                // Retry detokenization if the error is due to unauthorized access (HTTP 401)
                if (e.getHttpCode() == 401) {
                    detokenizeData(skyflowClient);
                } else {
                    // Rethrow the exception for other error codes
                    throw e;
                }
            }
        } catch (SkyflowException e) {
            // Handling any exceptions that occur during the process
            System.out.println("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Method to detokenize data using the Skyflow client.
     * It sends a detokenization request with a list of tokens and prints the response.
     *
     * @param skyflowClient The Skyflow client instance used for detokenization.
     * @throws SkyflowException If an error occurs during the detokenization process.
     */
    public static void detokenizeData(Skyflow skyflowClient) throws SkyflowException {
        // Creating a list of tokens to be detokenized
        DetokenizeData detokenizeDataToken1 = new DetokenizeData("<YOUR_TOKEN_VALUE_1>", RedactionType.PLAIN_TEXT);
        DetokenizeData detokenizeDataToken2 = new DetokenizeData("<YOUR_TOKEN_VALUE_2>");
        ArrayList<DetokenizeData> detokenizeDataList = new ArrayList<>();
        detokenizeDataList.add(detokenizeDataToken1); // First token
        detokenizeDataList.add(detokenizeDataToken2); // Second token

        // Building a detokenization request with the token list and configuration
        DetokenizeRequest detokenizeRequest = DetokenizeRequest.builder()
                .detokenizeData(detokenizeDataList) // Adding tokens to the request
                .continueOnError(false) // Stop on error
                .build();

        // Sending the detokenization request and receiving the response
        DetokenizeResponse detokenizeResponse = skyflowClient.vault().detokenize(detokenizeRequest);

        // Printing the detokenized response
        System.out.println(detokenizeResponse);
    }
}
