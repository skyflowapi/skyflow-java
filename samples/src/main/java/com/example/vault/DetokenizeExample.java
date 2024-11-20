package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.DetokenizeResponse;

import java.util.ArrayList;

public class DetokenizeExample {
    public static void main(String[] args) throws SkyflowException {
        Credentials credentials = new Credentials();
        credentials.setPath("<YOUR_CREDENTIALS_FILE_PATH_1>");

        VaultConfig blitzConfig = new VaultConfig();
        blitzConfig.setVaultId("<YOUR_VAULT_ID_1>");
        blitzConfig.setClusterId("<YOUR_CLUSTER_ID_1>");
        blitzConfig.setEnv(Env.DEV);
        blitzConfig.setCredentials(credentials);

        VaultConfig stageConfig = new VaultConfig();
        stageConfig.setVaultId("<YOUR_VAULT_ID_2>");
        stageConfig.setClusterId("<YOUR_CLUSTER_ID_2>");
        stageConfig.setEnv(Env.STAGE);

        Credentials skyflowCredentials = new Credentials();
        skyflowCredentials.setPath("<YOUR_CREDENTIALS_FILE_PATH_2>");

        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)
                .addVaultConfig(blitzConfig)
                .addVaultConfig(stageConfig)
                .addSkyflowCredentials(skyflowCredentials)
                .build();

        ArrayList<String> tokens1 = new ArrayList<>();
        tokens1.add("<YOUR_TOKEN_VALUE_1>");
        tokens1.add("<YOUR_TOKEN_VALUE_2>");
        DetokenizeRequest detokenizeRequest1 = DetokenizeRequest.builder().tokens(tokens1).continueOnError(true).build();
        DetokenizeResponse detokenizeResponse1 = skyflowClient.vault().detokenize(detokenizeRequest1);
        System.out.println(detokenizeResponse1);

        ArrayList<String> tokens2 = new ArrayList<>();
        tokens2.add("<YOUR_TOKEN_VALUE_1>");
        tokens2.add("<YOUR_TOKEN_VALUE_2>");

        DetokenizeRequest detokenizeRequest2 = DetokenizeRequest.builder()
                .tokens(tokens2).continueOnError(false).redactionType(RedactionType.DEFAULT).build();
        DetokenizeResponse detokenizeResponse2 = skyflowClient.vault("<YOUR_VAULT_ID_2>").detokenize(detokenizeRequest2);
        System.out.println(detokenizeResponse2);
    }
}
