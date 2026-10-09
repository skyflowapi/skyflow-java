package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Record map keys: {@code skyflowId}, {@code tableName}, {@code columns}, {@code httpCode},
 * {@code error}, {@code requestId} (set only when {@code error} is non-null).
 * {@code columns} is a list of maps with keys {@code column} and {@code status} ({@code "DELETED"}),
 * or null on a failed record.
 */
public class DeleteFilesResponse extends BaseDeleteFilesResponse {
    public DeleteFilesResponse(ArrayList<HashMap<String, Object>> records) {
        super(records);
    }
}
