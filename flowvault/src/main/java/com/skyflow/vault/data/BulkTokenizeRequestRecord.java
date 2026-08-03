package com.skyflow.vault.data;

import java.util.List;

/**
 * A record in a bulk tokenize request.
 *
 * <p>Carries nothing beyond {@link TokenizeRequestRecord} today. It exists as its own type so the
 * bulk request has somewhere to grow, mirroring the {@link BulkInsertRecord} / {@link InsertRecord}
 * split. The index that correlates a record with its result is assigned by the SDK from list
 * position and appears only on {@link BulkTokenizeResponseRecord} — callers never supply it.
 */
public class BulkTokenizeRequestRecord extends TokenizeRequestRecord {

    private BulkTokenizeRequestRecord(BulkTokenizeRequestRecordBuilder builder) {
        super(builder);
    }

    public static BulkTokenizeRequestRecordBuilder builder() {
        return new BulkTokenizeRequestRecordBuilder();
    }

    public static final class BulkTokenizeRequestRecordBuilder extends TokenizeRequestRecordBuilder {

        private BulkTokenizeRequestRecordBuilder() {}

        @Override
        public BulkTokenizeRequestRecordBuilder value(Object value) {
            this.value = value;
            return this;
        }

        @Override
        public BulkTokenizeRequestRecordBuilder token(Object token) {
            this.token = token;
            return this;
        }

        @Override
        public BulkTokenizeRequestRecordBuilder tokenGroupNames(List<String> tokenGroupNames) {
            this.tokenGroupNames = tokenGroupNames;
            return this;
        }

        @Override
        public BulkTokenizeRequestRecord build() {
            return new BulkTokenizeRequestRecord(this);
        }
    }
}
