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
        credentials.setPath("<path_to_your_credentials_file_1>");

        VaultConfig blitzConfig = new VaultConfig();
        blitzConfig.setVaultId("<your_vault_id_1>");
        blitzConfig.setClusterId("<your_cluster_id_1>");
        blitzConfig.setEnv(Env.DEV);
        blitzConfig.setCredentials(credentials);

        VaultConfig stageConfig = new VaultConfig();
        stageConfig.setVaultId("<your_vault_id_2>");
        stageConfig.setClusterId("<your_cluster_id_2>");
        stageConfig.setEnv(Env.STAGE);

        Credentials skyflowCredentials = new Credentials();
        credentials.setPath("<path_to_your_credentials_file_2>");

        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)
                .addVaultConfig(blitzConfig)
                .addVaultConfig(stageConfig)
                .addSkyflowCredentials(skyflowCredentials)
                .build();

        HashMap<String, Object> values1 = new HashMap<>();
        values1.put("<column_name_1>", "<column_value_1>");
        values1.put("<column_name_2>", "<column_value_1>");

        HashMap<String, Object> tokens = new HashMap<>();
        tokens.put("<column_name_2>", "<token_value_2>");

        UpdateRequest updateRequest1 = UpdateRequest.builder()
                .id("<your_skyflow_id")
                .table("<table_name>")
                .tokenStrict(Byot.ENABLE)
                .values(values1)
                .tokens(tokens)
                .returnTokens(true)
                .build();
        UpdateResponse updateResponse1 = skyflowClient.vault().update(updateRequest1);
        System.out.println(updateResponse1);

        HashMap<String, Object> values2 = new HashMap<>();
        values2.put("<column_name_1>", "<column_value_1>");
        values2.put("<column_name_2>", "<column_value_1>");

        UpdateRequest updateRequest2 = UpdateRequest.builder()
                .id("<your_skyflow_id")
                .table("<table_name>")
                .tokenStrict(Byot.DISABLE)
                .values(values2)
                .returnTokens(false)
                .build();
        UpdateResponse updateResponse2 = skyflowClient.vault("<your_vault_id_2>").update(updateRequest2);
        System.out.println(updateResponse2);
    }
}
