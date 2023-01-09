package com.skyflow.vault;

import com.skyflow.common.utils.Helpers;
import com.skyflow.common.utils.HttpUtility;
import com.skyflow.common.utils.LogUtil;
import com.skyflow.entities.UpdateOptions;
import com.skyflow.entities.UpdateRecordInput;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

final class UpdateBySkyflowId implements Callable<String> {

    public final UpdateOptions options;
    private final UpdateRecordInput record;
    private final Map<String, String> headers;
    private final String vaultID;
    private final String vaultURL;

    UpdateBySkyflowId(UpdateRecordInput record, String vaultID, String vaultURL, Map<String, String> headers, UpdateOptions updateOptions) {
        this.record = record;
        this.vaultID = vaultID;
        this.vaultURL = vaultURL;
        this.headers = headers;
        this.options = updateOptions;
    }


    @Override
    public String call() throws Exception {
        String response = null;
        try {
            JSONObject requestBody = Helpers.constructUpdateRequest(record, options);
            JSONObject finalBody = new JSONObject();

            finalBody.put("record", requestBody);
            finalBody.put("tokenization", options.isTokens());

            String url = vaultURL + "/v1/vaults/" + vaultID + "/" + record.getTable() + "/" + record.getId();
            response = HttpUtility.sendRequest("PUT", new URL(url), finalBody, headers);
            JSONObject formattedRecords = new JSONObject();
            JSONObject responseRecords = (JSONObject) (new JSONParser().parse(response));


            if (responseRecords != null && responseRecords.size() > 0) {
                JSONObject fields = (JSONObject) responseRecords.get("tokens");
                String id = (String) responseRecords.get("skyflow_id");

                JSONObject formattedRecord = new JSONObject();
                formattedRecord.put("id", id);
                formattedRecord.put("table", record.getTable());
                formattedRecord.put("fields", fields);
                formattedRecords.put("records", formattedRecord);
            }
            response = formattedRecords.toJSONString();
        } catch (IOException e) {
            LogUtil.printErrorLog(ErrorLogs.Server.getLog());
            throw new SkyflowException(ErrorCode.Server, e);
        } catch (ParseException e) {
            LogUtil.printErrorLog(Helpers.parameterizedString(ErrorLogs.ResponseParsingError.getLog(), "updateById"));
            throw new SkyflowException(ErrorCode.ResponseParsingError, e);
        } catch (SkyflowException e) {
            response = constructUpdateByIdErrorObject(e, record.getId());
        }
        return response;
    }

    private String constructUpdateByIdErrorObject(SkyflowException skyflowException, String ids) {

        String updateByIdResponse = null;
        JSONObject finalResponseError = new JSONObject();

        try {
            JSONArray idsArray = new JSONArray();
            Collections.addAll(idsArray, ids);
            finalResponseError.put("ids", idsArray);

            JSONObject errorObject = (JSONObject) ((JSONObject) new JSONParser().parse(skyflowException.getMessage())).get("error");
            if (errorObject != null) {
                JSONObject responseError = new JSONObject();
                responseError.put("code", errorObject.get("http_code"));
                responseError.put("description", errorObject.get("message"));
                finalResponseError.put("error", responseError);

                updateByIdResponse = finalResponseError.toString();
            }
        } catch (ParseException e) {
            JSONObject responseError = new JSONObject();
            responseError.put("code", skyflowException.getCode());
            responseError.put("description", skyflowException.getMessage());
            finalResponseError.put("error", responseError);
            updateByIdResponse = finalResponseError.toString();
        }
        return updateByIdResponse;
    }
}
