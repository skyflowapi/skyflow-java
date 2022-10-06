/*
	Copyright (c) 2022 Skyflow, Inc.
*/
package com.example;

import com.skyflow.errors.SkyflowException;
import java.io.File;
import java.util.List;

public class SignedTokenGeneration {
    public static void main(String args[]) {

        List<SignedDataTokenResponse> signedTokenValue;

        // Generate BearerToken with context by specifying credentials.json file path
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            String context = "abc";
            SignedDataTokens signedToken = new SignedDataTokens.SignedDataTokensBuilder()
                    .setCredentials(new File(filePath))
                    .setContext(context)
                    .setTimeToLive(30.0) // in seconds
                    .setDataTokens(new String[]{"dataToken1"}).build();

            signedTokenValue =  signedToken.getSignedDataTokens();
            System.out.println(signedTokenValue);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Generate BearerToken with context by specifying credentials.json as string
        try {
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>";
            String context = "abc";
            SignedDataTokens signedToken = new SignedDataTokens.SignedDataTokensBuilder()
                    .setCredentials(fileContents)
                    .setContext(context)
                    .setTimeToLive(30.0) // in seconds
                    .setDataTokens(new String[]{"dataToken1"}).build();

            signedTokenValue =  signedToken.getSignedDataTokens();
            System.out.println(signedTokenValue);

        } catch (SkyflowException e) {
            e.printStackTrace();
        }


    }
}
