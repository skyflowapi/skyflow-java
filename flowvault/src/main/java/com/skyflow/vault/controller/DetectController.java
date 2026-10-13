package com.skyflow.vault.controller;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.detect.DeidentifyFileRequest;
import com.skyflow.detect.DeidentifyFileResponse;
import com.skyflow.detect.GetRunRequest;
import com.skyflow.detect.ReidentifyFileRequest;
import com.skyflow.detect.ReidentifyFileResponse;
import com.skyflow.detect.internal.DeidentifyFileFlow;
import com.skyflow.detect.internal.DetectRequestMapper;
import com.skyflow.detect.internal.DetectValidations;
import com.skyflow.detect.internal.ReidentifyFileMapper;
import com.skyflow.detect.internal.ReidentifyFileValidations;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.detect.rest.core.ApiClientApiException;
import com.skyflow.generated.detect.rest.core.ApiClientException;
import com.skyflow.generated.detect.rest.core.ObjectMappers;
import com.skyflow.generated.detect.rest.core.RequestOptions;
import com.skyflow.generated.detect.rest.resources.filesv2.requests.DeidentifyFileRequestV2;
import com.skyflow.generated.detect.rest.resources.filesv2.requests.ReidentifyFileRequestV2;
import com.skyflow.generated.detect.rest.types.DeidentifyFileResponseV2;
import com.skyflow.generated.detect.rest.types.ReidentifyFileResponseV2;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.BaseConstants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;

/**
 * Skyflow Detect operations for a flowvault vault, returned by {@code Skyflow.detect()} and
 * {@code Skyflow.detect(vaultId)}. The shared Detect V2 methods are inherited from
 * {@link BaseDetectController}; this class adds the flowvault-only {@link #deidentifyFile} and
 * {@link #reidentifyFile} and
 * supplies the flowvault URL rules, SDK metrics and the vault-level timeout and retry overrides
 * from {@link VaultConfig}.
 */
public final class DetectController extends BaseDetectController<VaultConfig> {
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_MAX_RETRIES = 0;
    private static final long DEFAULT_INITIAL_RETRY_DELAY_MILLIS = 500L;
    private static final long DEFAULT_MAX_RETRY_DELAY_MILLIS = 2000L;

    public DetectController(VaultConfig vaultConfig, Credentials credentials) {
        super(vaultConfig, credentials);
    }

    /** Same precedence as the vault controller: environment variable, explicit vaultUrl, then cluster id and env. */
    @Override
    protected String resolveVaultUrl() throws SkyflowException {
        String vaultUrl = Utils.getEnvVaultUrl();
        if (vaultUrl == null || vaultUrl.isEmpty()) {
            vaultUrl = getVaultConfig().getVaultUrl();
        }
        if (vaultUrl == null || vaultUrl.isEmpty()) {
            vaultUrl = Utils.getVaultUrl(getVaultConfig().getClusterId(), getVaultConfig().getEnv());
        }
        return vaultUrl;
    }

    @Override
    protected String getSdkMetrics() {
        return Utils.getMetrics().toString();
    }

    @Override
    protected Integer effectiveTimeoutSeconds() {
        return firstNonNull(getVaultConfig().getTimeout(), super.effectiveTimeoutSeconds(), DEFAULT_TIMEOUT_SECONDS);
    }

    @Override
    protected Integer effectiveMaxRetries() {
        return firstNonNull(getVaultConfig().getMaxRetries(), super.effectiveMaxRetries(), DEFAULT_MAX_RETRIES);
    }

    @Override
    protected Long effectiveInitialRetryDelayMillis() {
        return firstNonNull(getVaultConfig().getInitialRetryDelayMillis(), super.effectiveInitialRetryDelayMillis(),
                DEFAULT_INITIAL_RETRY_DELAY_MILLIS);
    }

    @Override
    protected Long effectiveMaxRetryDelayMillis() {
        return firstNonNull(getVaultConfig().getMaxRetryDelayMillis(), super.effectiveMaxRetryDelayMillis(),
                DEFAULT_MAX_RETRY_DELAY_MILLIS);
    }

    // ------------------------------------------------------------------
    // Flowvault-only Detect V2 operations
    // ------------------------------------------------------------------

    /**
     * Detects and de-identifies sensitive data in a file. The API is asynchronous: by default this
     * submits the run and returns its id for use with {@link #getRun}. With
     * {@link DeidentifyFileRequest#getPollOptions() pollOptions} the SDK polls the run with exponential
     * backoff and returns the completed run, or the run id with a non-terminal status if the budget
     * runs out. On success, a request built from a local file has its outputs written to disk.
     *
     * @param request the file (base64, skyflow id, presigned URL, or a local file) plus a stored or inline configuration
     * @return run id, and the run's status, outputs and metrics when polled
     * @throws SkyflowException on validation failure, credential failure, an API error or an output write failure
     */
    public DeidentifyFileResponse deidentifyFile(DeidentifyFileRequest request) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DEIDENTIFY_FILE_TRIGGERED.getLog());
        LogUtil.printInfoLog(InfoLogs.VALIDATE_DEIDENTIFY_FILE_REQUEST.getLog());
        DetectValidations.validateDeidentifyFileRequest(request);
        String token = resolveToken();
        DeidentifyFileRequestV2 apiRequest = DetectRequestMapper.toDeidentifyFileRequest(request, getVaultConfig().getVaultId());
        String runId;
        try {
            DeidentifyFileResponseV2 apiResponse = filesClient().deidentifyFileV2(apiRequest, requestOptions(token));
            LogUtil.printInfoLog(InfoLogs.DEIDENTIFY_FILE_REQUEST_RESOLVED.getLog());
            runId = apiResponse.getRunId().orElse(null);
        } catch (ApiClientApiException e) {
            LogUtil.printErrorLog(ErrorLogs.DEIDENTIFY_FILE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), responseBody(e.body()));
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.DEIDENTIFY_FILE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e);
        }
        DeidentifyFileResponse response = DeidentifyFileFlow.complete(request, runId,
                id -> getRun(GetRunRequest.builder().runId(id).build()));
        LogUtil.printInfoLog(InfoLogs.DEIDENTIFY_FILE_SUCCESS.getLog());
        return response;
    }

    /**
     * Replaces tokens in a file with their original values or a redacted rendering. Synchronous:
     * the re-identified file is returned in the response rather than through a run id.
     *
     * @param request the file (base64, skyflow id or presigned URL) plus optional per-group redaction rules
     * @return status, output files and metrics
     * @throws SkyflowException on validation failure, credential failure or an API error
     */
    public ReidentifyFileResponse reidentifyFile(ReidentifyFileRequest request) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.REIDENTIFY_FILE_TRIGGERED.getLog());
        LogUtil.printInfoLog(InfoLogs.VALIDATE_REIDENTIFY_FILE_REQUEST.getLog());
        ReidentifyFileValidations.validateReidentifyFileRequest(request);
        String token = resolveToken();
        ReidentifyFileRequestV2 apiRequest = ReidentifyFileMapper.toApiRequest(request, getVaultConfig().getVaultId());
        try {
            ReidentifyFileResponseV2 apiResponse = filesClient().reidentifyFileV2(apiRequest, requestOptions(token));
            LogUtil.printInfoLog(InfoLogs.REIDENTIFY_FILE_REQUEST_RESOLVED.getLog());
            ReidentifyFileResponse response = ReidentifyFileMapper.toResponse(apiResponse);
            LogUtil.printInfoLog(InfoLogs.REIDENTIFY_FILE_SUCCESS.getLog());
            return response;
        } catch (ApiClientApiException e) {
            LogUtil.printErrorLog(ErrorLogs.REIDENTIFY_FILE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), responseBody(e.body()));
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.REIDENTIFY_FILE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e);
        }
    }

    private RequestOptions requestOptions(String token) {
        return RequestOptions.builder()
                .addHeader("Authorization", "Bearer " + token)
                .addHeader(BaseConstants.SDK_METRICS_HEADER_KEY, getSdkMetrics())
                .build();
    }

    private static String responseBody(Object body) {
        if (body == null) {
            return null;
        }
        if (body instanceof String) {
            return (String) body;
        }
        try {
            return ObjectMappers.JSON_MAPPER.writeValueAsString(body);
        } catch (Exception ex) {
            return body.toString();
        }
    }

    private static <T> T firstNonNull(T vaultLevel, T clientLevel, T defaultValue) {
        if (vaultLevel != null) {
            return vaultLevel;
        }
        return clientLevel != null ? clientLevel : defaultValue;
    }
}
