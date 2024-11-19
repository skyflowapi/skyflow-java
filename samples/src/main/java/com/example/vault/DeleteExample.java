package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.DeleteRequest;
import com.skyflow.vault.data.DeleteResponse;

import java.util.ArrayList;

public class DeleteExample {
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
        credentials.setPath("<YOUR_CREDENTIALS_FILE_PATH_2>");

        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.DEBUG)
                .addVaultConfig(blitzConfig)
                .addVaultConfig(stageConfig)
                .addSkyflowCredentials(skyflowCredentials)
                .build();

        ArrayList<String> ids1 = new ArrayList<>();
        ids1.add("<your_token_value>");
        DeleteRequest deleteRequest1 = DeleteRequest.builder().ids(ids1).table("<table_name>").build();
        DeleteResponse deleteResponse1 = skyflowClient.vault().delete(deleteRequest1);
        System.out.println(deleteResponse1);

        ArrayList<String> ids2 = new ArrayList<>();
        ids2.add("<your_token_value>");
        DeleteRequest deleteRequest2 = DeleteRequest.builder().ids(ids2).table("<table_name>").build();
        DeleteResponse deleteResponse2 = skyflowClient.vault("<your_vault_id_2>").delete(deleteRequest2);
        System.out.println(deleteResponse2);
    }
}
