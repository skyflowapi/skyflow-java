/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.vault;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyflow.common.utils.*;
import com.skyflow.entities.*;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public final class Skyflow {
    private final SkyflowConfiguration configuration;

    private Skyflow(SkyflowConfiguration config) {
        this.configuration = config;
        LogUtil.printInfoLog(InfoLogs.InitializedClient.getLog());
    }

    public static Skyflow init(SkyflowConfiguration clientConfig) throws SkyflowException {
        return new Skyflow(clientConfig);
    }

    public JSONObject insert(JSONObject records) throws SkyflowException {
        return insert(records, new InsertOptions(true));
    }

    public JSONObject insert(JSONObject records, InsertOptions insertOptions) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.InsertMethodCalled.getLog());
        Validators.validateConfiguration(configuration);
        LogUtil.printInfoLog(Helpers.parameterizedString(InfoLogs.ValidatedSkyflowConfiguration.getLog(), "insert"));

        if(insertOptions.getUpsertOptions() != null)
            Validators.validateUpsertOptions(insertOptions.getUpsertOptions());
        JSONObject insertResponse = null;
        try {
            InsertInput insertInput = new ObjectMapper().readValue(records.toString(), InsertInput.class);
            JSONObject requestBody = Helpers.constructInsertRequest(insertInput, insertOptions);

            String url = configuration.getVaultURL() + "/v1/vaults/" + configuration.getVaultID();

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + TokenUtils.getBearerToken(configuration.getTokenProvider()));

            String response = HttpUtility.sendRequest("POST", new URL(url), requestBody, headers);
            insertResponse = (JSONObject) new JSONParser().parse(response);
            LogUtil.printInfoLog(InfoLogs.ConstructInsertResponse.getLog());
            insertResponse = Helpers.constructInsertResponse(insertResponse, (List) requestBody.get("records"), insertOptions.isTokens());
        } catch (IOException e) {
            LogUtil.printErrorLog(ErrorLogs.InvalidInsertInput.getLog());
            throw new SkyflowException(ErrorCode.InvalidInsertInput, e);
        } catch (ParseException e) {
            LogUtil.printErrorLog(Helpers.parameterizedString(ErrorLogs.ResponseParsingError.getLog(), "insert"));
            throw new SkyflowException(ErrorCode.ResponseParsingError, e);
        }

        return insertResponse;
    }

    public JSONObject detokenize(JSONObject records) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DetokenizeMethodCalled.getLog());
        Validators.validateConfiguration(configuration);
        LogUtil.printInfoLog(Helpers.parameterizedString(InfoLogs.ValidatedSkyflowConfiguration.getLog(), "detokenize"));

        JSONObject finalResponse = new JSONObject();
        JSONArray successRecordsArray = new JSONArray();
        JSONArray errorRecordsArray = new JSONArray();
        try {
            DetokenizeInput detokenizeInput = new ObjectMapper().readValue(records.toJSONString(), DetokenizeInput.class);
            DetokenizeRecord[] inputRecords = detokenizeInput.getRecords();

            if (inputRecords == null || inputRecords.length == 0) {
                throw new SkyflowException(ErrorCode.EmptyRecords);
            }

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
                String taskData = (String) task.get();
                JSONParser parser = new JSONParser();
                JSONObject responseJson = (JSONObject) parser.parse(taskData);
                if (responseJson.containsKey("error")) {
                    errorRecordsArray.add(responseJson);
                } else if (responseJson.containsKey("value")) {
                    successRecordsArray.add(responseJson);
                }
            }
            if (errorRecordsArray.isEmpty()) {
                finalResponse.put("records", successRecordsArray);
            } else if (successRecordsArray.isEmpty()) {
                finalResponse.put("errors", errorRecordsArray);
                throw new SkyflowException(500, "Server returned errors, check SkyflowException.getData() for more", finalResponse);
            } else {
                finalResponse.put("records", successRecordsArray);
                finalResponse.put("errors", errorRecordsArray);
                throw new SkyflowException(500, "Server returned errors, check SkyflowException.getData() for more", finalResponse);
            }
        } catch (IOException exception) {
            LogUtil.printErrorLog(ErrorLogs.InvalidDetokenizeInput.getLog());
            throw new SkyflowException(ErrorCode.InvalidDetokenizeInput, exception);
        } catch (InterruptedException e) {
            LogUtil.printErrorLog(Helpers.parameterizedString(ErrorLogs.ThreadInterruptedException.getLog(), "detokenize"));
            throw new SkyflowException(ErrorCode.ThreadInterruptedException, e);
        } catch (ExecutionException e) {
            LogUtil.printErrorLog(Helpers.parameterizedString(ErrorLogs.ThreadExecutionException.getLog(), "detokenize"));
            throw new SkyflowException(ErrorCode.ThreadExecutionException, e);
        } catch (ParseException e) {
            LogUtil.printErrorLog(Helpers.parameterizedString(ErrorLogs.ResponseParsingError.getLog(), "detokenize"));
            throw new SkyflowException(ErrorCode.ResponseParsingError, e);
        }
        return finalResponse;
    }

    public JSONObject getById(JSONObject getByIdInput) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.GetByIdMethodCalled.getLog());
        Validators.validateConfiguration(configuration);
        LogUtil.printInfoLog(Helpers.parameterizedString(InfoLogs.ValidatedSkyflowConfiguration.getLog(), "getById"));

        JSONObject finalResponse = new JSONObject();
        JSONArray successRecordsArray = new JSONArray();
        JSONArray errorRecordsArray = new JSONArray();
        try {
            GetByIdInput input = new ObjectMapper().readValue(getByIdInput.toString(), GetByIdInput.class);
            GetByIdRecordInput[] recordInputs = input.getRecords();

            if (recordInputs == null || recordInputs.length == 0) {
                throw new SkyflowException(ErrorCode.EmptyRecords);
            }

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
                throw new SkyflowException(500, "Server returned errors, check SkyflowException.getData() for more", finalResponse);
            } else {
                finalResponse.put("records", successRecordsArray);
                finalResponse.put("errors", errorRecordsArray);
                throw new SkyflowException(500, "Server returned errors, check SkyflowException.getData() for more", finalResponse);
            }

        } catch (IOException e) {
            LogUtil.printErrorLog(ErrorLogs.InvalidGetByIdInput.getLog());
            throw new SkyflowException(ErrorCode.InvalidGetByIdInput, e);
        } catch (InterruptedException e) {
            LogUtil.printErrorLog(Helpers.parameterizedString(ErrorLogs.ThreadInterruptedException.getLog(), "getById"));
            throw new SkyflowException(ErrorCode.ThreadInterruptedException, e);
        } catch (ExecutionException e) {
            LogUtil.printErrorLog(Helpers.parameterizedString(ErrorLogs.ThreadExecutionException.getLog(), "getById"));
            throw new SkyflowException(ErrorCode.ThreadExecutionException, e);
        } catch (ParseException e) {
            LogUtil.printErrorLog(Helpers.parameterizedString(ErrorLogs.ResponseParsingError.getLog(), "getById"));
            throw new SkyflowException(ErrorCode.ResponseParsingError, e);
        }

        return finalResponse;
    }

    public JSONObject invokeConnection(JSONObject connectionConfig) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.InvokeConnectionCalled.getLog());
        JSONObject connectionResponse;
        try {
            Validators.validateConnectionConfiguration(connectionConfig, configuration);
            String filledURL = Helpers.constructConnectionURL(connectionConfig);

            Map<String, String> headers = new HashMap<>();

            if (connectionConfig.containsKey("requestHeader")) {
              headers = Helpers.constructConnectionHeadersMap((JSONObject) connectionConfig.get("requestHeader"));
            }
            if(!headers.containsKey("x-skyflow-authorization")) {
              headers.put("x-skyflow-authorization", TokenUtils.getBearerToken(configuration.getTokenProvider()));
            }

            String requestMethod = connectionConfig.get("methodName").toString();
            JSONObject requestBody = null;
            if (connectionConfig.containsKey("requestBody")) {
                requestBody = (JSONObject) connectionConfig.get("requestBody");
            }

            String response = HttpUtility.sendRequest(requestMethod, new URL(filledURL), requestBody, headers);
            connectionResponse = (JSONObject) new JSONParser().parse(response);

        } catch (IOException exception) {
            LogUtil.printErrorLog(ErrorLogs.InvalidInvokeConnectionInput.getLog());
            throw new SkyflowException(ErrorCode.InvalidConnectionInput, exception);
        } catch (ParseException exception) {
            LogUtil.printErrorLog(Helpers.parameterizedString(ErrorLogs.ResponseParsingError.getLog(), "invokeConnection"));
            throw new SkyflowException(ErrorCode.ResponseParsingError, exception);
        }
        return connectionResponse;
    }
}
