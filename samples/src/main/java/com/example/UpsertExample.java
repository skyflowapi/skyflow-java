package com.skyflow;

// Required imports for Skyflow API and HTTP client functionality
import com.skyflow.api.ApiClient;
import com.skyflow.api.resources.flowservice.requests.V1InsertRequest;
import com.skyflow.api.types.V1InsertRecordData;
import com.skyflow.api.types.V1InsertResponse;
import com.skyflow.api.types.V1Upsert;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import com.skyflow.api.types.FlowEnumUpdateType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example demonstrating upsert (insert or update) operations using Skyflow API
 */
public class UpsertExample {
    public static void main(String[] args) {
        // Set up HTTP client with authentication
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
        
        // Create Skyflow API client instance with vault URL and auth client
        ApiClient client = ApiClient.builder()
                .url("<VAULT_URL>")
                .httpClient(authClient)
                .build();

        // Initialize data structures for the records to be upserted
        List<V1InsertRecordData> list = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        
        // Add record fields and their corresponding values
        // These will be either inserted as new or used to update existing records
        map.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>");
        map.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_1>");
        
        // Create a record object with the field data and add to list
        V1InsertRecordData data = V1InsertRecordData.builder()
                .data(map)
                .build();
        list.add(data);

        // Specify which columns should be used to determine if a record exists
        // These columns will be used as unique identifiers for upsert operation
        List<String> upsertColumns = new ArrayList<>();
        upsertColumns.add("email");
        upsertColumns.add("name");
        V1Upsert upsert = V1Upsert.builder().uniqueColumns(upsertColumns).updateType(FlowEnumUpdateType.UPDATE).build();

        
        // Build the complete upsert request
        V1InsertRequest req = V1InsertRequest.builder()
            .vaultId("<VAULT_ID>")
            .tableName("<TABLE_NAME>")
            .records(list)
            .upsert(upsert)
            .build();

        // Execute the upsert operation and get the response
        V1InsertResponse res = client.flowservice().insert(req);

        // Print the operation result
        System.out.println(res.toString());
    }
}
