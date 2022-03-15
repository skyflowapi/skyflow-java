import com.skyflow.entities.*;
import com.skyflow.vault.Skyflow;
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.Token;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class InvokeConnectionExample {
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

    public static void main(String[] args) {
        try {
            SkyflowConfiguration config = new SkyflowConfiguration("<your_vaultID>",
                    "<your_vaultURL>", new DemoTokenProvider());
            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject testConfig = new JSONObject();
            testConfig.put("connectionURL", "<your_connection_url>");
            testConfig.put("methodName", RequestMethod.POST);

            JSONObject pathParamsJson = new JSONObject();
            pathParamsJson.put("<path_param_key>", "<path_param_value>");
            testConfig.put("pathParams", pathParamsJson);

            JSONObject queryParamsJson = new JSONObject();
            queryParamsJson.put("<query_param_key>", "<query_param_value>");
            testConfig.put("queryParams", queryParamsJson);

            JSONObject requestHeadersJson = new JSONObject();
            requestHeadersJson.put("<request_header_key>", "<request_header_value>");
            testConfig.put("requestHeader", requestHeadersJson);

            JSONObject requestBodyJson = new JSONObject();
            requestBodyJson.put("<request_body_key>", "<request_body_value>");
            testConfig.put("requestBody", requestBodyJson);

            JSONObject gatewayResponse = skyflowClient.invokeConnection(testConfig);
            System.out.println(gatewayResponse);

        } catch (SkyflowException exception) {
            exception.printStackTrace();
        }
    }
}