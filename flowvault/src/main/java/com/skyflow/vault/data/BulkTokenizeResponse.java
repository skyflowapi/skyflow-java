package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BulkTokenizeResponse {
    @Expose(serialize = true)
    private TokenizeSummary summary;

    @Expose(serialize = true)
    private List<BulkTokenizeResponseRecord> records;

    private List<BulkTokenizeRequestRecord> originalPayload;
    private List<BulkTokenizeRequestRecord> recordsToRetry;

    public BulkTokenizeResponse(List<BulkTokenizeResponseRecord> records) {
        this.records = records;
    }

    public BulkTokenizeResponse(List<BulkTokenizeResponseRecord> records,
                                List<BulkTokenizeRequestRecord> originalPayload) {
        this.records = records;
        this.originalPayload = originalPayload;
        this.summary = buildSummary(this.records, this.originalPayload);
    }

    /**
     * {@code totalTokens} counts the input values submitted. The remaining three classify each
     * value by how its token groups fared, so together they sum to the number of values.
     */
    private static TokenizeSummary buildSummary(List<BulkTokenizeResponseRecord> records,
                                                List<BulkTokenizeRequestRecord> originalPayload) {
        Map<Integer, int[]> outcomesByIndex = new LinkedHashMap<>(); // index -> [succeeded, failed]
        if (records != null) {
            for (BulkTokenizeResponseRecord record : records) {
                int[] outcomes = outcomesByIndex.computeIfAbsent(record.getIndex(), key -> new int[2]);
                if (record.getError() == null) {
                    outcomes[0]++;
                } else {
                    outcomes[1]++;
                }
            }
        }
        int totalTokenized = 0;
        int totalPartial = 0;
        int totalFailed = 0;
        int totalTokens;
        if (originalPayload != null) {
            totalTokens = originalPayload.size();
            for (int index = 0; index < originalPayload.size(); index++) {
                int[] outcomes = outcomesByIndex.getOrDefault(index, new int[2]);
                if (outcomes[0] > 0 && outcomes[1] > 0) {
                    totalPartial++;
                } else if (outcomes[0] > 0) {
                    totalTokenized++;
                } else {
                    totalFailed++;
                }
            }
        } else {
            totalTokens = records != null ? records.size() : 0;
            for (int[] outcomes : outcomesByIndex.values()) {
                if (outcomes[0] > 0 && outcomes[1] > 0) {
                    totalPartial++;
                } else if (outcomes[0] > 0) {
                    totalTokenized++;
                } else {
                    totalFailed++;
                }
            }
        }
        return new TokenizeSummary(totalTokens, totalTokenized, totalPartial, totalFailed);
    }

    public TokenizeSummary getSummary() {
        return summary;
    }

    public List<BulkTokenizeResponseRecord> getRecords() {
        return records;
    }

    /**
     * The records that failed with a retryable status, ready to be resubmitted as a new bulk request.
     *
     * <p>Retryable means a 5xx other than 529, matching the rule used elsewhere in the SDK. The
     * caller's original record objects are returned unchanged — they carry no index, exactly as they
     * were supplied.
     */
    public List<BulkTokenizeRequestRecord> getRecordsToRetry() {
        if (recordsToRetry == null) {
            recordsToRetry = new ArrayList<>();
            if (records != null && originalPayload != null) {
                Set<Integer> retryableIndexes = new LinkedHashSet<>();
                for (BulkTokenizeResponseRecord record : records) {
                    if (isRetryable(record)) {
                        retryableIndexes.add(record.getIndex());
                    }
                }
                for (int index : retryableIndexes) {
                    // the SDK assigns the index from the record's position in originalPayload, so a
                    // positional lookup is exact
                    if (index >= 0 && index < originalPayload.size()) {
                        recordsToRetry.add(originalPayload.get(index));
                    }
                }
            }
        }
        return recordsToRetry;
    }

    private static boolean isRetryable(BulkTokenizeResponseRecord record) {
        Integer httpCode = record.getHttpCode();
        return record.getError() != null
                && httpCode != null
                && httpCode >= 500 && httpCode <= 599
                && httpCode != 529;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
        return gson.toJson(this);
    }
}
