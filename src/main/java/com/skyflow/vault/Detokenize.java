package com.skyflow.vault;

import com.skyflow.common.utils.HttpUtility;
import com.skyflow.common.utils.LogUtil;
import com.skyflow.entities.DetokenizeRecord;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;

final class Detokenize implements Callable<String> {
    private final DetokenizeRecord record;
    private final String endPointURL;
    private final Map<String, String> headers;

    public Detokenize(DetokenizeRecord record, String endPointURL, Map<String, String> headers) {
        this.record = record;
        this.endPointURL = endPointURL;
        this.headers = headers;
    }

    @Override
    public String call() throws SkyflowException {
        String response = null;

        try {
            if (record.getToken() == null || record.getToken().isEmpty()) {
                LogUtil.printErrorLog(ErrorLogs.InvalidBearerToken.getLog());
                throw new SkyflowException(ErrorCode.InvalidToken);
            }
            JSONObject bodyJson = new JSONObject();
            JSONArray tokensArray = new JSONArray();
            JSONObject token = new JSONObject();
            token.put("token", record.getToken());
            tokensArray.add(token);
            bodyJson.put("detokenizationParameters", tokensArray);


            String apiResponse = HttpUtility.sendRequest("POST", endPointURL, bodyJson, headers);
            JSONParser parser = new JSONParser();
            JSONObject responseJson = (JSONObject) parser.parse(apiResponse);
            JSONArray responseRecords = (JSONArray) responseJson.get("records");
            JSONObject responseObject = (JSONObject) responseRecords.get(0);
            responseObject.remove("valueType");
            response = responseObject.toJSONString();
        } catch (IOException e) {
            LogUtil.printErrorLog(ErrorLogs.Server.getLog());
            throw new SkyflowException(ErrorCode.Server, e);
        } catch (ParseException e) {
            LogUtil.printErrorLog(ErrorLogs.ResponseParsingError.getLog());
            throw new SkyflowException(ErrorCode.ResponseParsingError, e);
        } catch (SkyflowException exception) {
            response = parseDetokenizeError(exception);
        }
        return response;
    }

    private String parseDetokenizeError(SkyflowException exception) {
        String errorResponse = null;
        String exceptionMessage = exception.getMessage();
        int exceptionCode = exception.getCode();
        JSONObject errorObject = new JSONObject();
        errorObject.put("token", record.getToken());
        JSONObject errorData = new JSONObject();

        try {
            JSONParser parser = new JSONParser();
            JSONObject errorJson = (JSONObject) parser.parse(exceptionMessage);
            JSONObject error = (JSONObject) errorJson.get("error");
            if(error != null) {
                errorData.put("code", error.get("http_code"));
                errorData.put("description", error.get("message"));
                errorObject.put("error", errorData);
                errorResponse = errorObject.toJSONString();
            }
        } catch (ParseException e) {
            errorData.put("code", exceptionCode);
            errorData.put("description", exceptionMessage);
            errorObject.put("error", errorData);
            errorResponse = errorObject.toJSONString();
        }

        return errorResponse;
    }
}
