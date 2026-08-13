package com.skyflow.vault.data;

import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.logger.LogUtil;

import java.util.List;
import java.util.Map;

public class InsertResponseRecord {
    private final String tableName;
    private final String skyflowId;
    private final Map<String, List<Token>> tokens;
    private final Map<String, Object> data;
    private final Map<String, Object> hashedData;
    private final int httpCode;
    private final String error;

    /**
     * @deprecated Use {@link #InsertResponseRecord(String, String, Map, Map, Map, int, String)} instead,
     * which also lets you populate {@code data}. This overload always leaves {@code data} null.
     */
    @Deprecated(since = "1.0.2", forRemoval = true)
    public InsertResponseRecord(String tableName, String skyflowId, Map<String, List<Token>> tokens,
                                 Map<String, Object> hashedData, int httpCode, String error) {
        this(tableName, skyflowId, tokens, null, hashedData, httpCode, error);
    }

    public InsertResponseRecord(String tableName, String skyflowId, Map<String, List<Token>> tokens,
                                 Map<String, Object> data, Map<String, Object> hashedData, int httpCode, String error) {
        this.tableName = tableName;
        this.skyflowId = skyflowId;
        this.tokens = tokens;
        this.data = data;
        this.hashedData = hashedData;
        this.httpCode = httpCode;
        this.error = error;
    }

    public String getTableName() {
        return tableName;
    }

    public String getSkyflowId() {
        return skyflowId;
    }

    /**
     * Per-column token data. The API models a column's tokens generically (see
     * {@link Token#parseTokens(Map)}), but the SDK parses that into {@link Token} objects here
     * so callers get {@link Token#getToken()}/{@link Token#getTokenGroupName()} directly, with no
     * casting required.
     */
    public Map<String, List<Token>> getTokens() {
        return tokens;
    }

    /**
     * @deprecated Response key 'fields' is deprecated. Use {@link #getTokens()} instead.
     */
    @Deprecated(since = "1.0.2", forRemoval = true)
    public Map<String, List<Token>> getFields() {
        LogUtil.printWarningLog(InfoLogs.DEPRECATED_INSERT_FIELDS_GETTER.getLog());
        return getTokens();
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Map<String, Object> getHashedData() {
        return hashedData;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public String getError() {
        return error;
    }
}
