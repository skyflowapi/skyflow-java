/*
	Copyright (c) 2022 Skyflow, Inc.
*/
package com.example;

import com.skyflow.entities.ResponseToken;
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.Token;
import java.io.File;

public class BearerTokenWithContextGeneration {
    public static void main(String args[]) {

        String bearerToken = null;

        // Generate BearerToken with context by specifying credentials.json file path
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            BearerToken token = new BearerToken.BearerTokenBuilder()
                    .setCredentials(new File(filePath))
                    .setContext("abc")
                    .build();

            bearerToken = token.getBearerToken();
            System.out.println(bearerToken);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Generate BearerToken with context by specifying credentials.json as string
        try {
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>";
            BearerToken token = new BearerToken.BearerTokenBuilder()
                    .setCredentials(fileContents)
                    .setContext("abc")
                    .build();

            bearerToken = token.getBearerToken();
            System.out.println(bearerToken);

        } catch (SkyflowException e) {
            e.printStackTrace();
        }

    }
}
