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
        @SuppressWarnings("unchecked")
        public BulkTokenizeRequest build() {
            // No other builder in this SDK throws from build() — validation is deferred to the
            // Validations layer at actual-use time, which is a SkyflowException-declaring method.
            // A plain TokenizeRequestRecord supplied through the inherited setter can't be verified
            // here without an immediate (and here, premature) cast; Validations.validateBulkTokenizeRequest
            // checks each element's real type before use and reports a clean SkyflowException instead.
            List<? extends TokenizeRequestRecord> copy = this.records == null ? null : new ArrayList<>(this.records);
            List<BulkTokenizeRequestRecord> bulkRecords = (List<BulkTokenizeRequestRecord>) copy;
            return new BulkTokenizeRequest(bulkRecords);
        }
    }
}
