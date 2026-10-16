package com.skyflow.vault.data;

import java.util.List;

public class GetTokensRequest extends BaseGetTokensRequest {
    private final GetTokensRequestBuilder builder;

    private GetTokensRequest(GetTokensRequestBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    /** Value/token-group pairs to look up. One response record is returned per entry, in order. */
    public List<GetTokensRequestRecord> getRecords() {
        return this.builder.records;
    }

    public static GetTokensRequestBuilder builder() {
        return new GetTokensRequestBuilder();
    }

    public static final class GetTokensRequestBuilder extends BaseGetTokensRequestBuilder {
        private List<GetTokensRequestRecord> records;

        private GetTokensRequestBuilder() {
        }

        public GetTokensRequestBuilder records(List<GetTokensRequestRecord> records) {
            this.records = records;
            return this;
        }

        public GetTokensRequest build() {
            return new GetTokensRequest(this);
        }
    }
}
