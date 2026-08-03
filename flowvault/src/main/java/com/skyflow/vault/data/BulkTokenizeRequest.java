package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.List;

public class BulkTokenizeRequest extends TokenizeRequest {

    private BulkTokenizeRequest(List<BulkTokenizeRequestRecord> records) {
        super(records);
    }

    public static BulkTokenizeRequestBuilder builder() {
        return new BulkTokenizeRequestBuilder();
    }

    /**
     * Narrows the inherited accessor to the bulk record type. Safe because the builder only ever
     * stores {@link BulkTokenizeRequestRecord}s.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<BulkTokenizeRequestRecord> getRecords() {
        return (List<BulkTokenizeRequestRecord>) super.getRecords();
    }

    public static final class BulkTokenizeRequestBuilder extends TokenizeRequestBuilder {

        private BulkTokenizeRequestBuilder() {}

        @Override
        public BulkTokenizeRequestBuilder records(List<? extends TokenizeRequestRecord> records) {
            this.records = records;
            return this;
        }

        @Override
        public BulkTokenizeRequest build() {
            List<BulkTokenizeRequestRecord> bulkRecords = null;
            if (this.records != null) {
                bulkRecords = new ArrayList<>();
                for (TokenizeRequestRecord record : this.records) {
                    // fail fast and clearly if a plain TokenizeRequestRecord was supplied to the
                    // bulk builder through the inherited setter
                    bulkRecords.add((BulkTokenizeRequestRecord) record);
                }
            }
            return new BulkTokenizeRequest(bulkRecords);
        }
    }
}
