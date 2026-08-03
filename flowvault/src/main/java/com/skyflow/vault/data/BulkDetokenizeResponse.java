package com.skyflow.vault.data;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BulkDetokenizeResponse {
    private DetokenizeSummary summary;
    private List<BulkDetokenizeResponseRecord> records;

    // Internal fields. transient keeps them out of the toString() output.
    private transient List<String> originalPayload;
    private transient List<String> tokensToRetry;

    public BulkDetokenizeResponse(List<BulkDetokenizeResponseRecord> records) {
        this.records = records;
    }

    public BulkDetokenizeResponse(
            List<BulkDetokenizeResponseRecord> records,
            List<String> originalPayload
    ) {
        this.records = records;
        this.originalPayload = originalPayload;
        int totalFailed = (int) records.stream().filter(record -> record.getError() != null).count();
        this.summary = new DetokenizeSummary(originalPayload.size(), records.size() - totalFailed, totalFailed);
    }

    public DetokenizeSummary getSummary() {
        return this.summary;
    }

    public List<BulkDetokenizeResponseRecord> getRecords() {
        return this.records;
    }

    public List<String> getTokensToRetry() {
        if (tokensToRetry == null) {
            // Per-batch responses are built without the original payload; nothing to retry from.
            if (originalPayload == null) {
                return new ArrayList<>();
            }
            tokensToRetry = records.stream()
                    .filter(record -> record.getHttpCode() >= 500 && record.getHttpCode() <= 599
                            && record.getHttpCode() != 529)
                    .map(record -> originalPayload.get(record.getIndex()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        return tokensToRetry;
    }

    @Override
    public String toString() {
        Gson gson = new Gson().newBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
