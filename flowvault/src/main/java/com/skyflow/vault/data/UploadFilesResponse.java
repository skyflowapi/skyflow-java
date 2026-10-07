package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Record map keys: {@code skyflowId}, {@code tableName}, {@code columns}, {@code httpCode},
 * {@code error}, {@code requestId} (set only when {@code error} is non-null).
 * Each entry of {@code columns} is a map with keys {@code column}, {@code fileName},
 * {@code uploadStatus} ({@code "UPLOADED"}, {@code "FAILED"} or {@code "SKIPPED"}) and {@code error}.
 */
public class UploadFilesResponse extends BaseUploadFilesResponse {
    public UploadFilesResponse(ArrayList<HashMap<String, Object>> records) {
        super(records);
    }
}
