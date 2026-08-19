package com.skyflow.vault.data;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BulkInsertResponse {
    private BulkSummary summary;
    private List<BulkInsertResponseRecord> records;

    // Internal fields. transient keeps them out of the toString() output.
    private transient List<InsertRequestRecord> originalPayload;
    private transient List<BulkInsertRequestRecord> recordsToRetry;

    public BulkInsertResponse(List<BulkInsertResponseRecord> records) {
        this.records = records;
    }

    public BulkInsertResponse(
            List<BulkInsertResponseRecord> records,
            List<InsertRequestRecord> originalPayload
    ) {
        this.records = records;
        this.originalPayload = originalPayload;
        this.summary = buildSummary(records, originalPayload);
    }

    private static BulkSummary buildSummary(List<BulkInsertResponseRecord> records,
                                             List<InsertRequestRecord> originalPayload) {
        int totalFailed = records != null
                ? (int) records.stream().filter(record -> record.getError() != null).count()
                : 0;
        int totalInserted = (records != null ? records.size() : 0) - totalFailed;
        int totalRecords = originalPayload != null
                ? originalPayload.size()
                : (records != null ? records.size() : 0);
        return new BulkSummary(totalRecords, totalInserted, totalFailed);
    }

    public BulkSummary getSummary() {
        return this.summary;
    }

    public List<BulkInsertResponseRecord> getRecords() {
        return this.records;
    }

    // Records are guaranteed to be BulkInsertRequestRecord: validateBulkInsertRequest rejects
    // any other InsertRequestRecord subtype before the request is ever sent.
    public List<BulkInsertRequestRecord> getRecordsToRetry() {
        if (recordsToRetry == null) {
            // Per-batch responses are built without the original payload; nothing to retry from.
            if (originalPayload == null) {
                return new ArrayList<>();
            }
            recordsToRetry = records.stream()
                    .filter(record -> record.getHttpCode() >= 500 && record.getHttpCode() <= 599
                            && record.getHttpCode() != 529)
                    .map(record -> (BulkInsertRequestRecord) originalPayload.get(record.getIndex()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        return recordsToRetry;
    }

    @Override
    public String toString() {
        Gson gson = new Gson().newBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
