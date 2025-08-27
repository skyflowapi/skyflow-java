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
    private int batchSize;
    private int concurrencyLimit;

    public VaultController(VaultConfig vaultConfig, Credentials credentials) {
        super(vaultConfig, credentials);
        this.batchSize = Constants.BATCH_SIZE;
        this.concurrencyLimit = Constants.CONCURRENCY_LIMIT;
    }

    public com.skyflow.vault.data.InsertResponse bulkInsert(InsertRequest insertRequest) throws SkyflowException {
        com.skyflow.vault.data.InsertResponse response;
        LogUtil.printInfoLog(InfoLogs.INSERT_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_INSERT_REQUEST.getLog());
            Validations.validateInsertRequest(insertRequest);
            setBearerToken();
            configureConcurrencyAndBatchSize(insertRequest.getValues().size());
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
//        Summary summary = new Summary();
//        summary.setTotalRecords(insertRequest.getValues().size());
//        if (response.getSuccessRecords() != null) {
//            summary.setTotalInserted(response.getSuccessRecords().size());
//        } else {
//            summary.setTotalInserted(0);
//        }
//        if (response.getErrorRecords() != null) {
//            summary.setTotalFailed(response.getErrorRecords().size());
//        } else {
//            summary.setTotalFailed(0);
//        }
//        response.setSummary(summary);
//        return response;
    }

    public CompletableFuture<com.skyflow.vault.data.InsertResponse> bulkInsertAsync(InsertRequest insertRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.INSERT_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_INSERT_REQUEST.getLog());
            Validations.validateInsertRequest(insertRequest);
            setBearerToken();
            configureConcurrencyAndBatchSize(insertRequest.getValues().size());
            com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest request = super.getBulkInsertRequestBody(insertRequest, super.getVaultConfig());

            List<ErrorRecord> errorRecords = new ArrayList<>();
            List<CompletableFuture<com.skyflow.vault.data.InsertResponse>> futures = this.insertBatchFutures(request, errorRecords);

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

                        return new com.skyflow.vault.data.InsertResponse(successRecords, errorRecords, insertRequest.getValues());
//                        Summary summary = new Summary();
//                        if (!successRecords.isEmpty()) {
//                            response.setSuccessRecords(successRecords);
//                            summary.setTotalInserted(successRecords.size());
//                        }
//                        if (!errorRecords.isEmpty()) {
//                            response.setErrorRecords(errorRecords);
//                            summary.setTotalFailed(errorRecords.size());
//                        }
//                        summary.setTotalRecords(insertRequest.getValues().size());
//                        response.setSummary(summary);
//                        return response;
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
//        if (!errorRecords.isEmpty()) {
//            response.setErrorRecords(errorRecords);
//        }
//        if (!successRecords.isEmpty()) {
//            response.setSuccessRecords(successRecords);
//        }
        LogUtil.printInfoLog(InfoLogs.INSERT_REQUEST_RESOLVED.getLog());
        return response;
    }


    private List<CompletableFuture<com.skyflow.vault.data.InsertResponse>> insertBatchFutures(
            com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest insertRequest, List<ErrorRecord> errorRecords
    ) {
        List<InsertRecordData> records = insertRequest.getRecords().get();

        ExecutorService executor = Executors.newFixedThreadPool(concurrencyLimit);
        List<List<InsertRecordData>> batches = Utils.createBatches(records, batchSize);
        List<CompletableFuture<com.skyflow.vault.data.InsertResponse>> futures = new ArrayList<>();

        try {
            for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
                List<InsertRecordData> batch = batches.get(batchIndex);
                int batchNumber = batchIndex;
                CompletableFuture<com.skyflow.vault.data.InsertResponse> future = CompletableFuture
                        .supplyAsync(() -> insertBatch(batch, insertRequest.getTableName().get()), executor)
                        .exceptionally(ex -> {
                            LogUtil.printInfoLog(ErrorLogs.PROCESSING_ERROR_RESPONSE.getLog());
                            errorRecords.addAll(handleBatchException(ex, batch, batchNumber, batches));
                            return null;
                        })
                        .thenApply(response -> formatResponse(response, batchNumber, batchSize));
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

    private void configureConcurrencyAndBatchSize(int totalRequests) {
        try {
            Dotenv dotenv = Dotenv.load();
            String userProvidedBatchSize = dotenv.get("BATCH_SIZE");
            String userProvidedConcurrencyLimit = dotenv.get("CONCURRENCY_LIMIT");

            if (userProvidedBatchSize != null) {
                try {
                    int batchSize = Integer.parseInt(userProvidedBatchSize);
                    if (batchSize > 0) {
                        this.batchSize = batchSize;
                    } else {
                        LogUtil.printWarningLog(WarningLogs.INVALID_BATCH_SIZE_PROVIDED.getLog());
                        this.batchSize = Constants.BATCH_SIZE;
                    }
                } catch (NumberFormatException e) {
                    LogUtil.printWarningLog(WarningLogs.INVALID_BATCH_SIZE_PROVIDED.getLog());
                    this.batchSize = Constants.BATCH_SIZE;
                }
            }

            // Max no of threads required to run all batches concurrently at once
            int maxConcurrencyNeeded = (totalRequests + this.batchSize - 1) / this.batchSize;

            if (userProvidedConcurrencyLimit != null) {
                try {
                    int concurrencyLimit = Integer.parseInt(userProvidedConcurrencyLimit);
                    if (concurrencyLimit > 0) {
                        this.concurrencyLimit = Math.min(concurrencyLimit, maxConcurrencyNeeded);
                    } else {
                        LogUtil.printWarningLog(WarningLogs.INVALID_CONCURRENCY_LIMIT_PROVIDED.getLog());
                        this.concurrencyLimit = Math.min(Constants.CONCURRENCY_LIMIT, maxConcurrencyNeeded);
                    }
                } catch (NumberFormatException e) {
                    LogUtil.printWarningLog(WarningLogs.INVALID_CONCURRENCY_LIMIT_PROVIDED.getLog());
                    this.concurrencyLimit = Math.min(Constants.CONCURRENCY_LIMIT, maxConcurrencyNeeded);
                }
            } else {
                this.concurrencyLimit = Math.min(Constants.CONCURRENCY_LIMIT, maxConcurrencyNeeded);
            }
        } catch (Exception e) {
            this.batchSize = Constants.BATCH_SIZE;
            int maxConcurrencyNeeded = (totalRequests + this.batchSize - 1) / this.batchSize;
            this.concurrencyLimit = Math.min(Constants.CONCURRENCY_LIMIT, maxConcurrencyNeeded);
        }
    }

}
