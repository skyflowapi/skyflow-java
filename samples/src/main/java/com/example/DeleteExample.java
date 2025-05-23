package com.example;

import com.skyflow.api.ApiClient;
import com.skyflow.api.core.ApiClientApiException;
import com.skyflow.api.core.RequestOptions;
import com.skyflow.api.resources.flowservice.FlowserviceClient;
import com.skyflow.api.resources.flowservice.requests.V1DeleteRequest;
import com.skyflow.api.types.V1DeleteResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.ArrayList;
import java.util.List;

/**
 * This example demonstrates how to use the Skyflow SDK to delete records from flowdb vault.
 * by specifying the vault configurations, credentials, and record IDs to delete.
 * <p>
 * Steps include:
 * 1. Setting up the auth client.
 * 2. Creating a Skyflow client.
 * 3. Setting up vault configurations.
 * 4. Creating a delete request with the vaultId, tableName, and skyflow IDs.
 * 5. Deleting records from the specified vault using record Skyflow IDs and table names.
 */

public class DeleteExample {

    public static void deleteRecords(FlowserviceClient client) {
        // Step 3: Setting up vault configurations.
        String vaultId = "<VAULT_ID>"; // Replace with the vault ID.
        String tableName = "<TABLE_NAME>"; // Replace with the table name in the vault.

        // List of Skyflow IDs to delete the record
        List<String> skyflowIDs = new ArrayList<>();
        skyflowIDs.add("<SKYFLOW_ID_1>"); // Replace with the record Skyflow ID to delete.
        skyflowIDs.add("<SKYFLOW_ID_2>");

        try {
            // Step 4: Creating a delete request with the vaultId, tableName, and skyflow IDs.
            V1DeleteRequest deleteRequest = V1DeleteRequest.builder()
                    .vaultId(vaultId)
                    .tableName(tableName)
                    .skyflowIDs(skyflowIDs)
                    .build();

            // Setting up request options
            RequestOptions requestOptions = RequestOptions.builder()
                    .timeout(5000) // Replace with the desired timeout in milliseconds.
                    .build();

            // Step 5: Deleting records from the specified vault using record Skyflow IDs and table names.
            V1DeleteResponse deleteResponse = client.delete(deleteRequest, requestOptions);
            System.out.println("Delete Response: " + deleteResponse);
        } catch (Exception ex) {
            System.out.println("Error during deleting the record: " + ex);
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

        // Call the deleteRecords method to delete records from the vault.
        deleteRecords(flowserviceClient);

    }
}
