package com.example.serviceaccount;

import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.SignedDataTokenResponse;
import com.skyflow.serviceaccount.util.SignedDataTokens;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SignedTokenGenerationExample {
    public static void main(String[] args) {
        List<SignedDataTokenResponse> signedTokenValues;

        // Generate Signed data token with context by specifying credentials.json file path
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            String context = "abc";
            ArrayList<String> dataTokens = new ArrayList<>();
            dataTokens.add("YOUR_DATA_TOKEN_1");
            SignedDataTokens signedToken = new SignedDataTokens.SignedDataTokensBuilder()
                    .setCredentials(new File(filePath))
                    .setCtx(context)
                    .setTimeToLive(30) // in seconds
                    .setDataTokens(dataTokens)
                    .build();

            signedTokenValues = signedToken.getSignedDataTokens();
            System.out.println(signedTokenValues);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Generate Signed data token with context by specifying credentials.json as string
        try {
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>";
            String context = "abc";
            ArrayList<String> dataTokens = new ArrayList<>();
            dataTokens.add("YOUR_DATA_TOKEN_1");
            SignedDataTokens signedToken = new SignedDataTokens.SignedDataTokensBuilder()
                    .setCredentials(fileContents)
                    .setCtx(context)
                    .setTimeToLive(30) // in seconds
                    .setDataTokens(dataTokens)
                    .build();

            signedTokenValues = signedToken.getSignedDataTokens();
            System.out.println(signedTokenValues);

        } catch (SkyflowException e) {
            e.printStackTrace();
        }
    }
}

