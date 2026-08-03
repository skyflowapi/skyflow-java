package com.skyflow.vault.data;

import java.util.ArrayList;

public class BaseGetRequest {
    private final BaseGetRequestBuilder builder;

    protected BaseGetRequest(BaseGetRequestBuilder builder) {
        this.builder = builder;
    }

    public String getTable() {
        return this.builder.table;
    }

    public ArrayList<String> getIds() {
        return this.builder.ids;
    }

    public ArrayList<String> getFields() {
        return this.builder.fields;
    }

    static class BaseGetRequestBuilder {
        protected String table;
        protected ArrayList<String> ids;
        protected ArrayList<String> fields;

        protected BaseGetRequestBuilder() {
        }

        public BaseGetRequestBuilder table(String table) {
            this.table = table;
            return this;
        }

        public BaseGetRequestBuilder ids(ArrayList<String> ids) {
            this.ids = ids;
            return this;
        }

        public BaseGetRequestBuilder fields(ArrayList<String> fields) {
            this.fields = fields;
            return this;
        }
    }
}
