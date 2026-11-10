package com.skyflow.vault.data;

import java.util.List;

/** One record per input entry, in request order. */
public class GetTokensResponse extends BaseGetTokensResponse<GetTokensResponseRecord> {
    public GetTokensResponse(List<GetTokensResponseRecord> records) {
        super(records);
    }
}
