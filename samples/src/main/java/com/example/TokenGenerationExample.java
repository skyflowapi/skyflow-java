package com.example;

import com.skyflow.entities.ResponseToken;
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.Token;

public class TokenGenerationExample {
    public static void main(String args[]) {

        try {
            String filePath = "";
            ResponseToken res = Token.GenerateToken(filePath);
            System.out.println(res.getTokenType() + ":" + res.getAccessToken());
        } catch (SkyflowException e) {
            e.printStackTrace();
        }
    }
}
