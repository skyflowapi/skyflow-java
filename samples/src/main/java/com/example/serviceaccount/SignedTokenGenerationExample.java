package com.example.serviceaccount;

import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.SignedDataTokenResponse;
import com.skyflow.serviceaccount.util.SignedDataTokens;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This example demonstrates how to generate Signed Data Tokens using:
 * 1. String context.
 * 2. JSON object context (Map) for conditional data access policies.
 * 3. Credentials string with context.
 */
public class SignedTokenGenerationExample {
    public static void main(String[] args) {
        List<SignedDataTokenResponse> signedTokenValues;

        // Example 1: Signed data tokens with string context
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            String context = "user_12345";
            ArrayList<String> dataTokens = new ArrayList<>();
            dataTokens.add("YOUR_DATA_TOKEN_1");

            SignedDataTokens signedToken = SignedDataTokens.builder()
                    .setCredentials(new File(filePath))
                    .setCtx(context)
                    .setTimeToLive(30)
                    .setDataTokens(dataTokens)
                    .build();

            signedTokenValues = signedToken.getSignedDataTokens();
            System.out.println("Signed Tokens (string context): " + signedTokenValues);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }

        // Example 2: Signed data tokens with JSON object context
        // Each key maps to a Skyflow CEL policy variable under request.context.*
        // For example: request.context.role == "analyst" && request.context.department == "research"
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("role", "analyst");
            ctx.put("department", "research");
            ctx.put("user_id", "user_67890");

            ArrayList<String> dataTokens = new ArrayList<>();
            dataTokens.add("YOUR_DATA_TOKEN_1");

            SignedDataTokens signedToken = SignedDataTokens.builder()
                    .setCredentials(new File(filePath))
                    .setCtx(ctx)
                    .setTimeToLive(30)
                    .setDataTokens(dataTokens)
                    .build();

            signedTokenValues = signedToken.getSignedDataTokens();
            System.out.println("Signed Tokens (object context): " + signedTokenValues);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }

        // Example 3: Signed data tokens from credentials string
        try {
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>";
            String context = "user_12345";
            ArrayList<String> dataTokens = new ArrayList<>();
            dataTokens.add("YOUR_DATA_TOKEN_1");

            SignedDataTokens signedToken = SignedDataTokens.builder()
                    .setCredentials(fileContents)
                    .setCtx(context)
                    .setTimeToLive(30)
                    .setDataTokens(dataTokens)
                    .build();

            signedTokenValues = signedToken.getSignedDataTokens();
            System.out.println("Signed Tokens (creds string): " + signedTokenValues);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }
    }
}
