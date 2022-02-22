package com.skyflow.common.utils;

import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HttpUtility {

    public static String sendRequest(String method, String requestUrl, JSONObject params, Map<String, String> headers) throws IOException, SkyflowException {
        HttpURLConnection connection = null;
        BufferedReader in = null;
        StringBuffer response = null;
        try {
            URL url = new URL(requestUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json");

            if (headers != null && headers.size() > 0) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            if (params != null && params.size() > 0) {
                connection.setDoOutput(true);
                try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                    byte[] input = params.toString().getBytes(StandardCharsets.UTF_8);
                    wr.write(input, 0, input.length);
                    wr.flush();
                }
            }

            int status = connection.getResponseCode();
            String requestID = connection.getHeaderField("x-request-id");

            Reader streamReader;
            if (status > 299) {
                if (connection.getErrorStream() != null)
                    streamReader = new InputStreamReader(connection.getErrorStream());
                else {
                    String description = Helpers.appendRequestId(ErrorCode.Server.getDescription(), requestID);
                    LogUtil.printErrorLog(description);
                    throw new SkyflowException(status, description);
                }
            } else {
                streamReader = new InputStreamReader(connection.getInputStream());
            }

            response = new StringBuffer();
            in = new BufferedReader(streamReader);
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            if (status > 299) {
                LogUtil.printErrorLog(Helpers.appendRequestId(response.toString(), requestID));
                String errorMsg = Helpers.appendRequestIdToErrorObj(status, response.toString(), requestID);
                throw new SkyflowException(status, errorMsg);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return response.toString();
    }

}
