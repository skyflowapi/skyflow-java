package com.skyflow.vault;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyflow.common.utils.Helpers;
import com.skyflow.common.utils.HttpUtility;
import com.skyflow.common.utils.TokenUtils;
import com.skyflow.common.utils.Validators;
import com.skyflow.entities.InsertInput;
import com.skyflow.entities.InsertOptions;
import com.skyflow.entities.SkyflowConfiguration;
import com.skyflow.errors.SkyflowException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.skyflow.errors.ErrorCodesEnum.InvalidInput;
import static com.skyflow.errors.ErrorCodesEnum.Server;

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
            throw new SkyflowException(InvalidInput, "Invalid insert input", e);
        } catch (SkyflowException e) {
            throw e;
        } catch (ParseException e) {
            throw new SkyflowException(Server, "Unable to parse insert response", e);
        }

        return insertResponse;
    }
}
