/*
	Copyright (c) 2022 Skyflow, Inc. 
*/

import com.skyflow.Configuration;
import com.skyflow.entities.*;
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.vault.Skyflow;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class InsertExample {

    public static void main(String[] args) {
        // blitz
//        String vaultID = "";
//        String vaultURL = "";

        // stage
        String vaultID = "aef438f3be974c8f897795ab02248f78";
        String vaultURL = "https://cf5643204594.vault.skyflowapis.tech";

        try {
            SkyflowConfiguration config = new SkyflowConfiguration(vaultID, vaultURL, new DemoTokenProvider());

            Configuration.setLogLevel(LogLevel.DEBUG);
            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record = new JSONObject();
            record.put("table", "customers");
            JSONObject fields = new JSONObject();
            fields.put("name", "chandragupta maurya");
            fields.put("card_number", "4111111111111111");
            fields.put("email", "chandragupta.maurya@test.com");
            record.put("fields", fields);

            JSONObject record2 = new JSONObject();
            record2.put("table", "pii_fields_upsert");
            JSONObject fields2 = new JSONObject();
            fields2.put("name", "java coe");
            fields2.put("card_number", "4111111111111111");
            fields2.put("expiration_year", "2025");
            fields2.put("expiration_month", "11");
            fields2.put("expiration_date", "11/25");
            fields2.put("cvv", "123");
            record2.put("fields", fields2);

            recordsArray.add(record);
//            recordsArray.add(record2);
            records.put("records", recordsArray);

            InsertOptions insertOptions = new InsertOptions(false);
            JSONObject res = skyflowClient.insert(records, insertOptions);

            System.out.println("final response " + res);
        } catch (SkyflowException e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    static class DemoTokenProvider implements TokenProvider {
        private String bearerToken;

        @Override
        public String getBearerToken() throws Exception {
            ResponseToken response = null;
            String env = "stage";
            try {
                String filePath = "/home/vivekj/Documents/credentials/sdk-integration-tests/" + env + "-credentials.json";
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
