package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Record map keys: {@code value}, {@code tokenGroupName}, {@code token}, {@code httpCode},
 * {@code error}, {@code requestId} (set only when {@code error} is non-null).
 * {@code token} is {@code ""} (never null) on a record that has no token.
 */
public class GetTokensResponse extends BaseGetTokensResponse {
    public GetTokensResponse(ArrayList<HashMap<String, Object>> records) {
        super(records);
    }
}
