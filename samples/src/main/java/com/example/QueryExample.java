/*
	Copyright (c) 2022 Skyflow, Inc.
*/
package com.example;

import com.skyflow.entities.QueryOptions;
import com.skyflow.entities.ResponseToken;
import com.skyflow.entities.SkyflowConfiguration;
import com.skyflow.entities.TokenProvider;
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.vault.Skyflow;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Samples {

    public static void main(String[] args) {

        try {
            SkyflowConfiguration config = new SkyflowConfiguration("<your_vaultID>",
                    "<your_vaultURL>", new DemoTokenProvider());
            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject queryInput = new JSONObject();

            queryInput.put("query", "<YOUR_SQL_QUERY>");

            QueryOptions options = new QueryOptions();

            JSONObject res = skyflowClient.query(queryInput, options);

            System.out.println(res);
        } catch (SkyflowException e) {
            System.out.println(e.getData());
            e.printStackTrace();
        }

    }

    static class DemoTokenProvider implements TokenProvider {

        private String bearerToken = null;

        @Override
        public String getBearerToken() throws Exception {
            ResponseToken response = null;
            try {
                String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
                if(Token.isExpired(bearerToken)) {
                    response = Token.generateBearerToken(filePath);
                    bearerToken = response.getAccessToken();
                }
            } catch (SkyflowException e) {
                e.printStackTrace();
            }

            return bearerToken;
        }
    }
}