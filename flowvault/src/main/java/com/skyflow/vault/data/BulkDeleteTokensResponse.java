package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

public class BulkDeleteTokensResponse {
    @Expose(serialize = true)
    private DeleteTokensSummary summary;

    @Expose(serialize = true)
    private List<BulkDeleteTokensResponseRecord> records;

    private List<String> originalPayload;
    private List<String> tokensToRetry;

    public BulkDeleteTokensResponse(List<BulkDeleteTokensResponseRecord> records) {
        this.records = records;
    }

    public BulkDeleteTokensResponse(List<BulkDeleteTokensResponseRecord> records, List<String> originalPayload) {
        this.records = records;
        this.originalPayload = originalPayload;
        this.summary = buildSummary(this.records, this.originalPayload);
    }

    private static DeleteTokensSummary buildSummary(List<BulkDeleteTokensResponseRecord> records, List<String> originalPayload) {
        int totalDeleted = 0;
        int totalFailed = 0;
        if (records != null) {
            for (BulkDeleteTokensResponseRecord record : records) {
                if (record.getError() == null) {
                    totalDeleted++;
                } else {
                    totalFailed++;
                }
            }
        }
        int totalTokens = originalPayload != null ? originalPayload.size() : totalDeleted + totalFailed;
        return new DeleteTokensSummary(totalTokens, totalDeleted, totalFailed);
    }

    public DeleteTokensSummary getSummary() {
        return summary;
    }

    public List<BulkDeleteTokensResponseRecord> getRecords() {
        return records;
    }

    /**
     * The tokens whose delete failed with a retryable status, ready to be resubmitted.
     *
     * <p>Retryable means a 5xx other than 529, matching the rule used by bulk insert and bulk
     * detokenize. Tokens are read straight off the failed records, so a token is never dropped
     * because of an index mismatch.
     */
    public List<String> getTokensToRetry() {
        if (tokensToRetry == null) {
            tokensToRetry = new ArrayList<>();
            if (records != null) {
                for (BulkDeleteTokensResponseRecord record : records) {
                    if (isRetryable(record) && record.getToken() != null) {
                        tokensToRetry.add(record.getToken());
                    }
                }
            }
        }
        return tokensToRetry;
    }

    private static boolean isRetryable(BulkDeleteTokensResponseRecord record) {
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
