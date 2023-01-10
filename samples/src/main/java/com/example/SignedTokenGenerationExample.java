/*
	Copyright (c) 2022 Skyflow, Inc.
*/
package com.example;

import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.SignedDataTokenResponse;
import com.skyflow.serviceaccount.util.SignedDataTokens;

import java.io.File;
import java.util.List;

public class SignedTokenGenerationExample {
    public static void main(String args[]) {

        List<SignedDataTokenResponse> signedTokenValue;

        // Generate Signed data token with context by specifying credentials.json file path
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            String context = "abc";
            SignedDataTokens signedToken = new SignedDataTokens.SignedDataTokensBuilder()
                    .setCredentials(new File(filePath))
                    .setCtx(context)
                    .setTimeToLive(30) // in seconds
                    .setDataTokens(new String[]{"dataToken1"}).build();

            signedTokenValue =  signedToken.getSignedDataTokens();
            System.out.println(signedTokenValue);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Generate Signed data token with context by specifying credentials.json as string
        try {
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>";
            String context = "abc";
            SignedDataTokens signedToken = new SignedDataTokens.SignedDataTokensBuilder()
                    .setCredentials(fileContents)
                    .setCtx(context)
                    .setTimeToLive(30) // in seconds
                    .setDataTokens(new String[]{"dataToken1"}).build();

            signedTokenValue =  signedToken.getSignedDataTokens();
            System.out.println(signedTokenValue);

        } catch (SkyflowException e) {
            e.printStackTrace();
        }


    }
}
