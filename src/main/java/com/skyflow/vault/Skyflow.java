package com.skyflow.vault;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyflow.common.utils.Helpers;
import com.skyflow.common.utils.HttpUtility;
import com.skyflow.common.utils.TokenUtils;
import com.skyflow.common.utils.Validators;
import com.skyflow.entities.*;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class Skyflow {
    private final SkyflowConfiguration configuration;

    private Skyflow(SkyflowConfiguration config) {
        this.configuration = config;
    }

    public static Skyflow init(SkyflowConfiguration clientConfig) throws SkyflowException {
        Validators.validateConfiguration(clientConfig);
        return new Skyflow(clientConfig);
    }

    public JSONObject insert(JSONObject records, InsertOptions insertOptions) throws SkyflowException {
        JSONObject insertResponse = null;
        try {
            InsertInput insertInput = new ObjectMapper().readValue(records.toString(), InsertInput.class);
            JSONObject requestBody = Helpers.constructInsertRequest(insertInput, insertOptions);

            String url = configuration.getVaultURL() + "/v1/vaults/" + configuration.getVaultID();

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + TokenUtils.getBearerToken(configuration.getTokenProvider()));

            String response = HttpUtility.sendRequest("POST", url, requestBody, headers);
            insertResponse = (JSONObject) new JSONParser().parse(response);
            insertResponse = Helpers.constructInsertResponse(insertResponse, (List) requestBody.get("records"), insertOptions.isTokens());
        } catch (IOException e) {
            throw new SkyflowException(ErrorCode.InvalidInsertInput, e);
        } catch (SkyflowException e) {
            throw e;
        } catch (ParseException e) {
            throw new SkyflowException(ErrorCode.ResponseParsingError, e);
        }

        return insertResponse;
    }

    public JSONObject detokenize(JSONObject records) throws SkyflowException {
        JSONArray successRecordsArray = new JSONArray();
        JSONArray errorRecordsArray = new JSONArray();
        try {
            DetokenizeInput detokenizeInput = new ObjectMapper().readValue(records.toJSONString(), DetokenizeInput.class);
            DetokenizeRecord[] inputRecords = detokenizeInput.getRecords();
            System.out.println(Arrays.toString(inputRecords));
            String apiEndpointURL = this.configuration.getVaultURL() + "/v1/vaults/" + this.configuration.getVaultID() + "/detokenize";
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + TokenUtils.getBearerToken(configuration.getTokenProvider()));

            FutureTask[] futureTasks = new FutureTask[inputRecords.length];
            for (int index = 0; index < inputRecords.length; index++) {
                Callable<String> callable = new Detokenize(inputRecords[index], apiEndpointURL, headers);
                futureTasks[index] = new FutureTask(callable);
                Thread thread = new Thread(futureTasks[index]);
                thread.start();
            }
            for (FutureTask task : futureTasks) {
                try {
                    String taskData = (String) task.get();
                    System.out.println(taskData);
                    JSONParser parser = new JSONParser();
                    JSONObject responseJson = (JSONObject) parser.parse(taskData);
                    if (responseJson.containsKey("error")) {
                        errorRecordsArray.add(responseJson);
                    } else if (responseJson.containsKey("value")) {
                        successRecordsArray.add(responseJson);
                    }

                } catch (ExecutionException | ParseException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException exception) {
            throw new SkyflowException(ErrorCode.InvalidDetokenizeInput, exception);
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }
        JSONObject finalResponse = new JSONObject();
        if (errorRecordsArray.isEmpty()) {
            finalResponse.put("records", successRecordsArray);
            return finalResponse;
        } else if (successRecordsArray.isEmpty()) {
            finalResponse.put("errors", errorRecordsArray);
            throw new SkyflowException(400, "partial error", finalResponse);
        } else {
            finalResponse.put("records", successRecordsArray);
            finalResponse.put("errors", errorRecordsArray);
            throw new SkyflowException(400, "partial error", finalResponse);
        }

    }

    public JSONObject getById(JSONObject getByIdInput) throws SkyflowException {
        JSONObject finalResponse = new JSONObject();
        JSONArray successRecordsArray = new JSONArray();
        JSONArray errorRecordsArray = new JSONArray();
        try {
            GetByIdInput input = new ObjectMapper().readValue(getByIdInput.toString(), GetByIdInput.class);
            GetByIdRecordInput[] recordInputs = input.getRecords();

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + TokenUtils.getBearerToken(configuration.getTokenProvider()));

            FutureTask[] futureTasks = new FutureTask[recordInputs.length];
            for (int i = 0; i < recordInputs.length; i++) {
                Callable<String> callable = new GetBySkyflowId(recordInputs[i], configuration.getVaultID(), configuration.getVaultURL(), headers);
                futureTasks[i] = new FutureTask(callable);

                Thread t = new Thread(futureTasks[i]);
                t.start();
            }

            for (FutureTask task : futureTasks) {
                String taskData = (String) task.get();
                JSONObject responseJson = (JSONObject) new JSONParser().parse(taskData);
                if (responseJson.containsKey("error")) {
                    errorRecordsArray.add(responseJson);
                } else if (responseJson.containsKey("records")) {
                    successRecordsArray.addAll((Collection) responseJson.get("records"));
                }
            }

            if (errorRecordsArray.isEmpty()) {
                finalResponse.put("records", successRecordsArray);
            } else if (successRecordsArray.isEmpty()) {
                finalResponse.put("errors", errorRecordsArray);
                throw new SkyflowException(400, "error", finalResponse);
            } else {
                finalResponse.put("records", successRecordsArray);
                finalResponse.put("errors", errorRecordsArray);
                throw new SkyflowException(400, "partial error", finalResponse);
            }

        } catch (IOException e) {
            throw new SkyflowException(ErrorCode.InvalidGetByIdInput, e);
        } catch (InterruptedException e) {
            throw new SkyflowException(ErrorCode.ThreadInterruptedException, e);
        } catch (ExecutionException e) {
            throw new SkyflowException(ErrorCode.ThreadExecutionException, e);
        } catch (ParseException e) {
            throw new SkyflowException(ErrorCode.ResponseParsingError, e);
        }

        return finalResponse;
    }
}
