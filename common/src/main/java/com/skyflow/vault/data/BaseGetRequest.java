package com.skyflow.vault.data;

import com.skyflow.utils.BaseConstants;

import java.util.ArrayList;

class BaseGetRequest {
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

    public Boolean getReturnTokens() {
        return this.builder.returnTokens;
    }

    public ArrayList<String> getFields() {
        return this.builder.fields;
    }

    public String getOffset() {
        return this.builder.offset;
    }

    public String getLimit() {
        return this.builder.limit;
    }

    public Boolean getDownloadURL() {
        return this.builder.downloadURL;
    }

    public String getColumnName() {
        return this.builder.columnName;
    }

    public ArrayList<String> getColumnValues() {
        return this.builder.columnValues;
    }

    public String getOrderBy() {
        return this.builder.orderBy;
    }

    static class BaseGetRequestBuilder {
        protected String table;
        protected ArrayList<String> ids;
        protected Boolean returnTokens;
        protected ArrayList<String> fields;
        protected String offset;
        protected String limit;
        protected Boolean downloadURL;
        protected String columnName;
        protected ArrayList<String> columnValues;
        protected String orderBy;

        protected BaseGetRequestBuilder() {
            this.downloadURL = true;
            this.orderBy = BaseConstants.ORDER_ASCENDING;
        }

        public BaseGetRequestBuilder table(String table) {
            this.table = table;
            return this;
        }

        public BaseGetRequestBuilder ids(ArrayList<String> ids) {
            this.ids = ids;
            return this;
        }

        public BaseGetRequestBuilder returnTokens(Boolean returnTokens) {
            this.returnTokens = returnTokens;
            return this;
        }

        public BaseGetRequestBuilder fields(ArrayList<String> fields) {
            this.fields = fields;
            return this;
        }

        public BaseGetRequestBuilder offset(String offset) {
            this.offset = offset;
            return this;
        }

        public BaseGetRequestBuilder limit(String limit) {
            this.limit = limit;
            return this;
        }

        public BaseGetRequestBuilder downloadURL(Boolean downloadURL) {
            this.downloadURL = downloadURL == null || downloadURL;
            return this;
        }

        public BaseGetRequestBuilder columnName(String columnName) {
            this.columnName = columnName;
            return this;
        }

        public BaseGetRequestBuilder columnValues(ArrayList<String> columnValues) {
            this.columnValues = columnValues;
            return this;
        }

        public BaseGetRequestBuilder orderBy(String orderBy) {
            this.orderBy = orderBy == null ? BaseConstants.ORDER_ASCENDING : orderBy;
            return this;
        }

    }
}
