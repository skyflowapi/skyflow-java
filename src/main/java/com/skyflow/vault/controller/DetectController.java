package com.skyflow.vault.controller;

import com.skyflow.VaultClient;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.resources.strings.requests.DeidentifyStringRequest;
import com.skyflow.generated.rest.resources.strings.requests.ReidentifyStringRequest;
import com.skyflow.generated.rest.types.DeidentifyStringResponse;
import com.skyflow.generated.rest.types.ReidentifyStringResponse;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.detect.DeidentifyTextRequest;
import com.skyflow.vault.detect.DeidentifyTextResponse;
import com.skyflow.vault.detect.ReidentifyTextRequest;
import com.skyflow.vault.detect.ReidentifyTextResponse;


public final class DetectController extends VaultClient {

    public DetectController(VaultConfig vaultConfig, Credentials credentials) {
        super(vaultConfig, credentials);
    }

    public DeidentifyTextResponse deidentifyText(DeidentifyTextRequest deidentifyTextRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DEIDENTIFY_TEXT_TRIGGERED.getLog());
        DeidentifyStringResponse deidentifyStringResponse = null;
        DeidentifyTextResponse deidentifyTextResponse = null;
        try {
            // Validate the request
            Validations.validateDeidentifyTextRequest(deidentifyTextRequest);
            setBearerToken();

            // Parse the request to DeidentifyStringRequest
            String vaultId = super.getVaultConfig().getVaultId();
            DeidentifyStringRequest request = getDeidentifyStringRequest(deidentifyTextRequest, vaultId);

            // Call the API to de-identify the string
            deidentifyStringResponse = super.getDetectTextApi().deidentifyString(request);

            // Parse the response to DeIdentifyTextResponse
            deidentifyTextResponse = getDeIdentifyTextResponse(deidentifyStringResponse);
            LogUtil.printInfoLog(InfoLogs.DEIDENTIFY_TEXT_REQUEST_RESOLVED.getLog());
        } catch (ApiClientApiException ex) {
            LogUtil.printErrorLog(ErrorLogs.DEIDENTIFY_TEXT_REQUEST_REJECTED.getLog());
            throw new SkyflowException(ex.statusCode(), ex, ex.headers(), ex.body().toString());
        } catch (Exception e) {
            throw new SkyflowException(e.getMessage(), e);
        }
        return deidentifyTextResponse;
    }

    public ReidentifyTextResponse reidentifyText(ReidentifyTextRequest reidentifyTextRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.REIDENTIFY_TEXT_TRIGGERED.getLog());
        ReidentifyTextResponse reidentifyTextResponse = null;
        try {
            // Validate the request
            Validations.validateReidentifyTextRequest(reidentifyTextRequest);
            setBearerToken();
            // Parse the request to ReidentifyTextRequest
            String vaultId = super.getVaultConfig().getVaultId();
            ReidentifyStringRequest request = getReidentifyStringRequest(reidentifyTextRequest, vaultId);

            // Call the API to re-identify the string
            ReidentifyStringResponse reidentifyStringResponse = super.getDetectTextApi().reidentifyString(request);

            // Parse the response to ReidentifyTextResponse
            reidentifyTextResponse = new ReidentifyTextResponse(reidentifyStringResponse.getAdditionalProperties().get("text").toString());
            LogUtil.printInfoLog(InfoLogs.REIDENTIFY_TEXT_REQUEST_RESOLVED.getLog());
        } catch (ApiClientApiException ex) {
            LogUtil.printErrorLog(ErrorLogs.REIDENTIFY_TEXT_REQUEST_REJECTED.getLog());
            throw new SkyflowException(ex.statusCode(), ex, ex.headers(), ex.body().toString());
        } catch (Exception e) {
            throw new SkyflowException(e.getMessage(), e);
        }
        return reidentifyTextResponse;
    }
}