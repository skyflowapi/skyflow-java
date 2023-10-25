package com.skyflow.vault;

import com.skyflow.common.utils.Helpers;
import com.skyflow.common.utils.HttpUtility;
import com.skyflow.common.utils.LogUtil;
import com.skyflow.common.utils.Validators;
import com.skyflow.entities.DeleteOptions;
import com.skyflow.entities.DeleteRecordInput;
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

public class DeleteBySkyflowId implements Callable<String> {
    private final DeleteRecordInput recordInput;
    private final String vaultID;
    private final String vaultURL;
    private final Map<String, String> headers;
    private final DeleteOptions deleteOptions;

    public DeleteBySkyflowId(DeleteRecordInput recordInput, String vaultID, String vaultURL, Map<String, String> headers, DeleteOptions deleteOptions) {
        this.recordInput = recordInput;
        this.vaultID = vaultID;
        this.vaultURL = vaultURL;
        this.headers = headers;
        this.deleteOptions = deleteOptions;
    }


    @Override
    public String call() throws SkyflowException {
        String response = null;
        try {
            Validators.validateDeleteBySkyflowId(recordInput);
            String url = vaultURL+ "/v1/vaults/"+ vaultID + "/" + recordInput.getTable() + "/" + recordInput.getId();
            response = HttpUtility.sendRequest("DELETE", new URL(url), null,headers);
            JSONObject formattedResponse = new JSONObject();
            JSONArray formattedRecords = new JSONArray();

            JSONObject responseRecords = ((JSONObject) (new JSONParser().parse(response)));
            if (responseRecords != null && responseRecords.size() > 0) {
                String id = (String)  responseRecords.get("skyflow_id");
                JSONObject formattedRecord = new JSONObject();
                formattedRecord.put("skyflow_id", responseRecords.get("skyflow_id"));
                formattedRecord.put("deleted", responseRecords.get("deleted"));
                formattedRecords.add(formattedRecord);
            }
            formattedResponse.put("records", formattedRecords);
            response = formattedResponse.toJSONString();

        } catch (IOException e) {
            LogUtil.printErrorLog(ErrorLogs.Server.getLog());
            throw new SkyflowException(ErrorCode.Server, e);
        } catch (ParseException e) {
            LogUtil.printErrorLog(ErrorLogs.ResponseParsingError.getLog());
            throw new SkyflowException(ErrorCode.ResponseParsingError, e);
        } catch (SkyflowException e) {
            response = constructDeleteByIdErrorObject(e, recordInput.getId());
        }
        return response;
    }
    private String constructDeleteByIdErrorObject(SkyflowException skyflowException, String id){
        String deleteByIdResponse = null;
        JSONObject finalResponseError = new JSONObject();
        finalResponseError.put("id", id);
        try{
            JSONObject errorObject = (JSONObject) ((JSONObject) new JSONParser().parse(skyflowException.getMessage())).get("error");
            if (errorObject != null) {
                JSONObject responseError = new JSONObject();
                responseError.put("code", errorObject.get("http_code"));
                responseError.put("description", errorObject.get("message"));
                finalResponseError.put("error", responseError);

                deleteByIdResponse = finalResponseError.toString();
            }

        } catch (ParseException e){
            JSONObject responseError = new JSONObject();
            responseError.put("code", skyflowException.getCode());
            responseError.put("description", skyflowException.getMessage());
            finalResponseError.put("error", responseError);
            deleteByIdResponse = finalResponseError.toString();
        }
        return deleteByIdResponse;
    }
}
