package com.example;

import com.skyflow.entities.RedactionType;
import com.skyflow.entities.ResponseToken;
import com.skyflow.entities.SkyflowConfiguration;
import com.skyflow.entities.TokenProvider;
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.vault.Skyflow;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class GetByIdExample {

    public static void main(String[] args) {

        try {
            SkyflowConfiguration config = new SkyflowConfiguration("<your_vaultID>",
                    "<your_vaultURL>", new DemoTokenProvider());
            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record = new JSONObject();
            JSONArray ids = new JSONArray();
            ids.add("<your_skyflowId>");

            record.put("ids", ids);
            record.put("table", "<you_table_name>");
            record.put("redaction", RedactionType.PLAIN_TEXT.toString());
            recordsArray.add(record);
            records.put("records", recordsArray);

            JSONObject response = skyflowClient.getById(records);
        } catch (SkyflowException e) {
            e.printStackTrace();
            System.out.println(e.getData());
        }

    }

    static class DemoTokenProvider implements TokenProvider {

        @Override
        public String getBearerToken() throws Exception {
            ResponseToken res = null;
            try {
                String filePath = "<your_credentials_file_path>";
                res = Token.GenerateToken(filePath);
            } catch (SkyflowException e) {
                e.printStackTrace();
            }
            return res.getAccessToken();
        }
    }
}
