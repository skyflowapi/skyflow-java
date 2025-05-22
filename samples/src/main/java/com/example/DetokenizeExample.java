package com.skyflow;

// Required imports for Skyflow API and HTTP client functionality
import java.util.ArrayList;
import java.util.List;

import com.skyflow.api.ApiClient;
import com.skyflow.api.resources.flowservice.requests.V1DetokenizeRequest;
import com.skyflow.api.types.V1DetokenizeResponse;
import com.skyflow.api.types.V1TokenGroupRedactions;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Example demonstrating detokenization operations using Skyflow API
 */
public class DetokenizeSample {
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
        
        // Create Skyflow API client with vault URL and auth client
        ApiClient client = ApiClient.builder().url("<VAULT_URL>").httpClient(authClient).build();

        // Create a list to store tokens that need to be detokenized
        List<String> tokens  = new ArrayList<>();
        tokens.add("<TOKEN>");

        // Configure redaction settings for token groups
        List<V1TokenGroupRedactions> red = new ArrayList<>();
        // Create redaction configuration for specific token group
        V1TokenGroupRedactions r = V1TokenGroupRedactions.builder()
            .redaction("<REDACTION_TYPE>")
            .tokenGroupName("<TOKEN_GROUP_NAME>")
            .build();
        red.add(r);

        // Build the detokenize request with vault ID, tokens and redaction settings
        V1DetokenizeRequest req = V1DetokenizeRequest.builder()
            .vaultId("<VAULT_ID>")
            .tokens(tokens)
            .tokenGroupRedactions(red)
            .build();

        // Execute the detokenize operation and get the response
        V1DetokenizeResponse res = client.flowservice().detokenize(req);

        // Print the operation result
        System.out.println("response is" + res);
    }
}
