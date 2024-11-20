package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Byot;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateResponse;

import java.util.HashMap;

public class UpdateExample {
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

        HashMap<String, Object> values1 = new HashMap<>();
        values1.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>");
        values1.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>");

        HashMap<String, Object> tokens = new HashMap<>();
        tokens.put("<COLUMN_NAME_2>", "<TOKEN_VALUE_2>");

        UpdateRequest updateRequest1 = UpdateRequest.builder()
                .id("<YOUR_SKYFLOW_ID>")
                .table("<TABLE_NAME>")
                .tokenStrict(Byot.ENABLE)
                .values(values1)
                .tokens(tokens)
                .returnTokens(true)
                .build();
        UpdateResponse updateResponse1 = skyflowClient.vault().update(updateRequest1);
        System.out.println(updateResponse1);

        HashMap<String, Object> values2 = new HashMap<>();
        values2.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>");
        values2.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>");

        UpdateRequest updateRequest2 = UpdateRequest.builder()
                .id("<YOUR_SKYFLOW_ID>")
                .table("<TABLE_NAME>")
                .tokenStrict(Byot.DISABLE)
                .values(values2)
                .returnTokens(false)
                .build();
        UpdateResponse updateResponse2 = skyflowClient.vault("<YOUR_VAULT_ID_2>").update(updateRequest2);
        System.out.println(updateResponse2);
    }
}
