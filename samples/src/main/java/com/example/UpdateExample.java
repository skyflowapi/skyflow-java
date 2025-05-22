package com.skyflow;

// Required imports for Skyflow API and HTTP client functionality
import com.skyflow.api.ApiClient;
import com.skyflow.api.resources.flowservice.requests.V1UpdateRequest;
import com.skyflow.api.types.V1UpdateRecordData;
import com.skyflow.api.types.V1UpdateResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example demonstrating record update operations using Skyflow API
 */
public class UpdateSample {
    public static void main(String[] args) {
        // Initialize HTTP client with Bearer token authentication for API requests
        // This client will automatically add the Bearer token to all requests
        OkHttpClient authClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request requestWithAuth = original.newBuilder()
                            .header("Authorization", "Bearer " + "<BEARER_TOKEN>")
                            .build();
                    return chain.proceed(requestWithAuth);
                })
                .build();
        
        // Initialize Skyflow API client with vault URL and authentication
        ApiClient client = ApiClient.builder().url("<VAULT_URL>").httpClient(authClient).build();

        // Create data structures for the records to be updated
        List<V1UpdateRecordData> list = new ArrayList<>();
        Map<String, Object> fields = new HashMap<>();

        // Add fields and values to be updated
        fields.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>");
        fields.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_1>");

        // Create update record object with new field values and Skyflow ID
        // Skyflow ID is the unique identifier for the record to be updated
        V1UpdateRecordData data = V1UpdateRecordData.builder()
                .data(fields)
                .skyflowId("<SKYFLOW_ID>")
                .build();
        list.add(data);

        // Build the update request with vault ID, table name and records
        V1UpdateRequest req = V1UpdateRequest.builder()
                .vaultId("<VAULT_ID>")
                .tableName("<TABLE_NAME>")
                .records(list)
                .build();

        // Execute the update operation and get the response
        V1UpdateResponse res = client.flowservice().update(req);

        // Print the operation result
        System.out.println(res.toString());
    }
}
