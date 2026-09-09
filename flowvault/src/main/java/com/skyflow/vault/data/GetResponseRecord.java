package com.skyflow.vault.data;

import java.util.List;
import java.util.Map;

// The wire shape is identical to InsertResponseRecord (the vault returns the same
// V1RecordResponseObject for get as it does for insert/update), kept as its own type so a
// get response doesn't expose an "Insert*" class name.
public class GetResponseRecord extends InsertResponseRecord {
    public GetResponseRecord(String tableName, String skyflowId, Map<String, List<Token>> tokens,
                              Map<String, Object> data, Map<String, Object> hashedData,
                              int httpCode, String error, String requestId) {
        super(tableName, skyflowId, tokens, data, hashedData, httpCode, error, requestId);
    }
}
