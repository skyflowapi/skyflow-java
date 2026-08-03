package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

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
        int totalTokenized = 0;
        int totalPartial = 0;
        int totalFailed = 0;
        if (records != null) {
            for (BulkTokenizeResponseRecord record : records) {
                int succeeded = 0;
                int failed = 0;
                if (record.getTokens() != null) {
                    for (TokenizeResponseToken token : record.getTokens()) {
                        if (token.getError() == null) {
                            succeeded++;
                        } else {
                            failed++;
                        }
                    }
                }
                if (succeeded > 0 && failed > 0) {
                    totalPartial++;
                } else if (succeeded > 0) {
                    totalTokenized++;
                } else {
                    // no token groups came back, or every one of them failed
                    totalFailed++;
                }
            }
        }
        int totalTokens = originalPayload != null
                ? originalPayload.size()
                : (records != null ? records.size() : 0);
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
     * were supplied. Records where nothing failed retryably are omitted.
     */
    public List<BulkTokenizeRequestRecord> getRecordsToRetry() {
        if (recordsToRetry == null) {
            recordsToRetry = new ArrayList<>();
            if (records != null && originalPayload != null) {
                for (BulkTokenizeResponseRecord record : records) {
                    // the SDK assigns the index from the record's position in originalPayload, so a
                    // positional lookup is exact
                    int index = record.getIndex();
                    if (index < 0 || index >= originalPayload.size() || !hasRetryableFailure(record)) {
                        continue;
                    }
                    recordsToRetry.add(originalPayload.get(index));
                }
            }
        }
        return recordsToRetry;
    }

    private static boolean hasRetryableFailure(BulkTokenizeResponseRecord record) {
        if (record.getTokens() == null) {
            return false;
        }
        for (TokenizeResponseToken token : record.getTokens()) {
            if (isRetryable(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRetryable(TokenizeResponseToken token) {
        Integer httpCode = token.getHttpCode();
        return token.getError() != null
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
