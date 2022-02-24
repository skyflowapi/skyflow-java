package com.example;

import com.skyflow.entities.ResponseToken;
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.Token;

public class TokenGenerationExample {
    public static void main(String args[]) {

        String bearerToken = null;

        // Generate BearerToken by specifying credentials.json file path
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            if(!Token.isValid(bearerToken)) {
                ResponseToken res = Token.generateBearerToken(filePath);
                bearerToken = res.getAccessToken();
            }
            System.out.println(bearerToken);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Generate BearerToken by specifying credentials.json as string
        try {
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>";
            if(!Token.isValid(bearerToken)) {
                ResponseToken res = Token.generateBearerTokenFromCreds(fileContents);
                bearerToken = res.getAccessToken();
            }
            System.out.println(bearerToken);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }

    }
}
