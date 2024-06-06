package com.skyflow.vault;

import com.skyflow.common.utils.HttpUtility;
import com.skyflow.common.utils.LogUtil;
import com.skyflow.entities.InsertBulkOptions;
import com.skyflow.entities.InsertRecordInput;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.skyflow.common.utils.Helpers.getUpsertColumn;

public final class Insert implements Callable<String> {
    private final InsertRecordInput record;
    private final String vaultID;
    private final String vaultURL;
    private final Map<String, String> headers;

    private final InsertBulkOptions insertOptions;
    private final int requestIndex;

    public Insert(InsertRecordInput record, String vaultID, String vaultURL, Map<String, String> headers, InsertBulkOptions insertOptions, int requestIndex) {
        this.record = record;
        this.vaultID = vaultID;
        this.vaultURL = vaultURL;
        this.headers = headers;
        this.insertOptions = insertOptions;
        this.requestIndex = requestIndex;
    }

    @Override
    public String call() throws Exception{
        String response = null;
        try {
            String url = vaultURL+ "/v1/vaults/" +vaultID+"/"+ record.getTable();

            JSONObject jsonBody = new JSONObject();
            JSONArray insertArray = new JSONArray();
            JSONObject recordObject = new JSONObject();
            recordObject.put("fields", record.getFields());

            if (insertOptions.getUpsertOptions() != null)
                jsonBody.put("upsert", getUpsertColumn(record.getTable(), insertOptions.getUpsertOptions()));

            insertArray.add(recordObject);
            jsonBody.put("records", insertArray);
            jsonBody.put("tokenization", insertOptions.isTokens());
            response = HttpUtility.sendRequest("POST", new URL(url), jsonBody, headers);

            JSONParser parser = new JSONParser();
            JSONObject responseJson = (JSONObject) parser.parse(response);
            JSONArray responseRecords = (JSONArray) responseJson.get("records");
            JSONObject responseObject = (JSONObject) responseRecords.get(0);
            JSONObject formattedResponseJson = new JSONObject();
            if (insertOptions.isTokens()) {
                JSONObject responseTokens = (JSONObject) responseObject.get("tokens");
                responseTokens.remove("*");
                responseTokens.put("skyflow_id", responseObject.get("skyflow_id"));
                formattedResponseJson.put("fields", responseTokens);

            } else {
                formattedResponseJson.put("skyflow_id", responseObject.get("skyflow_id"));
            }
            formattedResponseJson.put("table", record.getTable());
            formattedResponseJson.put("request_index", requestIndex);

            JSONObject resRecords = new JSONObject();
            JSONArray responseArray = new JSONArray();
            responseArray.add(formattedResponseJson);
            resRecords.put("records", responseArray);

            response = resRecords.toJSONString();

        } catch (IOException e) {
            LogUtil.printErrorLog(ErrorLogs.Server.getLog());
            throw new SkyflowException(ErrorCode.Server, e);
        } catch (SkyflowException exception){
            response = parseInsertError(exception, requestIndex);
        }
        return response;
    }
    private String parseInsertError(SkyflowException exception, int requestIndex) {
        String errorResponse = null;
        String exceptionMessage = exception.getMessage();
        int exceptionCode = exception.getCode();
        JSONObject errorObject = new JSONObject();
        JSONObject errorData = new JSONObject();

        try {
            JSONParser parser = new JSONParser();
            JSONObject errorJson = (JSONObject) parser.parse(exceptionMessage);
            JSONObject error = (JSONObject) errorJson.get("error");
            if (error != null) {
                errorData.put("code", error.get("http_code"));
                errorData.put("description", error.get("message"));
                errorData.put("request_index", requestIndex);
                errorObject.put("error", errorData);
                errorResponse = errorObject.toJSONString();
            }
        } catch (ParseException e) {
            errorData.put("code", exceptionCode);
            errorData.put("description", exceptionMessage);
            errorData.put("request_index", requestIndex);
            errorObject.put("error", errorData);
            errorResponse = errorObject.toJSONString();
        }

        return errorResponse;
    }

    }
