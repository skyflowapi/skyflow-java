package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.CustomHeaderKey;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * This sample demonstrates the Skyflow Java SDK's unary getTokens operation — looking up the
 * deterministic token previously issued for each plaintext value. This makes exactly one API call
 * per invocation.
 *
 * Only deterministic token groups are supported. One record comes back per input entry, in request
 * order; duplicate inputs are not collapsed. Record-level failures are reported on the response,
 * whole-call failures are thrown as a SkyflowException.
 */
public class GetTokensExample {

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

            // Step 4: Prepare the value/token-group pairs to look up
            List<GetTokensRequestRecord> records = Arrays.asList(
                    GetTokensRequestRecord.builder()
                            .value("john@example.com")
                            .tokenGroupName("<YOUR_DETERMINISTIC_TOKEN_GROUP_NAME>")
                            .build(),
                    GetTokensRequestRecord.builder()
                            .value("unknown@example.com")
                            .tokenGroupName("<YOUR_DETERMINISTIC_TOKEN_GROUP_NAME>")
                            .build()
            );

            // Step 5: Build and execute the getTokens request
            GetTokensRequest request = GetTokensRequest.builder()
                    .records(records)
                    .build();

            GetTokensOptions options = GetTokensOptions.builder()
                    .interceptor(ctx -> {
                        ctx.addHeader(CustomHeaderKey.REQUEST_ID_HEADER, "GetTokensOptions"); // pass the request id here
                    })
                    .build();
            GetTokensResponse response = skyflowClient.vault().getTokens(request, options);

            // Step 6: Read the outcome. A record succeeded when its error is null.
            for (HashMap<String, Object> record : response.getRecords()) {
                if (record.get("error") == null) {
                    System.out.printf("getTokens: %s -> %s%n", record.get("value"), record.get("token"));
                } else {
                    System.out.printf("getTokens failed (%s): %s%n", record.get("httpCode"), record.get("error"));
                }
            }
        } catch (SkyflowException e) {
            // Step 7: Handle any errors that occur during the process
            System.err.println("Error in getTokens operation:\t" + e);
        }
    }
}
