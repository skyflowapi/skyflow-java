package com.skyflow.vault.data;

import com.skyflow.enums.RedactionType;
import com.skyflow.utils.Constants;

import java.util.ArrayList;

public class GetRequest extends BaseGetRequest {
    private final GetRequestBuilder builder;

    private GetRequest(GetRequestBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    public static GetRequestBuilder builder() {
        return new GetRequestBuilder();
    }

    public RedactionType getRedactionType() {
        return this.builder.redactionType;
    }

    public static final class GetRequestBuilder extends BaseGetRequestBuilder {
        private RedactionType redactionType;

        private GetRequestBuilder() {
            super();
            this.downloadURL = true;
            this.orderBy = Constants.ORDER_ASCENDING;
        }

        @Override
        public GetRequestBuilder table(String table) {
            super.table(table);
            return this;
        }

        @Override
        public GetRequestBuilder ids(ArrayList<String> ids) {
            super.ids(ids);
            return this;
        }

        public GetRequestBuilder redactionType(RedactionType redactionType) {
            this.redactionType = redactionType;
            return this;
        }

        @Override
        public GetRequestBuilder returnTokens(Boolean returnTokens) {
            super.returnTokens(returnTokens);
            return this;
        }

        @Override
        public GetRequestBuilder fields(ArrayList<String> fields) {
            super.fields(fields);
            return this;
        }

        @Override
        public GetRequestBuilder offset(String offset) {
            super.offset(offset);
            return this;
        }

        @Override
        public GetRequestBuilder limit(String limit) {
            super.limit(limit);
            return this;
        }

        @Override
        public GetRequestBuilder downloadURL(Boolean downloadURL) {
            super.downloadURL(downloadURL);
            return this;
        }

        @Override
        public GetRequestBuilder columnName(String columnName) {
            super.columnName(columnName);
            return this;
        }

        @Override
        public GetRequestBuilder columnValues(ArrayList<String> columnValues) {
            super.columnValues(columnValues);
            return this;
        }

        @Override
        public GetRequestBuilder orderBy(String orderBy) {
            super.orderBy(orderBy);
            return this;
        }

        public GetRequest build() {
            return new GetRequest(this);
        }
    }
}
