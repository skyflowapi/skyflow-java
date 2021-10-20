package com.skyflow.examples.serviceaccount.token.main;

import com.skyflow.entities.ResponseToken;
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.Token;

public class ServiceAccountToken {

    public static void main(String args[]) {
        ResponseToken res;
        try {
            String filePath = "";
            res = Token.GenerateToken(filePath);
            System.out.println(res.getAccessToken() + ":" + res.getTokenType());
        } catch (SkyflowException e) {
            e.printStackTrace();
        }

    }
}
