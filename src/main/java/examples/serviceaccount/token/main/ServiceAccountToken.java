package examples.serviceaccount.token.main;

import entities.ResponseToken;
import errors.SkyflowException;
import serviceaccount.util.Token;

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
