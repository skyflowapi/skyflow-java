/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.vault;

import com.skyflow.common.utils.Helpers;
import com.skyflow.common.utils.HttpUtility;
import com.skyflow.common.utils.LogUtil;
import com.skyflow.common.utils.Validators;
import com.skyflow.entities.GetByIdRecordInput;
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

final class GetBySkyflowId implements Callable<String> {

    private final GetByIdRecordInput record;
    private final Map<String, String> headers;
    private final String vaultID;
    private final String vaultURL;

    GetBySkyflowId(GetByIdRecordInput record, String vaultID, String vaultURL, Map<String, String> headers) {
        this.record = record;
        this.vaultID = vaultID;
        this.vaultURL = vaultURL;
        this.headers = headers;
    }

    @Override
    public String call() throws SkyflowException {
        String response = null;
        try {
            Validators.validateGetByIdRequestRecord(record);
            StringBuilder paramsList = Helpers.constructGetByIdRequestURLParams(record);

            String url = vaultURL + "/v1/vaults/" + vaultID + "/" + record.getTable() + "?" + paramsList;

            response = HttpUtility.sendRequest("GET", new URL(url), null, headers);

            JSONObject formattedResponse = new JSONObject();
            JSONArray formattedRecords = new JSONArray();

            JSONArray responseRecords = (JSONArray) ((JSONObject) (new JSONParser().parse(response))).get("records");
            if (responseRecords != null && responseRecords.size() > 0) {
                for (Object responseRecord : responseRecords) {
                    JSONObject fields = (JSONObject) ((JSONObject) responseRecord).get("fields");
                    String id = (String) fields.get("skyflow_id");
                    fields.remove("skyflow_id");
                    fields.put("id", id);

                    JSONObject formattedRecord = new JSONObject();
                    formattedRecord.put("fields", fields);
                    formattedRecord.put("table", record.getTable());
                    formattedRecords.add(formattedRecord);
                }
                formattedResponse.put("records", formattedRecords);

                response = formattedResponse.toJSONString();
            }
        } catch (IOException e) {
            LogUtil.printErrorLog(ErrorLogs.Server.getLog());
            throw new SkyflowException(ErrorCode.Server, e);
        } catch (ParseException e) {
            LogUtil.printErrorLog(ErrorLogs.ResponseParsingError.getLog());
            throw new SkyflowException(ErrorCode.ResponseParsingError, e);
        } catch (SkyflowException e) {
            response = record.getIds() == null
                    ? constructGetByIdErrorObject(e)
                    : constructGetByIdErrorObject(e, record.getIds());
        }

        return response;
    }

    private String constructGetByIdErrorObject(SkyflowException skyflowException) {
        String getByIdResponse = null;
        JSONObject finalResponseError = new JSONObject();

        try {
            JSONObject errorObject = (JSONObject) ((JSONObject) new JSONParser().parse(skyflowException.getMessage())).get("error");
            if (errorObject != null) {
                JSONObject responseError = new JSONObject();
                responseError.put("code", errorObject.get("http_code"));
                responseError.put("description", errorObject.get("message"));
                finalResponseError.put("error", responseError);

                getByIdResponse = finalResponseError.toString();
            }
        } catch (ParseException e) {
            JSONObject responseError = new JSONObject();
            responseError.put("code", skyflowException.getCode());
            responseError.put("description", skyflowException.getMessage());
            finalResponseError.put("error", responseError);
            getByIdResponse = finalResponseError.toString();
        }
        return getByIdResponse;
    }

    private String constructGetByIdErrorObject(SkyflowException skyflowException, String[] ids) {

        JSONObject finalResponseError = new JSONObject();
        JSONArray idsArray = new JSONArray();
        Collections.addAll(idsArray, ids);
        finalResponseError.put("ids", idsArray);
        return constructGetByIdErrorObject(skyflowException);
    }
}
