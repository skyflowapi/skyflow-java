package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.HashMap;

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
        protected String upsert;
        protected BaseInsertRequestBuilder() {
        }

        public BaseInsertRequestBuilder table(String table) {
            this.table = table;
            return this;
        }

    }
}

