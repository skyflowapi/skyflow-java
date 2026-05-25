package com.example.vault.deprecated;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @deprecated Pre-v2.1 pattern. The {@code "skyflow_id"} key in the response record map is deprecated.
 * Use {@code "skyflowId"} instead (see {@link com.example.vault.GetExample}).
 *
 * This example is retained for reference during the deprecation window.
 * Both {@code "skyflow_id"} and {@code "skyflowId"} are present in the response map until
 * {@code "skyflow_id"} is removed in a future release.
 */
@Deprecated
public class GetExample {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Set up credentials
        Credentials credentials = new Credentials();
        credentials.setCredentialsString("<YOUR_CREDENTIALS_STRING_1>");

        // Step 2: Configure the vault
        VaultConfig vaultConfig = new VaultConfig();
        vaultConfig.setVaultId("<YOUR_VAULT_ID_1>");
        vaultConfig.setClusterId("<YOUR_CLUSTER_ID_1>");
        vaultConfig.setEnv(Env.PROD);
        vaultConfig.setCredentials(credentials);

        // Step 3: Set up credentials for the Skyflow client
        Credentials skyflowCredentials = new Credentials();
        skyflowCredentials.setCredentialsString("<YOUR_CREDENTIALS_STRING_2>");

        // Step 4: Create a Skyflow client
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.ERROR)
                .addVaultConfig(vaultConfig)
                .addSkyflowCredentials(skyflowCredentials)
                .build();

        // Example: Fetch records and read the Skyflow ID using the deprecated "skyflow_id" key
        // DEPRECATED: the response map contains both "skyflow_id" and "skyflowId".
        //             Access "skyflowId" instead — "skyflow_id" will be removed in a future release.
        try {
            ArrayList<String> ids = new ArrayList<>();
            ids.add("<YOUR_SKYFLOW_ID>");

            GetRequest request = GetRequest.builder()
                    .ids(ids)
                    .table("<TABLE_NAME>")
                    .redactionType(RedactionType.PLAIN_TEXT)
                    .build();

            GetResponse response = skyflowClient.vault().get(request);

            // DEPRECATED: reading "skyflow_id" from the response map
            for (HashMap<String, Object> record : response.getData()) {
                String deprecatedId = (String) record.get("skyflow_id"); // @deprecated — use "skyflowId"
                String preferredId  = (String) record.get("skyflowId");  // preferred
                System.out.println("skyflow_id (deprecated): " + deprecatedId);
                System.out.println("skyflowId  (preferred) : " + preferredId);
            }
        } catch (SkyflowException e) {
            System.out.println("Error during fetch:");
            e.printStackTrace();
        }
    }
}
