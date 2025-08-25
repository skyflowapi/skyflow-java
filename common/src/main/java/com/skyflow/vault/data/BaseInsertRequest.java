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

//    public Boolean getReturnTokens() {
//        return this.builder.returnTokens;
//    }

    static class BaseInsertRequestBuilder {
        protected String table;
        protected ArrayList<HashMap<String, Object>> values;
//        protected ArrayList<HashMap<String, Object>> tokens;
//        protected Boolean returnTokens;
        protected String upsert;

        protected BaseInsertRequestBuilder() {
//            this.returnTokens = false;
        }

        public BaseInsertRequestBuilder table(String table) {
            this.table = table;
            return this;
        }

        public BaseInsertRequestBuilder values(ArrayList<HashMap<String, Object>> values) {
            this.values = values;
            return this;
        }

//        public BaseInsertRequestBuilder returnTokens(Boolean returnTokens) {
//            this.returnTokens = returnTokens != null && returnTokens;
//            return this;
//        }
    }
}

