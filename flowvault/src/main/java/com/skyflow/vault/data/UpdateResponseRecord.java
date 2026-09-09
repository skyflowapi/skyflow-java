package com.skyflow.vault.data;

import java.util.List;
import java.util.Map;

// The wire shape is identical to InsertResponseRecord (the vault returns the same
// V1RecordResponseObject for both insert and update), kept as its own type so an update
// response doesn't expose an "Insert*" class name.
public class UpdateResponseRecord extends InsertResponseRecord {
    public UpdateResponseRecord(String tableName, String skyflowId, Map<String, List<Token>> tokens,
                                 Map<String, Object> data, Map<String, Object> hashedData,
                                 int httpCode, String error, String requestId) {
        super(tableName, skyflowId, tokens, data, hashedData, httpCode, error, requestId);
    }
}
