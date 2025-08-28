package com.skyflow.vault.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skyflow.VaultClient;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.types.InsertRecordData;
import com.skyflow.generated.rest.types.InsertResponse;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.logs.WarningLogs;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.ErrorRecord;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.Success;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.skyflow.utils.Utils.formatResponse;
import static com.skyflow.utils.Utils.handleBatchException;

public final class VaultController extends VaultClient {
    private static final Gson gson = new GsonBuilder().serializeNulls().create();
    private int insertBatchSize;
    private int insertConcurrencyLimit;
    private int detokenizeBatchSize;
    private int detokenizeConcurrencyLimit;

    public VaultController(VaultConfig vaultConfig, Credentials credentials) {
        super(vaultConfig, credentials);
        this.insertBatchSize = Constants.INSERT_BATCH_SIZE;
        this.insertConcurrencyLimit = Constants.INSERT_CONCURRENCY_LIMIT;
        this.detokenizeBatchSize = Constants.DETOKENIZE_BATCH_SIZE;
        this.detokenizeConcurrencyLimit = Constants.DETOKENIZE_CONCURRENCY_LIMIT;
    }

    public com.skyflow.vault.data.InsertResponse bulkInsert(InsertRequest insertRequest) throws SkyflowException {
        com.skyflow.vault.data.InsertResponse response;
        LogUtil.printInfoLog(InfoLogs.INSERT_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_INSERT_REQUEST.getLog());
            Validations.validateInsertRequest(insertRequest);
            setBearerToken();
            configureInsertConcurrencyAndBatchSize(insertRequest.getValues().size());
            com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest request = super.getBulkInsertRequestBody(insertRequest, super.getVaultConfig());

            response = this.processSync(request, insertRequest.getValues());
            return response;
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ExecutionException | InterruptedException e) {
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        }
    }

    public CompletableFuture<com.skyflow.vault.data.InsertResponse> bulkInsertAsync(InsertRequest insertRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.INSERT_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_INSERT_REQUEST.getLog());
            Validations.validateInsertRequest(insertRequest);
            setBearerToken();
            configureInsertConcurrencyAndBatchSize(insertRequest.getValues().size());
            com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest request = super.getBulkInsertRequestBody(insertRequest, super.getVaultConfig());

            List<CompletableFuture<com.skyflow.vault.data.InsertResponse>> futures = this.insertBatchFutures(request);

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        List<Success> successRecords = new ArrayList<>();
                        List<ErrorRecord> errorRecords = new ArrayList<>();

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

                        return new com.skyflow.vault.data.InsertResponse(successRecords, errorRecords, insertRequest.getValues());
                    });
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        }
    }

    private com.skyflow.vault.data.InsertResponse processSync(
            com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest insertRequest,
            ArrayList<HashMap<String, Object>> originalPayload
    ) throws ExecutionException, InterruptedException {
        LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());
        List<ErrorRecord> errorRecords = new ArrayList<>();
        List<Success> successRecords = new ArrayList<>();

        List<CompletableFuture<com.skyflow.vault.data.InsertResponse>> futures = this.insertBatchFutures(insertRequest);

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


    private List<CompletableFuture<com.skyflow.vault.data.InsertResponse>> insertBatchFutures(
            com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest insertRequest
    ) {
        List<InsertRecordData> records = insertRequest.getRecords().get();

        ExecutorService executor = Executors.newFixedThreadPool(insertConcurrencyLimit);
        List<List<InsertRecordData>> batches = Utils.createBatches(records, insertBatchSize);
        List<CompletableFuture<com.skyflow.vault.data.InsertResponse>> futures = new ArrayList<>();

        try {
            for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
                List<InsertRecordData> batch = batches.get(batchIndex);
                int batchNumber = batchIndex;
                CompletableFuture<com.skyflow.vault.data.InsertResponse> future = CompletableFuture
                        .supplyAsync(() -> insertBatch(batch, insertRequest.getTableName().get()), executor)
                        .thenApply(response -> formatResponse(response, batchNumber, insertBatchSize))
                        .exceptionally(ex -> new com.skyflow.vault.data.InsertResponse(null, handleBatchException(ex, batch, batchNumber, batches)));
                futures.add(future);
            }
        } finally {
            executor.shutdown();
        }
        return futures;
    }

    private InsertResponse insertBatch(List<InsertRecordData> batch, String tableName) {
        com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest req = com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest.builder()
                .vaultId(this.getVaultConfig().getVaultId())
                .tableName(tableName)
                .records(batch)
                .build();
        return this.getRecordsApi().insert(req);
    }

    private void configureInsertConcurrencyAndBatchSize(int totalRequests) {
        try {
            Dotenv dotenv = Dotenv.load();
            String userProvidedBatchSize = dotenv.get("INSERT_BATCH_SIZE");
            String userProvidedConcurrencyLimit = dotenv.get("INSERT_CONCURRENCY_LIMIT");

            if (userProvidedBatchSize != null) {
                try {
                    int batchSize = Integer.parseInt(userProvidedBatchSize);
                    int maxBatchSize = Math.min(batchSize, Constants.MAX_INSERT_BATCH_SIZE);
                    if (maxBatchSize > 0) {
                        this.insertBatchSize = batchSize;
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
}
