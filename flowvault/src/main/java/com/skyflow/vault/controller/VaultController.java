package com.skyflow.vault.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

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
import com.skyflow.generated.rest.resources.flowservice.requests.V1DeleteRequest;
import com.skyflow.generated.rest.resources.flowservice.requests.V1GetRequest;
import com.skyflow.generated.rest.resources.flowservice.requests.V1UpdateRequest;
import com.skyflow.generated.rest.resources.records.requests.V1ExecuteQueryRequest;
import com.skyflow.generated.rest.types.V1ExecuteQueryResponse;
import com.skyflow.generated.rest.types.V1FlowDeleteTokenResponse;
import com.skyflow.generated.rest.types.V1FlowTokenizeResponse;
import com.skyflow.generated.rest.types.V1InsertRecordData;
import com.skyflow.generated.rest.types.V1InsertResponse;
import com.skyflow.generated.rest.types.V1DeleteResponse;
import com.skyflow.generated.rest.types.V1GetResponse;
import com.skyflow.generated.rest.types.V1UpdateResponse;
import com.skyflow.generated.rest.types.V1Upsert;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.logs.WarningLogs;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.BulkDeleteTokensOptions;
import com.skyflow.vault.data.BulkDeleteTokensRequest;
import com.skyflow.vault.data.BulkDeleteTokensResponse;
import com.skyflow.vault.data.BulkDeleteTokensResponseRecord;
import com.skyflow.vault.data.BulkDetokenizeOptions;
import com.skyflow.vault.data.BulkDetokenizeRequest;
import com.skyflow.vault.data.BulkDetokenizeResponse;
import com.skyflow.vault.data.BulkDetokenizeResponseRecord;
import com.skyflow.vault.data.BulkInsertOptions;
import com.skyflow.vault.data.BulkInsertRequest;
import com.skyflow.vault.data.BulkInsertResponse;
import com.skyflow.vault.data.BulkInsertResponseRecord;
import com.skyflow.vault.data.BulkTokenizeOptions;
import com.skyflow.vault.data.BulkTokenizeRequest;
import com.skyflow.vault.data.BulkTokenizeRequestRecord;
import com.skyflow.vault.data.BulkTokenizeResponse;
import com.skyflow.vault.data.BulkTokenizeResponseRecord;
import com.skyflow.vault.data.DetokenizeOptions;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.DetokenizeResponse;
import com.skyflow.vault.data.InsertOptions;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertRequestRecord;
import com.skyflow.vault.data.InsertResponse;
import com.skyflow.vault.data.QueryOptions;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.QueryResponse;
import com.skyflow.vault.data.RequestContext;
import com.skyflow.vault.data.UpdateOptions;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateResponse;
import com.skyflow.vault.data.DeleteOptions;
import com.skyflow.vault.data.DeleteRequest;
import com.skyflow.vault.data.DeleteResponse;
import com.skyflow.vault.data.GetOptions;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;
import com.skyflow.vault.data.RequestInterceptor;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

public final class VaultController extends VaultClient {
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

    // ── Insert ────────────────────────────────────────────────────────────────
    // Unary counterpart of bulkInsert: sends every record in a single API call, with no
    // batching or concurrency involved.

    public InsertResponse insert(InsertRequest insertRequest) throws SkyflowException {
        return insert(insertRequest, null);
    }

    public InsertResponse insert(InsertRequest insertRequest, InsertOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.INSERT_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_INSERT_REQUEST.getLog());
            Validations.validateInsertRequest(insertRequest);

            setBearerToken();
            V1InsertRequest request = Utils.getInsertRequestBody(insertRequest, this.getVaultConfig());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            RequestContext ctx = new RequestContext("INSERT", 0, 1);
            if (interceptor != null) interceptor.intercept(ctx);

            ApiClientHttpResponse<V1InsertResponse> response =
                    this.getRecordsApi().withRawResponse().insert(request, buildRequestOptions(ctx));

            InsertResponse formattedResponse = Utils.formatInsertResponse(response.body(), response.headers());
            LogUtil.printInfoLog(InfoLogs.INSERT_REQUEST_RESOLVED.getLog());
            return formattedResponse;
        } catch (ApiClientApiException e) {
            // The lone record in a unary request can fail outright, which the vault reflects as
            // the overall HTTP status. If the body still carries the usual per-record shape,
            // surface it on the response like a 200 partial success would, not as an exception.
            InsertResponse fallback = Utils.handleInsertRequestException(e);
            if (fallback != null) {
                return fallback;
            }
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            throw new SkyflowException(e);
        }
    }


    // ── Bulk Insert ───────────────────────────────────────────────────────────

    public BulkInsertResponse bulkInsert(BulkInsertRequest insertRequest) throws SkyflowException {
        return bulkInsert(insertRequest, null);
    }

    public BulkInsertResponse bulkInsert(BulkInsertRequest insertRequest, BulkInsertOptions options) throws SkyflowException {
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
        } catch (ApiClientException e) {
            throw new SkyflowException(e);
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

    public CompletableFuture<BulkInsertResponse> bulkInsertAsync(BulkInsertRequest insertRequest, BulkInsertOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.INSERT_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_INSERT_REQUEST.getLog());
            Validations.validateBulkInsertRequest(insertRequest);
            BatchConfig cfg = configureInsertConcurrencyAndBatchSize(insertRequest.getRecords().size());

            setBearerToken();
            V1InsertRequest request = Utils.getBulkInsertRequestBody(insertRequest, this.getVaultConfig());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            List<CompletableFuture<BulkInsertResponse>> futures = this.insertBatchFutures(request, interceptor, cfg);

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        List<BulkInsertResponseRecord> records = new ArrayList<>();

                        for (CompletableFuture<BulkInsertResponse> future : futures) {
                            BulkInsertResponse futureResponse = future.join();
                            if (futureResponse != null && futureResponse.getRecords() != null) {
                                records.addAll(futureResponse.getRecords());
                            }
                        }

                        return new BulkInsertResponse(records, insertRequest.getRecords());
                    });
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (SkyflowException e) {
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            throw e;
        } catch (Exception e) {
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        }
    }

    // ── Detokenize ────────────────────────────────────────────────────────────
    // Unary counterpart of bulkDetokenize: sends every token in a single API call, with no
    // batching or concurrency involved.

    public DetokenizeResponse detokenize(DetokenizeRequest detokenizeRequest) throws SkyflowException {
        return detokenize(detokenizeRequest, null);
    }

    public DetokenizeResponse detokenize(DetokenizeRequest detokenizeRequest, DetokenizeOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DETOKENIZE_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_DETOKENIZE_REQUEST.getLog());
            Validations.validateDetokenizeRequest(detokenizeRequest);

            setBearerToken();
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest request =
                    Utils.getDetokenizeRequestBody(detokenizeRequest, this.getVaultConfig().getVaultId());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            RequestContext ctx = new RequestContext("DETOKENIZE", 0, 1);
            if (interceptor != null) interceptor.intercept(ctx);

            ApiClientHttpResponse<com.skyflow.generated.rest.types.V1FlowDetokenizeResponse> response =
                    this.getRecordsApi().withRawResponse().detokenize(request, buildRequestOptions(ctx));

            DetokenizeResponse formattedResponse = Utils.formatDetokenizeResponse(response.body(), response.headers());
            LogUtil.printInfoLog(InfoLogs.DETOKENIZE_REQUEST_RESOLVED.getLog());
            return formattedResponse;
        } catch (ApiClientApiException e) {
            // The lone record in a unary request can fail outright, which the vault reflects as
            // the overall HTTP status. If the body still carries the usual per-record shape,
            // surface it on the response like a 200 partial success would, not as an exception.
            DetokenizeResponse fallback = Utils.handleDetokenizeRequestException(e);
            if (fallback != null) {
                return fallback;
            }
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.DETOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.DETOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e);
        }
    }

    // ── Bulk Detokenize ───────────────────────────────────────────────────────

    public BulkDetokenizeResponse bulkDetokenize(BulkDetokenizeRequest detokenizeRequest) throws SkyflowException {
        return bulkDetokenize(detokenizeRequest, null);
    }

    public BulkDetokenizeResponse bulkDetokenize(BulkDetokenizeRequest detokenizeRequest, BulkDetokenizeOptions options) throws SkyflowException {
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
        } catch (ApiClientException e) {
            throw new SkyflowException(e);
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

    public CompletableFuture<BulkDetokenizeResponse> bulkDetokenizeAsync(BulkDetokenizeRequest detokenizeRequest, BulkDetokenizeOptions options) throws SkyflowException {
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

            List<BulkDetokenizeResponseRecord> records = new ArrayList<>();

            List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest> batches =
                    Utils.createBulkDetokenizeBatches(request, cfg.batchSize);

            executor = Executors.newFixedThreadPool(cfg.concurrencyLimit);
            List<CompletableFuture<BulkDetokenizeResponse>> futures = this.detokenizeBatchFutures(executor, batches, interceptor, cfg.batchSize);
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        for (CompletableFuture<BulkDetokenizeResponse> future : futures) {
                            BulkDetokenizeResponse futureResponse = future.join();
                            if (futureResponse != null && futureResponse.getRecords() != null) {
                                records.addAll(futureResponse.getRecords());
                            }
                        }
                        LogUtil.printInfoLog(InfoLogs.DETOKENIZE_REQUEST_RESOLVED.getLog());
                        return new BulkDetokenizeResponse(records, detokenizeRequest.getTokens());
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

    // ── Delete ────────────────────────────────────────────────────────────────
    // Deletes records by skyflowId or unique value, in a single API call. There is no
    // bulk/batched counterpart of this operation. Distinct from deleteTokens/bulkDeleteTokens,
    // which remove tokens only and leave the underlying record in place.

    public DeleteResponse delete(DeleteRequest deleteRequest) throws SkyflowException {
        return delete(deleteRequest, null);
    }

    public DeleteResponse delete(DeleteRequest deleteRequest, DeleteOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DELETE_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_DELETE_REQUEST.getLog());
            Validations.validateDeleteRequest(deleteRequest);

            setBearerToken();
            V1DeleteRequest request = Utils.getDeleteRequestBody(deleteRequest, this.getVaultConfig());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            RequestContext ctx = new RequestContext("DELETE", 0, 1);
            if (interceptor != null) interceptor.intercept(ctx);

            ApiClientHttpResponse<V1DeleteResponse> response =
                    this.getRecordsApi().withRawResponse().delete(request, buildRequestOptions(ctx));

            DeleteResponse formattedResponse = Utils.formatDeleteResponse(response.body(), response.headers());
            LogUtil.printInfoLog(InfoLogs.DELETE_REQUEST_RESOLVED.getLog());
            return formattedResponse;
        } catch (ApiClientApiException e) {
            // The lone record in a unary request can fail outright, which the vault reflects as
            // the overall HTTP status. If the body still carries the usual per-record shape,
            // surface it on the response like a 200 partial success would, not as an exception.
            DeleteResponse fallback = Utils.handleDeleteRequestException(e);
            if (fallback != null) {
                return fallback;
            }
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.DELETE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.DELETE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e);
        }
    }

    // ── Bulk Delete Tokens ────────────────────────────────────────────────────

    public BulkDeleteTokensResponse bulkDeleteTokens(BulkDeleteTokensRequest deleteTokensRequest) throws SkyflowException {
        return bulkDeleteTokens(deleteTokensRequest, null);
    }

    public BulkDeleteTokensResponse bulkDeleteTokens(BulkDeleteTokensRequest deleteTokensRequest, BulkDeleteTokensOptions options) throws SkyflowException {
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
        } catch (ApiClientException e) {
            throw new SkyflowException(e);
        } catch (ExecutionException | InterruptedException e) {
            LogUtil.printErrorLog(ErrorLogs.DELETE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        }
    }

    public CompletableFuture<BulkDeleteTokensResponse> bulkDeleteTokensAsync(BulkDeleteTokensRequest deleteTokensRequest) throws SkyflowException {
        return bulkDeleteTokensAsync(deleteTokensRequest, null);
    }

    public CompletableFuture<BulkDeleteTokensResponse> bulkDeleteTokensAsync(BulkDeleteTokensRequest deleteTokensRequest, BulkDeleteTokensOptions options) throws SkyflowException {
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

            List<BulkDeleteTokensResponseRecord> responseRecords = Collections.synchronizedList(new ArrayList<>());

            List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest> batches =
                    Utils.createBulkDeleteTokensBatches(request, cfg.batchSize);

            executor = Executors.newFixedThreadPool(cfg.concurrencyLimit);
            List<CompletableFuture<BulkDeleteTokensResponse>> futures =
                    this.deleteTokensBatchFutures(executor, batches, interceptor, cfg.batchSize);

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        for (CompletableFuture<BulkDeleteTokensResponse> future : futures) {
                            BulkDeleteTokensResponse futureResponse = future.join();
                            if (futureResponse != null && futureResponse.getRecords() != null) {
                                responseRecords.addAll(futureResponse.getRecords());
                            }
                        }
                        LogUtil.printInfoLog(InfoLogs.DELETE_TOKENS_REQUEST_RESOLVED.getLog());
                        return new BulkDeleteTokensResponse(
                                sortByIndex(responseRecords), deleteTokensRequest.getTokens());
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

    public BulkTokenizeResponse bulkTokenize(BulkTokenizeRequest tokenizeRequest, BulkTokenizeOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.TOKENIZE_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_TOKENIZE_REQUEST.getLog());
            Validations.validateBulkTokenizeRequest(tokenizeRequest);
            BatchConfig cfg = configureTokenizeConcurrencyAndBatchSize(tokenizeRequest.getRecords().size());
            setBearerToken();
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            return this.processBulkTokenizeSync(tokenizeRequest.getRecords(), interceptor, cfg);
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.TOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (SkyflowException e) {
            LogUtil.printErrorLog(ErrorLogs.TOKENIZE_REQUEST_REJECTED.getLog());
            throw e;
        } catch (ApiClientException e) {
            throw new SkyflowException(e);
        } catch (ExecutionException | InterruptedException e) {
            LogUtil.printErrorLog(ErrorLogs.TOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        }
    }

    public CompletableFuture<BulkTokenizeResponse> bulkTokenizeAsync(BulkTokenizeRequest tokenizeRequest) throws SkyflowException {
        return bulkTokenizeAsync(tokenizeRequest, null);
    }

    public CompletableFuture<BulkTokenizeResponse> bulkTokenizeAsync(BulkTokenizeRequest tokenizeRequest, BulkTokenizeOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.TOKENIZE_TRIGGERED.getLog());
        ExecutorService executor = null;
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_TOKENIZE_REQUEST.getLog());
            Validations.validateBulkTokenizeRequest(tokenizeRequest);
            BatchConfig cfg = configureTokenizeConcurrencyAndBatchSize(tokenizeRequest.getRecords().size());
            setBearerToken();
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;

            LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());

            List<BulkTokenizeResponseRecord> responseRecords = Collections.synchronizedList(new ArrayList<>());

            List<List<BulkTokenizeRequestRecord>> batches =
                    Utils.createBulkTokenizeBatches(tokenizeRequest.getRecords(), cfg.batchSize);

            executor = Executors.newFixedThreadPool(cfg.concurrencyLimit);
            List<CompletableFuture<BulkTokenizeResponse>> futures =
                    this.tokenizeBatchFutures(executor, batches, interceptor);

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        for (CompletableFuture<BulkTokenizeResponse> future : futures) {
                            BulkTokenizeResponse futureResponse = future.join();
                            if (futureResponse != null && futureResponse.getRecords() != null) {
                                responseRecords.addAll(futureResponse.getRecords());
                            }
                        }
                        LogUtil.printInfoLog(InfoLogs.TOKENIZE_REQUEST_RESOLVED.getLog());
                        return new BulkTokenizeResponse(
                                sortTokenizeByIndex(responseRecords), tokenizeRequest.getRecords());
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

    // ── Query ─────────────────────────────────────────────────────────────────
    // Runs a query in a single API call. There is no bulk/batched counterpart of this operation.

    public QueryResponse query(QueryRequest queryRequest) throws SkyflowException {
        return query(queryRequest, null);
    }

    public QueryResponse query(QueryRequest queryRequest, QueryOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.QUERY_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_QUERY_REQUEST.getLog());
            Validations.validateQueryRequest(queryRequest);

            setBearerToken();
            V1ExecuteQueryRequest request = Utils.getQueryRequestBody(queryRequest, this.getVaultConfig().getVaultId());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            RequestContext ctx = new RequestContext("QUERY", 0, 1);
            if (interceptor != null) interceptor.intercept(ctx);

            ApiClientHttpResponse<V1ExecuteQueryResponse> response =
                    this.getQueryApi().withRawResponse().flowServiceExecuteQuery(request, buildRequestOptions(ctx));

            QueryResponse formattedResponse = Utils.formatQueryResponse(response.body());
            LogUtil.printInfoLog(InfoLogs.QUERY_REQUEST_RESOLVED.getLog());
            return formattedResponse;
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.QUERY_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.QUERY_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e);
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────
    // Runs an update in a single API call. There is no bulk/batched counterpart of this operation.

    public UpdateResponse update(UpdateRequest updateRequest) throws SkyflowException {
        return update(updateRequest, null);
    }

    public UpdateResponse update(UpdateRequest updateRequest, UpdateOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.UPDATE_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_UPDATE_REQUEST.getLog());
            Validations.validateUpdateRequest(updateRequest);

            setBearerToken();
            V1UpdateRequest request = Utils.getUpdateRequestBody(updateRequest, this.getVaultConfig());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            RequestContext ctx = new RequestContext("UPDATE", 0, 1);
            if (interceptor != null) interceptor.intercept(ctx);

            ApiClientHttpResponse<V1UpdateResponse> response =
                    this.getRecordsApi().withRawResponse().update(request, buildRequestOptions(ctx));

            UpdateResponse formattedResponse = Utils.formatUpdateResponse(response.body(), response.headers());
            LogUtil.printInfoLog(InfoLogs.UPDATE_REQUEST_RESOLVED.getLog());
            return formattedResponse;
        } catch (ApiClientApiException e) {
            // The lone record in a unary request can fail outright, which the vault reflects as
            // the overall HTTP status. If the body still carries the usual per-record shape,
            // surface it on the response like a 200 partial success would, not as an exception.
            UpdateResponse fallback = Utils.handleUpdateRequestException(e);
            if (fallback != null) {
                return fallback;
            }
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.UPDATE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.UPDATE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e);
        }
    }

    // ── Get ───────────────────────────────────────────────────────────────────
    // Runs a get in a single API call. There is no bulk/batched counterpart of this operation.

    public GetResponse get(GetRequest getRequest) throws SkyflowException {
        return get(getRequest, null);
    }

    public GetResponse get(GetRequest getRequest, GetOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.GET_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_GET_REQUEST.getLog());
            Validations.validateGetRequest(getRequest);

            setBearerToken();
            V1GetRequest request = Utils.getGetRequestBody(getRequest, this.getVaultConfig());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            RequestContext ctx = new RequestContext("GET", 0, 1);
            if (interceptor != null) interceptor.intercept(ctx);

            ApiClientHttpResponse<V1GetResponse> response =
                    this.getRecordsApi().withRawResponse().get(request, buildRequestOptions(ctx));

            GetResponse formattedResponse = Utils.formatGetResponse(response.body(), response.headers());
            LogUtil.printInfoLog(InfoLogs.GET_REQUEST_RESOLVED.getLog());
            return formattedResponse;
        } catch (ApiClientApiException e) {
            // The lone record in a unary request can fail outright, which the vault reflects as
            // the overall HTTP status. If the body still carries the usual per-record shape,
            // surface it on the response like a 200 partial success would, not as an exception.
            GetResponse fallback = Utils.handleGetRequestException(e);
            if (fallback != null) {
                return fallback;
            }
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.GET_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.GET_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e);
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
        List<BulkDeleteTokensResponseRecord> responseRecords = new ArrayList<>();
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
                if (futureResponse != null && futureResponse.getRecords() != null) {
                    responseRecords.addAll(futureResponse.getRecords());
                }
            }
        } catch (Exception e) {
            LogUtil.printErrorLog(ErrorLogs.DELETE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        } finally {
            executor.shutdown();
        }
        BulkDeleteTokensResponse response =
                new BulkDeleteTokensResponse(sortByIndex(responseRecords), originalTokens);
        LogUtil.printInfoLog(InfoLogs.DELETE_TOKENS_REQUEST_RESOLVED.getLog());
        return response;
    }

    /**
     * Batches complete concurrently, so order the unified records by their position in the original
     * request before handing them back to the caller.
     */
    private static List<BulkDeleteTokensResponseRecord> sortByIndex(List<BulkDeleteTokensResponseRecord> records) {
        List<BulkDeleteTokensResponseRecord> sorted = new ArrayList<>(records);
        sorted.sort(Comparator.comparingInt(BulkDeleteTokensResponseRecord::getIndex));
        return sorted;
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
            RequestContext ctx = new RequestContext("DELETE_TOKENS", batchIndex, batches.size());
            if (interceptor != null) interceptor.intercept(ctx);
            CompletableFuture<BulkDeleteTokensResponse> future = CompletableFuture
                    .supplyAsync(() -> processDeleteTokensBatch(batch, ctx), executor)
                    .handle((result, ex) -> {
                        if (ex != null) {
                            List<BulkDeleteTokensResponseRecord> batchErrors =
                                    Utils.handleBulkDeleteTokensBatchException(ex, batch, index, batchSize);
                            return new BulkDeleteTokensResponse(batchErrors);
                        }
                        return Utils.formatBulkDeleteTokensResponse(
                                result.body(), batch, index, batchSize, result.headers());
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

    /**
     * Resolves a user-tunable batching setting: process environment first, then a {@code .env} file.
     * Package-private and swappable purely so tests can drive batching and concurrency without
     * mutating the JVM environment — deliberately not public, so it stays out of the frozen
     * public API surface.
     */
    static Function<String, String> settingResolver = VaultController::resolveSettingFromEnvironment;

    private static String resolveSettingFromEnvironment(String key) {
        String value = System.getenv(key);
        if (value == null) {
            try {
                value = Dotenv.load().get(key);
            } catch (DotenvException ignored) {
                // no .env available — environment-only
            }
        }
        return value;
    }

    private BatchConfig configureDeleteTokensConcurrencyAndBatchSize(int totalRequests) {
        int batchSize = Constants.DELETE_TOKENS_BATCH_SIZE;
        int concurrencyLimit;
        try {
            String userProvidedBatchSize = settingResolver.apply("DELETE_TOKENS_BATCH_SIZE");
            String userProvidedConcurrencyLimit = settingResolver.apply("DELETE_TOKENS_CONCURRENCY_LIMIT");

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
            List<BulkTokenizeRequestRecord> originalRecords,
            RequestInterceptor interceptor,
            BatchConfig cfg
    ) throws ExecutionException, InterruptedException, SkyflowException {
        LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());
        List<BulkTokenizeResponseRecord> responseRecords = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(cfg.concurrencyLimit);
        List<List<BulkTokenizeRequestRecord>> batches =
                Utils.createBulkTokenizeBatches(originalRecords, cfg.batchSize);
        try {
            List<CompletableFuture<BulkTokenizeResponse>> futures =
                    this.tokenizeBatchFutures(executor, batches, interceptor);
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            } catch (Exception e) {
                // individual batch errors are already captured
            }
            for (CompletableFuture<BulkTokenizeResponse> future : futures) {
                BulkTokenizeResponse futureResponse = future.get();
                if (futureResponse != null && futureResponse.getRecords() != null) {
                    responseRecords.addAll(futureResponse.getRecords());
                }
            }
        } catch (Exception e) {
            LogUtil.printErrorLog(ErrorLogs.TOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        } finally {
            executor.shutdown();
        }
        BulkTokenizeResponse response =
                new BulkTokenizeResponse(sortTokenizeByIndex(responseRecords), originalRecords);
        LogUtil.printInfoLog(InfoLogs.TOKENIZE_REQUEST_RESOLVED.getLog());
        return response;
    }

    /** Batches complete concurrently; order results by the index the SDK assigned each record. */
    private static List<BulkTokenizeResponseRecord> sortTokenizeByIndex(List<BulkTokenizeResponseRecord> records) {
        List<BulkTokenizeResponseRecord> sorted = new ArrayList<>(records);
        sorted.sort(Comparator.comparingInt(BulkTokenizeResponseRecord::getIndex));
        return sorted;
    }

    private List<CompletableFuture<BulkTokenizeResponse>> tokenizeBatchFutures(
            ExecutorService executor,
            List<List<BulkTokenizeRequestRecord>> batches,
            RequestInterceptor interceptor) {
        List<CompletableFuture<BulkTokenizeResponse>> futures = new ArrayList<>();
        if (batches == null) return futures;
        // batches are contiguous but not uniformly sized - a batch is cut short when it would
        // otherwise repeat a value - so track where each one starts rather than deriving it
        int nextStartIndex = 0;
        int batchPosition = 0;
        for (List<BulkTokenizeRequestRecord> batchRecords : batches) {
            final int startIndex = nextStartIndex;
            nextStartIndex += batchRecords.size();
            final int batchIndex = batchPosition++;
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowTokenizeRequest batch =
                    Utils.getBulkTokenizeRequestBody(batchRecords, this.getVaultConfig().getVaultId());
            RequestContext ctx = new RequestContext("TOKENIZE", batchIndex, batches.size());
            if (interceptor != null) interceptor.intercept(ctx);
            CompletableFuture<BulkTokenizeResponse> future = CompletableFuture
                    .supplyAsync(() -> processTokenizeBatch(batch, ctx), executor)
                    .handle((result, ex) -> {
                        if (ex != null) {
                            return new BulkTokenizeResponse(Utils.handleBulkTokenizeBatchException(
                                    ex, batchRecords, startIndex));
                        }
                        return Utils.formatBulkTokenizeResponse(
                                result.body(), batchRecords, startIndex, result.headers());
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
            String userProvidedBatchSize = settingResolver.apply("TOKENIZE_BATCH_SIZE");
            String userProvidedConcurrencyLimit = settingResolver.apply("TOKENIZE_CONCURRENCY_LIMIT");

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
            String userProvidedBatchSize = settingResolver.apply("DETOKENIZE_BATCH_SIZE");
            String userProvidedConcurrencyLimit = settingResolver.apply("DETOKENIZE_CONCURRENCY_LIMIT");

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
            List<InsertRequestRecord> originalPayload,
            RequestInterceptor interceptor,
            BatchConfig cfg
    ) throws ExecutionException, InterruptedException, SkyflowException {
        LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());
        List<BulkInsertResponseRecord> records = new ArrayList<>();

        try {
            List<CompletableFuture<BulkInsertResponse>> futures = this.insertBatchFutures(insertRequest, interceptor, cfg);
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            try {
                allFutures.join();
            } catch (Exception e) {
                // individual batch errors are already captured
            }
            for (CompletableFuture<BulkInsertResponse> future : futures) {
                BulkInsertResponse futureResponse = future.get();
                if (futureResponse != null && futureResponse.getRecords() != null) {
                    records.addAll(futureResponse.getRecords());
                }
            }
        } catch (Exception e) {
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        }
        BulkInsertResponse response = new BulkInsertResponse(records, originalPayload);
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
        List<BulkDetokenizeResponseRecord> records = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(cfg.concurrencyLimit);
        List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest> batches =
                Utils.createBulkDetokenizeBatches(detokenizeRequest, cfg.batchSize);
        try {
            List<CompletableFuture<BulkDetokenizeResponse>> futures = this.detokenizeBatchFutures(executor, batches, interceptor, cfg.batchSize);
            try {
                CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                allFutures.join();
            } catch (Exception e) {
                // individual batch errors are already captured
            }
            for (CompletableFuture<BulkDetokenizeResponse> future : futures) {
                BulkDetokenizeResponse futureResponse = future.get();
                if (futureResponse != null && futureResponse.getRecords() != null) {
                    records.addAll(futureResponse.getRecords());
                }
            }
        } catch (Exception e) {
            LogUtil.printErrorLog(ErrorLogs.DETOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        } finally {
            executor.shutdown();
        }
        BulkDetokenizeResponse response = new BulkDetokenizeResponse(records, originalTokens);
        LogUtil.printInfoLog(InfoLogs.DETOKENIZE_REQUEST_RESOLVED.getLog());
        return response;
    }

    private List<CompletableFuture<BulkDetokenizeResponse>> detokenizeBatchFutures(
            ExecutorService executor,
            List<com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest> batches,
            RequestInterceptor interceptor,
            int batchSize) {
        List<CompletableFuture<BulkDetokenizeResponse>> futures = new ArrayList<>();
        for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
            com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDetokenizeRequest batch = batches.get(batchIndex);
            int batchNumber = batchIndex;
            RequestContext ctx = new RequestContext("DETOKENIZE", batchIndex, batches.size());
            if (interceptor != null) interceptor.intercept(ctx);
            CompletableFuture<BulkDetokenizeResponse> future = CompletableFuture
                    .supplyAsync(() -> processDetokenizeBatch(batch, ctx), executor)
                    .thenApply(response -> Utils.formatBulkDetokenizeResponse(response.body(), batchNumber, batchSize, response.headers()))
                    .exceptionally(ex -> new BulkDetokenizeResponse(
                            Utils.handleBulkDetokenizeBatchException(ex, batch, batchNumber, batchSize)));
            futures.add(future);
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
            RequestInterceptor interceptor,
            BatchConfig cfg) {
        List<V1InsertRecordData> records = insertRequest.getRecords().get();

        ExecutorService executor = Executors.newFixedThreadPool(cfg.concurrencyLimit);
        List<List<V1InsertRecordData>> batches = Utils.createBulkInsertBatches(records, cfg.batchSize);
        List<CompletableFuture<BulkInsertResponse>> futures = new ArrayList<>();

        try {
            for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
                List<V1InsertRecordData> batch = batches.get(batchIndex);
                int batchNumber = batchIndex;
                RequestContext ctx = new RequestContext("INSERT", batchIndex, batches.size());
                if (interceptor != null) interceptor.intercept(ctx);
                CompletableFuture<BulkInsertResponse> future = CompletableFuture
                        .supplyAsync(() -> insertBatch(
                                batch,
                                insertRequest.getTableName().isPresent() ? insertRequest.getTableName().get() : null,
                                insertRequest.getUpsert().isPresent() ? insertRequest.getUpsert().get() : null,
                                ctx), executor)
                        .thenApply(response -> Utils.formatBulkInsertResponse(response.body(), batchNumber, cfg.batchSize, response.headers()))
                        .exceptionally(ex -> new BulkInsertResponse(
                                Utils.handleBulkInsertBatchException(ex, batch, batchNumber, cfg.batchSize)));
                futures.add(future);
            }
        } finally {
            executor.shutdown();
        }
        return futures;
    }

    // tableName and upsert live on the envelope when the caller set them at the request level, and
    // batching rebuilds the envelope per batch — so both have to be re-applied here or they are
    // silently dropped for every batch after the body was built.
    private ApiClientHttpResponse<V1InsertResponse> insertBatch(List<V1InsertRecordData> batch, String tableName,
                                                                V1Upsert upsert, RequestContext ctx) {
        V1InsertRequest.Builder req = V1InsertRequest.builder()
                .vaultId(this.getVaultConfig().getVaultId())
                .records(batch);
        if (tableName != null && !tableName.isEmpty()) {
            req.tableName(tableName);
        }
        if (upsert != null) {
            req.upsert(upsert);
        }
        V1InsertRequest request = req.build();
        return this.getRecordsApi().withRawResponse().insert(request, buildRequestOptions(ctx));
    }

    private BatchConfig configureInsertConcurrencyAndBatchSize(int totalRequests) {
        int batchSize = Constants.INSERT_BATCH_SIZE;
        int concurrencyLimit;
        try {
            String userProvidedBatchSize = settingResolver.apply("INSERT_BATCH_SIZE");
            String userProvidedConcurrencyLimit = settingResolver.apply("INSERT_CONCURRENCY_LIMIT");

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
