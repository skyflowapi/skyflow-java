package com.skyflow.vault;

import com.skyflow.common.utils.HttpUtility;
import com.skyflow.entities.GetByIdRecordInput;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

public class GetBySkyflowId implements Callable<String> {

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
            StringBuilder paramsList = new StringBuilder();
            for (String id : record.getIds()) {
                paramsList.append("skyflow_ids=" + id + "&");
            }

            if(record.getTable() == null || record.getTable().isEmpty()) {
                throw new SkyflowException(ErrorCode.InvalidTable);
            }

            String url = vaultURL + "/v1/vaults/" + vaultID + "/" +
                    record.getTable() + "?" + paramsList + "redaction=" + record.getRedaction();

            response = HttpUtility.sendRequest("GET", url, null, headers);

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
            throw new SkyflowException(ErrorCode.Server, e);
        } catch (ParseException e) {
            throw new SkyflowException(ErrorCode.ResponseParsingError, e);
        } catch (SkyflowException e) {
            response = constructGetByIdErrorObject(e, record.getIds());
        }

        return response;
    }

    private String constructGetByIdErrorObject(SkyflowException skyflowException, String[] ids) {

        String getByIdResponse = null;
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
}
