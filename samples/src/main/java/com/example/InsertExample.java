package com.skyflow;

import com.skyflow.api.ApiClient;
import com.skyflow.api.resources.flowservice.requests.V1InsertRequest;
import com.skyflow.api.types.V1InsertRecordData;
import com.skyflow.api.types.V1InsertResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InsertSample {
    public static void main(String[] args) {
        // Initialize HTTP client with Bearer token authentication
        OkHttpClient authClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request requestWithAuth = original.newBuilder()
                            .header("Authorization", "Bearer " + "<BEARER_TOKEN>")
                            .build();
                    return chain.proceed(requestWithAuth);
                })
                .build();
        
        // Create Skyflow API client with vault URL and auth client
        ApiClient client = ApiClient.builder().url("<VAULT_URL>").httpClient(authClient).build();

        // Prepare data for insertion
        List<V1InsertRecordData> list = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        
        // Add record fields to be inserted
        map.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>");
        map.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_1>");
        
        // Create record data object and add to list
        V1InsertRecordData data = V1InsertRecordData.builder().data(map).build();
        list.add(data);
        
        // Build insert request with vault ID, table name and records
        V1InsertRequest req = V1InsertRequest.builder()
            .vaultId("<VAULT_ID>")
            .tableName("<TABLE_NAME>")
            .records(list)
            .build();

        // Execute insert operation and print response
        V1InsertResponse res = client.flowservice().insert(req);
        System.out.println(res.toString());
    }
}
