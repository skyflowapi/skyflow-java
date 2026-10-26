package com.skyflow.vault.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.skyflow.generated.rest.resources.files.requests.FileDeleteRequest;
import com.skyflow.generated.rest.resources.files.requests.FileUploadRequest;
import com.skyflow.generated.rest.resources.query.requests.ExecuteQueryRequest;
import com.skyflow.generated.rest.resources.tokens.requests.DeleteTokenRequest;
import com.skyflow.generated.rest.resources.tokens.requests.GetTokensFromValuesRequest;
import com.skyflow.generated.rest.resources.tokens.requests.TokenizeRequest;
import com.skyflow.generated.rest.types.DeleteTokenResponse;
import com.skyflow.generated.rest.types.ExecuteQueryResponse;
import com.skyflow.generated.rest.types.FileDeleteResponse;
import com.skyflow.generated.rest.types.FileUploadResponse;
import com.skyflow.generated.rest.types.FileUploadResponseObject;
import com.skyflow.generated.rest.types.GetTokensFromValuesResponse;
import com.skyflow.generated.rest.types.InsertRecordData;
import com.skyflow.generated.rest.types.TokenizeResponse;
import com.skyflow.generated.rest.types.Upsert;
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
import com.skyflow.vault.data.RequestContext;
import com.skyflow.vault.data.UpdateOptions;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateResponse;
import com.skyflow.vault.data.DeleteFilesOptions;
import com.skyflow.vault.data.DeleteFilesRequest;
import com.skyflow.vault.data.DeleteFilesResponse;
import com.skyflow.vault.data.DeleteOptions;
import com.skyflow.vault.data.DeleteRequest;
import com.skyflow.vault.data.DeleteResponse;
import com.skyflow.vault.data.GetOptions;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;
import com.skyflow.vault.data.GetTokensOptions;
import com.skyflow.vault.data.GetTokensRequest;
import com.skyflow.vault.data.GetTokensResponse;
import com.skyflow.vault.data.QueryOptions;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.QueryResponse;
import com.skyflow.vault.data.RequestInterceptor;
import com.skyflow.vault.data.UploadFilesOptions;
import com.skyflow.vault.data.UploadFilesRequest;
import com.skyflow.vault.data.UploadFilesRequestColumn;
import com.skyflow.vault.data.UploadFilesRequestRecord;
import com.skyflow.vault.data.UploadFilesResponse;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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
            com.skyflow.generated.rest.resources.records.requests.InsertRequest request = Utils.getInsertRequestBody(insertRequest, this.getVaultConfig());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            RequestContext ctx = new RequestContext("INSERT", 0, 1);
            if (interceptor != null) interceptor.intercept(ctx);

            ApiClientHttpResponse<com.skyflow.generated.rest.types.InsertResponse> response =
                    this.getRecordsApi().withRawResponse().insertRecords(request, buildRequestOptions(ctx));

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
            String bodyString = gson.toJson(Utils.errorBody(e));
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
            com.skyflow.generated.rest.resources.records.requests.InsertRequest request = Utils.getBulkInsertRequestBody(insertRequest, this.getVaultConfig());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            return this.processBulkInsertSync(request, insertRequest.getRecords(), interceptor, cfg);
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(Utils.errorBody(e));
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
            com.skyflow.generated.rest.resources.records.requests.InsertRequest request = Utils.getBulkInsertRequestBody(insertRequest, this.getVaultConfig());
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
            String bodyString = gson.toJson(Utils.errorBody(e));
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
            com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest request =
                    Utils.getDetokenizeRequestBody(detokenizeRequest, this.getVaultConfig().getVaultId());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            RequestContext ctx = new RequestContext("DETOKENIZE", 0, 1);
            if (interceptor != null) interceptor.intercept(ctx);

            ApiClientHttpResponse<com.skyflow.generated.rest.types.DetokenizeResponse> response =
                    this.getTokensApi().withRawResponse().detokenize(request, buildRequestOptions(ctx));

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
            String bodyString = gson.toJson(Utils.errorBody(e));
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
            com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest request =
                    Utils.getBulkDetokenizeRequestBody(detokenizeRequest, this.getVaultConfig().getVaultId());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            return this.processBulkDetokenizeSync(request, detokenizeRequest.getTokens(), interceptor, cfg);
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(Utils.errorBody(e));
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
            com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest request =
                    Utils.getBulkDetokenizeRequestBody(detokenizeRequest, this.getVaultConfig().getVaultId());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;

            LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());

            List<BulkDetokenizeResponseRecord> records = new ArrayList<>();

            List<com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest> batches =
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
            String bodyString = gson.toJson(Utils.errorBody(e));
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
            com.skyflow.generated.rest.resources.records.requests.DeleteRequest request = Utils.getDeleteRequestBody(deleteRequest, this.getVaultConfig());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            RequestContext ctx = new RequestContext("DELETE", 0, 1);
            if (interceptor != null) interceptor.intercept(ctx);

            ApiClientHttpResponse<com.skyflow.generated.rest.types.DeleteResponse> response =
                    this.getRecordsApi().withRawResponse().deleteRecords(request, buildRequestOptions(ctx));

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
            String bodyString = gson.toJson(Utils.errorBody(e));
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
            DeleteTokenRequest request =
                    Utils.getBulkDeleteTokensRequestBody(deleteTokensRequest, this.getVaultConfig().getVaultId());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            return this.processBulkDeleteTokensSync(request, deleteTokensRequest.getTokens(), interceptor, cfg);
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(Utils.errorBody(e));
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
            DeleteTokenRequest request =
                    Utils.getBulkDeleteTokensRequestBody(deleteTokensRequest, this.getVaultConfig().getVaultId());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;

            LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());

            List<BulkDeleteTokensResponseRecord> responseRecords = Collections.synchronizedList(new ArrayList<>());

            List<DeleteTokenRequest> batches =
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
            String bodyString = gson.toJson(Utils.errorBody(e));
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
            String bodyString = gson.toJson(Utils.errorBody(e));
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
            String bodyString = gson.toJson(Utils.errorBody(e));
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
            com.skyflow.generated.rest.resources.records.requests.UpdateRequest request = Utils.getUpdateRequestBody(updateRequest, this.getVaultConfig());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            RequestContext ctx = new RequestContext("UPDATE", 0, 1);
            if (interceptor != null) interceptor.intercept(ctx);

            ApiClientHttpResponse<com.skyflow.generated.rest.types.UpdateResponse> response =
                    this.getRecordsApi().withRawResponse().updateRecords(request, buildRequestOptions(ctx));

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
            String bodyString = gson.toJson(Utils.errorBody(e));
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
            com.skyflow.generated.rest.resources.records.requests.GetRequest request = Utils.getGetRequestBody(getRequest, this.getVaultConfig());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            RequestContext ctx = new RequestContext("GET", 0, 1);
            if (interceptor != null) interceptor.intercept(ctx);

            ApiClientHttpResponse<com.skyflow.generated.rest.types.GetResponse> response =
                    this.getRecordsApi().withRawResponse().getRecords(request, buildRequestOptions(ctx));

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
            String bodyString = gson.toJson(Utils.errorBody(e));
            LogUtil.printErrorLog(ErrorLogs.GET_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.GET_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e);
        }
    }

    // ── Query ─────────────────────────────────────────────────────────────────
    // Runs a SQL SELECT in a single API call. There is no per-record status, so any failure is
    // thrown rather than returned on the response.

    public QueryResponse query(QueryRequest queryRequest) throws SkyflowException {
        return query(queryRequest, null);
    }

    public QueryResponse query(QueryRequest queryRequest, QueryOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.QUERY_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_QUERY_REQUEST.getLog());
            Validations.validateQueryRequest(queryRequest);

            setBearerToken();
            ExecuteQueryRequest request = Utils.getQueryRequestBody(queryRequest, this.getVaultConfig().getVaultId());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            RequestContext ctx = new RequestContext("QUERY", 0, 1);
            if (interceptor != null) interceptor.intercept(ctx);

            ApiClientHttpResponse<ExecuteQueryResponse> response =
                    this.getQueryApi().withRawResponse().executeQuery(request, buildRequestOptions(ctx));

            QueryResponse formattedResponse = Utils.formatQueryResponse(response.body(), response.headers());
            LogUtil.printInfoLog(InfoLogs.QUERY_REQUEST_RESOLVED.getLog());
            return formattedResponse;
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(Utils.errorBody(e));
            LogUtil.printErrorLog(ErrorLogs.QUERY_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.QUERY_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e);
        }
    }

    // ── Get Tokens ────────────────────────────────────────────────────────────
    // Looks up the existing deterministic token for each value in a single API call. Record-level
    // failures come back on the response (200/207); whole-call failures are thrown.

    public GetTokensResponse getTokens(GetTokensRequest getTokensRequest) throws SkyflowException {
        return getTokens(getTokensRequest, null);
    }

    public GetTokensResponse getTokens(GetTokensRequest getTokensRequest, GetTokensOptions options) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.GET_TOKENS_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_GET_TOKENS_REQUEST.getLog());
            Validations.validateGetTokensRequest(getTokensRequest);

            setBearerToken();
            GetTokensFromValuesRequest request = Utils.getGetTokensRequestBody(getTokensRequest, this.getVaultConfig().getVaultId());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            RequestContext ctx = new RequestContext("GET_TOKENS", 0, 1);
            if (interceptor != null) interceptor.intercept(ctx);

            ApiClientHttpResponse<GetTokensFromValuesResponse> response =
                    this.getTokensApi().withRawResponse().getTokens(request, buildRequestOptions(ctx));

            GetTokensResponse formattedResponse = Utils.formatGetTokensResponse(response.body(), response.headers());
            LogUtil.printInfoLog(InfoLogs.GET_TOKENS_REQUEST_RESOLVED.getLog());
            return formattedResponse;
        } catch (ApiClientApiException e) {
            // The lone record in a unary request can fail outright, which the vault reflects as
            // the overall HTTP status. If the body still carries the usual per-record shape,
            // surface it on the response like a 200 partial success would, not as an exception.
            GetTokensResponse fallback = Utils.handleGetTokensRequestException(e);
            if (fallback != null) {
                return fallback;
            }
            String bodyString = gson.toJson(Utils.errorBody(e));
            LogUtil.printErrorLog(ErrorLogs.GET_TOKENS_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.GET_TOKENS_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e);
        }
    }

    // ── Upload Files ──────────────────────────────────────────────────────────
    // Two steps inside one unary call. Phase A asks the vault for a signed upload URL per column;
    // Phase B PUTs each file's bytes to its URL, a few at a time. Any Phase A error is thrown; a
    // record that fails in Phase A has its columns SKIPPED; a column whose PUT fails is FAILED and
    // leaves every other column and record unaffected. Signed URLs never reach the caller.

    public UploadFilesResponse uploadFiles(UploadFilesRequest uploadFilesRequest) throws SkyflowException {
        return uploadFiles(uploadFilesRequest, null);
    }

    public UploadFilesResponse uploadFiles(UploadFilesRequest uploadFilesRequest, UploadFilesOptions options)
            throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.UPLOAD_FILES_TRIGGERED.getLog());
        ApiClientHttpResponse<FileUploadResponse> response;
        Map<UploadFilesRequestColumn, byte[]> decodedBase64;
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_UPLOAD_FILES_REQUEST.getLog());
            Validations.validateUploadFilesRequest(uploadFilesRequest);
            decodedBase64 = Utils.decodeBase64Columns(uploadFilesRequest);

            setBearerToken();
            FileUploadRequest request = Utils.getUploadFilesRequestBody(uploadFilesRequest, this.getVaultConfig().getVaultId());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            RequestContext ctx = new RequestContext("UPLOAD_FILES", 0, 1);
            if (interceptor != null) interceptor.intercept(ctx);

            response = this.getFilesApi().withRawResponse().uploadFiles(request, buildRequestOptions(ctx));
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(Utils.uploadFilesErrorBody(e));
            LogUtil.printErrorLog(ErrorLogs.UPLOAD_FILES_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.UPLOAD_FILES_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e);
        }

        LogUtil.printInfoLog(InfoLogs.UPLOADING_FILES_TO_SIGNED_URLS.getLog());
        UploadFilesResponse formattedResponse = uploadToSignedUrls(
                uploadFilesRequest.getRecords(), decodedBase64, response.body(), response.headers());
        LogUtil.printInfoLog(InfoLogs.UPLOAD_FILES_REQUEST_RESOLVED.getLog());
        return formattedResponse;
    }

    private UploadFilesResponse uploadToSignedUrls(List<UploadFilesRequestRecord> requested,
                                                   Map<UploadFilesRequestColumn, byte[]> decodedBase64,
                                                   FileUploadResponse body, Map<String, List<String>> headers) {
        List<FileUploadResponseObject> returned = body != null && body.getRecords() != null
                ? body.getRecords() : Collections.emptyList();
        // All signed URLs are issued together and expire together, so the PUTs run on a small pool
        // rather than one after another. Each record keeps its column futures in request order, so
        // the response does not depend on which upload finishes first.
        int fileCount = 0;
        for (UploadFilesRequestRecord requestRecord : requested) {
            fileCount += requestRecord.getColumns().size();
        }
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(fileCount, Constants.UPLOAD_FILES_CONCURRENCY_LIMIT));
        try {
            String[] errors = new String[returned.size()];
            List<List<CompletableFuture<HashMap<String, Object>>>> uploads = new ArrayList<>();
            for (int i = 0; i < returned.size(); i++) {
                FileUploadResponseObject record = returned.get(i);
                errors[i] = record.getError().filter(e -> !e.isEmpty()).orElse(null);
                if (errors[i] == null) {
                    uploads.add(uploadColumns(i < requested.size() ? requested.get(i) : null, decodedBase64,
                            record.getData().orElse(Collections.emptyMap()), executor));
                } else {
                    uploads.add(Collections.emptyList());
                }
            }

            ArrayList<HashMap<String, Object>> records = new ArrayList<>();
            for (int i = 0; i < returned.size(); i++) {
                FileUploadResponseObject record = returned.get(i);
                String error = errors[i];
                int httpCode = record.getHttpCode() != 0 ? record.getHttpCode() : (error != null ? 500 : 200);
                List<HashMap<String, Object>> columns;
                if (error != null) {
                    columns = Utils.skippedUploadColumns(i < requested.size() ? requested.get(i) : null, error);
                } else {
                    columns = new ArrayList<>();
                    for (CompletableFuture<HashMap<String, Object>> column : uploads.get(i)) {
                        columns.add(column.join());
                    }
                }
                records.add(Utils.uploadFilesRecordRow(record.getSkyflowId(), record.getTableName(), columns,
                        httpCode, error, error != null ? Utils.extractRequestId(headers) : null));
            }
            return new UploadFilesResponse(records);
        } finally {
            executor.shutdown();
        }
    }

    private List<CompletableFuture<HashMap<String, Object>>> uploadColumns(UploadFilesRequestRecord requestRecord,
                                                                           Map<UploadFilesRequestColumn, byte[]> decodedBase64,
                                                                           Map<String, Object> signedUrls,
                                                                           ExecutorService executor) {
        List<CompletableFuture<HashMap<String, Object>>> columns = new ArrayList<>();
        if (requestRecord == null) {
            return columns;
        }
        for (UploadFilesRequestColumn column : requestRecord.getColumns()) {
            Object signedUrl = signedUrls.get(column.getColumn());
            if (signedUrl == null) {
                columns.add(CompletableFuture.completedFuture(Utils.uploadFilesColumnRow(
                        column.getColumn(), Utils.resolveUploadFileName(column), Utils.UPLOAD_STATUS_SKIPPED, null)));
                continue;
            }
            columns.add(CompletableFuture.supplyAsync(
                    () -> uploadColumn(column, decodedBase64.get(column), signedUrl.toString()), executor));
        }
        return columns;
    }

    /** PUTs one column's file; a failure is reported on that column only. */
    private HashMap<String, Object> uploadColumn(UploadFilesRequestColumn column, byte[] decodedBase64,
                                                 String signedUrl) {
        String fileName = Utils.resolveUploadFileName(column);
        String error = null;
        try {
            RequestBody fileBody = Utils.buildUploadFileBody(
                    column, decodedBase64, Utils.resolveUploadContentType(fileName));
            int status = signedUrlUploader.upload(signedUrl, fileBody);
            if (status < 200 || status >= 300) {
                error = "PUT failed: " + status;
            }
        } catch (IOException | RuntimeException e) {
            error = "PUT failed: " + (e.getMessage() != null ? e.getMessage() : e.toString());
        }
        if (error != null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.SIGNED_URL_UPLOAD_FAILED.getLog(), column.getColumn()));
        }
        return Utils.uploadFilesColumnRow(column.getColumn(), fileName,
                error == null ? Utils.UPLOAD_STATUS_UPLOADED : Utils.UPLOAD_STATUS_FAILED, error);
    }

    /** Sends one file to its signed URL and returns the HTTP status. */
    interface SignedUrlUploader {
        int upload(String signedUrl, RequestBody fileBody) throws IOException;
    }

    /**
     * Package-private and swappable purely so tests can stand in for the storage endpoint, like
     * {@link #settingResolver}; deliberately not public.
     */
    SignedUrlUploader signedUrlUploader = this::putToSignedUrl;

    private int putToSignedUrl(String signedUrl, RequestBody fileBody) throws IOException {
        Request request = new Request.Builder().url(signedUrl).put(fileBody).build();
        try (Response response = getSignedUrlHttpClient().newCall(request).execute()) {
            return response.code();
        }
    }

    // ── Delete Files ──────────────────────────────────────────────────────────
    // Deletes the files in file columns of existing records, in a single API call. Record-level
    // failures come back on the response; whole-call failures are thrown.

    public DeleteFilesResponse deleteFiles(DeleteFilesRequest deleteFilesRequest) throws SkyflowException {
        return deleteFiles(deleteFilesRequest, null);
    }

    public DeleteFilesResponse deleteFiles(DeleteFilesRequest deleteFilesRequest, DeleteFilesOptions options)
            throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DELETE_FILES_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATING_DELETE_FILES_REQUEST.getLog());
            Validations.validateDeleteFilesRequest(deleteFilesRequest);

            setBearerToken();
            FileDeleteRequest request = Utils.getDeleteFilesRequestBody(deleteFilesRequest, this.getVaultConfig().getVaultId());
            RequestInterceptor interceptor = options != null ? options.getInterceptor() : null;
            RequestContext ctx = new RequestContext("DELETE_FILES", 0, 1);
            if (interceptor != null) interceptor.intercept(ctx);

            ApiClientHttpResponse<FileDeleteResponse> response =
                    this.getFilesApi().withRawResponse().deleteFiles(request, buildRequestOptions(ctx));

            DeleteFilesResponse formattedResponse = Utils.formatDeleteFilesResponse(response.body(), response.headers());
            LogUtil.printInfoLog(InfoLogs.DELETE_FILES_REQUEST_RESOLVED.getLog());
            return formattedResponse;
        } catch (ApiClientApiException e) {
            // The lone record in a unary request can fail outright, which the vault reflects as
            // the overall HTTP status. If the body still carries the usual per-record shape,
            // surface it on the response like a 200 partial success would, not as an exception.
            DeleteFilesResponse fallback = Utils.handleDeleteFilesRequestException(e);
            if (fallback != null) {
                return fallback;
            }
            String bodyString = gson.toJson(Utils.errorBody(e));
            LogUtil.printErrorLog(ErrorLogs.DELETE_FILES_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ApiClientException e) {
            LogUtil.printErrorLog(ErrorLogs.DELETE_FILES_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e);
        }
    }

    // ── Bulk private helpers ──────────────────────────────────────────────────

    private BulkDeleteTokensResponse processBulkDeleteTokensSync(
            DeleteTokenRequest deleteTokensRequest,
            List<String> originalTokens,
            RequestInterceptor interceptor,
            BatchConfig cfg
    ) throws ExecutionException, InterruptedException, SkyflowException {
        LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());
        List<BulkDeleteTokensResponseRecord> responseRecords = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(cfg.concurrencyLimit);
        List<DeleteTokenRequest> batches =
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
            List<DeleteTokenRequest> batches,
            RequestInterceptor interceptor,
            int batchSize) {
        List<CompletableFuture<BulkDeleteTokensResponse>> futures = new ArrayList<>();
        if (batches == null) return futures;
        for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
            final int index = batchIndex;
            DeleteTokenRequest batch = batches.get(index);
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

    private ApiClientHttpResponse<DeleteTokenResponse> processDeleteTokensBatch(
            DeleteTokenRequest batch,
            RequestContext ctx) {
        return this.getTokensApi().withRawResponse().deleteToken(batch, buildRequestOptions(ctx));
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
            TokenizeRequest batch =
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

    private ApiClientHttpResponse<TokenizeResponse> processTokenizeBatch(
            TokenizeRequest batch,
            RequestContext ctx) {
        return this.getTokensApi().withRawResponse().tokenize(batch, buildRequestOptions(ctx));
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
            com.skyflow.generated.rest.resources.records.requests.InsertRequest insertRequest,
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
            com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest detokenizeRequest,
            List<String> originalTokens,
            RequestInterceptor interceptor,
            BatchConfig cfg
    ) throws ExecutionException, InterruptedException, SkyflowException {
        LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());
        List<BulkDetokenizeResponseRecord> records = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(cfg.concurrencyLimit);
        List<com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest> batches =
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
            List<com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest> batches,
            RequestInterceptor interceptor,
            int batchSize) {
        List<CompletableFuture<BulkDetokenizeResponse>> futures = new ArrayList<>();
        for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
            com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest batch = batches.get(batchIndex);
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

    private ApiClientHttpResponse<com.skyflow.generated.rest.types.DetokenizeResponse> processDetokenizeBatch(
            com.skyflow.generated.rest.resources.tokens.requests.DetokenizeRequest batch,
            RequestContext ctx) {
        return this.getTokensApi().withRawResponse().detokenize(batch, buildRequestOptions(ctx));
    }

    private List<CompletableFuture<BulkInsertResponse>> insertBatchFutures(
            com.skyflow.generated.rest.resources.records.requests.InsertRequest insertRequest,
            RequestInterceptor interceptor,
            BatchConfig cfg) {
        List<InsertRecordData> records = insertRequest.getRecords();

        ExecutorService executor = Executors.newFixedThreadPool(cfg.concurrencyLimit);
        List<List<InsertRecordData>> batches = Utils.createBulkInsertBatches(records, cfg.batchSize);
        List<CompletableFuture<BulkInsertResponse>> futures = new ArrayList<>();

        try {
            for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
                List<InsertRecordData> batch = batches.get(batchIndex);
                int batchNumber = batchIndex;
                RequestContext ctx = new RequestContext("INSERT", batchIndex, batches.size());
                if (interceptor != null) interceptor.intercept(ctx);
                CompletableFuture<BulkInsertResponse> future = CompletableFuture
                        .supplyAsync(() -> insertBatch(
                                batch,
                                insertRequest.getTableName(),
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
    private ApiClientHttpResponse<com.skyflow.generated.rest.types.InsertResponse> insertBatch(List<InsertRecordData> batch, String tableName,
                                                                Upsert upsert, RequestContext ctx) {
        com.skyflow.generated.rest.resources.records.requests.InsertRequest request =
                Utils.buildInsertRequest(this.getVaultConfig().getVaultId(), tableName, batch, upsert);
        return this.getRecordsApi().withRawResponse().insertRecords(request, buildRequestOptions(ctx));
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
