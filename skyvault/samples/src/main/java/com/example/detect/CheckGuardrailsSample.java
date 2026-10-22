package com.example.detect;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.detect.CheckGuardrailsRequest;
import com.skyflow.detect.CheckGuardrailsResponse;
import com.skyflow.detect.GuardrailsValidation;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;

import java.util.Arrays;

/**
 * Screens text for toxicity and denied topics before it reaches an LLM.
 */
public class CheckGuardrailsSample {
    public static void main(String[] args) throws SkyflowException {
        Credentials credentials = new Credentials();
        credentials.setPath("<YOUR_CREDENTIALS_FILE_PATH>");   // Replace with the path to the credentials file

        VaultConfig vaultConfig = new VaultConfig();
        vaultConfig.setVaultId("<YOUR_VAULT_ID>");              // Replace with the ID of the vault
        vaultConfig.setClusterId("<YOUR_CLUSTER_ID>");          // Replace with the cluster ID of the vault
        vaultConfig.setEnv(Env.PROD);                           // Set the environment (e.g., DEV, STAGE, PROD)

        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.ERROR)
                .addVaultConfig(vaultConfig)
                .addSkyflowCredentials(credentials)
                .build();

        try {
            CheckGuardrailsResponse response = skyflowClient.detect().checkGuardrails(
                    CheckGuardrailsRequest.builder()
                            .text("Which candidate should I vote for in the upcoming election?")
                            .checkToxicity(true)                                   // server default is true when omitted
                            .denyTopics(Arrays.asList("politics", "medical advice"))
                            .build());
            System.out.println("Validation: " + response.getValidation()
                    + " toxic=" + response.getToxic() + " deniedTopic=" + response.getDeniedTopic());
            if (response.getValidation() == GuardrailsValidation.FAILED) {
                System.out.println("Blocked by guardrails");
            }
        } catch (SkyflowException e) {
            System.err.println("Error during checkGuardrails:");
            e.printStackTrace();
        }
    }
}
