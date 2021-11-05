package com.skyflow.common.utils;

import com.skyflow.entities.InsertInput;
import com.skyflow.entities.InsertOptions;
import com.skyflow.entities.InsertRecordInput;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class Helpers {

    public static JSONObject constructInsertRequest(InsertInput recordsInput, InsertOptions options) throws SkyflowException {
        JSONObject finalRequest = new JSONObject();
        List<JSONObject> requestBodyContent = new ArrayList<JSONObject>();
        boolean isTokens = options.isTokens();
        InsertRecordInput[] records = recordsInput.getRecords();

        if (records == null || records.length == 0) {
            throw new SkyflowException(ErrorCode.EmptyRecords);
        }

        for (int i = 0; i < records.length; i++) {
            InsertRecordInput record = records[i];

            if(record.getTable() == null || record.getTable().isEmpty()) {
                throw new SkyflowException(ErrorCode.InvalidTable);
            }
            if(record.getFields() == null) {
                throw new SkyflowException(ErrorCode.InvalidFields);
            }

            JSONObject postRequestInput = new JSONObject();
            postRequestInput.put("method", "POST");
            postRequestInput.put("quorum", true);
            postRequestInput.put("tableName", record.getTable());
            postRequestInput.put("fields", record.getFields());
            requestBodyContent.add(postRequestInput);

            if (isTokens) {
                JSONObject getRequestInput = new JSONObject();
                getRequestInput.put("method", "GET");
                getRequestInput.put("tableName", record.getTable());
                getRequestInput.put("ID", String.format("$responses.%d.records.0.skyflow_id", 2 * i));
                getRequestInput.put("tokenization", true);
                requestBodyContent.add(getRequestInput);
            }
        }
        finalRequest.put("records", requestBodyContent);

        return finalRequest;
    }

    public static JSONObject constructInsertResponse(JSONObject response, List requestRecords, boolean tokens) {

        JSONArray responses = (JSONArray) response.get("responses");
        JSONArray updatedResponses = new JSONArray();
        JSONObject insertResponse = new JSONObject();
        if (tokens) {
            for (int index = 0; index < responses.size(); index++) {
                if (index % 2 != 0) {
                    String skyflowId = (String) ((JSONObject) ((JSONArray) ((JSONObject) responses.get(index - 1)).get("records")).get(0)).get("skyflow_id");

                    JSONObject newObj = new JSONObject();
                    newObj.put("table", ((JSONObject) requestRecords.get(index)).get("tableName"));

                    JSONObject newFields = (JSONObject) ((JSONObject) responses.get(index)).get("fields");
                    newFields.remove("*");
                    newFields.put("skyflow_id", skyflowId);
                    newObj.put("fields", newFields);

                    updatedResponses.add(newObj);
                }
            }
        } else {
            for (int index = 0; index < responses.size(); index++) {
                JSONObject newObj = new JSONObject();

                newObj.put("table", ((JSONObject) requestRecords.get(index)).get("tableName"));
                newObj.put("skyflow_id", ((JSONObject) ((JSONArray) ((JSONObject) responses.get(index)).get("records")).get(0)).get("skyflow_id"));

                updatedResponses.add(newObj);
            }
        }
        insertResponse.put("records", updatedResponses);
        return insertResponse;
    }

    public static String parameterizedString(String base, String... args) {
        for (int index = 0; index < args.length; index++) {
            base = base.replace("%s" + (index + 1), args[index]);
        }
        return base;
    }
}
