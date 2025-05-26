package com.skyflow.vault.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skyflow.VaultClient;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.generated.rest.resources.strings.requests.DeidentifyStringRequest;
import com.skyflow.generated.rest.types.DeidentifyStringResponse;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.vault.detect.DeIdentifyTextRequest;

public final class DetectController extends VaultClient {
    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    public DetectController(VaultConfig vaultConfig, Credentials credentials) {
        super(vaultConfig, credentials);
    }

    public String deIdentifyText(DeIdentifyTextRequest deIdentifyTextRequest) {
        LogUtil.printInfoLog(InfoLogs.DEIDENTIFY_TEXT_TRIGGERED.getLog());
        DeidentifyStringResponse deidentifyStringResponse = null;
        String res = null;
        try {
            // Validate the request
            // Parse the request to DeidentifyStringRequest
            String vaultId = super.getVaultConfig().getVaultId();
            DeidentifyStringRequest request = getDeidentifyStringRequest(deIdentifyTextRequest, vaultId);

            setBearerToken();

            deidentifyStringResponse = super.getDetectTextApi().deidentifyString(request);

            System.out.println("## Responsee: " + deidentifyStringResponse.toString());

            res = gson.toJson(deidentifyStringResponse);
        } catch (Exception ex) {
            System.out.println("## Error: " + ex.getMessage());
            ex.printStackTrace();
        }
        return res;
    }

    private static DeidentifyStringRequest getDeidentifyStringRequest(DeIdentifyTextRequest deIdentifyTextRequest, String vaultId) {
        return
                DeidentifyStringRequest.builder()
                        .vaultId(vaultId)
                        .text(deIdentifyTextRequest.getText())
//                        .entityTypes(deIdentifyTextRequest.getEntities())
//                        .tokenType(deIdentifyTextRequest.getTokenFormat())
                        .allowRegex(deIdentifyTextRequest.getAllowRegexList())
                        .restrictRegex(deIdentifyTextRequest.getRestrictRegexList())
                        .transformations(deIdentifyTextRequest.getTransformations())
//                        .additionalProperties(deIdentifyTextRequest.getTransformations().getAdditionalProperties())
                        .build();
    }
}