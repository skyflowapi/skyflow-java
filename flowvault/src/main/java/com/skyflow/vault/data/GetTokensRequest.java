package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GetTokensRequest extends BaseGetTokensRequest {
    private final List<GetTokensRequestRecord> records;

    private GetTokensRequest(GetTokensRequestBuilder builder) {
        super(builder);
        // Snapshot the list so later changes to the builder or the caller's list cannot alter a built request.
        this.records = builder.records == null ? null
                : Collections.unmodifiableList(new ArrayList<>(builder.records));
    }

    /** Value/token-group pairs to look up. One response record is returned per entry, in order. */
    public List<GetTokensRequestRecord> getRecords() {
        return this.records;
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
