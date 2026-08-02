package com.skyflow.vault.data;

class BaseInsertRequest {
    private final BaseInsertRequestBuilder builder;

    protected BaseInsertRequest(BaseInsertRequestBuilder builder) {
        this.builder = builder;
    }

    public String getTable() {
        return this.builder.table;
    }

    static class BaseInsertRequestBuilder {
        protected String table;
        protected BaseInsertRequestBuilder() {
        }

        public BaseInsertRequestBuilder table(String table) {
            this.table = table;
            return this;
        }

    }
}
