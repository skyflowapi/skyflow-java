/*
	Copyright (c) 2024 Skyflow, Inc. 
*/
import com.skyflow.entities.*;
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.vault.Skyflow;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class InsertWithContinueOnErrorExample {

    public static void main(String[] args) {

        try {
            SkyflowConfiguration config = new SkyflowConfiguration(
                "<VAULT_ID>",
                "<VAULT_URL>",
                new DemoTokenProvider());
            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record1 = new JSONObject();
            record1.put("table", "<your_table_name>");
            JSONObject fields = new JSONObject();
            fields.put("<field_name>", "<your_field_value>");
            record1.put("fields", fields);

            JSONObject record2 = new JSONObject();
            record2.put("table", "<your_table_name>");
            JSONObject fields2 = new JSONObject();
            fields2.put("<field_name>", "<your_field_value>");
            record2.put("fields", fields2);

            recordsArray.add(record1);
            recordsArray.add(record2);
            records.put("records", recordsArray);

            InsertOptions insertOptions = new InsertOptions(true,true);
            JSONObject insertResponse = skyflowClient.insert(records, insertOptions);
            System.out.println(insertResponse);
        } catch (SkyflowException e) {
            System.out.println(e);
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
