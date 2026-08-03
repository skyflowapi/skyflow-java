package com.skyflow.vault.data;

import com.skyflow.enums.RedactionType;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.Constants;
import com.skyflow.utils.logger.LogUtil;

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

    public Boolean getReturnTokens() {
        return this.builder.returnTokens;
    }

    public String getOffset() {
        return this.builder.offset;
    }

    public String getLimit() {
        return this.builder.limit;
    }

    /**
     * @deprecated Use {@link #getDownloadUrl()} instead.
     */
    @Deprecated(since = "2.1", forRemoval = true)
    public Boolean getDownloadURL() {
        LogUtil.printWarningLog(InfoLogs.DEPRECATED_DOWNLOAD_URL.getLog());
        return getDownloadUrl();
    }

    public Boolean getDownloadUrl() {
        return this.builder.downloadUrl;
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

    public static final class GetRequestBuilder extends BaseGetRequestBuilder {
        private RedactionType redactionType;
        private Boolean returnTokens;
        private String offset;
        private String limit;
        private Boolean downloadUrl;
        private String columnName;
        private ArrayList<String> columnValues;
        private String orderBy;

        private GetRequestBuilder() {
            this.orderBy = Constants.ORDER_ASCENDING;
            this.downloadUrl = true;
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

        @Override
        public GetRequestBuilder fields(ArrayList<String> fields) {
            super.fields(fields);
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

        public GetRequestBuilder offset(String offset) {
            this.offset = offset;
            return this;
        }

        public GetRequestBuilder limit(String limit) {
            this.limit = limit;
            return this;
        }

        /**
         * @deprecated Use {@link #downloadUrl(Boolean)} instead.
         */
        @Deprecated(since = "2.1", forRemoval = true)
        public GetRequestBuilder downloadURL(Boolean downloadURL) {
            LogUtil.printWarningLog(InfoLogs.DEPRECATED_DOWNLOAD_URL.getLog());
            return downloadUrl(downloadURL);
        }

        public GetRequestBuilder downloadUrl(Boolean downloadUrl) {
            this.downloadUrl = downloadUrl == null || downloadUrl;
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
