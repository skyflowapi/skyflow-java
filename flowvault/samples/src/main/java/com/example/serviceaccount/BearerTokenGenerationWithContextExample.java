package com.example.serviceaccount;

import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.BearerToken;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Example program to generate a Bearer Token using Skyflow's BearerToken utility.
 * The token is generated using three approaches:
 * 1. By providing a string context.
 * 2. By providing a JSON object context (Map) for conditional data access policies.
 * 3. By providing the credentials as a string with context.
 */
public class BearerTokenGenerationWithContextExample {
    public static void main(String[] args) {
        String bearerToken = null;

        // Approach 1: Bearer token with string context
        // Use a simple string identifier when your policy references a single context value.
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            BearerToken token = BearerToken.builder()
                    .setCredentials(new File(filePath))
                    .setCtx("user_12345")
                    .build();

            bearerToken = token.getBearerToken();
            System.out.println("Bearer token (string context): " + bearerToken);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }

        // Approach 2: Bearer token with JSON object context
        // Use a structured Map when your policy needs multiple context values.
        // Each key maps to a Skyflow CEL policy variable under request.context.*
        // For example, the map below enables policies like:
        //   request.context.role == "admin" && request.context.department == "finance"
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("role", "admin");
            ctx.put("department", "finance");
            ctx.put("user_id", "user_12345");

            BearerToken token = BearerToken.builder()
                    .setCredentials(new File(filePath))
                    .setCtx(ctx)
                    .build();

            bearerToken = token.getBearerToken();
            System.out.println("Bearer token (object context): " + bearerToken);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }

        // Approach 3: Bearer token with string context from credentials string
        try {
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>";
            BearerToken token = BearerToken.builder()
                    .setCredentials(fileContents)
                    .setCtx("user_12345")
                    .build();

            bearerToken = token.getBearerToken();
            System.out.println("Bearer token (creds string): " + bearerToken);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }
    }
}
