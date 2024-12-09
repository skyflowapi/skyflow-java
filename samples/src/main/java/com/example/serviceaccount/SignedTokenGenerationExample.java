package com.example.serviceaccount;

import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.SignedDataTokenResponse;
import com.skyflow.serviceaccount.util.SignedDataTokens;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This example demonstrates how to generate Signed Data Tokens using two methods:
 * 1. Specifying the path to a credentials JSON file.
 * 2. Providing the credentials JSON as a string.
 * <p>
 * Signed data tokens are used to verify and securely transmit data with a specified context and TTL.
 */
public class SignedTokenGenerationExample {
    public static void main(String[] args) {
        List<SignedDataTokenResponse> signedTokenValues; // List to store signed data token responses

        // Example 1: Generate Signed Data Token using a credentials file path
        try {
            // Step 1: Specify the path to the service account credentials JSON file
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>"; // Replace with the actual file path

            // Step 2: Set the context and create the list of data tokens to be signed
            String context = "abc"; // Replace with your specific context (e.g., session identifier)
            ArrayList<String> dataTokens = new ArrayList<>();
            dataTokens.add("YOUR_DATA_TOKEN_1"); // Replace with your actual data token(s)

            // Step 3: Build the SignedDataTokens object
            SignedDataTokens signedToken = SignedDataTokens.builder()
                    .setCredentials(new File(filePath)) // Provide the credentials file
                    .setCtx(context)                   // Set the context for the token
                    .setTimeToLive(30)                 // Set the TTL (in seconds)
                    .setDataTokens(dataTokens)         // Set the data tokens to sign
                    .build();

            // Step 4: Retrieve and print the signed data tokens
            signedTokenValues = signedToken.getSignedDataTokens();
            System.out.println("Signed Tokens (using file path): " + signedTokenValues);
        } catch (SkyflowException e) {
            System.out.println("Error occurred while generating signed tokens using file path:");
            e.printStackTrace();
        }

        // Example 2: Generate Signed Data Token using credentials JSON as a string
        try {
            // Step 1: Provide the contents of the credentials JSON file as a string
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>"; // Replace with actual JSON content

            // Step 2: Set the context and create the list of data tokens to be signed
            String context = "abc"; // Replace with your specific context
            ArrayList<String> dataTokens = new ArrayList<>();
            dataTokens.add("YOUR_DATA_TOKEN_1"); // Replace with your actual data token(s)

            // Step 3: Build the SignedDataTokens object
            SignedDataTokens signedToken = SignedDataTokens.builder()
                    .setCredentials(fileContents)       // Provide the credentials as a string
                    .setCtx(context)                   // Set the context for the token
                    .setTimeToLive(30)                 // Set the TTL (in seconds)
                    .setDataTokens(dataTokens)         // Set the data tokens to sign
                    .build();

            // Step 4: Retrieve and print the signed data tokens
            signedTokenValues = signedToken.getSignedDataTokens();
            System.out.println("Signed Tokens (using credentials string): " + signedTokenValues);
        } catch (SkyflowException e) {
            System.out.println("Error occurred while generating signed tokens using credentials string:");
            e.printStackTrace();
        }
    }
}
