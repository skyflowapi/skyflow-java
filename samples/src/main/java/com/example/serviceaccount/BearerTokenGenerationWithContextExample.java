package com.example.serviceaccount;

import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.BearerToken;

import java.io.File;

public class BearerTokenGenerationWithContextExample {
    public static void main(String[] args) {
        String bearerToken = null;

        // Generate BearerToken with context by specifying credentials.json file path
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            BearerToken token = BearerToken.builder()
                    .setCredentials(new File(filePath))
                    .setCtx("abc")
                    .build();

            bearerToken = token.getBearerToken();
            System.out.println(bearerToken);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }

        // Generate BearerToken with context by specifying credentials.json as string
        try {
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>";
            BearerToken token = BearerToken.builder()
                    .setCredentials(fileContents)
                    .setCtx("abc")
                    .build();

            bearerToken = token.getBearerToken();
            System.out.println(bearerToken);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }
    }
}
