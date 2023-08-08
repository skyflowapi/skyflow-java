//	Copyright (c) 2022 Skyflow, Inc.
//
//package com.skyflow;
//
import com.skyflow.entities.ResponseToken;
import com.skyflow.entities.SkyflowConfiguration;
import com.skyflow.entities.TokenProvider;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.Skyflow;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class DeleteExample {

    public static void main(String[] args) {

        try {
            SkyflowConfiguration config = new SkyflowConfiguration("<your_vaultID>",
                    "<your_vaultURL>", new DemoTokenProvider());
            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record = new JSONObject();

            record.put("id", "<your_skyflowId>");
            record.put("table", "<you_table_name>");
            recordsArray.add(record);
            JSONObject record2 = new JSONObject();

            record2.put("id", "<your_skyflowId>");
            record2.put("table", "<you_table_name>");
            recordsArray.add(record2);

            records.put("records", recordsArray);

            JSONObject response = skyflowClient.delete(records);
            System.out.println(response);
        } catch (SkyflowException e) {
            e.printStackTrace();
            System.out.println("error"+ e.getData());
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
