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
import com.skyflow.generated.rest.types.V1InsertRecordData;
import com.skyflow.generated.rest.types.V1InsertResponse;
import com.skyflow.generated.rest.types.V1Upsert;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.logs.WarningLogs;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.*;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.skyflow.utils.Utils.*;

public final class VaultController extends VaultClient
        implements IVaultController<InsertRequest, InsertResponse, DetokenizeRequest, DetokenizeResponse> {
    private static final Gson gson = new GsonBuilder().serializeNulls().create();
    private JsonObject metrics = Utils.getMetrics();

    public VaultController(VaultConfig vaultConfig, Credentials credentials) throws SkyflowException {
        super(vaultConfig, credentials);
    }

    /**
     * Immutable per-call batch size / concurrency limit. Computed fresh on every bulk call
     * instead of being stored on instance fields, since a VaultController instance is cached
     * and reused (see Skyflow#vaultClientsMap) and may be invoked concurrently from multiple
     * threads.
     */
    private static final class BatchConfig {
        final int batchSize;
        final int concurrencyLimit;

        BatchConfig(int batchSize, int concurrencyLimit) {
            this.batchSize = batchSize;
            this.concurrencyLimit = concurrencyLimit;
        }
    }

    private RequestOptions buildRequestOptions(RequestContext context) {
        RequestOptions.Builder builder = RequestOptions.builder()
                .addHeader(Constants.SDK_METRICS_HEADER_KEY, metrics.toString());
        context.getHeaders().forEach((k, v) -> builder.addHeader(k.toString(), v));
        return builder.build();
    }

    @Override
    public InsertResponse insert(InsertRequest request) throws SkyflowException {
        return insert(request, null);
    }

    public InsertResponse insert(InsertRequest request, InsertOptions options) throws SkyflowException {
        try {
            LogUtil.printInfoLog(InfoLogs.INSERT_TRIGGERED.getLog());
            LogUtil.printInfoLog(InfoLogs.VALIDATE_INSERT_REQUEST.getLog());
            Validations.validateInsertRequest(request);
            setBearerToken();
            V1InsertRequest insertRequest = getBulkInsertRequestBody(request, super.getVaultConfig());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            RequestContext ctx = new RequestContext("INSERT");
            if (interceptor != null) interceptor.intercept(ctx);
            V1InsertResponse response = this.getRecordsApi().withRawResponse().insert(insertRequest, buildRequestOptions(ctx)).body();
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
        return detokenize(request, null);
    }

    public DetokenizeResponse detokenize(DetokenizeRequest request, DetokenizeOptions options) throws SkyflowException {
        try {
            LogUtil.printInfoLog(InfoLogs.DETOKENIZE_TRIGGERED.getLog());
            LogUtil.printInfoLog(InfoLogs.VALIDATE_DETOKENIZE_REQUEST.getLog());
            Validations.validateDetokenizeRequest(request);
            setBearerToken();

            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest requestDetokenize = getDetokenizeRequestBody(request, this.getVaultConfig().getVaultId());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            RequestContext ctx = new RequestContext("DETOKENIZE");
            if (interceptor != null) interceptor.intercept(ctx);
            com.skyflow.generated.rest.types.V1FlowDetokenizeResponse response = this.getRecordsApi().withRawResponse().detokenize(requestDetokenize, buildRequestOptions(ctx)).body();

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
        return tokenize(request, null);
    }

    public TokenizeResponse tokenize(TokenizeRequest request, TokenizeOptions options) throws SkyflowException {
        try {
            LogUtil.printInfoLog(InfoLogs.TOKENIZE_TRIGGERED.getLog());
            LogUtil.printInfoLog(InfoLogs.VALIDATING_TOKENIZE_REQUEST.getLog());
            Validations.validateTokenizeRequest(request);
            setBearerToken();

            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest requestTokenize = getTokenizeRequestBody(request, this.getVaultConfig().getVaultId());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            RequestContext ctx = new RequestContext("TOKENIZE");
            if (interceptor != null) interceptor.intercept(ctx);
            ApiClientHttpResponse<V1FlowTokenizeResponse> response = this.getRecordsApi().withRawResponse().tokenize(requestTokenize, buildRequestOptions(ctx));
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
        return deleteTokens(request, null);
    }

    public DeleteTokensResponse deleteTokens(DeleteTokensRequest request, DeleteTokensOptions options) throws SkyflowException {
        try {
            LogUtil.printInfoLog(InfoLogs.DELETE_TOKENS_TRIGGERED.getLog());
            LogUtil.printInfoLog(InfoLogs.VALIDATE_DELETE_TOKENS_REQUEST.getLog());
            Validations.validateDeleteTokensRequest(request);
            setBearerToken();

            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest requestDeleteTokens = getDeleteTokensRequestBody(request, this.getVaultConfig().getVaultId());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            RequestContext ctx = new RequestContext("DELETE_TOKENS");
            if (interceptor != null) interceptor.intercept(ctx);
            ApiClientHttpResponse<V1FlowDeleteTokenResponse> response = this.getRecordsApi().withRawResponse().deletetoken(requestDeleteTokens, buildRequestOptions(ctx));
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

    // ── Bulk Insert ───────────────────────────────────────────────────────────

    public BulkInsertResponse bulkInsert(BulkInsertRequest insertRequest) throws SkyflowException {
        return bulkInsert(insertRequest, null);
    }

    public BulkInsertResponse bulkInsert(BulkInsertRequest insertRequest, InsertOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.INSERT_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_INSERT_REQUEST.getLog());
            Validations.validateBulkInsertRequest(insertRequest);
            BatchConfig cfg = configureInsertConcurrencyAndBatchSize(insertRequest.getRecords().size());

            setBearerToken();
            V1InsertRequest request = Utils.getBulkInsertRequestBody(insertRequest, this.getVaultConfig());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            return this.processBulkInsertSync(request, insertRequest.getRecords(), interceptor, cfg);
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        } catch (ExecutionException e) {
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            Throwable cause = e.getCause();
            throw new SkyflowException(cause != null && cause.getMessage() != null ? cause.getMessage() : e.getMessage());
        }
    }

    public CompletableFuture<BulkInsertResponse> bulkInsertAsync(BulkInsertRequest insertRequest) throws SkyflowException {
        return bulkInsertAsync(insertRequest, null);
    }

    public CompletableFuture<BulkInsertResponse> bulkInsertAsync(BulkInsertRequest insertRequest, InsertOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.INSERT_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_INSERT_REQUEST.getLog());
            Validations.validateBulkInsertRequest(insertRequest);
            BatchConfig cfg = configureInsertConcurrencyAndBatchSize(insertRequest.getRecords().size());

            setBearerToken();
            V1InsertRequest request = Utils.getBulkInsertRequestBody(insertRequest, this.getVaultConfig());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            List<ErrorRecord> errorRecords = Collections.synchronizedList(new ArrayList<>());
            List<CompletableFuture<BulkInsertResponse>> futures = this.insertBatchFutures(request, errorRecords, interceptor, cfg);

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        List<Success> successRecords = new ArrayList<>();

                        for (CompletableFuture<BulkInsertResponse> future : futures) {
                            BulkInsertResponse futureResponse = future.join();
                            if (futureResponse != null) {
                                if (futureResponse.getSuccess() != null) {
                                    successRecords.addAll(futureResponse.getSuccess());
                                }
                                if (futureResponse.getErrors() != null) {
                                    errorRecords.addAll(futureResponse.getErrors());
                                }
                            }
                        }

                        return new BulkInsertResponse(successRecords, errorRecords, insertRequest.getRecords());
                    });
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        }
    }

    // ── Bulk Detokenize ───────────────────────────────────────────────────────

    public BulkDetokenizeResponse bulkDetokenize(BulkDetokenizeRequest detokenizeRequest) throws SkyflowException {
        return bulkDetokenize(detokenizeRequest, null);
    }

    public BulkDetokenizeResponse bulkDetokenize(BulkDetokenizeRequest detokenizeRequest, DetokenizeOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DETOKENIZE_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_DETOKENIZE_REQUEST.getLog());
            Validations.validateBulkDetokenizeRequest(detokenizeRequest);
            BatchConfig cfg = configureDetokenizeConcurrencyAndBatchSize(detokenizeRequest.getTokens().size());
            setBearerToken();
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest request =
                    Utils.getBulkDetokenizeRequestBody(detokenizeRequest, this.getVaultConfig().getVaultId());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            return this.processBulkDetokenizeSync(request, detokenizeRequest.getTokens(), interceptor, cfg);
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.DETOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SkyflowException(e.getMessage());
        } catch (ExecutionException e) {
            throw new SkyflowException(e.getMessage());
        }
    }

    public CompletableFuture<BulkDetokenizeResponse> bulkDetokenizeAsync(BulkDetokenizeRequest detokenizeRequest) throws SkyflowException {
        return bulkDetokenizeAsync(detokenizeRequest, null);
    }

    public CompletableFuture<BulkDetokenizeResponse> bulkDetokenizeAsync(BulkDetokenizeRequest detokenizeRequest, DetokenizeOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DETOKENIZE_TRIGGERED.getLog());
        ExecutorService executor = null;
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_DETOKENIZE_REQUEST.getLog());
            Validations.validateBulkDetokenizeRequest(detokenizeRequest);
            BatchConfig cfg = configureDetokenizeConcurrencyAndBatchSize(detokenizeRequest.getTokens().size());
            setBearerToken();
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest request =
                    Utils.getBulkDetokenizeRequestBody(detokenizeRequest, this.getVaultConfig().getVaultId());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;

            LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());

            List<ErrorRecord> errorTokens = Collections.synchronizedList(new ArrayList<>());
            List<DetokenizeResponseObject> successRecords = new ArrayList<>();

            List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest> batches =
                    Utils.createBulkDetokenizeBatches(request, cfg.batchSize);

            executor = Executors.newFixedThreadPool(cfg.concurrencyLimit);
            List<CompletableFuture<BulkDetokenizeResponse>> futures = this.detokenizeBatchFutures(executor, batches, errorTokens, interceptor, cfg.batchSize);
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        for (CompletableFuture<BulkDetokenizeResponse> future : futures) {
                            BulkDetokenizeResponse futureResponse = future.join();
                            if (futureResponse != null) {
                                if (futureResponse.getSuccess() != null) {
                                    successRecords.addAll(futureResponse.getSuccess());
                                }
                                if (futureResponse.getErrors() != null) {
                                    errorTokens.addAll(futureResponse.getErrors());
                                }
                            }
                        }
                        LogUtil.printInfoLog(InfoLogs.DETOKENIZE_REQUEST_RESOLVED.getLog());
                        return new BulkDetokenizeResponse(successRecords, errorTokens, detokenizeRequest.getTokens());
                    });
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.DETOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (SkyflowException e) {
            LogUtil.printErrorLog(ErrorLogs.DETOKENIZE_REQUEST_REJECTED.getLog());
            throw e;
        } catch (Exception e) {
            LogUtil.printErrorLog(ErrorLogs.DETOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        } finally {
            if (executor != null) executor.shutdown();
        }
    }

    // ── Bulk Delete Tokens ────────────────────────────────────────────────────

    public BulkDeleteTokensResponse bulkDeleteTokens(BulkDeleteTokensRequest deleteTokensRequest) throws SkyflowException {
        return bulkDeleteTokens(deleteTokensRequest, null);
    }

    public BulkDeleteTokensResponse bulkDeleteTokens(BulkDeleteTokensRequest deleteTokensRequest, DeleteTokensOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DELETE_TOKENS_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_DELETE_TOKENS_REQUEST.getLog());
            Validations.validateBulkDeleteTokensRequest(deleteTokensRequest);
            BatchConfig cfg = configureDeleteTokensConcurrencyAndBatchSize(deleteTokensRequest.getTokens().size());
            setBearerToken();
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest request =
                    Utils.getBulkDeleteTokensRequestBody(deleteTokensRequest, this.getVaultConfig().getVaultId());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            return this.processBulkDeleteTokensSync(request, deleteTokensRequest.getTokens(), interceptor, cfg);
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.DELETE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ExecutionException | InterruptedException e) {
            LogUtil.printErrorLog(ErrorLogs.DELETE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        }
    }

    public CompletableFuture<BulkDeleteTokensResponse> bulkDeleteTokensAsync(BulkDeleteTokensRequest deleteTokensRequest) throws SkyflowException {
        return bulkDeleteTokensAsync(deleteTokensRequest, null);
    }

    public CompletableFuture<BulkDeleteTokensResponse> bulkDeleteTokensAsync(BulkDeleteTokensRequest deleteTokensRequest, DeleteTokensOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DELETE_TOKENS_TRIGGERED.getLog());
        ExecutorService executor = null;
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_DELETE_TOKENS_REQUEST.getLog());
            Validations.validateBulkDeleteTokensRequest(deleteTokensRequest);
            BatchConfig cfg = configureDeleteTokensConcurrencyAndBatchSize(deleteTokensRequest.getTokens().size());
            setBearerToken();
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest request =
                    Utils.getBulkDeleteTokensRequestBody(deleteTokensRequest, this.getVaultConfig().getVaultId());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;

            LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());

            List<ErrorRecord> errorTokens = Collections.synchronizedList(new ArrayList<>());
            List<DeleteTokensSuccess> successRecords = Collections.synchronizedList(new ArrayList<>());

            List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest> batches =
                    Utils.createBulkDeleteTokensBatches(request, cfg.batchSize);

            executor = Executors.newFixedThreadPool(cfg.concurrencyLimit);
            List<CompletableFuture<BulkDeleteTokensResponse>> futures =
                    this.deleteTokensBatchFutures(executor, batches, interceptor, cfg.batchSize);

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        for (CompletableFuture<BulkDeleteTokensResponse> future : futures) {
                            BulkDeleteTokensResponse futureResponse = future.join();
                            if (futureResponse != null) {
                                if (futureResponse.getSuccess() != null) {
                                    successRecords.addAll(futureResponse.getSuccess());
                                }
                                if (futureResponse.getErrors() != null) {
                                    errorTokens.addAll(futureResponse.getErrors());
                                }
                            }
                        }
                        LogUtil.printInfoLog(InfoLogs.DELETE_TOKENS_REQUEST_RESOLVED.getLog());
                        return new BulkDeleteTokensResponse(successRecords, errorTokens, deleteTokensRequest.getTokens());
                    });
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.DELETE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (SkyflowException e) {
            LogUtil.printErrorLog(ErrorLogs.DELETE_REQUEST_REJECTED.getLog());
            throw e;
        } catch (Exception e) {
            LogUtil.printErrorLog(ErrorLogs.DELETE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        } finally {
            if (executor != null) executor.shutdown();
        }
    }

    // ── Bulk Tokenize ─────────────────────────────────────────────────────────

    public BulkTokenizeResponse bulkTokenize(BulkTokenizeRequest tokenizeRequest) throws SkyflowException {
        return bulkTokenize(tokenizeRequest, null);
    }

    public BulkTokenizeResponse bulkTokenize(BulkTokenizeRequest tokenizeRequest, TokenizeOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.TOKENIZE_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_TOKENIZE_REQUEST.getLog());
            Validations.validateBulkTokenizeRequest(tokenizeRequest);
            BatchConfig cfg = configureTokenizeConcurrencyAndBatchSize(tokenizeRequest.getData().size());
            setBearerToken();
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest request =
                    Utils.getBulkTokenizeRequestBody(tokenizeRequest, this.getVaultConfig().getVaultId());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            return this.processBulkTokenizeSync(request, tokenizeRequest.getData(), interceptor, cfg);
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.TOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (SkyflowException e) {
            LogUtil.printErrorLog(ErrorLogs.TOKENIZE_REQUEST_REJECTED.getLog());
            throw e;
        } catch (ExecutionException | InterruptedException e) {
            LogUtil.printErrorLog(ErrorLogs.TOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        }
    }

    public CompletableFuture<BulkTokenizeResponse> bulkTokenizeAsync(BulkTokenizeRequest tokenizeRequest) throws SkyflowException {
        return bulkTokenizeAsync(tokenizeRequest, null);
    }

    public CompletableFuture<BulkTokenizeResponse> bulkTokenizeAsync(BulkTokenizeRequest tokenizeRequest, TokenizeOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.TOKENIZE_TRIGGERED.getLog());
        ExecutorService executor = null;
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_TOKENIZE_REQUEST.getLog());
            Validations.validateBulkTokenizeRequest(tokenizeRequest);
            BatchConfig cfg = configureTokenizeConcurrencyAndBatchSize(tokenizeRequest.getData().size());
            setBearerToken();
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest request =
                    Utils.getBulkTokenizeRequestBody(tokenizeRequest, this.getVaultConfig().getVaultId());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;

            LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());

            List<ErrorRecord> errorRecords = Collections.synchronizedList(new ArrayList<>());
            List<TokenizeSuccess> successRecords = Collections.synchronizedList(new ArrayList<>());

            List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest> batches =
                    Utils.createBulkTokenizeBatches(request, cfg.batchSize);

            executor = Executors.newFixedThreadPool(cfg.concurrencyLimit);
            List<CompletableFuture<BulkTokenizeResponse>> futures =
                    this.tokenizeBatchFutures(executor, batches, interceptor, cfg.batchSize);

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        for (CompletableFuture<BulkTokenizeResponse> future : futures) {
                            BulkTokenizeResponse futureResponse = future.join();
                            if (futureResponse != null) {
                                if (futureResponse.getSuccess() != null) {
                                    successRecords.addAll(futureResponse.getSuccess());
                                }
                                if (futureResponse.getErrors() != null) {
                                    errorRecords.addAll(futureResponse.getErrors());
                                }
                            }
                        }
                        LogUtil.printInfoLog(InfoLogs.TOKENIZE_REQUEST_RESOLVED.getLog());
                        return new BulkTokenizeResponse(successRecords, errorRecords, tokenizeRequest.getData());
                    });
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.TOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (SkyflowException e) {
            LogUtil.printErrorLog(ErrorLogs.TOKENIZE_REQUEST_REJECTED.getLog());
            throw e;
        } catch (Exception e) {
            LogUtil.printErrorLog(ErrorLogs.TOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        } finally {
            if (executor != null) executor.shutdown();
        }
    }

    // ── Bulk private helpers ──────────────────────────────────────────────────

    private BulkDeleteTokensResponse processBulkDeleteTokensSync(
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest deleteTokensRequest,
            List<String> originalTokens,
            RequestInterceptor interceptor,
            BatchConfig cfg
    ) throws ExecutionException, InterruptedException, SkyflowException {
        LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());
        List<ErrorRecord> errorRecords = Collections.synchronizedList(new ArrayList<>());
        List<DeleteTokensSuccess> successRecords = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(cfg.concurrencyLimit);
        List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest> batches =
                Utils.createBulkDeleteTokensBatches(deleteTokensRequest, cfg.batchSize);
        try {
            List<CompletableFuture<BulkDeleteTokensResponse>> futures =
                    this.deleteTokensBatchFutures(executor, batches, interceptor, cfg.batchSize);
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            } catch (Exception e) {
                LogUtil.printErrorLog(ErrorLogs.DELETE_REQUEST_REJECTED.getLog());
            }
            for (CompletableFuture<BulkDeleteTokensResponse> future : futures) {
                BulkDeleteTokensResponse futureResponse = future.get();
                if (futureResponse != null) {
                    if (futureResponse.getSuccess() != null) successRecords.addAll(futureResponse.getSuccess());
                    if (futureResponse.getErrors() != null) errorRecords.addAll(futureResponse.getErrors());
                }
            }
        } catch (Exception e) {
            LogUtil.printErrorLog(ErrorLogs.DELETE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        } finally {
            executor.shutdown();
        }
        BulkDeleteTokensResponse response = new BulkDeleteTokensResponse(successRecords, errorRecords, originalTokens);
        LogUtil.printInfoLog(InfoLogs.DELETE_TOKENS_REQUEST_RESOLVED.getLog());
        return response;
    }

    private List<CompletableFuture<BulkDeleteTokensResponse>> deleteTokensBatchFutures(
            ExecutorService executor,
            List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest> batches,
            RequestInterceptor interceptor,
            int batchSize) {
        List<CompletableFuture<BulkDeleteTokensResponse>> futures = new ArrayList<>();
        if (batches == null) return futures;
        for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
            final int index = batchIndex;
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest batch = batches.get(index);
            RequestContext ctx = new RequestContext("DELETE_TOKENS");
            if (interceptor != null) interceptor.intercept(ctx);
            CompletableFuture<BulkDeleteTokensResponse> future = CompletableFuture
                    .supplyAsync(() -> processDeleteTokensBatch(batch, ctx), executor)
                    .handle((result, ex) -> {
                        if (ex != null) {
                            List<ErrorRecord> batchErrors =
                                    Utils.handleBulkDeleteTokensBatchException(ex, batch, index, batchSize);
                            return new BulkDeleteTokensResponse(new ArrayList<>(), batchErrors);
                        }
                        return Utils.formatBulkDeleteTokensResponse(result.body(), index, batchSize, result.headers());
                    });
            futures.add(future);
        }
        return futures;
    }

    private ApiClientHttpResponse<V1FlowDeleteTokenResponse> processDeleteTokensBatch(
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest batch,
            RequestContext ctx) {
        return this.getRecordsApi().withRawResponse().deletetoken(batch, buildRequestOptions(ctx));
    }

    private BatchConfig configureDeleteTokensConcurrencyAndBatchSize(int totalRequests) {
        int batchSize = Constants.DELETE_TOKENS_BATCH_SIZE;
        int concurrencyLimit;
        try {
            String userProvidedBatchSize = System.getenv("DELETE_TOKENS_BATCH_SIZE");
            String userProvidedConcurrencyLimit = System.getenv("DELETE_TOKENS_CONCURRENCY_LIMIT");

            Dotenv dotenv = null;
            try {
                dotenv = Dotenv.load();
            } catch (DotenvException ignored) {}

            if (userProvidedBatchSize == null && dotenv != null) {
                userProvidedBatchSize = dotenv.get("DELETE_TOKENS_BATCH_SIZE");
            }
            if (userProvidedConcurrencyLimit == null && dotenv != null) {
                userProvidedConcurrencyLimit = dotenv.get("DELETE_TOKENS_CONCURRENCY_LIMIT");
            }

            if (userProvidedBatchSize != null) {
                try {
                    int parsedBatchSize = Integer.parseInt(userProvidedBatchSize);
                    if (parsedBatchSize > Constants.MAX_DELETE_TOKENS_BATCH_SIZE) {
                        LogUtil.printWarningLog(WarningLogs.BATCH_SIZE_EXCEEDS_MAX_LIMIT.getLog());
                    }
                    int maxBatchSize = Math.min(parsedBatchSize, Constants.MAX_DELETE_TOKENS_BATCH_SIZE);
                    if (maxBatchSize > 0) {
                        batchSize = maxBatchSize;
                    } else {
                        LogUtil.printWarningLog(WarningLogs.INVALID_BATCH_SIZE_PROVIDED.getLog());
                        batchSize = Constants.DELETE_TOKENS_BATCH_SIZE;
                    }
                } catch (NumberFormatException e) {
                    LogUtil.printWarningLog(WarningLogs.INVALID_BATCH_SIZE_PROVIDED.getLog());
                    batchSize = Constants.DELETE_TOKENS_BATCH_SIZE;
                }
            }

            int maxConcurrencyNeeded = (totalRequests + batchSize - 1) / batchSize;

            if (userProvidedConcurrencyLimit != null) {
                try {
                    int parsedConcurrencyLimit = Integer.parseInt(userProvidedConcurrencyLimit);
                    if (parsedConcurrencyLimit > Constants.MAX_DELETE_TOKENS_CONCURRENCY_LIMIT) {
                        LogUtil.printWarningLog(WarningLogs.CONCURRENCY_EXCEEDS_MAX_LIMIT.getLog());
                    }
                    int maxConcurrencyLimit = Math.min(parsedConcurrencyLimit, Constants.MAX_DELETE_TOKENS_CONCURRENCY_LIMIT);
                    if (maxConcurrencyLimit > 0) {
                        concurrencyLimit = Math.min(maxConcurrencyLimit, maxConcurrencyNeeded);
                    } else {
                        LogUtil.printWarningLog(WarningLogs.INVALID_CONCURRENCY_LIMIT_PROVIDED.getLog());
                        concurrencyLimit = Math.min(Constants.DELETE_TOKENS_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
                    }
                } catch (NumberFormatException e) {
                    LogUtil.printWarningLog(WarningLogs.INVALID_CONCURRENCY_LIMIT_PROVIDED.getLog());
                    concurrencyLimit = Math.min(Constants.DELETE_TOKENS_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
                }
            } else {
                concurrencyLimit = Math.min(Constants.DELETE_TOKENS_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
            }
        } catch (Exception e) {
            batchSize = Constants.DELETE_TOKENS_BATCH_SIZE;
            int maxConcurrencyNeeded = (totalRequests + batchSize - 1) / batchSize;
            concurrencyLimit = Math.min(Constants.DELETE_TOKENS_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
        }
        return new BatchConfig(batchSize, concurrencyLimit);
    }

    private BulkTokenizeResponse processBulkTokenizeSync(
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest tokenizeRequest,
            ArrayList<BulkTokenizeRecord> originalData,
            RequestInterceptor interceptor,
            BatchConfig cfg
    ) throws ExecutionException, InterruptedException, SkyflowException {
        LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());
        List<ErrorRecord> errorRecords = Collections.synchronizedList(new ArrayList<>());
        List<TokenizeSuccess> successRecords = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(cfg.concurrencyLimit);
        List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest> batches =
                Utils.createBulkTokenizeBatches(tokenizeRequest, cfg.batchSize);
        try {
            List<CompletableFuture<BulkTokenizeResponse>> futures =
                    this.tokenizeBatchFutures(executor, batches, interceptor, cfg.batchSize);
            try {
                CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                allFutures.join();
            } catch (Exception e) {
                // individual batch errors are already captured
            }
            for (CompletableFuture<BulkTokenizeResponse> future : futures) {
                BulkTokenizeResponse futureResponse = future.get();
                if (futureResponse != null) {
                    if (futureResponse.getSuccess() != null) {
                        successRecords.addAll(futureResponse.getSuccess());
                    }
                    if (futureResponse.getErrors() != null) {
                        errorRecords.addAll(futureResponse.getErrors());
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.printErrorLog(ErrorLogs.TOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        } finally {
            executor.shutdown();
        }
        BulkTokenizeResponse response = new BulkTokenizeResponse(successRecords, errorRecords, originalData);
        LogUtil.printInfoLog(InfoLogs.TOKENIZE_REQUEST_RESOLVED.getLog());
        return response;
    }

    private List<CompletableFuture<BulkTokenizeResponse>> tokenizeBatchFutures(
            ExecutorService executor,
            List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest> batches,
            RequestInterceptor interceptor,
            int batchSize) {
        List<CompletableFuture<BulkTokenizeResponse>> futures = new ArrayList<>();
        if (batches == null) return futures;
        for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
            final int index = batchIndex;
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batch = batches.get(index);
            RequestContext ctx = new RequestContext("TOKENIZE");
            if (interceptor != null) interceptor.intercept(ctx);
            CompletableFuture<BulkTokenizeResponse> future = CompletableFuture
                    .supplyAsync(() -> processTokenizeBatch(batch, ctx), executor)
                    .handle((result, ex) -> {
                        if (ex != null) {
                            List<ErrorRecord> batchErrors =
                                    Utils.handleBulkTokenizeBatchException(ex, batch, index, batchSize);
                            return new BulkTokenizeResponse(new ArrayList<>(), batchErrors);
                        }
                        return Utils.formatBulkTokenizeResponse(result.body(), batch, index, batchSize, result.headers());
                    });
            futures.add(future);
        }
        return futures;
    }

    private ApiClientHttpResponse<V1FlowTokenizeResponse> processTokenizeBatch(
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batch,
            RequestContext ctx) {
        return this.getRecordsApi().withRawResponse().tokenize(batch, buildRequestOptions(ctx));
    }

    private BatchConfig configureTokenizeConcurrencyAndBatchSize(int totalRequests) {
        int batchSize = Constants.TOKENIZE_BATCH_SIZE;
        int concurrencyLimit;
        try {
            String userProvidedBatchSize = System.getenv("TOKENIZE_BATCH_SIZE");
            String userProvidedConcurrencyLimit = System.getenv("TOKENIZE_CONCURRENCY_LIMIT");

            Dotenv dotenv = null;
            try {
                dotenv = Dotenv.load();
            } catch (DotenvException ignored) {}

            if (userProvidedBatchSize == null && dotenv != null) {
                userProvidedBatchSize = dotenv.get("TOKENIZE_BATCH_SIZE");
            }
            if (userProvidedConcurrencyLimit == null && dotenv != null) {
                userProvidedConcurrencyLimit = dotenv.get("TOKENIZE_CONCURRENCY_LIMIT");
            }

            if (userProvidedBatchSize != null) {
                try {
                    int parsedBatchSize = Integer.parseInt(userProvidedBatchSize);
                    if (parsedBatchSize > Constants.MAX_TOKENIZE_BATCH_SIZE) {
                        LogUtil.printWarningLog(WarningLogs.BATCH_SIZE_EXCEEDS_MAX_LIMIT.getLog());
                    }
                    int maxBatchSize = Math.min(parsedBatchSize, Constants.MAX_TOKENIZE_BATCH_SIZE);
                    if (maxBatchSize > 0) {
                        batchSize = maxBatchSize;
                    } else {
                        LogUtil.printWarningLog(WarningLogs.INVALID_BATCH_SIZE_PROVIDED.getLog());
                        batchSize = Constants.TOKENIZE_BATCH_SIZE;
                    }
                } catch (NumberFormatException e) {
                    LogUtil.printWarningLog(WarningLogs.INVALID_BATCH_SIZE_PROVIDED.getLog());
                    batchSize = Constants.TOKENIZE_BATCH_SIZE;
                }
            }

            int maxConcurrencyNeeded = (totalRequests + batchSize - 1) / batchSize;

            if (userProvidedConcurrencyLimit != null) {
                try {
                    int parsedConcurrencyLimit = Integer.parseInt(userProvidedConcurrencyLimit);
                    if (parsedConcurrencyLimit > Constants.MAX_TOKENIZE_CONCURRENCY_LIMIT) {
                        LogUtil.printWarningLog(WarningLogs.CONCURRENCY_EXCEEDS_MAX_LIMIT.getLog());
                    }
                    int maxConcurrencyLimit = Math.min(parsedConcurrencyLimit, Constants.MAX_TOKENIZE_CONCURRENCY_LIMIT);
                    if (maxConcurrencyLimit > 0) {
                        concurrencyLimit = Math.min(maxConcurrencyLimit, maxConcurrencyNeeded);
                    } else {
                        LogUtil.printWarningLog(WarningLogs.INVALID_CONCURRENCY_LIMIT_PROVIDED.getLog());
                        concurrencyLimit = Math.min(Constants.TOKENIZE_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
                    }
                } catch (NumberFormatException e) {
                    LogUtil.printWarningLog(WarningLogs.INVALID_CONCURRENCY_LIMIT_PROVIDED.getLog());
                    concurrencyLimit = Math.min(Constants.TOKENIZE_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
                }
            } else {
                concurrencyLimit = Math.min(Constants.TOKENIZE_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
            }
        } catch (Exception e) {
            batchSize = Constants.TOKENIZE_BATCH_SIZE;
            int maxConcurrencyNeeded = (totalRequests + batchSize - 1) / batchSize;
            concurrencyLimit = Math.min(Constants.TOKENIZE_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
        }
        return new BatchConfig(batchSize, concurrencyLimit);
    }

    private BatchConfig configureDetokenizeConcurrencyAndBatchSize(int totalRequests) {
        int batchSize = Constants.DETOKENIZE_BATCH_SIZE;
        int concurrencyLimit;
        try {
            String userProvidedBatchSize = System.getenv("DETOKENIZE_BATCH_SIZE");
            String userProvidedConcurrencyLimit = System.getenv("DETOKENIZE_CONCURRENCY_LIMIT");

            Dotenv dotenv = null;
            try {
                dotenv = Dotenv.load();
            } catch (DotenvException ignored) {
                // ignore the case if .env file is not found
            }

            if (userProvidedBatchSize == null && dotenv != null) {
                userProvidedBatchSize = dotenv.get("DETOKENIZE_BATCH_SIZE");
            }
            if (userProvidedConcurrencyLimit == null && dotenv != null) {
                userProvidedConcurrencyLimit = dotenv.get("DETOKENIZE_CONCURRENCY_LIMIT");
            }

            if (userProvidedBatchSize != null) {
                try {
                    int parsedBatchSize = Integer.parseInt(userProvidedBatchSize);
                    if (parsedBatchSize > Constants.MAX_DETOKENIZE_BATCH_SIZE) {
                        LogUtil.printWarningLog(WarningLogs.BATCH_SIZE_EXCEEDS_MAX_LIMIT.getLog());
                    }
                    int maxBatchSize = Math.min(parsedBatchSize, Constants.MAX_DETOKENIZE_BATCH_SIZE);
                    if (maxBatchSize > 0) {
                        batchSize = maxBatchSize;
                    } else {
                        LogUtil.printWarningLog(WarningLogs.INVALID_BATCH_SIZE_PROVIDED.getLog());
                        batchSize = Constants.DETOKENIZE_BATCH_SIZE;
                    }
                } catch (NumberFormatException e) {
                    LogUtil.printWarningLog(WarningLogs.INVALID_BATCH_SIZE_PROVIDED.getLog());
                    batchSize = Constants.DETOKENIZE_BATCH_SIZE;
                }
            }

            int maxConcurrencyNeeded = (totalRequests + batchSize - 1) / batchSize;

            if (userProvidedConcurrencyLimit != null) {
                try {
                    int parsedConcurrencyLimit = Integer.parseInt(userProvidedConcurrencyLimit);
                    if (parsedConcurrencyLimit > Constants.MAX_DETOKENIZE_CONCURRENCY_LIMIT) {
                        LogUtil.printWarningLog(WarningLogs.CONCURRENCY_EXCEEDS_MAX_LIMIT.getLog());
                    }
                    int maxConcurrencyLimit = Math.min(parsedConcurrencyLimit, Constants.MAX_DETOKENIZE_CONCURRENCY_LIMIT);

                    if (maxConcurrencyLimit > 0) {
                        concurrencyLimit = Math.min(maxConcurrencyLimit, maxConcurrencyNeeded);
                    } else {
                        LogUtil.printWarningLog(WarningLogs.INVALID_CONCURRENCY_LIMIT_PROVIDED.getLog());
                        concurrencyLimit = Math.min(Constants.DETOKENIZE_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
                    }
                } catch (NumberFormatException e) {
                    LogUtil.printWarningLog(WarningLogs.INVALID_CONCURRENCY_LIMIT_PROVIDED.getLog());
                    concurrencyLimit = Math.min(Constants.DETOKENIZE_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
                }
            } else {
                concurrencyLimit = Math.min(Constants.DETOKENIZE_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
            }
        } catch (Exception e) {
            batchSize = Constants.DETOKENIZE_BATCH_SIZE;
            int maxConcurrencyNeeded = (totalRequests + batchSize - 1) / batchSize;
            concurrencyLimit = Math.min(Constants.DETOKENIZE_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
        }
        return new BatchConfig(batchSize, concurrencyLimit);
    }

    private BulkInsertResponse processBulkInsertSync(
            V1InsertRequest insertRequest,
            ArrayList<BulkInsertRecord> originalPayload,
            RequestInterceptor interceptor,
            BatchConfig cfg
    ) throws ExecutionException, InterruptedException, SkyflowException {
        LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());
        List<Success> successRecords = new ArrayList<>();
        List<ErrorRecord> errorRecords = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<BulkInsertResponse>> futures = this.insertBatchFutures(insertRequest, errorRecords, interceptor, cfg);

        try {
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            try {
                allFutures.join();
            } catch (Exception e) {
                // individual batch errors are already captured
            }
            for (CompletableFuture<BulkInsertResponse> future : futures) {
                BulkInsertResponse futureResponse = future.get();
                if (futureResponse != null) {
                    if (futureResponse.getSuccess() != null) {
                        successRecords.addAll(futureResponse.getSuccess());
                    }
                    if (futureResponse.getErrors() != null) {
                        errorRecords.addAll(futureResponse.getErrors());
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        }
        BulkInsertResponse response = new BulkInsertResponse(successRecords, errorRecords, originalPayload);
        LogUtil.printInfoLog(InfoLogs.INSERT_REQUEST_RESOLVED.getLog());
        return response;
    }

    private BulkDetokenizeResponse processBulkDetokenizeSync(
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest detokenizeRequest,
            List<String> originalTokens,
            RequestInterceptor interceptor,
            BatchConfig cfg
    ) throws ExecutionException, InterruptedException, SkyflowException {
        LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());
        List<ErrorRecord> errorTokens = Collections.synchronizedList(new ArrayList<>());
        List<DetokenizeResponseObject> successTokens = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(cfg.concurrencyLimit);
        List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest> batches =
                Utils.createBulkDetokenizeBatches(detokenizeRequest, cfg.batchSize);
        try {
            List<CompletableFuture<BulkDetokenizeResponse>> futures = this.detokenizeBatchFutures(executor, batches, errorTokens, interceptor, cfg.batchSize);
            try {
                CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                allFutures.join();
            } catch (Exception e) {
                // individual batch errors are already captured
            }
            for (CompletableFuture<BulkDetokenizeResponse> future : futures) {
                BulkDetokenizeResponse futureResponse = future.get();
                if (futureResponse != null) {
                    if (futureResponse.getSuccess() != null) {
                        successTokens.addAll(futureResponse.getSuccess());
                    }
                    if (futureResponse.getErrors() != null) {
                        errorTokens.addAll(futureResponse.getErrors());
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.printErrorLog(ErrorLogs.DETOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        } finally {
            executor.shutdown();
        }
        BulkDetokenizeResponse response = new BulkDetokenizeResponse(successTokens, errorTokens, originalTokens);
        LogUtil.printInfoLog(InfoLogs.DETOKENIZE_REQUEST_RESOLVED.getLog());
        return response;
    }

    private List<CompletableFuture<BulkDetokenizeResponse>> detokenizeBatchFutures(
            ExecutorService executor,
            List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest> batches,
            List<ErrorRecord> errorTokens,
            RequestInterceptor interceptor,
            int batchSize) {
        List<CompletableFuture<BulkDetokenizeResponse>> futures = new ArrayList<>();
        try {
            for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest batch = batches.get(batchIndex);
                int batchNumber = batchIndex;
                RequestContext ctx = new RequestContext("DETOKENIZE");
                if (interceptor != null) interceptor.intercept(ctx);
                CompletableFuture<BulkDetokenizeResponse> future = CompletableFuture
                        .supplyAsync(() -> processDetokenizeBatch(batch, ctx), executor)
                        .thenApply(response -> Utils.formatBulkDetokenizeResponse(response.body(), batchNumber, batchSize, response.headers()))
                        .exceptionally(ex -> {
                            errorTokens.addAll(Utils.handleBulkDetokenizeBatchException(ex, batch, batchNumber, batchSize));
                            return null;
                        });
                futures.add(future);
            }
        } catch (Exception e) {
            ErrorRecord errorRecord = new ErrorRecord(0, e.getMessage(), 500);
            errorTokens.add(errorRecord);
        }
        return futures;
    }

    private ApiClientHttpResponse<com.skyflow.generated.rest.types.V1FlowDetokenizeResponse> processDetokenizeBatch(
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest batch,
            RequestContext ctx) {
        return this.getRecordsApi().withRawResponse().detokenize(batch, buildRequestOptions(ctx));
    }

    private List<CompletableFuture<BulkInsertResponse>> insertBatchFutures(
            V1InsertRequest insertRequest,
            List<ErrorRecord> errorRecords,
            RequestInterceptor interceptor,
            BatchConfig cfg) {
        List<V1InsertRecordData> records = insertRequest.getRecords().get();

        ExecutorService executor = Executors.newFixedThreadPool(cfg.concurrencyLimit);
        List<List<V1InsertRecordData>> batches = Utils.createBulkInsertBatches(records, cfg.batchSize);
        List<CompletableFuture<BulkInsertResponse>> futures = new ArrayList<>();
        V1Upsert upsert = insertRequest.getUpsert().isPresent() ? insertRequest.getUpsert().get() : null;

        try {
            for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
                List<V1InsertRecordData> batch = batches.get(batchIndex);
                int batchNumber = batchIndex;
                RequestContext ctx = new RequestContext("INSERT");
                if (interceptor != null) interceptor.intercept(ctx);
                CompletableFuture<BulkInsertResponse> future = CompletableFuture
                        .supplyAsync(() -> insertBatch(batch, insertRequest.getTableName().isPresent() ? insertRequest.getTableName().get() : null, upsert, ctx), executor)
                        .thenApply(response -> Utils.formatBulkInsertResponse(response.body(), batchNumber, cfg.batchSize, response.headers()))
                        .exceptionally(ex -> {
                            errorRecords.addAll(Utils.handleBulkInsertBatchException(ex, batch, batchNumber, cfg.batchSize));
                            return null;
                        });
                futures.add(future);
            }
        } finally {
            executor.shutdown();
        }
        return futures;
    }

    private ApiClientHttpResponse<V1InsertResponse> insertBatch(List<V1InsertRecordData> batch, String tableName, V1Upsert upsert, RequestContext ctx) {
        V1InsertRequest.Builder req = V1InsertRequest.builder()
                .vaultId(this.getVaultConfig().getVaultId())
                .records(batch)
                .upsert(upsert);
        if (tableName != null && !tableName.isEmpty()) {
            req.tableName(tableName);
        }
        V1InsertRequest request = req.build();
        return this.getRecordsApi().withRawResponse().insert(request, buildRequestOptions(ctx));
    }

    private BatchConfig configureInsertConcurrencyAndBatchSize(int totalRequests) {
        int batchSize = Constants.INSERT_BATCH_SIZE;
        int concurrencyLimit;
        try {
            String userProvidedBatchSize = System.getenv("INSERT_BATCH_SIZE");
            String userProvidedConcurrencyLimit = System.getenv("INSERT_CONCURRENCY_LIMIT");

            Dotenv dotenv = null;
            try {
                dotenv = Dotenv.load();
            } catch (DotenvException ignored) {
                // ignore the case if .env file is not found
            }

            if (userProvidedBatchSize == null && dotenv != null) {
                userProvidedBatchSize = dotenv.get("INSERT_BATCH_SIZE");
            }
            if (userProvidedConcurrencyLimit == null && dotenv != null) {
                userProvidedConcurrencyLimit = dotenv.get("INSERT_CONCURRENCY_LIMIT");
            }

            if (userProvidedBatchSize != null) {
                try {
                    int parsedBatchSize = Integer.parseInt(userProvidedBatchSize);
                    if (parsedBatchSize > Constants.MAX_INSERT_BATCH_SIZE) {
                        LogUtil.printWarningLog(WarningLogs.BATCH_SIZE_EXCEEDS_MAX_LIMIT.getLog());
                    }
                    int maxBatchSize = Math.min(parsedBatchSize, Constants.MAX_INSERT_BATCH_SIZE);
                    if (maxBatchSize > 0) {
                        batchSize = maxBatchSize;
                    } else {
                        LogUtil.printWarningLog(WarningLogs.INVALID_BATCH_SIZE_PROVIDED.getLog());
                        batchSize = Constants.INSERT_BATCH_SIZE;
                    }
                } catch (NumberFormatException e) {
                    LogUtil.printWarningLog(WarningLogs.INVALID_BATCH_SIZE_PROVIDED.getLog());
                    batchSize = Constants.INSERT_BATCH_SIZE;
                }
            }

            // Max no of threads required to run all batches concurrently at once
            int maxConcurrencyNeeded = (totalRequests + batchSize - 1) / batchSize;

            if (userProvidedConcurrencyLimit != null) {
                try {
                    int parsedConcurrencyLimit = Integer.parseInt(userProvidedConcurrencyLimit);
                    int maxConcurrencyLimit = Math.min(parsedConcurrencyLimit, Constants.MAX_INSERT_CONCURRENCY_LIMIT);
                    if (parsedConcurrencyLimit > Constants.MAX_INSERT_CONCURRENCY_LIMIT) {
                        LogUtil.printWarningLog(WarningLogs.CONCURRENCY_EXCEEDS_MAX_LIMIT.getLog());
                    }
                    if (maxConcurrencyLimit > 0) {
                        concurrencyLimit = Math.min(maxConcurrencyLimit, maxConcurrencyNeeded);
                    } else {
                        LogUtil.printWarningLog(WarningLogs.INVALID_CONCURRENCY_LIMIT_PROVIDED.getLog());
                        concurrencyLimit = Math.min(Constants.INSERT_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
                    }
                } catch (NumberFormatException e) {
                    LogUtil.printWarningLog(WarningLogs.INVALID_CONCURRENCY_LIMIT_PROVIDED.getLog());
                    concurrencyLimit = Math.min(Constants.INSERT_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
                }
            } else {
                concurrencyLimit = Math.min(Constants.INSERT_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
            }
        } catch (Exception e) {
            batchSize = Constants.INSERT_BATCH_SIZE;
            int maxConcurrencyNeeded = (totalRequests + batchSize - 1) / batchSize;
            concurrencyLimit = Math.min(Constants.INSERT_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
        }
        return new BatchConfig(batchSize, concurrencyLimit);
    }

}
