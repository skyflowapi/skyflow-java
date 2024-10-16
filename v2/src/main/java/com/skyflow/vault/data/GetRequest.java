package com.skyflow.vault.data;

import com.skyflow.enums.RedactionType;
import com.skyflow.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class GetRequest {
    private final String table;
    private final ArrayList<String> ids;
    private final RedactionType redactionType;
    private final Boolean tokenization;
    private final List<String> fields;
    private final String offset;
    private final String limit;
    private final Boolean downloadURL;
    private final String columnName;
    private final ArrayList<String> columnValues;
    private final String orderBy;

    private GetRequest(GetRequestBuilder builder) {
        this.table = builder.table;
        this.ids = builder.ids;
        this.redactionType = builder.redactionType;
        this.tokenization = builder.tokenization;
        this.fields = builder.fields;
        this.offset = builder.offset;
        this.limit = builder.limit;
        this.downloadURL = builder.downloadURL;
        this.columnName = builder.columnName;
        this.columnValues = builder.columnValues;
        this.orderBy = builder.orderBy;
    }

    public static GetRequestBuilder builder() {
        return new GetRequestBuilder();
    }

    public String getTable() {
        return table;
    }

    public ArrayList<String> getIds() {
        return ids;
    }

    public RedactionType getRedactionType() {
        return redactionType;
    }

    public Boolean getTokenization() {
        return tokenization;
    }

    public List<String> getFields() {
        return fields;
    }

    public String getOffset() {
        return offset;
    }

    public String getLimit() {
        return limit;
    }

    public Boolean getDownloadURL() {
        return downloadURL;
    }

    public String getColumnName() {
        return columnName;
    }

    public ArrayList<String> getColumnValues() {
        return columnValues;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public static class GetRequestBuilder {
        private String table;
        private ArrayList<String> ids;
        private RedactionType redactionType;
        private Boolean tokenization;
        private List<String> fields;
        private String offset;
        private String limit;
        private Boolean downloadURL;
        private String columnName;
        private ArrayList<String> columnValues;
        private String orderBy;

        private GetRequestBuilder() {
            this.redactionType = RedactionType.PLAIN_TEXT;
            this.tokenization = false;
            this.downloadURL = true;
            this.orderBy = Constants.ORDER_ASCENDING;
        }

        public GetRequestBuilder table(String table) {
            this.table = table;
            return this;
        }

        public GetRequestBuilder ids(ArrayList<String> ids) {
            this.ids = ids;
            return this;
        }

        public GetRequestBuilder redactionType(RedactionType redactionType) {
            this.redactionType = redactionType;
            return this;
        }

        public GetRequestBuilder tokenization(Boolean tokenization) {
            this.tokenization = tokenization;
            return this;
        }

        public GetRequestBuilder fields(List<String> fields) {
            this.fields = fields;
            return this;
        }

        public GetRequestBuilder offset(String offset) {
            this.offset = offset;
            return this;
        }

        public GetRequestBuilder limit(String limit) {
            this.limit = limit;
            return this;
        }

        public GetRequestBuilder downloadURL(Boolean downloadURL) {
            this.downloadURL = downloadURL;
            return this;
        }

        public GetRequestBuilder columnName(String columnName) {
            this.columnName = columnName;
            return this;
        }

        public GetRequestBuilder columnValues(ArrayList<String> columnValues) {
            this.columnValues = columnValues;
            return this;
        }

        public GetRequestBuilder orderBy(String orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        public GetRequest build() {
            return new GetRequest(this);
        }
    }
}
