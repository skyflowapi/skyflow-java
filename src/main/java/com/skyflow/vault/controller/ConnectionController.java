package com.skyflow.vault.controller;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyflow.ConnectionClient;
import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.enums.RequestMethod;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.Constants;
import com.skyflow.utils.HttpUtility;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.connection.InvokeConnectionRequest;
import com.skyflow.vault.connection.InvokeConnectionResponse;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public final class ConnectionController extends ConnectionClient {
    public ConnectionController(ConnectionConfig connectionConfig, Credentials credentials) {
        super(connectionConfig, credentials);
    }

    public InvokeConnectionResponse invoke(InvokeConnectionRequest invokeConnectionRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.INVOKE_CONNECTION_TRIGGERED.getLog());
        InvokeConnectionResponse connectionResponse;
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_INVOKE_CONNECTION_REQUEST.getLog());
            Validations.validateInvokeConnectionRequest(invokeConnectionRequest);
            setBearerToken();
            String filledURL = Utils.constructConnectionURL(super.getConnectionConfig(), invokeConnectionRequest);
            Map<String, String> headers = new HashMap<>();

            Map<String, String> requestHeaders = invokeConnectionRequest.getRequestHeaders();
            if (requestHeaders != null) {
                headers = Utils.constructConnectionHeadersMap(invokeConnectionRequest.getRequestHeaders());
            }
            if (!headers.containsKey(Constants.SDK_AUTH_HEADER_KEY)) {
                headers.put(Constants.SDK_AUTH_HEADER_KEY, token == null ? apiKey : token);
            }
            headers.put(Constants.SDK_METRICS_HEADER_KEY, Utils.getMetrics().toString());

            RequestMethod requestMethod = invokeConnectionRequest.getMethod();
            JsonObject requestBody = null;
            Object requestBodyObject = invokeConnectionRequest.getRequestBody();

            if (requestBodyObject != null) {
                try {
                    requestBody = convertObjectToJson(requestBodyObject);
                } catch (Exception e) {
                    LogUtil.printErrorLog(ErrorLogs.INVALID_REQUEST_HEADERS.getLog());
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidRequestBody.getMessage());
                }
            }

            String response = HttpUtility.sendRequest(requestMethod.name(), new URL(filledURL), requestBody, headers);
            JsonObject data = JsonParser.parseString(response).getAsJsonObject();
            HashMap<String, String> metadata = new HashMap<>();
            metadata.put("requestId", HttpUtility.getRequestID());
            connectionResponse = new InvokeConnectionResponse(data, metadata, null);
            LogUtil.printInfoLog(InfoLogs.INVOKE_CONNECTION_REQUEST_RESOLVED.getLog());
        } catch (IOException e) {
            LogUtil.printErrorLog(ErrorLogs.INVOKE_CONNECTION_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage(), e);
        }
        return connectionResponse;
    }

    private JsonObject convertObjectToJson(Object object) {
        Gson gson = new Gson();
        JsonElement jsonElement = gson.toJsonTree(object);

        if (jsonElement.isJsonObject()) {
            return jsonElement.getAsJsonObject();
        } else {
            JsonObject wrapper = new JsonObject();
            wrapper.add("value", jsonElement);
            return wrapper;
        }
    }
}
