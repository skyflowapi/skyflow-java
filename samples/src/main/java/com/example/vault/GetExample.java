package com.example.vault;

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

public class GetExample {
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

        ArrayList<String> ids = new ArrayList<>();
        ids.add("<your_skyflow_id>");
        GetRequest getByIdRequest = GetRequest.builder().returnTokens(true).ids(ids).table("<table_name>").build();
        GetResponse getByIdResponse = skyflowClient.vault().get(getByIdRequest);
        System.out.println(getByIdResponse);

        ArrayList<String> columnValues = new ArrayList<>();
        columnValues.add("<your_skyflow_id>");
        GetRequest getByColumnRequest = GetRequest.builder()
                .table("<table_name>")
                .columnName("<column_name>")
                .columnValues(columnValues)
                .redactionType(RedactionType.PLAIN_TEXT)
                .build();
        GetResponse getByColumnResponse = skyflowClient.vault("<your_vault_id_2>").get(getByColumnRequest);
        System.out.println(getByColumnResponse);
    }
}
