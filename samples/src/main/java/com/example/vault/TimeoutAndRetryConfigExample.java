package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;

/**
 * This sample demonstrates how to configure HTTP timeout and retry behavior in the Skyflow Java SDK.
 *
 * <p>Configurable settings (all optional):
 * <ul>
 *   <li>{@code timeout}                  – overall call timeout in <b>seconds</b> (bounds the whole
 *       request including retries and backoff). Default: 60.</li>
 *   <li>{@code maxRetries}               – retry attempts after the first failure (retries on HTTP
 *       408 / 429 / 5xx). Default: 3.</li>
 *   <li>{@code initialRetryDelay}  – base backoff before the first retry, in <b>milliseconds</b>.
 *       Default: 500.</li>
 *   <li>{@code maxRetryDelay}      – cap on the (exponentially growing) backoff, in
 *       <b>milliseconds</b>. Default: 2000.</li>
 * </ul>
 *
 * <p><b>Two levels + precedence:</b> set client-wide defaults on {@code Skyflow.builder()}, and/or
 * per-vault overrides on {@code VaultConfig}. The most specific value wins, resolved per field:
 * <b>per-vault &rarr; client-wide &rarr; SDK default</b>.
 */
public class TimeoutAndRetryConfigExample {

    public static void main(String[] args) {
        try {
            // Step 1: Initialize credentials with the path to your service account key file
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            Credentials credentials = new Credentials();
            credentials.setPath(filePath);

            // Step 2: Configure the vault. Optionally override timeout/retry settings for THIS vault only.
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId("<YOUR_VAULT_ID>");
            vaultConfig.setClusterId("<YOUR_CLUSTER_ID>");
            vaultConfig.setEnv(Env.PROD);
            vaultConfig.setCredentials(credentials);
            // Per-vault overrides (optional). Any field left unset inherits the client-wide default below,
            // and then the SDK default.
            vaultConfig.setTimeout(30);                 // seconds  – tighter overall ceiling for this vault
            vaultConfig.setMaxRetries(2);               // fewer retries for this vault
            vaultConfig.setInitialRetryDelay(500L);
            vaultConfig.setMaxRetryDelay(1000L);

            // Step 3: Create the Skyflow client. Client-wide defaults apply to every vault
            //         unless that vault overrides them (as above).
            Skyflow skyflowClient = Skyflow.builder()
                    .setLogLevel(LogLevel.ERROR)
                    .timeout(60)                        // seconds  – client-wide overall call timeout
                    .maxRetries(3)                      // client-wide retry attempts
                    .initialRetryDelay(500L)      // client-wide base backoff (ms)
                    .maxRetryDelay(2000L)         // client-wide backoff cap (ms)
                    .addVaultConfig(vaultConfig)
                    .build();

            // Step 4: Use the client as usual. Requests now fail fast at the configured timeout and
            //         retry transient 408/429/5xx responses with exponential backoff + jitter.
            System.out.println("Skyflow client configured with custom timeout & retry settings: " + skyflowClient);

            // Example (uncomment and fill in a real request to try it):
            // DetokenizeResponse response = skyflowClient.vault().detokenize(detokenizeRequest);
            // System.out.println(response);
        } catch (SkyflowException e) {
            // Step 5: Handle any errors that occur during the process
            System.err.println("Error in Skyflow operations: " + e.getMessage());
        }
    }
}
