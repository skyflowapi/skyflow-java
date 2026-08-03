package com.example.vault.deprecated;

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

import java.util.ArrayList;

/**
 * @deprecated Pre-v2.1 pattern. The {@code downloadURL()} builder method is deprecated.
 * Use {@code downloadUrl()} instead (see {@link com.example.vault.DetokenizeExample}).
 *
 * This example is retained for reference during the deprecation window.
 * {@code downloadURL()} still works but emits a runtime warning and will be removed in a future release.
 */
@Deprecated
public class DetokenizeExample {
    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Set up Skyflow credentials
        Credentials credentials = new Credentials();
        credentials.setToken("<YOUR_BEARER_TOKEN>"); // Replace with the actual bearer token

        // Step 2: Configure the vault
        VaultConfig vaultConfig = new VaultConfig();
        vaultConfig.setVaultId("<YOUR_VAULT_ID_1>");
        vaultConfig.setClusterId("<YOUR_CLUSTER_ID_1>");
        vaultConfig.setEnv(Env.PROD);
        vaultConfig.setCredentials(credentials);

        // Step 3: Set up credentials for the Skyflow client
        Credentials skyflowCredentials = new Credentials();
        skyflowCredentials.setCredentialsString("<YOUR_CREDENTIALS_STRING>");

        // Step 4: Create a Skyflow client
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.ERROR)
                .addVaultConfig(vaultConfig)
                .addSkyflowCredentials(skyflowCredentials)
                .build();

        // Step 5: Detokenize with deprecated downloadURL()
        // DEPRECATED: use downloadUrl(true) instead of downloadURL(true)
        try {
            ArrayList<DetokenizeData> detokenizeData = new ArrayList<>();
            detokenizeData.add(new DetokenizeData("<YOUR_TOKEN_VALUE_1>", RedactionType.MASKED));
            detokenizeData.add(new DetokenizeData("<YOUR_TOKEN_VALUE_2>"));

            DetokenizeRequest request = DetokenizeRequest.builder()
                    .detokenizeData(detokenizeData)
                    .continueOnError(true)
                    .downloadURL(true) // @deprecated — use downloadUrl(true)
                    .build();

            DetokenizeResponse response = skyflowClient.vault().detokenize(request);
            System.out.println("Detokenize Response: " + response);
        } catch (SkyflowException e) {
            System.out.println("Error during detokenization:");
            e.printStackTrace();
        }
    }
}
