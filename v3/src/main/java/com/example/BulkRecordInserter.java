package com.example;

import com.skyflow.generated.rest.types.InsertRecordData;
import com.skyflow.generated.rest.types.InsertResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BulkRecordInserter {
    private final int concurrencyLimit;
    private final int batchSize;
    private final OkHttpClient httpClient;

    public BulkRecordInserter(int concurrencyLimit, int batchSize, String token) {
        this.concurrencyLimit = concurrencyLimit;
        this.batchSize = batchSize;
        this.httpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request requestWithAuth = original.newBuilder()
                            .header("Authorization", "Bearer " + token)
                            .build();
                    return chain.proceed(requestWithAuth);
                })
                .build();
    }

    public void process(List<InsertRecordData> records) {
        List<List<InsertRecordData>> batches = createBatches(records, batchSize);
        System.out.printf("Processing %d batches with concurrency=%d...\n", batches.size(), concurrencyLimit);
        List<CompletableFuture<InsertResponse>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        for (int i = 0; i < batches.size(); i++) {
            final int batchNumber = i + 1;
            List<InsertRecordData> batch = batches.get(i);
            try {
                System.out.println("Starting Batch " + batchNumber +
                        " on thread " + Thread.currentThread().getName());

                CompletableFuture<InsertResponse> future = InsertSample2.insertData(batch, httpClient);
//                            .thenAccept(response -> {
////                                System.out.println("Batch " + batchNumber + " completed on thread " +
////                                        Thread.currentThread().getName() + ": " + response);
//                            })
//                            .exceptionally(ex -> {
////                                System.err.println("Batch " + batchNumber + " failed: " + ex);
//                                return null;
//                            })
//                            .whenComplete((res, ex) -> System.out.println("Batch " + batchNumber + " processing finished on thread " +
//                                    Thread.currentThread().getName()));
                futures.add(future);
            } catch (Exception e) {
                System.out.printf("Batch %d failed with exception: %s\n", batchNumber, e.getMessage());
            } finally {
                System.out.println("Batch " + batchNumber + " processing finished on thread " +
                        Thread.currentThread().getName());
            }
        }

        for (CompletableFuture<InsertResponse> future : futures) {
            try {
                InsertResponse response = future.join(); // Retrieve the result
                System.out.println("Response here: " + response);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
                System.err.println("Error while processing future: " + e.getMessage());
            }
        }
        System.out.printf("All batches completed. Success=%d, Failed=%d\n",
                successCount.get(), failureCount.get());
    }

    private List<List<InsertRecordData>> createBatches(List<InsertRecordData> records, int batchSize) {
        List<List<InsertRecordData>> batches = new ArrayList<>();
        for (int i = 0; i < records.size(); i += batchSize) {
            batches.add(records.subList(i, Math.min(i + batchSize, records.size())));
        }
        return batches;
    }

    public static void main(String[] args) {
        int recordsCount = 1;
        int concurrency = 1;
        int batchSize = 1;

        BatchProcessor processor = new BatchProcessor(concurrency, batchSize, "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY2MiOiJiOTYzZTcxMjFkZDY0YTZkOTY0MGQ3ZTNlNGNjODdhNyIsImF1ZCI6Imh0dHBzOi8vbWFuYWdlLWJsaXR6LnNreWZsb3dhcGlzLmRldiIsImV4cCI6MTc1NTU5NjEwNSwiaWF0IjoxNzU1NTA5NzA1LCJpc3MiOiJzYS1hdXRoQG1hbmFnZS1ibGl0ei5za3lmbG93YXBpcy5kZXYiLCJqdGkiOiJ0NjQ2OWUyZWJlNzY0YmNiODIzZGY3ZjYwYTY1NjgyZSIsInN1YiI6ImUzMThiYjk0NzA4YjQ0MmRiZjBkMWY2MjEwYmQxMGU5In0.bhyK9y7wDvUXY1Q3UCJs_Rhv3xOfHScXBCa8-zZJ5xSd5uryBfOOjVhx4X9ZOO8vEwRAU4Ij8deBKZ08k7knXHd3MTLjkq7euKdFqIhkL1TknQkAQHhxES8FoZQuzV4_WZJejnmopZOXYviY-SeggaQTMyfoSdI3ObmLsPjkHo7InJJGwr9Jyc2eiae_4KVMFPTLw9yZVx55fJ7wa1i3VJFT_OMiiyxjVS8f6NYTjIzQtvqxh_NRMRhIMxcGoh0ZLh6yZJQzMU9sg9DBfRdrlVOMDOfGhptb38i3hodsnIw08AtOrDHdjwwHC_lqT96o8z8bUJzGW9fKZLclcAN3wg");
        List<InsertRecordData> mockRecords = generateMockRecords(recordsCount);
        processor.process(mockRecords);
    }

    private static List<InsertRecordData> generateMockRecords(int count) {
        List<InsertRecordData> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", "bharti");
            map.put("email", "email" + i + "@email.com");
            records.add(InsertRecordData.builder().data(map).build());
        }
        return records;
    }
}
