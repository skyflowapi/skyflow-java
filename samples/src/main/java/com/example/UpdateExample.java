import com.skyflow.entities.*;
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.vault.Skyflow;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class UpdateExample {
    public static void main(String[] args) {

        try {
            SkyflowConfiguration config = new SkyflowConfiguration("<your_vaultID>",
                    "<your_vaultURL>", new DemoTokenProvider());
            Skyflow skyflowClient = Skyflow.init(config);

            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();
            JSONObject record = new JSONObject();

            record.put("table", "<your_table_name>");
            record.put("id", "<your_skyflow_id>");

            JSONObject fields = new JSONObject();
            fields.put("<your_field_name>", "<your_field_value>");
            record.put("fields", fields);
            recordsArray.add(record);
            records.put("records", recordsArray);

            UpdateOptions updateOptions = new UpdateOptions(true);
            JSONObject res = skyflowClient.update(records, updateOptions);

        }
        catch (SkyflowException e) {
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

