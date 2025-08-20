package com.skyflow.vault;

public class RequestOptions {
    public RequestOptionsBuilder builder;

    public static RequestOptionsBuilder builder() {
        return new RequestOptionsBuilder();
    }

    public int getBatchSize() {
        return this.builder.batchSize;
    }

    public int getConcurrencyLimit() {
        return this.builder.concurrencyLimit;
    }

    public static class RequestOptionsBuilder {
        private int batchSize;
        private int concurrencyLimit;

        private RequestOptionsBuilder() {
            this.batchSize = 100;
            this.concurrencyLimit = 1;
        }

        public RequestOptionsBuilder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public RequestOptionsBuilder concurrencyLimit(int concurrencyLimit) {
            this.concurrencyLimit = concurrencyLimit;
            return this;
        }
    }
}
