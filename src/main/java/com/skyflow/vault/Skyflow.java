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

/**
 * The parent Skyflow class consists of all the methods exposed to the client.
 */
public final class Skyflow {
    private final SkyflowConfiguration configuration;

    private Skyflow(SkyflowConfiguration config) {
        this.configuration = config;
        LogUtil.printInfoLog(InfoLogs.InitializedClient.getLog());
    }

    /**
     * Initialises skyflow client.
     * @param clientConfig The configuration required for Skyflow client initialisation.
     * @return Returns an instance of skyflow client.
     * @throws SkyflowException Throws an exception when we encounter any error scenario.
     */
    public static Skyflow init(SkyflowConfiguration clientConfig) throws SkyflowException {
        return new Skyflow(clientConfig);
    }


    /**
     * Inserts data into the vault.
     * @param records A JSONObject that must have a records key and takes an array of records to insert as a value in the vault.
     * @return Returns the response for the insert operation.
     * @throws SkyflowException Throws an exception when we encounter any error scenario.
     */
    public JSONObject insert(JSONObject records) throws SkyflowException {
        return insert(records, new InsertOptions(true));
    }

    /**
     * Updates the records in your vault by skyflow_id.
     * @param records A JSONObject that must have a records key and takes an array of records to update as a value in the vault.
     * @return Returns the response for the update operation.
     * @throws SkyflowException Throws an exception when we encounter any error scenario.
     */
    public JSONObject update(JSONObject records) throws SkyflowException {
        return update(records, new UpdateOptions(true));
    }

    /**
     * Inserts data into the vault.
     * @param records A JSONObject that must have a records key and takes an array of records to insert as a value in the vault.
     * @param insertOptions Additional options for insert method/request.
     * @return Returns the response for the insert operation.
     * @throws SkyflowException Throws an exception when we encounter any error scenario.
     */
    public JSONObject insert(JSONObject records, InsertOptions insertOptions) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.InsertMethodCalled.getLog());
        Validators.validateConfiguration(configuration);
        LogUtil.printInfoLog(Helpers.parameterizedString(InfoLogs.ValidatedSkyflowConfiguration.getLog(), "insert"));

        if (insertOptions.getUpsertOptions() != null)
            Validators.validateUpsertOptions(insertOptions.getUpsertOptions());
        JSONObject insertResponse = null;
        try {
            InsertInput insertInput = new ObjectMapper().readValue(records.toString(), InsertInput.class);
            JSONObject requestBody = Helpers.constructInsertRequest(insertInput, insertOptions);

            String url = configuration.getVaultURL() + "/v1/vaults/" + configuration.getVaultID();

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + TokenUtils.getBearerToken(configuration.getTokenProvider()));
            headers.put(Constants.SDK_METRICS_HEADER_KEY, Helpers.getMetrics().toJSONString());
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

    /**
     * Retrieves record the data using tokens.
     * @param records This parameter takes a JSON object that contains tokens for fetching record values.
     * @return Returns the response for the detokenize operation.
     * @throws SkyflowException Throws an exception when we encounter any error scenario.
     */
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
            headers.put(Constants.SDK_METRICS_HEADER_KEY, Helpers.getMetrics().toJSONString());

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

    /**
     * Retrieves using SkyflowID's.
     * @param getByIdInput A JSONObject that must have a records key and takes an array of records to get from the vault.
     * @return Returns response of the getById.
     * @throws SkyflowException Throws an exception when we encounter any error scenario.
     */
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
            headers.put(Constants.SDK_METRICS_HEADER_KEY, Helpers.getMetrics().toJSONString());

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
                ErrorCode serverError = ErrorCode.ServerReturnedErrors;
                throw new SkyflowException(serverError.getCode(), serverError.getDescription(), finalResponse);
            } else {
                finalResponse.put("records", successRecordsArray);
                finalResponse.put("errors", errorRecordsArray);
                ErrorCode serverError = ErrorCode.ServerReturnedErrors;
                throw new SkyflowException(serverError.getCode(), serverError.getDescription(), finalResponse);
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

    /**
     * Retrieves using SkyflowID's and column values.
     * @param getInput A JSONObject that must have a records key and takes an array of records to get from the vault.
     * @return Returns response of the get.
     * @throws SkyflowException Throws an exception when we encounter any error scenario.
     */
    public JSONObject get(JSONObject getInput) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.GetMethodCalled.getLog());
        Validators.validateConfiguration(configuration);
        LogUtil.printInfoLog(Helpers.parameterizedString(InfoLogs.ValidatedSkyflowConfiguration.getLog(), "get"));

        JSONObject finalResponse = new JSONObject();
        JSONArray successRecordsArray = new JSONArray();
        JSONArray errorRecordsArray = new JSONArray();
        try {
            GetInput input = new ObjectMapper().readValue(getInput.toString(), GetInput.class);
            GetRecordInput[] recordInputs = input.getRecords();

            if (recordInputs == null || recordInputs.length == 0) {
                throw new SkyflowException(ErrorCode.EmptyRecords);
            }

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + TokenUtils.getBearerToken(configuration.getTokenProvider()));
            headers.put(Constants.SDK_METRICS_HEADER_KEY, Helpers.getMetrics().toJSONString());

            FutureTask[] futureTasks = new FutureTask[recordInputs.length];
            for (int i = 0; i < recordInputs.length; i++) {
                Callable<String> callable = new Get(recordInputs[i], configuration.getVaultID(), configuration.getVaultURL(), headers);
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
                ErrorCode serverError = ErrorCode.ServerReturnedErrors;
                throw new SkyflowException(serverError.getCode(), serverError.getDescription(), finalResponse);
            } else {
                finalResponse.put("records", successRecordsArray);
                finalResponse.put("errors", errorRecordsArray);
                ErrorCode serverError = ErrorCode.ServerReturnedErrors;
                throw new SkyflowException(serverError.getCode(), serverError.getDescription(), finalResponse);
            }

        } catch (IOException e) {
            LogUtil.printErrorLog(ErrorLogs.InvalidGetInput.getLog());
            throw new SkyflowException(ErrorCode.InvalidGetInput, e);
        } catch (InterruptedException e) {
            LogUtil.printErrorLog(Helpers.parameterizedString(ErrorLogs.ThreadInterruptedException.getLog(), "get"));
            throw new SkyflowException(ErrorCode.ThreadInterruptedException, e);
        } catch (ExecutionException e) {
            LogUtil.printErrorLog(Helpers.parameterizedString(ErrorLogs.ThreadExecutionException.getLog(), "get"));
            throw new SkyflowException(ErrorCode.ThreadExecutionException, e);
        } catch (ParseException e) {
            LogUtil.printErrorLog(Helpers.parameterizedString(ErrorLogs.ResponseParsingError.getLog(), "get"));
            throw new SkyflowException(ErrorCode.ResponseParsingError, e);
        }

        return finalResponse;
    }

    /**
     * Updates the records in your vault by skyflow_id.
     * @param records A JSONObject that must have a records key and takes an array of records to update as a value in the vault.
     * @param updateOptions Additional parameters for the update.
     * @return Returns the response of the update.
     * @throws SkyflowException Throws an exception when we encounter any error scenario.
     */
    public JSONObject update(JSONObject records, UpdateOptions updateOptions) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.UpdateMethodCalled.getLog());
        Validators.validateConfiguration(configuration);
        LogUtil.printInfoLog(Helpers.parameterizedString(InfoLogs.ValidatedSkyflowConfiguration.getLog(), "update"));

        JSONArray successRecordsArray = new JSONArray();
        JSONArray errorRecordsArray = new JSONArray();

        JSONObject updateResponse = new JSONObject();

        try {
            UpdateInput updateInput = new ObjectMapper().readValue(records.toString(), UpdateInput.class);
            UpdateRecordInput[] recordInputs = updateInput.getRecords();
            if (recordInputs == null || recordInputs.length == 0) {
                throw new SkyflowException(ErrorCode.EmptyRecords);
            }

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + TokenUtils.getBearerToken(configuration.getTokenProvider()));
            headers.put(Constants.SDK_METRICS_HEADER_KEY, Helpers.getMetrics().toJSONString());

            FutureTask[] futureTasks = new FutureTask[recordInputs.length];
            for (int i = 0; i < recordInputs.length; i++) {
                Callable<String> callable = new UpdateBySkyflowId(recordInputs[i], configuration.getVaultID(), configuration.getVaultURL(), headers, updateOptions);
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
                    successRecordsArray.add(responseJson.get("records"));
                }
            }
            if (errorRecordsArray.isEmpty()) {
                updateResponse.put("records", successRecordsArray);
            } else if (successRecordsArray.isEmpty()) {
                updateResponse.put("error", errorRecordsArray);
                throw new SkyflowException(500, "Server returned errors, check SkyflowException.Update() for more", updateResponse);
            } else {
                updateResponse.put("records", successRecordsArray);
                updateResponse.put("error", errorRecordsArray);
                throw new SkyflowException(500, "Server returned errors, check SkyflowException.Update() for more", updateResponse);
            }
        } catch (IOException e) {
            LogUtil.printErrorLog(ErrorLogs.InvalidUpdateInput.getLog());
            throw new SkyflowException(ErrorCode.InvalidUpdateInput, e);
        } catch (InterruptedException e) {
            LogUtil.printErrorLog(Helpers.parameterizedString(ErrorLogs.ThreadInterruptedException.getLog(), "updateById"));
            throw new SkyflowException(ErrorCode.ThreadInterruptedException, e);
        } catch (ExecutionException e) {
            LogUtil.printErrorLog(Helpers.parameterizedString(ErrorLogs.ThreadExecutionException.getLog(), "updateById"));
            throw new SkyflowException(ErrorCode.ThreadExecutionException, e);
        } catch (ParseException e) {
            LogUtil.printErrorLog(Helpers.parameterizedString(ErrorLogs.ResponseParsingError.getLog(), "updateById"));
            throw new SkyflowException(ErrorCode.ResponseParsingError, e);
        }
        return updateResponse;

    }

    /**
     * Integrates their server-side application with third party APIs and services without directly handling sensitive data.
     * @param connectionConfig Configuration required to establish a connection.##
     * @return Returns the connection response.
     * @throws SkyflowException Throws an exception when we encounter any error scenario.
     */
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
            if (!headers.containsKey("x-skyflow-authorization")) {
                headers.put("x-skyflow-authorization", TokenUtils.getBearerToken(configuration.getTokenProvider()));
            }
            headers.put(Constants.SDK_METRICS_HEADER_KEY, Helpers.getMetrics().toJSONString());
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
