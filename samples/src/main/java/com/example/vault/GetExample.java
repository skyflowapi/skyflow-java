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

        ArrayList<String> ids = new ArrayList<>();
        ids.add("<YOUR_SKYFLOW_ID>");
        GetRequest getByIdRequest = GetRequest.builder().returnTokens(true).ids(ids).table("<TABLE_NAME>").build();
        GetResponse getByIdResponse = skyflowClient.vault().get(getByIdRequest);
        System.out.println(getByIdResponse);

        ArrayList<String> columnValues = new ArrayList<>();
        columnValues.add("<YOUR_COLUMN_VALUE>");
        GetRequest getByColumnRequest = GetRequest.builder()
                .table("<TABLE_NAME>")
                .columnName("<COLUMN_NAME>")
                .columnValues(columnValues)
                .redactionType(RedactionType.PLAIN_TEXT)
                .build();
        GetResponse getByColumnResponse = skyflowClient.vault("<YOUR_VAULT_ID_2>").get(getByColumnRequest);
        System.out.println(getByColumnResponse);
    }
}
