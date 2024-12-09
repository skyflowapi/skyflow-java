package com.example.serviceaccount;

import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.BearerToken;

import java.io.File;

/**
 * Example program to generate a Bearer Token using Skyflow's BearerToken utility.
 * The token is generated using two approaches:
 * 1. By providing the credentials.json file path.
 * 2. By providing the contents of credentials.json as a string.
 */
public class BearerTokenGenerationWithContextExample {
    public static void main(String[] args) {
        // Variable to store the generated Bearer Token
        String bearerToken = null;

        // Approach 1: Generate Bearer Token by specifying the path to the credentials.json file
        try {
            // Replace <YOUR_CREDENTIALS_FILE_PATH> with the full path to your credentials.json file
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";

            // Create a BearerToken object using the file path
            BearerToken token = BearerToken.builder()
                    .setCredentials(new File(filePath)) // Set credentials using a File object
                    .setCtx("abc") // Set context string (example: "abc")
                    .build(); // Build the BearerToken object

            // Retrieve the Bearer Token as a string
            bearerToken = token.getBearerToken();

            // Print the generated Bearer Token to the console
            System.out.println(bearerToken);
        } catch (SkyflowException e) {
            // Handle exceptions specific to Skyflow operations
            e.printStackTrace();
        }

        // Approach 2: Generate Bearer Token by specifying the contents of credentials.json as a string
        try {
            // Replace <YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING> with the actual contents of your credentials.json file
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>";

            // Create a BearerToken object using the file contents as a string
            BearerToken token = BearerToken.builder()
                    .setCredentials(fileContents) // Set credentials using a string representation of the file
                    .setCtx("abc") // Set context string (example: "abc")
                    .build(); // Build the BearerToken object

            // Retrieve the Bearer Token as a string
            bearerToken = token.getBearerToken();

            // Print the generated Bearer Token to the console
            System.out.println(bearerToken);
        } catch (SkyflowException e) {
            // Handle exceptions specific to Skyflow operations
            e.printStackTrace();
        }
    }
}
