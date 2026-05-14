package com.skyflow.vault.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.skyflow.VaultClient;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.core.ApiClientHttpResponse;
import com.skyflow.generated.rest.core.RequestOptions;
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
import com.skyflow.enums.InterfaceName;
import com.skyflow.vault.data.DeleteTokensOptions;
import com.skyflow.vault.data.DeleteTokensRequest;
import com.skyflow.vault.data.DeleteTokensResponse;
import com.skyflow.vault.data.DeleteTokensSuccess;
import com.skyflow.vault.data.DetokenizeOptions;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.DetokenizeResponse;
import com.skyflow.vault.data.DetokenizeResponseObject;
import com.skyflow.vault.data.ErrorRecord;
import com.skyflow.vault.data.InsertOptions;
import com.skyflow.vault.data.InsertRecord;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.RequestContext;
import com.skyflow.vault.data.RequestInterceptor;
import com.skyflow.vault.data.Success;
import com.skyflow.vault.data.TokenizeOptions;
import com.skyflow.vault.data.TokenizeRequest;
import com.skyflow.vault.data.TokenizeResponse;
import com.skyflow.vault.data.TokenizeSuccess;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

public final class VaultController extends VaultClient {
    private static final Gson gson = new GsonBuilder().serializeNulls().create();
    private JsonObject metrics = Utils.getMetrics();
    private int insertBatchSize;
    private int insertConcurrencyLimit;
    private int detokenizeBatchSize;
    private int detokenizeConcurrencyLimit;
    private int deleteTokensBatchSize;
    private int deleteTokensConcurrencyLimit;
    private int tokenizeBatchSize;
    private int tokenizeConcurrencyLimit;

    public VaultController(VaultConfig vaultConfig, Credentials credentials) throws SkyflowException {
        super(vaultConfig, credentials);
        this.insertBatchSize = Constants.INSERT_BATCH_SIZE;
        this.insertConcurrencyLimit = Constants.INSERT_CONCURRENCY_LIMIT;
        this.detokenizeBatchSize = Constants.DETOKENIZE_BATCH_SIZE;
        this.detokenizeConcurrencyLimit = Constants.DETOKENIZE_CONCURRENCY_LIMIT;
        this.deleteTokensBatchSize = Constants.DELETE_TOKENS_BATCH_SIZE;
        this.deleteTokensConcurrencyLimit = Constants.DELETE_TOKENS_CONCURRENCY_LIMIT;
        this.tokenizeBatchSize = Constants.TOKENIZE_BATCH_SIZE;
        this.tokenizeConcurrencyLimit = Constants.TOKENIZE_CONCURRENCY_LIMIT;
    }

    // ── Insert ────────────────────────────────────────────────────────────────

    public com.skyflow.vault.data.InsertResponse bulkInsert(InsertRequest insertRequest) throws SkyflowException {
        return bulkInsert(insertRequest, null);
    }

    public com.skyflow.vault.data.InsertResponse bulkInsert(InsertRequest insertRequest, InsertOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.INSERT_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_INSERT_REQUEST.getLog());
            Validations.validateInsertRequest(insertRequest);
            configureInsertConcurrencyAndBatchSize(insertRequest.getRecords().size());

            setBearerToken();
            com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest request = super.getBulkInsertRequestBody(insertRequest, super.getVaultConfig());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            return this.processSync(request, insertRequest.getRecords(), interceptor);
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

    public CompletableFuture<com.skyflow.vault.data.InsertResponse> bulkInsertAsync(InsertRequest insertRequest) throws SkyflowException {
        return bulkInsertAsync(insertRequest, null);
    }

    public CompletableFuture<com.skyflow.vault.data.InsertResponse> bulkInsertAsync(InsertRequest insertRequest, InsertOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.INSERT_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_INSERT_REQUEST.getLog());
            Validations.validateInsertRequest(insertRequest);
            configureInsertConcurrencyAndBatchSize(insertRequest.getRecords().size());

            setBearerToken();
            com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest request = super.getBulkInsertRequestBody(insertRequest, super.getVaultConfig());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            List<ErrorRecord> errorRecords = Collections.synchronizedList(new ArrayList<>());
            List<CompletableFuture<com.skyflow.vault.data.InsertResponse>> futures = this.insertBatchFutures(request, errorRecords, interceptor);

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        List<Success> successRecords = new ArrayList<>();

                        for (CompletableFuture<com.skyflow.vault.data.InsertResponse> future : futures) {
                            com.skyflow.vault.data.InsertResponse futureResponse = future.join();
                            if (futureResponse != null) {
                                if (futureResponse.getSuccess() != null) {
                                    successRecords.addAll(futureResponse.getSuccess());
                                }
                                if (futureResponse.getErrors() != null) {
                                    errorRecords.addAll(futureResponse.getErrors());
                                }
                            }
                        }

                        return new com.skyflow.vault.data.InsertResponse(successRecords, errorRecords, insertRequest.getRecords());
                    });
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        }
    }

    // ── Detokenize ────────────────────────────────────────────────────────────

    public DetokenizeResponse bulkDetokenize(DetokenizeRequest detokenizeRequest) throws SkyflowException {
        return bulkDetokenize(detokenizeRequest, null);
    }

    public DetokenizeResponse bulkDetokenize(DetokenizeRequest detokenizeRequest, DetokenizeOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DETOKENIZE_TRIGGERED.getLog());
        try {
            configureDetokenizeConcurrencyAndBatchSize(detokenizeRequest.getTokens().size());
            LogUtil.printInfoLog(InfoLogs.VALIDATE_DETOKENIZE_REQUEST.getLog());
            Validations.validateDetokenizeRequest(detokenizeRequest);
            setBearerToken();
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest request = super.getDetokenizeRequestBody(detokenizeRequest);
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            return this.processDetokenizeSync(request, detokenizeRequest.getTokens(), interceptor);
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.DETOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ExecutionException | InterruptedException e) {
            LogUtil.printErrorLog(ErrorLogs.DETOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        }
    }

    public CompletableFuture<DetokenizeResponse> bulkDetokenizeAsync(DetokenizeRequest detokenizeRequest) throws SkyflowException {
        return bulkDetokenizeAsync(detokenizeRequest, null);
    }

    public CompletableFuture<DetokenizeResponse> bulkDetokenizeAsync(DetokenizeRequest detokenizeRequest, DetokenizeOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DETOKENIZE_TRIGGERED.getLog());
        ExecutorService executor = Executors.newFixedThreadPool(detokenizeConcurrencyLimit);
        try {
            configureDetokenizeConcurrencyAndBatchSize(detokenizeRequest.getTokens().size());
            LogUtil.printInfoLog(InfoLogs.VALIDATE_DETOKENIZE_REQUEST.getLog());
            Validations.validateDetokenizeRequest(detokenizeRequest);
            setBearerToken();
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest request = super.getDetokenizeRequestBody(detokenizeRequest);
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;

            LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());

            List<ErrorRecord> errorTokens = Collections.synchronizedList(new ArrayList<>());
            List<DetokenizeResponseObject> successRecords = new ArrayList<>();

            List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest> batches = Utils.createDetokenizeBatches(request, detokenizeBatchSize);

            List<CompletableFuture<DetokenizeResponse>> futures = this.detokenizeBatchFutures(executor, batches, errorTokens, interceptor);
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        for (CompletableFuture<DetokenizeResponse> future : futures) {
                            DetokenizeResponse futureResponse = future.join();
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
                        executor.shutdown();
                        return new DetokenizeResponse(successRecords, errorTokens, detokenizeRequest.getTokens());
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
            executor.shutdown();
        }
    }

    // ── Delete Tokens ─────────────────────────────────────────────────────────

    public DeleteTokensResponse bulkDeleteTokens(DeleteTokensRequest deleteTokensRequest) throws SkyflowException {
        return bulkDeleteTokens(deleteTokensRequest, null);
    }

    public DeleteTokensResponse bulkDeleteTokens(DeleteTokensRequest deleteTokensRequest, DeleteTokensOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DELETE_TOKENS_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_DELETE_TOKENS_REQUEST.getLog());
            Validations.validateDeleteTokensRequest(deleteTokensRequest);
            configureDeleteTokensConcurrencyAndBatchSize(deleteTokensRequest.getTokens().size());
            setBearerToken();
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest request =
                    super.getDeleteTokensRequestBody(deleteTokensRequest);
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            return this.processDeleteTokensSync(request, deleteTokensRequest.getTokens(), interceptor);
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.DELETE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ExecutionException | InterruptedException e) {
            LogUtil.printErrorLog(ErrorLogs.DELETE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        }
    }

    public CompletableFuture<DeleteTokensResponse> bulkDeleteTokensAsync(DeleteTokensRequest deleteTokensRequest) throws SkyflowException {
        return bulkDeleteTokensAsync(deleteTokensRequest, null);
    }

    public CompletableFuture<DeleteTokensResponse> bulkDeleteTokensAsync(DeleteTokensRequest deleteTokensRequest, DeleteTokensOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DELETE_TOKENS_TRIGGERED.getLog());
        ExecutorService executor = Executors.newFixedThreadPool(deleteTokensConcurrencyLimit);
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_DELETE_TOKENS_REQUEST.getLog());
            Validations.validateDeleteTokensRequest(deleteTokensRequest);
            configureDeleteTokensConcurrencyAndBatchSize(deleteTokensRequest.getTokens().size());
            setBearerToken();
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest request =
                    super.getDeleteTokensRequestBody(deleteTokensRequest);
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;

            LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());

            List<ErrorRecord> errorTokens = Collections.synchronizedList(new ArrayList<>());
            List<DeleteTokensSuccess> successRecords = Collections.synchronizedList(new ArrayList<>());

            List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest> batches =
                    Utils.createDeleteTokensBatches(request, deleteTokensBatchSize);

            List<CompletableFuture<DeleteTokensResponse>> futures =
                    this.deleteTokensBatchFutures(executor, batches, interceptor);

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        for (CompletableFuture<DeleteTokensResponse> future : futures) {
                            DeleteTokensResponse futureResponse = future.join();
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
                        executor.shutdown();
                        return new DeleteTokensResponse(successRecords, errorTokens, deleteTokensRequest.getTokens());
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
            executor.shutdown();
        }
    }

    // ── Tokenize ──────────────────────────────────────────────────────────────

    public TokenizeResponse bulkTokenize(TokenizeRequest tokenizeRequest) throws SkyflowException {
        return bulkTokenize(tokenizeRequest, null);
    }

    public TokenizeResponse bulkTokenize(TokenizeRequest tokenizeRequest, TokenizeOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.TOKENIZE_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_TOKENIZE_REQUEST.getLog());
            Validations.validateTokenizeRequest(tokenizeRequest);
            configureTokenizeConcurrencyAndBatchSize(tokenizeRequest.getData().size());
            setBearerToken();
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest request =
                    super.getTokenizeRequestBody(tokenizeRequest);
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            return this.processTokenizeSync(request, tokenizeRequest.getData(), interceptor);
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

    public CompletableFuture<TokenizeResponse> bulkTokenizeAsync(TokenizeRequest tokenizeRequest) throws SkyflowException {
        return bulkTokenizeAsync(tokenizeRequest, null);
    }

    public CompletableFuture<TokenizeResponse> bulkTokenizeAsync(TokenizeRequest tokenizeRequest, TokenizeOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.TOKENIZE_TRIGGERED.getLog());
        ExecutorService executor = Executors.newFixedThreadPool(tokenizeConcurrencyLimit);
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_TOKENIZE_REQUEST.getLog());
            Validations.validateTokenizeRequest(tokenizeRequest);
            configureTokenizeConcurrencyAndBatchSize(tokenizeRequest.getData().size());
            setBearerToken();
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest request =
                    super.getTokenizeRequestBody(tokenizeRequest);
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;

            LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());

            List<ErrorRecord> errorRecords = Collections.synchronizedList(new ArrayList<>());
            List<TokenizeSuccess> successRecords = Collections.synchronizedList(new ArrayList<>());

            List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest> batches =
                    Utils.createTokenizeBatches(request, tokenizeBatchSize);

            List<CompletableFuture<TokenizeResponse>> futures =
                    this.tokenizeBatchFutures(executor, batches, interceptor);

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        for (CompletableFuture<TokenizeResponse> future : futures) {
                            TokenizeResponse futureResponse = future.join();
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
                        executor.shutdown();
                        return new TokenizeResponse(successRecords, errorRecords, tokenizeRequest.getData());
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
            executor.shutdown();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private RequestOptions buildRequestOptions(RequestContext context) {
        RequestOptions.Builder builder = RequestOptions.builder()
                .addHeader(Constants.SDK_METRICS_HEADER_KEY, metrics.toString());
        context.getHeaders().forEach((k, v) -> builder.addHeader(k.toString(), v));
        return builder.build();
    }

    private DeleteTokensResponse processDeleteTokensSync(
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest deleteTokensRequest,
            List<String> originalTokens,
            RequestInterceptor interceptor
    ) throws ExecutionException, InterruptedException, SkyflowException {
        LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());
        List<ErrorRecord> errorRecords = Collections.synchronizedList(new ArrayList<>());
        List<DeleteTokensSuccess> successRecords = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(deleteTokensConcurrencyLimit);
        List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest> batches =
                Utils.createDeleteTokensBatches(deleteTokensRequest, deleteTokensBatchSize);
        try {
            List<CompletableFuture<DeleteTokensResponse>> futures =
                    this.deleteTokensBatchFutures(executor, batches, interceptor);
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            } catch (Exception e) {
                LogUtil.printErrorLog(ErrorLogs.DELETE_REQUEST_REJECTED.getLog());
            }
            for (CompletableFuture<DeleteTokensResponse> future : futures) {
                DeleteTokensResponse futureResponse = future.get();
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
        DeleteTokensResponse response = new DeleteTokensResponse(successRecords, errorRecords, originalTokens);
        LogUtil.printInfoLog(InfoLogs.DELETE_TOKENS_REQUEST_RESOLVED.getLog());
        return response;
    }

    private List<CompletableFuture<DeleteTokensResponse>> deleteTokensBatchFutures(
            ExecutorService executor,
            List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest> batches,
            RequestInterceptor interceptor) {
        List<CompletableFuture<DeleteTokensResponse>> futures = new ArrayList<>();
        if (batches == null) return futures;
        int totalBatches = batches.size();
        for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
            final int index = batchIndex;
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest batch = batches.get(index);
            RequestContext ctx = new RequestContext("DELETE_TOKENS", batchIndex, totalBatches);
            if (interceptor != null) interceptor.intercept(ctx);
            CompletableFuture<DeleteTokensResponse> future = CompletableFuture
                    .supplyAsync(() -> processDeleteTokensBatch(batch, ctx), executor)
                    .handle((result, ex) -> {
                        if (ex != null) {
                            List<ErrorRecord> batchErrors =
                                    Utils.handleDeleteTokensBatchException(ex, batch, index, deleteTokensBatchSize);
                            return new DeleteTokensResponse(new ArrayList<>(), batchErrors);
                        }
                        return Utils.formatDeleteTokensResponse(result.body(), index, deleteTokensBatchSize, result.headers());
                    });
            futures.add(future);
        }
        return futures;
    }

    private ApiClientHttpResponse<com.skyflow.generated.rest.types.V1FlowDeleteTokenResponse> processDeleteTokensBatch(
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest batch,
            RequestContext ctx) {
        return this.getRecordsApi().withRawResponse().deletetoken(batch, buildRequestOptions(ctx));
    }

    private void configureDeleteTokensConcurrencyAndBatchSize(int totalRequests) {
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
                    int batchSize = Integer.parseInt(userProvidedBatchSize);
                    if (batchSize > Constants.MAX_DELETE_TOKENS_BATCH_SIZE) {
                        LogUtil.printWarningLog(WarningLogs.BATCH_SIZE_EXCEEDS_MAX_LIMIT.getLog());
                    }
                    int maxBatchSize = Math.min(batchSize, Constants.MAX_DELETE_TOKENS_BATCH_SIZE);
                    if (maxBatchSize > 0) {
                        this.deleteTokensBatchSize = maxBatchSize;
                    } else {
                        LogUtil.printWarningLog(WarningLogs.INVALID_BATCH_SIZE_PROVIDED.getLog());
                        this.deleteTokensBatchSize = Constants.DELETE_TOKENS_BATCH_SIZE;
                    }
                } catch (NumberFormatException e) {
                    LogUtil.printWarningLog(WarningLogs.INVALID_BATCH_SIZE_PROVIDED.getLog());
                    this.deleteTokensBatchSize = Constants.DELETE_TOKENS_BATCH_SIZE;
                }
            }

            int maxConcurrencyNeeded = (totalRequests + this.deleteTokensBatchSize - 1) / this.deleteTokensBatchSize;

            if (userProvidedConcurrencyLimit != null) {
                try {
                    int concurrencyLimit = Integer.parseInt(userProvidedConcurrencyLimit);
                    if (concurrencyLimit > Constants.MAX_DELETE_TOKENS_CONCURRENCY_LIMIT) {
                        LogUtil.printWarningLog(WarningLogs.CONCURRENCY_EXCEEDS_MAX_LIMIT.getLog());
                    }
                    int maxConcurrencyLimit = Math.min(concurrencyLimit, Constants.MAX_DELETE_TOKENS_CONCURRENCY_LIMIT);
                    if (maxConcurrencyLimit > 0) {
                        this.deleteTokensConcurrencyLimit = Math.min(maxConcurrencyLimit, maxConcurrencyNeeded);
                    } else {
                        LogUtil.printWarningLog(WarningLogs.INVALID_CONCURRENCY_LIMIT_PROVIDED.getLog());
                        this.deleteTokensConcurrencyLimit = Math.min(Constants.DELETE_TOKENS_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
                    }
                } catch (NumberFormatException e) {
                    LogUtil.printWarningLog(WarningLogs.INVALID_CONCURRENCY_LIMIT_PROVIDED.getLog());
                    this.deleteTokensConcurrencyLimit = Math.min(Constants.DELETE_TOKENS_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
                }
            } else {
                this.deleteTokensConcurrencyLimit = Math.min(Constants.DELETE_TOKENS_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
            }
        } catch (Exception e) {
            this.deleteTokensBatchSize = Constants.DELETE_TOKENS_BATCH_SIZE;
            int maxConcurrencyNeeded = (totalRequests + this.deleteTokensBatchSize - 1) / this.deleteTokensBatchSize;
            this.deleteTokensConcurrencyLimit = Math.min(Constants.DELETE_TOKENS_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
        }
    }

    private TokenizeResponse processTokenizeSync(
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest tokenizeRequest,
            java.util.ArrayList<com.skyflow.vault.data.TokenizeRecord> originalData,
            RequestInterceptor interceptor
    ) throws ExecutionException, InterruptedException, SkyflowException {
        LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());
        List<ErrorRecord> errorRecords = Collections.synchronizedList(new ArrayList<>());
        List<TokenizeSuccess> successRecords = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(tokenizeConcurrencyLimit);
        List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest> batches =
                Utils.createTokenizeBatches(tokenizeRequest, tokenizeBatchSize);
        try {
            List<CompletableFuture<TokenizeResponse>> futures =
                    this.tokenizeBatchFutures(executor, batches, interceptor);
            try {
                CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                allFutures.join();
            } catch (Exception e) {
                // individual batch errors are already captured
            }
            for (CompletableFuture<TokenizeResponse> future : futures) {
                TokenizeResponse futureResponse = future.get();
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
        TokenizeResponse response = new TokenizeResponse(successRecords, errorRecords, originalData);
        LogUtil.printInfoLog(InfoLogs.TOKENIZE_REQUEST_RESOLVED.getLog());
        return response;
    }

    private List<CompletableFuture<TokenizeResponse>> tokenizeBatchFutures(
            ExecutorService executor,
            List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest> batches,
            RequestInterceptor interceptor) {
        List<CompletableFuture<TokenizeResponse>> futures = new ArrayList<>();
        if (batches == null) return futures;
        int totalBatches = batches.size();
        for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
            final int index = batchIndex;
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batch = batches.get(index);
            RequestContext ctx = new RequestContext("TOKENIZE", batchIndex, totalBatches);
            if (interceptor != null) interceptor.intercept(ctx);
            CompletableFuture<TokenizeResponse> future = CompletableFuture
                    .supplyAsync(() -> processTokenizeBatch(batch, ctx), executor)
                    .handle((result, ex) -> {
                        if (ex != null) {
                            List<ErrorRecord> batchErrors =
                                    Utils.handleTokenizeBatchException(ex, batch, index, tokenizeBatchSize);
                            return new TokenizeResponse(new ArrayList<>(), batchErrors);
                        }
                        return Utils.formatTokenizeResponse(result.body(), batch, index, tokenizeBatchSize, result.headers());
                    });
            futures.add(future);
        }
        return futures;
    }

    private ApiClientHttpResponse<com.skyflow.generated.rest.types.V1FlowTokenizeResponse> processTokenizeBatch(
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batch,
            RequestContext ctx) {
        return this.getRecordsApi().withRawResponse().tokenize(batch, buildRequestOptions(ctx));
    }

    private void configureTokenizeConcurrencyAndBatchSize(int totalRequests) {
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
                    int batchSize = Integer.parseInt(userProvidedBatchSize);
                    if (batchSize > Constants.MAX_TOKENIZE_BATCH_SIZE) {
                        LogUtil.printWarningLog(WarningLogs.BATCH_SIZE_EXCEEDS_MAX_LIMIT.getLog());
                    }
                    int maxBatchSize = Math.min(batchSize, Constants.MAX_TOKENIZE_BATCH_SIZE);
                    if (maxBatchSize > 0) {
                        this.tokenizeBatchSize = maxBatchSize;
                    } else {
                        LogUtil.printWarningLog(WarningLogs.INVALID_BATCH_SIZE_PROVIDED.getLog());
                        this.tokenizeBatchSize = Constants.TOKENIZE_BATCH_SIZE;
                    }
                } catch (NumberFormatException e) {
                    LogUtil.printWarningLog(WarningLogs.INVALID_BATCH_SIZE_PROVIDED.getLog());
                    this.tokenizeBatchSize = Constants.TOKENIZE_BATCH_SIZE;
                }
            }

            int maxConcurrencyNeeded = (totalRequests + this.tokenizeBatchSize - 1) / this.tokenizeBatchSize;

            if (userProvidedConcurrencyLimit != null) {
                try {
                    int concurrencyLimit = Integer.parseInt(userProvidedConcurrencyLimit);
                    if (concurrencyLimit > Constants.MAX_TOKENIZE_CONCURRENCY_LIMIT) {
                        LogUtil.printWarningLog(WarningLogs.CONCURRENCY_EXCEEDS_MAX_LIMIT.getLog());
                    }
                    int maxConcurrencyLimit = Math.min(concurrencyLimit, Constants.MAX_TOKENIZE_CONCURRENCY_LIMIT);
                    if (maxConcurrencyLimit > 0) {
                        this.tokenizeConcurrencyLimit = Math.min(maxConcurrencyLimit, maxConcurrencyNeeded);
                    } else {
                        LogUtil.printWarningLog(WarningLogs.INVALID_CONCURRENCY_LIMIT_PROVIDED.getLog());
                        this.tokenizeConcurrencyLimit = Math.min(Constants.TOKENIZE_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
                    }
                } catch (NumberFormatException e) {
                    LogUtil.printWarningLog(WarningLogs.INVALID_CONCURRENCY_LIMIT_PROVIDED.getLog());
                    this.tokenizeConcurrencyLimit = Math.min(Constants.TOKENIZE_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
                }
            } else {
                this.tokenizeConcurrencyLimit = Math.min(Constants.TOKENIZE_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
            }
        } catch (Exception e) {
            this.tokenizeBatchSize = Constants.TOKENIZE_BATCH_SIZE;
            int maxConcurrencyNeeded = (totalRequests + this.tokenizeBatchSize - 1) / this.tokenizeBatchSize;
            this.tokenizeConcurrencyLimit = Math.min(Constants.TOKENIZE_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
        }
    }

    private com.skyflow.vault.data.InsertResponse processSync(
            com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest insertRequest,
            ArrayList<InsertRecord> originalPayload,
            RequestInterceptor interceptor
    ) throws ExecutionException, InterruptedException {
        LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());
        List<Success> successRecords = new ArrayList<>();
        List<ErrorRecord> errorRecords = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<com.skyflow.vault.data.InsertResponse>> futures = this.insertBatchFutures(insertRequest, errorRecords, interceptor);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.join();

        for (CompletableFuture<com.skyflow.vault.data.InsertResponse> future : futures) {
            com.skyflow.vault.data.InsertResponse futureResponse = future.get();
            if (futureResponse != null) {
                if (futureResponse.getSuccess() != null) {
                    successRecords.addAll(futureResponse.getSuccess());
                }
                if (futureResponse.getErrors() != null) {
                    errorRecords.addAll(futureResponse.getErrors());
                }
            }
        }
        com.skyflow.vault.data.InsertResponse response = new com.skyflow.vault.data.InsertResponse(successRecords, errorRecords, originalPayload);
        LogUtil.printInfoLog(InfoLogs.INSERT_REQUEST_RESOLVED.getLog());
        return response;
    }

    private DetokenizeResponse processDetokenizeSync(
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest detokenizeRequest,
            List<String> originalTokens,
            RequestInterceptor interceptor
    ) throws ExecutionException, InterruptedException, SkyflowException {
        LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());
        List<ErrorRecord> errorTokens = Collections.synchronizedList(new ArrayList<>());
        List<DetokenizeResponseObject> successTokens = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(detokenizeConcurrencyLimit);
        List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest> batches = Utils.createDetokenizeBatches(detokenizeRequest, detokenizeBatchSize);
        try {
            List<CompletableFuture<DetokenizeResponse>> futures = this.detokenizeBatchFutures(executor, batches, errorTokens, interceptor);
            try {
                CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                allFutures.join();
            } catch (Exception e) {
                // individual batch errors are already captured
            }
            for (CompletableFuture<DetokenizeResponse> future : futures) {
                DetokenizeResponse futureResponse = future.get();
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
        DetokenizeResponse response = new DetokenizeResponse(successTokens, errorTokens, originalTokens);
        LogUtil.printInfoLog(InfoLogs.DETOKENIZE_REQUEST_RESOLVED.getLog());
        return response;
    }

    private List<CompletableFuture<DetokenizeResponse>> detokenizeBatchFutures(
            ExecutorService executor,
            List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest> batches,
            List<ErrorRecord> errorTokens,
            RequestInterceptor interceptor) {
        List<CompletableFuture<DetokenizeResponse>> futures = new ArrayList<>();
        try {
            int totalBatches = batches.size();
            for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
                com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest batch = batches.get(batchIndex);
                int batchNumber = batchIndex;
                RequestContext ctx = new RequestContext("DETOKENIZE", batchIndex, totalBatches);
                if (interceptor != null) interceptor.intercept(ctx);
                CompletableFuture<DetokenizeResponse> future = CompletableFuture
                        .supplyAsync(() -> processDetokenizeBatch(batch, ctx), executor)
                        .thenApply(response -> Utils.formatDetokenizeResponse(response.body(), batchNumber, detokenizeBatchSize, response.headers()))
                        .exceptionally(ex -> {
                            errorTokens.addAll(Utils.handleDetokenizeBatchException(ex, batch, batchNumber, detokenizeBatchSize));
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

    private List<CompletableFuture<com.skyflow.vault.data.InsertResponse>> insertBatchFutures(
            com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest insertRequest,
            List<ErrorRecord> errorRecords,
            RequestInterceptor interceptor) {
        List<V1InsertRecordData> records = insertRequest.getRecords().get();

        ExecutorService executor = Executors.newFixedThreadPool(insertConcurrencyLimit);
        List<List<V1InsertRecordData>> batches = Utils.createBatches(records, insertBatchSize);
        List<CompletableFuture<com.skyflow.vault.data.InsertResponse>> futures = new ArrayList<>();
        V1Upsert upsert = insertRequest.getUpsert().isPresent() ? insertRequest.getUpsert().get() : null;
        int totalBatches = batches.size();

        try {
            for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
                List<V1InsertRecordData> batch = batches.get(batchIndex);
                int batchNumber = batchIndex;
                RequestContext ctx = new RequestContext("INSERT", batchIndex, totalBatches);
                if (interceptor != null) interceptor.intercept(ctx);
                CompletableFuture<com.skyflow.vault.data.InsertResponse> future = CompletableFuture
                        .supplyAsync(() -> insertBatch(batch, insertRequest.getTableName().isPresent() ? insertRequest.getTableName().get() : null, upsert, ctx), executor)
                        .thenApply(response -> Utils.formatResponse(response.body(), batchNumber, insertBatchSize, response.headers()))
                        .exceptionally(ex -> {
                            errorRecords.addAll(Utils.handleBatchException(ex, batch, batchNumber, insertBatchSize));
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
        com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest.Builder req = com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest.builder()
                .vaultId(this.getVaultConfig().getVaultId())
                .records(batch)
                .upsert(upsert);
        if (tableName != null && !tableName.isEmpty()) {
            req.tableName(tableName);
        }
        com.skyflow.generated.rest.resources.flowservice.requests.V1InsertRequest request = req.build();
        return this.getRecordsApi().withRawResponse().insert(request, buildRequestOptions(ctx));
    }

    private void configureInsertConcurrencyAndBatchSize(int totalRequests) {
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
                    int batchSize = Integer.parseInt(userProvidedBatchSize);
                    if (batchSize > Constants.MAX_INSERT_BATCH_SIZE) {
                        LogUtil.printWarningLog(WarningLogs.BATCH_SIZE_EXCEEDS_MAX_LIMIT.getLog());
                    }
                    int maxBatchSize = Math.min(batchSize, Constants.MAX_INSERT_BATCH_SIZE);
                    if (maxBatchSize > 0) {
                        this.insertBatchSize = maxBatchSize;
                    } else {
                        LogUtil.printWarningLog(WarningLogs.INVALID_BATCH_SIZE_PROVIDED.getLog());
                        this.insertBatchSize = Constants.INSERT_BATCH_SIZE;
                    }
                } catch (NumberFormatException e) {
                    LogUtil.printWarningLog(WarningLogs.INVALID_BATCH_SIZE_PROVIDED.getLog());
                    this.insertBatchSize = Constants.INSERT_BATCH_SIZE;
                }
            }

            // Max no of threads required to run all batches concurrently at once
            int maxConcurrencyNeeded = (totalRequests + this.insertBatchSize - 1) / this.insertBatchSize;

            if (userProvidedConcurrencyLimit != null) {
                try {
                    int concurrencyLimit = Integer.parseInt(userProvidedConcurrencyLimit);
                    int maxConcurrencyLimit = Math.min(concurrencyLimit, Constants.MAX_INSERT_CONCURRENCY_LIMIT);
                    if (concurrencyLimit > Constants.MAX_INSERT_CONCURRENCY_LIMIT) {
                        LogUtil.printWarningLog(WarningLogs.CONCURRENCY_EXCEEDS_MAX_LIMIT.getLog());
                    }
                    if (maxConcurrencyLimit > 0) {
                        this.insertConcurrencyLimit = Math.min(maxConcurrencyLimit, maxConcurrencyNeeded);
                    } else {
                        LogUtil.printWarningLog(WarningLogs.INVALID_CONCURRENCY_LIMIT_PROVIDED.getLog());
                        this.insertConcurrencyLimit = Math.min(Constants.INSERT_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
                    }
                } catch (NumberFormatException e) {
                    LogUtil.printWarningLog(WarningLogs.INVALID_CONCURRENCY_LIMIT_PROVIDED.getLog());
                    this.insertConcurrencyLimit = Math.min(Constants.INSERT_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
                }
            } else {
                this.insertConcurrencyLimit = Math.min(Constants.INSERT_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
            }
        } catch (Exception e) {
            this.insertBatchSize = Constants.INSERT_BATCH_SIZE;
            int maxConcurrencyNeeded = (totalRequests + this.insertBatchSize - 1) / this.insertBatchSize;
            this.insertConcurrencyLimit = Math.min(Constants.INSERT_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
        }
    }


    private void configureDetokenizeConcurrencyAndBatchSize(int totalRequests) {
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
                    int batchSize = Integer.parseInt(userProvidedBatchSize);
                    if (batchSize > Constants.MAX_DETOKENIZE_BATCH_SIZE) {
                        LogUtil.printWarningLog(WarningLogs.BATCH_SIZE_EXCEEDS_MAX_LIMIT.getLog());
                    }
                    int maxBatchSize = Math.min(batchSize, Constants.MAX_DETOKENIZE_BATCH_SIZE);
                    if (maxBatchSize > 0) {
                        this.detokenizeBatchSize = maxBatchSize;
                    } else {
                        LogUtil.printWarningLog(WarningLogs.INVALID_BATCH_SIZE_PROVIDED.getLog());
                        this.detokenizeBatchSize = Constants.DETOKENIZE_BATCH_SIZE;
                    }
                } catch (NumberFormatException e) {
                    LogUtil.printWarningLog(WarningLogs.INVALID_BATCH_SIZE_PROVIDED.getLog());
                    this.detokenizeBatchSize = Constants.DETOKENIZE_BATCH_SIZE;
                }
            }

            // Max no of threads required to run all batches concurrently at once
            int maxConcurrencyNeeded = (totalRequests + this.detokenizeBatchSize - 1) / this.detokenizeBatchSize;

            if (userProvidedConcurrencyLimit != null) {
                try {
                    int concurrencyLimit = Integer.parseInt(userProvidedConcurrencyLimit);
                    if (concurrencyLimit > Constants.MAX_DETOKENIZE_CONCURRENCY_LIMIT) {
                        LogUtil.printWarningLog(WarningLogs.CONCURRENCY_EXCEEDS_MAX_LIMIT.getLog());
                    }
                    int maxConcurrencyLimit = Math.min(concurrencyLimit, Constants.MAX_DETOKENIZE_CONCURRENCY_LIMIT);

                    if (maxConcurrencyLimit > 0) {
                        this.detokenizeConcurrencyLimit = Math.min(maxConcurrencyLimit, maxConcurrencyNeeded);
                    } else {
                        LogUtil.printWarningLog(WarningLogs.INVALID_CONCURRENCY_LIMIT_PROVIDED.getLog());
                        this.detokenizeConcurrencyLimit = Math.min(Constants.DETOKENIZE_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
                    }
                } catch (NumberFormatException e) {
                    LogUtil.printWarningLog(WarningLogs.INVALID_CONCURRENCY_LIMIT_PROVIDED.getLog());
                    this.detokenizeConcurrencyLimit = Math.min(Constants.DETOKENIZE_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
                }
            } else {
                this.detokenizeConcurrencyLimit = Math.min(Constants.DETOKENIZE_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
            }
        } catch (Exception e) {
            this.detokenizeBatchSize = Constants.DETOKENIZE_BATCH_SIZE;
            int maxConcurrencyNeeded = (totalRequests + this.detokenizeBatchSize - 1) / this.detokenizeBatchSize;
            this.detokenizeConcurrencyLimit = Math.min(Constants.DETOKENIZE_CONCURRENCY_LIMIT, maxConcurrencyNeeded);
        }
    }

}