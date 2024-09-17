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


public class DetokenizeExample {

    public static void main(String[] args) {

        try {
            SkyflowConfiguration config = new SkyflowConfiguration(
                    "l906001b0b4e4843a69b091f278f7017",
                    "https://sb.area51.vault.skyflowapis.dev",
                    new DemoTokenProvider()
            );

            Configuration.setLogLevel(LogLevel.DEBUG);

            Skyflow skyflowClient = Skyflow.init(config);
            JSONObject records = new JSONObject();
            JSONArray recordsArray = new JSONArray();

            JSONObject record = new JSONObject();
            record.put("token", "4304-0207-2378-9259");
            record.put("redaction", RedactionType.PLAIN_TEXT.toString());
            JSONObject record1 = new JSONObject();
            record1.put("token", "3809-9467-7122-3994");
            record1.put("redaction", RedactionType.PLAIN_TEXT.toString());
            recordsArray.add(record);
            recordsArray.add(record1);
            records.put("records", recordsArray);

            JSONObject response = skyflowClient.detokenize(records);
            System.out.println(response);

        } catch (SkyflowException e) {
            e.printStackTrace();
            System.out.println(e.getData());
        }

    }

    static class DemoTokenProvider implements TokenProvider {

        private String bearerToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJodHRwczovL21hbmFnZS5za3lmbG93YXBpcy5kZXYiLCJjbGkiOiJ2OGY0NTE1OWYxNjA0NzViYTk4MjAxOGJkMmFiMmVlZSIsImV4cCI6MTcxMTA5NDYwMSwiaWF0IjoxNzExMDkxMDAyLCJpc3MiOiJzYS1hdXRoQG1hbmFnZS5za3lmbG93YXBpcy5kZXYiLCJqdGkiOiJuMzk0OGI1ODIxOGQ0NWQ2OWQyYTExZjMwYjk2YmRiMiIsImtleSI6Imc0NDRmZTQxMmEzZTQ5ZTA5YjJlN2E4ZGE2ZDhkZWNlIiwic2NwIjpudWxsLCJzdWIiOiJzZGstZTJlLWJsaXR6LW5ldyJ9.D1bkgk8U5yC7Ped-eC8W4zc7CSImRdMbH2jvx9o_ADGYLmRsUJg3Jm004RtmCz6Vqy1hfgBobje4SgccavlYQTO3JUdtwx9SLJVwMGSEYnM_LvTAKqTVInJkvkKgJThHfh6na4iKWZlzUjNjhIFVjLcEhJuikRO68dpw8YFo84mc8iAjzW4BiKIyfAPzEUqIYq6VNUtz80ZsIhkfMz99_93NFf_KKzIR_a-MVYa7LzJfCrO6EQ-1fXLrTeJkusuco4MFSOf3-x55TimJygcQJjJC34lEtDK0OhFJWbM65gJRpRv6ftaknXRcp8caVE6xkqXI320O6GcBZK933aHw8w";

        @Override
        public String getBearerToken() throws Exception {
//            ResponseToken response = null;
//            try {
//                String filePath = "/home/vivekj/SKYFLOW/SDKs/skyflow-java/samples/credentials.json";
//                if (Token.isExpired(bearerToken)) {
//                    response = Token.generateBearerToken(filePath);
//                    bearerToken = response.getAccessToken();
//                }
//            } catch (SkyflowException e) {
//                e.printStackTrace();
//            }

            return bearerToken;
        }
    }
}
