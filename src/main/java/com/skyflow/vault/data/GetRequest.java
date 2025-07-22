package com.skyflow.vault.data;

import com.skyflow.enums.RedactionType;
import com.skyflow.utils.Constants;

import java.util.ArrayList;

public class GetRequest {
    private final GetRequestBuilder builder;

    private GetRequest(GetRequestBuilder builder) {
        this.builder = builder;
    }

    public static GetRequestBuilder builder() {
        return new GetRequestBuilder();
    }

    public String getTable() {
        return this.builder.table;
    }

    public ArrayList<String> getIds() {
        return this.builder.ids;
    }

    public RedactionType getRedactionType() {
        return this.builder.redactionType;
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

    public static final class GetRequestBuilder {
        private String table;
        private ArrayList<String> ids;
        private RedactionType redactionType = RedactionType.PLAIN_TEXT;
        private Boolean returnTokens;
        private ArrayList<String> fields;
        private String offset;
        private String limit;
        private Boolean downloadURL;
        private String columnName;
        private ArrayList<String> columnValues;
        private String orderBy;

        private GetRequestBuilder() {
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

        public GetRequestBuilder returnTokens(Boolean returnTokens) {
            this.returnTokens = returnTokens;
            return this;
        }

        public GetRequestBuilder fields(ArrayList<String> fields) {
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
            this.downloadURL = downloadURL == null || downloadURL;
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
            this.orderBy = orderBy == null ? Constants.ORDER_ASCENDING : orderBy;
            return this;
        }

        public GetRequest build() {
            return new GetRequest(this);
        }
    }
}
