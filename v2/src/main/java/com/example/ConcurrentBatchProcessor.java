package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ConcurrentBatchProcessor {

    // A static executor to be shared by all methods in the class
    private static ExecutorService executor;

    // --- Main Method to demonstrate the functionality ---
    public static void main(String[] args) {
        // Define the parameters for the batch processing
        int totalRecords = 1000; // L: Total number of records to process
        int batchSize = 50;    // M: Size of each batch
        int concurrencyLimit = 5; // N: Maximum number of concurrent calls

        // Create a list of dummy records (e.g., strings)
        List<String> allRecords = new ArrayList<>();
        for (int i = 1; i <= totalRecords; i++) {
            allRecords.add("Record-" + i);
        }

        System.out.println("Starting batch processing for " + allRecords.size() + " records.");
        System.out.println("Batch Size (M): " + batchSize);
        System.out.println("Concurrency Limit (N): " + concurrencyLimit);

        try {
            processRecordsInBatches(allRecords, batchSize, concurrencyLimit);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Processing failed due to an exception: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("All batches have been processed.");
    }

    // --- Core logic for concurrent batch processing ---

    /**
     * Chunks a list of records and processes each batch concurrently.
     *
     * @param records          The list of all records (L) to be processed.
     * @param batchSize        The number of records in each batch (M).
     * @param concurrencyLimit The maximum number of concurrent calls (N).
     * @throws InterruptedException if the thread is interrupted while waiting.
     * @throws ExecutionException   if a task failed to complete.
     */
    public static void processRecordsInBatches(List<String> records, int batchSize, int concurrencyLimit)
            throws InterruptedException, ExecutionException {

        // Initialize the shared fixed-size thread pool
        executor = Executors.newFixedThreadPool(concurrencyLimit);

        // List to hold all the futures for each batch
        List<CompletableFuture<String>> futures = new ArrayList<>();

        // Loop through the records to create batches
//        1st itr -> i=0 bs=10 ei=10
//        2nd itr -> i=10 bs=10 ei=20

        int counter = 0;
        for (int i = 0; i < records.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, records.size());
            List<String> batch = records.subList(i, endIndex);
            int batchId = counter + 1;

            // Submit the batch processing task using CompletableFuture.supplyAsync()
            // This is where we create the CompletableFuture and pass the executor.

            CompletableFuture<String> future = CompletableFuture
                    .supplyAsync(() -> performApiCall(batch, batchId), executor)
                    .whenComplete((resp, ex) -> System.out.printf("Batch %d End Time:\t%s for thread %s\n", batchId, System.currentTimeMillis(), Thread.currentThread().getName())
                    );
            futures.add(future);
            counter++;
        }

        // Wait for all submitted tasks to complete.
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Optional: Process the results if needed.
        System.out.println("All batches completed. Here are the results:");
        for (CompletableFuture<String> future : futures) {
            // get() blocks until the future is complete.
            System.out.println(future.get());
        }

        // Shut down the executor gracefully
        executor.shutdown();
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            System.err.println("Executor did not terminate in the specified time.");
        }
    }

    // --- Placeholder method to simulate a synchronous API call ---

    /**
     * Simulates a synchronous, blocking API call with a batch of records.
     * This method does not return a CompletableFuture and doesn't need to know about the executor.
     *
     * @param batch The list of records for a single API call.
     * @return A success message string.
     */
    private static String performApiCall(List<String> batch, int batchId) {
        // Simulate a network call delay
        try {
            System.out.printf("Batch %d Start Time:\t%s for thread %s\n", batchId, System.currentTimeMillis(), Thread.currentThread().getName());
            long processingTime = (long) (Math.random() * 2000) + 500; // 0.5 to 2.5 seconds
            Thread.sleep(processingTime);
            String result = "Successfully processed batch of " + batch.size() +
                    " records, starting with " + batch.get(0) +
                    " (simulated duration: " + processingTime + "ms)";
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("API call was interrupted.", e);
        }
    }
}