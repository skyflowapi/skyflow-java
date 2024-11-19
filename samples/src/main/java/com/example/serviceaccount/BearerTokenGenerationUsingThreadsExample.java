package com.example.serviceaccount;

import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.BearerToken;

import java.io.File;

public class BearerTokenGenerationUsingThreadsExample {
    public static void main(String[] args) {
        // Generate BearerToken with context by specifying credentials.json file path
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            final BearerToken bearerToken = new BearerToken.BearerTokenBuilder()
                    .setCredentials(new File(filePath))
                    .setCtx("abc")
                    .build();

            Thread t = new Thread(() -> {
                for (int i = 0; i < 5; i++) {
                    try {
                        System.out.println(bearerToken.getBearerToken());
                    } catch (SkyflowException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);

                    }

                }
            });
            t.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Generate BearerToken with context by specifying credentials.json as string
        try {
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>";
            final BearerToken bearerToken = new BearerToken.BearerTokenBuilder()
                    .setCredentials(fileContents)
                    .setCtx("abc")
                    .build();

            Thread t = new Thread(() -> {
                for (int i = 0; i < 5; i++) {
                    try {
                        System.out.println(bearerToken.getBearerToken());
                    } catch (SkyflowException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);

                    }

                }
            });
            t.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}