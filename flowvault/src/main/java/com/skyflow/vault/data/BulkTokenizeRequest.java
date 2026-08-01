package com.skyflow.vault.data;

import java.util.ArrayList;

public class BulkTokenizeRequest {
    private final BulkTokenizeRequestBuilder builder;

    private BulkTokenizeRequest(BulkTokenizeRequestBuilder builder) {
        this.builder = builder;
    }

    public static BulkTokenizeRequestBuilder builder() {
        return new BulkTokenizeRequestBuilder();
    }

    public ArrayList<BulkTokenizeRecord> getData() {
        return this.builder.data;
    }

    public static final class BulkTokenizeRequestBuilder {
        private ArrayList<BulkTokenizeRecord> data;

        private BulkTokenizeRequestBuilder() {}

        public BulkTokenizeRequestBuilder data(ArrayList<BulkTokenizeRecord> data) {
            this.data = data;
            return this;
        }

        public BulkTokenizeRequest build() {
            return new BulkTokenizeRequest(this);
        }
    }
}
