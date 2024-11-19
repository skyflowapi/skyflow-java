package com.example.vault;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.QueryResponse;

public class QueryExample {
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

        String query1 = "<your_sql_query>";
        QueryRequest queryRequest1 = QueryRequest.builder().query(query1).build();
        QueryResponse queryResponse1 = skyflowClient.vault().query(queryRequest1);
        System.out.println(queryResponse1);

        String query2 = "<your_sql_query>";
        QueryRequest queryRequest2 = QueryRequest.builder().query(query2).build();
        QueryResponse queryResponse2 = skyflowClient.vault("<your_vault_id_2>").query(queryRequest2);
        System.out.println(queryResponse2);

    }
}
