package com.example.serviceaccount;

import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.BearerToken;
import com.skyflow.serviceaccount.util.Token;

import java.io.File;

/**
 * Example program to generate a Bearer Token using Skyflow's BearerToken utility.
 * The token can be generated in two ways:
 * 1. Using the file path to a credentials.json file.
 * 2. Using the JSON content of the credentials file as a string.
 */
public class BearerTokenGenerationExample {
    public static void main(String[] args) {
        // Variable to store the generated token
        String token = null;

        // Example 1: Generate Bearer Token using a credentials.json file
        try {
            // Specify the full file path to the credentials.json file
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";

            // Check if the token is either not initialized or has expired
            if (Token.isExpired(token)) {
                // Create a BearerToken object using the credentials file
                BearerToken bearerToken = BearerToken.builder()
                        .setCredentials(new File(filePath)) // Set credentials from the file path
                        .build();

                // Generate a new Bearer Token
                token = bearerToken.getBearerToken();
            }

            // Print the generated Bearer Token to the console
            System.out.println("Generated Bearer Token (from file): " + token);
        } catch (SkyflowException e) {
            // Handle any exceptions encountered during the token generation process
            e.printStackTrace();
        }

        // Example 2: Generate Bearer Token using the credentials JSON as a string
        try {
            // Provide the credentials JSON content as a string
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>";

            // Check if the token is either not initialized or has expired
            if (Token.isExpired(token)) {
                // Create a BearerToken object using the credentials string
                BearerToken bearerToken = BearerToken.builder()
                        .setCredentials(fileContents) // Set credentials from the string
                        .build();

                // Generate a new Bearer Token
                token = bearerToken.getBearerToken();
            }

            // Print the generated Bearer Token to the console
            System.out.println("Generated Bearer Token (from string): " + token);
        } catch (SkyflowException e) {
            // Handle any exceptions encountered during the token generation process
            e.printStackTrace();
        }
    }
}
