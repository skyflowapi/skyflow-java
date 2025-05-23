package com.example;

import com.skyflow.api.ApiClient;
import com.skyflow.api.core.ApiClientApiException;
import com.skyflow.api.core.RequestOptions;
import com.skyflow.api.resources.flowservice.FlowserviceClient;
import com.skyflow.api.resources.flowservice.requests.V1GetRequest;
import com.skyflow.api.types.V1ColumnRedactions;
import com.skyflow.api.types.V1GetResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.ArrayList;
import java.util.List;

/**
 * This example demonstrates how to use the Skyflow SDK to securely get records from flowdb vault based on the Skyflow Ids & column values.
 * It includes:
 * 1. Setting up the auth client.
 * 2. Creating a Skyflow client.
 * 3. Setting up vault configurations.
 * 4. Creating a get request with the vaultId, tableName, and skyflowIDs.
 * 5. Getting records using Skyflow IDs and column values.
 */

public class GetExample {

    // Example 1: Get records by Skyflow IDs from the flowdb vault.
    public static void getRecordsById(FlowserviceClient client, String vaultId, String tableName,
                                      List<String> skyflowIDs, Integer limit, Integer offset, RequestOptions requestOptions) {
        try {
            // Step 4: Creating a get request with the vaultId, tableName, and skyflowIDs.
            V1GetRequest getRequest = V1GetRequest.builder()
                    .vaultId(vaultId)
                    .tableName(tableName)
                    .skyflowIDs(skyflowIDs)
                    .limit(limit)
                    .offset(offset)
                    .build();

            // Step 5: Getting records using Skyflow IDs
            V1GetResponse records = client.get(getRequest, requestOptions);
            System.out.println("Get Response by id: " + records);
        } catch (Exception ex) {
            System.out.println("Error during get by id: " + ex);
            ex.printStackTrace();
        }
    }

    // Example 2: Get records by column values & column redaction from the flowdb vault.
    public static void getRecordsByColumnValues(FlowserviceClient client, String vaultId, String tableName,
                                                List<String> skyflowIDs, Integer limit, Integer offset, RequestOptions requestOptions) {
        try {
            // Creating a list of column values to get the records.
            List<String> columnValues = new ArrayList<>();
            columnValues.add("<COLUMN_1>"); // Replace with the column name present in the table.
            columnValues.add("<COLUMN_2>");

            // (Optional) Creating a list of column redactions to get the records in redacted format.
            List<V1ColumnRedactions> columnRedactions = new ArrayList<>();
            V1ColumnRedactions column1Redaction = V1ColumnRedactions.builder()
                    .redaction("<COLUMN_NAME_1_REDACTION>") // Replace with the redaction.
                    .columnName("<COLUMN_NAME_1>") // Replace with the column name present in the table.
                    .build();

            V1ColumnRedactions column2Redaction = V1ColumnRedactions.builder()
                    .redaction("<COLUMN_NAME_2_REDACTION>") // Replace with the redaction.
                    .columnName("<COLUMN_NAME_2>") // Replace with the column name present in the table.
                    .build();

            columnRedactions.add(column1Redaction);
            columnRedactions.add(column2Redaction);

            // Step 4: Get request with column values and column redactions.
            V1GetRequest getRecordsByColumnValuesRequest = V1GetRequest.builder()
                    .vaultId(vaultId)
                    .skyflowIDs(skyflowIDs)
                    .tableName(tableName)
                    .columnRedactions(columnRedactions)
                    .columns(columnValues)
                    .limit(limit)
                    .offset(offset)
                    .build();

            // Step 5: Getting records using column values.
            V1GetResponse records = client.get(getRecordsByColumnValuesRequest, requestOptions);
            System.out.println("Get Response by column values: " + records);
        } catch (Exception ex) {
            System.out.println("Error during get by column values: " + ex);
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Step 1: Setting up the auth client.
        String token = "<BEARER_TOKEN>"; //  Replace with the actual bearer token.
        OkHttpClient authClient = new OkHttpClient.Builder().addInterceptor(chain -> {
            Request original = chain.request();
            Request requestWithAuth = original.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
            return chain.proceed(requestWithAuth);
        }).build();

        // Step 2: Creating a Skyflow client.
        ApiClient skyflowApiClient = ApiClient.builder()
                .url("<VAULT_URL>") // Replace with the vault URL.
                .httpClient(authClient)
                .build();
        FlowserviceClient flowserviceClient = skyflowApiClient.flowservice();

        // Step 3: Setting up vault configurations.
        String vaultId = "<VAULT_ID>"; // Replace with the vault ID.
        String tableName = "<TABLE_NAME>"; // Replace with the table name in the vault.

        // List of Skyflow IDs to get the record.
        List<String> skyflowIDs = new ArrayList<>();
        skyflowIDs.add("<SKYFLOW_ID_1>"); // Replace with the record Skyflow ID
        skyflowIDs.add("<SKYFLOW_ID_2>");

        // Setting up request options
        RequestOptions requestOptions = RequestOptions.builder()
                .timeout(5000) // Replace with the desired timeout in milliseconds.
                .build();

        // The limit and offset are optional parameters.
        Integer limit = 5; // Replace with the desired limit.
        Integer offset = 0; // Replace with the desired offset.


        // Call the getRecordsById and getRecordsByColumnValues methods to get records from the vault.
        getRecordsById(flowserviceClient, vaultId, tableName, skyflowIDs, limit, offset, requestOptions);
        getRecordsByColumnValues(flowserviceClient, vaultId, tableName, skyflowIDs, limit, offset, requestOptions);
    }
}
