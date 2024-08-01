package com.example;

import com.skyflow.entities.*;
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.vault.Skyflow;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class InsertBulkWithUpsertExample {

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
            fields.put("<field_name", "<your_field_value>");

            record.put("fields", fields);
            recordsArray.add(record);

            records.put("records", recordsArray);

            // create an upsert option and insert in UpsertOptions array.
            UpsertOption[] upsertOptions = new UpsertOption[1];
            upsertOptions[0] = new UpsertOption("<table_name>", "<unique_column_name>");

            // pass upsert options in insert method options.
            InsertBulkOptions insertOptions = new InsertBulkOptions(true, upsertOptions);
            JSONObject res = skyflowClient.insertBulk(records, insertOptions);

            System.out.println(res);
        } catch (SkyflowException e) {
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
