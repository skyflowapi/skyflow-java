package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.tokens.ColumnValue;
import com.skyflow.vault.tokens.TokenizeRequest;
import com.skyflow.vault.tokens.TokenizeResponse;

import java.util.ArrayList;

public class TokenizeExample {
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

        ArrayList<ColumnValue> columnValues1 = new ArrayList<>();
        ColumnValue value1 = ColumnValue.builder().value("<VALUE>").columnGroup("<COLUMN_GROUP>").build();
        ColumnValue value2 = ColumnValue.builder().value("<VALUE>").columnGroup("<COLUMN_GROUP>").build();
        columnValues1.add(value1);
        columnValues1.add(value2);
        TokenizeRequest tokenizeRequest1 = TokenizeRequest.builder().values(columnValues1).build();
        TokenizeResponse tokenizeResponse1 = skyflowClient.vault().tokenize(tokenizeRequest1);
        System.out.println(tokenizeResponse1);

        ArrayList<ColumnValue> columnValues2 = new ArrayList<>();
        ColumnValue value3 = ColumnValue.builder().value("<VALUE>").columnGroup("<COLUMN_GROUP>").build();
        ColumnValue value4 = ColumnValue.builder().value("<VALUE>").columnGroup("<COLUMN_GROUP>").build();
        columnValues2.add(value3);
        columnValues2.add(value4);
        TokenizeRequest tokenizeRequest2 = TokenizeRequest.builder().values(columnValues2).build();
        TokenizeResponse tokenizeResponse2 = skyflowClient.vault("<YOUR_VAULT_ID_2>").tokenize(tokenizeRequest2);
        System.out.println(tokenizeResponse2);
    }
}
