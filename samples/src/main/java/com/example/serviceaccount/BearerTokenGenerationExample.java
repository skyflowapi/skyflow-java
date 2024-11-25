package com.example.serviceaccount;

import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.BearerToken;
import com.skyflow.serviceaccount.util.Token;

import java.io.File;

public class BearerTokenGenerationExample {
    public static void main(String[] args) {
        String token = null;

        // Generate BearerToken by specifying credentials.json file path
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            if (Token.isExpired(token)) {
                BearerToken bearerToken = BearerToken.builder().setCredentials(new File(filePath)).build();
                token = bearerToken.getBearerToken();
            }
            System.out.println(token);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }

        // Generate BearerToken by specifying credentials.json as string
        try {
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>";
            if (Token.isExpired(token)) {
                BearerToken bearerToken = BearerToken.builder().setCredentials(fileContents).build();
                token = bearerToken.getBearerToken();
            }
            System.out.println(token);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }
    }
}