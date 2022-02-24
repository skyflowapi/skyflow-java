package com.example;

import com.skyflow.entities.InsertOptions;
import com.skyflow.entities.ResponseToken;
import com.skyflow.entities.SkyflowConfiguration;
import com.skyflow.entities.TokenProvider;
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.vault.Skyflow;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class InsertExample {

    public static void main(String[] args) {

        try {
            SkyflowConfiguration config = new SkyflowConfiguration("<your_vaultID>",
                    "<your_vaultURL>", new DemoTokenProvider());
            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record = new JSONObject();
            record.put("table", "<your_table_name>");

            JSONObject fields = new JSONObject();
            fields.put("first_name", "<your_field_name>");
            record.put("fields", fields);
            recordsArray.add(record);
            records.put("records", recordsArray);

            InsertOptions insertOptions = new InsertOptions();
            JSONObject res = skyflowClient.insert(records, insertOptions);

            System.out.println(res);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }

    }

    static class DemoTokenProvider implements TokenProvider {

        String bearerToken = null;

        @Override
        public String getBearerToken() throws Exception {
            ResponseToken response = null;
            try {
                String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
                if(!Token.isValid(bearerToken)) {
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
