package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.logger.LogUtil;

import java.util.List;
import java.util.Map;

// Bulk counterpart of InsertResponseRecord. Extends it to inherit tableName/skyflowId/tokens/
// data/hashedData/httpCode/error/requestId unchanged; adds only what bulk needs on top: the
// caller-facing index, and the deprecated getFields()/6-arg-constructor back-compat surface that
// only bulk (the pre-existing method) still has to support for its pre-1.0.2 callers — the unary
// InsertResponseRecord is a brand-new type with none of that legacy surface to carry.
public class BulkInsertResponseRecord extends InsertResponseRecord {
    private final int index;

    /**
     * @deprecated Use {@link #BulkInsertResponseRecord(int, String, String, Map, Map, Map, int, String, String)}
     * instead, which also lets you populate {@code data}. This overload always leaves {@code data} null.
     */
    @Deprecated(since = "1.0.2", forRemoval = true)
    public BulkInsertResponseRecord(int index, String tableName, String skyflowId,
                                    Map<String, List<Token>> tokens, Map<String, Object> hashedData,
                                    int httpCode, String error, String requestId) {
        this(index, tableName, skyflowId, tokens, null, hashedData, httpCode, error, requestId);
    }

    public BulkInsertResponseRecord(int index, String tableName, String skyflowId,
                                    Map<String, List<Token>> tokens, Map<String, Object> data, Map<String, Object> hashedData,
                                    int httpCode, String error, String requestId) {
        super(tableName, skyflowId, tokens, data, hashedData, httpCode, error, requestId);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    /**
     * @deprecated Response key 'fields' is deprecated. Use {@link #getTokens()} instead. This
     * still returns {@code Map<String, Object>}, matching its original (pre-typed) contract —
     * see {@link Token#toRawTokens(Map)} for how {@link #getTokens()}'s typed data is rendered
     * back into that generic shape.
     */
    @Deprecated(since = "1.0.2", forRemoval = true)
    public Map<String, Object> getFields() {
        LogUtil.printWarningLog(InfoLogs.DEPRECATED_INSERT_FIELDS_GETTER.getLog());
        return Token.toRawTokens(getTokens());
    }

    @Override
    public String toString() {
        Gson gson = new Gson().newBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
