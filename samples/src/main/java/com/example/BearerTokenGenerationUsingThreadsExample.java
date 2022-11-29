/*
	Copyright (c) 2022 Skyflow, Inc.
*/
package com.example;

import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.BearerToken;

import java.io.File;

public class BearerTokenGenerationUsingThreadsExample {
    public static void main(String args[]) {

        String bearerToken = null;

        // Generate BearerToken with context by specifying credentials.json file path
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            final BearerToken token = new BearerToken.BearerTokenBuilder()
                    .setCredentials(new File(filePath))
                    .setCtx("abc")
                    .build();

            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {

                    for (int i = 0; i < 5; i++) {
                        try {
                            System.out.println(token.getBearerToken());
                        } catch (SkyflowException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);

                        }

                    }
                }
            });

            t.start();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
