package com.skyflow.vault.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.skyflow.VaultClient;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.core.ApiClientException;
import com.skyflow.generated.rest.core.ApiClientHttpResponse;
import com.skyflow.generated.rest.core.RequestOptions;
import com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest;
import com.skyflow.generated.rest.types.V1FlowDeleteTokenResponse;
import com.skyflow.generated.rest.types.V1FlowTokenizeResponse;
import com.skyflow.generated.rest.types.V1InsertResponse;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.*;

import static com.skyflow.utils.Utils.*;

public final class VaultController extends VaultClient
        implements IVaultController<InsertRequest, InsertResponse, DetokenizeRequest, DetokenizeResponse> {
    private static final Gson gson = new GsonBuilder().serializeNulls().create();
    private JsonObject metrics = Utils.getMetrics();

    public VaultController(VaultConfig vaultConfig, Credentials credentials) throws SkyflowException {
        super(vaultConfig, credentials);
    }
    private RequestOptions buildRequestOptions() {
        RequestOptions.Builder builder = RequestOptions.builder()
                .addHeader(Constants.SDK_METRICS_HEADER_KEY, metrics.toString());
        return builder.build();
    }

    @Override
    public InsertResponse insert(InsertRequest request) throws SkyflowException {
        try {
            LogUtil.printInfoLog(InfoLogs.INSERT_TRIGGERED.getLog());
            LogUtil.printInfoLog(InfoLogs.VALIDATE_INSERT_REQUEST.getLog());
            Validations.validateInsertRequest(request);
            setBearerToken();
            V1InsertRequest insertRequest = getBulkInsertRequestBody(request, super.getVaultConfig());
            V1InsertResponse response = this.getRecordsApi().withRawResponse().insert(insertRequest, buildRequestOptions()).body();
            return buildInsertResponse(response);
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        }
    }

    @Override
    public DetokenizeResponse detokenize(DetokenizeRequest request) throws SkyflowException {
        try {
            LogUtil.printInfoLog(InfoLogs.DETOKENIZE_TRIGGERED.getLog());
            LogUtil.printInfoLog(InfoLogs.VALIDATE_DETOKENIZE_REQUEST.getLog());
            Validations.validateDetokenizeRequest(request);
            setBearerToken();

            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest requestDetokenize = getDetokenizeRequestBody(request, this.getVaultConfig().getVaultId());
            com.skyflow.generated.rest.types.V1FlowDetokenizeResponse response = this.getRecordsApi().withRawResponse().detokenize(requestDetokenize, buildRequestOptions()).body();

            return buildDetokenizeResponse(response);
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.DETOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.DETOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        }
    }

    public TokenizeResponse tokenize(TokenizeRequest request) throws SkyflowException {
        try {
            LogUtil.printInfoLog(InfoLogs.TOKENIZE_TRIGGERED.getLog());
            LogUtil.printInfoLog(InfoLogs.VALIDATING_TOKENIZE_REQUEST.getLog());
            Validations.validateTokenizeRequest(request);
            setBearerToken();

            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest requestTokenize = getTokenizeRequestBody(request, this.getVaultConfig().getVaultId());
            ApiClientHttpResponse<V1FlowTokenizeResponse> response = this.getRecordsApi().withRawResponse().tokenize(requestTokenize, buildRequestOptions());
            LogUtil.printInfoLog(InfoLogs.TOKENIZE_REQUEST_RESOLVED.getLog());

            TokenizeResponse tokenizeResponse = buildTokenizeResponse(response.body(), response.headers(), request.getData().size());
            LogUtil.printInfoLog(InfoLogs.TOKENIZE_SUCCESS.getLog());
            return tokenizeResponse;
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.TOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.TOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        }
    }

    public DeleteTokensResponse deleteTokens(DeleteTokensRequest request) throws SkyflowException {
        try {
            LogUtil.printInfoLog(InfoLogs.DELETE_TOKENS_TRIGGERED.getLog());
            LogUtil.printInfoLog(InfoLogs.VALIDATE_DELETE_TOKENS_REQUEST.getLog());
            Validations.validateDeleteTokensRequest(request);
            setBearerToken();

            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest requestDeleteTokens = getDeleteTokensRequestBody(request, this.getVaultConfig().getVaultId());
            ApiClientHttpResponse<V1FlowDeleteTokenResponse> response = this.getRecordsApi().withRawResponse().deletetoken(requestDeleteTokens, buildRequestOptions());
            LogUtil.printInfoLog(InfoLogs.DELETE_TOKENS_REQUEST_RESOLVED.getLog());

            DeleteTokensResponse deleteTokensResponse = buildDeleteTokensResponse(response.body(), response.headers(), request.getTokens().size());
            LogUtil.printInfoLog(InfoLogs.DELETE_TOKENS_SUCCESS.getLog());
            return deleteTokensResponse;
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.DELETE_TOKENS_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.DELETE_TOKENS_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        }
    }


}