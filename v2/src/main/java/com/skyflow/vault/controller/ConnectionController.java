package com.skyflow.vault.controller;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.skyflow.ConnectionClient;
import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.auth.HttpBearerAuth;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.utils.HttpUtility;
import com.skyflow.utils.Utils;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.connection.InvokeConnectionRequest;
import com.skyflow.vault.connection.InvokeConnectionResponse;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URL;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public class ConnectionController extends ConnectionClient {
    private ConnectionConfig connectionConfig;
    private Credentials commonCredentials;

    private String token;

    public ConnectionController(ConnectionConfig connectionConfig, Credentials credentials) {
        super(connectionConfig, credentials);
        this.connectionConfig = connectionConfig;
        this.commonCredentials = credentials;
    }

    public void setCommonCredentials(Credentials commonCredentials) {
        this.commonCredentials = commonCredentials;
    }

    public ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public void setConnectionConfig(ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    public InvokeConnectionResponse invoke(InvokeConnectionRequest invokeConnectionRequest) throws SkyflowException, IOException {

        InvokeConnectionResponse connectionResponse;
        try {
            Validations.validateInvokeConnectionRequest(invokeConnectionRequest);
            setBearerToken();
            String filledURL = Utils.constructConnectionURL(connectionConfig, invokeConnectionRequest);
            Map<String, String> headers = new HashMap<>();

            if (invokeConnectionRequest.getRequestHeaders().containsKey("requestHeader")) {
                headers = Utils.constructConnectionHeadersMap(invokeConnectionRequest.getRequestHeaders());
            }
            if (!headers.containsKey("x-skyflow-authorization")) {
                headers.put("x-skyflow-authorization", token);
            }

            String requestMethod = invokeConnectionRequest.getMethodName();
            JsonObject requestBody = null;
            Object requestBodyObject = invokeConnectionRequest.getRequestBody();

            if(requestBodyObject!=null) {
                try {
                    requestBody = convertObjectToJson(requestBodyObject);
                } catch (Exception e) {
                    throw new SkyflowException();
                }
            }

            String response = HttpUtility.sendRequest(requestMethod, new URL(filledURL), requestBody, headers);
            connectionResponse = new InvokeConnectionResponse((JsonObject) new JsonParser().parse(response));
        } catch (IOException e) {
            throw new SkyflowException();
        }
        return connectionResponse;
    }

    private void setBearerToken() throws SkyflowException {
        Validations.validateCredentials(super.getFinalCredentials());
        if (token == null || Token.isExpired(token)) {
            token = Utils.generateBearerToken(super.getFinalCredentials());
        }
        HttpBearerAuth Bearer = (HttpBearerAuth) super.getApiClient().getAuthentication("Bearer");
        Bearer.setBearerToken(token);
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
