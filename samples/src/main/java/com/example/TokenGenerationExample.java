package com.example;

import com.skyflow.entities.ResponseToken;
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.Token;

public class TokenGenerationExample {
    public static void main(String args[]) {

        // Generate BearerToken by specifying credentials.json file path
        try {
            String filePath = "<your_credentials.json_file_path>";
            ResponseToken res = Token.generateBearerToken(filePath);
            System.out.println(res.getTokenType() + ":" + res.getAccessToken());
        } catch (SkyflowException e) {
            e.printStackTrace();
        }

        // Generate BearerToken by specifying credentials.json as string
        try {
            String filePath = "<your_credentials.json_file_as string>";
            ResponseToken res = Token.generateBearerTokenFromCreds(filePath);
            System.out.println(res.getTokenType() + ":" + res.getAccessToken());
        } catch (SkyflowException e) {
            e.printStackTrace();
        }

    }
}
