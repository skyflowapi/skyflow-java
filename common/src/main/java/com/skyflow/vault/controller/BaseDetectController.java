package com.skyflow.vault.controller;

import com.skyflow.BaseVaultClient;
import com.skyflow.config.BaseCredentials;
import com.skyflow.config.BaseVaultConfig;
import com.skyflow.detect.CheckGuardrailsRequest;
import com.skyflow.detect.CheckGuardrailsResponse;
import com.skyflow.detect.DeidentifyStringRequest;
import com.skyflow.detect.DeidentifyStringResponse;
import com.skyflow.detect.GetRunRequest;
import com.skyflow.detect.GetRunResponse;
import com.skyflow.detect.ReidentifyStringRequest;
import com.skyflow.detect.ReidentifyStringResponse;
import com.skyflow.detect.internal.DetectRequestMapper;
import com.skyflow.detect.internal.DetectResponseMapper;
import com.skyflow.detect.internal.DetectValidations;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.detect.rest.core.ApiClientApiException;
import com.skyflow.generated.detect.rest.core.ApiClientException;
import com.skyflow.generated.detect.rest.core.ClientOptions;
import com.skyflow.generated.detect.rest.core.Environment;
import com.skyflow.generated.detect.rest.core.ObjectMappers;
import com.skyflow.generated.detect.rest.core.RequestOptions;
import com.skyflow.generated.detect.rest.resources.filesv2.FilesV2Client;
import com.skyflow.generated.detect.rest.resources.filesv2.requests.GetRunV2Request;
import com.skyflow.generated.detect.rest.resources.guardrailsv2.GuardrailsV2Client;
import com.skyflow.generated.detect.rest.resources.guardrailsv2.requests.DetectGuardrailsRequestV2;
import com.skyflow.generated.detect.rest.resources.stringsv2.StringsV2Client;
import com.skyflow.generated.detect.rest.resources.stringsv2.requests.DeidentifyStringRequestV2;
import com.skyflow.generated.detect.rest.resources.stringsv2.requests.ReidentifyStringRequestV2;
import com.skyflow.generated.detect.rest.types.DeidentifyStringResponseV2;
import com.skyflow.generated.detect.rest.types.DetectGuardrailsResponseV2;
import com.skyflow.generated.detect.rest.types.DetectRunsResponseV2;
import com.skyflow.generated.detect.rest.types.ReidentifyStringResponseV2;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.BaseConstants;
import com.skyflow.utils.logger.LogUtil;

/**
 * Shared implementation of the Skyflow Detect V2 API. Each SDK's {@code DetectController}
 * extends this class and supplies only what differs per vault type: how the vault URL is
 * resolved, the SDK metrics header, and any vault-level timeout and retry overrides.
 *
 * <p>Credentials, bearer-token refresh and the vault configuration come from
 * {@link BaseVaultClient}. The generated V2 client is created lazily and rebuilt whenever the
 * vault URL or the effective HTTP settings change.
 *
 * @param <V> the SDK's vault configuration type
 */
public abstract class BaseDetectController<V extends BaseVaultConfig> extends BaseVaultClient<V> {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private Integer commonTimeoutSeconds;
    private Integer commonMaxRetries;
    private Long commonInitialRetryDelayMillis;
    private Long commonMaxRetryDelayMillis;

    private ClientOptions clientOptions;
    private String clientOptionsKey;
    private StringsV2Client stringsClient;
    private FilesV2Client filesClient;
    private GuardrailsV2Client guardrailsClient;

    protected BaseDetectController(V vaultConfig, BaseCredentials credentials) {
        super(vaultConfig, credentials);
    }

    // ------------------------------------------------------------------
    // Hooks for the concrete SDK controllers
    // ------------------------------------------------------------------

    /**
     * @return the vault base URL requests are sent to, for example
     * {@code https://<cluster>.vault.skyflowapis.com}
     * @throws SkyflowException if the URL cannot be determined
     */
    protected abstract String resolveVaultUrl() throws SkyflowException;

    /**
     * @return JSON sent in the {@code sky-metadata} header. Override to report SDK name and version.
     */
    protected String getSdkMetrics() {
        return "{}";
    }

    /**
     * Resolves the bearer token or API key for the next call, refreshing it when expired.
     * The default uses the credential chain in {@link BaseVaultClient}.
     */
    protected String resolveToken() throws SkyflowException {
        setBearerToken(this.vaultConfig.getCredentials());
        return this.token;
    }

    /** @return overall call timeout in seconds, or {@code null} for the transport default */
    protected Integer effectiveTimeoutSeconds() {
        return commonTimeoutSeconds;
    }

    /** @return retries on retryable HTTP statuses, or {@code null} for the transport default */
    protected Integer effectiveMaxRetries() {
        return commonMaxRetries;
    }

    protected Long effectiveInitialRetryDelayMillis() {
        return commonInitialRetryDelayMillis;
    }

    protected Long effectiveMaxRetryDelayMillis() {
        return commonMaxRetryDelayMillis;
    }

    // ------------------------------------------------------------------
    // Lifecycle, driven by each SDK's Skyflow builder. Public because the builders live in
    // com.skyflow while this class lives in com.skyflow.vault.controller; applications have
    // no reason to call these directly.
    // ------------------------------------------------------------------

    /** Applies client-level credentials; vault-level credentials keep precedence. */
    public void setCommonCredentials(BaseCredentials commonCredentials) throws SkyflowException {
        this.commonCredentials = commonCredentials;
        prioritiseCredentials(this.vaultConfig.getCredentials());
        onCommonCredentialsUpdated(commonCredentials);
    }

    /**
     * Called after client-level credentials change. Subclasses that hold other clients (for
     * example a V1 delegate) override this to forward the update; the default does nothing.
     */
    protected void onCommonCredentialsUpdated(BaseCredentials commonCredentials) throws SkyflowException {
    }

    /** Applies client-level timeout and retry settings; vault-level settings keep precedence. */
    public synchronized void setCommonHttpConfig(Integer timeoutSeconds, Integer maxRetries,
                                                    Long initialRetryDelayMillis, Long maxRetryDelayMillis) {
        this.commonTimeoutSeconds = timeoutSeconds;
        this.commonMaxRetries = maxRetries;
        this.commonInitialRetryDelayMillis = initialRetryDelayMillis;
        this.commonMaxRetryDelayMillis = maxRetryDelayMillis;
        invalidateClients();
    }

    /** Replaces the vault configuration, for example after {@code updateVaultConfig}. */
    public synchronized void setVaultConfig(V vaultConfig) throws SkyflowException {
        this.vaultConfig = vaultConfig;
        invalidateClients();
    }

    /** Forces the vault URL to be resolved again on the next call. */
    public synchronized void refreshVaultUrl() throws SkyflowException {
        invalidateClients();
    }

    /** Drops cached generated clients so the next call rebuilds them from current settings. */
    protected synchronized void invalidateClients() {
        this.clientOptions = null;
        this.clientOptionsKey = null;
        this.stringsClient = null;
        this.filesClient = null;
        this.guardrailsClient = null;
    }

    // ------------------------------------------------------------------
    // Detect V2 operations
    // ------------------------------------------------------------------

    /**
     * Detects and de-identifies sensitive data in a string. Synchronous.
     *
     * @param request text plus exactly one of a stored configuration id or an inline configuration
     * @return processed text, detected entities and metrics
     * @throws SkyflowException on validation failure, credential failure or an API error
     */
    public DeidentifyStringResponse deidentifyString(DeidentifyStringRequest request) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DEIDENTIFY_STRING_TRIGGERED.getLog());
        LogUtil.printInfoLog(InfoLogs.VALIDATE_DEIDENTIFY_STRING_REQUEST.getLog());
        DetectValidations.validateDeidentifyStringRequest(request);
        String token = resolveToken();
        DeidentifyStringRequestV2 apiRequest =
                DetectRequestMapper.toDeidentifyStringRequest(request, this.vaultConfig.getVaultId());
        try {
            DeidentifyStringResponseV2 apiResponse = stringsClient().deidentifyStringV2(apiRequest, requestOptions(token));
            LogUtil.printInfoLog(InfoLogs.DEIDENTIFY_STRING_REQUEST_RESOLVED.getLog());
            DeidentifyStringResponse response = DetectResponseMapper.toDeidentifyStringResponse(apiResponse);
            LogUtil.printInfoLog(InfoLogs.DEIDENTIFY_STRING_SUCCESS.getLog());
            return response;
        } catch (ApiClientApiException e) {
            throw toSkyflowException(e, ErrorLogs.DEIDENTIFY_STRING_REQUEST_REJECTED);
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.DEIDENTIFY_STRING_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e);
        }
    }

    /**
     * Replaces tokens in a string with their original values or a redacted rendering. Synchronous.
     *
     * @param request text containing tokens plus optional per-group redaction rules
     * @return re-identified text and metrics
     * @throws SkyflowException on validation failure, credential failure or an API error
     */
    public ReidentifyStringResponse reidentifyString(ReidentifyStringRequest request) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.REIDENTIFY_STRING_TRIGGERED.getLog());
        LogUtil.printInfoLog(InfoLogs.VALIDATE_REIDENTIFY_STRING_REQUEST.getLog());
        DetectValidations.validateReidentifyStringRequest(request);
        String token = resolveToken();
        ReidentifyStringRequestV2 apiRequest =
                DetectRequestMapper.toReidentifyStringRequest(request, this.vaultConfig.getVaultId());
        try {
            ReidentifyStringResponseV2 apiResponse = stringsClient().reidentifyStringV2(apiRequest, requestOptions(token));
            LogUtil.printInfoLog(InfoLogs.REIDENTIFY_STRING_REQUEST_RESOLVED.getLog());
            ReidentifyStringResponse response = DetectResponseMapper.toReidentifyStringResponse(apiResponse);
            LogUtil.printInfoLog(InfoLogs.REIDENTIFY_STRING_SUCCESS.getLog());
            return response;
        } catch (ApiClientApiException e) {
            throw toSkyflowException(e, ErrorLogs.REIDENTIFY_STRING_REQUEST_REJECTED);
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.REIDENTIFY_STRING_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e);
        }
    }

    /**
     * Retrieves the status and, once complete, the outputs of a Detect file run.
     *
     * @param request the run id returned by {@code deidentifyFile}
     * @return status, output files, metrics and failure message
     * @throws SkyflowException on validation failure, credential failure or an API error
     */
    public GetRunResponse getRun(GetRunRequest request) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.GET_RUN_TRIGGERED.getLog());
        LogUtil.printInfoLog(InfoLogs.VALIDATE_GET_RUN_REQUEST.getLog());
        DetectValidations.validateGetRunRequest(request);
        String token = resolveToken();
        String runId = request.getRunId().trim();
        GetRunV2Request apiRequest = GetRunV2Request.builder().vaultId(this.vaultConfig.getVaultId()).build();
        try {
            DetectRunsResponseV2 apiResponse = filesClient().getRunV2(runId, apiRequest, requestOptions(token));
            LogUtil.printInfoLog(InfoLogs.GET_RUN_REQUEST_RESOLVED.getLog());
            GetRunResponse response = DetectResponseMapper.toGetRunResponse(runId, apiResponse);
            LogUtil.printInfoLog(InfoLogs.GET_RUN_SUCCESS.getLog());
            return response;
        } catch (ApiClientApiException e) {
            throw toSkyflowException(e, ErrorLogs.GET_RUN_REQUEST_REJECTED);
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.GET_RUN_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e);
        }
    }

    /**
     * Screens text for toxicity and denied topics. Synchronous.
     *
     * @param request text plus the checks to apply
     * @return the overall validation result and the individual check outcomes
     * @throws SkyflowException on validation failure, credential failure or an API error
     */
    public CheckGuardrailsResponse checkGuardrails(CheckGuardrailsRequest request) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.CHECK_GUARDRAILS_TRIGGERED.getLog());
        LogUtil.printInfoLog(InfoLogs.VALIDATE_CHECK_GUARDRAILS_REQUEST.getLog());
        DetectValidations.validateCheckGuardrailsRequest(request);
        String token = resolveToken();
        DetectGuardrailsRequestV2 apiRequest =
                DetectRequestMapper.toCheckGuardrailsRequest(request, this.vaultConfig.getVaultId());
        try {
            DetectGuardrailsResponseV2 apiResponse = guardrailsClient().checkGuardrailsV2(apiRequest, requestOptions(token));
            LogUtil.printInfoLog(InfoLogs.CHECK_GUARDRAILS_REQUEST_RESOLVED.getLog());
            CheckGuardrailsResponse response = DetectResponseMapper.toCheckGuardrailsResponse(apiResponse);
            LogUtil.printInfoLog(InfoLogs.CHECK_GUARDRAILS_SUCCESS.getLog());
            return response;
        } catch (ApiClientApiException e) {
            throw toSkyflowException(e, ErrorLogs.CHECK_GUARDRAILS_REQUEST_REJECTED);
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.CHECK_GUARDRAILS_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e);
        }
    }

    // ------------------------------------------------------------------
    // Transport plumbing
    // ------------------------------------------------------------------

    /**
     * @return client options for the current URL, vault id and HTTP settings, rebuilt when any change
     */
    private synchronized ClientOptions clientOptions() throws SkyflowException {
        String vaultUrl = resolveVaultUrl();
        String key = clientKey(vaultUrl);
        if (clientOptions == null || !key.equals(clientOptionsKey)) {
            clientOptions = buildClientOptions(vaultUrl);
            clientOptionsKey = key;
            stringsClient = null;
            filesClient = null;
            guardrailsClient = null;
        }
        return clientOptions;
    }

    /** @return the generated strings client for the current settings */
    protected synchronized StringsV2Client stringsClient() throws SkyflowException {
        ClientOptions options = clientOptions();
        if (stringsClient == null) {
            stringsClient = new StringsV2Client(options);
        }
        return stringsClient;
    }

    /** @return the generated guardrails client for the current settings */
    protected synchronized GuardrailsV2Client guardrailsClient() throws SkyflowException {
        ClientOptions options = clientOptions();
        if (guardrailsClient == null) {
            guardrailsClient = new GuardrailsV2Client(options);
        }
        return guardrailsClient;
    }

    /** @return the generated files client for the current settings */
    protected synchronized FilesV2Client filesClient() throws SkyflowException {
        ClientOptions options = clientOptions();
        if (filesClient == null) {
            filesClient = new FilesV2Client(options);
        }
        return filesClient;
    }

    private String clientKey(String vaultUrl) {
        return vaultUrl + "|" + this.vaultConfig.getVaultId() + "|" + effectiveTimeoutSeconds() + "|"
                + effectiveMaxRetries() + "|" + effectiveInitialRetryDelayMillis() + "|" + effectiveMaxRetryDelayMillis();
    }

    private ClientOptions buildClientOptions(String vaultUrl) {
        ClientOptions.Builder builder = ClientOptions.builder().environment(Environment.custom(vaultUrl));
        Integer timeout = effectiveTimeoutSeconds();
        if (timeout != null) {
            builder.timeout(timeout);
        }
        Integer maxRetries = effectiveMaxRetries();
        if (maxRetries != null) {
            builder.maxRetries(maxRetries);
        }
        Long initialDelay = effectiveInitialRetryDelayMillis();
        if (initialDelay != null) {
            builder.initialRetryDelayMillis(initialDelay);
        }
        Long maxDelay = effectiveMaxRetryDelayMillis();
        if (maxDelay != null) {
            builder.maxRetryDelayMillis(maxDelay);
        }
        return builder.build();
    }

    private RequestOptions requestOptions(String token) {
        return RequestOptions.builder()
                .addHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .addHeader(BaseConstants.SDK_METRICS_HEADER_KEY, getSdkMetrics())
                .build();
    }

    private static SkyflowException toSkyflowException(ApiClientApiException e, ErrorLogs errorLog) {
        LogUtil.printErrorLog(errorLog.getLog());
        return new SkyflowException(e.statusCode(), e, e.headers(), bodyAsString(e.body()));
    }

    static String bodyAsString(Object body) {
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
}
