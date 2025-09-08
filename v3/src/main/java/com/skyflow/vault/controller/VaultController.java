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
import com.skyflow.vault.data.*;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

import static com.skyflow.utils.Utils.*;

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
            configureInsertConcurrencyAndBatchSize(insertRequest.getValues().size());

            setBearerToken();
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
            configureInsertConcurrencyAndBatchSize(insertRequest.getValues().size());

            setBearerToken();
            com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest request = super.getBulkInsertRequestBody(insertRequest, super.getVaultConfig());
            List<ErrorRecord> errorRecords = Collections.synchronizedList(new ArrayList<>());
            List<CompletableFuture<com.skyflow.vault.data.InsertResponse>> futures = this.insertBatchFutures(request, errorRecords);

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        List<Success> successRecords = new ArrayList<>();
//                        List<ErrorRecord> errorRecords = new ArrayList<>();

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

    public DetokenizeResponse bulkDetokenize(DetokenizeRequest detokenizeRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DETOKENIZE_TRIGGERED.getLog());
        try {
            DetokenizeResponse response;
            configureDetokenizeConcurrencyAndBatchSize(detokenizeRequest.getTokens().size());
            LogUtil.printInfoLog(InfoLogs.VALIDATE_DETOKENIZE_REQUEST.getLog());
            Validations.validateDetokenizeRequest(detokenizeRequest);
            setBearerToken();
            com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest request = super.getDetokenizeRequestBody(detokenizeRequest);

            response = this.processDetokenizeSync(request, detokenizeRequest.getTokens());
            return response;
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.DETOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ExecutionException | InterruptedException e) {
            LogUtil.printErrorLog(ErrorLogs.DETOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        }
    }

    public CompletableFuture<DetokenizeResponse> bulkDetokenizeAsync(DetokenizeRequest detokenizeRequest) throws SkyflowException{
        LogUtil.printInfoLog(InfoLogs.DETOKENIZE_TRIGGERED.getLog());
        ExecutorService executor = Executors.newFixedThreadPool(detokenizeConcurrencyLimit);
        try {
            configureDetokenizeConcurrencyAndBatchSize(detokenizeRequest.getTokens().size());
            LogUtil.printInfoLog(InfoLogs.VALIDATE_DETOKENIZE_REQUEST.getLog());
            Validations.validateDetokenizeRequest(detokenizeRequest);
            setBearerToken();
            com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest request = super.getDetokenizeRequestBody(detokenizeRequest);

            LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());

            List<ErrorRecord> errorTokens = Collections.synchronizedList(new ArrayList<>());
            List<DetokenizeResponseObject> successRecords = new ArrayList<>();

            // Create batches
            List<com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest> batches = Utils.createDetokenizeBatches(request, detokenizeBatchSize);

            List<CompletableFuture<DetokenizeResponse>> futures = this.detokenizeBatchFutures(executor, batches, errorTokens);
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
        } catch (Exception e){
            LogUtil.printErrorLog(ErrorLogs.DETOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        } finally {
            executor.shutdown();
        }
    }
    private com.skyflow.vault.data.InsertResponse processSync(
            com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest insertRequest,
            ArrayList<HashMap<String, Object>> originalPayload
    ) throws ExecutionException, InterruptedException {
        LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());
        List<Success> successRecords = new ArrayList<>();
        List<ErrorRecord> errorRecords = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<com.skyflow.vault.data.InsertResponse>> futures = this.insertBatchFutures(insertRequest, errorRecords);

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
            com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest detokenizeRequest,
            List<String> originalTokens
    ) throws ExecutionException, InterruptedException, SkyflowException {
        LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());
        List<ErrorRecord> errorTokens = Collections.synchronizedList(new ArrayList<>());
        List<DetokenizeResponseObject> successTokens = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(detokenizeConcurrencyLimit);
        List<com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest> batches = Utils.createDetokenizeBatches(detokenizeRequest, detokenizeBatchSize);
        try {
            List<CompletableFuture<DetokenizeResponse>> futures = this.detokenizeBatchFutures(executor, batches, errorTokens);
            try{

                CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                allFutures.join();
            } catch (Exception e){
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
        } catch (Exception e){
            LogUtil.printErrorLog(ErrorLogs.DETOKENIZE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        } finally {
            executor.shutdown();
        }
        DetokenizeResponse response = new DetokenizeResponse(successTokens, errorTokens, originalTokens);
        LogUtil.printInfoLog(InfoLogs.DETOKENIZE_REQUEST_RESOLVED.getLog());
        return response;
    }

    private List<CompletableFuture<DetokenizeResponse>> detokenizeBatchFutures(ExecutorService executor, List<com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest> batches, List<ErrorRecord> errorTokens) {
        List<CompletableFuture<DetokenizeResponse>> futures = new ArrayList<>();
        try {

            for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
               com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest batch = batches.get(batchIndex);
                int batchNumber = batchIndex;
                CompletableFuture<DetokenizeResponse> future = CompletableFuture
                        .supplyAsync(() -> processDetokenizeBatch(batch), executor)
                        .thenApply(response -> formatDetokenizeResponse(response, batchNumber, detokenizeBatchSize))
                        .exceptionally(ex -> {
                            errorTokens.addAll(handleDetokenizeBatchException(ex, batch, batchNumber, detokenizeBatchSize));
                            return null;
                        });
                futures.add(future);
            }
        } catch (Exception e){
            ErrorRecord errorRecord = new ErrorRecord(0, e.getMessage(), 500);
            errorTokens.add(errorRecord);
        }
        return futures;
    }
    private com.skyflow.generated.rest.types.DetokenizeResponse processDetokenizeBatch(com.skyflow.generated.rest.resources.recordservice.requests.DetokenizeRequest batch) {
        return this.getRecordsApi().detokenize(batch);
    }

    private List<CompletableFuture<com.skyflow.vault.data.InsertResponse>>
    insertBatchFutures(
            com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest insertRequest,
            List<ErrorRecord> errorRecords) {
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
                        .exceptionally(ex -> {
                            errorRecords.addAll(handleBatchException(ex, batch, batchNumber));
                            return null;
                        });
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
            Dotenv dotenv = Dotenv.load();
            String userProvidedBatchSize = dotenv.get("DETOKENIZE_BATCH_SIZE");
            String userProvidedConcurrencyLimit = dotenv.get("DETOKENIZE_CONCURRENCY_LIMIT");

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