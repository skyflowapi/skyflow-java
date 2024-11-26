package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Byot;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;

import java.util.ArrayList;
import java.util.HashMap;

public class InsertExample {
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

        ArrayList<HashMap<String, Object>> values1 = new ArrayList<>();
        HashMap<String, Object> value1 = new HashMap<>();
        value1.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>");
        value1.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>");
        values1.add(value1);

        ArrayList<HashMap<String, Object>> tokens = new ArrayList<>();
        HashMap<String, Object> token = new HashMap<>();
        token.put("<COLUMN_NAME_2>", "<TOKEN_VALUE_2>");
        tokens.add(token);

        InsertRequest insertRequest = InsertRequest.builder()
                .table("<TABLE_NAME>")
                .continueOnError(true)
                .tokenStrict(Byot.ENABLE)
                .values(values1)
                .tokens(tokens)
                .returnTokens(true)
                .build();
        InsertResponse insertResponse = skyflowClient.vault().insert(insertRequest);
        System.out.println(insertResponse);

        ArrayList<HashMap<String, Object>> values2 = new ArrayList<>();
        HashMap<String, Object> value2 = new HashMap<>();
        value2.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>");
        value2.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>");
        values2.add(value2);

        InsertRequest upsertRequest = InsertRequest.builder()
                .table("<TABLE_NAME>")
                .continueOnError(false)
                .tokenStrict(Byot.DISABLE)
                .values(values2)
                .returnTokens(false)
                .upsert("<UPSERT_COLUMN>")
                .build();
        InsertResponse upsertResponse = skyflowClient.vault("<YOUR_VAULT_ID_2>").insert(upsertRequest);
        System.out.println(upsertResponse);
    }
}
