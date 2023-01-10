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

public class GetExample {
    public static void main(String[] args) {

        try {
            SkyflowConfiguration config = new SkyflowConfiguration("<your_vaultID>",
                    "<your_vaultURL>", new DemoTokenProvider());
            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject firstRecord = new JSONObject();

            JSONArray ids = new JSONArray();
            ids.add("<your_skyflowId>");

            firstRecord.put("ids", ids);
            firstRecord.put("table", "<your_table_name>");
            firstRecord.put("redaction", RedactionType.PLAIN_TEXT.toString());

            JSONObject secondRecord = new JSONObject();

            JSONArray valuesArray = new JSONArray();
            valuesArray.add("<your_column_value>");

            secondRecord.put("table", "<your_table_name>");
            secondRecord.put("columnName", "<unique_column_name>");
            secondRecord.put("columnValues", valuesArray);
            secondRecord.put("redaction", RedactionType.PLAIN_TEXT.toString());

            recordsArray.add(firstRecord);
            recordsArray.add(secondRecord);
            records.put("records", recordsArray);

            JSONObject response = skyflowClient.get(records);
        } catch (SkyflowException e) {
            e.printStackTrace();
            System.out.println(e.getData());
        }

    }

    static class DemoTokenProvider implements TokenProvider {
        private String bearerToken = null;

        @Override
        public String getBearerToken() throws Exception {
            ResponseToken response = null;
            try {
                String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
                if (Token.isExpired(bearerToken)) {
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