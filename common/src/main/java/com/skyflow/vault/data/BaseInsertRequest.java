package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.HashMap;

public class BaseInsertRequest {
    private final BaseInsertRequestBuilder builder;

    protected BaseInsertRequest(BaseInsertRequestBuilder builder) {
        this.builder = builder;
    }

    public String getTable() {
        return this.builder.table;
    }

    public ArrayList<HashMap<String, Object>> getValues() {
        return this.builder.values;
    }

    public ArrayList<HashMap<String, Object>> getTokens() {
        return this.builder.tokens;
    }

    public Boolean getReturnTokens() {
        return this.builder.returnTokens;
    }

//    public String getUpsert() {
//        return this.builder.upsert;
//    }

    static class BaseInsertRequestBuilder {
        protected String table;
        protected ArrayList<HashMap<String, Object>> values;
        protected ArrayList<HashMap<String, Object>> tokens;
        protected Boolean returnTokens;
        protected String upsert;

        protected BaseInsertRequestBuilder() {
            this.returnTokens = false;
        }

        public BaseInsertRequestBuilder table(String table) {
            this.table = table;
            return this;
        }

        public BaseInsertRequestBuilder values(ArrayList<HashMap<String, Object>> values) {
            this.values = values;
            return this;
        }

        public BaseInsertRequestBuilder tokens(ArrayList<HashMap<String, Object>> tokens) {
            this.tokens = tokens;
            return this;
        }

        public BaseInsertRequestBuilder returnTokens(Boolean returnTokens) {
            this.returnTokens = returnTokens != null && returnTokens;
            return this;
        }

//        public BaseInsertRequestBuilder upsert(String upsert) {
//            this.upsert = upsert;
//            return this;
//        }
    }
}

