import com.skyflow.entities.RedactionType;
import com.skyflow.vault.Skyflow;
import com.skyflow.entities.TokenProvider;
import com.skyflow.errors.SkyflowException;
import com.skyflow.entities.ResponseToken;
import com.skyflow.entities.SkyflowConfiguration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class InvokeConnectionExample {
    class DemoTokenProvider implements TokenProvider {

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