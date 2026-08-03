package com.example.serviceaccount;

import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.BearerToken;

import java.io.File;

/**
 * This example demonstrates how to generate Bearer tokens in two different ways:
 * 1. Using a credentials file specified by its file path.
 * 2. Using the credentials as a string.
 * <p>
 * The code also showcases multithreaded token generation with a shared context (`ctx`),
 * where each thread generates and prints tokens repeatedly.
 */
public class BearerTokenGenerationUsingThreadsExample {
    public static void main(String[] args) {
        // Example 1: Generate Bearer token using a credentials file path
        try {
            // Step 1: Specify the path to the credentials file
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>"; // Replace with the actual file path

            // Step 2: Create a BearerToken object using the file path
            final BearerToken bearerToken = BearerToken.builder()
                    .setCredentials(new File(filePath)) // Provide the credentials file
                    .setCtx("abc")                     // Specify a context string ("abc" in this case)
                    .build();

            // Step 3: Create and start a thread to repeatedly generate and print tokens
            Thread t = new Thread(() -> {
                for (int i = 0; i < 5; i++) { // Loop to generate tokens 5 times
                    try {
                        System.out.println(bearerToken.getBearerToken()); // Print the Bearer token
                    } catch (SkyflowException e) { // Handle exceptions during token generation
                        Thread.currentThread().interrupt(); // Interrupt the thread on error
                        throw new RuntimeException(e); // Wrap and propagate the exception
                    }
                }
            });
            t.start(); // Start the thread
        } catch (Exception e) { // Handle exceptions during BearerToken creation
            e.printStackTrace();
        }

        // Example 2: Generate Bearer token using credentials as a string
        try {
            // Step 1: Specify the credentials as a string (file contents)
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>"; // Replace with actual file contents

            // Step 2: Create a BearerToken object using the credentials string
            final BearerToken bearerToken = BearerToken.builder()
                    .setCredentials(fileContents) // Provide the credentials as a string
                    .setCtx("abc")               // Specify a context string ("abc" in this case)
                    .build();

            // Step 3: Create and start a thread to repeatedly generate and print tokens
            Thread t = new Thread(() -> {
                for (int i = 0; i < 5; i++) { // Loop to generate tokens 5 times
                    try {
                        System.out.println(bearerToken.getBearerToken()); // Print the Bearer token
                    } catch (SkyflowException e) { // Handle exceptions during token generation
                        Thread.currentThread().interrupt(); // Interrupt the thread on error
                        throw new RuntimeException(e); // Wrap and propagate the exception
                    }
                }
            });
            t.start(); // Start the thread
        } catch (Exception e) { // Handle exceptions during BearerToken creation
            e.printStackTrace();
        }
    }
}
