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
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.ErrorRecord;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.Success;
import com.skyflow.vault.data.Summary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.skyflow.utils.Utils.formatResponse;
import static com.skyflow.utils.Utils.handleBatchException;

public final class VaultController extends VaultClient {
    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    public VaultController(VaultConfig vaultConfig, Credentials credentials) {
        super(vaultConfig, credentials);
    }

    // add methods for v3 SDK
    public com.skyflow.vault.data.InsertResponse bulkInsert(InsertRequest insertRequest) throws SkyflowException {
        com.skyflow.vault.data.InsertResponse response;
        LogUtil.printInfoLog(InfoLogs.INSERT_TRIGGERED.getLog());
        try {
            // validation
            LogUtil.printInfoLog(InfoLogs.VALIDATE_INSERT_REQUEST.getLog());
            Validations.validateInsertRequest(insertRequest);
            int batchSize = 50;
            int concurrencyLimit = 10;
            setBearerToken();
            // calculate batch concurrency

            // req
            com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest request = super.getBUlkInsertRequestBody(insertRequest, super.getVaultConfig());

            response = this.processSync(batchSize, concurrencyLimit, request);
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        } catch (ExecutionException | InterruptedException e){
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            throw new SkyflowException(e.getMessage());
        }
        Summary summary = new Summary();
        summary.setTotalRecords(insertRequest.getValues().size());
        if (response.getSuccess() != null) {
            summary.setTotalInserted(response.getSuccess().size());
        } else {
            summary.setTotalInserted(0);
        }
        if (response.getErrors() != null) {
            summary.setTotalFailed(response.getErrors().size());
        } else {
            summary.setTotalFailed(0);
        }
        response.setSummary(summary);
        return response;
    }

    public CompletableFuture<com.skyflow.vault.data.InsertResponse> bulkInsertAsync(InsertRequest insertRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.INSERT_TRIGGERED.getLog());
        try {
            // validation
            LogUtil.printInfoLog(InfoLogs.VALIDATE_INSERT_REQUEST.getLog());
            Validations.validateInsertRequest(insertRequest);
            int batchSize = 50;
            int concurrencyLimit = 10;
            setBearerToken();
            // calculate batch concurrency

            // req
            com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest request = super.getBUlkInsertRequestBody(insertRequest, super.getVaultConfig());

            List<ErrorRecord> errorRecords = new ArrayList<>();

            List<CompletableFuture<com.skyflow.vault.data.InsertResponse>> futures = this.insertBatchFutures(
                    batchSize, concurrencyLimit, request, errorRecords
            );

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        List<Success> successRecords1 = new ArrayList<>();

                        for (CompletableFuture<com.skyflow.vault.data.InsertResponse> future : futures) {
                            com.skyflow.vault.data.InsertResponse futureResponse = future.join();
                            if (futureResponse != null) {
                                if (futureResponse.getSuccess() != null) {
                                    successRecords1.addAll(futureResponse.getSuccess());
                                }
                                if (futureResponse.getErrors() != null) {
                                    errorRecords.addAll(futureResponse.getErrors());
                                }
                            }
                        }

                        com.skyflow.vault.data.InsertResponse response1 = new com.skyflow.vault.data.InsertResponse();
                        Summary summary = new Summary();
                        if (!successRecords1.isEmpty()) {
                            response1.setSuccess(successRecords1);
                            summary.setTotalInserted(successRecords1.size());
                        }
                        if (!errorRecords.isEmpty()) {
                            response1.setErrors(errorRecords);
                            summary.setTotalFailed(errorRecords.size());
                        }
                        summary.setTotalRecords(insertRequest.getValues().size());
                        response1.setSummary(summary);
                        return response1;
                    });
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.INSERT_RECORDS_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        }
    }

    private com.skyflow.vault.data.InsertResponse processSync(
            int batchSize, int concurrencyLimit, com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest insertRequest
    ) throws ExecutionException, InterruptedException {
        LogUtil.printInfoLog(InfoLogs.PROCESSING_BATCHES.getLog());
        List<ErrorRecord> errorRecords = new ArrayList<>();
        List<Success> successRecords = new ArrayList<>();

            List<CompletableFuture<com.skyflow.vault.data.InsertResponse>> futures = this.insertBatchFutures(
                    batchSize, concurrencyLimit, insertRequest, errorRecords
            );

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
        com.skyflow.vault.data.InsertResponse response = new com.skyflow.vault.data.InsertResponse();
        if (!errorRecords.isEmpty()) {
            response.setErrors(errorRecords);
        }
        if (!successRecords.isEmpty()) {
            response.setSuccess(successRecords);
        }
        LogUtil.printInfoLog(InfoLogs.INSERT_REQUEST_RESOLVED.getLog());
        return response;
    }


    private List<CompletableFuture<com.skyflow.vault.data.InsertResponse>> insertBatchFutures(
            int batchSize, int concurrencyLimit, com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest insertRequest, List<ErrorRecord> errorRecords
    ) {
        List<InsertRecordData> records = insertRequest.getRecords().get();

        ExecutorService executor = Executors.newFixedThreadPool(concurrencyLimit);
        List<List<InsertRecordData>> batches = Utils.createBatches(records, batchSize);
        List<CompletableFuture<com.skyflow.vault.data.InsertResponse>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < batches.size(); i++) {
                List<InsertRecordData> batch = batches.get(i);
                int batchNumber = i;
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

    private InsertResponse insertBatch(List<InsertRecordData> batch, String tableName){
        com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest req = com.skyflow.generated.rest.resources.recordservice.requests.InsertRequest.builder()
                .vaultId(this.getVaultConfig().getVaultId())
                .tableName(tableName)
                .records(batch)
                .build();
        return this.getRecordsApi().insert(req);
    }
}
